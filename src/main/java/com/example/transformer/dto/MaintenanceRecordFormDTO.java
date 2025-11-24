package com.example.transformer.dto;

import com.example.transformer.model.MaintenanceStatus;

import java.util.List;
import java.util.Map;

public record MaintenanceRecordFormDTO(
        TransformerDTO transformer,
        InspectionDTO inspection,
        TransformerImageDTO maintenanceImage,
        List<FaultRegionDTO> anomalies,
        Map<String, String> displayBoxColors,
        List<MaintenanceStatus> allowedStatuses,
        MaintenanceRecordDTO existingRecord
) { }
