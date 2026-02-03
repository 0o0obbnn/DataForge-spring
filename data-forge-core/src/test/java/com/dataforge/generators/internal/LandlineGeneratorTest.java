package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("LandlineGenerator 测试")
class LandlineGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new LandlineGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "landline";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{3,4}-\\d{7,8}$"; // 座机格式: 区号-号码
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
