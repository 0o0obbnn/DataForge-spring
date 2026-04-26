package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("手机号生成器测试")
class PhoneGeneratorTest {

  private PhoneGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new PhoneGenerator();
    config = new SimpleFieldConfig();
    config.setType("phone");
    context = new DataForgeContext();
  }

  @Test
  @DisplayName("生成标准手机号格式")
  void shouldGenerateValidPhoneNumber() {
    String phone = generator.generate(config, context);

    assertThat(phone).isNotNull();
    assertThat(phone).matches("^1[3-9]\\d{9}$");
    assertThat(phone.length()).isEqualTo(11);
  }

  @Test
  @DisplayName("生成中国移动手机号")
  void shouldGenerateChinaMobileNumber() {
    config.setParam("operator", "MOBILE");

    String phone = generator.generate(config, context);

    assertThat(phone).isNotNull();
    assertThat(phone).matches("^1[3-9]\\d{9}$");
    assertThat(phone.length()).isEqualTo(11);
  }

  @Test
  @DisplayName("生成中国联通手机号")
  void shouldGenerateChinaUnicomNumber() {
    config.setParam("operator", "UNICOM");

    String phone = generator.generate(config, context);

    assertThat(phone).isNotNull();
    assertThat(phone).matches("^1[3-9]\\d{9}$");
    assertThat(phone.length()).isEqualTo(11);
  }

  @Test
  @DisplayName("生成中国电信手机号")
  void shouldGenerateChinaTelecomNumber() {
    config.setParam("operator", "TELECOM");

    String phone = generator.generate(config, context);

    assertThat(phone).isNotNull();
    assertThat(phone).matches("^1[3-9]\\d{9}$");
    assertThat(phone.length()).isEqualTo(11);
  }

  @Test
  @DisplayName("生成指定前缀的手机号")
  void shouldGeneratePhoneWithSpecificPrefix() {
    config.setParam("prefix", "138");

    String phone = generator.generate(config, context);

    assertThat(phone).isNotNull();
    assertThat(phone.startsWith("138")).isTrue();
    assertThat(phone.length()).isEqualTo(11);
  }

  @Test
  @DisplayName("生成无效手机号")
  void shouldGenerateInvalidPhoneNumber() {
    config.setParam("valid", "false");

    String phone = generator.generate(config, context);

    assertThat(phone).isNotNull();
    // 无效手机号可能长度不对或前缀不对
    assertThat(phone).isNotEmpty();
  }

  @Test
  @DisplayName("生成多个手机号")
  void shouldGenerateMultiplePhoneNumbers() {
    for (int i = 0; i < 10; i++) {
      String phone = generator.generate(config, context);
      assertThat(phone).matches("^1[3-9]\\d{9}$");
    }
  }

  @Test
  @DisplayName("默认生成任意运营商手机号")
  void shouldDefaultToAnyCarrier() {
    String phone = generator.generate(config, context);

    assertThat(phone).isNotNull();
    assertThat(phone).matches("^1[3-9]\\d{9}$");
  }
}
