package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("ImageFileHeaderGenerator 测试")
class ImageFileHeaderGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new ImageFileHeaderGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "image_file_header";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 图片文件头
  }
}
