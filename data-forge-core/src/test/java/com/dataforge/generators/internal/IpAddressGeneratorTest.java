package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("IpAddressGenerator 测试")
class IpAddressGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new IpAddressGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "ip_address";
  }

  @Override
  protected String getValuePattern() {
    return "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"; // IPv4格式
  }

  @Override
  protected boolean requiresUniqueness() {
    return true;
  }
}
