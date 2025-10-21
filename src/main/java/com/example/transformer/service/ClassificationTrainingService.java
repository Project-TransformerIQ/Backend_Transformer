package com.example.transformer.service;

import com.example.transformer.dto.TrainModelResponseDTO;
import com.example.transformer.model.AnomalyDetectionConfig;
import com.example.transformer.model.TransformerImage;
import com.example.transformer.model.FaultRegion;
import com.example.transformer.model.OriginalAnomalyResult;
import com.example.transformer.repository.AnomalyDetectionConfigRepository;
import com.example.transformer.repository.TransformerImageRepository;
import com.example.transformer.repository.FaultRegionRepository;
import com.example.transformer.repository.OriginalAnomalyResultRepository;
import com.example.transformer.exception.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassificationTrainingService {

    @Value("${anomaly.detection.api.url:http://localhost:5000}")
    private String flaskApiUrl;

    private final RestTemplate restTemplate;
    private final AnomalyDetectionConfigRepository configRepository;
    private final TransformerImageRepository imageRepository;
    private final FaultRegionRepository faultRegionRepository;
    private final OriginalAnomalyResultRepository originalAnomalyResultRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ClassificationTrainingService(
            RestTemplate restTemplate,
            AnomalyDetectionConfigRepository configRepository,
            TransformerImageRepository imageRepository,
            FaultRegionRepository faultRegionRepository,
            OriginalAnomalyResultRepository originalAnomalyResultRepository,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.configRepository = configRepository;
        this.imageRepository = imageRepository;
        this.faultRegionRepository = faultRegionRepository;
        this.originalAnomalyResultRepository = originalAnomalyResultRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    /**
     * Train the classification model by sending baseline image, maintenance image,
     * current configuration, and anomaly detection results to the classification
     * server.
     * 
     * @param transformerId      The transformer ID
     * @param baselineImageId    The baseline image ID
     * @param maintenanceImageId The maintenance image ID
     * @return TrainModelResponseDTO containing the training result
     */
    @Transactional
    public TrainModelResponseDTO trainModel(Long transformerId, Long baselineImageId, Long maintenanceImageId) {
        // Validate and retrieve baseline image
        TransformerImage baselineImage = imageRepository.findById(baselineImageId)
                .orElseThrow(() -> new NotFoundException("Baseline image " + baselineImageId + " not found"));

        if (!baselineImage.getTransformer().getId().equals(transformerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Baseline image does not belong to transformer " + transformerId);
        }

        // Validate and retrieve maintenance image
        TransformerImage maintenanceImage = imageRepository.findById(maintenanceImageId)
                .orElseThrow(() -> new NotFoundException("Maintenance image " + maintenanceImageId + " not found"));

        if (!maintenanceImage.getTransformer().getId().equals(transformerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maintenance image does not belong to transformer " + transformerId);
        }

        // Get current active configuration
        AnomalyDetectionConfig currentConfig = getActiveConfig();

        // Get anomaly detection results for the maintenance image
        List<FaultRegion> faultRegions = faultRegionRepository.findByImageIdOrderByRegionIdAsc(maintenanceImageId);

        if (faultRegions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No anomaly detection results found for maintenance image " + maintenanceImageId);
        }

        // Filter out deleted regions - don't send deleted results to classification server
        List<FaultRegion> activeFaultRegions = faultRegions.stream()
                .filter(region -> region.getIsDeleted() == null || !region.getIsDeleted())
                .toList();

        if (activeFaultRegions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No active (non-deleted) anomaly detection results found for maintenance image " + maintenanceImageId);
        }

        // Prepare request to classification server
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Add baseline image
        Resource baselineResource = fileStorageService.load(baselineImage.getStoragePath());
        body.add("baseline_image", baselineResource);

        // Add maintenance image
        Resource maintenanceResource = fileStorageService.load(maintenanceImage.getStoragePath());
        body.add("maintenance_image", maintenanceResource);

        // Add current configuration as JSON
        String configJson = createConfigJson(currentConfig);
        ByteArrayResource configResource = new ByteArrayResource(configJson.getBytes()) {
            @Override
            public String getFilename() {
                return "config.json";
            }
        };
        body.add("config", configResource);

        // Add anomaly detection results as JSON
        String anomalyResultsJson = createAnomalyResultsJson(activeFaultRegions);
        ByteArrayResource anomalyResource = new ByteArrayResource(anomalyResultsJson.getBytes()) {
            @Override
            public String getFilename() {
                return "anomaly_results.json";
            }
        };
        body.add("anomaly_results", anomalyResource);

        // Add original anomaly results from classification server as JSON
        OriginalAnomalyResult originalResult = originalAnomalyResultRepository.findByImageId(maintenanceImageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No original anomaly detection results found for maintenance image " + maintenanceImageId +
                        ". This data is required for training."));

        // Use the original JSON directly from the database
        ByteArrayResource originalResource = new ByteArrayResource(originalResult.getAnomalyJson().getBytes()) {
            @Override
            public String getFilename() {
                return "original_anomaly_results.json";
            }
        };
        body.add("original_anomaly_results", originalResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // Call classification server training endpoint
            ResponseEntity<String> response = restTemplate.postForEntity(
                    flaskApiUrl + "/update-config",
                    requestEntity,
                    String.class);

            // Parse response and update configuration
            String responseBody = response.getBody();
            if (responseBody != null && !responseBody.isEmpty()) {
                return updateConfigurationFromTrainingResponse(responseBody, currentConfig);
            } else {
                throw new RuntimeException("Empty response from classification server");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to train model on Flask API server: " + e.getMessage(), e);
        }
    }

    /**
     * Update the configuration based on the training response from classification
     * server
     */
    private TrainModelResponseDTO updateConfigurationFromTrainingResponse(String responseJson,
            AnomalyDetectionConfig currentConfig) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            // Parse updated configuration from response
            if (root.has("updated_config")) {
                JsonNode configNode = root.get("updated_config");

                // Create new configuration with a shorter name to avoid exceeding 100 char limit
                // Format: "trained_YYYY-MM-DD_HH-MM-SS" (max ~27 characters)
                String timestamp = LocalDateTime.now().toString()
                        .replace(":", "-")
                        .replace("T", "_")
                        .substring(0, 19); // YYYY-MM-DD_HH-MM-SS
                String newConfigName = "trained_" + timestamp;

                // Deactivate current config
                currentConfig.setIsActive(false);
                configRepository.save(currentConfig);

                // Create new config with updated parameters
                AnomalyDetectionConfig newConfig = new AnomalyDetectionConfig();
                newConfig.setConfigName(newConfigName);
                newConfig.setDescription("Configuration updated from classification training at " +
                        LocalDateTime.now());

                // Update all 41 parameters from response
                if (configNode.has("delta_k_sigma"))
                    newConfig.setDeltaKSigma(configNode.get("delta_k_sigma").asDouble());
                if (configNode.has("delta_abs_min"))
                    newConfig.setDeltaAbsMin(configNode.get("delta_abs_min").asInt());
                if (configNode.has("min_blob_area_px"))
                    newConfig.setMinBlobAreaPx(configNode.get("min_blob_area_px").asInt());
                if (configNode.has("open_iters"))
                    newConfig.setOpenIters(configNode.get("open_iters").asInt());
                if (configNode.has("dilate_iters"))
                    newConfig.setDilateIters(configNode.get("dilate_iters").asInt());
                if (configNode.has("keep_component_min_ratio"))
                    newConfig.setKeepComponentMinRatio(configNode.get("keep_component_min_ratio").asDouble());
                if (configNode.has("fault_red_ratio"))
                    newConfig.setFaultRedRatio(configNode.get("fault_red_ratio").asDouble());
                if (configNode.has("fault_red_min_pixels"))
                    newConfig.setFaultRedMinPixels(configNode.get("fault_red_min_pixels").asInt());
                if (configNode.has("potential_yellow_ratio"))
                    newConfig.setPotentialYellowRatio(configNode.get("potential_yellow_ratio").asDouble());
                if (configNode.has("fullwire_hot_fraction"))
                    newConfig.setFullwireHotFraction(configNode.get("fullwire_hot_fraction").asDouble());
                if (configNode.has("elongated_aspect_ratio"))
                    newConfig.setElongatedAspectRatio(configNode.get("elongated_aspect_ratio").asDouble());
                if (configNode.has("merge_close_frac"))
                    newConfig.setMergeCloseFrac(configNode.get("merge_close_frac").asDouble());
                if (configNode.has("min_cluster_area_px"))
                    newConfig.setMinClusterAreaPx(configNode.get("min_cluster_area_px").asInt());
                if (configNode.has("sidebar_search_frac"))
                    newConfig.setSidebarSearchFrac(configNode.get("sidebar_search_frac").asDouble());
                if (configNode.has("sidebar_min_width_frac"))
                    newConfig.setSidebarMinWidthFrac(configNode.get("sidebar_min_width_frac").asDouble());
                if (configNode.has("sidebar_max_width_frac"))
                    newConfig.setSidebarMaxWidthFrac(configNode.get("sidebar_max_width_frac").asDouble());
                if (configNode.has("sidebar_min_valid_frac"))
                    newConfig.setSidebarMinValidFrac(configNode.get("sidebar_min_valid_frac").asDouble());
                if (configNode.has("sidebar_hue_span_deg"))
                    newConfig.setSidebarHueSpanDeg(configNode.get("sidebar_hue_span_deg").asInt());
                if (configNode.has("sidebar_margin_px"))
                    newConfig.setSidebarMarginPx(configNode.get("sidebar_margin_px").asInt());
                if (configNode.has("text_bottom_band_frac"))
                    newConfig.setTextBottomBandFrac(configNode.get("text_bottom_band_frac").asDouble());
                if (configNode.has("mask_top_left_overlay"))
                    newConfig.setMaskTopLeftOverlay(configNode.get("mask_top_left_overlay").asBoolean());

                // Convert top_left_box from List<Double> to comma-separated string
                if (configNode.has("top_left_box")) {
                    JsonNode topLeftBoxNode = configNode.get("top_left_box");
                    if (topLeftBoxNode.isArray()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < topLeftBoxNode.size(); i++) {
                            if (i > 0)
                                sb.append(",");
                            sb.append(topLeftBoxNode.get(i).asDouble());
                        }
                        newConfig.setTopLeftBox(sb.toString());
                    }
                }

                if (configNode.has("h_bins"))
                    newConfig.setHBins(configNode.get("h_bins").asInt());
                if (configNode.has("hist_distance_min"))
                    newConfig.setHistDistanceMin(configNode.get("hist_distance_min").asDouble());
                if (configNode.has("red_bg_ratio_min_increase"))
                    newConfig.setRedBgRatioMinIncrease(configNode.get("red_bg_ratio_min_increase").asDouble());
                if (configNode.has("red_bg_min_abs"))
                    newConfig.setRedBgMinAbs(configNode.get("red_bg_min_abs").asDouble());
                if (configNode.has("roi_s_min"))
                    newConfig.setRoiSMin(configNode.get("roi_s_min").asInt());
                if (configNode.has("roi_v_min"))
                    newConfig.setRoiVMin(configNode.get("roi_v_min").asInt());
                if (configNode.has("blue_h_lo"))
                    newConfig.setBlueHLo(configNode.get("blue_h_lo").asInt());
                if (configNode.has("blue_h_hi"))
                    newConfig.setBlueHHi(configNode.get("blue_h_hi").asInt());
                if (configNode.has("blue_s_min"))
                    newConfig.setBlueSMin(configNode.get("blue_s_min").asInt());
                if (configNode.has("blue_v_min"))
                    newConfig.setBlueVMin(configNode.get("blue_v_min").asInt());
                if (configNode.has("black_v_hi"))
                    newConfig.setBlackVHi(configNode.get("black_v_hi").asInt());
                if (configNode.has("white_bg_S_max"))
                    newConfig.setWhiteBgSMax(configNode.get("white_bg_S_max").asInt());
                if (configNode.has("white_bg_V_min"))
                    newConfig.setWhiteBgVMin(configNode.get("white_bg_V_min").asInt());
                if (configNode.has("white_bg_exclude_near_warm_px"))
                    newConfig.setWhiteBgExcludeNearWarmPx(configNode.get("white_bg_exclude_near_warm_px").asInt());
                if (configNode.has("white_bg_column_frac"))
                    newConfig.setWhiteBgColumnFrac(configNode.get("white_bg_column_frac").asDouble());
                if (configNode.has("white_bg_row_frac"))
                    newConfig.setWhiteBgRowFrac(configNode.get("white_bg_row_frac").asDouble());

                // Activate the new configuration
                newConfig.setIsActive(true);
                AnomalyDetectionConfig savedConfig = configRepository.save(newConfig);

                // Build response
                String message = root.has("message") ? root.get("message").asText()
                        : "Model trained successfully and configuration updated";
                String status = root.has("status") ? root.get("status").asText() : "success";

                return new TrainModelResponseDTO(
                        status,
                        message,
                        savedConfig.getId(),
                        savedConfig.getConfigName(),
                        LocalDateTime.now());
            } else {
                throw new RuntimeException("No updated configuration found in training response");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse training response and update configuration: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Get the active configuration or create a default one
     */
    private AnomalyDetectionConfig getActiveConfig() {
        return configRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    AnomalyDetectionConfig defaultConfig = new AnomalyDetectionConfig();
                    defaultConfig.setConfigName("default");
                    defaultConfig.setIsActive(true);
                    defaultConfig.setDescription("Default configuration for anomaly detection");
                    return configRepository.save(defaultConfig);
                });
    }

    /**
     * Create JSON representation of the configuration
     */
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

    /**
     * Create JSON representation of anomaly detection results (fault regions)
     */
    private String createAnomalyResultsJson(List<FaultRegion> faultRegions) {
        try {
            Map<String, Object> resultsMap = new HashMap<>();
            List<Map<String, Object>> regions = new java.util.ArrayList<>();

            for (FaultRegion region : faultRegions) {
                Map<String, Object> regionMap = new HashMap<>();

                if (region.getRegionId() != null)
                    regionMap.put("id", region.getRegionId());
                if (region.getType() != null)
                    regionMap.put("type", region.getType());
                if (region.getDominantColor() != null)
                    regionMap.put("dominant_color", region.getDominantColor());
                if (region.getColorRgb() != null)
                    regionMap.put("color_rgb", region.getColorRgb());

                // Bounding box
                if (region.getBoundingBox() != null) {
                    Map<String, Object> bbox = new HashMap<>();
                    bbox.put("x", region.getBoundingBox().getX());
                    bbox.put("y", region.getBoundingBox().getY());
                    bbox.put("width", region.getBoundingBox().getWidth());
                    bbox.put("height", region.getBoundingBox().getHeight());
                    bbox.put("areaPx", region.getBoundingBox().getAreaPx());
                    regionMap.put("boundingBox", bbox);
                }

                // Centroid
                if (region.getCentroid() != null) {
                    Map<String, Object> centroid = new HashMap<>();
                    centroid.put("x", region.getCentroid().getX());
                    centroid.put("y", region.getCentroid().getY());
                    regionMap.put("centroid", centroid);
                }

                if (region.getAspectRatio() != null)
                    regionMap.put("aspect_ratio", region.getAspectRatio());
                if (region.getElongated() != null)
                    regionMap.put("elongated", region.getElongated());
                if (region.getConnectedToWire() != null)
                    regionMap.put("connected_to_wire", region.getConnectedToWire());
                if (region.getTag() != null)
                    regionMap.put("tag", region.getTag());
                if (region.getConfidence() != null)
                    regionMap.put("confidence", region.getConfidence());

                regions.add(regionMap);
            }

            resultsMap.put("fault_regions", regions);
            return objectMapper.writeValueAsString(resultsMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create anomaly results JSON: " + e.getMessage(), e);
        }
    }
}

