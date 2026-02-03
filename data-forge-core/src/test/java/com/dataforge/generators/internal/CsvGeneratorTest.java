package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("CsvGenerator 测试")
class CsvGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new CsvGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "csv";
  }

  @Override
  protected String getValuePattern() {
    return "^.+,.+$"; // CSV格式
  }
}
