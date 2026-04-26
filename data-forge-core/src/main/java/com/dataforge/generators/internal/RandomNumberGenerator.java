package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 随机数生成器
 *
 * <p>支持生成各种类型的随机数，包括整数、长整数、大整数等， 用于数值测试、性能测试、统计分析等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 数值类型 (INT|LONG|BIGINT|BYTE|SHORT) 默认: INT
 *   <li>min: 最小值 默认: 0
 *   <li>max: 最大值 默认: 100
 *   <li>distribution: 分布类型 (UNIFORM|NORMAL|EXPONENTIAL|POISSON) 默认: UNIFORM
 *   <li>mean: 正态分布的均值（仅对NORMAL分布有效）默认: 50
 *   <li>stddev: 正态分布的标准差（仅对NORMAL分布有效）默认: 15
 *   <li>lambda: 指数分布/泊松分布的参数 默认: 1.0
 *   <li>positive_only: 是否只生成正数 默认: false
 *   <li>exclude_zero: 是否排除零 默认: false
 *   <li>format: 输出格式 (DECIMAL|HEX|OCTAL|BINARY|SCIENTIFIC) 默认: DECIMAL
 *   <li>precision: 精度位数（用于大数）默认: 10
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class RandomNumberGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(RandomNumberGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 数值类型枚举
  public enum NumberType {
    INT("32位整数"),
    LONG("64位长整数"),
    BIGINT("大整数"),
    BYTE("8位字节"),
    SHORT("16位短整数");

    private final String description;

    NumberType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 分布类型枚举
  public enum DistributionType {
    UNIFORM("均匀分布"),
    NORMAL("正态分布"),
    EXPONENTIAL("指数分布"),
    POISSON("泊松分布");

    private final String description;

    DistributionType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 输出格式枚举
  public enum OutputFormat {
    DECIMAL("十进制"),
    HEX("十六进制"),
    OCTAL("八进制"),
    BINARY("二进制"),
    SCIENTIFIC("科学计数法");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @Override
  public String getType() {
    return "random_number";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取数值类型
      String typeStr = getStringParam(config, "type", "INT");
      NumberType numberType = parseNumberType(typeStr);

      // 获取分布类型
      String distributionStr = getStringParam(config, "distribution", "UNIFORM");
      DistributionType distribution = parseDistributionType(distributionStr);

      // 获取输出格式
      String formatStr = getStringParam(config, "format", "DECIMAL");
      OutputFormat format = parseOutputFormat(formatStr);

      // 生成随机数
      Number number = generateRandomNumber(numberType, distribution, config);

      // 格式化输出
      return formatNumber(number, format, numberType);

    } catch (Exception e) {
      logger.error("Failed to generate random number", e);
      // 返回一个默认的随机数作为fallback
      return String.valueOf(random.nextInt(100));
    }
  }

  /** 解析数值类型 */
  private NumberType parseNumberType(String typeStr) {
    try {
      return NumberType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid number type: {}, using INT as default", typeStr);
      return NumberType.INT;
    }
  }

  /** 解析分布类型 */
  private DistributionType parseDistributionType(String distributionStr) {
    try {
      return DistributionType.valueOf(distributionStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid distribution type: {}, using UNIFORM as default", distributionStr);
      return DistributionType.UNIFORM;
    }
  }

  /** 解析输出格式 */
  private OutputFormat parseOutputFormat(String formatStr) {
    try {
      return OutputFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output format: {}, using DECIMAL as default", formatStr);
      return OutputFormat.DECIMAL;
    }
  }

  /** 生成随机数 */
  private Number generateRandomNumber(
      NumberType numberType, DistributionType distribution, FieldConfig config) {
    // 获取范围参数
    long min = getLongParam(config, "min", 0L);
    long max = getLongParam(config, "max", 100L);

    // 获取其他参数
    boolean positiveOnly = getBooleanParam(config, "positive_only", false);
    boolean excludeZero = getBooleanParam(config, "exclude_zero", false);

    // 调整范围
    if (positiveOnly && min <= 0) {
      min = 1;
    }

    if (excludeZero && min <= 0 && max >= 0) {
      if (min == 0) min = 1;
      if (max == 0) max = 1;
    }

    // 根据分布类型生成数值
    double value;
    switch (distribution) {
      case UNIFORM:
        value = generateUniform(min, max);
        break;
      case NORMAL:
        value = generateNormal(config, min, max);
        break;
      case EXPONENTIAL:
        value = generateExponential(config, min, max);
        break;
      case POISSON:
        value = generatePoisson(config, min, max);
        break;
      default:
        value = generateUniform(min, max);
        break;
    }

    // 转换为指定的数值类型
    return convertToNumberType(value, numberType, config);
  }

  /** 生成均匀分布随机数 */
  private double generateUniform(long min, long max) {
    if (min >= max) {
      return min;
    }
    return min + random.nextDouble() * (max - min);
  }

  /** 生成正态分布随机数 */
  private double generateNormal(FieldConfig config, long min, long max) {
    double mean = getDoubleParam(config, "mean", (min + max) / 2.0);
    double stddev = getDoubleParam(config, "stddev", (max - min) / 6.0);

    double value;
    do {
      value = random.nextGaussian() * stddev + mean;
    } while (value < min || value > max);

    return value;
  }

  /** 生成指数分布随机数 */
  private double generateExponential(FieldConfig config, long min, long max) {
    double lambda = getDoubleParam(config, "lambda", 1.0);

    double value;
    do {
      value = -Math.log(1 - random.nextDouble()) / lambda + min;
    } while (value > max);

    return value;
  }

  /** 生成泊松分布随机数 */
  private double generatePoisson(FieldConfig config, long min, long max) {
    double lambda = getDoubleParam(config, "lambda", (max - min) / 2.0);

    // 使用Knuth算法生成泊松分布
    double L = Math.exp(-lambda);
    double p = 1.0;
    int k = 0;

    do {
      k++;
      p *= random.nextDouble();
    } while (p > L);

    double value = k - 1 + min;
    return Math.min(value, max);
  }

  /** 转换为指定的数值类型 */
  private Number convertToNumberType(double value, NumberType numberType, FieldConfig config) {
    switch (numberType) {
      case BYTE:
        return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, Math.round(value)));
      case SHORT:
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(value)));
      case INT:
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(value)));
      case LONG:
        return Math.round(value);
      case BIGINT:
        int precision = getIntParam(config, "precision", 10);
        return generateBigInteger(value, precision);
      default:
        return Math.round(value);
    }
  }

  /** 生成大整数 */
  private BigInteger generateBigInteger(double baseValue, int precision) {
    // 基于baseValue生成大整数
    StringBuilder sb = new StringBuilder();

    // 添加符号
    if (baseValue < 0) {
      sb.append("-");
      baseValue = Math.abs(baseValue);
    }

    // 生成第一位（非零）
    sb.append(1 + random.nextInt(9));

    // 生成剩余位数
    for (int i = 1; i < precision; i++) {
      sb.append(random.nextInt(10));
    }

    return new BigInteger(sb.toString());
  }

  /** 格式化数值 */
  private String formatNumber(Number number, OutputFormat format, NumberType numberType) {
    switch (format) {
      case DECIMAL:
        return number.toString();
      case HEX:
        return formatAsHex(number);
      case OCTAL:
        return formatAsOctal(number);
      case BINARY:
        return formatAsBinary(number);
      case SCIENTIFIC:
        return formatAsScientific(number);
      default:
        return number.toString();
    }
  }

  /** 格式化为十六进制 */
  private String formatAsHex(Number number) {
    if (number instanceof BigInteger) {
      return "0x" + ((BigInteger) number).toString(16).toUpperCase();
    } else {
      return "0x" + Long.toHexString(number.longValue()).toUpperCase();
    }
  }

  /** 格式化为八进制 */
  private String formatAsOctal(Number number) {
    if (number instanceof BigInteger) {
      return "0" + ((BigInteger) number).toString(8);
    } else {
      return "0" + Long.toOctalString(number.longValue());
    }
  }

  /** 格式化为二进制 */
  private String formatAsBinary(Number number) {
    if (number instanceof BigInteger) {
      return "0b" + ((BigInteger) number).toString(2);
    } else {
      return "0b" + Long.toBinaryString(number.longValue());
    }
  }

  /** 格式化为科学计数法 */
  private String formatAsScientific(Number number) {
    double value = number.doubleValue();
    return String.format("%.3E", value);
  }
}
