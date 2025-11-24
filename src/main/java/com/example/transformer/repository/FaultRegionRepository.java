package com.example.transformer.repository;

import com.example.transformer.model.FaultRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FaultRegionRepository extends JpaRepository<FaultRegion, Long> {

    // Used commonly throughout backend
    List<FaultRegion> findByImageIdOrderByRegionIdAsc(Long imageId);

    // Required by ErrorAnnotationService
    List<FaultRegion> findByImageId(Long imageId);

    // Required when deleting an inspection
    @Transactional
    @Modifying
    void deleteByImageId(Long imageId);
}
