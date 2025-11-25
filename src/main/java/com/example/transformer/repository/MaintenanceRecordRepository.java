package com.example.transformer.repository;

import com.example.transformer.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByTransformerIdOrderByInspectionTimestampDesc(Long transformerId);

    Optional<MaintenanceRecord> findByMaintenanceImageId(Long maintenanceImageId);
    List<MaintenanceRecord> findByInspectionId(Long inspectionId);

    // 🔹 for Option B cascade delete:
    @Transactional
    @Modifying
    void deleteByInspectionId(Long inspectionId);
    @Transactional
    @Modifying
    void deleteByMaintenanceImageId(Long maintenanceImageId);

    @Transactional
    @Modifying
    void deleteByTransformerId(Long transformerId);
}
