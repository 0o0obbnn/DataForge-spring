package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("VerificationCodeGenerator 测试")
class VerificationCodeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new VerificationCodeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "verification_code";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{4,8}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false; // 验证码可以重复
  }
}
