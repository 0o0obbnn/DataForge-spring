package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("PathTraversalGenerator 测试")
class PathTraversalGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new PathTraversalGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "path_traversal";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 路径遍历测试payload
  }
}
