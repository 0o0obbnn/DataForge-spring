package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("XssPayloadGenerator 测试")
class XssPayloadGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new XssPayloadGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "xss_payload";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // XSS攻击payload
  }
}
