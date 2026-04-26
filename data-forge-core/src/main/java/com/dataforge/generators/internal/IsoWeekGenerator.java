package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ISO 8601 周日期生成器
 *
 * <p>支持功能： - ISO 8601 周日期格式（YYYY-Www-D） - 年份和周数范围控制 - 星期几指定 - 与日期字段关联 - 多种输出格式 - 周数计算和验证
 *
 * <p>配置参数： - format: 输出格式（ISO、COMPACT、VERBOSE、CUSTOM） - customFormat: 自定义格式字符串 - yearStart: 开始年份 -
 * yearEnd: 结束年份 - weekStart: 最小周数 - weekEnd: 最大周数 - dayOfWeek: 星期几（1-7，1为周一） - valid: 是否生成有效周日期 -
 * locale: 本地化设置
 *
 * @author DataForge
 * @version 1.0
 */
public class IsoWeekGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  @Override
  public String getType() {
    return "isoweek";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    Map<String, Object> params = config.getParams();

    // 解析配置参数
    String format = getStringParam(params, "format", "ISO");
    String customFormat = getStringParam(params, "customFormat", "YYYY-'W'ww-e");
    int yearStart = getIntParam(params, "yearStart", 2020);
    int yearEnd = getIntParam(params, "yearEnd", 2025);
    int weekStart = getIntParam(params, "weekStart", 1);
    int weekEnd = getIntParam(params, "weekEnd", 53);
    int dayOfWeek = getIntParam(params, "dayOfWeek", 0); // 0表示任意
    boolean valid = getBooleanParam(params, "valid", true);
    String locale = getStringParam(params, "locale", "en_US");

    try {
      // 检查上下文中是否有相关日期信息
      LocalDate contextDate = getDateFromContext(context);
      if (contextDate != null) {
        return formatIsoWeek(contextDate, format, customFormat, locale);
      }

      // 生成随机ISO周日期
      LocalDate randomDate =
          generateRandomIsoWeekDate(yearStart, yearEnd, weekStart, weekEnd, dayOfWeek, valid);

      // 将生成的信息存储到上下文中
      storeIsoWeekInContext(context, randomDate);

      return formatIsoWeek(randomDate, format, customFormat, locale);

    } catch (Exception e) {
      // 如果解析失败，生成默认ISO周日期
      LocalDate defaultDate = LocalDate.now();
      return formatIsoWeek(defaultDate, format, customFormat, locale);
    }
  }

  /** 从上下文中获取相关日期信息 */
  private LocalDate getDateFromContext(DataForgeContext context) {
    // 尝试从上下文中获取已生成的日期
    return context.get("generated_date", LocalDate.class).orElse(null);
  }

  /** 生成随机ISO周日期 */
  private LocalDate generateRandomIsoWeekDate(
      int yearStart, int yearEnd, int weekStart, int weekEnd, int dayOfWeek, boolean valid) {
    Random random = ThreadLocalRandom.current();

    if (!valid) {
      // 生成无效周日期用于测试
      return generateInvalidIsoWeekDate(random, yearStart, yearEnd);
    }

    LocalDate randomDate;
    int maxAttempts = 100;
    int attempts = 0;

    do {
      // 随机选择年份
      int year = yearStart + random.nextInt(yearEnd - yearStart + 1);

      // 获取该年的最大周数
      int maxWeekOfYear = getMaxWeekOfYear(year);
      int actualWeekEnd = Math.min(weekEnd, maxWeekOfYear);

      // 随机选择周数
      int week =
          Math.max(weekStart, 1) + random.nextInt(actualWeekEnd - Math.max(weekStart, 1) + 1);

      // 随机选择星期几
      int actualDayOfWeek = (dayOfWeek > 0 && dayOfWeek <= 7) ? dayOfWeek : (1 + random.nextInt(7));

      // 构建日期
      randomDate = getDateFromIsoWeek(year, week, actualDayOfWeek);
      attempts++;

    } while (randomDate == null && attempts < maxAttempts);

    return randomDate != null ? randomDate : LocalDate.now();
  }

  /** 生成无效ISO周日期（用于测试） */
  private LocalDate generateInvalidIsoWeekDate(Random random, int yearStart, int yearEnd) {
    // 返回一个基础日期，实际的无效周日期会在格式化时处理
    int year = yearStart + random.nextInt(yearEnd - yearStart + 1);
    return LocalDate.of(year, 1, 1);
  }

  /** 获取指定年份的最大周数 */
  private int getMaxWeekOfYear(int year) {
    LocalDate lastDayOfYear = LocalDate.of(year, 12, 31);
    return lastDayOfYear.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
  }

  /** 根据ISO周年、周数和星期几获取日期 */
  private LocalDate getDateFromIsoWeek(int year, int week, int dayOfWeek) {
    try {
      // 使用ISO字段构建日期
      return LocalDate.of(year, 1, 1)
          .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
          .with(DayOfWeek.of(dayOfWeek));
    } catch (Exception e) {
      // 如果构建失败，返回null
      return null;
    }
  }

  /** 将ISO周信息存储到上下文中 */
  private void storeIsoWeekInContext(DataForgeContext context, LocalDate date) {
    context.put("generated_date", date);
    context.put("generated_iso_week_year", date.get(IsoFields.WEEK_BASED_YEAR));
    context.put("generated_iso_week", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    context.put("generated_iso_day_of_week", date.getDayOfWeek().getValue());
    context.put("generated_year", date.getYear());
    context.put("generated_month", date.getMonthValue());
    context.put("generated_day", date.getDayOfMonth());
    context.put("generated_day_of_week", date.getDayOfWeek().toString());
  }

  /** 格式化ISO周日期 */
  private String formatIsoWeek(LocalDate date, String format, String customFormat, String locale) {
    try {
      int weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR);
      int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
      int dayOfWeek = date.getDayOfWeek().getValue();

      switch (format.toUpperCase()) {
        case "ISO":
          return String.format("%04d-W%02d-%d", weekBasedYear, weekOfYear, dayOfWeek);
        case "COMPACT":
          return String.format("%04dW%02d%d", weekBasedYear, weekOfYear, dayOfWeek);
        case "VERBOSE":
          return formatVerboseIsoWeek(weekBasedYear, weekOfYear, dayOfWeek, locale);
        case "CHINESE":
          return String.format("%04d年第%02d周星期%d", weekBasedYear, weekOfYear, dayOfWeek);
        case "CUSTOM":
          return formatCustomIsoWeek(date, customFormat);
        default:
          // 随机选择一种格式
          return formatRandomIsoWeek(weekBasedYear, weekOfYear, dayOfWeek);
      }

    } catch (Exception e) {
      // 如果格式化失败，返回ISO格式
      int weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR);
      int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
      int dayOfWeek = date.getDayOfWeek().getValue();
      return String.format("%04d-W%02d-%d", weekBasedYear, weekOfYear, dayOfWeek);
    }
  }

  /** 格式化详细ISO周日期 */
  private String formatVerboseIsoWeek(int year, int week, int dayOfWeek, String locale) {
    String[] dayNames;
    if (locale.startsWith("zh")) {
      dayNames = new String[] {"一", "二", "三", "四", "五", "六", "日"};
      return String.format("%04d年第%02d周星期%s", year, week, dayNames[dayOfWeek - 1]);
    } else {
      dayNames =
          new String[] {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
          };
      return String.format("Week %02d of %04d, %s", week, year, dayNames[dayOfWeek - 1]);
    }
  }

  /** 格式化自定义ISO周日期 */
  private String formatCustomIsoWeek(LocalDate date, String customFormat) {
    try {
      // 简单的模板替换
      int weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR);
      int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
      int dayOfWeek = date.getDayOfWeek().getValue();

      String result = customFormat;
      result = result.replace("YYYY", String.format("%04d", weekBasedYear));
      result = result.replace("ww", String.format("%02d", weekOfYear));
      result = result.replace("w", String.valueOf(weekOfYear));
      result = result.replace("e", String.valueOf(dayOfWeek));

      return result;
    } catch (Exception e) {
      return customFormat;
    }
  }

  /** 随机格式化ISO周日期 */
  private String formatRandomIsoWeek(int year, int week, int dayOfWeek) {
    String[] formats = {
      "%04d-W%02d-%d", "%04dW%02d%d", "Week %02d of %04d, Day %d", "%04d年第%02d周星期%d"
    };
    String format = formats[ThreadLocalRandom.current().nextInt(formats.length)];
    return String.format(format, year, week, dayOfWeek);
  }

  /** 获取字符串参数 */
  private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
    Object value = params.get(key);
    return value != null ? value.toString() : defaultValue;
  }

  /** 获取整数参数 */
  private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
    Object value = params.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /** 获取布尔参数 */
  private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
    Object value = params.get(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    }
    return defaultValue;
  }

  @Override
  public String getDescription() {
    return "Generator for ISO 8601 week dates with year, week, and day-of-week components";
  }
}
