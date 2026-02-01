package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 持续时间生成器
 *
 * <p>支持功能： - ISO 8601 持续时间格式（P1Y2M10DT2H30M） - 多种时间单位（年、月、日、小时、分钟、秒） - 人性化格式输出 - 任务持续时间、项目工期等场景 -
 * 与日期时间字段关联 - 精确和近似持续时间
 *
 * <p>配置参数： - format: 输出格式（ISO、HUMAN、COMPACT、VERBOSE、CUSTOM） - units: 包含的时间单位（Y、M、D、H、MIN、S） -
 * minValue: 各单位的最小值 - maxValue: 各单位的最大值 - precision: 精度级别（YEAR、MONTH、DAY、HOUR、MINUTE、SECOND） -
 * type: 持续时间类型（SHORT、MEDIUM、LONG、CUSTOM） - includeZero: 是否包含零值单位 - locale: 本地化设置
 *
 * @author DataForge
 * @version 1.0
 */
public class DurationGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  @Override
  public String getType() {
    return "duration";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {

    // 解析配置参数
    String format = getStringParam(config, "format", "ISO");
    String units = getStringParam(config, "units", "Y,M,D,H,MIN,S");
    int minValue = getIntParam(config, "minValue", 0);
    int maxValue = getIntParam(config, "maxValue", 10);
    String precision = getStringParam(config, "precision", "DAY");
    String type = getStringParam(config, "type", "MEDIUM");
    boolean includeZero = getBooleanParam(config, "includeZero", false);
    String locale = getStringParam(config, "locale", "en_US");

    try {
      // 检查上下文中是否有相关持续时间信息
      DurationInfo contextDuration = getDurationFromContext(context);
      if (contextDuration != null) {
        return formatDuration(contextDuration, format, locale);
      }

      // 生成随机持续时间
      DurationInfo randomDuration =
          generateRandomDuration(units, minValue, maxValue, precision, type, includeZero);

      // 将生成的持续时间存储到上下文中
      storeDurationInContext(context, randomDuration);

      return formatDuration(randomDuration, format, locale);

    } catch (Exception e) {
      // 如果解析失败，生成默认持续时间
      DurationInfo defaultDuration = new DurationInfo(0, 0, 1, 0, 0, 0);
      return formatDuration(defaultDuration, format, locale);
    }
  }

  /** 从上下文中获取相关持续时间信息 */
  private DurationInfo getDurationFromContext(DataForgeContext context) {
    // 尝试从上下文中获取已生成的持续时间
    return context.get("generated_duration", DurationInfo.class).orElse(null);
  }

  /** 生成随机持续时间 */
  private DurationInfo generateRandomDuration(
      String units,
      int minValue,
      int maxValue,
      String precision,
      String type,
      boolean includeZero) {
    Random random = ThreadLocalRandom.current();
    String[] unitArray = units.split(",");

    // 根据类型设置默认范围
    int[] ranges = getTypeRanges(type);
    int actualMin = Math.max(minValue, ranges[0]);
    int actualMax = Math.min(maxValue, ranges[1]);

    DurationInfo duration = new DurationInfo(0, 0, 0, 0, 0, 0);

    for (String unit : unitArray) {
      unit = unit.trim().toUpperCase();
      int value = generateUnitValue(unit, actualMin, actualMax, precision, includeZero, random);

      switch (unit) {
        case "Y":
          duration.years = value;
          break;
        case "M":
          duration.months = value;
          break;
        case "D":
          duration.days = value;
          break;
        case "H":
          duration.hours = value;
          break;
        case "MIN":
          duration.minutes = value;
          break;
        case "S":
          duration.seconds = value;
          break;
      }
    }

    // 确保至少有一个非零值
    if (!includeZero && isAllZero(duration)) {
      // 根据精度设置一个默认值
      setDefaultValue(duration, precision, random);
    }

    return duration;
  }

  /** 根据类型获取范围 */
  private int[] getTypeRanges(String type) {
    switch (type.toUpperCase()) {
      case "SHORT":
        return new int[] {0, 2}; // 0-2 单位
      case "MEDIUM":
        return new int[] {1, 10}; // 1-10 单位
      case "LONG":
        return new int[] {5, 50}; // 5-50 单位
      case "CUSTOM":
      default:
        return new int[] {0, 100}; // 0-100 单位
    }
  }

  /** 生成单位值 */
  private int generateUnitValue(
      String unit,
      int minValue,
      int maxValue,
      String precision,
      boolean includeZero,
      Random random) {
    // 根据精度调整范围
    int[] unitRange = getUnitRange(unit, precision);
    int actualMin = Math.max(minValue, unitRange[0]);
    int actualMax = Math.min(maxValue, unitRange[1]);

    if (actualMax <= actualMin) {
      return includeZero ? 0 : actualMin;
    }

    int value = actualMin + random.nextInt(actualMax - actualMin + 1);
    return (value == 0 && !includeZero) ? 1 : value;
  }

  /** 获取单位的合理范围 */
  private int[] getUnitRange(String unit, String precision) {
    switch (unit) {
      case "Y":
        return new int[] {0, 10}; // 0-10年
      case "M":
        return new int[] {0, 11}; // 0-11月
      case "D":
        return new int[] {0, 30}; // 0-30天
      case "H":
        return new int[] {0, 23}; // 0-23小时
      case "MIN":
        return new int[] {0, 59}; // 0-59分钟
      case "S":
        return new int[] {0, 59}; // 0-59秒
      default:
        return new int[] {0, 100};
    }
  }

  /** 检查是否所有值都为零 */
  private boolean isAllZero(DurationInfo duration) {
    return duration.years == 0
        && duration.months == 0
        && duration.days == 0
        && duration.hours == 0
        && duration.minutes == 0
        && duration.seconds == 0;
  }

  /** 设置默认值 */
  private void setDefaultValue(DurationInfo duration, String precision, Random random) {
    switch (precision.toUpperCase()) {
      case "YEAR":
        duration.years = 1;
        break;
      case "MONTH":
        duration.months = 1;
        break;
      case "DAY":
        duration.days = 1;
        break;
      case "HOUR":
        duration.hours = 1;
        break;
      case "MINUTE":
        duration.minutes = 1;
        break;
      case "SECOND":
      default:
        duration.seconds = 1;
        break;
    }
  }

  /** 将持续时间信息存储到上下文中 */
  private void storeDurationInContext(DataForgeContext context, DurationInfo duration) {
    context.put("generated_duration", duration);
    context.put("generated_duration_years", duration.years);
    context.put("generated_duration_months", duration.months);
    context.put("generated_duration_days", duration.days);
    context.put("generated_duration_hours", duration.hours);
    context.put("generated_duration_minutes", duration.minutes);
    context.put("generated_duration_seconds", duration.seconds);

    // 计算总秒数（近似）
    long totalSeconds =
        duration.seconds
            + duration.minutes * 60L
            + duration.hours * 3600L
            + duration.days * 86400L
            + duration.months * 2592000L
            + // 30天
            duration.years * 31536000L; // 365天
    context.put("generated_duration_total_seconds", totalSeconds);
  }

  /** 格式化持续时间 */
  private String formatDuration(DurationInfo duration, String format, String locale) {
    switch (format.toUpperCase()) {
      case "ISO":
        return formatIso8601Duration(duration);
      case "HUMAN":
        return formatHumanDuration(duration, locale);
      case "COMPACT":
        return formatCompactDuration(duration);
      case "VERBOSE":
        return formatVerboseDuration(duration, locale);
      case "CHINESE":
        return formatChineseDuration(duration);
      default:
        return formatIso8601Duration(duration);
    }
  }

  /** 格式化为ISO 8601持续时间 */
  private String formatIso8601Duration(DurationInfo duration) {
    StringBuilder sb = new StringBuilder("P");

    // 日期部分
    if (duration.years > 0) {
      sb.append(duration.years).append("Y");
    }
    if (duration.months > 0) {
      sb.append(duration.months).append("M");
    }
    if (duration.days > 0) {
      sb.append(duration.days).append("D");
    }

    // 时间部分
    if (duration.hours > 0 || duration.minutes > 0 || duration.seconds > 0) {
      sb.append("T");
      if (duration.hours > 0) {
        sb.append(duration.hours).append("H");
      }
      if (duration.minutes > 0) {
        sb.append(duration.minutes).append("M");
      }
      if (duration.seconds > 0) {
        sb.append(duration.seconds).append("S");
      }
    }

    // 如果所有值都为0，返回P0D
    return sb.length() == 1 ? "P0D" : sb.toString();
  }

  /** 格式化为人性化持续时间 */
  private String formatHumanDuration(DurationInfo duration, String locale) {
    StringBuilder sb = new StringBuilder();
    boolean isFirst = true;

    if (duration.years > 0) {
      if (!isFirst) sb.append(" ");
      sb.append(duration.years)
          .append(locale.startsWith("zh") ? "年" : " year" + (duration.years > 1 ? "s" : ""));
      isFirst = false;
    }
    if (duration.months > 0) {
      if (!isFirst) sb.append(" ");
      sb.append(duration.months)
          .append(locale.startsWith("zh") ? "个月" : " month" + (duration.months > 1 ? "s" : ""));
      isFirst = false;
    }
    if (duration.days > 0) {
      if (!isFirst) sb.append(" ");
      sb.append(duration.days)
          .append(locale.startsWith("zh") ? "天" : " day" + (duration.days > 1 ? "s" : ""));
      isFirst = false;
    }
    if (duration.hours > 0) {
      if (!isFirst) sb.append(" ");
      sb.append(duration.hours)
          .append(locale.startsWith("zh") ? "小时" : " hour" + (duration.hours > 1 ? "s" : ""));
      isFirst = false;
    }
    if (duration.minutes > 0) {
      if (!isFirst) sb.append(" ");
      sb.append(duration.minutes)
          .append(locale.startsWith("zh") ? "分钟" : " minute" + (duration.minutes > 1 ? "s" : ""));
      isFirst = false;
    }
    if (duration.seconds > 0) {
      if (!isFirst) sb.append(" ");
      sb.append(duration.seconds)
          .append(locale.startsWith("zh") ? "秒" : " second" + (duration.seconds > 1 ? "s" : ""));
      isFirst = false;
    }

    return sb.length() == 0 ? (locale.startsWith("zh") ? "0秒" : "0 seconds") : sb.toString();
  }

  /** 格式化为紧凑持续时间 */
  private String formatCompactDuration(DurationInfo duration) {
    StringBuilder sb = new StringBuilder();

    if (duration.years > 0) sb.append(duration.years).append("y");
    if (duration.months > 0) sb.append(duration.months).append("mo");
    if (duration.days > 0) sb.append(duration.days).append("d");
    if (duration.hours > 0) sb.append(duration.hours).append("h");
    if (duration.minutes > 0) sb.append(duration.minutes).append("m");
    if (duration.seconds > 0) sb.append(duration.seconds).append("s");

    return sb.length() == 0 ? "0s" : sb.toString();
  }

  /** 格式化为详细持续时间 */
  private String formatVerboseDuration(DurationInfo duration, String locale) {
    if (locale.startsWith("zh")) {
      return formatChineseDuration(duration);
    }

    StringBuilder sb = new StringBuilder();
    boolean isFirst = true;

    if (duration.years > 0) {
      sb.append(duration.years).append(" year").append(duration.years > 1 ? "s" : "");
      isFirst = false;
    }
    if (duration.months > 0) {
      if (!isFirst) sb.append(", ");
      sb.append(duration.months).append(" month").append(duration.months > 1 ? "s" : "");
      isFirst = false;
    }
    if (duration.days > 0) {
      if (!isFirst) sb.append(", ");
      sb.append(duration.days).append(" day").append(duration.days > 1 ? "s" : "");
      isFirst = false;
    }
    if (duration.hours > 0) {
      if (!isFirst) sb.append(", ");
      sb.append(duration.hours).append(" hour").append(duration.hours > 1 ? "s" : "");
      isFirst = false;
    }
    if (duration.minutes > 0) {
      if (!isFirst) sb.append(", ");
      sb.append(duration.minutes).append(" minute").append(duration.minutes > 1 ? "s" : "");
      isFirst = false;
    }
    if (duration.seconds > 0) {
      if (!isFirst) sb.append(", ");
      sb.append(duration.seconds).append(" second").append(duration.seconds > 1 ? "s" : "");
    }

    return sb.length() == 0 ? "0 seconds" : sb.toString();
  }

  /** 格式化为中文持续时间 */
  private String formatChineseDuration(DurationInfo duration) {
    StringBuilder sb = new StringBuilder();

    if (duration.years > 0) sb.append(duration.years).append("年");
    if (duration.months > 0) sb.append(duration.months).append("个月");
    if (duration.days > 0) sb.append(duration.days).append("天");
    if (duration.hours > 0) sb.append(duration.hours).append("小时");
    if (duration.minutes > 0) sb.append(duration.minutes).append("分钟");
    if (duration.seconds > 0) sb.append(duration.seconds).append("秒");

    return sb.length() == 0 ? "0秒" : sb.toString();
  }

  /** 持续时间信息内部类 */
  private static class DurationInfo {
    int years;
    int months;
    int days;
    int hours;
    int minutes;
    int seconds;

    DurationInfo(int years, int months, int days, int hours, int minutes, int seconds) {
      this.years = years;
      this.months = months;
      this.days = days;
      this.hours = hours;
      this.minutes = minutes;
      this.seconds = seconds;
    }
  }

  @Override
  public String getDescription() {
    return "Generator for duration values in ISO 8601 and human-readable formats";
  }
}
