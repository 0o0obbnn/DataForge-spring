package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.EnhancedGeneratorTestBase;
import com.dataforge.model.SimpleFieldConfig;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * YAML生成器全面测试 - 基于现代Java测试模式
 *
 * <p>覆盖YamlGenerator的所有业务逻辑，包括边界条件、性能测试和异常处理
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("YamlGenerator 全面测试")
class YamlGeneratorComprehensiveTest extends EnhancedGeneratorTestBase<YamlGenerator> {

  @Override
  protected YamlGenerator createGenerator() {
    return new YamlGenerator();
  }

  @Override
  protected String getGeneratorType() {
    return "yaml";
  }

  @Nested
  @DisplayName("结构类型测试")
  class StructureTypeTests {

    @Test
    @DisplayName("应生成简单结构YAML")
    void shouldGenerateSimpleStructure() {
      config.setParam("structure", "SIMPLE");
      config.setParam("key_count", "3");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains(":").doesNotContain("  "); // 无缩进

      // 验证键值对数量
      long keyCount = yaml.lines().filter(line -> line.contains(":")).count();
      assertThat(keyCount).isEqualTo(3);
    }

    @Test
    @DisplayName("应生成嵌套结构YAML")
    void shouldGenerateNestedStructure() {
      config.setParam("structure", "NESTED");
      config.setParam("depth", "3");

      String yaml = generator.generate(config, context);

      assertThat(yaml)
          .contains("  ") // 有缩进
          .contains("\n");

      // 验证嵌套深度
      long maxIndent =
          yaml.lines()
              .filter(line -> !line.trim().isEmpty())
              .mapToLong(line -> line.chars().takeWhile(c -> c == ' ').count())
              .max()
              .orElse(0);
      assertThat(maxIndent).isGreaterThanOrEqualTo(4); // 至少2级缩进
    }

    @Test
    @DisplayName("应生成列表结构YAML")
    void shouldGenerateListStructure() {
      config.setParam("structure", "LIST");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("- ");

      // 验证列表项数量
      long itemCount = yaml.lines().filter(line -> line.trim().startsWith("-")).count();
      assertThat(itemCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("应生成复杂结构YAML")
    void shouldGenerateComplexStructure() {
      config.setParam("structure", "COMPLEX");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("config:").contains("database:").contains("features:");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIMPLE", "NESTED", "LIST", "COMPLEX"})
    @DisplayName("应支持所有结构类型")
    void shouldSupportAllStructureTypes(String structureType) {
      config.setParam("structure", structureType);

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull().isNotEmpty();
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10})
    @DisplayName("应支持有效深度范围")
    void shouldSupportValidDepthRange(int depth) {
      config.setParam("structure", "NESTED");
      config.setParam("depth", String.valueOf(depth));

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, 100})
    @DisplayName("应处理超出范围的深度")
    void shouldHandleOutOfRangeDepth(int depth) {
      config.setParam("structure", "NESTED");
      config.setParam("depth", String.valueOf(depth));

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull(); // 不应抛出异常
    }

    @Test
    @DisplayName("应生成无效YAML用于测试")
    void shouldGenerateInvalidYaml() {
      config.setParam("structure", "SIMPLE");
      config.setParam("invalid_yaml", "true");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      // 验证生成的YAML确实无效
      assertThat(isValidYaml(yaml)).isFalse();
    }

    private boolean isValidYaml(String yaml) {
      // 简化验证：检查是否缺少冒号
      return yaml.lines()
          .filter(line -> !line.trim().isEmpty())
          .allMatch(line -> line.contains(":"));
    }

    @Test
    @DisplayName("应处理超大键值对数量")
    void shouldHandleLargeKeyCount() {
      config.setParam("structure", "SIMPLE");
      config.setParam("key_count", "1000");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      // 验证实际生成的数量在合理范围内
      long actualKeyCount = yaml.lines().filter(line -> line.contains(":")).count();
      assertThat(actualKeyCount).isBetween(1L, 1000L);
    }
  }

  @Nested
  @DisplayName("值类型测试")
  class ValueTypeTests {

    @Test
    @DisplayName("应生成字符串值")
    void shouldGenerateStringValues() {
      config.setParam("value_types", "STRING");

      String yaml = generator.generate(config, context);

      assertThat(yaml).contains("\"");
    }

    @Test
    @DisplayName("应生成数值")
    void shouldGenerateNumberValues() {
      config.setParam("value_types", "NUMBER");

      String yaml = generator.generate(config, context);

      assertThat(yaml).containsPattern(": \\d+");
    }

    @Test
    @DisplayName("应生成布尔值")
    void shouldGenerateBooleanValues() {
      config.setParam("value_types", "BOOLEAN");

      String yaml = generator.generate(config, context);

      assertThat(yaml)
          .satisfiesAnyOf(
              y -> assertThat(y).contains("true"), y -> assertThat(y).contains("false"));
    }

    @Test
    @DisplayName("应生成null值")
    void shouldGenerateNullValues() {
      config.setParam("value_types", "NULL");

      String yaml = generator.generate(config, context);

      assertThat(yaml.toLowerCase()).contains("null");
    }

    @ParameterizedTest
    @CsvSource({"STRING,NUMBER", "STRING,BOOLEAN", "NUMBER,BOOLEAN", "STRING,NUMBER,BOOLEAN"})
    @DisplayName("应支持值类型组合")
    void shouldSupportValueTypeCombinations(String valueTypes) {
      config.setParam("value_types", valueTypes);

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull().isNotEmpty();
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("应快速生成简单YAML")
    void shouldGenerateSimpleYamlQuickly() {
      config.setParam("structure", "SIMPLE");

      // 测试1000次生成性能
      long start = System.nanoTime();

      try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        var futures =
            IntStream.range(0, 1000)
                .mapToObj(i -> executor.submit(() -> generator.generate(config, context)))
                .toList();

        futures.forEach(
            future -> {
              try {
                String yaml = future.get();
                assertThat(yaml).isNotNull();
              } catch (Exception e) {
                throw new RuntimeException("Performance test failed", e);
              }
            });
      }

      long duration = System.nanoTime() - start;
      assertThat(java.time.Duration.ofNanos(duration)).isLessThan(java.time.Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("应高效处理复杂结构")
    void shouldHandleComplexStructureEfficiently() {
      config.setParam("structure", "COMPLEX");
      config.setParam("depth", "5");

      // 测试100次复杂结构生成性能
      long start = System.nanoTime();

      for (int i = 0; i < 100; i++) {
        String yaml = generator.generate(config, context);
        assertThat(yaml).isNotNull();
      }

      long duration = System.nanoTime() - start;
      assertThat(java.time.Duration.ofNanos(duration)).isLessThan(java.time.Duration.ofSeconds(3));
    }
  }

  @Nested
  @DisplayName("并发测试")
  class ConcurrencyTests {

    @Test
    @DisplayName("应支持并发生成")
    void shouldSupportConcurrentGeneration() {
      config.setParam("structure", "SIMPLE");

      Set<String> generatedYamls = ConcurrentHashMap.newKeySet();

      try (var executor = java.util.concurrent.Executors.newFixedThreadPool(10)) {
        var futures =
            IntStream.range(0, 100)
                .mapToObj(
                    i ->
                        executor.submit(
                            () -> {
                              String yaml = generator.generate(config, context);
                              generatedYamls.add(yaml);
                              return yaml;
                            }))
                .toList();

        futures.forEach(
            future -> {
              try {
                String yaml = future.get();
                assertThat(yaml).isNotNull();
              } catch (Exception e) {
                throw new RuntimeException("Concurrency test failed", e);
              }
            });
      }

      assertThat(generatedYamls).hasSize(100);
      assertThat(generatedYamls).allMatch(yaml -> yaml != null && !yaml.isEmpty());
    }

    @Test
    @DisplayName("应处理并发参数修改")
    void shouldHandleConcurrentParameterModification() {
      try (var executor = java.util.concurrent.Executors.newFixedThreadPool(5)) {
        var futures =
            IntStream.range(0, 50)
                .mapToObj(
                    i ->
                        executor.submit(
                            () -> {
                              SimpleFieldConfig localConfig = new SimpleFieldConfig();
                              localConfig.setType("yaml");
                              localConfig.setParam("structure", i % 2 == 0 ? "SIMPLE" : "NESTED");
                              localConfig.setParam("key_count", String.valueOf(i + 1));

                              return generator.generate(localConfig, context);
                            }))
                .toList();

        futures.forEach(
            future -> {
              try {
                String yaml = future.get();
                assertThat(yaml).isNotNull();
              } catch (Exception e) {
                throw new RuntimeException("Concurrent parameter test failed", e);
              }
            });
      }
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("应处理无效结构类型")
    void shouldHandleInvalidStructureType() {
      config.setParam("structure", "INVALID_TYPE");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull(); // 应返回默认结构
    }

    @Test
    @DisplayName("应处理非数字参数")
    void shouldHandleNonNumericParameters() {
      config.setParam("structure", "SIMPLE");
      config.setParam("key_count", "not_a_number");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull(); // 应使用默认值
    }

    @Test
    @DisplayName("应处理空参数值")
    void shouldHandleEmptyParameterValues() {
      config.setParam("structure", "");
      config.setParam("key_count", "");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull(); // 应使用默认值
    }

    @Test
    @DisplayName("应处理特殊字符参数")
    void shouldHandleSpecialCharacterParameters() {
      config.setParam("structure", "SIMPLE");
      config.setParam("custom_key", "key-with-特殊字符-😊");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull();
      // 验证特殊字符被正确处理
      assertThat(yaml).contains("key-with");
    }
  }

  @Nested
  @DisplayName("集成测试")
  class IntegrationTests {

    @Test
    @DisplayName("应与上下文正确集成")
    void shouldIntegrateCorrectlyWithContext() {
      // 设置上下文参数
      context.put("template_name", "test_template");
      context.put("environment", "test");

      config.setParam("structure", "COMPLEX");
      config.setParam("use_context", "true");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull().contains("test_template").contains("test");
    }

    @Test
    @DisplayName("应支持参数模板")
    void shouldSupportParameterTemplates() {
      config.setParam("structure", "SIMPLE");
      config.setParam("template", "${key}: ${value}");

      String yaml = generator.generate(config, context);

      assertThat(yaml).isNotNull().containsPattern("\\w+:\\s+\\w+");
    }
  }

  @Test
  @DisplayName("应通过基本功能测试套件")
  void shouldPassBasicFunctionalityTestSuite() {
    runBasicFunctionalityTests();
  }

  @Test
  @DisplayName("应通过性能测试套件")
  void shouldPassPerformanceTestSuite() {
    runPerformanceTests();
  }

  @Test
  @DisplayName("应通过唯一性测试套件")
  void shouldPassUniquenessTestSuite() {
    runUniquenessTests();
  }
}
