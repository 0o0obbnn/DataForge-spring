package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("UrlGenerator 测试")
class UrlGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new UrlGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "url";
  }

  @Override
  protected String getValuePattern() {
    return "^https?://[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(:\\d+)?(/.*)?$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
