
package com.example.transformer.dto;

import com.example.transformer.model.InspectionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInspectionDTO(
  @NotBlank String title,
  @NotBlank String inspector,
  String notes,
  @NotNull InspectionStatus status // OPEN | IN_PROGRESS | CLOSED
) {}
