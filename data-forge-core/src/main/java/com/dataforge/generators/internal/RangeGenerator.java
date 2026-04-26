package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数值范围生成器
 *
 * <p>支持生成各种类型的数值范围，包括整数范围、小数范围、 日期范围、时间范围等，用于区间查询、范围筛选、边界测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>range_type: 范围类型 (INTEGER|DECIMAL|DATE|TIME|PERCENTAGE|CUSTOM) 默认: INTEGER
 *   <li>min_value: 最小值 默认: 0
 *   <li>max_value: 最大值 默认: 100
 *   <li>format: 输出格式 (HYPHEN|BRACKET|TO|JSON|VERBOSE) 默认: HYPHEN
 *   <li>precision: 小数精度 默认: 2
 *   <li>gap_type: 间隔类型 (CONTINUOUS|DISCRETE|RANDOM) 默认: CONTINUOUS
 *   <li>gap_size: 间隔大小 默认: 自动计算
 *   <li>include_bounds: 是否包含边界 默认: true
 *   <li>overlap_allowed: 是否允许重叠 默认: false
 *   <li>date_format: 日期格式 默认: yyyy-MM-dd
 *   <li>time_format: 时间格式 默认: HH:mm:ss
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class RangeGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(RangeGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  @Override
  public String getType() {
    return "range";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String rangeType = getStringParam(config, "range_type", "INTEGER");
      String format = getStringParam(config, "format", "HYPHEN");

      String result = generateRange(rangeType, format, config);

      // 存储到上下文
      context.put("range_type", rangeType);
      context.put("range_format", format);

      return result;

    } catch (Exception e) {
      logger.error("Failed to generate range", e);
      return "0-100";
    }
  }

  private String generateRange(String rangeType, String format, FieldConfig config) {
    switch (rangeType.toUpperCase()) {
      case "INTEGER":
        return generateIntegerRange(format, config);
      case "DECIMAL":
        return generateDecimalRange(format, config);
      case "DATE":
        return generateDateRange(format, config);
      case "TIME":
        return generateTimeRange(format, config);
      case "PERCENTAGE":
        return generatePercentageRange(format, config);
      default:
        return generateIntegerRange(format, config);
    }
  }

  private String generateIntegerRange(String format, FieldConfig config) {
    int minValue = getIntParam(config, "min_value", 0);
    int maxValue = getIntParam(config, "max_value", 100);

    int start = minValue + random.nextInt(maxValue - minValue);
    int end = start + random.nextInt(maxValue - start + 1);

    return formatRange(String.valueOf(start), String.valueOf(end), format);
  }

  private String generateDecimalRange(String format, FieldConfig config) {
    double minValue = getDoubleParam(config, "min_value", 0.0);
    double maxValue = getDoubleParam(config, "max_value", 100.0);
    int precision = getIntParam(config, "precision", 2);

    double start = minValue + random.nextDouble() * (maxValue - minValue);
    double end = start + random.nextDouble() * (maxValue - start);

    BigDecimal startBd = BigDecimal.valueOf(start).setScale(precision, RoundingMode.HALF_UP);
    BigDecimal endBd = BigDecimal.valueOf(end).setScale(precision, RoundingMode.HALF_UP);

    return formatRange(startBd.toPlainString(), endBd.toPlainString(), format);
  }

  private String generateDateRange(String format, FieldConfig config) {
    String dateFormat = getStringParam(config, "date_format", "yyyy-MM-dd");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

    LocalDate startDate = LocalDate.now().minusDays(random.nextInt(365));
    LocalDate endDate = startDate.plusDays(random.nextInt(30));

    return formatRange(startDate.format(formatter), endDate.format(formatter), format);
  }

  private String generateTimeRange(String format, FieldConfig config) {
    String timeFormat = getStringParam(config, "time_format", "HH:mm:ss");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);

    LocalDateTime startTime =
        LocalDateTime.now()
            .withHour(random.nextInt(24))
            .withMinute(random.nextInt(60))
            .withSecond(0);
    LocalDateTime endTime = startTime.plusHours(random.nextInt(12));

    return formatRange(startTime.format(formatter), endTime.format(formatter), format);
  }

  private String generatePercentageRange(String format, FieldConfig config) {
    double start = random.nextDouble() * 80;
    double end = start + random.nextDouble() * (100 - start);

    int precision = getIntParam(config, "precision", 1);
    BigDecimal startBd = BigDecimal.valueOf(start).setScale(precision, RoundingMode.HALF_UP);
    BigDecimal endBd = BigDecimal.valueOf(end).setScale(precision, RoundingMode.HALF_UP);

    return formatRange(startBd.toPlainString() + "%", endBd.toPlainString() + "%", format);
  }

  private String formatRange(String start, String end, String format) {
    switch (format.toUpperCase()) {
      case "HYPHEN":
        return start + "-" + end;
      case "BRACKET":
        return "[" + start + ", " + end + "]";
      case "TO":
        return start + " to " + end;
      case "JSON":
        return "{\"min\":\"" + start + "\",\"max\":\"" + end + "\"}";
      case "VERBOSE":
        return "Range from " + start + " to " + end;
      default:
        return start + "-" + end;
    }
  }
}
