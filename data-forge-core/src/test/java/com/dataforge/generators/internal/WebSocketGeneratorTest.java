package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("WebSocketGenerator 测试")
class WebSocketGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new WebSocketGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "websocket";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // WebSocket消息
  }
}
