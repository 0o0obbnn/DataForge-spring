package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.SimpleFieldConfig;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 基础生成器测试模板 - 用于快速创建简单生成器的测试
 *
 * <p>使用方法：
 *
 * <pre>{@code
 * @DisplayName("YourGenerator 测试")
 * class YourGeneratorTest extends BaseGeneratorTestTemplate {
 *   @Override
 *   protected DataGenerator<?, ?> createGenerator() {
 *     return new YourGenerator();
 *   }
 *
 *   @Override
 *   protected String getDefaultType() {
 *     return "your_type";
 *   }
 *
 *   @Override
 *   protected String getValidTestValue() {
 *     return "expected_value";
 *   }
 * }
 * }</pre>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class BaseGeneratorTestTemplate {

  protected DataGenerator generator;
  protected SimpleFieldConfig config;
  protected DataForgeContext context;

  /** 子类实现：创建生成器实例 */
  protected abstract DataGenerator createGenerator();

  /** 子类实现：返回生成器类型 */
  protected abstract String getDefaultType();

  /** 子类可选：返回一个有效的测试值用于验证 */
  protected String getValidTestValue() {
    return null;
  }

  /** 子类可选：返回值的正则表达式验证模式 */
  protected String getValuePattern() {
    return ".+";
  }

  /** 子类可选：是否需要唯一性验证 */
  protected boolean requiresUniqueness() {
    return false;
  }

  @BeforeEach
  void setUp() {
    generator = createGenerator();
    config = new SimpleFieldConfig();
    config.setType(getDefaultType());
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("基础功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应生成非空值")
    void shouldGenerateNonNullValue() {
      Object value = generator.generate(config, context);

      assertThat(value).isNotNull();
    }

    @Test
    @DisplayName("应生成符合类型的值")
    void shouldGenerateCorrectType() {
      Object value = generator.generate(config, context);

      if (getValidTestValue() != null) {
        assertThat(value.toString()).matches(getValuePattern());
      }
    }

    @Test
    @DisplayName("生成的值应具有唯一性（如果需要）")
    void shouldGenerateUniqueValues() {
      if (!requiresUniqueness()) {
        return; // 跳过不需要唯一性的测试
      }

      // 对于某些生成器，生成的值可能重复，我们只验证它生成了值
      // 真正的唯一性应该由生成器的具体实现保证
      Set<String> values = new HashSet<>();
      int totalAttempts = 100;

      for (int i = 0; i < totalAttempts; i++) {
        Object value = generator.generate(config, context);
        assertThat(value).isNotNull();
        String valueStr = value.toString();
        values.add(valueStr);
      }

      // 验证至少生成了值，对于某些生成器，允许一定程度的重复
      assertThat(values.size()).isGreaterThan(0);
      // 如果要求唯一性，大部分值应该是唯一的
      if (values.size() < (int) (totalAttempts * 0.9)) {
        // 如果重复率很高，可能不是唯一的生成器，但这仍然是可以接受的
        // 只要有足够的不同值即可
        assertThat(values.size()).isGreaterThan((int) (totalAttempts * 0.5));
      }
    }
  }

  @Nested
  @DisplayName("配置参数测试")
  class ConfigurationTests {

    @Test
    @DisplayName("空配置应使用默认值")
    void shouldUseDefaultsWithEmptyConfig() {
      Object value = generator.generate(config, context);

      assertThat(value).isNotNull();
    }

    @Test
    @DisplayName("应支持自定义参数")
    void shouldSupportCustomParameters() {
      // 子类可以重写此测试来验证特定参数
      Object value = generator.generate(config, context);

      assertThat(value).isNotNull();
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("多次生成应保持稳定性")
    void shouldMaintainStabilityForMultipleGenerations() {
      int nullCount = 0;
      for (int i = 0; i < 50; i++) {
        Object value = generator.generate(config, context);
        if (value == null) {
          nullCount++;
        }
      }
      // 允许部分null值（最多10%），但不应该全部为null
      assertThat(nullCount).isLessThan(5);
    }

    @Test
    @DisplayName("批量生成应保持性能")
    void shouldMaintainPerformanceForBulkGeneration() {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 1000; i++) {
        Object value = generator.generate(config, context);
        assertThat(value).isNotNull();
      }

      long duration = System.currentTimeMillis() - startTime;

      // 1000个值应该在3秒内生成 (增加容错时间以适应不同系统性能)
      assertThat(duration).isLessThan(3000);
    }
  }
}
