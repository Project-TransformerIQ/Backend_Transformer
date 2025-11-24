package com.example.transformer.dto;

import com.example.transformer.model.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

public record UpdateMaintenanceRecordDTO(

        @NotNull
        Long id,   // record id

        // Optional fields to update; you can make them all nullable and do patch-like updates if you want
        Long inspectionId,
        LocalDateTime inspectionTimestamp,
        String inspectorName,
        MaintenanceStatus status,
        Map<String, String> electricalReadings,
        String recommendedAction,
        String additionalRemarks
) { }
