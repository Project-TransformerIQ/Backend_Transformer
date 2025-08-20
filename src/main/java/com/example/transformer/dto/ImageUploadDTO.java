package com.example.transformer.dto;

import com.example.transformer.model.EnvCondition;
import com.example.transformer.model.ImageType;
import jakarta.validation.constraints.*;

public record ImageUploadDTO(
  @NotNull ImageType imageType,     // BASELINE or MAINTENANCE
  EnvCondition envCondition,        // REQUIRED iff imageType == BASELINE (weather must be set)
  @NotBlank String uploader
) {}
