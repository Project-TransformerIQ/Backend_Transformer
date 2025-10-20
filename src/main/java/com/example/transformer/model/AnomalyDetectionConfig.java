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

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === ΔV (brightness) sensitivity ===
    @Column(name = "delta_k_sigma")
    private Double deltaKSigma = 0.28;

    @Column(name = "delta_abs_min")
    private Integer deltaAbsMin = 5;

    // === Morphology / cleanup ===
    @Column(name = "min_blob_area_px")
    private Integer minBlobAreaPx = 24;

    @Column(name = "open_iters")
    private Integer openIters = 1;

    @Column(name = "dilate_iters")
    private Integer dilateIters = 2;

    @Column(name = "keep_component_min_ratio")
    private Double keepComponentMinRatio = 8e-6;

    // === Decision thresholds ===
    @Column(name = "fault_red_ratio")
    private Double faultRedRatio = 7e-5;

    @Column(name = "fault_red_min_pixels")
    private Integer faultRedMinPixels = 100;

    @Column(name = "potential_yellow_ratio")
    private Double potentialYellowRatio = 1.2e-4;

    @Column(name = "fullwire_hot_fraction")
    private Double fullwireHotFraction = 0.05;

    // === Wire-like heuristic ===
    @Column(name = "elongated_aspect_ratio")
    private Double elongatedAspectRatio = 3.0;

    // === Cluster nearby detections ===
    @Column(name = "merge_close_frac")
    private Double mergeCloseFrac = 0.02;

    @Column(name = "min_cluster_area_px")
    private Integer minClusterAreaPx = 100;

    // === AUTO SIDEBAR (colorbar) detection ===
    @Column(name = "sidebar_search_frac")
    private Double sidebarSearchFrac = 0.25;

    @Column(name = "sidebar_min_width_frac")
    private Double sidebarMinWidthFrac = 0.02;

    @Column(name = "sidebar_max_width_frac")
    private Double sidebarMaxWidthFrac = 0.18;

    @Column(name = "sidebar_min_valid_frac")
    private Double sidebarMinValidFrac = 0.45;

    @Column(name = "sidebar_hue_span_deg")
    private Integer sidebarHueSpanDeg = 80;

    @Column(name = "sidebar_margin_px")
    private Integer sidebarMarginPx = 2;

    // === Overlay masking ===
    @Column(name = "text_bottom_band_frac")
    private Double textBottomBandFrac = 0.16;

    @Column(name = "mask_top_left_overlay")
    private Boolean maskTopLeftOverlay = true;

    @Column(name = "top_left_box", length = 100)
    private String topLeftBox = "0.0,0.0,0.5,0.12";

    // === Histogram / background deltas ===
    @Column(name = "h_bins")
    private Integer hBins = 36;

    @Column(name = "hist_distance_min")
    private Double histDistanceMin = 0.06;

    @Column(name = "red_bg_ratio_min_increase")
    private Double redBgRatioMinIncrease = 0.15;

    @Column(name = "red_bg_min_abs")
    private Double redBgMinAbs = 0.001;

    @Column(name = "roi_s_min")
    private Integer roiSMin = 40;

    @Column(name = "roi_v_min")
    private Integer roiVMin = 35;

    // === Background (blue/black) ===
    @Column(name = "blue_h_lo")
    private Integer blueHLo = 90;

    @Column(name = "blue_h_hi")
    private Integer blueHHi = 140;

    @Column(name = "blue_s_min")
    private Integer blueSMin = 40;

    @Column(name = "blue_v_min")
    private Integer blueVMin = 30;

    @Column(name = "black_v_hi")
    private Integer blackVHi = 55;

    // === WHITE BACKGROUND (NEW) ===
    @Column(name = "white_bg_S_max")
    private Integer whiteBgSMax = 35;

    @Column(name = "white_bg_V_min")
    private Integer whiteBgVMin = 245;

    @Column(name = "white_bg_exclude_near_warm_px")
    private Integer whiteBgExcludeNearWarmPx = 9;

    @Column(name = "white_bg_column_frac")
    private Double whiteBgColumnFrac = 0.92;

    @Column(name = "white_bg_row_frac")
    private Double whiteBgRowFrac = 0.92;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Double getDeltaKSigma() {
        return deltaKSigma;
    }

    public void setDeltaKSigma(Double deltaKSigma) {
        this.deltaKSigma = deltaKSigma;
    }

    public Integer getDeltaAbsMin() {
        return deltaAbsMin;
    }

    public void setDeltaAbsMin(Integer deltaAbsMin) {
        this.deltaAbsMin = deltaAbsMin;
    }

    public Integer getMinBlobAreaPx() {
        return minBlobAreaPx;
    }

    public void setMinBlobAreaPx(Integer minBlobAreaPx) {
        this.minBlobAreaPx = minBlobAreaPx;
    }

    public Integer getOpenIters() {
        return openIters;
    }

    public void setOpenIters(Integer openIters) {
        this.openIters = openIters;
    }

    public Integer getDilateIters() {
        return dilateIters;
    }

    public void setDilateIters(Integer dilateIters) {
        this.dilateIters = dilateIters;
    }

    public Double getKeepComponentMinRatio() {
        return keepComponentMinRatio;
    }

    public void setKeepComponentMinRatio(Double keepComponentMinRatio) {
        this.keepComponentMinRatio = keepComponentMinRatio;
    }

    public Double getFaultRedRatio() {
        return faultRedRatio;
    }

    public void setFaultRedRatio(Double faultRedRatio) {
        this.faultRedRatio = faultRedRatio;
    }

    public Integer getFaultRedMinPixels() {
        return faultRedMinPixels;
    }

    public void setFaultRedMinPixels(Integer faultRedMinPixels) {
        this.faultRedMinPixels = faultRedMinPixels;
    }

    public Double getPotentialYellowRatio() {
        return potentialYellowRatio;
    }

    public void setPotentialYellowRatio(Double potentialYellowRatio) {
        this.potentialYellowRatio = potentialYellowRatio;
    }

    public Double getFullwireHotFraction() {
        return fullwireHotFraction;
    }

    public void setFullwireHotFraction(Double fullwireHotFraction) {
        this.fullwireHotFraction = fullwireHotFraction;
    }

    public Double getElongatedAspectRatio() {
        return elongatedAspectRatio;
    }

    public void setElongatedAspectRatio(Double elongatedAspectRatio) {
        this.elongatedAspectRatio = elongatedAspectRatio;
    }

    public Double getMergeCloseFrac() {
        return mergeCloseFrac;
    }

    public void setMergeCloseFrac(Double mergeCloseFrac) {
        this.mergeCloseFrac = mergeCloseFrac;
    }

    public Integer getMinClusterAreaPx() {
        return minClusterAreaPx;
    }

    public void setMinClusterAreaPx(Integer minClusterAreaPx) {
        this.minClusterAreaPx = minClusterAreaPx;
    }

    public Double getSidebarSearchFrac() {
        return sidebarSearchFrac;
    }

    public void setSidebarSearchFrac(Double sidebarSearchFrac) {
        this.sidebarSearchFrac = sidebarSearchFrac;
    }

    public Double getSidebarMinWidthFrac() {
        return sidebarMinWidthFrac;
    }

    public void setSidebarMinWidthFrac(Double sidebarMinWidthFrac) {
        this.sidebarMinWidthFrac = sidebarMinWidthFrac;
    }

    public Double getSidebarMaxWidthFrac() {
        return sidebarMaxWidthFrac;
    }

    public void setSidebarMaxWidthFrac(Double sidebarMaxWidthFrac) {
        this.sidebarMaxWidthFrac = sidebarMaxWidthFrac;
    }

    public Double getSidebarMinValidFrac() {
        return sidebarMinValidFrac;
    }

    public void setSidebarMinValidFrac(Double sidebarMinValidFrac) {
        this.sidebarMinValidFrac = sidebarMinValidFrac;
    }

    public Integer getSidebarHueSpanDeg() {
        return sidebarHueSpanDeg;
    }

    public void setSidebarHueSpanDeg(Integer sidebarHueSpanDeg) {
        this.sidebarHueSpanDeg = sidebarHueSpanDeg;
    }

    public Integer getSidebarMarginPx() {
        return sidebarMarginPx;
    }

    public void setSidebarMarginPx(Integer sidebarMarginPx) {
        this.sidebarMarginPx = sidebarMarginPx;
    }

    public Double getTextBottomBandFrac() {
        return textBottomBandFrac;
    }

    public void setTextBottomBandFrac(Double textBottomBandFrac) {
        this.textBottomBandFrac = textBottomBandFrac;
    }

    public Boolean getMaskTopLeftOverlay() {
        return maskTopLeftOverlay;
    }

    public void setMaskTopLeftOverlay(Boolean maskTopLeftOverlay) {
        this.maskTopLeftOverlay = maskTopLeftOverlay;
    }

    public String getTopLeftBox() {
        return topLeftBox;
    }

    public void setTopLeftBox(String topLeftBox) {
        this.topLeftBox = topLeftBox;
    }

    public Integer getHBins() {
        return hBins;
    }

    public void setHBins(Integer hBins) {
        this.hBins = hBins;
    }

    public Double getHistDistanceMin() {
        return histDistanceMin;
    }

    public void setHistDistanceMin(Double histDistanceMin) {
        this.histDistanceMin = histDistanceMin;
    }

    public Double getRedBgRatioMinIncrease() {
        return redBgRatioMinIncrease;
    }

    public void setRedBgRatioMinIncrease(Double redBgRatioMinIncrease) {
        this.redBgRatioMinIncrease = redBgRatioMinIncrease;
    }

    public Double getRedBgMinAbs() {
        return redBgMinAbs;
    }

    public void setRedBgMinAbs(Double redBgMinAbs) {
        this.redBgMinAbs = redBgMinAbs;
    }

    public Integer getRoiSMin() {
        return roiSMin;
    }

    public void setRoiSMin(Integer roiSMin) {
        this.roiSMin = roiSMin;
    }

    public Integer getRoiVMin() {
        return roiVMin;
    }

    public void setRoiVMin(Integer roiVMin) {
        this.roiVMin = roiVMin;
    }

    public Integer getBlueHLo() {
        return blueHLo;
    }

    public void setBlueHLo(Integer blueHLo) {
        this.blueHLo = blueHLo;
    }

    public Integer getBlueHHi() {
        return blueHHi;
    }

    public void setBlueHHi(Integer blueHHi) {
        this.blueHHi = blueHHi;
    }

    public Integer getBlueSMin() {
        return blueSMin;
    }

    public void setBlueSMin(Integer blueSMin) {
        this.blueSMin = blueSMin;
    }

    public Integer getBlueVMin() {
        return blueVMin;
    }

    public void setBlueVMin(Integer blueVMin) {
        this.blueVMin = blueVMin;
    }

    public Integer getBlackVHi() {
        return blackVHi;
    }

    public void setBlackVHi(Integer blackVHi) {
        this.blackVHi = blackVHi;
    }

    public Integer getWhiteBgSMax() {
        return whiteBgSMax;
    }

    public void setWhiteBgSMax(Integer whiteBgSMax) {
        this.whiteBgSMax = whiteBgSMax;
    }

    public Integer getWhiteBgVMin() {
        return whiteBgVMin;
    }

    public void setWhiteBgVMin(Integer whiteBgVMin) {
        this.whiteBgVMin = whiteBgVMin;
    }

    public Integer getWhiteBgExcludeNearWarmPx() {
        return whiteBgExcludeNearWarmPx;
    }

    public void setWhiteBgExcludeNearWarmPx(Integer whiteBgExcludeNearWarmPx) {
        this.whiteBgExcludeNearWarmPx = whiteBgExcludeNearWarmPx;
    }

    public Double getWhiteBgColumnFrac() {
        return whiteBgColumnFrac;
    }

    public void setWhiteBgColumnFrac(Double whiteBgColumnFrac) {
        this.whiteBgColumnFrac = whiteBgColumnFrac;
    }

    public Double getWhiteBgRowFrac() {
        return whiteBgRowFrac;
    }

    public void setWhiteBgRowFrac(Double whiteBgRowFrac) {
        this.whiteBgRowFrac = whiteBgRowFrac;
    }
}
