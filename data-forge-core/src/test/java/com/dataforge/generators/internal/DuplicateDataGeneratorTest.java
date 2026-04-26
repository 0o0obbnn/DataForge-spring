package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("DuplicateDataGenerator 测试")
class DuplicateDataGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new DuplicateDataGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "duplicate_data";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 重复数据
  }
}
