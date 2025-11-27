package com.example.transformer.controller;

import com.example.transformer.dto.DisplayMetadataDTO;
import com.example.transformer.dto.FaultRegionDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.*;
import com.example.transformer.repository.DisplayMetadataRepository;
import com.example.transformer.repository.FaultRegionRepository;
import com.example.transformer.repository.OriginalAnomalyResultRepository;
import com.example.transformer.repository.TransformerImageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/transformers")
public class ImageAnomalyController {

    private final TransformerImageRepository images;
    private final FaultRegionRepository faultRegionRepository;
    private final DisplayMetadataRepository displayMetadataRepository;
    private final OriginalAnomalyResultRepository originalAnomalyResultRepository;
    private final ObjectMapper objectMapper;

    public ImageAnomalyController(TransformerImageRepository images,
                                  FaultRegionRepository faultRegionRepository,
                                  DisplayMetadataRepository displayMetadataRepository,
                                  OriginalAnomalyResultRepository originalAnomalyResultRepository,
                                  ObjectMapper objectMapper) {
        this.images = images;
        this.faultRegionRepository = faultRegionRepository;
        this.displayMetadataRepository = displayMetadataRepository;
        this.originalAnomalyResultRepository = originalAnomalyResultRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/images/{imageId}/fault-regions")
    public List<FaultRegionDTO> getFaultRegions(@PathVariable Long imageId) {
        if (!images.existsById(imageId)) {
            throw new NotFoundException("Image " + imageId + " not found");
        }
        List<FaultRegion> entities = faultRegionRepository.findByImageIdOrderByRegionIdAsc(imageId);
        return entities.stream().map(FaultRegionDTO::fromEntity).toList();
    }

    @GetMapping("/images/{imageId}/display-metadata")
    public ResponseEntity<DisplayMetadataDTO> getDisplayMetadata(@PathVariable Long imageId) {
        if (!images.existsById(imageId)) {
            throw new NotFoundException("Image " + imageId + " not found");
        }

        Optional<DisplayMetadata> metadata = displayMetadataRepository.findByImageId(imageId);
        return metadata.map(entity -> ResponseEntity.ok(DisplayMetadataDTO.fromEntity(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

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

        List<FaultRegionDTO> faultRegionDTOs = faultRegionEntities.stream()
                .map(FaultRegionDTO::fromEntity)
                .toList();

        result.put("fault_regions", faultRegionDTOs);
        displayMetadataEntity.ifPresent(dm -> result.put("display_metadata", DisplayMetadataDTO.fromEntity(dm)));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/images/{imageId}/anomaly-comparison")
    public ResponseEntity<Map<String, Object>> downloadAnomalyComparison(@PathVariable Long imageId) {
        TransformerImage img = images.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));

        if (img.getImageType() != ImageType.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Anomaly comparison is only available for maintenance images");
        }

        Optional<OriginalAnomalyResult> originalResultOpt = originalAnomalyResultRepository.findByImageId(imageId);
        List<FaultRegion> currentFaultRegions = faultRegionRepository.findByImageIdOrderByRegionIdAsc(imageId);
        Optional<DisplayMetadata> currentDisplayMetadata = displayMetadataRepository.findByImageId(imageId);

        Map<String, Object> response = new HashMap<>();

        Map<String, Object> imageInfo = new HashMap<>();
        imageInfo.put("imageId", img.getId());
        imageInfo.put("filename", img.getFilename());
        imageInfo.put("uploadedAt", img.getCreatedAt());
        imageInfo.put("uploader", img.getUploader());
        if (img.getInspection() != null) {
            imageInfo.put("inspectionId", img.getInspection().getId());
            imageInfo.put("inspectionTitle", img.getInspection().getTitle());
        }
        response.put("imageInfo", imageInfo);

        if (originalResultOpt.isPresent()) {
            try {
                Object originalJson = objectMapper.readValue(originalResultOpt.get().getAnomalyJson(), Object.class);
                Map<String, Object> originalData = new HashMap<>();
                originalData.put("receivedAt", originalResultOpt.get().getCreatedAt());
                originalData.put("data", originalJson);
                response.put("originalResults", originalData);
            } catch (Exception e) {
                System.err.println("Failed to parse original anomaly JSON: " + e.getMessage());
                response.put("originalResults", null);
            }
        } else {
            response.put("originalResults", null);
        }

        Map<String, Object> currentResults = new HashMap<>();

        List<FaultRegionDTO> currentFaultRegionDTOs = currentFaultRegions.stream()
                .map(FaultRegionDTO::fromEntity)
                .toList();
        currentResults.put("fault_regions", currentFaultRegionDTOs);

        currentDisplayMetadata.ifPresent(dm ->
                currentResults.put("display_metadata", DisplayMetadataDTO.fromEntity(dm)));

        long manuallyAddedCount = currentFaultRegions.stream()
                .filter(fr -> fr.getIsManual() != null && fr.getIsManual())
                .count();
        long deletedCount = currentFaultRegions.stream()
                .filter(fr -> fr.getIsDeleted() != null && fr.getIsDeleted())
                .count();
        long modifiedCount = currentFaultRegions.stream()
                .filter(fr -> fr.getLastModifiedAt() != null)
                .count();

        Map<String, Object> editSummary = new HashMap<>();
        editSummary.put("totalRegions", currentFaultRegions.size());
        editSummary.put("manuallyAdded", manuallyAddedCount);
        editSummary.put("deleted", deletedCount);
        editSummary.put("modified", modifiedCount);
        currentResults.put("editSummary", editSummary);

        response.put("currentResults", currentResults);
        response.put("generatedAt", LocalDateTime.now());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"anomaly-comparison-image-" + imageId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @GetMapping("/debug/latest-maintenance-bounding-box")
    public ResponseEntity<Map<String, Object>> getLatestMaintenanceBoundingBox() {
        List<TransformerImage> maintenanceImages = images.findByImageTypeOrderByCreatedAtDesc(ImageType.MAINTENANCE);

        if (maintenanceImages.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransformerImage latestImage = maintenanceImages.get(0);
        Long imageId = latestImage.getId();

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
