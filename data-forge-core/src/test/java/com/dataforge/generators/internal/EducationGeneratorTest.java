package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("EducationGenerator 测试")
class EducationGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new EducationGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "education";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 学历名称
  }
}
