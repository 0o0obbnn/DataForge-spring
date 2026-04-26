package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("CommandInjectionGenerator 测试")
class CommandInjectionGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new CommandInjectionGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "command_injection";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 命令注入测试payload
  }
}
