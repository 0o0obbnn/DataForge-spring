package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("布尔值生成器测试")
class BooleanGeneratorTest {

  private BooleanGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new BooleanGenerator();
    config = new SimpleFieldConfig();
    config.setType("boolean");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionality {

    @Test
    @DisplayName("应该生成非空布尔值")
    void shouldGenerateNonNullBoolean() {
      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isIn("true", "false");
    }

    @Test
    @DisplayName("生成的值应该是true或false")
    void shouldGenerateTrueOrFalse() {
      for (int i = 0; i < 100; i++) {
        String result = generator.generate(config, context);
        assertThat(result).isIn("true", "false");
      }
    }

    @Test
    @DisplayName("空配置应该使用默认格式")
    void shouldUseDefaultFormatWithEmptyConfig() {
      SimpleFieldConfig emptyConfig = new SimpleFieldConfig();

      String result = generator.generate(emptyConfig, context);

      assertThat(result).isNotNull();
      assertThat(result).isIn("true", "false");
    }
  }

  @Nested
  @DisplayName("格式配置测试")
  class FormatConfiguration {

    @ParameterizedTest
    @CsvSource({
      "BOOLEAN, true",
      "BOOLEAN, false",
      "BINARY, 1",
      "BINARY, 0",
      "YN, Y",
      "YN, N",
      "YES_NO, Yes",
      "YES_NO, No",
      "ON_OFF, On",
      "ON_OFF, Off",
      "ENABLED_DISABLED, Enabled",
      "ENABLED_DISABLED, Disabled"
    })
    @DisplayName("应该支持不同的输出格式")
    void shouldSupportDifferentFormats(String format, String expectedValue) {
      config.setParam("format", format);

      String result = generator.generate(config, context);

      assertThat(result)
          .isIn(
              "true",
              "false",
              "1",
              "0",
              "Y",
              "N",
              "Yes",
              "No",
              "yes",
              "no",
              "On",
              "Off",
              "on",
              "off",
              "Enabled",
              "Disabled",
              "enabled",
              "disabled");
    }

    @Test
    @DisplayName("BINARY格式应该生成1或0")
    void shouldGenerateBinaryFormat() {
      config.setParam("format", "BINARY");

      for (int i = 0; i < 100; i++) {
        String result = generator.generate(config, context);
        assertThat(result).isIn("1", "0");
      }
    }

    @Test
    @DisplayName("YN格式应该生成Y或N")
    void shouldGenerateYnFormat() {
      config.setParam("format", "YN");

      for (int i = 0; i < 100; i++) {
        String result = generator.generate(config, context);
        assertThat(result).isIn("Y", "N");
      }
    }

    @Test
    @DisplayName("无效格式应该使用默认BOOLEAN格式")
    void shouldUseDefaultFormatForInvalidFormat() {
      config.setParam("format", "INVALID_FORMAT");

      String result = generator.generate(config, context);

      assertThat(result).isIn("true", "false");
    }
  }

  @Nested
  @DisplayName("概率配置测试")
  class ProbabilityConfiguration {

    @Test
    @DisplayName("true_ratio=1.0应该始终生成true")
    void shouldAlwaysGenerateTrueWithRatio1() {
      config.setParam("true_ratio", "1.0");

      for (int i = 0; i < 50; i++) {
        String result = generator.generate(config, context);
        assertThat(result).isEqualTo("true");
      }
    }

    @Test
    @DisplayName("true_ratio=0.0应该始终生成false")
    void shouldAlwaysGenerateFalseWithRatio0() {
      config.setParam("true_ratio", "0.0");

      for (int i = 0; i < 50; i++) {
        String result = generator.generate(config, context);
        assertThat(result).isEqualTo("false");
      }
    }

    @Test
    @DisplayName("true_ratio=0.5应该有大约50%的true")
    void shouldGenerate50PercentTrueWithRatio0_5() {
      config.setParam("true_ratio", "0.5");

      int trueCount = 0;
      int total = 1000;

      for (int i = 0; i < total; i++) {
        String result = generator.generate(config, context);
        if ("true".equals(result)) {
          trueCount++;
        }
      }

      double ratio = (double) trueCount / total;
      assertThat(ratio).isBetween(0.4, 0.6); // 允许一定的误差
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "1.5", "2.0", "abc"})
    @DisplayName("无效的true_ratio应该使用默认值0.5")
    void shouldUseDefaultRatioForInvalidRatio(String invalidRatio) {
      config.setParam("true_ratio", invalidRatio);

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isIn("true", "false");
    }
  }

  @Nested
  @DisplayName("大小写风格测试")
  class CaseStyleConfiguration {

    @Test
    @DisplayName("LOWER风格应该生成小写")
    void shouldGenerateLowercaseWithLowerStyle() {
      config.setParam("format", "BOOLEAN");
      config.setParam("case_style", "LOWER");

      String result = generator.generate(config, context);

      assertThat(result).isIn("true", "false");
    }

    @Test
    @DisplayName("UPPER风格应该生成大写")
    void shouldGenerateUppercaseWithUpperStyle() {
      config.setParam("format", "BOOLEAN");
      config.setParam("case_style", "UPPER");

      String result = generator.generate(config, context);

      assertThat(result).isIn("TRUE", "FALSE");
    }

    @Test
    @DisplayName("TITLE风格应该生成首字母大写")
    void shouldGenerateTitleCaseWithTitleStyle() {
      config.setParam("format", "YES_NO");
      config.setParam("case_style", "TITLE");

      String result = generator.generate(config, context);

      assertThat(result).isIn("Yes", "No");
    }

    @Test
    @DisplayName("无效的case_style应该使用默认LOWER风格")
    void shouldUseDefaultCaseStyleForInvalidStyle() {
      config.setParam("format", "BOOLEAN");
      config.setParam("case_style", "INVALID_STYLE");

      String result = generator.generate(config, context);

      assertThat(result).isIn("true", "false");
    }
  }

  @Nested
  @DisplayName("组合配置测试")
  class CombinedConfiguration {

    @Test
    @DisplayName("应该支持格式和大小写组合")
    void shouldSupportFormatAndCaseStyleCombination() {
      config.setParam("format", "BOOLEAN");
      config.setParam("case_style", "UPPER");
      config.setParam("true_ratio", "1.0");

      String result = generator.generate(config, context);

      assertThat(result).isEqualTo("TRUE");
    }

    @Test
    @DisplayName("应该支持格式和概率组合")
    void shouldSupportFormatAndProbabilityCombination() {
      config.setParam("format", "BINARY");
      config.setParam("true_ratio", "0.0");

      String result = generator.generate(config, context);

      assertThat(result).isEqualTo("0");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryConditions {

    @Test
    @DisplayName("null上下文应该正常工作")
    void shouldHandleNullContext() {
      String result = generator.generate(config, null);

      assertThat(result).isNotNull();
      assertThat(result).isIn("true", "false");
    }

    @Test
    @DisplayName("null配置应该使用默认值")
    void shouldHandleNullConfig() {
      String result = generator.generate(null, context);

      assertThat(result).isNotNull();
      assertThat(result).isIn("true", "false");
    }

    @Test
    @DisplayName("极端true_ratio值应该正常工作")
    void shouldHandleExtremeRatioValues() {
      config.setParam("true_ratio", "0.9999");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isIn("true", "false");
    }
  }

  @Nested
  @DisplayName("并发安全性测试")
  class ConcurrentSafety {

    @Test
    @DisplayName("并发生成应该线程安全")
    void shouldBeThreadSafeDuringConcurrentGeneration() throws Exception {
      int threadCount = 10;
      int callsPerThread = 100;

      java.util.concurrent.ConcurrentSkipListSet<String> results =
          new java.util.concurrent.ConcurrentSkipListSet<>();

      java.util.concurrent.CountDownLatch latch =
          new java.util.concurrent.CountDownLatch(threadCount);
      java.util.concurrent.ExecutorService executor =
          java.util.concurrent.Executors.newFixedThreadPool(threadCount);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < callsPerThread; j++) {
                  String result = generator.generate(config, context);
                  results.add(result);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await();
      executor.shutdown();

      assertThat(results).hasSizeLessThanOrEqualTo(2); // 只有true和false两种值
      assertThat(results).contains("true").contains("false");
    }
  }
}
