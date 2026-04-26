package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("BusinessDocumentGenerator 测试")
class BusinessDocumentGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new BusinessDocumentGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "business_document";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 商业文档
  }
}
