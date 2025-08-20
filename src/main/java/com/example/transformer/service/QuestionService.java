package com.example.transformer.service;

import com.example.transformer.dto.QuestionDTO;
import com.example.transformer.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionService {
  Question create(QuestionDTO dto);
  Page<Question> list(Pageable pageable);
  Question get(Long id);
  Question update(Long id, QuestionDTO dto);
  void delete(Long id);
}
