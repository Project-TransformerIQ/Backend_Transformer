package com.example.transformer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "transformer_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransformerImage {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "transformer_id")
  private Transformer transformer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ImageType imageType;             // BASELINE requires envCondition

  @Embedded
  private EnvCondition envCondition;       // nullable unless BASELINE

  @Column(nullable = false) private String uploader;

  // file metadata (we’ll implement storage next)
  private String filename;
  private String contentType;
  private Long sizeBytes;
  private String storagePath;

  @Column(name = "created_at", updatable = false, insertable = false)
  private LocalDateTime createdAt;
}
