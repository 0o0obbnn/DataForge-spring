package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("MathExpressionGenerator 测试")
class MathExpressionGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new MathExpressionGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "math_expression";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 数学表达式
  }
}
