package com.example.transformer.dto;

import com.example.transformer.model.EnvCondition;
import com.example.transformer.model.ImageType;
import jakarta.validation.constraints.*;

public record ImageUploadDTO(
  @NotNull ImageType imageType,     
  EnvCondition envCondition,        
  @NotBlank String uploader,
  Long inspectionId
) {}
