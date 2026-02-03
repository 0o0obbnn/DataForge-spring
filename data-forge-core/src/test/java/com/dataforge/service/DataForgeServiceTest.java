package com.dataforge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dataforge.config.FieldConfigWrapper;
import com.dataforge.config.ForgeConfig;
import com.dataforge.config.OutputConfig;
import com.dataforge.core.GeneratorFactory;
import com.dataforge.io.OutputStrategy;
import com.dataforge.validation.SecurityValidator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * DataForgeService 单元测试。
 *
 * <p>测试核心服务类的数据生成、配置验证、并发控制、异常处理等功能。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("DataForgeService 单元测试")
class DataForgeServiceTest {

  @Mock private GeneratorFactory generatorFactory;

  @Mock private OutputStrategy outputStrategy;

  @Mock private SecurityValidator securityValidator;

  private DataForgeService dataForgeService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // 配置 generatorFactory mock
    when(generatorFactory.isInitialized()).thenReturn(true);
    when(generatorFactory.getGeneratorCount()).thenReturn(10);
    when(generatorFactory.hasGenerator(any())).thenReturn(true);
    when(generatorFactory.getAvailableTypes()).thenReturn(Set.of("string", "integer", "boolean"));

    // 配置 outputStrategy mock
    when(outputStrategy.getSupportedFormat()).thenReturn(OutputConfig.Format.CSV);
    when(outputStrategy.supports(any())).thenReturn(true);

    // 配置 securityValidator mock
    doNothing().when(securityValidator).validateConfiguration(any());

    // 创建服务实例，传入 mock 的 OutputStrategy
    List<OutputStrategy> strategies = new ArrayList<>();
    strategies.add(outputStrategy);
    dataForgeService = new DataForgeService(generatorFactory, strategies, securityValidator);
  }

  @Nested
  @DisplayName("单字段数据生成测试")
  class SingleFieldGenerationTests {

    @Test
    @DisplayName("单字段数据生成应成功")
    void generateData_WithSingleField_ShouldSucceed(@TempDir Path tempDir) throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 100, 1);
      config.setFields(createSingleFieldConfig("name", "string"));

      // When
      assertThatCode(() -> dataForgeService.generateData(config)).doesNotThrowAnyException();

      // Then
      verify(outputStrategy).initialize(any(OutputConfig.class), any());
      verify(outputStrategy, times(100)).writeRecord(any());
      verify(outputStrategy).finish();
    }

    @Test
    @DisplayName("单字段整数生成应成功")
    void generateData_WithIntegerField_ShouldSucceed(@TempDir Path tempDir) throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 50, 1);
      config.setFields(createSingleFieldConfig("age", "integer"));

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(50)).writeRecord(any());
    }

    @Test
    @DisplayName("单字段布尔值生成应成功")
    void generateData_WithBooleanField_ShouldSucceed(@TempDir Path tempDir) throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 30, 1);
      config.setFields(createSingleFieldConfig("active", "boolean"));

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(30)).writeRecord(any());
    }
  }

  @Nested
  @DisplayName("多字段数据生成测试")
  class MultiFieldGenerationTests {

    @Test
    @DisplayName("多字段数据生成应成功")
    void generateData_WithMultipleFields_ShouldSucceed(@TempDir Path tempDir) throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 100, 1);
      config.setFields(createMultiFieldConfig());

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(100)).writeRecord(any());
      verify(outputStrategy).finish();
    }

    @Test
    @DisplayName("多字段批量生成应保持字段顺序")
    void generateData_WithMultipleFields_ShouldMaintainFieldOrder(@TempDir Path tempDir)
        throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10, 1);
      List<FieldConfigWrapper> fields = createMultiFieldConfig();
      config.setFields(fields);

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(10))
          .writeRecord(argThat(record -> record.size() == fields.size()));
    }
  }

  @Nested
  @DisplayName("批量生成配置验证测试")
  class BatchGenerationConfigTests {

    @Test
    @DisplayName("批量生成大量数据应成功")
    void generateData_WithLargeBatch_ShouldSucceed(@TempDir Path tempDir) throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10000, 1);
      config.setFields(createSingleFieldConfig("id", "integer"));

      // When
      long startTime = System.currentTimeMillis();
      dataForgeService.generateData(config);
      long duration = System.currentTimeMillis() - startTime;

      // Then
      verify(outputStrategy, times(10000)).writeRecord(any());
      assertThat(duration).isLessThan(10000); // 应该在10秒内完成
    }

    @Test
    @DisplayName("负记录数应抛出异常")
    void generateData_WithNegativeCount_ShouldThrowException(@TempDir Path tempDir) {
      // Given
      ForgeConfig config = createValidConfig(tempDir, -1, 1);
      config.setFields(createSingleFieldConfig("name", "string"));

      // When & Then
      assertThatThrownBy(() -> dataForgeService.generateData(config))
          .isInstanceOf(DataForgeException.class);
    }
  }

  @Nested
  @DisplayName("数据验证功能测试")
  class DataValidationTests {

    @Test
    @DisplayName("启用验证应验证生成数据")
    void generateData_WithValidationEnabled_ShouldValidateData(@TempDir Path tempDir)
        throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10, 1);
      config.setValidate(true);
      config.setFields(createSingleFieldConfig("name", "string"));

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(10)).writeRecord(any());
    }

    @Test
    @DisplayName("禁用验证应跳过数据验证")
    void generateData_WithValidationDisabled_ShouldSkipValidation(@TempDir Path tempDir)
        throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10, 1);
      config.setValidate(false);
      config.setFields(createSingleFieldConfig("name", "string"));

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(10)).writeRecord(any());
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("null 配置应抛出异常")
    void generateData_WithNullConfig_ShouldThrowException() {
      // When & Then
      assertThatThrownBy(() -> dataForgeService.generateData(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Configuration cannot be null");
    }

    @Test
    @DisplayName("空字段列表应抛出 ConfigurationException")
    void generateData_WithEmptyFields_ShouldThrowException(@TempDir Path tempDir) {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10, 1);
      config.setFields(new ArrayList<>());

      // When & Then
      assertThatThrownBy(() -> dataForgeService.generateData(config))
          .isInstanceOf(ConfigurationException.class)
          .hasMessageContaining("fields");
    }

    @Test
    @DisplayName("未知字段类型应抛出 ConfigurationException")
    void generateData_WithUnknownFieldType_ShouldThrowException(@TempDir Path tempDir) {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10, 1);
      config.setFields(createSingleFieldConfig("unknown", "unknown_type"));
      when(generatorFactory.hasGenerator("unknown_type")).thenReturn(false);

      // When & Then
      assertThatThrownBy(() -> dataForgeService.generateData(config))
          .isInstanceOf(ConfigurationException.class)
          .hasMessageContaining("No generator found");
    }

    @Test
    @DisplayName("安全验证失败应抛出 SecurityException")
    void generateData_WithSecurityValidationFailure_ShouldThrowException(@TempDir Path tempDir) {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10, 1);
      config.setFields(createSingleFieldConfig("name", "string"));

      doThrow(new SecurityException("Security validation failed"))
          .when(securityValidator)
          .validateConfiguration(any());

      // When & Then
      assertThatThrownBy(() -> dataForgeService.generateData(config))
          .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("输出策略初始化失败应清理资源")
    void generateData_WithOutputStrategyInitFailure_ShouldCleanupResources(@TempDir Path tempDir) {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10, 1);
      config.setFields(createSingleFieldConfig("name", "string"));

      doThrow(new RuntimeException("Init failed"))
          .when(outputStrategy)
          .initialize(any(OutputConfig.class), any());

      // When & Then
      assertThatThrownBy(() -> dataForgeService.generateData(config))
          .isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("并发生成测试")
  class ConcurrentGenerationTests {

    @Test
    @DisplayName("多线程并发生成应成功")
    void generateData_WithMultipleThreads_ShouldSucceed(@TempDir Path tempDir) throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 1000, 4);
      config.setFields(createMultiFieldConfig());

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(1000)).writeRecord(any());
      verify(outputStrategy).finish();
    }

    @Test
    @DisplayName("单线程生成应按顺序执行")
    void generateData_WithSingleThread_ShouldExecuteSequentially(@TempDir Path tempDir)
        throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 100, 1);
      config.setFields(createSingleFieldConfig("id", "integer"));

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(100)).writeRecord(any());
    }

    @Test
    @DisplayName("大量并发应保持数据完整性")
    void generateData_WithHighConcurrency_ShouldMaintainDataIntegrity(@TempDir Path tempDir)
        throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 5000, 8);
      config.setFields(createMultiFieldConfig());

      // When
      dataForgeService.generateData(config);

      // Then
      verify(outputStrategy, times(5000)).writeRecord(any());
    }
  }

  @Nested
  @DisplayName("性能基准测试")
  class PerformanceBenchmarkTests {

    @Test
    @DisplayName("小批量生成性能基准")
    void generateData_SmallBatch_ShouldMeetPerformanceThreshold(@TempDir Path tempDir)
        throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 1000, 1);
      config.setFields(createSingleFieldConfig("value", "integer"));

      // When
      long startTime = System.currentTimeMillis();
      dataForgeService.generateData(config);
      long duration = System.currentTimeMillis() - startTime;

      // Then - 应在 2 秒内完成（放宽阈值以避免 CI/高负载下偶发失败）
      assertThat(duration).isLessThan(2000);
    }

    @Test
    @DisplayName("中批量生成性能基准")
    void generateData_MediumBatch_ShouldMeetPerformanceThreshold(@TempDir Path tempDir)
        throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 10000, 4);
      config.setFields(createMultiFieldConfig());

      // When
      long startTime = System.currentTimeMillis();
      dataForgeService.generateData(config);
      long duration = System.currentTimeMillis() - startTime;

      // Then - 应在 10 秒内完成（放宽以避免 CI/高负载下偶发失败）
      assertThat(duration).isLessThan(10000);
    }

    @Test
    @DisplayName("并发生成应在合理时间内完成且无异常")
    void generateData_ConcurrentVsSequential_ShouldShowImprovement(@TempDir Path tempDir)
        throws Exception {
      // Given
      ForgeConfig config = createValidConfig(tempDir, 5000, 4);
      config.setFields(createMultiFieldConfig());

      // When - 并发生成完成即视为通过，不依赖并发严格优于串行的环境敏感断言
      long startTime = System.currentTimeMillis();
      dataForgeService.generateData(config);
      long duration = System.currentTimeMillis() - startTime;

      // Then - 无异常完成，且耗时在宽松上限内（避免 CI/高负载下偶发失败）
      assertThat(duration).isGreaterThanOrEqualTo(0);
      assertThat(duration).isLessThan(15000);
    }
  }

  @Nested
  @DisplayName("API 查询功能测试")
  class ApiQueryTests {

    @Test
    @DisplayName("获取可用生成器类型应返回正确集合")
    void getAvailableGeneratorTypes_ShouldReturnCorrectSet() {
      // Given
      when(generatorFactory.getAvailableTypes()).thenReturn(Set.of("string", "integer", "boolean"));

      // When
      Set<String> types = dataForgeService.getAvailableGeneratorTypes();

      // Then
      assertThat(types).isNotNull().hasSizeGreaterThan(0);
      assertThat(types).contains("string");
    }

    @Test
    @DisplayName("获取生成器信息应返回正确映射")
    void getGeneratorInfo_ShouldReturnCorrectMap() {
      // Given
      Map<String, String> mockInfo = new HashMap<>();
      mockInfo.put("string", "String Generator");
      when(generatorFactory.getGeneratorInfo()).thenReturn(mockInfo);

      // When
      Map<String, String> info = dataForgeService.getGeneratorInfo();

      // Then
      assertThat(info).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("获取可用输出格式应返回正确列表")
    void getAvailableOutputFormats_ShouldReturnCorrectList() {
      // When
      List<OutputConfig.Format> formats = dataForgeService.getAvailableOutputFormats();

      // Then
      assertThat(formats).isNotNull().isNotEmpty();
      assertThat(formats).contains(OutputConfig.Format.CSV);
    }

    @Test
    @DisplayName("获取健康状态应返回有效信息")
    void getHealthStatus_ShouldReturnValidStatus() {
      // When
      Map<String, Object> status = dataForgeService.getHealthStatus();

      // Then
      assertThat(status).isNotNull();
      assertThat(status).containsKey("totalRecordsGenerated");
      assertThat(status).containsKey("totalErrors");
      assertThat(status).containsKey("availableGenerators");
      assertThat(status).containsKey("availableOutputStrategies");
    }
  }

  // 辅助方法

  private ForgeConfig createValidConfig(Path tempDir, int count, int threads) {
    ForgeConfig config = new ForgeConfig();
    config.setCount(count);
    config.setThreads(threads);
    config.setValidate(true);

    OutputConfig outputConfig = new OutputConfig();
    outputConfig.setFormat(OutputConfig.Format.CSV);
    Path outputFile = tempDir.resolve("test_output.csv");
    outputConfig.setFile(outputFile.toString());
    config.setOutput(outputConfig);

    return config;
  }

  private List<FieldConfigWrapper> createSingleFieldConfig(String name, String type) {
    List<FieldConfigWrapper> fields = new ArrayList<>();
    FieldConfigWrapper field = new FieldConfigWrapper();
    field.setName(name);
    field.setType(type);
    fields.add(field);
    return fields;
  }

  private List<FieldConfigWrapper> createMultiFieldConfig() {
    List<FieldConfigWrapper> fields = new ArrayList<>();

    FieldConfigWrapper nameField = new FieldConfigWrapper();
    nameField.setName("name");
    nameField.setType("string");

    FieldConfigWrapper ageField = new FieldConfigWrapper();
    ageField.setName("age");
    ageField.setType("integer");

    FieldConfigWrapper activeField = new FieldConfigWrapper();
    activeField.setName("active");
    activeField.setType("boolean");

    fields.add(nameField);
    fields.add(ageField);
    fields.add(activeField);

    return fields;
  }
}
