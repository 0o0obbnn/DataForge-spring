package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("JsonSchemaGenerator 测试")
class JsonSchemaGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new JsonSchemaGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "json_schema";
  }

  @Override
  protected String getValuePattern() {
    return "^\\{.+:.+\\}$"; // JSON Schema格式
  }
}
