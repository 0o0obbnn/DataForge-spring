package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("BloodTypeGenerator 测试")
class BloodTypeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new BloodTypeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "blood_type";
  }

  @Override
  protected String getValuePattern() {
    return "^(A|B|AB|O)[+-]?$"; // 血型格式
  }
}
