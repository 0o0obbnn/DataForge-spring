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
 * 百分比生成器
 *
 * <p>支持生成各种格式的百分比数据，包括成功率、完成度、 增长率、折扣率等，用于统计分析、业务报表、UI显示等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>min: 最小百分比值 默认: 0.0
 *   <li>max: 最大百分比值 默认: 100.0
 *   <li>precision: 小数位数 默认: 1
 *   <li>format: 输出格式 (PERCENT|DECIMAL|FRACTION|RATIO|CUSTOM) 默认: PERCENT
 *   <li>symbol: 百分号符号 默认: %
 *   <li>locale: 本地化设置 默认: en_US
 *   <li>distribution: 分布类型 (UNIFORM|NORMAL|BETA|EXPONENTIAL) 默认: UNIFORM
 *   <li>common_values: 是否生成常见值 默认: false
 *   <li>allow_over_100: 是否允许超过100% 默认: false
 *   <li>negative_allowed: 是否允许负值 默认: false
 *   <li>round_to_nearest: 舍入到最近的值 (1|5|10|25) 默认: 不舍入
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class PercentageGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(PercentageGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 输出格式枚举
  public enum OutputFormat {
    PERCENT("百分比格式"),
    DECIMAL("小数格式"),
    FRACTION("分数格式"),
    RATIO("比率格式"),
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
    BETA("贝塔分布"),
    EXPONENTIAL("指数分布");

    private final String description;

    DistributionType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 常见百分比值
  private static final double[] COMMON_PERCENTAGES = {
    0.0, 1.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 33.33, 40.0, 50.0, 60.0, 66.67, 70.0, 75.0, 80.0,
    85.0, 90.0, 95.0, 99.0, 100.0
  };

  // 常见折扣百分比
  private static final double[] DISCOUNT_PERCENTAGES = {
    5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0
  };

  // 常见增长率百分比
  private static final double[] GROWTH_PERCENTAGES = {
    -50.0, -30.0, -20.0, -10.0, -5.0, 0.0, 2.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 50.0, 75.0,
    100.0, 150.0, 200.0
  };

  @Override
  public String getType() {
    return "percentage";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取分布类型
      String distributionStr = getStringParam(config, "distribution", "UNIFORM");
      DistributionType distribution = parseDistributionType(distributionStr);

      // 获取输出格式
      String formatStr = getStringParam(config, "format", "PERCENT");
      OutputFormat format = parseOutputFormat(formatStr);

      // 生成百分比值
      BigDecimal percentage = generatePercentage(distribution, config);

      // 格式化输出
      String result = formatPercentage(percentage, format, config);

      // 存储到上下文
      context.put("percentage_value", percentage);
      context.put(
          "percentage_decimal",
          percentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
      context.put("percentage_format", format.name());

      return result;

    } catch (Exception e) {
      logger.error("Failed to generate percentage", e);
      return "50.0%";
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
      logger.warn("Invalid output format: {}, using PERCENT as default", formatStr);
      return OutputFormat.PERCENT;
    }
  }

  /** 生成百分比值 */
  private BigDecimal generatePercentage(DistributionType distribution, FieldConfig config) {
    // 检查是否使用常见值
    if (getBooleanParam(config, "common_values", false)) {
      return generateCommonPercentage(config);
    }

    // 获取范围参数
    double min = getDoubleParam(config, "min", 0.0);
    double max = getDoubleParam(config, "max", 100.0);

    // 检查约束
    boolean allowOver100 = getBooleanParam(config, "allow_over_100", false);
    boolean negativeAllowed = getBooleanParam(config, "negative_allowed", false);

    if (!allowOver100 && max > 100.0) {
      max = 100.0;
    }

    if (!negativeAllowed && min < 0.0) {
      min = 0.0;
    }

    // 根据分布类型生成值
    double value;
    switch (distribution) {
      case UNIFORM:
        value = generateUniform(min, max);
        break;
      case NORMAL:
        value = generateNormal(config, min, max);
        break;
      case BETA:
        value = generateBeta(config, min, max);
        break;
      case EXPONENTIAL:
        value = generateExponential(config, min, max);
        break;
      default:
        value = generateUniform(min, max);
        break;
    }

    // 应用舍入
    value = applyRounding(value, config);

    // 应用精度
    int precision = getIntParam(config, "precision", 1);
    return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);
  }

  /** 生成常见百分比值 */
  private BigDecimal generateCommonPercentage(FieldConfig config) {
    String context = getStringParam(config, "context", "GENERAL");
    double[] values;

    switch (context.toUpperCase()) {
      case "DISCOUNT":
        values = DISCOUNT_PERCENTAGES;
        break;
      case "GROWTH":
        values = GROWTH_PERCENTAGES;
        break;
      case "GENERAL":
      default:
        values = COMMON_PERCENTAGES;
        break;
    }

    double value = values[random.nextInt(values.length)];
    int precision = getIntParam(config, "precision", 1);
    return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);
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
    int attempts = 0;
    do {
      value = random.nextGaussian() * stddev + mean;
      attempts++;
    } while ((value < min || value > max) && attempts < 100);

    // 如果超出范围，截断到边界
    return Math.max(min, Math.min(max, value));
  }

  /** 生成贝塔分布随机数 */
  private double generateBeta(FieldConfig config, double min, double max) {
    double alpha = getDoubleParam(config, "alpha", 2.0);
    double beta = getDoubleParam(config, "beta", 2.0);

    // 使用简化的贝塔分布生成算法
    double x = generateGamma(alpha);
    double y = generateGamma(beta);
    double betaValue = x / (x + y);

    // 缩放到指定范围
    return min + betaValue * (max - min);
  }

  /** 生成伽马分布随机数（用于贝塔分布） */
  private double generateGamma(double shape) {
    if (shape < 1.0) {
      return generateGamma(shape + 1.0) * Math.pow(random.nextDouble(), 1.0 / shape);
    }

    double d = shape - 1.0 / 3.0;
    double c = 1.0 / Math.sqrt(9.0 * d);

    while (true) {
      double x = random.nextGaussian();
      double v = 1.0 + c * x;

      if (v > 0) {
        v = v * v * v;
        double u = random.nextDouble();

        if (u < 1.0 - 0.0331 * x * x * x * x) {
          return d * v;
        }

        if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v))) {
          return d * v;
        }
      }
    }
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

  /** 应用舍入规则 */
  private double applyRounding(double value, FieldConfig config) {
    String roundToStr = getStringParam(config, "round_to_nearest", null);
    if (roundToStr == null) {
      return value;
    }

    try {
      double roundTo = Double.parseDouble(roundToStr);
      return Math.round(value / roundTo) * roundTo;
    } catch (NumberFormatException e) {
      logger.warn("Invalid round_to_nearest value: {}", roundToStr);
      return value;
    }
  }

  /** 格式化百分比 */
  private String formatPercentage(BigDecimal percentage, OutputFormat format, FieldConfig config) {
    switch (format) {
      case PERCENT:
        return formatAsPercent(percentage, config);
      case DECIMAL:
        return formatAsDecimal(percentage, config);
      case FRACTION:
        return formatAsFraction(percentage, config);
      case RATIO:
        return formatAsRatio(percentage, config);
      case CUSTOM:
        return formatAsCustom(percentage, config);
      default:
        return formatAsPercent(percentage, config);
    }
  }

  /** 格式化为百分比格式 */
  private String formatAsPercent(BigDecimal percentage, FieldConfig config) {
    String symbol = getStringParam(config, "symbol", "%");
    String localeStr = getStringParam(config, "locale", "en_US");

    try {
      Locale locale = parseLocale(localeStr);
      NumberFormat percentFormat = NumberFormat.getPercentInstance(locale);

      int precision = getIntParam(config, "precision", 1);
      percentFormat.setMaximumFractionDigits(precision);
      percentFormat.setMinimumFractionDigits(precision);

      // 将百分比转换为小数（除以100）
      double decimalValue = percentage.doubleValue() / 100.0;
      return percentFormat.format(decimalValue);

    } catch (Exception e) {
      // 如果本地化失败，使用简单格式
      return percentage.toPlainString() + symbol;
    }
  }

  /** 格式化为小数格式 */
  private String formatAsDecimal(BigDecimal percentage, FieldConfig config) {
    // 将百分比转换为小数
    BigDecimal decimal = percentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

    int precision = getIntParam(config, "precision", 3);
    return decimal.setScale(precision, RoundingMode.HALF_UP).toPlainString();
  }

  /** 格式化为分数格式 */
  private String formatAsFraction(BigDecimal percentage, FieldConfig config) {
    double value = percentage.doubleValue();

    // 将百分比转换为分数
    int denominator = 100;
    int numerator = (int) Math.round(value);

    // 简化分数
    int gcd = gcd(numerator, denominator);
    numerator /= gcd;
    denominator /= gcd;

    return numerator + "/" + denominator;
  }

  /** 格式化为比率格式 */
  private String formatAsRatio(BigDecimal percentage, FieldConfig config) {
    double value = percentage.doubleValue();

    if (value == 0) {
      return "0:100";
    } else if (value == 100) {
      return "100:0";
    } else {
      int part1 = (int) Math.round(value);
      int part2 = 100 - part1;
      return part1 + ":" + part2;
    }
  }

  /** 格式化为自定义格式 */
  private String formatAsCustom(BigDecimal percentage, FieldConfig config) {
    String pattern = getStringParam(config, "pattern", "#0.0%");

    try {
      DecimalFormat customFormat = new DecimalFormat(pattern);

      // 根据模式判断是否需要转换为小数
      if (pattern.contains("%")) {
        double decimalValue = percentage.doubleValue() / 100.0;
        return customFormat.format(decimalValue);
      } else {
        return customFormat.format(percentage.doubleValue());
      }
    } catch (Exception e) {
      logger.warn("Invalid custom pattern: {}, using default format", pattern);
      return percentage.toPlainString() + "%";
    }
  }

  /** 计算最大公约数 */
  private int gcd(int a, int b) {
    while (b != 0) {
      int temp = b;
      b = a % b;
      a = temp;
    }
    return Math.abs(a);
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

  /** 生成特定场景的百分比 */
  public String generateForContext(String context, FieldConfig config) {
    // 根据上下文调整参数
    switch (context.toUpperCase()) {
      case "SUCCESS_RATE":
        config.getParams().put("min", "70.0");
        config.getParams().put("max", "99.9");
        config.getParams().put("distribution", "BETA");
        config.getParams().put("alpha", "8.0");
        config.getParams().put("beta", "2.0");
        break;
      case "COMPLETION":
        config.getParams().put("min", "0.0");
        config.getParams().put("max", "100.0");
        config.getParams().put("common_values", "true");
        break;
      case "DISCOUNT":
        config.getParams().put("context", "DISCOUNT");
        config.getParams().put("common_values", "true");
        break;
      case "GROWTH":
        config.getParams().put("context", "GROWTH");
        config.getParams().put("negative_allowed", "true");
        config.getParams().put("allow_over_100", "true");
        break;
      case "PROBABILITY":
        config.getParams().put("min", "0.0");
        config.getParams().put("max", "100.0");
        config.getParams().put("precision", "2");
        break;
    }

    return generate(config, new DataForgeContext());
  }

  /** 验证百分比值 */
  public static boolean isValidPercentage(String value) {
    try {
      String cleanValue = value.replace("%", "").trim();
      double percentage = Double.parseDouble(cleanValue);
      return percentage >= 0.0 && percentage <= 100.0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /** 转换百分比格式 */
  public static String convertFormat(
      String percentage, OutputFormat fromFormat, OutputFormat toFormat) {
    try {
      // 解析原始值
      double value;
      switch (fromFormat) {
        case PERCENT:
          value = Double.parseDouble(percentage.replace("%", ""));
          break;
        case DECIMAL:
          value = Double.parseDouble(percentage) * 100;
          break;
        default:
          return percentage;
      }

      // 转换为目标格式
      BigDecimal bd = BigDecimal.valueOf(value);
      FieldConfig config = new com.dataforge.config.SimpleFieldConfig();
      PercentageGenerator generator = new PercentageGenerator();
      return generator.formatPercentage(bd, toFormat, config);

    } catch (Exception e) {
      return percentage;
    }
  }
}
