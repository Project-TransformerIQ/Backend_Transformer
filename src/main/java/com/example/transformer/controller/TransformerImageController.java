package com.example.transformer.controller;

import com.example.transformer.dto.ImageUploadDTO;
import com.example.transformer.dto.ImageUploadResponseDTO;
import com.example.transformer.dto.TransformerImageDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.*;
import com.example.transformer.repository.*;
import com.example.transformer.service.AnomalyDetectionService;
import com.example.transformer.service.FileStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

    import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/transformers")
public class TransformerImageController {

    private final TransformerRepository transformers;
    private final TransformerImageRepository images;
    private final FileStorageService storage;
    private final InspectionRepository inspectionRepository;
    private final FaultRegionRepository faultRegionRepository;
    private final DisplayMetadataRepository displayMetadataRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    private final OriginalAnomalyResultRepository originalAnomalyResultRepository;
    private final ObjectMapper objectMapper;

    public TransformerImageController(TransformerRepository transformers,
                                      TransformerImageRepository images,
                                      FileStorageService storage,
                                      InspectionRepository inspectionRepository,
                                      FaultRegionRepository faultRegionRepository,
                                      DisplayMetadataRepository displayMetadataRepository,
                                      AnomalyDetectionService anomalyDetectionService,
                                      OriginalAnomalyResultRepository originalAnomalyResultRepository,
                                      ObjectMapper objectMapper) {
        this.transformers = transformers;
        this.images = images;
        this.storage = storage;
        this.inspectionRepository = inspectionRepository;
        this.faultRegionRepository = faultRegionRepository;
        this.displayMetadataRepository = displayMetadataRepository;
        this.anomalyDetectionService = anomalyDetectionService;
        this.originalAnomalyResultRepository = originalAnomalyResultRepository;
        this.objectMapper = objectMapper;
    }

    // ---- Upload image (baseline/maintenance) ----
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponseDTO upload(@PathVariable Long id,
                                         @RequestPart("file") MultipartFile file,
                                         @RequestPart("meta") @Valid ImageUploadDTO meta,
                                         @RequestParam(value = "inspectionId", required = false) Long inspectionId)
            throws IOException {

        Transformer t = transformers.findById(id)
                .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));

        final Long finalInspectionId;
        if (meta.imageType() == ImageType.BASELINE) {
            finalInspectionId = null;
            if (meta.envCondition() == null || meta.envCondition().getWeather() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Baseline image requires envCondition.weather (SUNNY/CLOUDY/RAINY)");
            }
        } else {
            finalInspectionId = meta.inspectionId();
        }

        Inspection inspection = null;
        if (meta.imageType() == ImageType.MAINTENANCE) {
            if (finalInspectionId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "inspectionId is required for MAINTENANCE images");
            }
            inspection = inspectionRepository.findById(finalInspectionId)
                    .orElseThrow(() -> new NotFoundException("Inspection " + finalInspectionId + " not found"));

            if (inspection.getTransformer() == null || !Objects.equals(inspection.getTransformer().getId(), id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inspection does not belong to this transformer");
            }
        }

        String subfolder = meta.imageType().name().toLowerCase(Locale.ROOT);
        String relativePath = storage.saveTransformerImage(id, subfolder, file);

        TransformerImage entity = new TransformerImage();
        entity.setTransformer(t);
        entity.setInspection(inspection);
        entity.setImageType(meta.imageType());
        entity.setEnvCondition(meta.imageType() == ImageType.BASELINE ? meta.envCondition() : null);
        entity.setUploader(meta.uploader());
        entity.setFilename(Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin"));
        entity.setContentType(Optional.ofNullable(file.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE));
        entity.setSizeBytes(file.getSize());
        entity.setStoragePath(relativePath);
        entity.setCreatedAt(LocalDateTime.now());

        TransformerImage img = images.save(entity);

        TransformerImageDTO imageDTO = new TransformerImageDTO(
                img.getId(),
                img.getImageType(),
                img.getUploader(),
                img.getEnvCondition(),
                img.getFilename(),
                img.getCreatedAt(),
                img.getContentType(),
                img.getSizeBytes(),
                img.getInspection() == null ? null : img.getInspection().getId());

        Object anomalyResult = null;

        if (meta.imageType() == ImageType.MAINTENANCE) {
            List<TransformerImage> baselineImages = images.findByTransformerIdAndImageTypeOrderByCreatedAtDesc(id,
                    ImageType.BASELINE);

            if (!baselineImages.isEmpty()) {
                TransformerImage baseline = baselineImages.get(0);

                try {
                    String flaskJson = anomalyDetectionService.detectAnomalies(
                            storage.load(baseline.getStoragePath()),
                            storage.load(img.getStoragePath()));

                    JsonNode root = objectMapper.readTree(flaskJson);

                    if (root.has("fault_regions")) {
                        JsonNode faultRegionsNode = root.get("fault_regions");

                        for (JsonNode fr : faultRegionsNode) {
                            FaultRegion region = new FaultRegion();

                            region.setRegionId(fr.has("id") && !fr.get("id").isNull() ? fr.get("id").asInt() : null);
                            region.setType(fr.has("type") && !fr.get("type").isNull() ? fr.get("type").asText() : null);
                            region.setDominantColor(
                                    fr.has("dominant_color") && !fr.get("dominant_color").isNull()
                                            ? fr.get("dominant_color").asText()
                                            : null);

                            List<Integer> rgb = new ArrayList<>();
                            if (fr.has("color_rgb") && !fr.get("color_rgb").isNull()) {
                                for (JsonNode c : fr.get("color_rgb")) {
                                    if (!c.isNull())
                                        rgb.add(c.asInt());
                                }
                            }
                            region.setColorRgb(rgb);

                            if (fr.has("boundingBox") && !fr.get("boundingBox").isNull()) {
                                FaultRegion.BoundingBox bb = new FaultRegion.BoundingBox();
                                JsonNode bbNode = fr.get("boundingBox");
                                bb.setX(bbNode.has("x") && !bbNode.get("x").isNull() ? bbNode.get("x").asInt() : null);
                                bb.setY(bbNode.has("y") && !bbNode.get("y").isNull() ? bbNode.get("y").asInt() : null);
                                bb.setWidth(
                                        bbNode.has("width") && !bbNode.get("width").isNull() ? bbNode.get("width").asInt()
                                                : null);
                                bb.setHeight(
                                        bbNode.has("height") && !bbNode.get("height").isNull()
                                                ? bbNode.get("height").asInt()
                                                : null);
                                bb.setAreaPx(
                                        bbNode.has("areaPx") && !bbNode.get("areaPx").isNull()
                                                ? bbNode.get("areaPx").asInt()
                                                : null);

                                region.setBoundingBox(bb);
                            } else if (fr.has("bounding_box") && !fr.get("bounding_box").isNull()) {
                                FaultRegion.BoundingBox bb = new FaultRegion.BoundingBox();
                                JsonNode bbNode = fr.get("bounding_box");
                                bb.setX(bbNode.has("x") && !bbNode.get("x").isNull() ? bbNode.get("x").asInt() : null);
                                bb.setY(bbNode.has("y") && !bbNode.get("y").isNull() ? bbNode.get("y").asInt() : null);
                                bb.setWidth(
                                        bbNode.has("width") && !bbNode.get("width").isNull() ? bbNode.get("width").asInt()
                                                : null);
                                bb.setHeight(
                                        bbNode.has("height") && !bbNode.get("height").isNull()
                                                ? bbNode.get("height").asInt()
                                                : null);

                                Integer areaPx = null;
                                if (bbNode.has("area_px") && !bbNode.get("area_px").isNull()) {
                                    areaPx = bbNode.get("area_px").asInt();
                                } else if (bbNode.has("areaPx") && !bbNode.get("areaPx").isNull()) {
                                    areaPx = bbNode.get("areaPx").asInt();
                                }
                                bb.setAreaPx(areaPx);

                                region.setBoundingBox(bb);
                            }

                            if (fr.has("centroid") && !fr.get("centroid").isNull()) {
                                FaultRegion.Centroid cent = new FaultRegion.Centroid();
                                JsonNode centNode = fr.get("centroid");
                                cent.setX(
                                        centNode.has("x") && !centNode.get("x").isNull() ? centNode.get("x").asInt()
                                                : null);
                                cent.setY(
                                        centNode.has("y") && !centNode.get("y").isNull() ? centNode.get("y").asInt()
                                                : null);
                                region.setCentroid(cent);
                            }

                            region.setAspectRatio(
                                    fr.has("aspect_ratio") && !fr.get("aspect_ratio").isNull()
                                            ? fr.get("aspect_ratio").asDouble()
                                            : null);
                            region.setElongated(
                                    fr.has("elongated") && !fr.get("elongated").isNull()
                                            ? fr.get("elongated").asBoolean()
                                            : null);
                            region.setConnectedToWire(fr.has("connected_to_wire") && !fr.get("connected_to_wire").isNull()
                                    ? fr.get("connected_to_wire").asBoolean()
                                    : null);
                            region.setTag(fr.has("tag") && !fr.get("tag").isNull() ? fr.get("tag").asText() : null);
                            region.setConfidence(
                                    fr.has("confidence") && !fr.get("confidence").isNull()
                                            ? fr.get("confidence").asDouble()
                                            : null);
                            region.setImage(img);
                            faultRegionRepository.save(region);
                        }
                    }

                    if (root.has("display_metadata") && !root.get("display_metadata").isNull()) {
                        DisplayMetadata dm = new DisplayMetadata();
                        JsonNode dmNode = root.get("display_metadata");

                        if (dmNode.has("box_colors") && !dmNode.get("box_colors").isNull()) {
                            Map<String, String> boxColors = new HashMap<>();
                            JsonNode bcNode = dmNode.get("box_colors");
                            Iterator<String> keys = bcNode.fieldNames();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                JsonNode val = bcNode.get(key);
                                if (val != null && val.isArray() && val.size() >= 3) {
                                    String rgbStr = val.get(0).asInt() + "," + val.get(1).asInt() + "," + val.get(2).asInt();
                                    boxColors.put(key, rgbStr);
                                }
                            }
                            dm.setBoxColors(boxColors);
                        }

                        if (root.has("timestamp") && !root.get("timestamp").isNull()) {
                            try {
                                dm.setTimestamp(LocalDateTime.parse(root.get("timestamp").asText()));
                            } catch (Exception ex) {
                                System.err.println("Failed to parse timestamp: " + ex.getMessage());
                            }
                        }
                        dm.setImage(img);
                        displayMetadataRepository.save(dm);
                    }

                    anomalyResult = objectMapper.readValue(flaskJson, Object.class);

                    OriginalAnomalyResult originalResult = OriginalAnomalyResult.builder()
                            .image(img)
                            .anomalyJson(flaskJson)
                            .build();
                    originalAnomalyResultRepository.save(originalResult);

                } catch (Exception e) {
                    System.err.println("Anomaly detection failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return new ImageUploadResponseDTO(imageDTO, anomalyResult);
    }

    @GetMapping("/{id}/images")
    public List<TransformerImageDTO> listImages(@PathVariable Long id,
                                                @RequestParam(value = "type", required = false) ImageType type,
                                                @RequestParam(value = "inspectionId", required = false) Long inspectionId) {
        if (!transformers.existsById(id)) {
            throw new NotFoundException("Transformer " + id + " not found");
        }

        List<TransformerImage> all = images.findByTransformerIdOrderByCreatedAtDesc(id);

        List<TransformerImageDTO> out = new java.util.ArrayList<>(all.size());
        for (int i = 0; i < all.size(); i++) {
            TransformerImage img = all.get(i);

            if (type != null && img.getImageType() != type) {
                continue;
            }

            if (inspectionId != null) {
                if (img.getInspection() == null) {
                    continue;
                }
                if (!java.util.Objects.equals(img.getInspection().getId(), inspectionId)) {
                    continue;
                }
            }

            out.add(new TransformerImageDTO(
                    img.getId(),
                    img.getImageType(),
                    img.getUploader(),
                    img.getEnvCondition(),
                    img.getFilename(),
                    img.getCreatedAt(),
                    img.getContentType(),
                    img.getSizeBytes(),
                    (img.getInspection() == null ? null : img.getInspection().getId())));
        }
        return out;
    }

    @GetMapping("/images/{imageId}/raw")
    public ResponseEntity<?> raw(@PathVariable Long imageId) {
        TransformerImage img = images.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));
        var res = storage.load(img.getStoragePath());
        if (!res.exists())
            throw new NotFoundException("File missing on disk");

        MediaType mt = Optional.ofNullable(img.getContentType())
                .map(MediaType::parseMediaType)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mt)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + img.getFilename() + "\"")
                .body(res);
    }
}
