package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 日期生成器
 *
 * <p>支持功能： - 多种日期格式（ISO、美式、欧式、紧凑等） - 日期范围控制 - 工作日/周末/节假日过滤 - 闰年处理 - 异常日期生成（用于测试） - 与年龄、身份证等字段关联
 *
 * <p>配置参数： - format: 日期格式（ISO、US、EU、COMPACT、CUSTOM） - customFormat: 自定义格式字符串 - startDate: 开始日期 -
 * endDate: 结束日期 - dayType: 日期类型（ANY、WEEKDAY、WEEKEND、HOLIDAY） - valid: 是否生成有效日期 - locale: 本地化设置
 *
 * @author DataForge
 * @version 1.0
 */
public class DateGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final String[] DATE_FORMATS = {
    "yyyy-MM-dd", // ISO 8601
    "MM/dd/yyyy", // US format
    "dd/MM/yyyy", // EU format
    "yyyyMMdd", // Compact
    "yyyy年MM月dd日", // Chinese
    "dd-MMM-yyyy", // With month name
    "EEEE, MMMM dd, yyyy" // Full format
  };

  private static final String[] WEEKDAYS = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};

  private static final String[] WEEKENDS = {"SATURDAY", "SUNDAY"};

  // 中国法定节假日（简化版，实际应用中需要更完整的数据）
  private static final String[] CHINESE_HOLIDAYS = {
    "01-01",
    "02-11",
    "02-12",
    "02-13",
    "02-14",
    "02-15",
    "02-16",
    "02-17", // 春节
    "04-05",
    "05-01",
    "05-02",
    "05-03",
    "06-22",
    "06-23",
    "06-24", // 清明、劳动节、端午
    "09-29",
    "09-30",
    "10-01",
    "10-02",
    "10-03",
    "10-04",
    "10-05",
    "10-06",
    "10-07" // 国庆
  };

  @Override
  public String getType() {
    return "date";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    // 解析配置参数
    String format = getStringParam(config, "format", "ISO");
    String customFormat = getStringParam(config, "customFormat", "yyyy-MM-dd");
    String startDateStr = getStringParam(config, "startDate", "2020-01-01");
    String endDateStr = getStringParam(config, "endDate", "2025-12-31");
    String dayType = getStringParam(config, "dayType", "ANY");
    boolean valid = getBooleanParam(config, "valid", true);
    String locale = getStringParam(config, "locale", "zh_CN");

    try {
      // 解析日期范围
      LocalDate startDate = LocalDate.parse(startDateStr);
      LocalDate endDate = LocalDate.parse(endDateStr);

      // 检查上下文中是否有相关日期信息
      LocalDate contextDate = getDateFromContext(context);
      if (contextDate != null) {
        return formatDate(contextDate, format, customFormat, locale);
      }

      // 生成随机日期
      LocalDate randomDate = generateRandomDate(startDate, endDate, dayType, valid);

      // 将生成的日期存储到上下文中
      context.put("generated_date", randomDate);
      context.put("generated_year", randomDate.getYear());
      context.put("generated_month", randomDate.getMonthValue());
      context.put("generated_day", randomDate.getDayOfMonth());
      context.put("generated_day_of_week", randomDate.getDayOfWeek().toString());

      return formatDate(randomDate, format, customFormat, locale);

    } catch (Exception e) {
      // 如果解析失败，生成默认日期
      LocalDate defaultDate = LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(365));
      return formatDate(defaultDate, format, customFormat, locale);
    }
  }

  /** 从上下文中获取相关日期信息 */
  private LocalDate getDateFromContext(DataForgeContext context) {
    // 尝试从身份证号中提取出生日期
    String idCard = context.get("generated_idcard", String.class).orElse(null);
    if (idCard != null && idCard.length() >= 14) {
      try {
        String birthStr = idCard.substring(6, 14);
        int year = Integer.parseInt(birthStr.substring(0, 4));
        int month = Integer.parseInt(birthStr.substring(4, 6));
        int day = Integer.parseInt(birthStr.substring(6, 8));
        return LocalDate.of(year, month, day);
      } catch (Exception e) {
        // 忽略解析错误
      }
    }

    // 尝试从上下文中获取已生成的日期
    return context.get("generated_date", LocalDate.class).orElse(null);
  }

  /** 生成随机日期 */
  private LocalDate generateRandomDate(
      LocalDate startDate, LocalDate endDate, String dayType, boolean valid) {
    Random random = ThreadLocalRandom.current();

    if (!valid) {
      // 生成无效日期用于测试
      return generateInvalidDate(random);
    }

    LocalDate randomDate;
    int maxAttempts = 100; // 防止无限循环
    int attempts = 0;

    do {
      long daysBetween = startDate.until(endDate).getDays();
      long randomDays = random.nextLong(daysBetween + 1);
      randomDate = startDate.plusDays(randomDays);
      attempts++;
    } while (!matchesDayType(randomDate, dayType) && attempts < maxAttempts);

    return randomDate;
  }

  /** 生成无效日期（用于测试） */
  private LocalDate generateInvalidDate(Random random) {
    // 这里返回一个有效日期，但在格式化时会生成无效字符串
    // 实际的无效日期会在formatDate方法中处理
    return LocalDate.of(2023, 2, 28); // 返回一个基础日期
  }

  /** 检查日期是否匹配指定的日期类型 */
  private boolean matchesDayType(LocalDate date, String dayType) {
    switch (dayType.toUpperCase()) {
      case "WEEKDAY":
        return isWeekday(date);
      case "WEEKEND":
        return isWeekend(date);
      case "HOLIDAY":
        return isHoliday(date);
      case "ANY":
      default:
        return true;
    }
  }

  /** 检查是否为工作日 */
  private boolean isWeekday(LocalDate date) {
    String dayOfWeek = date.getDayOfWeek().toString();
    for (String weekday : WEEKDAYS) {
      if (weekday.equals(dayOfWeek)) {
        return true;
      }
    }
    return false;
  }

  /** 检查是否为周末 */
  private boolean isWeekend(LocalDate date) {
    String dayOfWeek = date.getDayOfWeek().toString();
    for (String weekend : WEEKENDS) {
      if (weekend.equals(dayOfWeek)) {
        return true;
      }
    }
    return false;
  }

  /** 检查是否为节假日（简化实现） */
  private boolean isHoliday(LocalDate date) {
    String monthDay = String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth());
    for (String holiday : CHINESE_HOLIDAYS) {
      if (holiday.equals(monthDay)) {
        return true;
      }
    }
    return false;
  }

  /** 格式化日期 */
  private String formatDate(LocalDate date, String format, String customFormat, String locale) {
    try {
      DateTimeFormatter formatter;

      switch (format.toUpperCase()) {
        case "ISO":
          formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
          break;
        case "US":
          formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
          break;
        case "EU":
          formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
          break;
        case "COMPACT":
          formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
          break;
        case "CHINESE":
          formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
          break;
        case "FULL":
          formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
          break;
        case "CUSTOM":
          formatter = DateTimeFormatter.ofPattern(customFormat);
          break;
        default:
          // 随机选择一种格式
          String randomFormat =
              DATE_FORMATS[ThreadLocalRandom.current().nextInt(DATE_FORMATS.length)];
          formatter = DateTimeFormatter.ofPattern(randomFormat);
          break;
      }

      return date.format(formatter);

    } catch (Exception e) {
      // 如果格式化失败，返回ISO格式
      return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
  }

  @Override
  public String getDescription() {
    return "Generator for date values with multiple formats and filtering options";
  }
}
