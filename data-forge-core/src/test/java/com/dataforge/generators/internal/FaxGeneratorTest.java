package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("FaxGenerator 测试")
class FaxGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new FaxGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "fax";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{3,4}-\\d{7,8}$"; // 传真格式: 区号-号码
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
