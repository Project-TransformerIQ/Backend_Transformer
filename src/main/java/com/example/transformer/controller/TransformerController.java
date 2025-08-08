package com.example.transformer.controller;

import com.example.transformer.dto.ImageUploadDTO;
import com.example.transformer.dto.TransformerDTO;
import com.example.transformer.dto.TransformerImageDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.ImageType;
import com.example.transformer.model.Transformer;
import com.example.transformer.model.TransformerImage;
import com.example.transformer.repository.TransformerImageRepository;
import com.example.transformer.repository.TransformerRepository;
import com.example.transformer.service.FileStorageService;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/transformers")
public class TransformerController {

  private final TransformerRepository transformers;
  private final TransformerImageRepository images;
  private final FileStorageService storage;
  private final InspectionRepository inspectionRepository;

  public TransformerController(TransformerRepository transformers,
                               TransformerImageRepository images,
                               FileStorageService storage,
                               InspectionRepository inspectionRepository) {
    this.transformers = transformers;
    this.images = images;
    this.storage = storage;
    this.inspectionRepository = inspectionRepository;
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
  public List<Transformer> list() { return transformers.findAll(); }

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
    if (!transformers.existsById(id)) throw new NotFoundException("Transformer " + id + " not found");
    transformers.deleteById(id);
  }

  // ---- Upload image (baseline/maintenance) ----
  // Expect multipart with parts:
  //   file : binary
  //   meta : application/json -> ImageUploadDTO { imageType, envCondition?, uploader? }
  // For MAINTENANCE: pass inspectionId as query param or form field.
  @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public TransformerImageDTO upload(@PathVariable Long id,
                                    @RequestPart("file") MultipartFile file,
                                    @RequestPart("meta") @Valid ImageUploadDTO meta,
                                    @RequestParam(value = "inspectionId", required = false) Long inspectionId
  ) throws IOException {
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

    // Persist file under /uploads/transformers/{id}/{baseline|maintenance}/TS-filename
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

    return new TransformerImageDTO(
        img.getId(),
        img.getImageType(),
        img.getUploader(),
        img.getEnvCondition(),
        img.getFilename(),
        img.getCreatedAt(),
        img.getContentType(),
        img.getSizeBytes(),
        img.getInspection() == null ? null : img.getInspection().getId()
    );
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
          (img.getInspection() == null ? null : img.getInspection().getId())
      ));
    }
    return out;
  }

  // ---- Serve raw file for thumbnails ----
  @GetMapping("/images/{imageId}/raw")
  public ResponseEntity<?> raw(@PathVariable Long imageId) {
    TransformerImage img = images.findById(imageId)
        .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));
    var res = storage.load(img.getStoragePath());
    if (!res.exists()) throw new NotFoundException("File missing on disk");

    MediaType mt = Optional.ofNullable(img.getContentType())
        .map(MediaType::parseMediaType)
        .orElse(MediaType.APPLICATION_OCTET_STREAM);

    return ResponseEntity.ok()
        .contentType(mt)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + img.getFilename() + "\"")
        .body(res);
  }

  // ---- Inspections ----
  @GetMapping("/{id}/inspections")
  public List<InspectionDTO> listInspections(@PathVariable Long id) {
    if (!transformers.existsById(id)) throw new NotFoundException("Transformer " + id + " not found");
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
}