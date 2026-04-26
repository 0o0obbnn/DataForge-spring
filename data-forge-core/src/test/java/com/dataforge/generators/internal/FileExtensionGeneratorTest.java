package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("FileExtensionGenerator 测试")
class FileExtensionGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new FileExtensionGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "file_extension";
  }

  @Override
  protected String getValuePattern() {
    return "^\\.[a-z]{2,4}$|^[a-z]{2,4}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false;
  }
}
