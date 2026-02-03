package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("MaritalStatusGenerator 测试")
class MaritalStatusGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new MaritalStatusGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "marital_status";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 婚姻状况
  }
}
