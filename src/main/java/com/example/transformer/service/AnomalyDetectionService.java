package com.example.transformer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class AnomalyDetectionService {

    @Value("${anomaly.detection.api.url:http://localhost:5000}")
    private String flaskApiUrl;

    private final RestTemplate restTemplate;

    public AnomalyDetectionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String detectAnomalies(Resource baselineImage, Resource maintenanceImage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("baseline", baselineImage);
        body.add("candidate", maintenanceImage);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                flaskApiUrl + "/detect-anomalies",
                requestEntity,
                String.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Flask API for anomaly detection: " + e.getMessage(), e);
        }
    }
}
