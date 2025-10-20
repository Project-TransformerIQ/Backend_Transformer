package com.example.transformer.service;

import com.example.transformer.dto.CreateErrorAnnotationDTO;
import com.example.transformer.dto.ErrorAnnotationDTO;
import com.example.transformer.dto.UpdateErrorAnnotationDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.FaultRegion;
import com.example.transformer.model.TransformerImage;
import com.example.transformer.repository.FaultRegionRepository;
import com.example.transformer.repository.TransformerImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ErrorAnnotationService {

    private final FaultRegionRepository faultRegionRepository;
    private final TransformerImageRepository imageRepository;

    public ErrorAnnotationService(FaultRegionRepository faultRegionRepository,
            TransformerImageRepository imageRepository) {
        this.faultRegionRepository = faultRegionRepository;
        this.imageRepository = imageRepository;
    }

    @Transactional
    public ErrorAnnotationDTO createErrorAnnotation(CreateErrorAnnotationDTO dto) {
        // Validate that the image exists
        TransformerImage image = imageRepository.findById(dto.imageId())
                .orElseThrow(() -> new NotFoundException("Image with id " + dto.imageId() + " not found"));

        // Create FaultRegion entity from the DTO
        FaultRegion faultRegion = new FaultRegion();

        // Calculate bounding box from center coordinates
        FaultRegion.BoundingBox boundingBox = new FaultRegion.BoundingBox();
        if (dto.cx() != null && dto.w() != null) {
            boundingBox.setX((int) Math.round(dto.cx() - (dto.w() / 2.0)));
        }
        if (dto.cy() != null && dto.h() != null) {
            boundingBox.setY((int) Math.round(dto.cy() - (dto.h() / 2.0)));
        }
        if (dto.w() != null) {
            boundingBox.setWidth(dto.w().intValue());
        }
        if (dto.h() != null) {
            boundingBox.setHeight(dto.h().intValue());
        }
        if (dto.w() != null && dto.h() != null) {
            boundingBox.setAreaPx((int) Math.round(dto.w() * dto.h()));
        }
        faultRegion.setBoundingBox(boundingBox);

        // Set centroid
        FaultRegion.Centroid centroid = new FaultRegion.Centroid();
        centroid.setX(dto.cx() != null ? dto.cx().intValue() : null);
        centroid.setY(dto.cy() != null ? dto.cy().intValue() : null);
        faultRegion.setCentroid(centroid);

        // Set other properties
        faultRegion.setType(dto.label());
        faultRegion.setTag(dto.status());
        faultRegion.setConfidence(dto.confidence());
        faultRegion.setColorRgb(dto.colorRgb());
        faultRegion.setImage(image);

        // Set manual annotation fields
        faultRegion.setComment(dto.comment());
        faultRegion.setIsManual(dto.isManual() != null ? dto.isManual() : true);
        faultRegion.setCreatedAt(dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now());
        faultRegion.setCreatedBy(dto.createdBy() != null ? dto.createdBy() : "unknown");

        // Save to database
        FaultRegion savedRegion = faultRegionRepository.save(faultRegion);

        // Build response DTO
        String id = UUID.nameUUIDFromBytes(("fault-region-" + savedRegion.getDbId()).getBytes()).toString();
        String regionId = savedRegion.getRegionId() != null ? "ai-region-" + savedRegion.getRegionId() : null;

        boolean isPoint = (dto.w() == null || dto.w() == 0) && (dto.h() == null || dto.h() == 0);

        return new ErrorAnnotationDTO(
                id,
                savedRegion.getImage().getId(),
                regionId,
                dto.cx(),
                dto.cy(),
                dto.w(),
                dto.h(),
                savedRegion.getTag(),
                savedRegion.getType(),
                savedRegion.getComment(),
                savedRegion.getConfidence(),
                savedRegion.getColorRgb(),
                savedRegion.getIsManual(),
                isPoint,
                false, // isDeleted
                savedRegion.getCreatedAt(),
                savedRegion.getCreatedBy(),
                savedRegion.getLastModifiedAt(),
                savedRegion.getLastModifiedBy(),
                null // deletedAt
        );
    }

    @Transactional
    public ErrorAnnotationDTO updateErrorAnnotation(Long imageId, String errorId, UpdateErrorAnnotationDTO dto) {
        // Validate that the image exists
        imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image with id " + imageId + " not found"));

        // Find the fault region by extracting dbId from the UUID
        // The UUID is generated from "fault-region-{dbId}", so we need to find by
        // imageId and match UUID
        java.util.List<FaultRegion> regions = faultRegionRepository.findByImageId(imageId);

        FaultRegion faultRegion = regions.stream()
                .filter(region -> {
                    String regionUuid = UUID.nameUUIDFromBytes(("fault-region-" + region.getDbId()).getBytes())
                            .toString();
                    return regionUuid.equals(errorId);
                })
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Error annotation with id " + errorId + " not found for image " + imageId));

        // Update bounding box if coordinates are provided
        if (dto.cx() != null || dto.cy() != null || dto.w() != null || dto.h() != null) {
            FaultRegion.BoundingBox boundingBox = faultRegion.getBoundingBox();
            if (boundingBox == null) {
                boundingBox = new FaultRegion.BoundingBox();
            }

            // Update bounding box coordinates
            if (dto.cx() != null && dto.w() != null) {
                boundingBox.setX((int) Math.round(dto.cx() - (dto.w() / 2.0)));
                boundingBox.setWidth(dto.w().intValue());
            }
            if (dto.cy() != null && dto.h() != null) {
                boundingBox.setY((int) Math.round(dto.cy() - (dto.h() / 2.0)));
                boundingBox.setHeight(dto.h().intValue());
            }
            if (dto.w() != null && dto.h() != null) {
                boundingBox.setAreaPx((int) Math.round(dto.w() * dto.h()));
            }
            faultRegion.setBoundingBox(boundingBox);

            // Update centroid
            FaultRegion.Centroid centroid = faultRegion.getCentroid();
            if (centroid == null) {
                centroid = new FaultRegion.Centroid();
            }
            if (dto.cx() != null) {
                centroid.setX(dto.cx().intValue());
            }
            if (dto.cy() != null) {
                centroid.setY(dto.cy().intValue());
            }
            faultRegion.setCentroid(centroid);
        }

        // Update other fields if provided
        if (dto.label() != null) {
            faultRegion.setType(dto.label());
        }
        if (dto.status() != null) {
            faultRegion.setTag(dto.status());
        }
        if (dto.confidence() != null) {
            faultRegion.setConfidence(dto.confidence());
        }
        if (dto.colorRgb() != null) {
            faultRegion.setColorRgb(dto.colorRgb());
        }
        if (dto.comment() != null) {
            faultRegion.setComment(dto.comment());
        }

        // Update modification tracking
        faultRegion.setLastModifiedAt(dto.lastModifiedAt() != null ? dto.lastModifiedAt() : LocalDateTime.now());
        faultRegion.setLastModifiedBy(dto.lastModifiedBy() != null ? dto.lastModifiedBy() : "unknown");

        // Mark as manual if it's being updated
        if (faultRegion.getIsManual() == null || !faultRegion.getIsManual()) {
            faultRegion.setIsManual(true);
        }

        // Save to database
        FaultRegion updatedRegion = faultRegionRepository.save(faultRegion);

        // Build response using the fromFaultRegion method
        return ErrorAnnotationDTO.fromFaultRegion(updatedRegion);
    }

    @Transactional
    public ErrorAnnotationDTO deleteErrorAnnotation(Long imageId, String errorId) {
        // Validate that the image exists
        imageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image with id " + imageId + " not found"));

        // Find the fault region by extracting dbId from the UUID
        // The UUID is generated from "fault-region-{dbId}", so we need to find by
        // imageId and match UUID
        java.util.List<FaultRegion> regions = faultRegionRepository.findByImageId(imageId);

        FaultRegion faultRegion = regions.stream()
                .filter(region -> {
                    String regionUuid = UUID.nameUUIDFromBytes(("fault-region-" + region.getDbId()).getBytes())
                            .toString();
                    return regionUuid.equals(errorId);
                })
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Error annotation with id " + errorId + " not found for image " + imageId));

        // Check if already deleted
        if (faultRegion.getIsDeleted() != null && faultRegion.getIsDeleted()) {
            throw new IllegalStateException("Error annotation with id " + errorId + " is already deleted");
        }

        // Soft delete: mark as deleted and set deletion timestamp
        faultRegion.setIsDeleted(true);
        faultRegion.setDeletedAt(LocalDateTime.now());

        // Save to database
        FaultRegion deletedRegion = faultRegionRepository.save(faultRegion);

        // Build response using the fromFaultRegion method
        return ErrorAnnotationDTO.fromFaultRegion(deletedRegion);
    }
}
