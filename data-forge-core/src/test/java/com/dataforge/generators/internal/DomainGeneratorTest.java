package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("DomainGenerator 测试")
class DomainGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new DomainGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "domain";
  }

  @Override
  protected String getValuePattern() {
    return "^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
