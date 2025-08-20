package com.example.transformer.repository;

import com.example.transformer.model.TransformerImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransformerImageRepository extends JpaRepository<TransformerImage, Long> {
  List<TransformerImage> findByTransformerIdOrderByCreatedAtDesc(Long transformerId);
}
