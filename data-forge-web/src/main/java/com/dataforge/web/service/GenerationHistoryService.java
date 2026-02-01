package com.dataforge.web.service;

import com.dataforge.web.entity.GenerationHistory;
import java.time.LocalDateTime;
import java.util.List;

public interface GenerationHistoryService {

  GenerationHistory createHistory(GenerationHistory history);

  GenerationHistory updateHistory(Long id, GenerationHistory history);

  GenerationHistory getHistoryById(Long id);

  List<GenerationHistory> getHistoriesByStatus(String status);

  List<GenerationHistory> getHistoriesByTemplateId(Long templateId);

  List<GenerationHistory> getHistoriesByTimeRange(LocalDateTime start, LocalDateTime end);

  List<GenerationHistory> getRecentHistories(int limit);

  List<GenerationHistory> getAllHistories();

  void deleteHistory(Long id);

  void deleteHistoriesByTemplateId(Long templateId);
}
