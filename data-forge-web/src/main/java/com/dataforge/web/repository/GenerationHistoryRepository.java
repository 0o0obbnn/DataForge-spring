package com.dataforge.web.repository;

import com.dataforge.web.entity.GenerationHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenerationHistoryRepository extends JpaRepository<GenerationHistory, Long> {

  List<GenerationHistory> findByStatus(String status);

  List<GenerationHistory> findByTemplateId(Long templateId);

  List<GenerationHistory> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

  List<GenerationHistory> findTop10ByOrderByCreatedAtDesc();

  // 支持动态limit的查询方法
  List<GenerationHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
