package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

/**
 * ColorGenerator 测试 - 使用模板快速创建
 *
 * <p>这个测试展示了如何使用 BaseGeneratorTestTemplate 快速创建测试 对于简单的生成器，只需要实现 3 个方法即可
 */
@DisplayName("ColorGenerator 测试")
class ColorGeneratorQuickTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new ColorGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "color";
  }

  @Override
  protected String getValidTestValue() {
    return "#FF0000";
  }

  @Override
  protected String getValuePattern() {
    return "#[0-9A-Fa-f]{6}|rgb\\(\\d+,\\s*\\d+,\\s*\\d+\\)|rgba?\\(\\s*(\\d+%?,\\s*){3}(1|0\\.?\\d+)\\s*\\)|[a-zA-Z]+";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false; // 颜色可以重复
  }

  // 如果需要额外测试，可以添加嵌套类
  // 但是对于简单生成器，模板提供的测试已经足够
}
