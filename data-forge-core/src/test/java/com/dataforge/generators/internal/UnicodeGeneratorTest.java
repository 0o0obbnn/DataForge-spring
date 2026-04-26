package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("UnicodeGenerator 测试")
class UnicodeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new UnicodeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "unicode";
  }

  @Override
  protected String getValuePattern() {
    return "."; // Unicode 字符可以是任何字符
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
