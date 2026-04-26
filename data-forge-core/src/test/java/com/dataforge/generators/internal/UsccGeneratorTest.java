package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("UsccGenerator 测试")
class UsccGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new UsccGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "uscc";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{18}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
