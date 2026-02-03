package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("MeasurementUnitGenerator 测试")
class MeasurementUnitGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new MeasurementUnitGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "measurement_unit";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 计量单位
  }
}
