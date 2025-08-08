package com.example.transformer.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnvCondition {
  private Double temperatureC;         
  private Double humidity;             
  @Enumerated(EnumType.STRING)
  private Weather weather;             // SUNNY/CLOUDY/RAINY
  private String locationNote;         // optional free text
}
