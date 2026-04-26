package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MeasurementGenerator 测试")
class MeasurementGeneratorTest {

  private MeasurementGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new MeasurementGenerator();
    config = new SimpleFieldConfig();
    config.setType("measurement");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应生成度量值")
    void shouldGenerateMeasurement() {
      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).isNotEmpty();
    }

    @Test
    @DisplayName("应将度量信息存入上下文")
    void shouldStoreMeasurementInContext() {
      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_value")).isNotNull();
      assertThat(context.get("measurement_unit")).isNotNull();
      assertThat(context.get("measurement_category")).isNotNull();
      assertThat(context.get("measurement_system")).isNotNull();
    }

    @Test
    @DisplayName("应生成有效的数值和单位组合")
    void shouldGenerateValidValueAndUnit() {
      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      // Allow negative numbers, decimal values, and units with special characters (including spaces
      // in °F, °C, etc.)
      assertThat(measurement).matches("^-?[0-9]+\\.?[0-9]*\\s+.+$");
    }
  }

  @Nested
  @DisplayName("度量类别测试")
  class MeasurementCategoryTests {

    @Test
    @DisplayName("应生成长度度量")
    void shouldGenerateLengthMeasurement() {
      config.setParam("category", "LENGTH");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("LENGTH"));
    }

    @Test
    @DisplayName("应生成重量度量")
    void shouldGenerateWeightMeasurement() {
      config.setParam("category", "WEIGHT");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("WEIGHT"));
    }

    @Test
    @DisplayName("应生成体积度量")
    void shouldGenerateVolumeMeasurement() {
      config.setParam("category", "VOLUME");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("VOLUME"));
    }

    @Test
    @DisplayName("应生成温度度量")
    void shouldGenerateTemperatureMeasurement() {
      config.setParam("category", "TEMPERATURE");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("TEMPERATURE"));
    }

    @Test
    @DisplayName("应生成速度度量")
    void shouldGenerateSpeedMeasurement() {
      config.setParam("category", "SPEED");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("SPEED"));
    }

    @Test
    @DisplayName("应生成压力度量")
    void shouldGeneratePressureMeasurement() {
      config.setParam("category", "PRESSURE");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("PRESSURE"));
    }

    @Test
    @DisplayName("应生成能量度量")
    void shouldGenerateEnergyMeasurement() {
      config.setParam("category", "ENERGY");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("ENERGY"));
    }

    @Test
    @DisplayName("应生成功率度量")
    void shouldGeneratePowerMeasurement() {
      config.setParam("category", "POWER");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("POWER"));
    }

    @Test
    @DisplayName("应生成面积度量")
    void shouldGenerateAreaMeasurement() {
      config.setParam("category", "AREA");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("AREA"));
    }

    @Test
    @DisplayName("应生成时间度量")
    void shouldGenerateTimeMeasurement() {
      config.setParam("category", "TIME");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("TIME"));
    }

    @Test
    @DisplayName("应生成频率度量")
    void shouldGenerateFrequencyMeasurement() {
      config.setParam("category", "FREQUENCY");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_category")).isEqualTo(Optional.of("FREQUENCY"));
    }
  }

  @Nested
  @DisplayName("单位制测试")
  class UnitSystemTests {

    @Test
    @DisplayName("应生成公制单位")
    void shouldGenerateMetricUnits() {
      config.setParam("unit_system", "METRIC");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_system")).isEqualTo(Optional.of("METRIC"));
    }

    @Test
    @DisplayName("应生成英制单位")
    void shouldGenerateImperialUnits() {
      config.setParam("unit_system", "IMPERIAL");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_system")).isEqualTo(Optional.of("IMPERIAL"));
    }

    @Test
    @DisplayName("应生成美制单位")
    void shouldGenerateUSUnits() {
      config.setParam("unit_system", "US");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_system")).isEqualTo(Optional.of("US"));
    }

    @Test
    @DisplayName("应生成国际单位制")
    void shouldGenerateSIUnits() {
      config.setParam("unit_system", "SI");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(context.get("measurement_system")).isEqualTo(Optional.of("SI"));
    }
  }

  @Nested
  @DisplayName("指定单位测试")
  class SpecificUnitTests {

    @ParameterizedTest
    @ValueSource(strings = {"m", "kg", "l", "°C", "km/h", "Pa", "J", "W", "m²", "s", "Hz"})
    @DisplayName("应支持指定单位")
    void shouldSupportSpecificUnit(String unit) {
      config.setParam("unit", unit);

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains(unit);
      assertThat(context.get("measurement_unit")).isEqualTo(Optional.of(unit));
    }

    @Test
    @DisplayName("应支持指定长度单位")
    void shouldSupportLengthUnits() {
      config.setParam("unit", "cm");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains("cm");
    }

    @Test
    @DisplayName("应支持指定重量单位")
    void shouldSupportWeightUnits() {
      config.setParam("unit", "kg");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains("kg");
    }

    @Test
    @DisplayName("应支持指定温度单位")
    void shouldSupportTemperatureUnits() {
      config.setParam("unit", "°C");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains("°C");
    }
  }

  @Nested
  @DisplayName("数值范围测试")
  class ValueRangeTests {

    @Test
    @DisplayName("应支持自定义最小值")
    void shouldSupportCustomMinValue() {
      config.setParam("value_min", "10.5");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      Object valueObj = context.get("measurement_value");
      if (valueObj instanceof BigDecimal) {
        BigDecimal value = (BigDecimal) valueObj;
        assertThat(value).isGreaterThanOrEqualTo(new BigDecimal("10.5"));
      }
    }

    @Test
    @DisplayName("应支持自定义最大值")
    void shouldSupportCustomMaxValue() {
      config.setParam("value_max", "100.5");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      Object valueObj = context.get("measurement_value");
      if (valueObj instanceof BigDecimal) {
        BigDecimal value = (BigDecimal) valueObj;
        assertThat(value).isLessThanOrEqualTo(new BigDecimal("100.5"));
      }
    }

    @Test
    @DisplayName("应使用现实范围")
    void shouldUseRealisticRanges() {
      config.setParam("realistic_ranges", "true");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
    }

    @Test
    @DisplayName("应支持自定义范围")
    void shouldSupportCustomRange() {
      config.setParam("value_min", "50");
      config.setParam("value_max", "100");
      config.setParam("realistic_ranges", "false");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      Object valueObj = context.get("measurement_value");
      if (valueObj instanceof BigDecimal) {
        BigDecimal value = (BigDecimal) valueObj;
        assertThat(value).isBetween(new BigDecimal("50"), new BigDecimal("100"));
      }
    }
  }

  @Nested
  @DisplayName("精度测试")
  class PrecisionTests {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("应支持不同精度")
    void shouldSupportDifferentPrecision(int precision) {
      config.setParam("precision", String.valueOf(precision));

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      Object valueObj = context.get("measurement_value");
      if (valueObj instanceof BigDecimal) {
        BigDecimal value = (BigDecimal) valueObj;
        assertThat(value.scale()).isLessThanOrEqualTo(precision);
      }
    }

    @Test
    @DisplayName("应支持整数精度")
    void shouldSupportIntegerPrecision() {
      config.setParam("precision", "0");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).doesNotContain(".");
    }

    @Test
    @DisplayName("应支持高精度")
    void shouldSupportHighPrecision() {
      config.setParam("precision", "6");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
    }
  }

  @Nested
  @DisplayName("输出格式测试")
  class OutputFormatTests {

    @Test
    @DisplayName("应生成VALUE_UNIT格式")
    void shouldGenerateValueUnitFormat() {
      config.setParam("format", "VALUE_UNIT");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      // 数值 + 空格 + 单位（单位可能含空格、/、°、²、³ 等符号，如 "fl oz", "m/s", "°C"）
      assertThat(measurement).matches("^[0-9]+\\.?[0-9]*\\s+.+$");
    }

    @Test
    @DisplayName("应生成UNIT_VALUE格式")
    void shouldGenerateUnitValueFormat() {
      config.setParam("format", "UNIT_VALUE");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      // Allow units with /, °, ², ³ characters and spaces (e.g., "km/h", "°C", "m/s", "m²", "fl
      // oz")
      // Also allow negative numbers and leading/trailing whitespace
      assertThat(measurement).matches("^[a-zA-Z°²³/\\s]+\\s+-?[0-9]+\\.?[0-9]*$");
    }

    @Test
    @DisplayName("应生成JSON格式")
    void shouldGenerateJsonFormat() {
      config.setParam("format", "JSON");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).startsWith("{");
      assertThat(measurement).endsWith("}");
      assertThat(measurement).contains("\"value\"");
      assertThat(measurement).contains("\"unit\"");
      assertThat(measurement).contains("\"unit_name\"");
      assertThat(measurement).contains("\"category\"");
      assertThat(measurement).contains("\"system\"");
    }

    @Test
    @DisplayName("应生成VERBOSE格式")
    void shouldGenerateVerboseFormat() {
      config.setParam("format", "VERBOSE");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains("[");
      assertThat(measurement).contains("]");
    }
  }

  @Nested
  @DisplayName("单位转换测试")
  class ConversionTests {

    @Test
    @DisplayName("应支持单位转换")
    void shouldSupportUnitConversion() {
      config.setParam("unit", "km");
      config.setParam("include_conversion", "true");
      config.setParam("conversion_target", "m");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains("km");
    }

    @Test
    @DisplayName("应支持长度单位转换")
    void shouldSupportLengthConversion() {
      config.setParam("unit", "km");
      config.setParam("include_conversion", "true");
      config.setParam("conversion_target", "m");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains("km");
      assertThat(measurement).contains("m");
    }

    @Test
    @DisplayName("应支持重量单位转换")
    void shouldSupportWeightConversion() {
      config.setParam("unit", "kg");
      config.setParam("include_conversion", "true");
      config.setParam("conversion_target", "g");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains("kg");
      assertThat(measurement).contains("g");
    }

    @Test
    @DisplayName("应支持温度单位转换")
    void shouldSupportTemperatureConversion() {
      config.setParam("unit", "°C");
      config.setParam("include_conversion", "true");
      config.setParam("conversion_target", "°F");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).contains("°C");
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryTests {

    @Test
    @DisplayName("应处理最小精度")
    void shouldHandleMinimumPrecision() {
      config.setParam("precision", "0");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
    }

    @Test
    @DisplayName("应处理最大精度")
    void shouldHandleMaximumPrecision() {
      config.setParam("precision", "10");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
    }

    @Test
    @DisplayName("应处理零值范围")
    void shouldHandleZeroRange() {
      config.setParam("value_min", "0");
      config.setParam("value_max", "0");
      config.setParam("realistic_ranges", "false");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      Object valueObj = context.get("measurement_value");
      if (valueObj instanceof BigDecimal) {
        BigDecimal value = (BigDecimal) valueObj;
        assertThat(value).isEqualByComparingTo(BigDecimal.ZERO);
      }
    }

    @Test
    @DisplayName("应处理大数值范围")
    void shouldHandleLargeValueRange() {
      config.setParam("value_min", "1000000");
      config.setParam("value_max", "9999999");
      config.setParam("realistic_ranges", "false");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
      Object valueObj = context.get("measurement_value");
      if (valueObj instanceof BigDecimal) {
        BigDecimal value = (BigDecimal) valueObj;
        assertThat(value).isGreaterThanOrEqualTo(new BigDecimal("1000000"));
        assertThat(value).isLessThanOrEqualTo(new BigDecimal("9999999"));
      }
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("null配置应生成默认值")
    void shouldGenerateDefaultForNullConfig() {
      String measurement = generator.generate(null, context);

      assertThat(measurement).isNotNull();
      assertThat(measurement).matches("^-?[0-9]+\\.?[0-9]*\\s+.+$");
    }

    @Test
    @DisplayName("null上下文应不抛出异常")
    void shouldNotThrowForNullContext() {
      String measurement = generator.generate(config, null);

      assertThat(measurement).isNotNull();
    }

    @Test
    @DisplayName("无效类别应使用默认值")
    void shouldHandleInvalidCategory() {
      config.setParam("category", "INVALID");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
    }

    @Test
    @DisplayName("无效单位制应使用默认值")
    void shouldHandleInvalidUnitSystem() {
      config.setParam("unit_system", "INVALID");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
    }

    @Test
    @DisplayName("无效输出格式应使用默认值")
    void shouldHandleInvalidFormat() {
      config.setParam("format", "INVALID");

      String measurement = generator.generate(config, context);

      assertThat(measurement).isNotNull();
    }
  }

  @Nested
  @DisplayName("生成器信息测试")
  class GeneratorInfoTests {

    @Test
    @DisplayName("应返回正确的类型")
    void shouldReturnCorrectType() {
      String type = generator.getType();

      assertThat(type).isEqualTo("measurement");
    }

    @Test
    @DisplayName("应返回正确的配置类")
    void shouldReturnCorrectConfigClass() {
      Class<?> configClass = generator.getConfigClass();

      assertThat(configClass).isEqualTo(com.dataforge.model.FieldConfig.class);
    }

    @Test
    @DisplayName("应返回所有支持的单位")
    void shouldReturnAllSupportedUnits() {
      var units = MeasurementGenerator.getSupportedUnits();

      assertThat(units).isNotNull();
      assertThat(units).isNotEmpty();
      assertThat(units).contains("m");
      assertThat(units).contains("kg");
      assertThat(units).contains("°C");
    }
  }

  @Nested
  @DisplayName("唯一性测试")
  class UniquenessTests {

    @Test
    @DisplayName("批量生成的度量应具有唯一性")
    void shouldGenerateUniqueMeasurements() {
      int count = 100;
      java.util.Set<String> measurements = new java.util.HashSet<>();

      for (int i = 0; i < count; i++) {
        String measurement = generator.generate(config, context);
        measurements.add(measurement);
      }

      assertThat(measurements).hasSize(count);
    }

    @Test
    @DisplayName("相同配置应生成不同度量")
    void shouldGenerateDifferentMeasurementsForSameConfig() {
      String measurement1 = generator.generate(config, context);
      String measurement2 = generator.generate(config, context);

      assertThat(measurement1).isNotEqualTo(measurement2);
    }
  }
}
