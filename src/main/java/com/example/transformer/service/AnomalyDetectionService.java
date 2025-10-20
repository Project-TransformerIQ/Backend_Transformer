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

import java.util.HashMap;
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

            // SSIM Configuration
            Map<String, Object> ssimConfig = new HashMap<>();
            ssimConfig.put("weight", config.getSsimWeight());
            ssimConfig.put("threshold", config.getSsimThreshold());
            configMap.put("ssim", ssimConfig);

            // MSE Configuration
            Map<String, Object> mseConfig = new HashMap<>();
            mseConfig.put("weight", config.getMseWeight());
            mseConfig.put("threshold", config.getMseThreshold());
            configMap.put("mse", mseConfig);

            // Histogram Configuration
            Map<String, Object> histogramConfig = new HashMap<>();
            histogramConfig.put("weight", config.getHistogramWeight());
            histogramConfig.put("threshold", config.getHistogramThreshold());
            configMap.put("histogram", histogramConfig);

            // Combined Threshold
            configMap.put("combined_threshold", config.getCombinedThreshold());

            // Image Processing Configuration
            Map<String, Object> imageProcessing = new HashMap<>();
            imageProcessing.put("resize_width", config.getResizeWidth());
            imageProcessing.put("resize_height", config.getResizeHeight());
            imageProcessing.put("blur_kernel_size", config.getBlurKernelSize());
            configMap.put("image_processing", imageProcessing);

            // Detection Configuration
            Map<String, Object> detection = new HashMap<>();
            detection.put("min_contour_area", config.getMinContourArea());
            detection.put("dilation_iterations", config.getDilationIterations());
            detection.put("erosion_iterations", config.getErosionIterations());
            configMap.put("detection", detection);

            return objectMapper.writeValueAsString(configMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create configuration JSON: " + e.getMessage(), e);
        }
    }
}
