package com.example.transformer.service;

import com.example.transformer.model.AnomalyDetectionConfig;
import com.example.transformer.repository.AnomalyDetectionConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnomalyDetectionService {

    @Value("${anomaly.detection.api.url:http://localhost:5000}")
    private String flaskApiUrl;

    private final RestTemplate restTemplate;
    private final AnomalyDetectionConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    public AnomalyDetectionService(RestTemplate restTemplate,
            AnomalyDetectionConfigRepository configRepository,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.configRepository = configRepository;
        this.objectMapper = objectMapper;
    }

    public String detectAnomalies(Resource baselineImage, Resource maintenanceImage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Get active configuration from database
        AnomalyDetectionConfig config = getActiveConfig();

        // Create configuration JSON
        String configJson = createConfigJson(config);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("baseline", baselineImage);
        body.add("candidate", maintenanceImage);

        // Add configuration as a JSON file
        ByteArrayResource configResource = new ByteArrayResource(configJson.getBytes()) {
            @Override
            public String getFilename() {
                return "config.json";
            }
        };
        body.add("config", configResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    flaskApiUrl + "/detect-anomalies",
                    requestEntity,
                    String.class);

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Flask API for anomaly detection: " + e.getMessage(), e);
        }
    }

    private AnomalyDetectionConfig getActiveConfig() {
        return configRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    // If no active config found, create and return default config
                    AnomalyDetectionConfig defaultConfig = new AnomalyDetectionConfig();
                    defaultConfig.setConfigName("default");
                    defaultConfig.setIsActive(true);
                    defaultConfig.setDescription("Default configuration for anomaly detection");
                    return configRepository.save(defaultConfig);
                });
    }

    private String createConfigJson(AnomalyDetectionConfig config) {
        try {
            Map<String, Object> configMap = new HashMap<>();

            // All 41 parameters matching Python dict structure
            configMap.put("delta_k_sigma", config.getDeltaKSigma());
            configMap.put("delta_abs_min", config.getDeltaAbsMin());
            configMap.put("min_blob_area_px", config.getMinBlobAreaPx());
            configMap.put("open_iters", config.getOpenIters());
            configMap.put("dilate_iters", config.getDilateIters());
            configMap.put("keep_component_min_ratio", config.getKeepComponentMinRatio());
            configMap.put("fault_red_ratio", config.getFaultRedRatio());
            configMap.put("fault_red_min_pixels", config.getFaultRedMinPixels());
            configMap.put("potential_yellow_ratio", config.getPotentialYellowRatio());
            configMap.put("fullwire_hot_fraction", config.getFullwireHotFraction());
            configMap.put("elongated_aspect_ratio", config.getElongatedAspectRatio());
            configMap.put("merge_close_frac", config.getMergeCloseFrac());
            configMap.put("min_cluster_area_px", config.getMinClusterAreaPx());
            configMap.put("sidebar_search_frac", config.getSidebarSearchFrac());
            configMap.put("sidebar_min_width_frac", config.getSidebarMinWidthFrac());
            configMap.put("sidebar_max_width_frac", config.getSidebarMaxWidthFrac());
            configMap.put("sidebar_min_valid_frac", config.getSidebarMinValidFrac());
            configMap.put("sidebar_hue_span_deg", config.getSidebarHueSpanDeg());
            configMap.put("sidebar_margin_px", config.getSidebarMarginPx());
            configMap.put("text_bottom_band_frac", config.getTextBottomBandFrac());
            configMap.put("mask_top_left_overlay", config.getMaskTopLeftOverlay());

            // Parse top_left_box from string "x1,y1,x2,y2" to List<Double>
            String topLeftBoxStr = config.getTopLeftBox();
            if (topLeftBoxStr != null && !topLeftBoxStr.isEmpty()) {
                String[] parts = topLeftBoxStr.split(",");
                List<Double> topLeftBox = new ArrayList<>();
                for (String part : parts) {
                    topLeftBox.add(Double.parseDouble(part.trim()));
                }
                configMap.put("top_left_box", topLeftBox);
            }

            configMap.put("h_bins", config.getHBins());
            configMap.put("hist_distance_min", config.getHistDistanceMin());
            configMap.put("red_bg_ratio_min_increase", config.getRedBgRatioMinIncrease());
            configMap.put("red_bg_min_abs", config.getRedBgMinAbs());
            configMap.put("roi_s_min", config.getRoiSMin());
            configMap.put("roi_v_min", config.getRoiVMin());
            configMap.put("blue_h_lo", config.getBlueHLo());
            configMap.put("blue_h_hi", config.getBlueHHi());
            configMap.put("blue_s_min", config.getBlueSMin());
            configMap.put("blue_v_min", config.getBlueVMin());
            configMap.put("black_v_hi", config.getBlackVHi());
            configMap.put("white_bg_S_max", config.getWhiteBgSMax());
            configMap.put("white_bg_V_min", config.getWhiteBgVMin());
            configMap.put("white_bg_exclude_near_warm_px", config.getWhiteBgExcludeNearWarmPx());
            configMap.put("white_bg_column_frac", config.getWhiteBgColumnFrac());
            configMap.put("white_bg_row_frac", config.getWhiteBgRowFrac());

            return objectMapper.writeValueAsString(configMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create configuration JSON: " + e.getMessage(), e);
        }
    }
}
