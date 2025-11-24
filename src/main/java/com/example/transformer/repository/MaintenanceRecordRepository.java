package com.example.transformer.repository;

import com.example.transformer.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByTransformerIdOrderByInspectionTimestampDesc(Long transformerId);

    Optional<MaintenanceRecord> findByMaintenanceImageId(Long maintenanceImageId);
}
