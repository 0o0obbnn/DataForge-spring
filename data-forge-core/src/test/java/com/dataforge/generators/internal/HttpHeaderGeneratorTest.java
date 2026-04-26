package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("HttpHeaderGenerator 测试")
class HttpHeaderGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new HttpHeaderGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "http_header";
  }

  @Override
  protected String getValuePattern() {
    return "^[a-zA-Z-]+:\\s*.+$"; // HTTP头格式
  }
}
