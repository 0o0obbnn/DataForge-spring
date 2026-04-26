package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("SpecialCharGenerator 测试")
class SpecialCharGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new SpecialCharGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "special_char";
  }

  @Override
  protected String getValuePattern() {
    return "^[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]+";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
