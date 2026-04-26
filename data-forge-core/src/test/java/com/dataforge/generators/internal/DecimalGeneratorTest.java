package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.TestFieldConfig;
import java.math.BigDecimal;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DecimalGenerator单元测试
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("DecimalGenerator - 小数生成器测试")
class DecimalGeneratorTest {

  private DecimalGenerator generator;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new DecimalGenerator();
    context = new DataForgeContext();
  }

  @Test
  @DisplayName("生成默认小数")
  void shouldGenerateDefaultDecimal() {
    TestFieldConfig config = new TestFieldConfig("field", "decimal", new HashMap<>());

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(new BigDecimal(result)).isNotNull();
  }

  @Test
  @DisplayName("生成指定精度的小数")
  void shouldGenerateDecimalWithPrecision() {
    TestFieldConfig config = new TestFieldConfig("field", "decimal", new HashMap<>());
    config.set("precision", 2);

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    // 验证小数位数
    if (result.contains(".")) {
      int decimalPlaces = result.length() - result.indexOf(".") - 1;
      assertThat(decimalPlaces).isLessThanOrEqualTo(2);
    }
  }

  @Test
  @DisplayName("生成指定范围的小数")
  void shouldGenerateDecimalInRange() {
    TestFieldConfig config = new TestFieldConfig("field", "decimal", new HashMap<>());
    config.set("min", 0);
    config.set("max", 100);

    String result = generator.generate(config, context);
    BigDecimal value = new BigDecimal(result);

    assertThat(value).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    assertThat(value).isLessThanOrEqualTo(new BigDecimal("100"));
  }

  @Test
  @DisplayName("生成正数小数")
  void shouldGeneratePositiveDecimal() {
    TestFieldConfig config = new TestFieldConfig("field", "decimal", new HashMap<>());
    config.set("positive", true);

    String result = generator.generate(config, context);
    BigDecimal value = new BigDecimal(result);

    assertThat(value).isGreaterThanOrEqualTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("验证生成器类型")
  void shouldReturnCorrectType() {
    assertThat(generator.getType()).isEqualTo("decimal");
  }
}
