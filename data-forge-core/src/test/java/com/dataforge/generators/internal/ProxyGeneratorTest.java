package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("ProxyGenerator 测试")
class ProxyGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new ProxyGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "proxy";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,5}:\\d{1,5}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
