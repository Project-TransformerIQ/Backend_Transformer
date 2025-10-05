package com.example.transformer.repository;

import com.example.transformer.model.DisplayMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DisplayMetadataRepository extends JpaRepository<DisplayMetadata, Long> {
    Optional<DisplayMetadata> findByImageId(Long imageId);
}
