package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 小数生成器
 *
 * <p>支持生成各种精度和格式的小数，包括浮点数、双精度数、 高精度小数等，用于财务计算、科学计算、精度测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 数值类型 (FLOAT|DOUBLE|BIGDECIMAL) 默认: DOUBLE
 *   <li>min: 最小值 默认: 0.0
 *   <li>max: 最大值 默认: 100.0
 *   <li>precision: 总精度位数 默认: 10
 *   <li>scale: 小数位数 默认: 2
 *   <li>rounding: 舍入模式 (UP|DOWN|CEILING|FLOOR|HALF_UP|HALF_DOWN|HALF_EVEN) 默认: HALF_UP
 *   <li>format: 输出格式 (PLAIN|SCIENTIFIC|CURRENCY|PERCENTAGE|CUSTOM) 默认: PLAIN
 *   <li>locale: 本地化设置 默认: en_US
 *   <li>currency_code: 货币代码（仅对CURRENCY格式有效）默认: USD
 *   <li>pattern: 自定义格式模式（仅对CUSTOM格式有效）
 *   <li>positive_only: 是否只生成正数 默认: false
 *   <li>exclude_zero: 是否排除零 默认: false
 *   <li>distribution: 分布类型 (UNIFORM|NORMAL|EXPONENTIAL) 默认: UNIFORM
 *   <li>mean: 正态分布的均值 默认: (min+max)/2
 *   <li>stddev: 正态分布的标准差 默认: (max-min)/6
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class DecimalGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(DecimalGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 数值类型枚举
  public enum DecimalType {
    FLOAT("32位浮点数"),
    DOUBLE("64位双精度数"),
    BIGDECIMAL("高精度小数");

    private final String description;

    DecimalType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 输出格式枚举
  public enum OutputFormat {
    PLAIN("普通格式"),
    SCIENTIFIC("科学计数法"),
    CURRENCY("货币格式"),
    PERCENTAGE("百分比格式"),
    CUSTOM("自定义格式");

    private final String description;

    OutputFormat(String description) {
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
    EXPONENTIAL("指数分布");

    private final String description;

    DistributionType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @Override
  public String getType() {
    return "decimal";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取数值类型
      String typeStr = getStringParam(config, "type", "DOUBLE");
      DecimalType decimalType = parseDecimalType(typeStr);

      // 获取分布类型
      String distributionStr = getStringParam(config, "distribution", "UNIFORM");
      DistributionType distribution = parseDistributionType(distributionStr);

      // 获取输出格式
      String formatStr = getStringParam(config, "format", "PLAIN");
      OutputFormat format = parseOutputFormat(formatStr);

      // 生成小数
      BigDecimal decimal = generateDecimal(distribution, config);

      // 转换为指定类型
      Number number = convertToDecimalType(decimal, decimalType);

      // 格式化输出
      return formatDecimal(number, format, config);

    } catch (Exception e) {
      logger.error("Failed to generate decimal number", e);
      // 返回一个默认的小数作为fallback
      return String.format("%.2f", random.nextDouble() * 100);
    }
  }

  /** 解析小数类型 */
  private DecimalType parseDecimalType(String typeStr) {
    try {
      return DecimalType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid decimal type: {}, using DOUBLE as default", typeStr);
      return DecimalType.DOUBLE;
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
      logger.warn("Invalid output format: {}, using PLAIN as default", formatStr);
      return OutputFormat.PLAIN;
    }
  }

  /** 生成小数 */
  private BigDecimal generateDecimal(DistributionType distribution, FieldConfig config) {
    // 获取范围参数
    double min = getDoubleParam(config, "min", 0.0);
    double max = getDoubleParam(config, "max", 100.0);

    // 获取其他参数
    boolean positiveOnly = getBooleanParam(config, "positive_only", false);
    boolean excludeZero = getBooleanParam(config, "exclude_zero", false);

    // 调整范围
    if (positiveOnly && min <= 0) {
      min = 0.01;
    }

    if (excludeZero && min <= 0 && max >= 0) {
      if (min == 0) min = 0.01;
      if (max == 0) max = 0.01;
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
      default:
        value = generateUniform(min, max);
        break;
    }

    // 应用精度和舍入
    int scale = getIntParam(config, "scale", 2);
    String roundingStr = getStringParam(config, "rounding", "HALF_UP");
    RoundingMode rounding = parseRoundingMode(roundingStr);

    return BigDecimal.valueOf(value).setScale(scale, rounding);
  }

  /** 生成均匀分布随机数 */
  private double generateUniform(double min, double max) {
    if (min >= max) {
      return min;
    }
    return min + random.nextDouble() * (max - min);
  }

  /** 生成正态分布随机数 */
  private double generateNormal(FieldConfig config, double min, double max) {
    double mean = getDoubleParam(config, "mean", (min + max) / 2.0);
    double stddev = getDoubleParam(config, "stddev", (max - min) / 6.0);

    double value;
    do {
      value = random.nextGaussian() * stddev + mean;
    } while (value < min || value > max);

    return value;
  }

  /** 生成指数分布随机数 */
  private double generateExponential(FieldConfig config, double min, double max) {
    double lambda = getDoubleParam(config, "lambda", 1.0);

    double value;
    do {
      value = -Math.log(1 - random.nextDouble()) / lambda + min;
    } while (value > max);

    return value;
  }

  /** 解析舍入模式 */
  private RoundingMode parseRoundingMode(String roundingStr) {
    try {
      return RoundingMode.valueOf(roundingStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid rounding mode: {}, using HALF_UP as default", roundingStr);
      return RoundingMode.HALF_UP;
    }
  }

  /** 转换为指定的小数类型 */
  private Number convertToDecimalType(BigDecimal decimal, DecimalType decimalType) {
    switch (decimalType) {
      case FLOAT:
        return decimal.floatValue();
      case DOUBLE:
        return decimal.doubleValue();
      case BIGDECIMAL:
        return decimal;
      default:
        return decimal.doubleValue();
    }
  }

  /** 格式化小数 */
  private String formatDecimal(Number number, OutputFormat format, FieldConfig config) {
    switch (format) {
      case PLAIN:
        return formatAsPlain(number, config);
      case SCIENTIFIC:
        return formatAsScientific(number);
      case CURRENCY:
        return formatAsCurrency(number, config);
      case PERCENTAGE:
        return formatAsPercentage(number, config);
      case CUSTOM:
        return formatAsCustom(number, config);
      default:
        return number.toString();
    }
  }

  /** 格式化为普通格式 */
  private String formatAsPlain(Number number, FieldConfig config) {
    int scale = getIntParam(config, "scale", 2);

    if (number instanceof BigDecimal) {
      return ((BigDecimal) number).toPlainString();
    } else {
      DecimalFormat df = new DecimalFormat();
      df.setMaximumFractionDigits(scale);
      df.setMinimumFractionDigits(scale);
      df.setGroupingUsed(false);
      return df.format(number);
    }
  }

  /** 格式化为科学计数法 */
  private String formatAsScientific(Number number) {
    return String.format("%.3E", number.doubleValue());
  }

  /** 格式化为货币格式 */
  private String formatAsCurrency(Number number, FieldConfig config) {
    String localeStr = getStringParam(config, "locale", "en_US");
    String currencyCode = getStringParam(config, "currency_code", "USD");

    Locale locale = parseLocale(localeStr);
    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);

    try {
      java.util.Currency currency = java.util.Currency.getInstance(currencyCode);
      currencyFormat.setCurrency(currency);
    } catch (Exception e) {
      logger.warn("Invalid currency code: {}, using default", currencyCode);
    }

    return currencyFormat.format(number);
  }

  /** 格式化为百分比格式 */
  private String formatAsPercentage(Number number, FieldConfig config) {
    String localeStr = getStringParam(config, "locale", "en_US");
    Locale locale = parseLocale(localeStr);

    NumberFormat percentFormat = NumberFormat.getPercentInstance(locale);
    int scale = getIntParam(config, "scale", 2);
    percentFormat.setMaximumFractionDigits(scale);
    percentFormat.setMinimumFractionDigits(scale);

    // 将数值转换为百分比（除以100）
    double percentValue = number.doubleValue() / 100.0;
    return percentFormat.format(percentValue);
  }

  /** 格式化为自定义格式 */
  private String formatAsCustom(Number number, FieldConfig config) {
    String pattern = getStringParam(config, "pattern", "#,##0.00");

    try {
      DecimalFormat customFormat = new DecimalFormat(pattern);
      return customFormat.format(number);
    } catch (Exception e) {
      logger.warn("Invalid custom pattern: {}, using default format", pattern);
      return number.toString();
    }
  }

  /** 解析本地化设置 */
  private Locale parseLocale(String localeStr) {
    try {
      if (localeStr.contains("_")) {
        String[] parts = localeStr.split("_");
        if (parts.length >= 2) {
          return new Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build();
        }
      }
      return Locale.forLanguageTag(localeStr.replace("_", "-"));
    } catch (Exception e) {
      logger.warn("Invalid locale: {}, using English as default", localeStr);
      return Locale.ENGLISH;
    }
  }
}
