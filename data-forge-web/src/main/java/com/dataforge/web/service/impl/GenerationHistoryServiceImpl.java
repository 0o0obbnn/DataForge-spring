package com.dataforge.web.service.impl;

import com.dataforge.web.entity.GenerationHistory;
import com.dataforge.web.exception.ResourceNotFoundException;
import com.dataforge.web.repository.GenerationHistoryRepository;
import com.dataforge.web.service.GenerationHistoryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GenerationHistoryServiceImpl implements GenerationHistoryService {

  private final GenerationHistoryRepository generationHistoryRepository;

  public GenerationHistoryServiceImpl(GenerationHistoryRepository generationHistoryRepository) {
    this.generationHistoryRepository = generationHistoryRepository;
  }

  @Override
  public GenerationHistory createHistory(GenerationHistory history) {
    return generationHistoryRepository.save(history);
  }

  @Override
  public GenerationHistory updateHistory(Long id, GenerationHistory history) {
    Optional<GenerationHistory> existingHistoryOpt = generationHistoryRepository.findById(id);
    if (existingHistoryOpt.isEmpty()) {
      throw new IllegalArgumentException("Generation history with id '" + id + "' not found");
    }

    // existingHistoryOpt.get() 在 isEmpty() 检查后调用，不会为 null
    GenerationHistory existingHistory = existingHistoryOpt.get();
    existingHistory.setStatus(history.getStatus());
    existingHistory.setDurationMs(history.getDurationMs());
    existingHistory.setErrorMessage(history.getErrorMessage());
    existingHistory.setCompletedAt(history.getCompletedAt());

    return generationHistoryRepository.save(existingHistory);
  }

  @Override
  @Transactional(readOnly = true)
  public GenerationHistory getHistoryById(Long id) {
    return generationHistoryRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException("Generation history with id '" + id + "' not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public List<GenerationHistory> getHistoriesByStatus(String status) {
    return generationHistoryRepository.findByStatus(status);
  }

  @Override
  @Transactional(readOnly = true)
  public List<GenerationHistory> getHistoriesByTemplateId(Long templateId) {
    return generationHistoryRepository.findByTemplateId(templateId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<GenerationHistory> getHistoriesByTimeRange(LocalDateTime start, LocalDateTime end) {
    return generationHistoryRepository.findByCreatedAtBetween(start, end);
  }

  @Override
  @Transactional(readOnly = true)
  public List<GenerationHistory> getRecentHistories(Pageable pageable) {
    return generationHistoryRepository.findAllByOrderByCreatedAtDesc(pageable);
  }

  @Override
  @Deprecated
  @Transactional(readOnly = true)
  public List<GenerationHistory> getRecentHistories(int limit) {
    return getRecentHistories(PageRequest.of(0, limit));
  }

  @Override
  @Transactional(readOnly = true)
  public List<GenerationHistory> getAllHistories() {
    return generationHistoryRepository.findAll();
  }

  @Override
  public void deleteHistory(Long id) {
    if (!generationHistoryRepository.existsById(id)) {
      throw new IllegalArgumentException("Generation history with id '" + id + "' not found");
    }
    generationHistoryRepository.deleteById(id);
  }

  @Override
  public void deleteHistoriesByTemplateId(Long templateId) {
    List<GenerationHistory> histories = generationHistoryRepository.findByTemplateId(templateId);
    generationHistoryRepository.deleteAll(histories);
  }
}
