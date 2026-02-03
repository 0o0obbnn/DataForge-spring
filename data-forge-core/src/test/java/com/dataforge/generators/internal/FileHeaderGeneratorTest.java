package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("FileHeaderGenerator 测试")
class FileHeaderGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new FileHeaderGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "file_header";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 文件头
  }
}
