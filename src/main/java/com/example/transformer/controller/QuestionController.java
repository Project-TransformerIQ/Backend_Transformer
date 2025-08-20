package com.example.transformer.controller;

import com.example.transformer.dto.QuestionDTO;
import com.example.transformer.model.Question;
import com.example.transformer.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
  private final QuestionService service;
  public QuestionController(QuestionService service) { this.service = service; }

  @PostMapping
  public Question create(@RequestBody @Valid QuestionDTO dto) { return service.create(dto); }

  @GetMapping
  public Page<Question> list(Pageable pageable) { return service.list(pageable); }

  @GetMapping("/{id}")
  public Question get(@PathVariable Long id) { return service.get(id); }

  @PutMapping("/{id}")
  public Question update(@PathVariable Long id, @RequestBody @Valid QuestionDTO dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) { service.delete(id); }
}
