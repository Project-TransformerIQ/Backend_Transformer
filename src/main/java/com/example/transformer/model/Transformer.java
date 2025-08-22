package com.example.transformer.model;

import com.example.transformer.model.TransformerType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;




@Entity @Table(name = "transformers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transformer {
  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String transformerNo;

  @Column(nullable = false)
  private String poleNo;

  @Column(nullable = false)
  private String region;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransformerType transformerType;

  @Column(name = "created_at", updatable = false, insertable = false)
  private LocalDateTime createdAt;
}
