package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("SequenceGenerator 测试")
class SequenceGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new SequenceGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "sequence";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d+(,\\d+)*$"; // 匹配逗号分隔的数字序列
  }

  @Override
  protected boolean requiresUniqueness() {
    return false; // SequenceGenerator generates fixed sequences, not unique individual values
  }
}
