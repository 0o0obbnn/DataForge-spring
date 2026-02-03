package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("EthnicityGenerator 测试")
class EthnicityGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new EthnicityGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "ethnicity";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 民族名称
  }
}
