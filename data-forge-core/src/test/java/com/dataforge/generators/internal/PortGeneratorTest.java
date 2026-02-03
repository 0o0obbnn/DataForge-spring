package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("PortGenerator 测试")
class PortGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new PortGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "port";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{1,5}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
