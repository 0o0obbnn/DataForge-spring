package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("MacAddressGenerator 测试")
class MacAddressGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new MacAddressGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "mac";
  }

  @Override
  protected String getValuePattern() {
    return "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$";
  }

  @Override
  protected boolean requiresUniqueness() {
    return false; // MAC 地址可以重复（随机生成）
  }
}
