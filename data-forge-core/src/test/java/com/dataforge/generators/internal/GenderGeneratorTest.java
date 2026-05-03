package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GenderGenerator 测试")
class GenderGeneratorTest {

  private GenderGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new GenderGenerator();
    config = new SimpleFieldConfig();
    config.setType("gender");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("默认配置测试")
  class DefaultConfigurationTests {

    @Test
    @DisplayName("默认配置应生成有效性别")
    void shouldGenerateValidGenderWithDefaultConfig() {
      String gender = generator.generate(config, context);

      assertThat(gender).isNotNull();
      assertThat(gender).isIn("男", "女", "其他");
    }

    @Test
    @DisplayName("默认性别格式应为中文")
    void shouldUseChineseFormatByDefault() {
      String gender = generator.generate(config, context);

      assertThat(gender).isIn("男", "女", "其他");
    }

    @Test
    @DisplayName("生成的性别应存储在上下文中")
    void shouldStoreGenderInContext() {
      generator.generate(config, context);

      String genderFromContext = context.get("gender", String.class).orElse(null);

      assertThat(genderFromContext).isNotNull();
      assertThat(genderFromContext).isIn("MALE", "FEMALE", "OTHER");
    }
  }

  @Nested
  @DisplayName("性别类型指定测试")
  class GenderTypeTests {

    @Test
    @DisplayName("应生成指定的男性性别")
    void shouldGenerateMaleGender() {
      config.setParam("type", "MALE");

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("男");
    }

    @Test
    @DisplayName("应生成指定的女性性别")
    void shouldGenerateFemaleGender() {
      config.setParam("type", "FEMALE");

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("女");
    }

    @Test
    @DisplayName("应生成指定的其他性别")
    void shouldGenerateOtherGender() {
      config.setParam("type", "OTHER");

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("其他");
    }

    @Test
    @DisplayName("无效性别类型应使用随机生成")
    void shouldUseRandomGenerationForInvalidType() {
      config.setParam("type", "INVALID");

      String gender = generator.generate(config, context);

      assertThat(gender).isNotNull();
      assertThat(gender).isIn("男", "女", "其他");
    }
  }

  @Nested
  @DisplayName("格式化输出测试")
  class FormatTests {

    @Test
    @DisplayName("CHINESE格式应输出中文性别")
    void shouldFormatGenderAsChinese() {
      config.setParam("format", "CHINESE");
      config.setParam("type", "MALE");

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("男");
    }

    @Test
    @DisplayName("ENGLISH格式应输出英文性别")
    void shouldFormatGenderAsEnglish() {
      config.setParam("format", "ENGLISH");

      String genderMale = generator.generate(config, context);
      String genderFemale = generator.generate(config, context);

      assertThat(genderMale).isIn("Male", "Female", "Other");
      assertThat(genderFemale).isIn("Male", "Female", "Other");
    }

    @Test
    @DisplayName("NUMBER格式应输出数字性别")
    void shouldFormatGenderAsNumber() {
      config.setParam("format", "NUMBER");
      config.setParam("type", "MALE");

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("1");
    }

    @Test
    @DisplayName("SYMBOL格式应输出符号性别")
    void shouldFormatGenderAsSymbol() {
      config.setParam("format", "SYMBOL");

      String gender = generator.generate(config, context);

      assertThat(gender).isIn("M", "F", "O");
    }

    @Test
    @DisplayName("CN格式应等同于CHINESE")
    void shouldTreatCnAsChinese() {
      config.setParam("format", "CN");
      config.setParam("type", "FEMALE");

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("女");
    }

    @Test
    @DisplayName("无效格式应使用默认CHINESE")
    void shouldUseChineseForInvalidFormat() {
      config.setParam("format", "INVALID");

      String gender = generator.generate(config, context);

      assertThat(gender).isIn("男", "女", "其他");
    }

    @Test
    @DisplayName("所有格式应正确转换性别")
    void shouldConvertAllFormatsCorrectly() {
      config.setParam("type", "MALE");

      // 中文
      config.setParam("format", "CHINESE");
      assertThat(generator.generate(config, context)).isEqualTo("男");

      // 英文
      config.setParam("format", "ENGLISH");
      assertThat(generator.generate(config, context)).isEqualTo("Male");

      // 数字
      config.setParam("format", "NUMBER");
      assertThat(generator.generate(config, context)).isEqualTo("1");

      // 符号
      config.setParam("format", "SYMBOL");
      assertThat(generator.generate(config, context)).isEqualTo("M");
    }
  }

  @Nested
  @DisplayName("性别比例测试")
  class GenderRatioTests {

    @Test
    @DisplayName("应尊重男性比例配置")
    void shouldRespectMaleRatio() {
      config.setParam("male_ratio", "0.8"); // 80% 男性

      int maleCount = 0;
      int total = 100;

      for (int i = 0; i < total; i++) {
        String gender = generator.generate(config, context);
        if ("男".equals(gender)) {
          maleCount++;
        }
      }

      // 允许一定的误差范围（70%-90%）
      assertThat(maleCount).isBetween(70, 90);
    }

    @Test
    @DisplayName("应尊重其他性别比例配置")
    void shouldRespectOtherRatio() {
      config.setParam("other_ratio", "0.1"); // 10% 其他性别

      int otherCount = 0;
      int total = 200;

      for (int i = 0; i < total; i++) {
        String gender = generator.generate(config, context);
        if ("其他".equals(gender)) {
          otherCount++;
        }
      }

      // 允许一定的误差范围（约 5%-20%），避免随机波动导致偶发失败
      assertThat(otherCount).isBetween(5, 45);
    }

    @Test
    @DisplayName("极端比例值应被限制在合理范围内")
    void shouldClampRatioValues() {
      config.setParam("male_ratio", "1.5"); // 超过1.0

      String gender = generator.generate(config, context);

      assertThat(gender).isNotNull();
      assertThat(gender).isIn("男", "女", "其他");
    }

    @Test
    @DisplayName("负比例值应被限制为0")
    void shouldClampNegativeRatio() {
      config.setParam("male_ratio", "-0.5");

      String gender = generator.generate(config, context);

      assertThat(gender).isNotNull();
    }

    @Test
    @DisplayName("默认比例应生成接近1:1的男女比例")
    void shouldGenerateBalancedGenderWithDefaultRatio() {
      int maleCount = 0;
      int femaleCount = 0;
      int total = 200;

      for (int i = 0; i < total; i++) {
        String gender = generator.generate(config, context);
        if ("男".equals(gender)) {
          maleCount++;
        } else if ("女".equals(gender)) {
          femaleCount++;
        }
      }

      // 允许一定的误差范围（40%-60%）
      assertThat(maleCount).isBetween(75, 125);
      assertThat(femaleCount).isBetween(75, 125);
    }
  }

  @Nested
  @DisplayName("身份证号关联测试")
  class IdCardLinkTests {

    @Test
    @DisplayName("应从身份证号提取男性性别")
    void shouldExtractMaleGenderFromIdCard() {
      config.setParam("link_idcard", "true");

      // 身份证号第17位为奇数表示男性
      String idCard = "110101199001011234"; // 第17位是3（奇数）
      context.put("idcard", idCard);

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("男");
    }

    @Test
    @DisplayName("应从身份证号提取女性性别")
    void shouldExtractFemaleGenderFromIdCard() {
      config.setParam("link_idcard", "true");

      // 身份证号第17位为偶数表示女性
      // 110101199001011222 - 第17位是2（偶数）
      String idCard = "110101199001011222";
      context.put("idcard", idCard);

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("女");
    }

    @Test
    @DisplayName("禁用身份证关联时应忽略身份证号")
    void shouldIgnoreIdCardWhenLinkDisabled() {
      config.setParam("link_idcard", "false");

      String idCard = "110101199001011234";
      context.put("idcard", idCard);

      String gender = generator.generate(config, context);

      // 应该随机生成，而不是从身份证号提取
      assertThat(gender).isNotNull();
    }

    @Test
    @DisplayName("无效身份证号应使用随机生成")
    void shouldHandleInvalidIdCard() {
      config.setParam("link_idcard", "true");

      context.put("idcard", "invalid");

      String gender = generator.generate(config, context);

      assertThat(gender).isNotNull();
      assertThat(gender).isIn("男", "女", "其他");
    }

    @Test
    @DisplayName("过短身份证号应使用随机生成")
    void shouldHandleShortIdCard() {
      config.setParam("link_idcard", "true");

      context.put("idcard", "12345"); // 长度不足17位

      String gender = generator.generate(config, context);

      assertThat(gender).isNotNull();
      assertThat(gender).isIn("男", "女", "其他");
    }

    @Test
    @DisplayName("身份证号优先级高于随机生成")
    void shouldPreferIdCardOverRandom() {
      config.setParam("link_idcard", "true");

      String idCard = "110101199001011234"; // 男性
      context.put("idcard", idCard);

      String gender = generator.generate(config, context);

      assertThat(gender).isEqualTo("男");
    }
  }

  @Nested
  @DisplayName("上下文存储测试")
  class ContextStorageTests {

    @Test
    @DisplayName("应将性别信息存储到上下文")
    void shouldStoreGenderInContext() {
      config.setParam("type", "MALE");

      generator.generate(config, context);

      String genderFromContext = context.get("gender", String.class).orElse(null);

      assertThat(genderFromContext).isEqualTo("MALE");
    }

    @Test
    @DisplayName("上下文中的性别应为枚举名称")
    void shouldStoreGenderAsEnumName() {
      generator.generate(config, context);

      String genderFromContext = context.get("gender", String.class).orElse(null);

      assertThat(genderFromContext).isNotNull();
      assertThat(genderFromContext).isIn("MALE", "FEMALE", "OTHER");
    }

    @Test
    @DisplayName("每次生成应更新上下文")
    void shouldUpdateContextOnEachGeneration() {
      config.setParam("type", "FEMALE");

      generator.generate(config, context);

      String gender = context.get("gender", String.class).orElse(null);
      assertThat(gender).isEqualTo("FEMALE");

      // 再次生成
      config.setParam("type", "MALE");
      generator.generate(config, context);

      gender = context.get("gender", String.class).orElse(null);
      assertThat(gender).isEqualTo("MALE");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("指定类型时应忽略比例配置")
    void shouldIgnoreRatioWhenTypeSpecified() {
      config.setParam("type", "MALE");
      config.setParam("male_ratio", "0.01"); // 几乎全是女性

      for (int i = 0; i < 20; i++) {
        String gender = generator.generate(config, context);
        assertThat(gender).isEqualTo("男");
      }
    }

    @Test
    @DisplayName("批量生成应保持性能")
    void shouldMaintainPerformanceForBulkGeneration() {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 10000; i++) {
        String gender = generator.generate(config, context);
        assertThat(gender).isNotNull();
      }

      long duration = System.currentTimeMillis() - startTime;

      // 10000个性别应该在2秒内生成
      assertThat(duration).isLessThan(2000);
    }

    @Test
    @DisplayName("所有比例值为0应生成女性")
    void shouldGenerateFemaleWhenAllRatiosZero() {
      config.setParam("male_ratio", "0.0");
      config.setParam("other_ratio", "0.0");

      for (int i = 0; i < 20; i++) {
        String gender = generator.generate(config, context);
        assertThat(gender).isEqualTo("女");
      }
    }

    @Test
    @DisplayName("所有比例值为1应生成其他性别")
    void shouldGenerateOtherWhenAllRatiosOne() {
      config.setParam("male_ratio", "1.0");
      config.setParam("other_ratio", "1.0");

      // other_ratio 优先级最高
      for (int i = 0; i < 20; i++) {
        String gender = generator.generate(config, context);
        assertThat(gender).isEqualTo("其他");
      }
    }
  }
}
