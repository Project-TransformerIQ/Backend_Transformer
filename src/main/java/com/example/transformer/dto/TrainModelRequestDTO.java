package com.example.transformer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record TrainModelRequestDTO(
        @NotNull(message = "Transformer ID is required") @JsonProperty("transformerId") Long transformerId,

        @NotNull(message = "Baseline image ID is required") @JsonProperty("baselineImageId") Long baselineImageId,

        @NotNull(message = "Maintenance image ID is required") @JsonProperty("maintenanceImageId") Long maintenanceImageId) {
}
