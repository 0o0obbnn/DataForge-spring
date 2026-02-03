package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("IsoWeekGenerator 测试")
class IsoWeekGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new IsoWeekGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "iso_week";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{4}-W\\d{2}$"; // ISO周格式: 2024-W01
  }
}
