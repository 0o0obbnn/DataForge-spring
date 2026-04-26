package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("YearRangeGenerator 测试")
class YearRangeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new YearRangeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "year_range";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{4}-\\d{4}$"; // 年份范围格式: 1990-2024
  }
}
