package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("RangeGenerator 测试")
class RangeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new RangeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "range";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d+\\.\\d+\\s*-\\s*\\d+\\.\\d+$|^\\d+\\s*-\\s*\\d+$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
