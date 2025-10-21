package com.example.transformer.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "original_anomaly_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OriginalAnomalyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", unique = true, nullable = false)
    private TransformerImage image;

    @Column(name = "anomaly_json", columnDefinition = "TEXT")
    private String anomalyJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

