package com.example.transformer.service;

import com.example.transformer.dto.QuestionDTO;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.Question;
import com.example.transformer.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class QuestionServiceImpl implements QuestionService {
  private final QuestionRepository repo;
  public QuestionServiceImpl(QuestionRepository repo) { this.repo = repo; }

  @Override @Transactional
  public Question create(QuestionDTO dto) {
    var q = new Question();
    q.setText(dto.text());
    return repo.save(q);
  }

  @Override
  public Page<Question> list(Pageable pageable) {
    return repo.findAll(pageable);
  }

  @Override
  public Question get(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("Question " + id + " not found"));
  }

  @Override @Transactional
  public Question update(Long id, QuestionDTO dto) {
    var q = get(id);
    q.setText(dto.text());
    return repo.save(q);
  }

  @Override @Transactional
  public void delete(Long id) {
    if (!repo.existsById(id)) throw new NotFoundException("Question " + id + " not found");
    repo.deleteById(id);
  }
}
