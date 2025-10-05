package com.example.transformer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "display_metadata")
public class DisplayMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(name = "display_box_colors", joinColumns = @JoinColumn(name = "display_metadata_id"))
    @MapKeyColumn(name = "color_key")
    @Column(name = "color_value")
    private Map<String, String> boxColors; // store RGB as comma-separated string

    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private TransformerImage image;

    // Constructors
    public DisplayMetadata() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Map<String, String> getBoxColors() { return boxColors; }
    public void setBoxColors(Map<String, String> boxColors) { this.boxColors = boxColors; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public TransformerImage getImage() { return image; }
    public void setImage(TransformerImage image) { this.image = image; }
}
