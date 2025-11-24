package com.example.transformer.dto;

import com.example.transformer.model.MaintenanceRecord;
import com.example.transformer.model.MaintenanceStatus;

import java.time.LocalDateTime;
import java.util.Map;

public record MaintenanceRecordDTO(
        Long id,
        Long transformerId,
        Long inspectionId,
        Long maintenanceImageId,
        LocalDateTime inspectionTimestamp,
        String inspectorName,
        MaintenanceStatus status,
        Map<String, String> electricalReadings,
        String recommendedAction,
        String additionalRemarks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MaintenanceRecordDTO fromEntity(MaintenanceRecord rec) {
        return new MaintenanceRecordDTO(
            rec.getId(),
            rec.getTransformer() != null ? rec.getTransformer().getId() : null,
            rec.getInspection() != null ? rec.getInspection().getId() : null,
            rec.getMaintenanceImage() != null ? rec.getMaintenanceImage().getId() : null,
            rec.getInspectionTimestamp(),
            rec.getInspectorName(),
            rec.getStatus(),
            rec.getElectricalReadings(),
            rec.getRecommendedAction(),
            rec.getAdditionalRemarks(),
            rec.getCreatedAt(),
            rec.getUpdatedAt()
        );
    }
}
