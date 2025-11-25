package com.example.transformer.controller;

import com.example.transformer.model.MaintenanceRecord;
import com.example.transformer.model.MaintenanceStatus;
import com.example.transformer.repository.MaintenanceRecordRepository;
import com.example.transformer.dto.MaintenanceRecordDTO;
import com.example.transformer.dto.CreateMaintenanceRecordDTO;
import com.example.transformer.dto.UpdateMaintenanceRecordDTO;
import com.example.transformer.dto.MaintenanceRecordFormDTO;

import com.example.transformer.dto.CreateErrorAnnotationDTO;
import com.example.transformer.dto.UpdateErrorAnnotationDTO;
import com.example.transformer.dto.ImageUploadDTO;
import com.example.transformer.dto.TransformerDTO;
import com.example.transformer.dto.TransformerImageDTO;
import com.example.transformer.dto.ImageUploadResponseDTO;
import com.example.transformer.dto.FaultRegionDTO;
import com.example.transformer.dto.DisplayMetadataDTO;
import com.example.transformer.dto.ErrorAnnotationDTO;
import com.example.transformer.dto.TrainModelRequestDTO;
import com.example.transformer.dto.TrainModelResponseDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.ImageType;
import com.example.transformer.model.Transformer;
import com.example.transformer.model.TransformerImage;
import com.example.transformer.model.FaultRegion;
import com.example.transformer.model.DisplayMetadata;
import com.example.transformer.model.OriginalAnomalyResult;
import com.example.transformer.repository.TransformerImageRepository;
import com.example.transformer.repository.TransformerRepository;
import com.example.transformer.service.FileStorageService;
import com.example.transformer.service.AnomalyDetectionService;
import com.example.transformer.service.ErrorAnnotationService;
import com.example.transformer.service.ClassificationTrainingService;
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
import com.example.transformer.repository.OriginalAnomalyResultRepository;
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
  private final ClassificationTrainingService classificationTrainingService;
  private final OriginalAnomalyResultRepository originalAnomalyResultRepository;
  private final ObjectMapper objectMapper;
  private final MaintenanceRecordRepository maintenanceRecordRepository;

  public TransformerController(TransformerRepository transformers,
      TransformerImageRepository images,
      FileStorageService storage,
      InspectionRepository inspectionRepository,
      FaultRegionRepository faultRegionRepository,
      DisplayMetadataRepository displayMetadataRepository,
      AnomalyDetectionService anomalyDetectionService,
      ErrorAnnotationService errorAnnotationService,
      ClassificationTrainingService classificationTrainingService,
      OriginalAnomalyResultRepository originalAnomalyResultRepository,
      ObjectMapper objectMapper,
      MaintenanceRecordRepository maintenanceRecordRepository) {
    this.transformers = transformers;
    this.images = images;
    this.storage = storage;
    this.inspectionRepository = inspectionRepository;
    this.faultRegionRepository = faultRegionRepository;
    this.displayMetadataRepository = displayMetadataRepository;
    this.anomalyDetectionService = anomalyDetectionService;
    this.errorAnnotationService = errorAnnotationService;
    this.classificationTrainingService = classificationTrainingService;
    this.originalAnomalyResultRepository = originalAnomalyResultRepository;
    this.objectMapper = objectMapper;
    this.maintenanceRecordRepository = maintenanceRecordRepository;
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
  public ResponseEntity<Void> delete(@PathVariable Long id) {

      // 1. Load transformer or 404
      Transformer t = transformers.findById(id)
              .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));

      // 2. Clean up all inspections belonging to this transformer
      List<Inspection> inspections = inspectionRepository
              .findByTransformerIdOrderByCreatedAtDesc(id);

      for (Inspection ins : inspections) {
          Long inspectionId = ins.getId();

          // 2a. Delete maintenance records linked to this inspection
          maintenanceRecordRepository.deleteByInspectionId(inspectionId);

          // 2b. Delete images linked to this inspection
          List<TransformerImage> imgs = images.findByInspectionId(inspectionId);
          for (TransformerImage img : imgs) {
              Long imgId = img.getId();

              // Delete anomaly-related data for this image
              originalAnomalyResultRepository.deleteByImageId(imgId);
              faultRegionRepository.deleteByImageId(imgId);
              displayMetadataRepository.deleteByImageId(imgId);

              // Delete any maintenance records tied specifically to this image
              maintenanceRecordRepository.deleteByMaintenanceImageId(imgId);

              // Finally delete the image itself
              images.delete(img);
          }

          // 2c. Delete the inspection row itself
          inspectionRepository.delete(ins);
      }

      // 3. Clean up any remaining images for this transformer (e.g. baseline images not tied to inspections)
      List<TransformerImage> remainingImages = images.findByTransformerIdOrderByCreatedAtDesc(id);
      for (TransformerImage img : remainingImages) {
          Long imgId = img.getId();

          originalAnomalyResultRepository.deleteByImageId(imgId);
          faultRegionRepository.deleteByImageId(imgId);
          displayMetadataRepository.deleteByImageId(imgId);
          maintenanceRecordRepository.deleteByMaintenanceImageId(imgId);

          images.delete(img);
      }

      // 4. As extra safety, delete any remaining maintenance records directly linked to this transformer
      maintenanceRecordRepository.deleteByTransformerId(id);

      // 5. Finally delete the transformer itself
      transformers.delete(t);

      return ResponseEntity.noContent().build();
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

          // Save original anomaly results for future comparison
          OriginalAnomalyResult originalResult = OriginalAnomalyResult.builder()
              .image(img)
              .anomalyJson(flaskJson)
              .build();
          originalAnomalyResultRepository.save(originalResult);

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
  public ResponseEntity<Map<String, Object>> getImageErrors(
      @PathVariable Long imageId,
      @RequestParam(value = "includeDeleted", defaultValue = "true") boolean includeDeleted) {
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

    // Filter out deleted errors unless includeDeleted is true
    List<ErrorAnnotationDTO> errorAnnotations = faultRegionEntities.stream()
        .filter(region -> includeDeleted || region.getIsDeleted() == null || !region.getIsDeleted())
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

  // Update an existing error annotation for a specific image
  @PutMapping("/images/{imageId}/errors/{errorId}")
  public ResponseEntity<Map<String, Object>> updateImageError(
      @PathVariable Long imageId,
      @PathVariable String errorId,
      @RequestBody @Valid UpdateErrorAnnotationDTO request) {

    // Validate that errorId in path matches errorId in request body
    if (!errorId.equals(request.id())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Error ID in path does not match error ID in request body");
    }

    // Update the error annotation using the service
    ErrorAnnotationDTO updatedAnnotation = errorAnnotationService.updateErrorAnnotation(imageId, errorId, request);

    // Wrap response in a "data" field as per API specification
    Map<String, Object> response = new HashMap<>();
    response.put("data", updatedAnnotation);

    return ResponseEntity.ok(response);
  }

  // Soft-delete an error annotation (mark as deleted, retain in database)
  @DeleteMapping("/images/{imageId}/errors/{errorId}")
  public ResponseEntity<Map<String, Object>> deleteImageError(
      @PathVariable Long imageId,
      @PathVariable String errorId) {

    // Delete the error annotation using the service (soft delete)
    ErrorAnnotationDTO deletedAnnotation = errorAnnotationService.deleteErrorAnnotation(imageId, errorId);

    // Wrap response in a "data" field as per API specification
    Map<String, Object> response = new HashMap<>();
    response.put("data", deletedAnnotation);

    return ResponseEntity.ok(response);
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

  // ---- Download Combined Anomaly Results ----
  /**
   * Download both original anomaly results from classification server and
   * current edited anomaly results in a single JSON file.
   * Returns a comparison showing what was originally detected vs current state.
   *
   * @param imageId The ID of the maintenance image
   * @return JSON file containing original and edited anomaly results
   */
  @GetMapping("/images/{imageId}/anomaly-comparison")
  public ResponseEntity<Map<String, Object>> downloadAnomalyComparison(@PathVariable Long imageId) {
    TransformerImage img = images.findById(imageId)
        .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));

    if (img.getImageType() != ImageType.MAINTENANCE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Anomaly comparison is only available for maintenance images");
    }

    // Get original anomaly results from classification server
    Optional<OriginalAnomalyResult> originalResultOpt = originalAnomalyResultRepository.findByImageId(imageId);

    // Get current edited anomaly results
    List<FaultRegion> currentFaultRegions = faultRegionRepository.findByImageIdOrderByRegionIdAsc(imageId);
    Optional<DisplayMetadata> currentDisplayMetadata = displayMetadataRepository.findByImageId(imageId);

    // Build response
    Map<String, Object> response = new HashMap<>();

    // Add image information
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

    // Add original results from classification server
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

    // Add current edited results
    Map<String, Object> currentResults = new HashMap<>();

    List<FaultRegionDTO> currentFaultRegionDTOs = currentFaultRegions.stream()
        .map(FaultRegionDTO::fromEntity)
        .toList();
    currentResults.put("fault_regions", currentFaultRegionDTOs);

    if (currentDisplayMetadata.isPresent()) {
      currentResults.put("display_metadata", DisplayMetadataDTO.fromEntity(currentDisplayMetadata.get()));
    }

    // Add metadata about edits
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

    // Add generation timestamp
    response.put("generatedAt", LocalDateTime.now());

    // Return as downloadable JSON
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"anomaly-comparison-image-" + imageId + ".json\"")
        .contentType(MediaType.APPLICATION_JSON)
        .body(response);
  }

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
  // ---- Delete a single inspection ----
  @DeleteMapping("/{transformerId}/inspections/{inspectionId}")
  public ResponseEntity<Void> deleteInspection(
          @PathVariable Long transformerId,
          @PathVariable Long inspectionId) {

      // 1. Check transformer exists
      Transformer t = transformers.findById(transformerId)
          .orElseThrow(() -> new NotFoundException("Transformer " + transformerId + " not found"));

      // 2. Load inspection
      Inspection ins = inspectionRepository.findById(inspectionId)
          .orElseThrow(() -> new NotFoundException("Inspection " + inspectionId + " not found"));

      // 3. Ensure it belongs to this transformer
      if (ins.getTransformer() == null ||
          !Objects.equals(ins.getTransformer().getId(), t.getId())) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Inspection does not belong to this transformer"
          );
      }

      // 4. (Optional but IMPORTANT) Clean up related data if needed:
      //    - maintenance records
      //    - maintenance images
      //    - fault regions / display metadata attached to those images

      // Example: delete maintenance records for this inspection
      maintenanceRecordRepository.deleteByInspectionId(inspectionId);

      // Example: delete images of this inspection (if you want hard delete)
      List<TransformerImage> imgs = images.findByInspectionId(inspectionId);
       for (TransformerImage img : imgs) {

            // 1) Delete anomaly results FIRST
            originalAnomalyResultRepository.deleteByImageId(img.getId());

            // 2) Delete fault regions
            faultRegionRepository.deleteByImageId(img.getId());

            // 3) Delete display metadata
            displayMetadataRepository.deleteByImageId(img.getId());

            // 4) Finally delete the image
            images.delete(img);
        }

      // 5. Finally delete inspection
      inspectionRepository.delete(ins);

      return ResponseEntity.noContent().build();
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

  // ---- Training endpoint for classification model ----
  /**
   * Train the classification model with baseline image, maintenance image,
   * current configuration, and anomaly detection results.
   * The classification server will return an updated configuration that will
   * be saved to the database and set as active.
   * 
   * @param transformerId The ID of the transformer
   * @param request       The training request containing image IDs
   * @return TrainModelResponseDTO containing training result and updated config
   *         info
   */
  @PostMapping("/{id}/train")
  public ResponseEntity<TrainModelResponseDTO> trainModel(
      @PathVariable("id") Long transformerId,
      @RequestBody @Valid TrainModelRequestDTO request) {

    // Validate that path parameter matches request body
    if (!transformerId.equals(request.transformerId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Transformer ID in path does not match transformer ID in request body");
    }

    // Validate transformer exists
    if (!transformers.existsById(transformerId)) {
      throw new NotFoundException("Transformer " + transformerId + " not found");
    }

    // Call training service
    TrainModelResponseDTO response = classificationTrainingService.trainModel(
        transformerId,
        request.baselineImageId(),
        request.maintenanceImageId());

    return ResponseEntity.ok(response);
  }
  // ---- FR4.x: Maintenance Records ----

  /**
   * FR4.1: Generate Maintenance Record Form
   *
   * Returns a form payload containing:
   *  - Transformer metadata
   *  - Inspection info
   *  - Maintenance image info (for thumbnail)
   *  - Detected anomalies for that image
   *  - Display metadata (box colors)
   *  - Allowed statuses
   *  - Existing record (if already saved for this image)
   *
   * Frontend can use this to render a combined "system-generated" + "editable" view.
   */
  @GetMapping("/{id}/maintenance-record-form")
  public ResponseEntity<MaintenanceRecordFormDTO> getMaintenanceRecordForm(
          @PathVariable Long id,
          @RequestParam(value = "inspectionId", required = false) Long inspectionId,
          @RequestParam(value = "imageId", required = false) Long imageId) {

      Transformer t = get(id); // uses existing get()

      // 1) Resolve inspection
      Inspection inspection = null;
      if (inspectionId != null) {
          inspection = inspectionRepository.findById(inspectionId)
                  .orElseThrow(() -> new NotFoundException("Inspection " + inspectionId + " not found"));
          if (inspection.getTransformer() == null ||
              !Objects.equals(inspection.getTransformer().getId(), id)) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                      "Inspection does not belong to this transformer");
          }
      }

      // 2) Resolve maintenance image
      TransformerImage maintenanceImage = null;
      if (imageId != null) {
          maintenanceImage = images.findById(imageId)
                  .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));
          if (!Objects.equals(maintenanceImage.getTransformer().getId(), id)) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                      "Image does not belong to this transformer");
          }
          if (maintenanceImage.getImageType() != ImageType.MAINTENANCE) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                      "Maintenance record form requires a MAINTENANCE image");
          }
          if (inspection == null && maintenanceImage.getInspection() != null) {
              inspection = maintenanceImage.getInspection();
          }
      } else {
        List<TransformerImage> allMaint;

        if (inspection != null) {
            // Fetch ONLY maintenance images for this inspection
            allMaint = images.findByTransformerIdAndImageTypeAndInspection_IdOrderByCreatedAtDesc(
                    id,
                    ImageType.MAINTENANCE,
                    inspection.getId()
            );
        } else {
            // Fallback: no inspection given, fetch all
            allMaint = images.findByTransformerIdAndImageTypeOrderByCreatedAtDesc(
                    id,
                    ImageType.MAINTENANCE
            );
        }

        if (!allMaint.isEmpty()) {
            maintenanceImage = allMaint.get(0);

            // If inspection wasn't resolved earlier, pick from image
            if (inspection == null) {
                inspection = maintenanceImage.getInspection();
            }
        }
      }


      if (maintenanceImage == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  "No maintenance image found or provided for this transformer");
      }

      // 3) Fetch anomalies + display metadata
      List<FaultRegion> faultRegions = faultRegionRepository
              .findByImageIdOrderByRegionIdAsc(maintenanceImage.getId());
      List<FaultRegionDTO> anomalyDTOs = faultRegions.stream()
              .map(FaultRegionDTO::fromEntity)
              .toList();

      Map<String, String> boxColors = new HashMap<>();
      Optional<DisplayMetadata> dmOpt = displayMetadataRepository.findByImageId(maintenanceImage.getId());
      if (dmOpt.isPresent() && dmOpt.get().getBoxColors() != null) {
          boxColors.putAll(dmOpt.get().getBoxColors());
      }

      // 4) Existing record (if any) for this maintenance image
      MaintenanceRecordDTO existingRecordDTO = maintenanceRecordRepository
              .findByMaintenanceImageId(maintenanceImage.getId())
              .map(MaintenanceRecordDTO::fromEntity)
              .orElse(null);

      // 5) Build DTOs
      TransformerDTO transformerDTO = new TransformerDTO(
        t.getId(),
        t.getTransformerNo(),
        t.getPoleNo(),
        t.getRegion(),
        t.getTransformerType()
    );

      InspectionDTO inspectionDTO = (inspection == null) ? null : InspectionDTO.fromEntity(inspection);

      TransformerImageDTO imageDTO = new TransformerImageDTO(
              maintenanceImage.getId(),
              maintenanceImage.getImageType(),
              maintenanceImage.getUploader(),
              maintenanceImage.getEnvCondition(),
              maintenanceImage.getFilename(),
              maintenanceImage.getCreatedAt(),
              maintenanceImage.getContentType(),
              maintenanceImage.getSizeBytes(),
              maintenanceImage.getInspection() == null ? null : maintenanceImage.getInspection().getId()
      );

      MaintenanceRecordFormDTO formDTO = new MaintenanceRecordFormDTO(
              transformerDTO,
              inspectionDTO,
              imageDTO,
              anomalyDTOs,
              boxColors,
              Arrays.asList(MaintenanceStatus.values()),
              existingRecordDTO
      );

      return ResponseEntity.ok(formDTO);
  }


  /**
   * FR4.2 / FR4.3: Save a completed maintenance record.
   *
   * Associates the record with:
   *  - transformer
   *  - inspection (optional; will use image's inspection if not provided)
   *  - maintenance image
   */
  @PostMapping("/{id}/maintenance-records")
  public ResponseEntity<MaintenanceRecordDTO> createMaintenanceRecord(
          @PathVariable Long id,
          @RequestBody @Valid CreateMaintenanceRecordDTO body) {

      if (!id.equals(body.transformerId())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  "Transformer ID in path does not match transformerId in body");
      }

      Transformer transformer = transformers.findById(id)
              .orElseThrow(() -> new NotFoundException("Transformer " + id + " not found"));

      TransformerImage image = images.findById(body.maintenanceImageId())
              .orElseThrow(() -> new NotFoundException("Image " + body.maintenanceImageId() + " not found"));

      if (!Objects.equals(image.getTransformer().getId(), id)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  "Maintenance image does not belong to this transformer");
      }

      if (image.getImageType() != ImageType.MAINTENANCE) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  "Maintenance records can only be created for MAINTENANCE images");
      }

      // Resolve inspection
      Inspection inspection = null;
      if (body.inspectionId() != null) {
          inspection = inspectionRepository.findById(body.inspectionId())
                  .orElseThrow(() -> new NotFoundException("Inspection " + body.inspectionId() + " not found"));
          if (inspection.getTransformer() == null ||
              !Objects.equals(inspection.getTransformer().getId(), id)) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                      "Inspection does not belong to this transformer");
          }
      } else if (image.getInspection() != null) {
          inspection = image.getInspection();
      }

      // Prevent duplicate record for same image (optional, but usually sensible)
      if (maintenanceRecordRepository.findByMaintenanceImageId(image.getId()).isPresent()) {
          throw new ResponseStatusException(HttpStatus.CONFLICT,
                  "A maintenance record already exists for this image. Use PUT to update.");
      }

      LocalDateTime inspectionTimestamp = body.inspectionTimestamp();
      if (inspectionTimestamp == null) {
          if (inspection != null && inspection.getCreatedAt() != null) {
              inspectionTimestamp = inspection.getCreatedAt();
          } else {
              inspectionTimestamp = image.getCreatedAt();
          }
      }

      MaintenanceRecord rec = MaintenanceRecord.builder()
              .transformer(transformer)
              .inspection(inspection)
              .maintenanceImage(image)
              .inspectionTimestamp(inspectionTimestamp)
              .inspectorName(body.inspectorName())
              .status(body.status())
              .electricalReadings(
                      body.electricalReadings() == null
                              ? new HashMap<>()
                              : new HashMap<>(body.electricalReadings()))
              .recommendedAction(body.recommendedAction())
              .additionalRemarks(body.additionalRemarks())
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();

      maintenanceRecordRepository.save(rec);

      return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceRecordDTO.fromEntity(rec));
  }
  @PutMapping("/maintenance-records/{recordId}")
  public ResponseEntity<MaintenanceRecordDTO> updateMaintenanceRecord(
          @PathVariable Long recordId,
          @RequestBody @Valid UpdateMaintenanceRecordDTO body) {

      if (!recordId.equals(body.id())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  "Record ID in path does not match id in body");
      }

      MaintenanceRecord rec = maintenanceRecordRepository.findById(recordId)
              .orElseThrow(() -> new NotFoundException("Maintenance record " + recordId + " not found"));

      // Patch-style update: only update non-null fields
      if (body.inspectionId() != null) {
          Inspection inspection = inspectionRepository.findById(body.inspectionId())
                  .orElseThrow(() -> new NotFoundException("Inspection " + body.inspectionId() + " not found"));
          // sanity check transformer match
          if (inspection.getTransformer() != null &&
                  !Objects.equals(inspection.getTransformer().getId(), rec.getTransformer().getId())) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                      "Inspection does not belong to the same transformer as this record");
          }
          rec.setInspection(inspection);
      }

      if (body.inspectionTimestamp() != null) {
          rec.setInspectionTimestamp(body.inspectionTimestamp());
      }

      if (body.inspectorName() != null) {
          rec.setInspectorName(body.inspectorName());
      }

      if (body.status() != null) {
          rec.setStatus(body.status());
      }

      if (body.electricalReadings() != null) {
          rec.setElectricalReadings(new HashMap<>(body.electricalReadings()));
      }

      if (body.recommendedAction() != null) {
          rec.setRecommendedAction(body.recommendedAction());
      }

      if (body.additionalRemarks() != null) {
          rec.setAdditionalRemarks(body.additionalRemarks());
      }

      rec.setUpdatedAt(LocalDateTime.now());
      maintenanceRecordRepository.save(rec);

      return ResponseEntity.ok(MaintenanceRecordDTO.fromEntity(rec));
  }
  /**
 * FR4.3: View all past maintenance records for a given transformer.
 */
  @GetMapping("/{id}/maintenance-records")
  public List<MaintenanceRecordDTO> listMaintenanceRecords(@PathVariable Long id) {
      if (!transformers.existsById(id)) {
          throw new NotFoundException("Transformer " + id + " not found");
      }

      return maintenanceRecordRepository
              .findByTransformerIdOrderByInspectionTimestampDesc(id)
              .stream()
              .map(MaintenanceRecordDTO::fromEntity)
              .toList();
  }

  /**
   * Optional: Fetch a single maintenance record by ID (e.g. for detail view).
   */
  @GetMapping("/maintenance-records/{recordId}")
  public ResponseEntity<MaintenanceRecordDTO> getMaintenanceRecord(@PathVariable Long recordId) {
      return maintenanceRecordRepository.findById(recordId)
              .map(rec -> ResponseEntity.ok(MaintenanceRecordDTO.fromEntity(rec)))
              .orElse(ResponseEntity.notFound().build());
  }


}
