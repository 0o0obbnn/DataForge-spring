package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("DeviceIdGenerator 测试")
class DeviceIdGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new DeviceIdGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "device_id";
  }

  @Override
  protected String getValuePattern() {
    return "^[a-zA-Z0-9_-]{16,64}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
