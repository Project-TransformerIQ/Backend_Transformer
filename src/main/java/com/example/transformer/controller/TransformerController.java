package com.example.transformer.controller;

import com.example.transformer.dto.CreateErrorAnnotationDTO;
import com.example.transformer.dto.ImageUploadDTO;
import com.example.transformer.dto.TransformerDTO;
import com.example.transformer.dto.TransformerImageDTO;
import com.example.transformer.dto.ImageUploadResponseDTO;
import com.example.transformer.dto.FaultRegionDTO;
import com.example.transformer.dto.DisplayMetadataDTO;
import com.example.transformer.dto.ErrorAnnotationDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.ImageType;
import com.example.transformer.model.Transformer;
import com.example.transformer.model.TransformerImage;
import com.example.transformer.model.FaultRegion;
import com.example.transformer.model.DisplayMetadata;
import com.example.transformer.repository.TransformerImageRepository;
import com.example.transformer.repository.TransformerRepository;
import com.example.transformer.service.FileStorageService;
import com.example.transformer.service.AnomalyDetectionService;
import com.example.transformer.service.ErrorAnnotationService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.example.transformer.dto.CreateInspectionDTO;
import com.example.transformer.dto.InspectionDTO;
import com.example.transformer.model.Inspection;
import com.example.transformer.model.InspectionStatus;
import com.example.transformer.repository.InspectionRepository;
import com.example.transformer.repository.FaultRegionRepository;
import com.example.transformer.repository.DisplayMetadataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/transformers")
public class TransformerController {

  private final TransformerRepository transformers;
  private final TransformerImageRepository images;
  private final FileStorageService storage;
  private final InspectionRepository inspectionRepository;
  private final FaultRegionRepository faultRegionRepository;
  private final DisplayMetadataRepository displayMetadataRepository;
  private final AnomalyDetectionService anomalyDetectionService;
  private final ErrorAnnotationService errorAnnotationService;
  private final ObjectMapper objectMapper;

  public TransformerController(TransformerRepository transformers,
      TransformerImageRepository images,
      FileStorageService storage,
      InspectionRepository inspectionRepository,
      FaultRegionRepository faultRegionRepository,
      DisplayMetadataRepository displayMetadataRepository,
      AnomalyDetectionService anomalyDetectionService,
      ErrorAnnotationService errorAnnotationService,
      ObjectMapper objectMapper) {
    this.transformers = transformers;
    this.images = images;
    this.storage = storage;
    this.inspectionRepository = inspectionRepository;
    this.faultRegionRepository = faultRegionRepository;
    this.displayMetadataRepository = displayMetadataRepository;
    this.anomalyDetectionService = anomalyDetectionService;
    this.errorAnnotationService = errorAnnotationService;
    this.objectMapper = objectMapper;
  }

  // ---- CRUD: transformers ----
  @PostMapping
  public Transformer create(@RequestBody @Valid TransformerDTO dto) {
    Transformer t = Transformer.builder()
        .transformerNo(dto.transformerNo())
        .poleNo(dto.poleNo())
        .region(dto.region())
        .transformerType(dto.transformerType())
        .build();
    return transformers.save(t);
  }

  @GetMapping
  public List<Transformer> list() {
    return transformers.findAll();
  }

  @GetMapping("/{id}")
  public Transformer get(@PathVariable Long id) {
    return transformers.findById(id)
        .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));
  }

  @PutMapping("/{id}")
  public Transformer update(@PathVariable Long id, @RequestBody @Valid TransformerDTO dto) {
    Transformer t = get(id);
    t.setTransformerNo(dto.transformerNo());
    t.setPoleNo(dto.poleNo());
    t.setRegion(dto.region());
    t.setTransformerType(dto.transformerType());
    return transformers.save(t);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    if (!transformers.existsById(id))
      throw new NotFoundException("Transformer " + id + " not found");
    transformers.deleteById(id);
  }

  // ---- Upload image (baseline/maintenance) ----
  // Expect multipart with parts:
  // file : binary
  // meta : application/json -> ImageUploadDTO { imageType, envCondition?,
  // uploader? }
  // For MAINTENANCE: pass inspectionId as query param or form field.
  @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImageUploadResponseDTO upload(@PathVariable Long id,
      @RequestPart("file") MultipartFile file,
      @RequestPart("meta") @Valid ImageUploadDTO meta,
      @RequestParam(value = "inspectionId", required = false) Long inspectionId) throws IOException {
    Transformer t = get(id);

    // Consolidate inspectionId from query OR multipart part
    final Long finalInspectionId;
    if (meta.imageType() == ImageType.BASELINE) {
      finalInspectionId = null; // baseline must NOT tie to an inspection
      if (meta.envCondition() == null || meta.envCondition().getWeather() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Baseline image requires envCondition.weather (SUNNY/CLOUDY/RAINY)");
      }
    } else {
      finalInspectionId = meta.inspectionId();
    }

    // Validate Maintenance rules
    Inspection inspection = null;
    if (meta.imageType() == ImageType.MAINTENANCE) {
      if (finalInspectionId == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "inspectionId is required for MAINTENANCE images");
      }
      inspection = inspectionRepository.findById(finalInspectionId)
          .orElseThrow(() -> new NotFoundException("Inspection " + finalInspectionId + " not found"));

      // Ensure inspection belongs to this transformer
      if (inspection.getTransformer() == null || !Objects.equals(inspection.getTransformer().getId(), id)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inspection does not belong to this transformer");
      }
    }

    // Persist file under
    // /uploads/transformers/{id}/{baseline|maintenance}/TS-filename
    String subfolder = meta.imageType().name().toLowerCase(Locale.ROOT);
    String relativePath = storage.saveTransformerImage(id, subfolder, file);

    // Save DB row
    TransformerImage entity = new TransformerImage();
    entity.setTransformer(t);
    entity.setInspection(inspection); // null for baseline; set for maintenance
    entity.setImageType(meta.imageType());
    entity.setEnvCondition(meta.imageType() == ImageType.BASELINE ? meta.envCondition() : null);
    entity.setUploader(meta.uploader());
    entity.setFilename(Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin"));
    entity.setContentType(Optional.ofNullable(file.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE));
    entity.setSizeBytes(file.getSize());
    entity.setStoragePath(relativePath); // if your entity has this field
    entity.setCreatedAt(LocalDateTime.now());

    TransformerImage img = images.save(entity);

    // Create base response
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

    // --- Anomaly detection and DB save ---
    if (meta.imageType() == ImageType.MAINTENANCE) {
      // Find latest baseline image
      List<TransformerImage> baselineImages = images.findByTransformerIdAndImageTypeOrderByCreatedAtDesc(id,
          ImageType.BASELINE);

      if (!baselineImages.isEmpty()) {
        TransformerImage baseline = baselineImages.get(0);

        try {
          // Call Flask API with actual service
          String flaskJson = anomalyDetectionService.detectAnomalies(
              storage.load(baseline.getStoragePath()),
              storage.load(img.getStoragePath()));

          // Parse and save to database
          JsonNode root = objectMapper.readTree(flaskJson);

          // Save fault regions
          if (root.has("fault_regions")) {
            JsonNode faultRegionsNode = root.get("fault_regions");

            for (JsonNode fr : faultRegionsNode) {
              FaultRegion region = new FaultRegion();

              // Safe parsing with null checks
              region.setRegionId(fr.has("id") && !fr.get("id").isNull() ? fr.get("id").asInt() : null);
              region.setType(fr.has("type") && !fr.get("type").isNull() ? fr.get("type").asText() : null);
              region.setDominantColor(
                  fr.has("dominant_color") && !fr.get("dominant_color").isNull() ? fr.get("dominant_color").asText()
                      : null);

              // Parse color RGB
              List<Integer> rgb = new ArrayList<>();
              if (fr.has("color_rgb") && !fr.get("color_rgb").isNull()) {
                for (JsonNode c : fr.get("color_rgb")) {
                  if (!c.isNull())
                    rgb.add(c.asInt());
                }
              }
              region.setColorRgb(rgb);

              // Parse bounding box - handle both camelCase and snake_case
              if (fr.has("boundingBox") && !fr.get("boundingBox").isNull()) {
                // Handle camelCase format from Flask API
                FaultRegion.BoundingBox bb = new FaultRegion.BoundingBox();
                JsonNode bbNode = fr.get("boundingBox");
                bb.setX(bbNode.has("x") && !bbNode.get("x").isNull() ? bbNode.get("x").asInt() : null);
                bb.setY(bbNode.has("y") && !bbNode.get("y").isNull() ? bbNode.get("y").asInt() : null);
                bb.setWidth(bbNode.has("width") && !bbNode.get("width").isNull() ? bbNode.get("width").asInt() : null);
                bb.setHeight(
                    bbNode.has("height") && !bbNode.get("height").isNull() ? bbNode.get("height").asInt() : null);
                bb.setAreaPx(
                    bbNode.has("areaPx") && !bbNode.get("areaPx").isNull() ? bbNode.get("areaPx").asInt() : null);

                region.setBoundingBox(bb);
              } else if (fr.has("bounding_box") && !fr.get("bounding_box").isNull()) {
                // Handle snake_case format (legacy)
                FaultRegion.BoundingBox bb = new FaultRegion.BoundingBox();
                JsonNode bbNode = fr.get("bounding_box");
                bb.setX(bbNode.has("x") && !bbNode.get("x").isNull() ? bbNode.get("x").asInt() : null);
                bb.setY(bbNode.has("y") && !bbNode.get("y").isNull() ? bbNode.get("y").asInt() : null);
                bb.setWidth(bbNode.has("width") && !bbNode.get("width").isNull() ? bbNode.get("width").asInt() : null);
                bb.setHeight(
                    bbNode.has("height") && !bbNode.get("height").isNull() ? bbNode.get("height").asInt() : null);

                // Handle both snake_case (area_px) and camelCase (areaPx) formats for area
                Integer areaPx = null;
                if (bbNode.has("area_px") && !bbNode.get("area_px").isNull()) {
                  areaPx = bbNode.get("area_px").asInt();
                } else if (bbNode.has("areaPx") && !bbNode.get("areaPx").isNull()) {
                  areaPx = bbNode.get("areaPx").asInt();
                }
                bb.setAreaPx(areaPx);

                region.setBoundingBox(bb);
              }

              // Parse centroid
              if (fr.has("centroid") && !fr.get("centroid").isNull()) {
                FaultRegion.Centroid cent = new FaultRegion.Centroid();
                JsonNode centNode = fr.get("centroid");
                cent.setX(centNode.has("x") && !centNode.get("x").isNull() ? centNode.get("x").asInt() : null);
                cent.setY(centNode.has("y") && !centNode.get("y").isNull() ? centNode.get("y").asInt() : null);
                region.setCentroid(cent);
              }

              region.setAspectRatio(
                  fr.has("aspect_ratio") && !fr.get("aspect_ratio").isNull() ? fr.get("aspect_ratio").asDouble()
                      : null);
              region.setElongated(
                  fr.has("elongated") && !fr.get("elongated").isNull() ? fr.get("elongated").asBoolean() : null);
              region.setConnectedToWire(fr.has("connected_to_wire") && !fr.get("connected_to_wire").isNull()
                  ? fr.get("connected_to_wire").asBoolean()
                  : null);
              region.setTag(fr.has("tag") && !fr.get("tag").isNull() ? fr.get("tag").asText() : null);
              region.setConfidence(
                  fr.has("confidence") && !fr.get("confidence").isNull() ? fr.get("confidence").asDouble() : null);
              region.setImage(img);
              faultRegionRepository.save(region);
            }
          }

          // Save display metadata
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
                  // Store as comma-separated RGB string
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

          // Parse JSON for response
          anomalyResult = objectMapper.readValue(flaskJson, Object.class);

        } catch (Exception e) {
          // Log error but don't fail the upload
          System.err.println("Anomaly detection failed: " + e.getMessage());
          e.printStackTrace();
        }
      }
    }

    return new ImageUploadResponseDTO(imageDTO, anomalyResult);
  }

  // ---- List images (supports optional filters) ----
  // /{id}/images?type=BASELINE|MAINTENANCE&inspectionId=123
  @GetMapping("/{id}/images")
  public List<TransformerImageDTO> listImages(@PathVariable Long id,
      @RequestParam(value = "type", required = false) ImageType type,
      @RequestParam(value = "inspectionId", required = false) Long inspectionId) {
    if (!transformers.existsById(id)) {
      throw new NotFoundException("Transformer " + id + " not found");
    }

    // Always start with images for this transformer, newest first
    List<TransformerImage> all = images.findByTransformerIdOrderByCreatedAtDesc(id);

    // Build response with plain loops (no streams/lambdas)
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

  // ---- Serve raw file for thumbnails ----
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

  // ---- Anomaly Detection Data Endpoints ----

  // Get fault regions for a specific image
  @GetMapping("/images/{imageId}/fault-regions")
  public List<FaultRegionDTO> getFaultRegions(@PathVariable Long imageId) {
    if (!images.existsById(imageId)) {
      throw new NotFoundException("Image " + imageId + " not found");
    }
    List<FaultRegion> entities = faultRegionRepository.findByImageIdOrderByRegionIdAsc(imageId);
    return entities.stream().map(FaultRegionDTO::fromEntity).toList();
  }

  // Get display metadata for a specific image
  @GetMapping("/images/{imageId}/display-metadata")
  public ResponseEntity<DisplayMetadataDTO> getDisplayMetadata(@PathVariable Long imageId) {
    if (!images.existsById(imageId)) {
      throw new NotFoundException("Image " + imageId + " not found");
    }

    Optional<DisplayMetadata> metadata = displayMetadataRepository.findByImageId(imageId);
    return metadata.map(entity -> ResponseEntity.ok(DisplayMetadataDTO.fromEntity(entity)))
        .orElse(ResponseEntity.notFound().build());
  }

  // Get all annotated errors for a specific image
  @GetMapping("/images/{imageId}/errors")
  public ResponseEntity<Map<String, Object>> getImageErrors(@PathVariable Long imageId) {
    TransformerImage img = images.findById(imageId)
        .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));

    if (img.getImageType() != ImageType.MAINTENANCE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Error annotations are only available for maintenance images");
    }

    List<FaultRegion> faultRegionEntities = faultRegionRepository.findByImageIdOrderByRegionIdAsc(imageId);

    if (faultRegionEntities.isEmpty()) {
      // Return empty data array instead of 404
      Map<String, Object> result = new HashMap<>();
      result.put("data", new ArrayList<>());
      return ResponseEntity.ok(result);
    }

    // Convert entities to error annotation DTOs
    List<ErrorAnnotationDTO> errorAnnotations = faultRegionEntities.stream()
        .map(ErrorAnnotationDTO::fromFaultRegion)
        .toList();

    Map<String, Object> result = new HashMap<>();
    result.put("data", errorAnnotations);

    return ResponseEntity.ok(result);
  }

  // Create a new annotated error for a specific image
  @PostMapping("/images/{imageId}/errors")
  public ResponseEntity<Map<String, Object>> createImageError(
      @PathVariable Long imageId,
      @RequestBody @Valid CreateErrorAnnotationDTO request) {

    // Validate that imageId in path matches imageId in request body
    if (!imageId.equals(request.imageId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Image ID in path does not match image ID in request body");
    }

    // Create the error annotation using the service
    ErrorAnnotationDTO createdAnnotation = errorAnnotationService.createErrorAnnotation(request);

    // Wrap response in a "data" field as per API specification
    Map<String, Object> response = new HashMap<>();
    response.put("data", createdAnnotation);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // Legacy endpoint - kept for backward compatibility
  @GetMapping("/images/{imageId}/anomaly-results")
  public ResponseEntity<Map<String, Object>> getAnomalyResults(@PathVariable Long imageId) {
    TransformerImage img = images.findById(imageId)
        .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));

    if (img.getImageType() != ImageType.MAINTENANCE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Anomaly detection results are only available for maintenance images");
    }

    List<FaultRegion> faultRegionEntities = faultRegionRepository.findByImageIdOrderByRegionIdAsc(imageId);
    Optional<DisplayMetadata> displayMetadataEntity = displayMetadataRepository.findByImageId(imageId);

    if (faultRegionEntities.isEmpty() && displayMetadataEntity.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Map<String, Object> result = new HashMap<>();

    // Convert entities to DTOs
    List<FaultRegionDTO> faultRegionDTOs = faultRegionEntities.stream()
        .map(FaultRegionDTO::fromEntity)
        .toList();

    result.put("fault_regions", faultRegionDTOs);
    displayMetadataEntity.ifPresent(dm -> result.put("display_metadata", DisplayMetadataDTO.fromEntity(dm)));

    return ResponseEntity.ok(result);
  }

  // ---- End Anomaly Detection Endpoints ----

  // ---- Inspections ----
  @GetMapping("/{id}/inspections")
  public List<InspectionDTO> listInspections(@PathVariable Long id) {
    if (!transformers.existsById(id))
      throw new NotFoundException("Transformer " + id + " not found");
    return inspectionRepository.findByTransformerIdOrderByCreatedAtDesc(id)
        .stream().map(InspectionDTO::fromEntity).toList();
  }

  @PostMapping("/{id}/inspections")
  public ResponseEntity<InspectionDTO> addInspection(@PathVariable Long id,
      @RequestBody @Valid CreateInspectionDTO body) {
    Transformer t = transformers.findById(id)
        .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));

    Inspection ins = Inspection.builder()
        .transformer(t)
        .title(body.title())
        .inspector(body.inspector())
        .notes(body.notes())
        .status(body.status() == null ? InspectionStatus.OPEN : body.status())
        .createdAt(LocalDateTime.now())
        .build();

    inspectionRepository.save(ins);
    return ResponseEntity.status(HttpStatus.CREATED).body(InspectionDTO.fromEntity(ins));
  }

  // ---- Debug endpoint to check bounding box values ----
  @GetMapping("/debug/latest-maintenance-bounding-box")
  public ResponseEntity<Map<String, Object>> getLatestMaintenanceBoundingBox() {
    // Find the latest maintenance image
    List<TransformerImage> maintenanceImages = images.findByImageTypeOrderByCreatedAtDesc(ImageType.MAINTENANCE);

    if (maintenanceImages.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    TransformerImage latestImage = maintenanceImages.get(0);
    Long imageId = latestImage.getId();

    // Get fault regions for this image
    List<FaultRegion> faultRegions = faultRegionRepository.findByImageIdOrderByRegionIdAsc(imageId);

    Map<String, Object> debugInfo = new HashMap<>();
    debugInfo.put("imageId", imageId);
    debugInfo.put("imageName", latestImage.getFilename());
    debugInfo.put("createdAt", latestImage.getCreatedAt());
    debugInfo.put("faultRegionCount", faultRegions.size());

    List<Map<String, Object>> boundingBoxDetails = new ArrayList<>();

    for (FaultRegion region : faultRegions) {
      Map<String, Object> regionInfo = new HashMap<>();
      regionInfo.put("dbId", region.getDbId());
      regionInfo.put("regionId", region.getRegionId());
      regionInfo.put("type", region.getType());

      if (region.getBoundingBox() != null) {
        Map<String, Object> bbInfo = new HashMap<>();
        bbInfo.put("x", region.getBoundingBox().getX());
        bbInfo.put("y", region.getBoundingBox().getY());
        bbInfo.put("width", region.getBoundingBox().getWidth());
        bbInfo.put("height", region.getBoundingBox().getHeight());
        bbInfo.put("areaPx", region.getBoundingBox().getAreaPx());
        regionInfo.put("boundingBox", bbInfo);
      } else {
        regionInfo.put("boundingBox", "NULL");
      }

      if (region.getCentroid() != null) {
        Map<String, Object> centroidInfo = new HashMap<>();
        centroidInfo.put("x", region.getCentroid().getX());
        centroidInfo.put("y", region.getCentroid().getY());
        regionInfo.put("centroid", centroidInfo);
      } else {
        regionInfo.put("centroid", "NULL");
      }

      regionInfo.put("confidence", region.getConfidence());
      regionInfo.put("tag", region.getTag());

      boundingBoxDetails.add(regionInfo);
    }

    debugInfo.put("faultRegions", boundingBoxDetails);

    return ResponseEntity.ok(debugInfo);
  }
}
