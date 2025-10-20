package com.example.transformer.repository;

import com.example.transformer.model.AnomalyDetectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnomalyDetectionConfigRepository extends JpaRepository<AnomalyDetectionConfig, Long> {

    Optional<AnomalyDetectionConfig> findByIsActiveTrue();

    Optional<AnomalyDetectionConfig> findByConfigName(String configName);
}
