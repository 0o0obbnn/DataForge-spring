package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("StatisticsGenerator 测试")
class StatisticsGeneratorTest {

  private StatisticsGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new StatisticsGenerator();
    config = new SimpleFieldConfig();
    config.setType("statistics");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应生成统计结果")
    void shouldGenerateStatistics() {
      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("应将统计信息存入上下文")
    void shouldStoreStatisticsInContext() {
      generator.generate(config, context);

      assertThat(context.get("statistic_type")).isNotNull();
      assertThat(context.get("statistic_value")).isNotNull();
      assertThat(context.get("dataset_size")).isNotNull();
    }

    @Test
    @DisplayName("应生成数值结果")
    void shouldGenerateNumericResult() {
      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).matches("^[0-9.-]+$");
    }
  }

  @Nested
  @DisplayName("统计类型测试")
  class StatisticTypeTests {

    @Test
    @DisplayName("应计算算术平均数")
    void shouldCalculateMean() {
      config.setParam("type", "MEAN");
      config.setParam("data_size", "10");
      config.setParam("data_min", "0");
      config.setParam("data_max", "100");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("MEAN"));
    }

    @Test
    @DisplayName("应计算中位数")
    void shouldCalculateMedian() {
      config.setParam("type", "MEDIAN");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("MEDIAN"));
    }

    @Test
    @DisplayName("应计算众数")
    void shouldCalculateMode() {
      config.setParam("type", "MODE");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("MODE"));
    }

    @Test
    @DisplayName("应计算方差")
    void shouldCalculateVariance() {
      config.setParam("type", "VARIANCE");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("VARIANCE"));
    }

    @Test
    @DisplayName("应计算标准差")
    void shouldCalculateStandardDeviation() {
      config.setParam("type", "STDDEV");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("STDDEV"));
    }

    @Test
    @DisplayName("应计算极差")
    void shouldCalculateRange() {
      config.setParam("type", "RANGE");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("RANGE"));
    }

    @Test
    @DisplayName("应计算分位数")
    void shouldCalculatePercentile() {
      config.setParam("type", "PERCENTILE");
      config.setParam("percentile", "75");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("PERCENTILE"));
    }

    @Test
    @DisplayName("应计算Z分数")
    void shouldCalculateZScore() {
      config.setParam("type", "ZSCORE");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("ZSCORE"));
    }

    @Test
    @DisplayName("应计算置信区间")
    void shouldCalculateConfidenceInterval() {
      config.setParam("type", "CONFIDENCE_INTERVAL");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("CONFIDENCE_INTERVAL"));
    }

    @Test
    @DisplayName("应计算相关系数")
    void shouldCalculateCorrelation() {
      config.setParam("type", "CORRELATION");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(context.get("statistic_type")).isEqualTo(Optional.of("CORRELATION"));
    }
  }

  @Nested
  @DisplayName("分布类型测试")
  class DistributionTypeTests {

    @Test
    @DisplayName("应使用均匀分布")
    void shouldUseUniformDistribution() {
      config.setParam("distribution", "UNIFORM");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应使用正态分布")
    void shouldUseNormalDistribution() {
      config.setParam("distribution", "NORMAL");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应使用指数分布")
    void shouldUseExponentialDistribution() {
      config.setParam("distribution", "EXPONENTIAL");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应使用泊松分布")
    void shouldUsePoissonDistribution() {
      config.setParam("distribution", "POISSON");
      config.setParam("data_size", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("输出格式测试")
  class OutputFormatTests {

    @Test
    @DisplayName("应输出纯数字格式")
    void shouldOutputNumberFormat() {
      config.setParam("format", "NUMBER");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).matches("^[0-9.-]+$");
    }

    @Test
    @DisplayName("应输出格式化字符串")
    void shouldOutputFormattedString() {
      config.setParam("format", "FORMATTED");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).contains(":");
    }

    @Test
    @DisplayName("应输出JSON格式")
    void shouldOutputJsonFormat() {
      config.setParam("format", "JSON");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).startsWith("{");
      assertThat(result).endsWith("}");
    }

    @Test
    @DisplayName("应输出详细格式")
    void shouldOutputVerboseFormat() {
      config.setParam("format", "VERBOSE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).contains("(");
    }
  }

  @Nested
  @DisplayName("数据集参数测试")
  class DatasetParameterTests {

    @Test
    @DisplayName("应支持自定义数据集大小")
    void shouldSupportCustomDatasetSize() {
      config.setParam("data_size", "50");

      generator.generate(config, context);

      assertThat(context.get("dataset_size")).isEqualTo(Optional.of(50));
    }

    @Test
    @DisplayName("应支持自定义数据范围")
    void shouldSupportCustomDataRange() {
      config.setParam("data_min", "10");
      config.setParam("data_max", "90");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持正态分布参数")
    void shouldSupportNormalDistributionParams() {
      config.setParam("distribution", "NORMAL");
      config.setParam("mean", "50");
      config.setParam("stddev", "10");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持指数分布参数")
    void shouldSupportExponentialDistributionParams() {
      config.setParam("distribution", "EXPONENTIAL");
      config.setParam("lambda", "0.5");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("分位数测试")
  class PercentileTests {

    @ParameterizedTest
    @ValueSource(ints = {0, 25, 50, 75, 100})
    @DisplayName("应支持不同分位数值")
    void shouldSupportDifferentPercentiles(int percentile) {
      config.setParam("type", "PERCENTILE");
      config.setParam("percentile", String.valueOf(percentile));

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持25分位数")
    void shouldSupport25thPercentile() {
      config.setParam("type", "PERCENTILE");
      config.setParam("percentile", "25");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持75分位数")
    void shouldSupport75thPercentile() {
      config.setParam("type", "PERCENTILE");
      config.setParam("percentile", "75");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持90分位数")
    void shouldSupport90thPercentile() {
      config.setParam("type", "PERCENTILE");
      config.setParam("percentile", "90");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("置信区间测试")
  class ConfidenceIntervalTests {

    @Test
    @DisplayName("应支持95%置信水平")
    void shouldSupport95ConfidenceLevel() {
      config.setParam("type", "CONFIDENCE_INTERVAL");
      config.setParam("confidence_level", "0.95");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持99%置信水平")
    void shouldSupport99ConfidenceLevel() {
      config.setParam("type", "CONFIDENCE_INTERVAL");
      config.setParam("confidence_level", "0.99");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持90%置信水平")
    void shouldSupport90ConfidenceLevel() {
      config.setParam("type", "CONFIDENCE_INTERVAL");
      config.setParam("confidence_level", "0.90");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("精度测试")
  class PrecisionTests {

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 4, 6, 8})
    @DisplayName("应支持不同精度")
    void shouldSupportDifferentPrecisions(int precision) {
      config.setParam("precision", String.valueOf(precision));

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持高精度输出")
    void shouldSupportHighPrecision() {
      config.setParam("precision", "8");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应支持低精度输出")
    void shouldSupportLowPrecision() {
      config.setParam("precision", "0");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("原始数据测试")
  class RawDataTests {

    @Test
    @DisplayName("应包含原始数据")
    void shouldIncludeRawData() {
      config.setParam("format", "JSON");
      config.setParam("include_raw_data", "true");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).contains("raw_data");
    }

    @Test
    @DisplayName("应不包含原始数据")
    void shouldNotIncludeRawData() {
      config.setParam("format", "JSON");
      config.setParam("include_raw_data", "false");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
      assertThat(result).doesNotContain("raw_data");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("应处理最小数据集")
    void shouldHandleMinimumDataset() {
      config.setParam("data_size", "1");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应处理大数据集")
    void shouldHandleLargeDataset() {
      config.setParam("data_size", "1000");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应处理相同最小最大值")
    void shouldHandleSameMinMax() {
      config.setParam("data_min", "50");
      config.setParam("data_max", "50");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("应处理负值范围")
    void shouldHandleNegativeRange() {
      config.setParam("data_min", "-100");
      config.setParam("data_max", "100");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("null配置应返回默认值")
    void shouldReturnDefaultForNullConfig() {
      String result = generator.generate(null, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("null上下文应不抛出异常")
    void shouldNotThrowForNullContext() {
      String result = generator.generate(config, null);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("无效统计类型应使用默认值")
    void shouldUseDefaultForInvalidStatisticType() {
      config.setParam("type", "INVALID_TYPE");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("无效分布类型应使用默认值")
    void shouldUseDefaultForInvalidDistribution() {
      config.setParam("distribution", "INVALID_DISTRIBUTION");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("无效格式应使用默认值")
    void shouldUseDefaultForInvalidFormat() {
      config.setParam("format", "INVALID_FORMAT");

      String result = generator.generate(config, context);

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("性能测试")
  class PerformanceTests {

    @Test
    @DisplayName("批量生成应高效")
    void shouldGenerateBatchEfficiently() {
      int count = 100;
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < count; i++) {
        generator.generate(config, context);
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("大数据集计算应高效")
    void shouldCalculateLargeDatasetEfficiently() {
      config.setParam("data_size", "1000");

      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 10; i++) {
        generator.generate(config, context);
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(10000);
    }

    @Test
    @DisplayName("复杂统计计算应高效")
    void shouldCalculateComplexStatisticsEfficiently() {
      config.setParam("type", "CONFIDENCE_INTERVAL");
      config.setParam("format", "JSON");
      config.setParam("include_raw_data", "true");

      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 50; i++) {
        generator.generate(config, context);
      }

      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isLessThan(10000);
    }
  }

  @Nested
  @DisplayName("生成器信息测试")
  class GeneratorInfoTests {

    @Test
    @DisplayName("应返回正确的类型")
    void shouldReturnCorrectType() {
      String type = generator.getType();

      assertThat(type).isEqualTo("statistics");
    }

    @Test
    @DisplayName("应返回正确的配置类")
    void shouldReturnCorrectConfigClass() {
      Class<?> configClass = generator.getConfigClass();

      assertThat(configClass).isEqualTo(com.dataforge.model.FieldConfig.class);
    }
  }
}
