package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import com.dataforge.validation.IdCardValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("身份证生成器测试")
class IdCardGeneratorTest {

  @Mock private IdCardValidator idCardValidator;

  private IdCardGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new IdCardGenerator(idCardValidator);
    config = new SimpleFieldConfig();
    config.setType("idcard");
    context = new DataForgeContext();

    when(idCardValidator.calculateCheckCode(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn('1');
  }

  @Test
  @DisplayName("生成标准18位身份证")
  void shouldGenerateValidIdCard() {
    String idCard = generator.generate(config, context);

    assertThat(idCard).isNotNull();
    assertThat(idCard.length()).isEqualTo(18);
    // 前17位是数字，第18位是数字或X
    assertThat(idCard).matches("^\\d{17}[\\dXx]$");
  }

  @Test
  @DisplayName("生成身份证包含有效地区代码")
  void shouldGenerateIdCardWithValidAreaCode() {
    String idCard = generator.generate(config, context);

    assertThat(idCard).isNotNull();
    // 前6位是地区代码
    String areaCode = idCard.substring(0, 6);
    assertThat(areaCode).matches("^\\d{6}$");
  }

  @Test
  @DisplayName("生成身份证包含有效出生日期")
  void shouldGenerateIdCardWithValidBirthDate() {
    String idCard = generator.generate(config, context);

    assertThat(idCard).isNotNull();
    // 第7-14位是出生日期 YYYYMMDD
    String birthDate = idCard.substring(6, 14);
    assertThat(birthDate).matches("^\\d{8}$");

    // 验证年月日基本有效性
    int year = Integer.parseInt(birthDate.substring(0, 4));
    int month = Integer.parseInt(birthDate.substring(4, 6));
    int day = Integer.parseInt(birthDate.substring(6, 8));

    assertThat(year).isBetween(1900, 2026);
    assertThat(month).isBetween(1, 12);
    assertThat(day).isBetween(1, 31);
  }

  @Test
  @DisplayName("生成身份证包含有效校验码")
  void shouldGenerateIdCardWithValidCheckCode() {
    String idCard = generator.generate(config, context);

    assertThat(idCard).isNotNull();
    // 最后一位是校验码
    char checkCode = idCard.charAt(17);
    assertThat(Character.isDigit(checkCode) || checkCode == 'X' || checkCode == 'x').isTrue();
  }

  @Test
  @DisplayName("生成多个不同身份证")
  void shouldGenerateDifferentIdCards() {
    String idCard1 = generator.generate(config, context);
    String idCard2 = generator.generate(config, context);

    assertThat(idCard1).isNotNull();
    assertThat(idCard2).isNotNull();
    assertThat(idCard1).matches("^\\d{17}[\\dXx]$");
    assertThat(idCard2).matches("^\\d{17}[\\dXx]$");
  }

  @Test
  @DisplayName("生成多个不同身份证")
  void shouldGenerateMultipleIdCards() {
    for (int i = 0; i < 10; i++) {
      String idCard = generator.generate(config, context);
      assertThat(idCard).isNotNull();
      assertThat(idCard.length()).isEqualTo(18);
      assertThat(idCard).matches("^\\d{17}[\\dXx]$");
    }
  }

  @Test
  @DisplayName("默认生成有效身份证")
  void shouldDefaultToValidIdCard() {
    SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

    String idCard = generator.generate(emptyConfig, context);

    assertThat(idCard).isNotNull();
    assertThat(idCard).matches("^\\d{17}[\\dXx]$");
  }
}
