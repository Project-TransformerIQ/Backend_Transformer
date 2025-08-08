
package com.example.transformer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "inspections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inspection {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "transformer_id", nullable = false)
  private Transformer transformer;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String inspector;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InspectionStatus status;

  @Column(nullable = false)
  private LocalDateTime createdAt;
}
