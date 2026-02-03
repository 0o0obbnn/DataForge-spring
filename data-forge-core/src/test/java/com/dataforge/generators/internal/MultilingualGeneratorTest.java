package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("MultilingualGenerator 测试")
class MultilingualGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new MultilingualGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "multilingual";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 多语言文本
  }
}
