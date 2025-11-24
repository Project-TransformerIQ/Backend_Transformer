package com.example.transformer.repository;

import com.example.transformer.model.DisplayMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface DisplayMetadataRepository extends JpaRepository<DisplayMetadata, Long> {
    Optional<DisplayMetadata> findByImageId(Long imageId);
    @Transactional
    @Modifying
    void deleteByImageId(Long imageId);
}
