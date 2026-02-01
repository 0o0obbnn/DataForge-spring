package com.dataforge.web.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.dataforge.web.config.TestRedisConfiguration;
import com.dataforge.web.entity.GenerationHistory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TestRedisConfiguration.class)
@DisplayName("GenerationHistoryRepository单元测试")
class GenerationHistoryRepositoryTest {

  @Autowired private GenerationHistoryRepository generationHistoryRepository;

  @Test
  @DisplayName("测试保存和查询生成历史")
  void should_save_and_find_generation_history() {
    GenerationHistory history = new GenerationHistory();
    history.setTemplateName("测试模板");
    history.setRecordCount(100);
    history.setStatus("COMPLETED");

    GenerationHistory saved = generationHistoryRepository.save(history);
    Optional<GenerationHistory> found = generationHistoryRepository.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals("测试模板", found.get().getTemplateName());
    assertEquals(100, found.get().getRecordCount());
  }

  @Test
  @DisplayName("测试根据状态查询历史记录")
  void should_find_history_by_status() {
    GenerationHistory history = new GenerationHistory();
    history.setTemplateName("查询测试");
    history.setRecordCount(50);
    history.setStatus("PENDING");
    generationHistoryRepository.save(history);

    List<GenerationHistory> histories = generationHistoryRepository.findByStatus("PENDING");

    assertFalse(histories.isEmpty());
    assertEquals("PENDING", histories.get(0).getStatus());
  }

  @Test
  @DisplayName("测试根据模板ID查询历史记录")
  void should_find_history_by_template_id() {
    GenerationHistory history = new GenerationHistory();
    history.setTemplateId(1L);
    history.setTemplateName("模板测试");
    history.setRecordCount(30);
    history.setStatus("COMPLETED");
    generationHistoryRepository.save(history);

    List<GenerationHistory> histories = generationHistoryRepository.findByTemplateId(1L);

    assertFalse(histories.isEmpty());
    assertEquals(1L, histories.get(0).getTemplateId());
  }

  @Test
  @DisplayName("测试删除历史记录")
  void should_delete_history() {
    GenerationHistory history = new GenerationHistory();
    history.setTemplateName("待删除");
    history.setRecordCount(10);
    history.setStatus("COMPLETED");
    GenerationHistory saved = generationHistoryRepository.save(history);

    generationHistoryRepository.deleteById(saved.getId());

    Optional<GenerationHistory> found = generationHistoryRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
