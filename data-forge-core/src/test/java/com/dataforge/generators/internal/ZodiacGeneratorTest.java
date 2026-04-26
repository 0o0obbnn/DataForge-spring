package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.GeneratorTestBase;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * ZodiacGenerator 完整测试类
 *
 * <p>测试星座生成器的所有业务逻辑，包括星座类型、格式、日期关联等功能
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("ZodiacGenerator 测试")
class ZodiacGeneratorTest extends GeneratorTestBase<ZodiacGenerator> {

  @Override
  protected ZodiacGenerator createGenerator() {
    return new ZodiacGenerator();
  }

  @Override
  protected String getGeneratorType() {
    return "zodiac";
  }

  @Override
  protected String getBusinessLogicPattern() {
    // 匹配星座名称（中文或英文）或星座符号
    return "^(白羊座|金牛座|双子座|巨蟹座|狮子座|处女座|天秤座|天蝎座|射手座|摩羯座|水瓶座|双鱼座|Aries|Taurus|Gemini|Cancer|Leo|Virgo|Libra|Scorpio|Sagittarius|Capricorn|Aquarius|Pisces|♈|♉|♊|♋|♌|♍|♎|♏|♐|♑|♒|♓)$";
  }

  @Override
  protected Map<String, String> getSupportedParameters() {
    return Map.of(
        "sign", "ARIES",
        "format", "CHINESE",
        "birth_date_related", "true");
  }

  @Nested
  @DisplayName("星座类型测试")
  class ZodiacTypeTests {

    @ParameterizedTest
    @CsvSource({
      "ARIES, 白羊座",
      "TAURUS, 金牛座",
      "GEMINI, 双子座",
      "CANCER, 巨蟹座",
      "LEO, 狮子座",
      "VIRGO, 处女座",
      "LIBRA, 天秤座",
      "SCORPIO, 天蝎座",
      "SAGITTARIUS, 射手座",
      "CAPRICORN, 摩羯座",
      "AQUARIUS, 水瓶座",
      "PISCES, 双鱼座"
    })
    @DisplayName("应生成指定星座类型")
    void shouldGenerateSpecificZodiacSign(String sign, String expectedChinese) {
      config.setParam("sign", sign);
      config.setParam("format", "CHINESE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(expectedChinese);
    }

    @Test
    @DisplayName("应生成随机星座当未指定类型时")
    void shouldGenerateRandomZodiacWhenSignNotSpecified() {
      // 不设置sign参数，让生成器随机选择
      config.setParam("format", "CHINESE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).matches(getBusinessLogicPattern());
    }

    @Test
    @DisplayName("应处理无效星座类型")
    void shouldHandleInvalidZodiacSign() {
      config.setParam("sign", "INVALID_SIGN");
      config.setParam("format", "CHINESE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).matches(getBusinessLogicPattern());
    }
  }

  @Nested
  @DisplayName("输出格式测试")
  class OutputFormatTests {

    @ParameterizedTest
    @CsvSource({"CHINESE, 白羊座", "ENGLISH, Aries", "SYMBOL, ♈", "CODE, ARIES"})
    @DisplayName("应生成指定格式的星座")
    void shouldGenerateZodiacInSpecifiedFormat(String format, String expectedValue) {
      config.setParam("sign", "ARIES");
      config.setParam("format", format);

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(expectedValue);
    }

    @Test
    @DisplayName("应使用默认格式当未指定时")
    void shouldUseDefaultFormatWhenNotSpecified() {
      config.setParam("sign", "ARIES");
      // 不设置format参数

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isEqualTo("白羊座"); // 默认应该是中文
    }

    @Test
    @DisplayName("应处理无效格式参数")
    void shouldHandleInvalidFormat() {
      config.setParam("sign", "ARIES");
      config.setParam("format", "INVALID_FORMAT");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).matches(getBusinessLogicPattern());
    }
  }

  @Nested
  @DisplayName("日期关联测试")
  class DateRelatedTests {

    @ParameterizedTest
    @CsvSource({
      "1990-03-21, ARIES",
      "1990-04-20, TAURUS",
      "1990-05-21, GEMINI",
      "1990-06-21, CANCER",
      "1990-07-23, LEO",
      "1990-08-23, VIRGO",
      "1990-09-23, LIBRA",
      "1990-10-23, SCORPIO",
      "1990-11-22, SAGITTARIUS",
      "1990-12-22, CAPRICORN",
      "1990-01-20, AQUARIUS",
      "1990-02-19, PISCES"
    })
    @DisplayName("应根据出生日期生成对应星座")
    void shouldGenerateZodiacBasedOnBirthDate(String birthDate, String expectedSign) {
      config.setParam("birth_date_related", "true");
      config.setParam("birth_date", birthDate);
      config.setParam("format", "CODE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isEqualTo(expectedSign);
    }

    @Test
    @DisplayName("应处理无效日期格式")
    void shouldHandleInvalidDateFormat() {
      config.setParam("birth_date_related", "true");
      config.setParam("birth_date", "invalid-date");
      config.setParam("format", "CHINESE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).matches(getBusinessLogicPattern());
    }

    @Test
    @DisplayName("应忽略日期关联当设置为false时")
    void shouldIgnoreDateRelationWhenSetToFalse() {
      config.setParam("birth_date_related", "false");
      config.setParam("birth_date", "1990-03-21");
      config.setParam("sign", "TAURUS");
      config.setParam("format", "CHINESE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isEqualTo("金牛座"); // 应该使用指定的sign而不是日期
    }
  }

  @Nested
  @DisplayName("上下文信息测试")
  class ContextInformationTests {

    @Test
    @DisplayName("应将星座信息存入上下文")
    void shouldStoreZodiacInfoInContext() {
      config.setParam("sign", "ARIES");
      config.setParam("format", "CHINESE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("zodiac_sign", String.class).orElse(null)).isEqualTo("ARIES");
      assertThat(context.get("zodiac_chinese", String.class).orElse(null)).isEqualTo("白羊座");
      assertThat(context.get("zodiac_english", String.class).orElse(null)).isEqualTo("Aries");
      assertThat(context.get("zodiac_symbol", String.class).orElse(null)).isEqualTo("♈");
    }

    @Test
    @DisplayName("应存储日期关联信息")
    void shouldStoreDateRelationInfo() {
      config.setParam("birth_date_related", "true");
      config.setParam("birth_date", "1990-03-21");
      config.setParam("format", "CHINESE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("birth_date", String.class).orElse(null)).isEqualTo("1990-03-21");
      assertThat(context.get("calculated_zodiac", String.class).orElse(null)).isEqualTo("ARIES");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryConditionTests {

    @Test
    @DisplayName("应处理边界日期")
    void shouldHandleBoundaryDates() {
      // 测试星座边界日期
      String[] boundaryDates = {
        "1990-03-21", // 白羊座开始
        "1990-04-19", // 白羊座结束
        "1990-04-20", // 金牛座开始
        "1990-12-31", // 摩羯座
        "1990-01-01" // 摩羯座
      };

      for (String date : boundaryDates) {
        config.setParam("birth_date_related", "true");
        config.setParam("birth_date", date);
        config.setParam("format", "CODE");

        String result = generator.generate(config, context);
        assertThat(result).isNotNull();
        assertThat(result).matches("^[A-Z]+$");
      }
    }

    @Test
    @DisplayName("应处理闰年日期")
    void shouldHandleLeapYearDates() {
      config.setParam("birth_date_related", "true");
      config.setParam("birth_date", "2000-02-29"); // 闰年
      config.setParam("format", "CHINESE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).matches(getBusinessLogicPattern());
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("应快速生成大量星座数据")
    void shouldGenerateLargeVolumeQuickly() {
      int iterations = 1000;
      long startTime = System.nanoTime();

      for (int i = 0; i < iterations; i++) {
        String result = generator.generate(config, context);
        assertThat(result).isNotNull();
      }

      long duration = System.nanoTime() - startTime;
      long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(duration);

      // 1000次生成应该在1秒内完成
      assertThat(durationMs).isLessThan(1000);
    }

    @Test
    @DisplayName("应具有稳定的性能表现")
    void shouldHaveStablePerformance() {
      int iterations = 100;
      long[] durations = new long[5];

      for (int run = 0; run < 5; run++) {
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
          generator.generate(config, context);
        }

        durations[run] = System.nanoTime() - startTime;
      }

      // 验证性能波动不超过 150%（JVM 预热/GC/负载导致波动大，避免 CI 偶发失败）
      long average = (durations[0] + durations[1] + durations[2] + durations[3] + durations[4]) / 5;
      for (long duration : durations) {
        double deviation = average == 0 ? 0 : Math.abs((double) (duration - average) / average);
        assertThat(deviation).isLessThan(1.5);
      }
    }
  }
}
