package com.example.transformer.repository;

import com.example.transformer.model.FaultRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FaultRegionRepository extends JpaRepository<FaultRegion, Long> {
    @Query("SELECT fr FROM FaultRegion fr WHERE fr.image.id = :imageId")
    List<FaultRegion> findByImageId(@Param("imageId") Long imageId);

    @Query("SELECT fr FROM FaultRegion fr WHERE fr.image.id = :imageId ORDER BY fr.regionId ASC")
    List<FaultRegion> findByImageIdOrderByRegionIdAsc(@Param("imageId") Long imageId);
}
