package com.example.transformer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "maintenance_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to transformer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformer_id", nullable = false)
    private Transformer transformer;

    // Link to inspection (optional but recommended)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    // Link to maintenance image used for this record
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_image_id", nullable = false, unique = true)
    private TransformerImage maintenanceImage;

    // Timestamp of inspection (can be from inspection or image)
    private LocalDateTime inspectionTimestamp;

    // Engineer-editable fields
    private String inspectorName;

    @Enumerated(EnumType.STRING)
    private MaintenanceStatus status; // OK / NEEDS_MAINTENANCE / URGENT_ATTENTION

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "maintenance_record_electrical_readings",
        joinColumns = @JoinColumn(name = "maintenance_record_id")
    )
    @MapKeyColumn(name = "reading_key")
    @Column(name = "reading_value")
    private Map<String, String> electricalReadings = new HashMap<>();
    // e.g. { "voltagePhaseA": "11kV", "currentPhaseA": "120A" }

    @Column(length = 2000)
    private String recommendedAction;

    @Column(length = 2000)
    private String additionalRemarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
