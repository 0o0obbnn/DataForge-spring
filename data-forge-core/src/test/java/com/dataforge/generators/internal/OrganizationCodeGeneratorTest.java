package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("OrganizationCodeGenerator 测试")
class OrganizationCodeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new OrganizationCodeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "org_code";
  }

  @Override
  protected String getValuePattern() {
    return "^\\d{8}-\\d$|[A-Z0-9]{8,12}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
