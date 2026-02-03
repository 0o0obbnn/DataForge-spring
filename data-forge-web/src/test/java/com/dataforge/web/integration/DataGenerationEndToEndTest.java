package com.dataforge.web.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dataforge.config.ForgeConfig;
import com.dataforge.web.api.BaseApiTest;
import com.dataforge.web.entity.DataTemplate;
import com.dataforge.web.entity.GenerationHistory;
import com.dataforge.web.model.ApiResponse;
import com.dataforge.web.model.GenerateRequest;
import com.dataforge.web.repository.DataTemplateRepository;
import com.dataforge.web.repository.GenerationHistoryRepository;
import com.dataforge.web.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 数据生成端到端集成测试。
 *
 * <p>测试完整的数据生成流程，包括模板创建、数据生成、结果验证等。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("数据生成端到端集成测试")
public class DataGenerationEndToEndTest extends BaseApiTest {

  @Autowired private ObjectMapper objectMapper;

  @Autowired private DataTemplateRepository templateRepository;

  @Autowired private GenerationHistoryRepository historyRepository;

  @AfterEach
  void tearDown() {
    historyRepository.deleteAll();
    templateRepository.deleteAll();
  }

  @Nested
  @DisplayName("完整生成流程测试")
  class FullGenerationFlowTests {

    @Test
    @DisplayName("应完成完整生成流程")
    void shouldCompleteFullGenerationFlow() throws Exception {
      int recordCount = 100;

      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(recordCount);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.message").value("Data generated successfully"))
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);

      List<GenerationHistory> histories = historyRepository.findAll();
      assertThat(histories).isNotEmpty();
    }

    @Test
    @DisplayName("应生成包含多种类型的数据")
    void shouldGenerateDataWithMultipleTypes() throws Exception {
      int recordCount = 50;

      GenerateRequest request = TestDataFactory.createComplexGenerateRequest(recordCount);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("应支持JSON格式输出")
    void shouldSupportJsonFormatOutput() throws Exception {
      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(10);
      request.setOutput(TestDataFactory.createJsonOutputConfig(null));

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("应支持CSV格式输出")
    void shouldSupportCsvFormatOutput() throws Exception {
      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(10);
      request.setOutput(TestDataFactory.createCsvOutputConfig(null));

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("应支持YAML格式输出")
    void shouldSupportYamlFormatOutput() throws Exception {
      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(10);
      request.setOutput(TestDataFactory.createYamlOutputConfig(null));

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
    }
  }

  @Nested
  @DisplayName("异步生成流程测试")
  class AsyncGenerationFlowTests {

    @Test
    @DisplayName("应成功提交异步任务")
    void shouldSubmitAsyncTaskSuccessfully() throws Exception {
      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(100);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate/async")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.message").value("Task submitted successfully"))
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
      assertThat(response.getData()).isNotNull();
    }

    @Test
    @DisplayName("应能查询异步任务状态")
    void shouldQueryAsyncTaskStatus() throws Exception {
      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(50);

      MvcResult submitResult =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate/async")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> submitResponse =
          objectMapper.readValue(
              submitResult.getResponse().getContentAsString(), ApiResponse.class);

      Long taskId = ((Number) submitResponse.getData()).longValue();

      mockMvc
          .perform(get("/api/v1/dataforge/tasks/" + taskId).header("Authorization", getAuthToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data.id").value(taskId));
    }

    @Test
    @DisplayName("应能获取最近任务列表")
    void shouldGetRecentTasksList() throws Exception {
      mockMvc
          .perform(get("/api/v1/dataforge/tasks").header("Authorization", getAuthToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.message").value("Recent tasks retrieved successfully"))
          .andExpect(jsonPath("$.data").isArray());
    }
  }

  @Nested
  @DisplayName("模板生成流程测试")
  class TemplateGenerationFlowTests {

    @Test
    @DisplayName("应能创建并使用模板生成数据")
    void shouldCreateAndUseTemplate() throws Exception {
      DataTemplate template = new DataTemplate();
      template.setName("test-template");
      template.setDescription("Test template for E2E");
      ForgeConfig forgeConfig = new ForgeConfig();
      forgeConfig.setFields(
          List.of(
              TestDataFactory.createField("id", "uuid"),
              TestDataFactory.createField("name", "string"),
              TestDataFactory.createField("email", "email")));
      forgeConfig.setOutput(TestDataFactory.createOutputConfig());
      template.setConfig(objectMapper.writeValueAsString(forgeConfig));
      template.setActive(true);

      DataTemplate savedTemplate = templateRepository.saveAndFlush(template);

      // 验证模板确实被保存了
      assertThat(savedTemplate.getId()).isNotNull();
      DataTemplate verifiedTemplate = templateRepository.findById(savedTemplate.getId()).orElse(null);
      assertThat(verifiedTemplate).isNotNull();
      assertThat(verifiedTemplate.getName()).isEqualTo("test-template");

      GenerateRequest request = new GenerateRequest();
      request.setCount(50);
      request.setOutput(TestDataFactory.createOutputConfig());
      // 添加字段配置以满足验证要求
      request.setFields(List.of(
          TestDataFactory.createField("test", "string")
      ));

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate/template/" + savedTemplate.getId())
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("应能获取模板列表")
    void shouldGetTemplateList() throws Exception {
      DataTemplate template1 = new DataTemplate();
      template1.setName("template-1");
      template1.setDescription("Template 1");
      ForgeConfig config1 = new ForgeConfig();
      config1.setFields(List.of(TestDataFactory.createField("name", "string")));
      config1.setOutput(TestDataFactory.createOutputConfig());
      template1.setConfig(objectMapper.writeValueAsString(config1));
      template1.setActive(true);

      DataTemplate template2 = new DataTemplate();
      template2.setName("template-2");
      template2.setDescription("Template 2");
      ForgeConfig config2 = new ForgeConfig();
      config2.setFields(List.of(TestDataFactory.createField("email", "email")));
      config2.setOutput(TestDataFactory.createOutputConfig());
      template2.setConfig(objectMapper.writeValueAsString(config2));
      template2.setActive(true);

      templateRepository.saveAll(List.of(template1, template2));

      mockMvc
          .perform(
              get("/api/v1/templates/page")
                  .header("Authorization", getAuthToken())
                  .param("page", "0")
                  .param("size", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data.content").isArray())
          .andExpect(jsonPath("$.data.content.length()").value(2));
    }
  }

  @Nested
  @DisplayName("批量生成流程测试")
  class BatchGenerationFlowTests {

    @Test
    @DisplayName("应支持批量生成请求")
    void shouldSupportBatchGeneration() throws Exception {
      int batchSize = 10;
      int recordsPerRequest = 20;

      for (int i = 0; i < batchSize; i++) {
        GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(recordsPerRequest);

        mockMvc
            .perform(
                post("/api/v1/dataforge/generate")
                    .header("Authorization", getAuthToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
      }

      List<GenerationHistory> histories = historyRepository.findAll();
      assertThat(histories).hasSize(batchSize);
    }

    @Test
    @DisplayName("应支持大批量数据生成")
    void shouldSupportLargeBatchGeneration() throws Exception {
      int largeCount = 1000;

      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(largeCount);
      request.setThreads(4);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
    }
  }

  @Nested
  @DisplayName("错误处理流程测试")
  class ErrorHandlingFlowTests {

    @Test
    @DisplayName("应正确处理无效请求")
    void shouldHandleInvalidRequest() throws Exception {
      GenerateRequest request = new GenerateRequest();
      request.setCount(-1);

      mockMvc
          .perform(
              post("/api/v1/dataforge/generate")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("应正确处理不存在的模板")
    void shouldHandleNonExistentTemplate() throws Exception {
      GenerateRequest request = new GenerateRequest();
      request.setCount(10);
      request.setOutput(TestDataFactory.createOutputConfig());

      mockMvc
          .perform(
              post("/api/v1/dataforge/generate/template/99999")
                  .header("Authorization", getAuthToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("应正确处理不存在的任务")
    void shouldHandleNonExistentTask() throws Exception {
      mockMvc
          .perform(get("/api/v1/dataforge/tasks/99999").header("Authorization", getAuthToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(404));
    }
  }

  @Nested
  @DisplayName("数据验证流程测试")
  class DataValidationFlowTests {

    @Test
    @DisplayName("应支持启用数据验证")
    void shouldSupportDataValidation() throws Exception {
      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(10);
      request.setValidate(true);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("应支持禁用数据验证")
    void shouldSupportDisableDataValidation() throws Exception {
      GenerateRequest request = TestDataFactory.createSimpleGenerateRequest(10);
      request.setValidate(false);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/dataforge/generate")
                      .header("Authorization", getAuthToken())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andReturn();

      ApiResponse<?> response =
          objectMapper.readValue(result.getResponse().getContentAsString(), ApiResponse.class);

      assertThat(response).isNotNull();
      assertThat(response.getCode()).isEqualTo(200);
    }
  }
}
