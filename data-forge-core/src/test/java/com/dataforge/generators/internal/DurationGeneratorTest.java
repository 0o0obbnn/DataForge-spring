package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("DurationGenerator 测试")
class DurationGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new DurationGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "duration";
  }

  @Override
  protected String getValuePattern() {
    return "^PT\\d+[HMS]$|^P\\d+DT\\d+H\\d+M\\d+S$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
