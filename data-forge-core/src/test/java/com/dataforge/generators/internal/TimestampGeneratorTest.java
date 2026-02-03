package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("TimestampGenerator 测试")
class TimestampGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new TimestampGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "timestamp";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
