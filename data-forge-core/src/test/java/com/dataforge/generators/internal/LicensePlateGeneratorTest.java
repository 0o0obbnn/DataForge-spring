package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("LicensePlateGenerator 测试")
class LicensePlateGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new LicensePlateGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "license_plate";
  }

  @Override
  protected String getValuePattern() {
    return "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青琼藏宁澳港澳使领][A-Z][A-Za-z0-9]{5}$|^^[A-Z]{2}[A-Za-z0-9]{5}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
