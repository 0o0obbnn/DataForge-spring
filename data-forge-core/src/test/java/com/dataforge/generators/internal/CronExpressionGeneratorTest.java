package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("CronExpressionGenerator 测试")
class CronExpressionGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new CronExpressionGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "cron";
  }

  @Override
  protected String getValuePattern() {
    return "^$|^\\*\\s+\\*\\s+\\*\\s+\\*\\s+\\*$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
