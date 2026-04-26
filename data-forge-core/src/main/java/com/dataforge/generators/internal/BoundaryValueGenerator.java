package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 边界值/极端值生成器
 *
 * <p>根据DataForge设计文档第12.2节：边界值/极端值 (Boundary / Extreme Values) 针对数值型、字符串型、日期时间型数据生成边界值和极端值，
 * 用于测试系统在边界条件下的行为。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class BoundaryValueGenerator extends BaseGenerator
    implements DataGenerator<Object, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(BoundaryValueGenerator.class);

  private final Random random;

  // 数值边界值
  private static final List<Integer> INTEGER_BOUNDARIES =
      Arrays.asList(
          Integer.MIN_VALUE,
          Integer.MIN_VALUE + 1,
          -1,
          0,
          1,
          Integer.MAX_VALUE - 1,
          Integer.MAX_VALUE);

  private static final List<Long> LONG_BOUNDARIES =
      Arrays.asList(
          Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, Long.MAX_VALUE - 1, Long.MAX_VALUE);

  private static final List<Double> DOUBLE_BOUNDARIES =
      Arrays.asList(
          -Double.MAX_VALUE,
          Double.NEGATIVE_INFINITY,
          -1.0,
          -0.0,
          0.0,
          1.0,
          Double.POSITIVE_INFINITY,
          Double.MAX_VALUE);

  // 字符串边界值
  private static final List<String> STRING_BOUNDARIES =
      Arrays.asList(
          "", // 空字符串
          " ", // 单个空格
          "\t", // Tab字符
          "\n", // 换行符
          "\r\n", // 回车换行
          "a", // 单个字符
          "中", // 单个中文字符
          "🚀", // Emoji字符
          "\u0000", // Null字符
          "\u200B", // 零宽空格
          "A".repeat(255), // 接近常见字段长度限制
          "A".repeat(65535) // 接近最大字符串长度
          );

  // 日期边界值
  private static final List<String> DATE_BOUNDARIES =
      Arrays.asList(
          "1900-01-01", // 历史边界
          "1970-01-01", // Unix纪元
          "2000-01-01", // 千年边界
          "2038-01-19", // 32位时间戳溢出
          "2100-12-31", // 未来边界
          "2999-12-31" // 远未来
          );

  public BoundaryValueGenerator() {
    this.random = new Random();
  }

  @Override
  public String getType() {
    return "BOUNDARY_VALUE";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public Object generate(FieldConfig config, DataForgeContext context) {
    try {
      String dataType = config.getParam("dataType", String.class, "ALL");
      String valueType = config.getParam("valueType", String.class, "ANY");
      double frequency = Double.parseDouble(config.getParam("frequency", String.class, "0.1"));
      String format = config.getParam("format", String.class, "STRING");

      // 根据频率决定是否生成边界值
      if (random.nextDouble() > frequency) {
        return generateNormalValue(dataType);
      }

      return generateBoundaryValue(dataType, valueType, format);

    } catch (Exception e) {
      logger.warn("Error generating boundary value: {}", e.getMessage());
      return 0; // 返回安全的默认值
    }
  }

  /** 生成边界值 */
  private Object generateBoundaryValue(String dataType, String valueType, String format) {
    switch (dataType.toUpperCase()) {
      case "INTEGER":
        return generateIntegerBoundary(valueType);
      case "LONG":
        return generateLongBoundary(valueType);
      case "DOUBLE":
      case "FLOAT":
        return generateDoubleBoundary(valueType, format);
      case "STRING":
        return generateStringBoundary(valueType);
      case "DATE":
        return generateDateBoundary(valueType, format);
      case "DATETIME":
        return generateDateTimeBoundary(valueType, format);
      case "BIGINTEGER":
        return generateBigIntegerBoundary(valueType);
      case "BIGDECIMAL":
        return generateBigDecimalBoundary(valueType);
      case "ALL":
      default:
        return generateRandomBoundary();
    }
  }

  /** 生成整数边界值 */
  private Object generateIntegerBoundary(String valueType) {
    List<Integer> boundaries = INTEGER_BOUNDARIES;

    switch (valueType.toUpperCase()) {
      case "MIN":
        return Integer.MIN_VALUE;
      case "MAX":
        return Integer.MAX_VALUE;
      case "ZERO":
        return 0;
      case "NEGATIVE":
        return boundaries.stream()
            .filter(i -> i < 0)
            .skip(random.nextInt(3))
            .findFirst()
            .orElse(-1);
      case "POSITIVE":
        return boundaries.stream().filter(i -> i > 0).skip(random.nextInt(2)).findFirst().orElse(1);
      case "ANY":
      default:
        return boundaries.get(random.nextInt(boundaries.size()));
    }
  }

  /** 生成长整数边界值 */
  private Object generateLongBoundary(String valueType) {
    List<Long> boundaries = LONG_BOUNDARIES;

    switch (valueType.toUpperCase()) {
      case "MIN":
        return Long.MIN_VALUE;
      case "MAX":
        return Long.MAX_VALUE;
      case "ZERO":
        return 0L;
      case "NEGATIVE":
        return boundaries.stream()
            .filter(l -> l < 0)
            .skip(random.nextInt(3))
            .findFirst()
            .orElse(-1L);
      case "POSITIVE":
        return boundaries.stream()
            .filter(l -> l > 0)
            .skip(random.nextInt(2))
            .findFirst()
            .orElse(1L);
      case "ANY":
      default:
        return boundaries.get(random.nextInt(boundaries.size()));
    }
  }

  /** 生成浮点数边界值 */
  private Object generateDoubleBoundary(String valueType, String format) {
    List<Double> boundaries = DOUBLE_BOUNDARIES;
    Double value;

    switch (valueType.toUpperCase()) {
      case "MIN":
        value = -Double.MAX_VALUE;
        break;
      case "MAX":
        value = Double.MAX_VALUE;
        break;
      case "ZERO":
        value = 0.0;
        break;
      case "INFINITY":
        value = random.nextBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        break;
      case "NAN":
        value = Double.NaN;
        break;
      case "NEGATIVE":
        value =
            boundaries.stream()
                .filter(d -> d < 0 && !Double.isInfinite(d) && !Double.isNaN(d))
                .skip(random.nextInt(3))
                .findFirst()
                .orElse(-1.0);
        break;
      case "POSITIVE":
        value =
            boundaries.stream()
                .filter(d -> d > 0 && !Double.isInfinite(d) && !Double.isNaN(d))
                .skip(random.nextInt(2))
                .findFirst()
                .orElse(1.0);
        break;
      case "ANY":
      default:
        value = boundaries.get(random.nextInt(boundaries.size()));
        break;
    }

    return formatDoubleValue(value, format);
  }

  /** 格式化浮点数值 */
  private Object formatDoubleValue(Double value, String format) {
    switch (format.toUpperCase()) {
      case "FLOAT":
        return value.floatValue();
      case "STRING":
        return value.toString();
      case "BIGDECIMAL":
        if (Double.isInfinite(value) || Double.isNaN(value)) {
          return value.toString();
        }
        return BigDecimal.valueOf(value);
      case "DOUBLE":
      default:
        return value;
    }
  }

  /** 生成字符串边界值 */
  private Object generateStringBoundary(String valueType) {
    List<String> boundaries = STRING_BOUNDARIES;

    switch (valueType.toUpperCase()) {
      case "EMPTY":
        return "";
      case "NULL":
        return null;
      case "WHITESPACE":
        return Arrays.asList(" ", "\t", "\n", "\r\n").get(random.nextInt(4));
      case "SINGLE_CHAR":
        return Arrays.asList("a", "中", "🚀").get(random.nextInt(3));
      case "LONG":
        return "A".repeat(65535);
      case "CONTROL":
        return Arrays.asList("\u0000", "\u0001", "\u007F", "\u200B").get(random.nextInt(4));
      case "ANY":
      default:
        return boundaries.get(random.nextInt(boundaries.size()));
    }
  }

  /** 生成日期边界值 */
  private Object generateDateBoundary(String valueType, String format) {
    List<String> boundaries = DATE_BOUNDARIES;
    String dateStr;

    switch (valueType.toUpperCase()) {
      case "MIN":
        dateStr = "1900-01-01";
        break;
      case "MAX":
        dateStr = "2999-12-31";
        break;
      case "EPOCH":
        dateStr = "1970-01-01";
        break;
      case "Y2K":
        dateStr = "2000-01-01";
        break;
      case "Y2038":
        dateStr = "2038-01-19";
        break;
      case "ANY":
      default:
        dateStr = boundaries.get(random.nextInt(boundaries.size()));
        break;
    }

    return formatDateValue(dateStr, format);
  }

  /** 生成日期时间边界值 */
  private Object generateDateTimeBoundary(String valueType, String format) {
    String dateStr = (String) generateDateBoundary(valueType, "STRING");
    String timeStr = generateBoundaryTime();
    String dateTimeStr = dateStr + "T" + timeStr;

    return formatDateTimeValue(dateTimeStr, format);
  }

  /** 生成边界时间 */
  private String generateBoundaryTime() {
    List<String> timeBoundaries =
        Arrays.asList("00:00:00", "00:00:01", "12:00:00", "23:59:58", "23:59:59");
    return timeBoundaries.get(random.nextInt(timeBoundaries.size()));
  }

  /** 格式化日期值 */
  private Object formatDateValue(String dateStr, String format) {
    LocalDate date = LocalDate.parse(dateStr);

    switch (format.toUpperCase()) {
      case "LOCALDATE":
        return date;
      case "TIMESTAMP":
        return date.atStartOfDay().toString();
      case "EPOCH":
        return date.toEpochDay();
      case "STRING":
      default:
        return dateStr;
    }
  }

  /** 格式化日期时间值 */
  private Object formatDateTimeValue(String dateTimeStr, String format) {
    LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr);

    switch (format.toUpperCase()) {
      case "LOCALDATETIME":
        return dateTime;
      case "TIMESTAMP":
        return dateTime.toString();
      case "ISO":
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      case "STRING":
      default:
        return dateTimeStr;
    }
  }

  /** 生成BigInteger边界值 */
  private Object generateBigIntegerBoundary(String valueType) {
    switch (valueType.toUpperCase()) {
      case "MIN":
        return new BigInteger("-" + "9".repeat(100));
      case "MAX":
        return new BigInteger("9".repeat(100));
      case "ZERO":
        return BigInteger.ZERO;
      case "ANY":
      default:
        List<BigInteger> boundaries =
            Arrays.asList(
                BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE),
                BigInteger.valueOf(Integer.MIN_VALUE),
                BigInteger.valueOf(-1),
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.valueOf(Integer.MAX_VALUE),
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
        return boundaries.get(random.nextInt(boundaries.size()));
    }
  }

  /** 生成BigDecimal边界值 */
  private Object generateBigDecimalBoundary(String valueType) {
    switch (valueType.toUpperCase()) {
      case "MIN":
        return new BigDecimal("-" + "9".repeat(100) + "." + "9".repeat(100));
      case "MAX":
        return new BigDecimal("9".repeat(100) + "." + "9".repeat(100));
      case "ZERO":
        return BigDecimal.ZERO;
      case "ANY":
      default:
        List<BigDecimal> boundaries =
            Arrays.asList(
                BigDecimal.valueOf(Double.MIN_VALUE),
                BigDecimal.valueOf(-1.0),
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.valueOf(Double.MAX_VALUE));
        return boundaries.get(random.nextInt(boundaries.size()));
    }
  }

  /** 生成随机边界值 */
  private Object generateRandomBoundary() {
    String[] dataTypes = {"INTEGER", "LONG", "DOUBLE", "STRING", "DATE"};
    String randomType = dataTypes[random.nextInt(dataTypes.length)];
    return generateBoundaryValue(randomType, "ANY", "STRING");
  }

  /** 生成正常值（非边界值） */
  private Object generateNormalValue(String dataType) {
    switch (dataType.toUpperCase()) {
      case "INTEGER":
        return random.nextInt(1000);
      case "LONG":
        return random.nextLong();
      case "DOUBLE":
        return random.nextDouble() * 1000;
      case "STRING":
        return "normal_value_" + random.nextInt(1000);
      case "DATE":
        return LocalDate.now().plusDays(random.nextInt(365) - 182).toString();
      default:
        return "normal_value";
    }
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String dataType = config.getParam("dataType", String.class, "ALL");
    String valueType = config.getParam("valueType", String.class, "ANY");
    double frequency = Double.parseDouble(config.getParam("frequency", String.class, "0.1"));

    // 验证数据类型
    String[] validDataTypes = {
      "INTEGER", "LONG", "DOUBLE", "FLOAT", "STRING",
      "DATE", "DATETIME", "BIGINTEGER", "BIGDECIMAL", "ALL"
    };
    if (!Arrays.asList(validDataTypes).contains(dataType.toUpperCase())) {
      return false;
    }

    // 验证值类型
    String[] validValueTypes = {
      "MIN", "MAX", "ZERO", "NEGATIVE", "POSITIVE",
      "INFINITY", "NAN", "EMPTY", "NULL", "WHITESPACE",
      "SINGLE_CHAR", "LONG", "CONTROL", "EPOCH", "Y2K",
      "Y2038", "ANY"
    };
    if (!Arrays.asList(validValueTypes).contains(valueType.toUpperCase())) {
      return false;
    }

    // 验证频率
    return frequency >= 0.0 && frequency <= 1.0;
  }

  @Override
  public String getDescription() {
    return "生成边界值和极端值，包括数值型（最大值、最小值、零）、字符串型（空字符串、超长字符串）、" + "日期时间型（历史边界、未来边界）等，用于测试系统在边界条件下的行为";
  }
}
