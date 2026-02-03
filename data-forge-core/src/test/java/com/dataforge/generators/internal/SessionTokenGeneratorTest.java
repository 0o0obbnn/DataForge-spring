package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("SessionTokenGenerator 测试")
class SessionTokenGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new SessionTokenGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "session_token";
  }

  @Override
  protected String getValuePattern() {
    return "^[a-zA-Z0-9_-]{20,128}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return true; // Session token 应该唯一
  }
}
