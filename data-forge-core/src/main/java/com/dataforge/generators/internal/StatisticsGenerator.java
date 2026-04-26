package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统计数值生成器
 *
 * <p>支持生成各种统计指标和数值，包括均值、方差、标准差、 分位数、相关系数等，用于数据分析、报表生成、统计测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 统计类型
 *       (MEAN|MEDIAN|MODE|VARIANCE|STDDEV|RANGE|PERCENTILE|CORRELATION|ZSCORE|CONFIDENCE_INTERVAL)
 *       默认: MEAN
 *   <li>data_size: 数据集大小 默认: 100
 *   <li>data_min: 数据最小值 默认: 0.0
 *   <li>data_max: 数据最大值 默认: 100.0
 *   <li>distribution: 数据分布 (UNIFORM|NORMAL|EXPONENTIAL|POISSON) 默认: NORMAL
 *   <li>mean: 正态分布均值 默认: 50.0
 *   <li>stddev: 正态分布标准差 默认: 15.0
 *   <li>percentile: 分位数值 (0-100) 默认: 50
 *   <li>confidence_level: 置信水平 (0-1) 默认: 0.95
 *   <li>precision: 结果精度 默认: 4
 *   <li>format: 输出格式 (NUMBER|FORMATTED|JSON|VERBOSE) 默认: NUMBER
 *   <li>include_raw_data: 是否包含原始数据 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class StatisticsGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(StatisticsGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 统计类型枚举
  public enum StatisticType {
    MEAN("算术平均数"),
    MEDIAN("中位数"),
    MODE("众数"),
    VARIANCE("方差"),
    STDDEV("标准差"),
    RANGE("极差"),
    PERCENTILE("分位数"),
    CORRELATION("相关系数"),
    ZSCORE("Z分数"),
    CONFIDENCE_INTERVAL("置信区间");

    private final String description;

    StatisticType(String description) {
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
    NUMBER("纯数字"),
    FORMATTED("格式化"),
    JSON("JSON格式"),
    VERBOSE("详细格式");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 统计结果类
  public static class StatisticResult {
    private final StatisticType type;
    private final double value;
    private final List<Double> rawData;
    private final Map<String, Object> metadata;

    public StatisticResult(
        StatisticType type, double value, List<Double> rawData, Map<String, Object> metadata) {
      this.type = type;
      this.value = value;
      this.rawData = rawData != null ? new ArrayList<>(rawData) : null;
      this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    // Getters
    public StatisticType getType() {
      return type;
    }

    public double getValue() {
      return value;
    }

    public List<Double> getRawData() {
      return rawData;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }
  }

  @Override
  public String getType() {
    return "statistics";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取统计类型
      String typeStr = getStringParam(config, "type", "MEAN");
      StatisticType statisticType = parseStatisticType(typeStr);

      // 获取输出格式
      String formatStr = getStringParam(config, "format", "NUMBER");
      OutputFormat format = parseOutputFormat(formatStr);

      // 生成数据集
      List<Double> dataset = generateDataset(config);

      // 计算统计值
      StatisticResult result = calculateStatistic(statisticType, dataset, config);

      // 格式化输出
      String output = formatResult(result, format, config);

      // 存储到上下文
      context.put("statistic_type", statisticType.name());
      context.put("statistic_value", result.getValue());
      context.put("dataset_size", dataset.size());
      context.put("dataset_mean", calculateMean(dataset));
      context.put("dataset_stddev", calculateStandardDeviation(dataset));

      return output;

    } catch (Exception e) {
      logger.error("Failed to generate statistics", e);
      return "50.0";
    }
  }

  /** 解析统计类型 */
  private StatisticType parseStatisticType(String typeStr) {
    try {
      return StatisticType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid statistic type: {}, using MEAN as default", typeStr);
      return StatisticType.MEAN;
    }
  }

  /** 解析输出格式 */
  private OutputFormat parseOutputFormat(String formatStr) {
    try {
      return OutputFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output format: {}, using NUMBER as default", formatStr);
      return OutputFormat.NUMBER;
    }
  }

  /** 生成数据集 */
  private List<Double> generateDataset(FieldConfig config) {
    int size = getIntParam(config, "data_size", 100);
    double min = getDoubleParam(config, "data_min", 0.0);
    double max = getDoubleParam(config, "data_max", 100.0);

    String distributionStr = getStringParam(config, "distribution", "NORMAL");
    DistributionType distribution = parseDistributionType(distributionStr);

    List<Double> dataset = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      double value = generateValue(distribution, min, max, config);
      dataset.add(value);
    }

    return dataset;
  }

  /** 解析分布类型 */
  private DistributionType parseDistributionType(String distributionStr) {
    try {
      return DistributionType.valueOf(distributionStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid distribution type: {}, using NORMAL as default", distributionStr);
      return DistributionType.NORMAL;
    }
  }

  /** 生成单个数值 */
  private double generateValue(
      DistributionType distribution, double min, double max, FieldConfig config) {
    switch (distribution) {
      case UNIFORM:
        return min + random.nextDouble() * (max - min);
      case NORMAL:
        return generateNormalValue(config, min, max);
      case EXPONENTIAL:
        return generateExponentialValue(config, min, max);
      case POISSON:
        return generatePoissonValue(config, min, max);
      default:
        return min + random.nextDouble() * (max - min);
    }
  }

  /** 生成正态分布值 */
  private double generateNormalValue(FieldConfig config, double min, double max) {
    double mean = getDoubleParam(config, "mean", (min + max) / 2.0);
    double stddev = getDoubleParam(config, "stddev", (max - min) / 6.0);

    double value;
    do {
      value = random.nextGaussian() * stddev + mean;
    } while (value < min || value > max);

    return value;
  }

  /** 生成指数分布值 */
  private double generateExponentialValue(FieldConfig config, double min, double max) {
    double lambda = getDoubleParam(config, "lambda", 1.0);

    double value;
    do {
      value = -Math.log(1 - random.nextDouble()) / lambda + min;
    } while (value > max);

    return value;
  }

  /** 生成泊松分布值 */
  private double generatePoissonValue(FieldConfig config, double min, double max) {
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

  /** 计算统计值 */
  private StatisticResult calculateStatistic(
      StatisticType type, List<Double> dataset, FieldConfig config) {
    Map<String, Object> metadata = new HashMap<>();
    double result;

    switch (type) {
      case MEAN:
        result = calculateMean(dataset);
        break;
      case MEDIAN:
        result = calculateMedian(dataset);
        break;
      case MODE:
        result = calculateMode(dataset);
        break;
      case VARIANCE:
        result = calculateVariance(dataset);
        break;
      case STDDEV:
        result = calculateStandardDeviation(dataset);
        break;
      case RANGE:
        result = calculateRange(dataset);
        metadata.put("min", Collections.min(dataset));
        metadata.put("max", Collections.max(dataset));
        break;
      case PERCENTILE:
        int percentile = getIntParam(config, "percentile", 50);
        result = calculatePercentile(dataset, percentile);
        metadata.put("percentile", percentile);
        break;
      case CORRELATION:
        result = calculateCorrelation(dataset);
        break;
      case ZSCORE:
        result = calculateZScore(dataset, config);
        break;
      case CONFIDENCE_INTERVAL:
        result = calculateConfidenceInterval(dataset, config);
        break;
      default:
        result = calculateMean(dataset);
        break;
    }

    // 添加通用元数据
    metadata.put("sample_size", dataset.size());
    metadata.put("data_mean", calculateMean(dataset));
    metadata.put("data_stddev", calculateStandardDeviation(dataset));

    return new StatisticResult(
        type,
        result,
        getBooleanParam(config, "include_raw_data", false) ? dataset : null,
        metadata);
  }

  /** 计算算术平均数 */
  private double calculateMean(List<Double> dataset) {
    return dataset.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
  }

  /** 计算中位数 */
  private double calculateMedian(List<Double> dataset) {
    List<Double> sorted = dataset.stream().sorted().collect(Collectors.toList());
    int size = sorted.size();

    if (size % 2 == 0) {
      return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    } else {
      return sorted.get(size / 2);
    }
  }

  /** 计算众数 */
  private double calculateMode(List<Double> dataset) {
    Map<Double, Integer> frequency = new HashMap<>();

    for (Double value : dataset) {
      // 四舍五入到2位小数来分组
      Double rounded = Math.round(value * 100.0) / 100.0;
      frequency.put(rounded, frequency.getOrDefault(rounded, 0) + 1);
    }

    return frequency.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(0.0);
  }

  /** 计算方差 */
  private double calculateVariance(List<Double> dataset) {
    double mean = calculateMean(dataset);
    double sumSquaredDiff = dataset.stream().mapToDouble(x -> Math.pow(x - mean, 2)).sum();

    return sumSquaredDiff / (dataset.size() - 1); // 样本方差
  }

  /** 计算标准差 */
  private double calculateStandardDeviation(List<Double> dataset) {
    return Math.sqrt(calculateVariance(dataset));
  }

  /** 计算极差 */
  private double calculateRange(List<Double> dataset) {
    double min = Collections.min(dataset);
    double max = Collections.max(dataset);
    return max - min;
  }

  /** 计算分位数 */
  private double calculatePercentile(List<Double> dataset, int percentile) {
    List<Double> sorted = dataset.stream().sorted().collect(Collectors.toList());
    int size = sorted.size();

    if (percentile <= 0) return sorted.get(0);
    if (percentile >= 100) return sorted.get(size - 1);

    double index = (percentile / 100.0) * (size - 1);
    int lowerIndex = (int) Math.floor(index);
    int upperIndex = (int) Math.ceil(index);

    if (lowerIndex == upperIndex) {
      return sorted.get(lowerIndex);
    } else {
      double weight = index - lowerIndex;
      return sorted.get(lowerIndex) * (1 - weight) + sorted.get(upperIndex) * weight;
    }
  }

  /** 计算相关系数（自相关） */
  private double calculateCorrelation(List<Double> dataset) {
    if (dataset.size() < 2) return 0.0;

    // 计算滞后1的自相关系数
    List<Double> x = dataset.subList(0, dataset.size() - 1);
    List<Double> y = dataset.subList(1, dataset.size());

    double meanX = x.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double meanY = y.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

    double numerator = 0.0;
    double sumXSquared = 0.0;
    double sumYSquared = 0.0;

    for (int i = 0; i < x.size(); i++) {
      double diffX = x.get(i) - meanX;
      double diffY = y.get(i) - meanY;

      numerator += diffX * diffY;
      sumXSquared += diffX * diffX;
      sumYSquared += diffY * diffY;
    }

    double denominator = Math.sqrt(sumXSquared * sumYSquared);
    return denominator == 0 ? 0.0 : numerator / denominator;
  }

  /** 计算Z分数 */
  private double calculateZScore(List<Double> dataset, FieldConfig config) {
    double value = getDoubleParam(config, "target_value", calculateMean(dataset));
    double mean = calculateMean(dataset);
    double stddev = calculateStandardDeviation(dataset);

    return stddev == 0 ? 0.0 : (value - mean) / stddev;
  }

  /** 计算置信区间 */
  private double calculateConfidenceInterval(List<Double> dataset, FieldConfig config) {
    double confidenceLevel = getDoubleParam(config, "confidence_level", 0.95);
    double stddev = calculateStandardDeviation(dataset);
    int n = dataset.size();

    // 使用t分布的近似值（对于大样本，接近正态分布）
    double alpha = 1 - confidenceLevel;
    double tValue = getTValue(alpha / 2, n - 1);

    double marginOfError = tValue * (stddev / Math.sqrt(n));

    // 返回置信区间的半宽度
    return marginOfError;
  }

  /** 获取t分布的临界值（简化版本） */
  private double getTValue(double alpha, int df) {
    // 简化的t值表，实际应用中应使用更精确的计算
    if (df >= 30) {
      // 大样本时使用正态分布近似
      return 1.96; // 95%置信水平
    } else if (df >= 20) {
      return 2.086;
    } else if (df >= 10) {
      return 2.228;
    } else {
      return 2.571; // 保守估计
    }
  }

  /** 格式化结果 */
  private String formatResult(StatisticResult result, OutputFormat format, FieldConfig config) {
    int precision = getIntParam(config, "precision", 4);

    switch (format) {
      case NUMBER:
        return formatAsNumber(result.getValue(), precision);
      case FORMATTED:
        return formatAsFormatted(result, precision);
      case JSON:
        return formatAsJson(result, precision, config);
      case VERBOSE:
        return formatAsVerbose(result, precision);
      default:
        return formatAsNumber(result.getValue(), precision);
    }
  }

  /** 格式化为纯数字 */
  private String formatAsNumber(double value, int precision) {
    BigDecimal bd = BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);
    return bd.toPlainString();
  }

  /** 格式化为格式化字符串 */
  private String formatAsFormatted(StatisticResult result, int precision) {
    String value = formatAsNumber(result.getValue(), precision);
    return result.getType().getDescription() + ": " + value;
  }

  /** 格式化为JSON */
  private String formatAsJson(StatisticResult result, int precision, FieldConfig config) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"type\":\"").append(result.getType().name()).append("\",");
    json.append("\"value\":").append(formatAsNumber(result.getValue(), precision)).append(",");
    json.append("\"description\":\"").append(result.getType().getDescription()).append("\"");

    // 添加元数据
    if (!result.getMetadata().isEmpty()) {
      json.append(",\"metadata\":{");
      boolean first = true;
      for (Map.Entry<String, Object> entry : result.getMetadata().entrySet()) {
        if (!first) json.append(",");
        json.append("\"").append(entry.getKey()).append("\":");
        if (entry.getValue() instanceof Number) {
          json.append(formatAsNumber(((Number) entry.getValue()).doubleValue(), precision));
        } else {
          json.append("\"").append(entry.getValue()).append("\"");
        }
        first = false;
      }
      json.append("}");
    }

    // 添加原始数据（如果包含）
    if (result.getRawData() != null && getBooleanParam(config, "include_raw_data", false)) {
      json.append(",\"raw_data\":[");
      for (int i = 0; i < result.getRawData().size(); i++) {
        if (i > 0) json.append(",");
        json.append(formatAsNumber(result.getRawData().get(i), precision));
      }
      json.append("]");
    }

    json.append("}");
    return json.toString();
  }

  /** 格式化为详细格式 */
  private String formatAsVerbose(StatisticResult result, int precision) {
    StringBuilder verbose = new StringBuilder();
    verbose.append(result.getType().getDescription()).append(": ");
    verbose.append(formatAsNumber(result.getValue(), precision));

    // 添加元数据信息
    if (!result.getMetadata().isEmpty()) {
      verbose.append(" (");
      boolean first = true;
      for (Map.Entry<String, Object> entry : result.getMetadata().entrySet()) {
        if (!first) verbose.append(", ");
        verbose.append(entry.getKey()).append("=");
        if (entry.getValue() instanceof Number) {
          verbose.append(formatAsNumber(((Number) entry.getValue()).doubleValue(), precision));
        } else {
          verbose.append(entry.getValue());
        }
        first = false;
      }
      verbose.append(")");
    }

    return verbose.toString();
  }

  /** 生成描述性统计摘要 */
  public String generateSummaryStatistics(List<Double> dataset, int precision) {
    StringBuilder summary = new StringBuilder();
    summary.append("样本数量: ").append(dataset.size()).append(", ");
    summary.append("均值: ").append(formatAsNumber(calculateMean(dataset), precision)).append(", ");
    summary
        .append("中位数: ")
        .append(formatAsNumber(calculateMedian(dataset), precision))
        .append(", ");
    summary
        .append("标准差: ")
        .append(formatAsNumber(calculateStandardDeviation(dataset), precision))
        .append(", ");
    summary.append("极差: ").append(formatAsNumber(calculateRange(dataset), precision));

    return summary.toString();
  }

  /** 验证统计值 */
  public static boolean isValidStatistic(double value, StatisticType type) {
    switch (type) {
      case VARIANCE:
      case STDDEV:
        return value >= 0;
      case CORRELATION:
        return value >= -1.0 && value <= 1.0;
      case PERCENTILE:
        return value >= 0 && value <= 100;
      default:
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
  }

  /** 获取支持的统计类型 */
  public static List<StatisticType> getSupportedStatistics() {
    return Arrays.asList(StatisticType.values());
  }
}
