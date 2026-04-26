package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;

/**
 * Generator测试基类 - 提供通用测试能力
 *
 * <p>基于TEST_OPTIMIZATION_PLAN.md中的设计，提供全面的测试基础设施
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public abstract class GeneratorTestBase<T extends DataGenerator<?, ? super FieldConfig>> {

  protected T generator;
  protected DataForgeContext context;
  protected SimpleFieldConfig config;

  /** 创建生成器实例 */
  protected abstract T createGenerator();

  /** 返回生成器类型 */
  protected abstract String getGeneratorType();

  @BeforeEach
  void baseSetUp() {
    generator = createGenerator();
    context = new DataForgeContext();
    config = new SimpleFieldConfig();
    config.setType(getGeneratorType());
  }

  /** 通用测试方法 - 生成非空值 */
  protected void assertGeneratesNonNull() {
    Object result = generator.generate(config, context);
    assertThat(result).isNotNull();
  }

  /** 通用测试方法 - 验证格式 */
  protected void assertGeneratesValidFormat(String regex) {
    Object result = generator.generate(config, context);
    assertThat(result.toString()).matches(regex);
  }

  /** 通用测试方法 - 处理空配置 */
  protected void assertHandlesNullConfig() {
    assertThatNoException().isThrownBy(() -> generator.generate(null, context));
  }

  /** 通用测试方法 - 处理空上下文 */
  protected void assertHandlesNullContext() {
    assertThatNoException().isThrownBy(() -> generator.generate(config, null));
  }

  /** 通用测试方法 - 性能测试 */
  protected void assertPerformance(int iterations, long maxDurationMs) {
    long start = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      generator.generate(config, context);
    }
    assertThat(System.currentTimeMillis() - start).isLessThan(maxDurationMs);
  }

  /** 通用测试方法 - 批量生成验证 */
  protected void assertBatchGeneration(int batchSize) {
    for (int i = 0; i < batchSize; i++) {
      Object result = generator.generate(config, context);
      assertThat(result).isNotNull();
    }
  }

  /** 通用测试方法 - 参数配置验证 */
  protected void assertParameterConfiguration(String paramName, String paramValue) {
    config.setParam(paramName, paramValue);
    Object result = generator.generate(config, context);
    assertThat(result).isNotNull();
  }
}
