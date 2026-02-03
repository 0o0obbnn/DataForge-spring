package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("RandomNumberGenerator 测试")
class RandomNumberGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new RandomNumberGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "random";
  }

  @Override
  protected String getValuePattern() {
    return "^-?\\d+(\\.\\d+)?$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
