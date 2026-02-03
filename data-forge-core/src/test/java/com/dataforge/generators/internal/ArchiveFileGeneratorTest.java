package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("ArchiveFileGenerator 测试")
class ArchiveFileGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new ArchiveFileGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "archive_file";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 压缩文件数据
  }
}
