package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.TestFieldConfig;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CurrencyGenerator单元测试
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("CurrencyGenerator - 货币生成器测试")
class CurrencyGeneratorTest {

  private CurrencyGenerator generator;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new CurrencyGenerator();
    context = new DataForgeContext();
  }

  @Test
  @DisplayName("生成默认货币")
  void shouldGenerateDefaultCurrency() {
    TestFieldConfig config = new TestFieldConfig("field", "currency", new HashMap<>());

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();
  }

  @Test
  @DisplayName("生成人民币")
  void shouldGenerateRmbCurrency() {
    TestFieldConfig config = new TestFieldConfig("field", "currency", new HashMap<>());
    config.set("currency", "CNY");

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).matches(".*[¥CNY].*");
  }

  @Test
  @DisplayName("生成美元")
  void shouldGenerateUsdCurrency() {
    TestFieldConfig config = new TestFieldConfig("field", "currency", new HashMap<>());
    config.set("currency", "USD");

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result)
        .satisfiesAnyOf(r -> assertThat(r).contains("$"), r -> assertThat(r).contains("USD"));
  }

  @Test
  @DisplayName("生成带格式的货币")
  void shouldGenerateFormattedCurrency() {
    TestFieldConfig config = new TestFieldConfig("field", "currency", new HashMap<>());
    config.set("formatted", true);

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();
  }

  @Test
  @DisplayName("验证生成器类型")
  void shouldReturnCorrectType() {
    assertThat(generator.getType()).isEqualTo("currency");
  }
}
