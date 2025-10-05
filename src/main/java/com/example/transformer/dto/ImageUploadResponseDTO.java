package com.example.transformer.dto;

public record ImageUploadResponseDTO(
    TransformerImageDTO imageData,
    Object anomalyDetectionResult
) {}
