package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("SqlInjectionGenerator 测试")
class SqlInjectionGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new SqlInjectionGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "sql_injection";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // SQL注入测试payload
  }
}
