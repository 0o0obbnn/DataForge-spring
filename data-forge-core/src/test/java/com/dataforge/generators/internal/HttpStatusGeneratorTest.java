package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("HttpStatusGenerator 测试")
class HttpStatusGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new HttpStatusGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "http_status";
  }

  @Override
  protected String getValuePattern() {
    return "^[1-5]\\d{2}\\s[A-Z]+$"; // HTTP状态码格式: 200 OK
  }
}
