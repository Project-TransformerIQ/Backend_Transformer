package com.example.transformer.repository;

import com.example.transformer.model.TransformerImage;
import com.example.transformer.model.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransformerImageRepository extends JpaRepository<TransformerImage, Long> {
  List<TransformerImage> findByTransformerIdOrderByCreatedAtDesc(Long transformerId);
  List<TransformerImage> findByTransformerIdAndImageTypeOrderByCreatedAtDesc(Long transformerId, ImageType imageType);
  List<TransformerImage> findByImageTypeOrderByCreatedAtDesc(ImageType imageType);
  List<TransformerImage> findByTransformerIdAndImageTypeAndInspection_IdOrderByCreatedAtDesc(
        Long transformerId,
        ImageType imageType,
        Long inspectionId
  );
  List<TransformerImage> findByInspectionId(Long inspectionId);

}
