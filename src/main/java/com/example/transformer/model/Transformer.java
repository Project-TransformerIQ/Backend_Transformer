package com.example.transformer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "transformers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transformer {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false) private String name;     // e.g., "TX-101"
  private String site;                               // substation / feeder note
  private Integer ratingKva;                         // nameplate rating
  private Double latitude;
  private Double longitude;

  @Column(name = "created_at", updatable = false, insertable = false)
  private LocalDateTime createdAt;
}
