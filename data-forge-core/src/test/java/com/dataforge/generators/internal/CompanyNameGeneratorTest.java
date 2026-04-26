package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("CompanyNameGenerator 测试")
class CompanyNameGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new CompanyNameGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "company_name";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 公司名称
  }
}
