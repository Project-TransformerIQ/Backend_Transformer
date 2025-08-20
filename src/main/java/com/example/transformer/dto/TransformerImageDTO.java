package com.example.transformer.dto;

import com.example.transformer.model.EnvCondition;
import com.example.transformer.model.ImageType;
import java.time.LocalDateTime;

public record TransformerImageDTO(
    Long id,
    ImageType imageType,
    String uploader,
    EnvCondition envCondition,
    String filename,
    java.time.LocalDateTime createdAt,
    String contentType,     // <- add
    Long sizeBytes          // <- add
) {}
