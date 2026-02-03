package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("TrackingNumberGenerator 测试")
class TrackingNumberGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new TrackingNumberGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "tracking_number";
  }

  @Override
  protected String getValuePattern() {
    return "^[A-Z]{2}\\d{9}$|\\d{10,20}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
