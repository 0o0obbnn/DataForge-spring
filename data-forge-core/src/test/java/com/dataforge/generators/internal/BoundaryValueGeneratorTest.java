package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("BoundaryValueGenerator 测试")
class BoundaryValueGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new BoundaryValueGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "boundary_value";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 边界值
  }
}
