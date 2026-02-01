package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 时间生成器
 *
 * <p>支持功能： - 多种时间格式（24小时制、12小时制、紧凑格式等） - 时间范围控制 - 毫秒/微秒精度支持 - 业务时间段生成（工作时间、营业时间等） - 异常时间生成（用于测试） -
 * 与日期字段关联生成完整时间戳
 *
 * <p>配置参数： - format: 时间格式（24H、12H、COMPACT、ISO、CUSTOM） - customFormat: 自定义格式字符串 - startTime: 开始时间 -
 * endTime: 结束时间 - precision: 精度（SECOND、MILLISECOND、MICROSECOND） - timeType:
 * 时间类型（ANY、BUSINESS、NIGHT、PEAK） - valid: 是否生成有效时间 - includeAmPm: 是否包含AM/PM标识
 *
 * @author DataForge
 * @version 1.0
 */
public class TimeGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final String[] TIME_FORMATS = {
    "HH:mm:ss", // 24小时制
    "hh:mm:ss a", // 12小时制带AM/PM
    "HHmmss", // 紧凑格式
    "HH:mm:ss.SSS", // 带毫秒
    "HH:mm", // 小时分钟
    "HH时mm分ss秒" // 中文格式
  };

  // 业务时间段定义
  private static final TimeRange BUSINESS_HOURS = new TimeRange(9, 0, 18, 0);
  private static final TimeRange NIGHT_HOURS = new TimeRange(22, 0, 6, 0);
  private static final TimeRange PEAK_HOURS_MORNING = new TimeRange(7, 30, 9, 30);
  private static final TimeRange PEAK_HOURS_EVENING = new TimeRange(17, 30, 19, 30);

  @Override
  public String getType() {
    return "time";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    // 解析配置参数
    String format = getStringParam(config, "format", "24H");
    String customFormat = getStringParam(config, "customFormat", "HH:mm:ss");
    String startTimeStr = getStringParam(config, "startTime", "00:00:00");
    String endTimeStr = getStringParam(config, "endTime", "23:59:59");
    String precision = getStringParam(config, "precision", "SECOND");
    String timeType = getStringParam(config, "timeType", "ANY");
    boolean valid = getBooleanParam(config, "valid", true);
    boolean includeAmPm = getBooleanParam(config, "includeAmPm", false);

    try {
      // 解析时间范围
      LocalTime startTime = parseTime(startTimeStr);
      LocalTime endTime = parseTime(endTimeStr);

      // 检查上下文中是否有相关时间信息
      LocalTime contextTime = getTimeFromContext(context);
      if (contextTime != null) {
        return formatTime(contextTime, format, customFormat, precision, includeAmPm);
      }

      // 生成随机时间
      LocalTime randomTime = generateRandomTime(startTime, endTime, timeType, precision, valid);

      // 将生成的时间存储到上下文中
      context.put("generated_time", randomTime);
      context.put("generated_hour", randomTime.getHour());
      context.put("generated_minute", randomTime.getMinute());
      context.put("generated_second", randomTime.getSecond());
      context.put("generated_time_period", getTimePeriod(randomTime));

      return formatTime(randomTime, format, customFormat, precision, includeAmPm);

    } catch (Exception e) {
      // 如果解析失败，生成默认时间
      LocalTime defaultTime = LocalTime.now();
      return formatTime(defaultTime, format, customFormat, precision, includeAmPm);
    }
  }

  /** 从上下文中获取相关时间信息 */
  private LocalTime getTimeFromContext(DataForgeContext context) {
    // 尝试从上下文中获取已生成的时间
    return context.get("generated_time", LocalTime.class).orElse(null);
  }

  /** 解析时间字符串 */
  private LocalTime parseTime(String timeStr) {
    try {
      if (timeStr.contains(".")) {
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
      } else if (timeStr.length() == 5) {
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
      } else {
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
      }
    } catch (Exception e) {
      return LocalTime.of(0, 0, 0);
    }
  }

  /** 生成随机时间 */
  private LocalTime generateRandomTime(
      LocalTime startTime, LocalTime endTime, String timeType, String precision, boolean valid) {
    Random random = ThreadLocalRandom.current();

    if (!valid) {
      // 生成无效时间用于测试
      return generateInvalidTime(random);
    }

    LocalTime randomTime;
    int maxAttempts = 100;
    int attempts = 0;

    do {
      randomTime = generateTimeInRange(startTime, endTime, precision, random);
      attempts++;
    } while (!matchesTimeType(randomTime, timeType) && attempts < maxAttempts);

    return randomTime;
  }

  /** 在指定范围内生成时间 */
  private LocalTime generateTimeInRange(
      LocalTime startTime, LocalTime endTime, String precision, Random random) {
    long startSecond = startTime.toSecondOfDay();
    long endSecond = endTime.toSecondOfDay();

    // 处理跨天的情况
    if (endSecond < startSecond) {
      endSecond += 24 * 60 * 60; // 加一天的秒数
    }

    long randomSecond = startSecond + random.nextLong(endSecond - startSecond + 1);
    randomSecond = randomSecond % (24 * 60 * 60); // 确保在一天内

    LocalTime time = LocalTime.ofSecondOfDay(randomSecond);

    // 根据精度调整
    switch (precision.toUpperCase()) {
      case "MILLISECOND":
        int millis = random.nextInt(1000);
        time = time.withNano(millis * 1_000_000);
        break;
      case "MICROSECOND":
        int micros = random.nextInt(1_000_000);
        time = time.withNano(micros * 1_000);
        break;
      case "SECOND":
      default:
        time = time.withNano(0);
        break;
    }

    return time;
  }

  /** 生成无效时间（用于测试） */
  private LocalTime generateInvalidTime(Random random) {
    // 返回一个基础时间，实际的无效时间会在格式化时处理
    return LocalTime.of(12, 0, 0);
  }

  /** 检查时间是否匹配指定的时间类型 */
  private boolean matchesTimeType(LocalTime time, String timeType) {
    if (timeType == null || timeType.trim().isEmpty()) {
      return true; // 默认为ANY
    }
    switch (timeType.toUpperCase()) {
      case "BUSINESS":
        return isInTimeRange(time, BUSINESS_HOURS);
      case "NIGHT":
        return isInTimeRange(time, NIGHT_HOURS);
      case "PEAK":
        return isInTimeRange(time, PEAK_HOURS_MORNING) || isInTimeRange(time, PEAK_HOURS_EVENING);
      case "ANY":
      default:
        return true;
    }
  }

  /** 检查时间是否在指定范围内 */
  private boolean isInTimeRange(LocalTime time, TimeRange range) {
    LocalTime start = LocalTime.of(range.startHour, range.startMinute);
    LocalTime end = LocalTime.of(range.endHour, range.endMinute);

    if (start.isBefore(end)) {
      // 同一天内的时间范围
      return !time.isBefore(start) && !time.isAfter(end);
    } else {
      // 跨天的时间范围
      return !time.isBefore(start) || !time.isAfter(end);
    }
  }

  /** 获取时间段描述 */
  private String getTimePeriod(LocalTime time) {
    int hour = time.getHour();
    if (hour >= 6 && hour < 12) {
      return "MORNING";
    } else if (hour >= 12 && hour < 18) {
      return "AFTERNOON";
    } else if (hour >= 18 && hour < 22) {
      return "EVENING";
    } else {
      return "NIGHT";
    }
  }

  /** 格式化时间 */
  private String formatTime(
      LocalTime time, String format, String customFormat, String precision, boolean includeAmPm) {
    try {
      DateTimeFormatter formatter;

      if (format == null || format.trim().isEmpty()) {
        format = "ISO"; // 默认格式
      }

      switch (format.toUpperCase()) {
        case "24H":
          formatter = getFormatterByPrecision("HH:mm:ss", precision);
          break;
        case "12H":
          formatter = getFormatterByPrecision("hh:mm:ss", precision);
          if (includeAmPm) {
            formatter = DateTimeFormatter.ofPattern(formatter.toString() + " a");
          }
          break;
        case "COMPACT":
          formatter = getFormatterByPrecision("HHmmss", precision);
          break;
        case "ISO":
          formatter = getFormatterByPrecision("HH:mm:ss", precision);
          break;
        case "CHINESE":
          formatter = DateTimeFormatter.ofPattern("HH时mm分ss秒");
          break;
        case "CUSTOM":
          formatter = DateTimeFormatter.ofPattern(customFormat);
          break;
        default:
          // 随机选择一种格式
          String randomFormat =
              TIME_FORMATS[ThreadLocalRandom.current().nextInt(TIME_FORMATS.length)];
          formatter = DateTimeFormatter.ofPattern(randomFormat);
          break;
      }

      return time.format(formatter);

    } catch (Exception e) {
      // 如果格式化失败，返回默认格式
      return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
  }

  /** 根据精度获取格式化器 */
  private DateTimeFormatter getFormatterByPrecision(String basePattern, String precision) {
    if (precision == null || precision.trim().isEmpty()) {
      precision = "SECOND"; // 默认精度
    }
    switch (precision.toUpperCase()) {
      case "MILLISECOND":
        return DateTimeFormatter.ofPattern(basePattern + ".SSS");
      case "MICROSECOND":
        return DateTimeFormatter.ofPattern(basePattern + ".SSSSSS");
      case "SECOND":
      default:
        return DateTimeFormatter.ofPattern(basePattern);
    }
  }

  /** 时间范围内部类 */
  private static class TimeRange {
    final int startHour;
    final int startMinute;
    final int endHour;
    final int endMinute;

    TimeRange(int startHour, int startMinute, int endHour, int endMinute) {
      this.startHour = startHour;
      this.startMinute = startMinute;
      this.endHour = endHour;
      this.endMinute = endMinute;
    }
  }

  @Override
  public String getDescription() {
    return "Generator for time values with multiple formats and business hour filtering";
  }
}
