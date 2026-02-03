package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("SocialSecurityGenerator 测试")
class SocialSecurityGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new SocialSecurityGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "ssn";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{3}-\\d{2}-\\d{4}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
