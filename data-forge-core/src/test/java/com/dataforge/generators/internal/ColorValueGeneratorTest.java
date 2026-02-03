package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("ColorValueGenerator 测试")
class ColorValueGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new ColorValueGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "color_value";
  }

  @Override
  protected String getValuePattern() {
    return "^#[0-9A-Fa-f]{6}$|^rgb\\(\\d+,\\s*\\d+,\\s*\\d+\\)$"; // 颜色值格式
  }
}
