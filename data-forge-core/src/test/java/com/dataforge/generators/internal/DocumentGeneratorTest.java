package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("DocumentGenerator 测试")
class DocumentGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new DocumentGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "document";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 文档内容
  }
}
