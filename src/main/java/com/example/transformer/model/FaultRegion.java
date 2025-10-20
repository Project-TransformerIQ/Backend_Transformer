package com.example.transformer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "fault_regions")
public class FaultRegion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dbId;

    private Integer regionId;
    private String type;
    private String dominantColor;

    @ElementCollection
    @CollectionTable(name = "fault_region_colors", joinColumns = @JoinColumn(name = "fault_region_id"))
    @Column(name = "color_value")
    private List<Integer> colorRgb;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "bbox_x")),
            @AttributeOverride(name = "y", column = @Column(name = "bbox_y")),
            @AttributeOverride(name = "width", column = @Column(name = "bbox_width")),
            @AttributeOverride(name = "height", column = @Column(name = "bbox_height")),
            @AttributeOverride(name = "areaPx", column = @Column(name = "bbox_area_px"))
    })
    private BoundingBox boundingBox;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "centroid_x")),
            @AttributeOverride(name = "y", column = @Column(name = "centroid_y"))
    })
    private Centroid centroid;

    private Double aspectRatio;
    private Boolean elongated;
    private Boolean connectedToWire;
    private String tag;
    private Double confidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private TransformerImage image;

    // Manual annotation fields
    private String comment;
    private Boolean isManual;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Constructors
    public FaultRegion() {
    }

    // Getters and Setters
    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public Integer getRegionId() {
        return regionId;
    }

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDominantColor() {
        return dominantColor;
    }

    public void setDominantColor(String dominantColor) {
        this.dominantColor = dominantColor;
    }

    public List<Integer> getColorRgb() {
        return colorRgb;
    }

    public void setColorRgb(List<Integer> colorRgb) {
        this.colorRgb = colorRgb;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public Centroid getCentroid() {
        return centroid;
    }

    public void setCentroid(Centroid centroid) {
        this.centroid = centroid;
    }

    public Double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(Double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Boolean getElongated() {
        return elongated;
    }

    public void setElongated(Boolean elongated) {
        this.elongated = elongated;
    }

    public Boolean getConnectedToWire() {
        return connectedToWire;
    }

    public void setConnectedToWire(Boolean connectedToWire) {
        this.connectedToWire = connectedToWire;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public TransformerImage getImage() {
        return image;
    }

    public void setImage(TransformerImage image) {
        this.image = image;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getIsManual() {
        return isManual;
    }

    public void setIsManual(Boolean isManual) {
        this.isManual = isManual;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Embeddable
    public static class BoundingBox {
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;
        private Integer areaPx;

        public BoundingBox() {
        }

        public Integer getX() {
            return x;
        }

        public void setX(Integer x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public Integer getAreaPx() {
            return areaPx;
        }

        public void setAreaPx(Integer areaPx) {
            this.areaPx = areaPx;
        }
    }

    @Embeddable
    public static class Centroid {
        private Integer x;
        private Integer y;

        public Centroid() {
        }

        public Integer getX() {
            return x;
        }

        public void setX(Integer x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }
    }
}
