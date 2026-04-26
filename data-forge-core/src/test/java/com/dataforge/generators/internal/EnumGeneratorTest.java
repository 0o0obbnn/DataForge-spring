package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("EnumGenerator 测试")
class EnumGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new EnumGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "enum";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 枚举值取决于配置
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
