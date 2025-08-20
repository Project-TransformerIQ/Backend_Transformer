package com.example.transformer.dto;

import jakarta.validation.constraints.*;

public record TransformerDTO(
  @NotBlank String name,
  String site,
  @PositiveOrZero Integer ratingKva,
  Double latitude,
  Double longitude
) {}
