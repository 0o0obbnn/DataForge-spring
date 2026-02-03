package com.dataforge.web.service;

import com.dataforge.config.ForgeConfig;
import com.dataforge.service.DataForgeService;
import com.dataforge.web.entity.DataTemplate;
import com.dataforge.web.entity.GenerationHistory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncDataGenerationService {

  private final DataForgeService dataForgeService;
  private final GenerationHistoryService generationHistoryService;
  private final DataTemplateService dataTemplateService;
  private final ObjectMapper jsonMapper = new ObjectMapper();
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  // 任务开始时间映射，用于正确计算耗时
  private final ConcurrentMap<Long, Long> taskStartTimes = new ConcurrentHashMap<>();

  public AsyncDataGenerationService(
      DataForgeService dataForgeService,
      GenerationHistoryService generationHistoryService,
      DataTemplateService dataTemplateService) {
    this.dataForgeService = dataForgeService;
    this.generationHistoryService = generationHistoryService;
    this.dataTemplateService = dataTemplateService;
  }

  @Async
  public CompletableFuture<GenerationHistory> generateDataAsync(
      String templateName, Integer recordCount, String templateConfig) {
    // 记录任务开始时间
    long startTime = System.currentTimeMillis();

    GenerationHistory history = createInitialHistory(null, templateName, recordCount);
    GenerationHistory savedHistory = generationHistoryService.createHistory(history);
    taskStartTimes.put(savedHistory.getId(), startTime);

    try {
      ForgeConfig config = parseConfigString(templateConfig);
      config.setCount(recordCount);
      dataForgeService.generateData(config);

      updateHistoryAsCompleted(savedHistory);
    } catch (Exception e) {
      updateHistoryAsFailed(savedHistory, e);
    }

    GenerationHistory result =
        generationHistoryService.updateHistory(savedHistory.getId(), savedHistory);
    taskStartTimes.remove(savedHistory.getId()); // 清理
    return CompletableFuture.completedFuture(result);
  }

  @Async
  public CompletableFuture<GenerationHistory> generateDataByTemplateIdAsync(
      Long templateId, Integer recordCount) {
    // 记录任务开始时间
    long startTime = System.currentTimeMillis();

    GenerationHistory history = createInitialHistory(templateId, null, recordCount);
    GenerationHistory savedHistory = generationHistoryService.createHistory(history);
    taskStartTimes.put(savedHistory.getId(), startTime);

    try {
      DataTemplate template = dataTemplateService.getTemplateById(templateId);
      ForgeConfig config = parseConfigString(template.getConfig());
      config.setCount(recordCount);
      dataForgeService.generateData(config);

      updateHistoryAsCompleted(savedHistory);
    } catch (Exception e) {
      updateHistoryAsFailed(savedHistory, e);
    }

    GenerationHistory result =
        generationHistoryService.updateHistory(savedHistory.getId(), savedHistory);
    taskStartTimes.remove(savedHistory.getId()); // 清理
    return CompletableFuture.completedFuture(result);
  }

  /**
   * 创建初始任务记录
   *
   * @param templateId 模板ID
   * @param templateName 模板名称
   * @param recordCount 记录数量
   * @return GenerationHistory对象
   */
  private GenerationHistory createInitialHistory(
      Long templateId, String templateName, Integer recordCount) {
    GenerationHistory history = new GenerationHistory();
    history.setTemplateId(templateId);
    history.setTemplateName(templateName);
    history.setRecordCount(recordCount);
    history.setStatus("IN_PROGRESS");
    return history;
  }

  /**
   * 更新任务状态为完成
   *
   * @param history 任务记录
   */
  private void updateHistoryAsCompleted(GenerationHistory history) {
    Long startTime = taskStartTimes.get(history.getId());
    if (startTime == null) {
      startTime = System.currentTimeMillis(); // 降级处理：使用当前时间
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime; // 正确计算耗时

    history.setStatus("COMPLETED");
    history.setDurationMs(duration);
    history.setCompletedAt(LocalDateTime.now());
  }

  /**
   * 更新任务状态为失败
   *
   * @param history 任务记录
   * @param e 异常
   */
  private void updateHistoryAsFailed(GenerationHistory history, Exception e) {
    history.setStatus("FAILED");
    history.setErrorMessage(e.getMessage());
    history.setCompletedAt(LocalDateTime.now());
  }

  /**
   * 解析配置字符串为ForgeConfig对象
   *
   * @param configString 配置字符串（JSON或YAML格式）
   * @return ForgeConfig对象
   */
  private ForgeConfig parseConfigString(String configString) throws Exception {
    if (configString == null || configString.trim().isEmpty()) {
      throw new IllegalArgumentException("配置字符串不能为空");
    }

    configString = configString.trim();

    if (configString.startsWith("{") || configString.startsWith("[")) {
      return jsonMapper.readValue(configString, ForgeConfig.class);
    } else {
      return yamlMapper.readValue(configString, ForgeConfig.class);
    }
  }
}
