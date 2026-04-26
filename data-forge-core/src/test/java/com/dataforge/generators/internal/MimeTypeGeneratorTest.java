package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("MimeTypeGenerator 测试")
class MimeTypeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new MimeTypeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "mime_type";
  }

  @Override
  protected String getValuePattern() {
    return "^[a-z]+/[a-z]+[-+]?[a-z]+$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
