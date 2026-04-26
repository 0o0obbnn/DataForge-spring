package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("RichTextGenerator 测试")
class RichTextGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new RichTextGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "rich_text";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 富文本
  }
}
