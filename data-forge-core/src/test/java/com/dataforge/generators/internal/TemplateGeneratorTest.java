package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("TemplateGenerator 测试")
class TemplateGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new TemplateGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "template";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 模板值取决于配置
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
