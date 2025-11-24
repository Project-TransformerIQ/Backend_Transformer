package com.example.transformer.dto;

import com.example.transformer.model.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

public record CreateMaintenanceRecordDTO(

        @NotNull
        Long transformerId,

        // Can be null; will default from image if missing
        Long inspectionId,

        @NotNull
        Long maintenanceImageId,

        // Optional override, else image/inspection timestamp used
        LocalDateTime inspectionTimestamp,

        String inspectorName,

        @NotNull
        MaintenanceStatus status,

        // Key-value readings: "voltagePhaseA" -> "11kV", etc.
        Map<String, String> electricalReadings,

        String recommendedAction,
        String additionalRemarks
) { }
