package com.example.transformer.controller;

import com.example.transformer.dto.CreateErrorAnnotationDTO;
import com.example.transformer.dto.ErrorAnnotationDTO;
import com.example.transformer.dto.UpdateErrorAnnotationDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.FaultRegion;
import com.example.transformer.model.ImageType;
import com.example.transformer.model.TransformerImage;
import com.example.transformer.repository.FaultRegionRepository;
import com.example.transformer.repository.TransformerImageRepository;
import com.example.transformer.service.ErrorAnnotationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/transformers")
public class ImageErrorAnnotationController {

    private final TransformerImageRepository images;
    private final FaultRegionRepository faultRegionRepository;
    private final ErrorAnnotationService errorAnnotationService;

    public ImageErrorAnnotationController(TransformerImageRepository images,
                                          FaultRegionRepository faultRegionRepository,
                                          ErrorAnnotationService errorAnnotationService) {
        this.images = images;
        this.faultRegionRepository = faultRegionRepository;
        this.errorAnnotationService = errorAnnotationService;
    }

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
            Map<String, Object> result = new HashMap<>();
            result.put("data", new ArrayList<>());
            return ResponseEntity.ok(result);
        }

        List<ErrorAnnotationDTO> errorAnnotations = faultRegionEntities.stream()
                .filter(region -> includeDeleted || region.getIsDeleted() == null || !region.getIsDeleted())
                .map(ErrorAnnotationDTO::fromFaultRegion)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("data", errorAnnotations);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/images/{imageId}/errors")
    public ResponseEntity<Map<String, Object>> createImageError(
            @PathVariable Long imageId,
            @RequestBody @Valid CreateErrorAnnotationDTO request) {

        if (!imageId.equals(request.imageId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image ID in path does not match image ID in request body");
        }

        ErrorAnnotationDTO createdAnnotation = errorAnnotationService.createErrorAnnotation(request);

        Map<String, Object> response = new HashMap<>();
        response.put("data", createdAnnotation);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/images/{imageId}/errors/{errorId}")
    public ResponseEntity<Map<String, Object>> updateImageError(
            @PathVariable Long imageId,
            @PathVariable String errorId,
            @RequestBody @Valid UpdateErrorAnnotationDTO request) {

        if (!errorId.equals(request.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Error ID in path does not match error ID in request body");
        }

        ErrorAnnotationDTO updatedAnnotation = errorAnnotationService.updateErrorAnnotation(imageId, errorId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("data", updatedAnnotation);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/images/{imageId}/errors/{errorId}")
    public ResponseEntity<Map<String, Object>> deleteImageError(
            @PathVariable Long imageId,
            @PathVariable String errorId) {

        ErrorAnnotationDTO deletedAnnotation = errorAnnotationService.deleteErrorAnnotation(imageId, errorId);

        Map<String, Object> response = new HashMap<>();
        response.put("data", deletedAnnotation);

        return ResponseEntity.ok(response);
    }
}
