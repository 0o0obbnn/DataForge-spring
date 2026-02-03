package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("PercentageGenerator 测试")
class PercentageGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new PercentageGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "percentage";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d+%?$|^\\d+\\.\\d+%?$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
