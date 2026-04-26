package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("TimezoneGenerator 测试")
class TimezoneGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new TimezoneGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "timezone";
  }

  @Override
  protected String getValuePattern() {
    return "^[A-Za-z]+/[A-Za-z/_]+$"; // 时区格式: Asia/Shanghai
  }
}
