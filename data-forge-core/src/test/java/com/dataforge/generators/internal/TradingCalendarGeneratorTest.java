package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("TradingCalendarGenerator 测试")
class TradingCalendarGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new TradingCalendarGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "trading_calendar";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{4}-\\d{2}-\\d{2}$"; // 交易日格式
  }
}
