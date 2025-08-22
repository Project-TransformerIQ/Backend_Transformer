// src/main/java/com/example/transformer/repository/InspectionRepository.java
package com.example.transformer.repository;

import com.example.transformer.model.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
  List<Inspection> findByTransformerIdOrderByCreatedAtDesc(Long transformerId);
}
