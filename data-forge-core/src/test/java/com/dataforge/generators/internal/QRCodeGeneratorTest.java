package com.dataforge.generators.internal;

import com.dataforge.generators.spi.DataGenerator;
import org.junit.jupiter.api.DisplayName;

@DisplayName("QRCodeGenerator 测试")
class QRCodeGeneratorTest extends BaseGeneratorTestTemplate {

  @Override
  protected DataGenerator<?, ?> createGenerator() {
    return new QRCodeGenerator();
  }

  @Override
  protected String getDefaultType() {
    return "qrcode";
  }

  @Override
  protected String getValuePattern() {
    return ".+"; // 二维码数据
  }
}
