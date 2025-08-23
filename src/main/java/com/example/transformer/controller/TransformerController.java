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
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

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
  //   meta : application/json -> ImageUploadDTO { imageType, envCondition?, uploader? ... }
  @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public TransformerImageDTO upload(@PathVariable Long id,
                                    @RequestPart("file") MultipartFile file,
                                    @RequestPart("meta") @Valid ImageUploadDTO meta) throws IOException {
    Transformer t = get(id);

    if (meta.imageType() == ImageType.BASELINE &&
        (meta.envCondition() == null || meta.envCondition().getWeather() == null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Baseline image requires envCondition.weather (SUNNY/CLOUDY/RAINY)");
    }

    // 👇 NEW: tell storage which subfolder to use (baseline / maintenance)
    String relativePath = storage.saveTransformerImage(id, meta.imageType().name(), file);

    TransformerImage img = images.save(
        TransformerImage.builder()
            .transformer(t)
            .imageType(meta.imageType())
            .envCondition(meta.imageType() == ImageType.BASELINE ? meta.envCondition() : null)
            .uploader(meta.uploader())
            .filename(file.getOriginalFilename())
            .contentType(file.getContentType())
            .sizeBytes(file.getSize())
            .storagePath(relativePath) // 👈 store relative path from uploads/
            .build()
    );

    return new TransformerImageDTO(
        img.getId(),
        img.getImageType(),
        img.getUploader(),
        img.getEnvCondition(),
        img.getFilename(),
        img.getCreatedAt(),
        img.getContentType(),
        img.getSizeBytes()
    );
  }

  // ---- List images (DTOs) ----
  @GetMapping("/{id}/images")
  public List<TransformerImageDTO> listImages(@PathVariable Long id) {
    if (!transformers.existsById(id)) throw new NotFoundException("Transformer " + id + " not found");
    return images.findByTransformerIdOrderByCreatedAtDesc(id).stream()
        .map(img -> new TransformerImageDTO(
            img.getId(),
            img.getImageType(),
            img.getUploader(),
            img.getEnvCondition(),
            img.getFilename(),
            img.getCreatedAt(),
            img.getContentType(),
            img.getSizeBytes()
        ))
        .toList();
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
