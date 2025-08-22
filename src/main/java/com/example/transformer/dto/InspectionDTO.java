// src/main/java/com/example/transformer/dto/InspectionDTO.java
package com.example.transformer.dto;

import com.example.transformer.model.Inspection;
import com.example.transformer.model.InspectionStatus;
import java.time.LocalDateTime;

public record InspectionDTO(
  Long id,
  String title,
  String inspector,
  String notes,
  InspectionStatus status,
  LocalDateTime createdAt
) {
  public static InspectionDTO fromEntity(Inspection e) {
    return new InspectionDTO(
      e.getId(), e.getTitle(), e.getInspector(),
      e.getNotes(), e.getStatus(), e.getCreatedAt()
    );
  }
}
