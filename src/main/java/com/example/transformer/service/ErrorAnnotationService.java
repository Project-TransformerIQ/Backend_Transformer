package com.example.transformer.service;

import com.example.transformer.dto.CreateErrorAnnotationDTO;
import com.example.transformer.dto.ErrorAnnotationDTO;
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
                null, // lastModifiedAt
                null, // lastModifiedBy
                null  // deletedAt
        );
    }
}
