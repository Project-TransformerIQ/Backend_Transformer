package com.example.transformer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record CreateErrorAnnotationDTO(
        @JsonProperty("imageId")
        @NotNull(message = "imageId is required")
        Long imageId,

        @JsonProperty("cx")
        @NotNull(message = "cx is required")
        Double cx,

        @JsonProperty("cy")
        @NotNull(message = "cy is required")
        Double cy,

        @JsonProperty("w")
        Double w,

        @JsonProperty("h")
        Double h,

        @JsonProperty("status")
        String status,

        @JsonProperty("label")
        String label,

        @JsonProperty("comment")
        String comment,

        @JsonProperty("confidence")
        Double confidence,

        @JsonProperty("colorRgb")
        List<Integer> colorRgb,

        @JsonProperty("isManual")
        Boolean isManual,

        @JsonProperty("createdBy")
        String createdBy,

        @JsonProperty("createdAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        LocalDateTime createdAt
) {
}

