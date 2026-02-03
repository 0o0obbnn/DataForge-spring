package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("ProductCodeGenerator 测试")
class ProductCodeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new ProductCodeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "product_code";
  }

  @Override
  protected String getValuePattern() {
    return "^[A-Z0-9]{8,13}$|^[A-Z]{2}\\d{4,8}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
