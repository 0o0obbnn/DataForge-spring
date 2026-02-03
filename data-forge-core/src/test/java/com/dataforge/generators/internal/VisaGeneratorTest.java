package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("VisaGenerator 测试")
class VisaGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new VisaGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "visa";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{16}$"; // Visa卡号格式
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
