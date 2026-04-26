package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("OccupationGenerator 测试")
class OccupationGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new OccupationGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "occupation";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 职业名称
  }
}
