package com.example.transformer.repository;

import com.example.transformer.model.OriginalAnomalyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface OriginalAnomalyResultRepository extends JpaRepository<OriginalAnomalyResult, Long> {

    Optional<OriginalAnomalyResult> findByImageId(Long imageId);

    boolean existsByImageId(Long imageId);
    @Transactional
    @Modifying
    void deleteByImageId(Long imageId);
}

