package com.example.transformer.repository;
import com.example.transformer.model.Transformer;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TransformerRepository extends JpaRepository<Transformer, Long> {}
