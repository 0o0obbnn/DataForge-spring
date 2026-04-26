package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("ReligionGenerator 测试")
class ReligionGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new ReligionGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "religion";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 宗教名称
  }
}
