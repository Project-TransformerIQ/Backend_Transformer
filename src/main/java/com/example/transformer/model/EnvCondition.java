package com.example.transformer.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnvCondition {
  private Double temperatureC;         // e.g., 32.5
  private Double humidity;             // percent, e.g., 78.0
  @Enumerated(EnumType.STRING)
  private Weather weather;             // SUNNY/CLOUDY/RAINY
  private String locationNote;         // optional free text
}
