package com.dataforge.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.dataforge.api.context.DataForgeContext;
import com.dataforge.api.generator.DataGenerator;
import com.dataforge.api.model.FieldConfig;
import com.dataforge.api.model.SimpleFieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleDataForgeService 测试")
class SimpleDataForgeServiceTest {

  @Mock
  private GeneratorFactory generatorFactory;

  @Mock
  private DataGenerator<Object, FieldConfig> mockGenerator;

  private SimpleDataForgeService service;

  @BeforeEach
  void setUp() {
    service = new SimpleDataForgeService(generatorFactory);
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应使用工厂生成数据")
    void shouldUseFactoryToGenerateData() {
      FieldConfig config = new SimpleFieldConfig("test", "string");
      when(generatorFactory.getGenerator("string")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), any())).thenReturn("generated");

      Object result = service.generate(config);

      assertThat(result).isEqualTo("generated");
      verify(mockGenerator).generate(any(), any());
    }

    @Test
    @DisplayName("应使用自定义上下文生成数据")
    void shouldUseCustomContextToGenerateData() {
      FieldConfig config = new SimpleFieldConfig("test", "string");
      DataForgeContext customContext = service.createContext();
      when(generatorFactory.getGenerator("string")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), eq(customContext))).thenReturn("generated");

      Object result = service.generate(config, customContext);

      assertThat(result).isEqualTo("generated");
      verify(mockGenerator).generate(any(), eq(customContext));
    }

    @Test
    @DisplayName("应创建默认上下文")
    void shouldCreateDefaultContext() {
      DataForgeContext context = service.createContext();

      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("应返回生成器工厂")
    void shouldReturnGeneratorFactory() {
      GeneratorFactory factory = service.getGeneratorFactory();

      assertThat(factory).isSameAs(generatorFactory);
    }
  }

  @Nested
  @DisplayName("批量生成测试")
  class BatchGenerationTests {

    @Test
    @DisplayName("批量生成应返回正确数量")
    void shouldGenerateCorrectBatchSize() {
      List<FieldConfig> configs =
          Arrays.asList(
              createConfig("field1", "string"),
              createConfig("field2", "number"));

      when(generatorFactory.getGenerator("string")).thenReturn(Optional.of(mockGenerator));
      when(generatorFactory.getGenerator("number")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), any())).thenAnswer(invocation -> {
        FieldConfig config = invocation.getArgument(0);
        return config.getType().equals("string") ? "test" : 123;
      });

      List<Map<String, Object>> results = service.generateBatch(configs, 100);

      assertThat(results).hasSize(100);
      results.forEach(
          row -> {
            assertThat(row).containsKeys("field1", "field2");
            assertThat(row.get("field1")).isEqualTo("test");
            assertThat(row.get("field2")).isEqualTo(123);
          });
    }

    @Test
    @DisplayName("批量生成应处理空配置列表")
    void shouldHandleEmptyConfigList() {
      List<FieldConfig> configs = List.of();

      List<Map<String, Object>> results = service.generateBatch(configs, 10);

      assertThat(results).hasSize(10);
      results.forEach(row -> assertThat(row).isEmpty());
    }

    @Test
    @DisplayName("批量生成应处理零数量")
    void shouldHandleZeroCount() {
      List<FieldConfig> configs = Arrays.asList(createConfig("field1", "string"));

      List<Map<String, Object>> results = service.generateBatch(configs, 0);

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("批量生成应处理单个配置")
    void shouldHandleSingleConfig() {
      List<FieldConfig> configs = Arrays.asList(createConfig("field1", "string"));
      when(generatorFactory.getGenerator("string")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), any())).thenReturn("value");

      List<Map<String, Object>> results = service.generateBatch(configs, 5);

      assertThat(results).hasSize(5);
      results.forEach(row -> {
        assertThat(row).containsKey("field1");
        assertThat(row.get("field1")).isEqualTo("value");
      });
    }

    @Test
    @DisplayName("批量生成应为每条记录创建独立上下文")
    void shouldCreateIndependentContextForEachRecord() {
      List<FieldConfig> configs = Arrays.asList(createConfig("field1", "string"));
      when(generatorFactory.getGenerator("string")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), any())).thenAnswer(invocation -> {
        DataForgeContext context = invocation.getArgument(1);
        return context.getString("recordId").orElse("default");
      });

      List<Map<String, Object>> results = service.generateBatch(configs, 3);

      assertThat(results).hasSize(3);
      results.forEach(row -> assertThat(row.get("field1")).isEqualTo("default"));
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("不支持的类型应抛出异常")
    void shouldThrowExceptionForUnsupportedType() {
      FieldConfig config = new SimpleFieldConfig("test", "unsupported");
      when(generatorFactory.getGenerator("unsupported")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.generate(config))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported generator type: unsupported");
    }

    @Test
    @DisplayName("null配置应抛出异常")
    void shouldThrowExceptionForNullConfig() {
      assertThatThrownBy(() -> service.generate(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("config cannot be null");
    }

    @Test
    @DisplayName("批量生成null配置列表应抛出异常")
    void shouldThrowExceptionForNullConfigList() {
      assertThatThrownBy(() -> service.generateBatch(null, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("configs cannot be null");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("应处理大量批量生成")
    void shouldHandleLargeBatchGeneration() {
      List<FieldConfig> configs = Arrays.asList(createConfig("field1", "string"));
      when(generatorFactory.getGenerator("string")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), any())).thenReturn("value");

      List<Map<String, Object>> results = service.generateBatch(configs, 10000);

      assertThat(results).hasSize(10000);
    }

    @Test
    @DisplayName("应处理多字段配置")
    void shouldHandleMultipleFieldConfigs() {
      List<FieldConfig> configs =
          Arrays.asList(
              createConfig("field1", "string"),
              createConfig("field2", "number"),
              createConfig("field3", "boolean"));

      when(generatorFactory.getGenerator("string")).thenReturn(Optional.of(mockGenerator));
      when(generatorFactory.getGenerator("number")).thenReturn(Optional.of(mockGenerator));
      when(generatorFactory.getGenerator("boolean")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), any())).thenAnswer(invocation -> {
        FieldConfig config = invocation.getArgument(0);
        return switch (config.getType()) {
          case "string" -> "test";
          case "number" -> 123;
          case "boolean" -> true;
          default -> null;
        };
      });

      List<Map<String, Object>> results = service.generateBatch(configs, 1);

      assertThat(results).hasSize(1);
      Map<String, Object> row = results.get(0);
      assertThat(row).containsKeys("field1", "field2", "field3");
      assertThat(row.get("field1")).isEqualTo("test");
      assertThat(row.get("field2")).isEqualTo(123);
      assertThat(row.get("field3")).isEqualTo(true);
    }

    @Test
    @DisplayName("应处理生成器返回null")
    void shouldHandleGeneratorReturningNull() {
      FieldConfig config = new SimpleFieldConfig("test", "null");
      when(generatorFactory.getGenerator("null")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), any())).thenReturn(null);

      Object result = service.generate(config);

      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("批量生成应在合理时间内完成")
    void shouldCompleteBatchGenerationInReasonableTime() {
      List<FieldConfig> configs = Arrays.asList(createConfig("field1", "string"));
      when(generatorFactory.getGenerator("string")).thenReturn(Optional.of(mockGenerator));
      when(mockGenerator.generate(any(), any())).thenReturn("value");

      long startTime = System.currentTimeMillis();
      List<Map<String, Object>> results = service.generateBatch(configs, 1000);
      long duration = System.currentTimeMillis() - startTime;

      assertThat(results).hasSize(1000);
      assertThat(duration).isLessThan(5000);
    }
  }

  private FieldConfig createConfig(String name, String type) {
    return new SimpleFieldConfig(name, type);
  }
}
