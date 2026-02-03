package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("LongTextGenerator 测试")
class LongTextGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new LongTextGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "longtext";
  }

  @Override
  protected String getValuePattern() {
    return ".{100,}"; // 至少100个字符
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
