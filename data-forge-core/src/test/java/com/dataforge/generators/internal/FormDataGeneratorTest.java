package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("FormDataGenerator 测试")
class FormDataGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new FormDataGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "form_data";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 表单数据可以是任何格式
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
