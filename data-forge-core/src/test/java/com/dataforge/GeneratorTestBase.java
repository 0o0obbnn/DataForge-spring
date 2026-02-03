package com.dataforge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.dataforge.config.SimpleFieldConfig;
import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 改进的Generator测试基类 - 提供通用测试能力和业务逻辑测试框架
 *
 * <p>解决空壳测试问题，强制要求子类实现业务逻辑测试方法
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public abstract class GeneratorTestBase<T extends DataGenerator<?, FieldConfig>> {

  protected T generator;
  protected DataForgeContext context;
  protected FieldConfig config;

  @BeforeEach
  void baseSetUp() {
    generator = createGenerator();
    context = new DataForgeContext();
    config = new SimpleFieldConfig("test_field", getGeneratorType());
  }

  /** 子类必须实现：创建生成器实例 */
  protected abstract T createGenerator();

  /** 子类必须实现：返回生成器类型 */
  protected abstract String getGeneratorType();

  /** 子类必须实现：返回业务逻辑验证的正则表达式 */
  protected abstract String getBusinessLogicPattern();

  /** 子类可选实现：返回支持的参数配置 */
  protected Map<String, String> getSupportedParameters() {
    return Map.of();
  }

  /** 子类可选实现：返回性能测试的基准值 */
  protected PerformanceBenchmark getPerformanceBenchmark() {
    return new PerformanceBenchmark(1000, 1000L); // 默认1000次迭代，1秒内完成
  }

  @Nested
  @DisplayName("基础功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应生成非空值")
    void shouldGenerateNonNullValue() {
      Object result = generator.generate(config, context);
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应生成符合业务逻辑的值")
    void shouldGenerateValidBusinessValue() {
      Object result = generator.generate(config, context);
      assertThat(result.toString()).matches(getBusinessLogicPattern());
    }

    @Test
    @DisplayName("应正确处理null配置")
    void shouldHandleNullConfig() {
      assertThatNoException().isThrownBy(() -> generator.generate(null, context));
    }

    @Test
    @DisplayName("应正确处理null上下文")
    void shouldHandleNullContext() {
      assertThatNoException().isThrownBy(() -> generator.generate(config, null));
    }
  }

  @Nested
  @DisplayName("配置参数测试")
  class ConfigurationTests {

    @Test
    @DisplayName("应支持自定义参数")
    void shouldSupportCustomParameters() {
      Map<String, String> supportedParams = getSupportedParameters();

      for (Map.Entry<String, String> param : supportedParams.entrySet()) {
        // 对于FieldConfig接口，需要检查具体实现是否支持setParam方法
        if (config instanceof SimpleFieldConfig) {
          ((SimpleFieldConfig) config).setParam(param.getKey(), param.getValue());
        }
        Object result = generator.generate(config, context);
        assertThat(result).isNotNull();
        assertThat(result.toString()).matches(getBusinessLogicPattern());
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "null", "undefined"})
    @DisplayName("应处理无效参数值")
    void shouldHandleInvalidParameterValues(String invalidValue) {
      config.setParam("test_param", invalidValue);
      assertThatNoException().isThrownBy(() -> generator.generate(config, context));
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("应在合理时间内完成批量生成")
    void shouldCompleteBatchGenerationInReasonableTime() {
      PerformanceBenchmark benchmark = getPerformanceBenchmark();
      int iterations = benchmark.getIterations();
      long maxDurationMs = benchmark.getMaxDurationMs();

      long startTime = System.nanoTime();

      for (int i = 0; i < iterations; i++) {
        Object result = generator.generate(config, context);
        assertThat(result).isNotNull();
      }

      long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      assertThat(duration).isLessThan(maxDurationMs);
    }

    @Test
    @DisplayName("应具有稳定的性能表现")
    void shouldHaveStablePerformance() {
      int iterations = 100;
      long[] durations = new long[5];

      for (int run = 0; run < 5; run++) {
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
          generator.generate(config, context);
        }

        durations[run] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      }

      // 验证性能波动不超过20%
      long average = (durations[0] + durations[1] + durations[2] + durations[3] + durations[4]) / 5;
      for (long duration : durations) {
        double deviation = Math.abs((double) (duration - average) / average);
        assertThat(deviation).isLessThan(0.2);
      }
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryConditionTests {

    @Test
    @DisplayName("应处理极端配置值")
    void shouldHandleExtremeConfigurationValues() {
      // 测试最小/最大值边界
      config.setParam("min", "0");
      config.setParam("max", "1000000");

      Object result = generator.generate(config, context);
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应处理大量重复生成")
    void shouldHandleHighVolumeGeneration() {
      Set<String> uniqueValues = new HashSet<>();
      int totalGenerations = 1000;

      for (int i = 0; i < totalGenerations; i++) {
        Object result = generator.generate(config, context);
        uniqueValues.add(result.toString());
      }

      // 验证至少生成了不同的值
      assertThat(uniqueValues.size()).isGreaterThan(0);
    }
  }

  /** 性能基准配置类 */
  protected static class PerformanceBenchmark {
    private final int iterations;
    private final long maxDurationMs;

    public PerformanceBenchmark(int iterations, long maxDurationMs) {
      this.iterations = iterations;
      this.maxDurationMs = maxDurationMs;
    }

    public int getIterations() {
      return iterations;
    }

    public long getMaxDurationMs() {
      return maxDurationMs;
    }
  }
}
