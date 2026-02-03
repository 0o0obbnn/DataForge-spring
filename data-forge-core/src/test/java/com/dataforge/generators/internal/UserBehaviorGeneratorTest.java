package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("UserBehaviorGenerator 测试")
class UserBehaviorGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new UserBehaviorGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "user_behavior";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 用户行为数据
  }
}
