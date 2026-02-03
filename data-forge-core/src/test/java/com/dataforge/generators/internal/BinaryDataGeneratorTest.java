package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("BinaryDataGenerator 测试")
class BinaryDataGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new BinaryDataGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "binary";
  }

  @Override
  protected String getValuePattern() {
    return "^[01]+$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
