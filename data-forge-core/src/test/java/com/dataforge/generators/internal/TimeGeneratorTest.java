package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("TimeGenerator 测试")
class TimeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new TimeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "time";
  }

  @Override
  protected String getValuePattern() {
    return "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
