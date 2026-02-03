package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("GeolocationGenerator 测试")
class GeolocationGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new GeolocationGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "geolocation";
  }

  @Override
  protected String getValuePattern() {
    return "^-?\\d+\\.\\d+,-?\\d+\\.\\d+$"; // 纬度,经度格式
  }
}
