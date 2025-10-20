package com.example.transformer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_detection_config")
public class AnomalyDetectionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_name", nullable = false, unique = true, length = 100)
    private String configName;

    // SSIM Configuration
    @Column(name = "ssim_weight")
    private Double ssimWeight = 0.5;

    @Column(name = "ssim_threshold")
    private Double ssimThreshold = 0.85;

    // MSE Configuration
    @Column(name = "mse_weight")
    private Double mseWeight = 0.3;

    @Column(name = "mse_threshold")
    private Double mseThreshold = 1000.0;

    // Histogram Configuration
    @Column(name = "histogram_weight")
    private Double histogramWeight = 0.2;

    @Column(name = "histogram_threshold")
    private Double histogramThreshold = 0.7;

    // Combined Threshold
    @Column(name = "combined_threshold")
    private Double combinedThreshold = 0.75;

    // Image Processing Configuration
    @Column(name = "resize_width")
    private Integer resizeWidth = 800;

    @Column(name = "resize_height")
    private Integer resizeHeight = 600;

    @Column(name = "blur_kernel_size")
    private Integer blurKernelSize = 5;

    // Detection Configuration
    @Column(name = "min_contour_area")
    private Integer minContourArea = 100;

    @Column(name = "dilation_iterations")
    private Integer dilationIterations = 2;

    @Column(name = "erosion_iterations")
    private Integer erosionIterations = 1;

    // Active flag
    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "description", length = 500)
    private String description;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public AnomalyDetectionConfig() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public Double getSsimWeight() {
        return ssimWeight;
    }

    public void setSsimWeight(Double ssimWeight) {
        this.ssimWeight = ssimWeight;
    }

    public Double getSsimThreshold() {
        return ssimThreshold;
    }

    public void setSsimThreshold(Double ssimThreshold) {
        this.ssimThreshold = ssimThreshold;
    }

    public Double getMseWeight() {
        return mseWeight;
    }

    public void setMseWeight(Double mseWeight) {
        this.mseWeight = mseWeight;
    }

    public Double getMseThreshold() {
        return mseThreshold;
    }

    public void setMseThreshold(Double mseThreshold) {
        this.mseThreshold = mseThreshold;
    }

    public Double getHistogramWeight() {
        return histogramWeight;
    }

    public void setHistogramWeight(Double histogramWeight) {
        this.histogramWeight = histogramWeight;
    }

    public Double getHistogramThreshold() {
        return histogramThreshold;
    }

    public void setHistogramThreshold(Double histogramThreshold) {
        this.histogramThreshold = histogramThreshold;
    }

    public Double getCombinedThreshold() {
        return combinedThreshold;
    }

    public void setCombinedThreshold(Double combinedThreshold) {
        this.combinedThreshold = combinedThreshold;
    }

    public Integer getResizeWidth() {
        return resizeWidth;
    }

    public void setResizeWidth(Integer resizeWidth) {
        this.resizeWidth = resizeWidth;
    }

    public Integer getResizeHeight() {
        return resizeHeight;
    }

    public void setResizeHeight(Integer resizeHeight) {
        this.resizeHeight = resizeHeight;
    }

    public Integer getBlurKernelSize() {
        return blurKernelSize;
    }

    public void setBlurKernelSize(Integer blurKernelSize) {
        this.blurKernelSize = blurKernelSize;
    }

    public Integer getMinContourArea() {
        return minContourArea;
    }

    public void setMinContourArea(Integer minContourArea) {
        this.minContourArea = minContourArea;
    }

    public Integer getDilationIterations() {
        return dilationIterations;
    }

    public void setDilationIterations(Integer dilationIterations) {
        this.dilationIterations = dilationIterations;
    }

    public Integer getErosionIterations() {
        return erosionIterations;
    }

    public void setErosionIterations(Integer erosionIterations) {
        this.erosionIterations = erosionIterations;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
