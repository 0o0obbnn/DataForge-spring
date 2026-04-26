package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.TestFieldConfig;
import com.dataforge.model.FieldConfig;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ColorGenerator单元测试
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("ColorGenerator - 颜色生成器测试")
class ColorGeneratorTest {

  private ColorGenerator generator;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new ColorGenerator();
    context = new DataForgeContext();
  }

  @Test
  @DisplayName("生成HEX格式颜色")
  void shouldGenerateHexColor() {
    TestFieldConfig config = new TestFieldConfig("field", "color", new HashMap<>());
    config.set("format", "HEX");

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).matches("^#[0-9A-F]{6}$");
  }

  @Test
  @DisplayName("生成RGB格式颜色")
  void shouldGenerateRgbColor() {
    TestFieldConfig config = new TestFieldConfig("field", "color", new HashMap<>());
    config.set("format", "RGB");

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).startsWith("rgb(");
    assertThat(result).endsWith(")");
  }

  @Test
  @DisplayName("生成带透明度的HEX颜色")
  void shouldGenerateHexColorWithAlpha() {
    TestFieldConfig config = new TestFieldConfig("field", "color", new HashMap<>());
    config.set("format", "HEX");
    config.set("alpha", true);

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).matches("^#[0-9A-F]{8}$");
  }

  @Test
  @DisplayName("生成颜色名称")
  void shouldGenerateColorName() {
    TestFieldConfig config = new TestFieldConfig("field", "color", new HashMap<>());
    config.set("format", "NAME");

    String result = generator.generate(config, context);

    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();
  }

  @Test
  @DisplayName("验证生成器类型")
  void shouldReturnCorrectType() {
    assertThat(generator.getType()).isEqualTo("color2");
    // Note: ColorGenerator uses type "color2" to avoid conflict with ColorValueGenerator
  }

  @Test
  @DisplayName("验证配置类")
  void shouldReturnCorrectConfigClass() {
    assertThat(generator.getConfigClass()).isEqualTo(FieldConfig.class);
  }
}
