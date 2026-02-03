package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("MatrixDataGenerator 测试")
class MatrixDataGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new MatrixDataGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "matrix_data";
  }

  @Override
  protected String getValuePattern() {
    return "^\\[\\[.*\\]\\]$"; // 矩阵格式
  }
}
