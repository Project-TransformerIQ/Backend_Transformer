package com.example.transformer.dto;

import com.example.transformer.model.FaultRegion;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ErrorAnnotationDTO(
        @JsonProperty("id") String id,

        @JsonProperty("imageId") Long imageId,

        @JsonProperty("regionId") String regionId,

        @JsonProperty("cx") Double cx,

        @JsonProperty("cy") Double cy,

        @JsonProperty("w") Double w,

        @JsonProperty("h") Double h,

        @JsonProperty("status") String status,

        @JsonProperty("label") String label,

        @JsonProperty("comment") String comment,

        @JsonProperty("confidence") Double confidence,

        @JsonProperty("colorRgb") List<Integer> colorRgb,

        @JsonProperty("isManual") Boolean isManual,

        @JsonProperty("isPoint") Boolean isPoint,

        @JsonProperty("isDeleted") Boolean isDeleted,

        @JsonProperty("createdAt") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime createdAt,

        @JsonProperty("createdBy") String createdBy,

        @JsonProperty("lastModifiedAt") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime lastModifiedAt,

        @JsonProperty("lastModifiedBy") String lastModifiedBy,

        @JsonProperty("deletedAt") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime deletedAt) {
    public static ErrorAnnotationDTO fromFaultRegion(FaultRegion entity) {
        // Calculate center coordinates from bounding box
        Double cx = null;
        Double cy = null;
        Double w = null;
        Double h = null;

        if (entity.getBoundingBox() != null) {
            FaultRegion.BoundingBox bb = entity.getBoundingBox();
            if (bb.getX() != null && bb.getWidth() != null) {
                cx = bb.getX() + (bb.getWidth() / 2.0);
            }
            if (bb.getY() != null && bb.getHeight() != null) {
                cy = bb.getY() + (bb.getHeight() / 2.0);
            }
            w = bb.getWidth() != null ? bb.getWidth().doubleValue() : null;
            h = bb.getHeight() != null ? bb.getHeight().doubleValue() : null;
        } else if (entity.getCentroid() != null) {
            // Fallback to centroid if no bounding box
            cx = entity.getCentroid().getX() != null ? entity.getCentroid().getX().doubleValue() : null;
            cy = entity.getCentroid().getY() != null ? entity.getCentroid().getY().doubleValue() : null;
        }

        // Determine status based on tag or confidence
        String status = determineStatus(entity.getTag(), entity.getConfidence());

        // Use type as label
        String label = entity.getType() != null ? entity.getType() : "Unknown";

        // Generate UUID-like ID from dbId
        String id = entity.getDbId() != null
                ? UUID.nameUUIDFromBytes(("fault-region-" + entity.getDbId()).getBytes()).toString()
                : UUID.randomUUID().toString();

        // RegionId from AI detection
        String regionId = entity.getRegionId() != null ? "ai-region-" + entity.getRegionId() : null;

        // Determine if it's a point (no width/height)
        boolean isPoint = (w == null || w == 0) && (h == null || h == 0);

        // AI-generated annotations are not manual
        boolean isManual = false;

        // Get image creation time as default timestamps
        LocalDateTime createdAt = entity.getImage() != null ? entity.getImage().getCreatedAt() : LocalDateTime.now();

        return new ErrorAnnotationDTO(
                id,
                entity.getImage() != null ? entity.getImage().getId() : null,
                regionId,
                cx,
                cy,
                w,
                h,
                status,
                label,
                null, // comment - not available from AI detection
                entity.getConfidence(),
                entity.getColorRgb(),
                isManual,
                isPoint,
                false, // isDeleted
                createdAt,
                "ai-system", // createdBy
                createdAt, // lastModifiedAt (same as created for AI)
                "ai-system", // lastModifiedBy
                null // deletedAt
        );
    }

    private static String determineStatus(String tag, Double confidence) {
        if (tag != null) {
            String upperTag = tag.toUpperCase();
            if (upperTag.contains("FAULTY") || upperTag.contains("CRITICAL")) {
                return "FAULTY";
            } else if (upperTag.contains("POTENTIAL") || upperTag.contains("WARNING")) {
                return "POTENTIAL";
            } else if (upperTag.contains("NORMAL") || upperTag.contains("OK")) {
                return "NORMAL";
            }
        }

        // Fallback to confidence-based status
        if (confidence != null) {
            if (confidence >= 0.85) {
                return "FAULTY";
            } else if (confidence >= 0.5) {
                return "POTENTIAL";
            } else {
                return "NORMAL";
            }
        }

        return "POTENTIAL"; // Default
    }
}
