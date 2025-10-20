package com.example.transformer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record TrainModelResponseDTO(
        @JsonProperty("status") String status,

        @JsonProperty("message") String message,

        @JsonProperty("configId") Long configId,

        @JsonProperty("configName") String configName,

        @JsonProperty("trainedAt") LocalDateTime trainedAt) {
}
