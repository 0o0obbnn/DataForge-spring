package com.dataforge.web.service;

import com.dataforge.web.entity.GenerationHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface GenerationHistoryService {

  GenerationHistory createHistory(GenerationHistory history);

  GenerationHistory updateHistory(Long id, GenerationHistory history);

  GenerationHistory getHistoryById(Long id);

  List<GenerationHistory> getHistoriesByStatus(String status);

  List<GenerationHistory> getHistoriesByTemplateId(Long templateId);

  List<GenerationHistory> getHistoriesByTimeRange(LocalDateTime start, LocalDateTime end);

  /**
   * 按创建时间倒序分页查询最近任务。
   *
   * @param pageable 分页参数（page、size）
   * @return 任务列表
   */
  List<GenerationHistory> getRecentHistories(Pageable pageable);

  /**
   * @deprecated 使用 {@link #getRecentHistories(Pageable)} 代替
   */
  @Deprecated
  List<GenerationHistory> getRecentHistories(int limit);

  List<GenerationHistory> getAllHistories();

  void deleteHistory(Long id);

  void deleteHistoriesByTemplateId(Long templateId);
}
