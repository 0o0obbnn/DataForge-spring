package com.dataforge.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.dataforge.config.FieldConfigWrapper;
import com.dataforge.config.OutputConfig;
import com.dataforge.web.entity.GenerationHistory;
import com.dataforge.web.exception.ResourceNotFoundException;
import com.dataforge.web.model.GenerateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@DisplayName("DataForgeController 测试")
public class DataForgeControllerTest extends BaseApiTest {

  @Autowired private ObjectMapper objectMapper;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.dataforge.service.DataForgeService dataForgeService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.dataforge.web.service.AsyncDataGenerationService asyncDataGenerationService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.dataforge.web.service.GenerationHistoryService generationHistoryService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private com.dataforge.web.service.MetricsService metricsService;

  @Nested
  @DisplayName("同步数据生成测试")
  class SyncGenerationTests {

    @Test
    @DisplayName("应成功生成数据")
    void shouldGenerateDataSuccessfully() throws Exception {
      GenerateRequest request = createValidGenerateRequest(10);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(MockMvcResultMatchers.status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.message").value("Data generated successfully"));

      verify(dataForgeService, times(1)).generateData(any());
      verify(metricsService, times(1)).recordGenerationRequest();
      verify(metricsService, times(1)).recordGenerationSuccess(eq(10L), anyLong());
    }

    @Test
    @DisplayName("应支持JSON格式输出")
    void shouldSupportJsonFormat() throws Exception {
      GenerateRequest request = createValidGenerateRequest(5);
      request.getOutput().setFormat(OutputConfig.Format.JSON);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("应支持CSV格式输出")
    void shouldSupportCsvFormat() throws Exception {
      GenerateRequest request = createValidGenerateRequest(5);
      request.getOutput().setFormat(OutputConfig.Format.CSV);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("应支持SQL格式输出")
    void shouldSupportSqlFormat() throws Exception {
      GenerateRequest request = createValidGenerateRequest(5);
      request.getOutput().setFormat(OutputConfig.Format.SQL);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("应支持多线程生成")
    void shouldSupportMultiThreadGeneration() throws Exception {
      GenerateRequest request = createValidGenerateRequest(100);
      request.setThreads(4);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("应支持随机种子")
    void shouldSupportRandomSeed() throws Exception {
      GenerateRequest request = createValidGenerateRequest(10);
      request.setSeed(12345L);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("应支持禁用验证")
    void shouldSupportDisableValidation() throws Exception {
      GenerateRequest request = createValidGenerateRequest(10);
      request.setValidate(false);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isOk());
    }
  }

  @Nested
  @DisplayName("参数验证测试")
  class ValidationTests {

    @Test
    @DisplayName("count为0时应返回400错误")
    void shouldReturn400WhenCountIsZero() throws Exception {
      GenerateRequest request = createValidGenerateRequest(0);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("count为负数时应返回400错误")
    void shouldReturn400WhenCountIsNegative() throws Exception {
      GenerateRequest request = createValidGenerateRequest(-10);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("output为null时应返回400错误")
    void shouldReturn400WhenOutputIsNull() throws Exception {
      GenerateRequest request = new GenerateRequest();
      request.setCount(10);
      request.setFields(Collections.singletonList(FieldConfigWrapper.of("name", "name")));
      request.setOutput(null);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("fields为空列表时应返回400错误")
    void shouldReturn400WhenFieldsIsEmpty() throws Exception {
      GenerateRequest request = new GenerateRequest();
      request.setCount(10);
      request.setFields(Collections.emptyList());
      request.setOutput(new OutputConfig());

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("threads为0时应返回400错误")
    void shouldReturn400WhenThreadsIsZero() throws Exception {
      GenerateRequest request = createValidGenerateRequest(10);
      request.setThreads(0);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("请求体为空时应返回400错误")
    void shouldReturn400WhenRequestBodyIsEmpty() throws Exception {
      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("缺少必需字段时应返回400错误")
    void shouldReturn400WhenMissingRequiredFields() throws Exception {
      String invalidRequest =
          """
          {
            "count": 10,
            "output": {
              "format": "JSON"
            }
          }
          """;

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(invalidRequest))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("异步数据生成测试")
  class AsyncGenerationTests {

    @Test
    @DisplayName("应成功提交异步任务")
    void shouldSubmitAsyncTaskSuccessfully() throws Exception {
      GenerateRequest request = createValidGenerateRequest(100);

      GenerationHistory history = createMockHistory(123L);
      CompletableFuture<GenerationHistory> future = CompletableFuture.completedFuture(history);

      when(asyncDataGenerationService.generateDataAsync(anyString(), anyInt(), anyString()))
          .thenReturn(future);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate/async")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.message").value("Task submitted successfully"))
          .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(123));
    }

    @Test
    @DisplayName("异步任务失败时应返回500错误")
    void shouldReturn500WhenAsyncTaskFails() throws Exception {
      GenerateRequest request = createValidGenerateRequest(100);

      CompletableFuture<GenerationHistory> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Task failed"));

      when(asyncDataGenerationService.generateDataAsync(anyString(), anyInt(), anyString()))
          .thenReturn(failedFuture);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate/async")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().is5xxServerError());
    }
  }

  @Nested
  @DisplayName("按模板生成测试")
  class TemplateGenerationTests {

    @Test
    @DisplayName("应按模板ID成功生成数据")
    void shouldGenerateDataByTemplateId() throws Exception {
      GenerateRequest request = new GenerateRequest();
      request.setCount(50);
      request.setOutput(new OutputConfig());
      request.setFields(Collections.singletonList(FieldConfigWrapper.of("name", "name")));

      GenerationHistory history = createMockHistory(456L);
      CompletableFuture<GenerationHistory> future = CompletableFuture.completedFuture(history);

      when(asyncDataGenerationService.generateDataByTemplateIdAsync(anyLong(), anyInt()))
          .thenReturn(future);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate/template/1")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
          .andExpect(MockMvcResultMatchers.jsonPath("$.data").value(456));
    }

    @Test
    @DisplayName("模板不存在时应返回400错误")
    void shouldReturn500WhenTemplateNotFound() throws Exception {
      GenerateRequest request = new GenerateRequest();
      request.setCount(50);
      request.setOutput(new OutputConfig());
      request.setFields(Collections.singletonList(FieldConfigWrapper.of("name", "name")));

      CompletableFuture<GenerationHistory> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(
          new IllegalArgumentException("Template not found with id: 999"));

      when(asyncDataGenerationService.generateDataByTemplateIdAsync(eq(999L), anyInt()))
          .thenReturn(failedFuture);

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate/template/999")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("任务状态查询测试")
  class TaskStatusTests {

    @Test
    @DisplayName("应成功获取任务状态")
    void shouldGetTaskStatusSuccessfully() throws Exception {
      GenerationHistory history = createMockHistory(123L);
      history.setStatus("COMPLETED");

      when(generationHistoryService.getHistoryById(123L)).thenReturn(history);

      mockMvc
          .perform(
              MockMvcRequestBuilders.get("/api/v1/dataforge/tasks/123")
                  .header("Authorization", getAuthToken()))
          .andExpect(MockMvcResultMatchers.status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.message")
                  .value("Task status retrieved successfully"))
          .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(123))
          .andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("任务不存在时应返回404错误")
    void shouldReturn404WhenTaskNotFound() throws Exception {
      when(generationHistoryService.getHistoryById(999L))
          .thenThrow(new ResourceNotFoundException("Generation history with id '999' not found"));

      mockMvc
          .perform(
              MockMvcRequestBuilders.get("/api/v1/dataforge/tasks/999")
                  .header("Authorization", getAuthToken()))
          .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("应成功获取最近任务列表")
    void shouldGetRecentTasksSuccessfully() throws Exception {
      List<GenerationHistory> histories =
          List.of(createMockHistory(1L), createMockHistory(2L), createMockHistory(3L));

      when(generationHistoryService.getRecentHistories(any(Pageable.class))).thenReturn(histories);

      mockMvc
          .perform(
              MockMvcRequestBuilders.get("/api/v1/dataforge/tasks")
                  .header("Authorization", getAuthToken()))
          .andExpect(MockMvcResultMatchers.status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
          .andExpect(
              MockMvcResultMatchers.jsonPath("$.message")
                  .value("Recent tasks retrieved successfully"))
          .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
          .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("应返回空列表当没有任务时")
    void shouldReturnEmptyListWhenNoTasks() throws Exception {
      when(generationHistoryService.getRecentHistories(any(Pageable.class)))
          .thenReturn(Collections.emptyList());

      mockMvc
          .perform(
              MockMvcRequestBuilders.get("/api/v1/dataforge/tasks")
                  .header("Authorization", getAuthToken()))
          .andExpect(MockMvcResultMatchers.status().isOk())
          .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
          .andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
          .andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(0));
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("服务异常时应记录失败指标")
    void shouldRecordFailureMetricWhenServiceThrowsException() throws Exception {
      GenerateRequest request = createValidGenerateRequest(10);

      org.mockito.Mockito.doThrow(new RuntimeException("Service error"))
          .when(dataForgeService)
          .generateData(any());

      mockMvc
          .perform(
              MockMvcRequestBuilders.post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(MockMvcResultMatchers.status().is5xxServerError());

      verify(metricsService, times(1)).recordGenerationRequest();
      verify(metricsService, times(1)).recordGenerationFailure();
    }
  }

  private GenerateRequest createValidGenerateRequest(int count) {
    GenerateRequest request = new GenerateRequest();
    request.setCount(count);
    request.setValidate(true);
    request.setThreads(1);

    OutputConfig outputConfig = new OutputConfig();
    outputConfig.setFormat(OutputConfig.Format.JSON);
    request.setOutput(outputConfig);

    request.setFields(Collections.singletonList(FieldConfigWrapper.of("name", "name")));

    return request;
  }

  private GenerationHistory createMockHistory(Long id) {
    GenerationHistory history = new GenerationHistory();
    history.setId(id);
    history.setTemplateName("test-template");
    history.setStatus("COMPLETED");
    history.setRecordCount(100);
    history.setCreatedAt(LocalDateTime.now());
    history.setCompletedAt(LocalDateTime.now());
    return history;
  }
}
