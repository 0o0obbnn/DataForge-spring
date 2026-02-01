package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 时间戳生成器
 *
 * <p>支持功能： - 多种时间戳格式（Unix秒、毫秒、微秒、纳秒） - 多种输出格式（数字、ISO字符串、自定义格式） - 时间范围控制 - 时区支持 - 未来/过去时间戳生成 -
 * 与日期时间字段关联 - 高精度时间戳支持
 *
 * <p>配置参数： - unit: 时间戳单位（SECONDS、MILLISECONDS、MICROSECONDS、NANOSECONDS） - format:
 * 输出格式（NUMERIC、ISO、CUSTOM） - customFormat: 自定义格式字符串 - startDate: 开始日期时间 - endDate: 结束日期时间 -
 * timezone: 时区 - precision: 精度控制 - type: 时间戳类型（PAST、CURRENT、FUTURE、RANGE）
 *
 * @author DataForge
 * @version 1.0
 */
public class TimestampGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  @Override
  public String getType() {
    return "timestamp";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    // 解析配置参数
    String unit = getStringParam(config, "unit", "MILLISECONDS");
    String format = getStringParam(config, "format", "NUMERIC");
    String customFormat = getStringParam(config, "customFormat", "yyyy-MM-dd HH:mm:ss");
    String startDateStr = getStringParam(config, "startDate", "2020-01-01 00:00:00");
    String endDateStr = getStringParam(config, "endDate", "2025-12-31 23:59:59");
    String timezone = getStringParam(config, "timezone", "UTC");
    String type = getStringParam(config, "type", "RANGE");

    try {
      // 检查上下文中是否有相关时间信息
      Long contextTimestamp = getTimestampFromContext(context, unit);
      if (contextTimestamp != null) {
        return formatTimestamp(contextTimestamp, unit, format, customFormat, timezone);
      }

      // 生成随机时间戳
      long timestamp = generateRandomTimestamp(startDateStr, endDateStr, timezone, unit, type);

      // 将生成的时间戳存储到上下文中
      storeTimestampInContext(context, timestamp, unit, timezone);

      return formatTimestamp(timestamp, unit, format, customFormat, timezone);

    } catch (Exception e) {
      // 如果解析失败，生成当前时间戳
      long currentTimestamp = getCurrentTimestamp(unit);
      return formatTimestamp(currentTimestamp, unit, format, customFormat, timezone);
    }
  }

  /** 从上下文中获取相关时间戳信息 */
  private Long getTimestampFromContext(DataForgeContext context, String unit) {
    // 尝试从上下文中获取已生成的时间戳
    Long existingTimestamp = context.get("generated_timestamp", Long.class).orElse(null);
    if (existingTimestamp != null) {
      return convertTimestampUnit(existingTimestamp, "MILLISECONDS", unit);
    }

    // 尝试从日期和时间字段构建时间戳
    java.time.LocalDate date =
        context.get("generated_date", java.time.LocalDate.class).orElse(null);
    java.time.LocalTime time =
        context.get("generated_time", java.time.LocalTime.class).orElse(null);

    if (date != null && time != null) {
      LocalDateTime dateTime = LocalDateTime.of(date, time);
      long epochMilli = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
      return convertTimestampUnit(epochMilli, "MILLISECONDS", unit);
    }

    return null;
  }

  /** 生成随机时间戳 */
  private long generateRandomTimestamp(
      String startDateStr, String endDateStr, String timezone, String unit, String type) {
    Random random = ThreadLocalRandom.current();
    ZoneId zoneId = ZoneId.of(timezone);

    long startTimestamp, endTimestamp;

    if (type == null || type.trim().isEmpty()) {
      type = "RANGE"; // 默认类型
    }

    switch (type.toUpperCase()) {
      case "PAST":
        endTimestamp = getCurrentTimestamp("SECONDS");
        startTimestamp = endTimestamp - (365L * 24 * 60 * 60 * 5); // 5年前
        break;
      case "CURRENT":
        long current = getCurrentTimestamp("SECONDS");
        startTimestamp = current - 3600; // 1小时前
        endTimestamp = current + 3600; // 1小时后
        break;
      case "FUTURE":
        startTimestamp = getCurrentTimestamp("SECONDS");
        endTimestamp = startTimestamp + (365L * 24 * 60 * 60 * 5); // 5年后
        break;
      case "RANGE":
      default:
        startTimestamp = parseDateTime(startDateStr, zoneId);
        endTimestamp = parseDateTime(endDateStr, zoneId);
        break;
    }

    // 生成随机时间戳（秒级）
    long randomSeconds = startTimestamp + random.nextLong(endTimestamp - startTimestamp + 1);

    // 转换为指定单位
    return convertTimestampUnit(randomSeconds, "SECONDS", unit);
  }

  /** 解析日期时间字符串为时间戳 */
  private long parseDateTime(String dateTimeStr, ZoneId zoneId) {
    try {
      DateTimeFormatter formatter;
      if (dateTimeStr.length() == 10) {
        // 只有日期
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return java.time.LocalDate.parse(dateTimeStr, formatter)
            .atStartOfDay(zoneId)
            .toEpochSecond();
      } else {
        // 日期和时间
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter).atZone(zoneId).toEpochSecond();
      }
    } catch (Exception e) {
      // 解析失败，返回当前时间
      return Instant.now().getEpochSecond();
    }
  }

  /** 获取当前时间戳 */
  private long getCurrentTimestamp(String unit) {
    Instant now = Instant.now();
    if (unit == null || unit.trim().isEmpty()) {
      unit = "MILLISECONDS"; // 默认单位
    }
    switch (unit.toUpperCase()) {
      case "SECONDS":
        return now.getEpochSecond();
      case "MILLISECONDS":
        return now.toEpochMilli();
      case "MICROSECONDS":
        return now.toEpochMilli() * 1000L + (now.getNano() % 1_000_000) / 1000;
      case "NANOSECONDS":
        return now.getEpochSecond() * 1_000_000_000L + now.getNano();
      default:
        return now.toEpochMilli();
    }
  }

  /** 转换时间戳单位 */
  private long convertTimestampUnit(long timestamp, String fromUnit, String toUnit) {
    if (fromUnit == null || fromUnit.trim().isEmpty()) {
      fromUnit = "MILLISECONDS";
    }
    if (toUnit == null || toUnit.trim().isEmpty()) {
      toUnit = "MILLISECONDS";
    }

    if (fromUnit.equals(toUnit)) {
      return timestamp;
    }

    // 先转换为毫秒
    long millis;
    switch (fromUnit.toUpperCase()) {
      case "SECONDS":
        millis = timestamp * 1000L;
        break;
      case "MILLISECONDS":
        millis = timestamp;
        break;
      case "MICROSECONDS":
        millis = timestamp / 1000L;
        break;
      case "NANOSECONDS":
        millis = timestamp / 1_000_000L;
        break;
      default:
        millis = timestamp;
        break;
    }

    // 再转换为目标单位
    switch (toUnit.toUpperCase()) {
      case "SECONDS":
        return millis / 1000L;
      case "MILLISECONDS":
        return millis;
      case "MICROSECONDS":
        return millis * 1000L;
      case "NANOSECONDS":
        return millis * 1_000_000L;
      default:
        return millis;
    }
  }

  /** 将时间戳信息存储到上下文中 */
  private void storeTimestampInContext(
      DataForgeContext context, long timestamp, String unit, String timezone) {
    context.put("generated_timestamp", timestamp);
    context.put("generated_timestamp_unit", unit);
    context.put("generated_timezone", timezone);

    // 转换为标准时间并存储
    long millis = convertTimestampUnit(timestamp, unit, "MILLISECONDS");
    Instant instant = Instant.ofEpochMilli(millis);
    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of(timezone));

    context.put("generated_datetime", dateTime);
    context.put("generated_date", dateTime.toLocalDate());
    context.put("generated_time", dateTime.toLocalTime());
    context.put("generated_year", dateTime.getYear());
    context.put("generated_month", dateTime.getMonthValue());
    context.put("generated_day", dateTime.getDayOfMonth());
    context.put("generated_hour", dateTime.getHour());
    context.put("generated_minute", dateTime.getMinute());
    context.put("generated_second", dateTime.getSecond());
  }

  /** 格式化时间戳 */
  private String formatTimestamp(
      long timestamp, String unit, String format, String customFormat, String timezone) {
    if (format == null || format.trim().isEmpty()) {
      format = "NUMERIC"; // 默认格式
    }
    switch (format.toUpperCase()) {
      case "NUMERIC":
        return String.valueOf(timestamp);
      case "ISO":
        return formatAsIsoString(timestamp, unit, timezone);
      case "CUSTOM":
        return formatAsCustomString(timestamp, unit, customFormat, timezone);
      default:
        return String.valueOf(timestamp);
    }
  }

  /** 格式化为ISO字符串 */
  private String formatAsIsoString(long timestamp, String unit, String timezone) {
    try {
      long millis = convertTimestampUnit(timestamp, unit, "MILLISECONDS");
      Instant instant = Instant.ofEpochMilli(millis);
      LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of(timezone));
      return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (Exception e) {
      return String.valueOf(timestamp);
    }
  }

  /** 格式化为自定义字符串 */
  private String formatAsCustomString(
      long timestamp, String unit, String customFormat, String timezone) {
    try {
      long millis = convertTimestampUnit(timestamp, unit, "MILLISECONDS");
      Instant instant = Instant.ofEpochMilli(millis);
      LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of(timezone));
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(customFormat);
      return dateTime.format(formatter);
    } catch (Exception e) {
      return String.valueOf(timestamp);
    }
  }

  @Override
  public String getDescription() {
    return "Generator for timestamp values with multiple units and formats";
  }
}
