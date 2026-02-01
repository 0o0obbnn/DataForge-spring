package com.dataforge.web.service;

import com.dataforge.config.ForgeConfig;
import com.dataforge.service.DataForgeService;
import com.dataforge.web.entity.DataTemplate;
import com.dataforge.web.entity.GenerationHistory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncDataGenerationService {

  @Autowired private DataForgeService dataForgeService;

  @Autowired private GenerationHistoryService generationHistoryService;

  @Autowired private DataTemplateService dataTemplateService;

  private final ObjectMapper jsonMapper = new ObjectMapper();
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Async
  public CompletableFuture<GenerationHistory> generateDataAsync(
      String templateName, Integer recordCount, String templateConfig) {
    GenerationHistory history = createInitialHistory(null, templateName, recordCount);
    GenerationHistory savedHistory = generationHistoryService.createHistory(history);

    try {
      ForgeConfig config = parseConfigString(templateConfig);
      config.setCount(recordCount);
      dataForgeService.generateData(config);

      updateHistoryAsCompleted(savedHistory);
    } catch (Exception e) {
      updateHistoryAsFailed(savedHistory, e);
    }

    return CompletableFuture.completedFuture(
        generationHistoryService.updateHistory(savedHistory.getId(), savedHistory));
  }

  @Async
  public CompletableFuture<GenerationHistory> generateDataByTemplateIdAsync(
      Long templateId, Integer recordCount) {
    GenerationHistory history = createInitialHistory(templateId, null, recordCount);
    GenerationHistory savedHistory = generationHistoryService.createHistory(history);

    try {
      DataTemplate template = dataTemplateService.getTemplateById(templateId);
      ForgeConfig config = parseConfigString(template.getConfig());
      config.setCount(recordCount);
      dataForgeService.generateData(config);

      updateHistoryAsCompleted(savedHistory);
    } catch (Exception e) {
      updateHistoryAsFailed(savedHistory, e);
    }

    return CompletableFuture.completedFuture(
        generationHistoryService.updateHistory(savedHistory.getId(), savedHistory));
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
    long startTime = System.currentTimeMillis();
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

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
