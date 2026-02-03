package com.dataforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.model.SimpleFieldConfig;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 增强版生成器测试基类 - 提供现代Java测试能力
 *
 * <p>基于Java 21+特性设计，支持虚拟线程、模式匹配等现代功能
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public abstract class EnhancedGeneratorTestBase<T extends DataGenerator<?, ? super FieldConfig>> {

  protected T generator;
  protected DataForgeContext context;
  protected SimpleFieldConfig config;

  @BeforeEach
  void baseSetUp() {
    generator = createGenerator();
    context = new DataForgeContext();
    config = new SimpleFieldConfig();
    config.setType(getGeneratorType());
  }

  /** 子类实现：创建生成器实例 */
  protected abstract T createGenerator();

  /** 子类实现：返回生成器类型 */
  protected abstract String getGeneratorType();

  // ==================== 通用测试方法 ====================

  /** 验证生成器能生成非空结果 */
  protected void assertGeneratesNonNull() {
    Object result = generator.generate(config, context);
    assertThat(result).isNotNull();
  }

  /** 验证生成结果符合指定格式（正则表达式） */
  protected void assertGeneratesValidFormat(String regex) {
    Object result = generator.generate(config, context);
    assertThat(result.toString()).matches(regex);
  }

  /** 验证生成器能处理null配置 */
  protected void assertHandlesNullConfig() {
    assertThatNoException().isThrownBy(() -> generator.generate(null, context));
  }

  /** 验证生成器能处理null上下文 */
  protected void assertHandlesNullContext() {
    assertThatNoException().isThrownBy(() -> generator.generate(config, null));
  }

  /** 性能测试：验证生成器在指定迭代次数内的执行时间 */
  protected void assertPerformance(int iterations, Duration maxDuration) {
    long start = System.nanoTime();

    // 使用虚拟线程进行并发性能测试
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          IntStream.range(0, iterations)
              .mapToObj(i -> executor.submit(() -> generator.generate(config, context)))
              .toList();

      // 等待所有任务完成
      futures.forEach(
          future -> {
            try {
              future.get();
            } catch (Exception e) {
              throw new RuntimeException("Performance test failed", e);
            }
          });
    }

    long duration = System.nanoTime() - start;
    assertThat(Duration.ofNanos(duration)).isLessThan(maxDuration);
  }

  /** 唯一性测试：验证生成器在多次调用中是否产生唯一结果 */
  protected void assertUniqueness(int sampleSize, double uniquenessThreshold) {
    Set<Object> uniqueResults = ConcurrentHashMap.newKeySet();

    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          IntStream.range(0, sampleSize)
              .mapToObj(i -> executor.submit(() -> generator.generate(config, context)))
              .toList();

      futures.forEach(
          future -> {
            try {
              uniqueResults.add(future.get());
            } catch (Exception e) {
              throw new RuntimeException("Uniqueness test failed", e);
            }
          });
    }

    double actualUniqueness = (double) uniqueResults.size() / sampleSize;
    assertThat(actualUniqueness).isGreaterThanOrEqualTo(uniquenessThreshold);
  }

  /** 边界值测试：验证生成器处理边界参数的能力 */
  protected void assertBoundaryHandling(String paramName, Object... boundaryValues) {
    for (Object value : boundaryValues) {
      config.setParam(paramName, value.toString());
      assertThatNoException().isThrownBy(() -> generator.generate(config, context));
    }
  }

  /** 参数组合测试：验证多个参数组合的兼容性 */
  protected void assertParameterCombinations(Map<String, Object[]> parameterCombinations) {
    // 递归生成所有参数组合
    testParameterCombinations(parameterCombinations, Map.of(), 0);
  }

  private void testParameterCombinations(
      Map<String, Object[]> allParams, Map<String, Object> currentParams, int index) {
    if (index == allParams.size()) {
      // 应用当前参数组合并测试
      currentParams.forEach((key, value) -> config.setParam(key, value.toString()));

      assertThatNoException().isThrownBy(() -> generator.generate(config, context));
      return;
    }

    var entry = allParams.entrySet().stream().skip(index).findFirst().orElseThrow();
    for (Object value : entry.getValue()) {
      var newParams = new java.util.HashMap<>(currentParams);
      newParams.put(entry.getKey(), value);
      testParameterCombinations(allParams, newParams, index + 1);
    }
  }

  // ==================== 测试模板方法 ====================

  /** 执行基本功能测试套件 */
  protected void runBasicFunctionalityTests() {
    assertGeneratesNonNull();
    assertHandlesNullConfig();
    assertHandlesNullContext();
  }

  /** 执行性能测试套件 */
  protected void runPerformanceTests() {
    assertPerformance(1000, Duration.ofSeconds(5));
    assertPerformance(10000, Duration.ofSeconds(30));
  }

  /** 执行唯一性测试套件 */
  protected void runUniquenessTests() {
    assertUniqueness(1000, 0.95); // 95%唯一性要求
  }

  // ==================== 内嵌测试类模板 ====================

  /** 基本功能测试模板 */
  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTestsTemplate {

    @Test
    @DisplayName("应生成非空结果")
    void shouldGenerateNonNull() {
      assertGeneratesNonNull();
    }

    @Test
    @DisplayName("应处理null配置")
    void shouldHandleNullConfig() {
      assertHandlesNullConfig();
    }

    @Test
    @DisplayName("应处理null上下文")
    void shouldHandleNullContext() {
      assertHandlesNullContext();
    }
  }

  /** 性能测试模板 */
  @Nested
  @DisplayName("性能测试")
  class PerformanceTestsTemplate {

    @Test
    @DisplayName("1000次生成应在5秒内完成")
    void shouldComplete1000GenerationsWithin5Seconds() {
      assertPerformance(1000, Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("10000次生成应在30秒内完成")
    void shouldComplete10000GenerationsWithin30Seconds() {
      assertPerformance(10000, Duration.ofSeconds(30));
    }
  }

  /** 边界条件测试模板 */
  @Nested
  @DisplayName("边界条件测试")
  class BoundaryConditionTestsTemplate {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "null", "undefined"})
    @DisplayName("应处理特殊参数值")
    void shouldHandleSpecialParameterValues(String value) {
      config.setParam("test_param", value);
      assertThatNoException().isThrownBy(() -> generator.generate(config, context));
    }

    @Test
    @DisplayName("应处理超大参数值")
    void shouldHandleLargeParameterValues() {
      config.setParam("large_param", "x".repeat(10000));
      assertThatNoException().isThrownBy(() -> generator.generate(config, context));
    }
  }
}
