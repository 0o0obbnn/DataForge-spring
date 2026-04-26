package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("UserAgentGenerator 测试")
class UserAgentGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new UserAgentGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "user_agent";
  }

  @Override
  protected String getValuePattern() {
    return "^.+/.+\\s+\\(.+\\).+"; // User-Agent格式
  }
}
