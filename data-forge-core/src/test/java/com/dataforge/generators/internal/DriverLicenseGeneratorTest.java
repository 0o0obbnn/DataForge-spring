package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("DriverLicenseGenerator 测试")
class DriverLicenseGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new DriverLicenseGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "driver_license";
  }

  @Override
  protected String getValuePattern() {
    return "^[A-Z0-9]{10,20}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
