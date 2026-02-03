package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("CouponCodeGenerator 测试")
class CouponCodeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new CouponCodeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "coupon_code";
  }

  @Override
  protected String getValuePattern() {
    return "^[A-Z0-9]{8,16}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
