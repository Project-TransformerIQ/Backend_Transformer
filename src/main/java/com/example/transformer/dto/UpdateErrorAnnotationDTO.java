package com.example.transformer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateErrorAnnotationDTO(
        @JsonProperty("id") @NotNull(message = "id is required") String id,

        @JsonProperty("cx") Double cx,

        @JsonProperty("cy") Double cy,

        @JsonProperty("w") Double w,

        @JsonProperty("h") Double h,

        @JsonProperty("status") String status,

        @JsonProperty("label") String label,

        @JsonProperty("comment") String comment,

        @JsonProperty("confidence") Double confidence,

        @JsonProperty("colorRgb") List<Integer> colorRgb,

        @JsonProperty("lastModifiedBy") String lastModifiedBy,

        @JsonProperty("lastModifiedAt") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime lastModifiedAt) {
}
