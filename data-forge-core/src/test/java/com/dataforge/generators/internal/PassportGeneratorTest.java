package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("PassportGenerator 测试")
class PassportGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new PassportGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "passport";
  }

  @Override
  protected String getValuePattern() {
    return "^[A-Z]{1,2}[0-9]{6,9}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
