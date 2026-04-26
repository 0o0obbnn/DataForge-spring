package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("InvalidDataGenerator 测试")
class InvalidDataGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new InvalidDataGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "invalid_data";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 无效数据
  }
}
