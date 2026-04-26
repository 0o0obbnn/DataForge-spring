package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cron表达式生成器
 *
 * <p>支持功能： - 标准Cron表达式（5字段和6字段） - 多种复杂度级别（简单、中等、复杂） - 常见时间模式（每分钟、每小时、每天、每周、每月） -
 * 特殊字符支持（*、?、-、,、/、L、W、#） - 业务场景模板（工作时间、维护窗口、报表生成等） - 表达式验证和解释
 *
 * <p>配置参数： - type: 表达式类型（SIMPLE、COMPLEX、BUSINESS、CUSTOM） - fields: 字段数量（5或6，是否包含秒字段） - pattern:
 * 时间模式（MINUTELY、HOURLY、DAILY、WEEKLY、MONTHLY、YEARLY） - businessHours: 是否限制在工作时间 - includeSeconds:
 * 是否包含秒字段 - complexity: 复杂度级别（LOW、MEDIUM、HIGH） - valid: 是否生成有效表达式
 *
 * @author DataForge
 * @version 1.0
 */
public class CronExpressionGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  // Cron字段索引
  private static final int SECOND = 0;
  private static final int MINUTE = 1;
  private static final int HOUR = 2;
  private static final int DAY_OF_MONTH = 3;
  private static final int MONTH = 4;
  private static final int DAY_OF_WEEK = 5;

  // 常见的Cron模式模板
  private static final String[] SIMPLE_PATTERNS = {
    "0 * * * *", // 每小时
    "0 0 * * *", // 每天午夜
    "0 0 * * 0", // 每周日午夜
    "0 0 1 * *", // 每月1号午夜
    "*/5 * * * *", // 每5分钟
    "0 */2 * * *", // 每2小时
    "0 0 */2 * *", // 每2天
    "0 0 0 1 */3", // 每季度
  };

  private static final String[] BUSINESS_PATTERNS = {
    "0 9 * * 1-5", // 工作日上午9点
    "0 18 * * 1-5", // 工作日下午6点
    "0 0 2 * * 1-5", // 工作日凌晨2点（维护窗口）
    "0 30 8 * * 1-5", // 工作日上午8:30
    "0 0 12 * * 1-5", // 工作日中午12点
    "0 0 0 * * 6", // 每周六午夜
    "0 0 6 1 * *", // 每月1号早上6点
    "0 */15 9-17 * * 1-5", // 工作时间每15分钟
  };

  @Override
  public String getType() {
    return "cron";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {

    // 解析配置参数
    String type = getStringParam(config, "type", "SIMPLE");
    int fields = getIntParam(config, "fields", 5);
    String pattern = getStringParam(config, "pattern", "RANDOM");
    boolean businessHours = getBooleanParam(config, "businessHours", false);
    boolean includeSeconds = getBooleanParam(config, "includeSeconds", false);
    String complexity = getStringParam(config, "complexity", "MEDIUM");
    boolean valid = getBooleanParam(config, "valid", true);

    try {
      // 检查上下文中是否有相关时间信息
      String contextCron = getCronFromContext(context);
      if (contextCron != null) {
        return contextCron;
      }

      // 生成Cron表达式
      String cronExpression =
          generateCronExpression(
              type, fields, pattern, businessHours, includeSeconds, complexity, valid);

      // 将生成的表达式存储到上下文中
      storeCronInContext(context, cronExpression);

      return cronExpression;

    } catch (Exception e) {
      // 如果解析失败，生成默认Cron表达式
      return includeSeconds ? "0 0 * * * *" : "0 * * * *";
    }
  }

  /** 从上下文中获取相关Cron信息 */
  private String getCronFromContext(DataForgeContext context) {
    return context.get("generated_cron", String.class).orElse(null);
  }

  /** 生成Cron表达式 */
  private String generateCronExpression(
      String type,
      int fields,
      String pattern,
      boolean businessHours,
      boolean includeSeconds,
      String complexity,
      boolean valid) {
    if (!valid) {
      return generateInvalidCronExpression(includeSeconds);
    }

    switch (type.toUpperCase()) {
      case "SIMPLE":
        return generateSimpleCronExpression(includeSeconds);
      case "COMPLEX":
        return generateComplexCronExpression(includeSeconds, complexity);
      case "BUSINESS":
        return generateBusinessCronExpression(includeSeconds, businessHours);
      case "PATTERN":
        return generatePatternCronExpression(pattern, includeSeconds);
      case "CUSTOM":
      default:
        return generateRandomCronExpression(includeSeconds, complexity);
    }
  }

  /** 生成简单Cron表达式 */
  private String generateSimpleCronExpression(boolean includeSeconds) {
    Random random = ThreadLocalRandom.current();
    String pattern = SIMPLE_PATTERNS[random.nextInt(SIMPLE_PATTERNS.length)];

    if (includeSeconds) {
      return "0 " + pattern;
    }
    return pattern;
  }

  /** 生成复杂Cron表达式 */
  private String generateComplexCronExpression(boolean includeSeconds, String complexity) {
    Random random = ThreadLocalRandom.current();
    String[] fields = new String[includeSeconds ? 6 : 5];
    int startIndex = includeSeconds ? 0 : 1;

    for (int i = startIndex; i < fields.length; i++) {
      fields[i] = generateFieldValue(i, complexity, random);
    }

    // 确保DAY_OF_MONTH和DAY_OF_WEEK不同时指定具体值
    if (!fields[DAY_OF_MONTH].equals("*") && !fields[DAY_OF_WEEK].equals("*")) {
      if (random.nextBoolean()) {
        fields[DAY_OF_MONTH] = "*";
      } else {
        fields[DAY_OF_WEEK] = "?";
      }
    }

    StringBuilder sb = new StringBuilder();
    for (int i = startIndex; i < fields.length; i++) {
      if (i > startIndex) sb.append(" ");
      sb.append(fields[i]);
    }

    return sb.toString();
  }

  /** 生成业务场景Cron表达式 */
  private String generateBusinessCronExpression(boolean includeSeconds, boolean businessHours) {
    Random random = ThreadLocalRandom.current();
    String pattern = BUSINESS_PATTERNS[random.nextInt(BUSINESS_PATTERNS.length)];

    if (includeSeconds) {
      return "0 " + pattern;
    }
    return pattern;
  }

  /** 生成模式Cron表达式 */
  private String generatePatternCronExpression(String pattern, boolean includeSeconds) {
    Random random = ThreadLocalRandom.current();

    switch (pattern.toUpperCase()) {
      case "MINUTELY":
        return includeSeconds ? "0 * * * * *" : "* * * * *";
      case "HOURLY":
        int minute = random.nextInt(60);
        return includeSeconds
            ? String.format("0 %d * * * *", minute)
            : String.format("%d * * * *", minute);
      case "DAILY":
        int hour = random.nextInt(24);
        minute = random.nextInt(60);
        return includeSeconds
            ? String.format("0 %d %d * * *", minute, hour)
            : String.format("%d %d * * *", minute, hour);
      case "WEEKLY":
        hour = random.nextInt(24);
        minute = random.nextInt(60);
        int dayOfWeek = random.nextInt(7);
        return includeSeconds
            ? String.format("0 %d %d * * %d", minute, hour, dayOfWeek)
            : String.format("%d %d * * %d", minute, hour, dayOfWeek);
      case "MONTHLY":
        hour = random.nextInt(24);
        minute = random.nextInt(60);
        int dayOfMonth = 1 + random.nextInt(28); // 避免月末问题
        return includeSeconds
            ? String.format("0 %d %d %d * *", minute, hour, dayOfMonth)
            : String.format("%d %d %d * *", minute, hour, dayOfMonth);
      case "YEARLY":
        hour = random.nextInt(24);
        minute = random.nextInt(60);
        dayOfMonth = 1 + random.nextInt(28);
        int month = 1 + random.nextInt(12);
        return includeSeconds
            ? String.format("0 %d %d %d %d *", minute, hour, dayOfMonth, month)
            : String.format("%d %d %d %d *", minute, hour, dayOfMonth, month);
      default:
        return generateRandomCronExpression(includeSeconds, "MEDIUM");
    }
  }

  /** 生成随机Cron表达式 */
  private String generateRandomCronExpression(boolean includeSeconds, String complexity) {
    Random random = ThreadLocalRandom.current();

    // 随机选择一个基础模式，然后添加复杂性
    if (random.nextDouble() < 0.3) {
      return generateSimpleCronExpression(includeSeconds);
    } else if (random.nextDouble() < 0.6) {
      return generateBusinessCronExpression(includeSeconds, true);
    } else {
      return generateComplexCronExpression(includeSeconds, complexity);
    }
  }

  /** 生成字段值 */
  private String generateFieldValue(int fieldIndex, String complexity, Random random) {
    switch (fieldIndex) {
      case SECOND:
      case MINUTE:
        return generateTimeFieldValue(0, 59, complexity, random);
      case HOUR:
        return generateTimeFieldValue(0, 23, complexity, random);
      case DAY_OF_MONTH:
        return generateDayOfMonthValue(complexity, random);
      case MONTH:
        return generateMonthValue(complexity, random);
      case DAY_OF_WEEK:
        return generateDayOfWeekValue(complexity, random);
      default:
        return "*";
    }
  }

  /** 生成时间字段值（秒、分、时） */
  private String generateTimeFieldValue(int min, int max, String complexity, Random random) {
    double complexityFactor = getComplexityFactor(complexity);

    if (random.nextDouble() < 0.4) {
      return "*"; // 任意值
    } else if (random.nextDouble() < 0.3) {
      // 步长值
      int step = 2 + random.nextInt(Math.max(1, (int) (10 * complexityFactor)));
      return "*/" + step;
    } else if (random.nextDouble() < 0.2 && complexityFactor > 0.3) {
      // 范围值
      int start = min + random.nextInt((max - min) / 2);
      int end = start + 1 + random.nextInt((max - start) / 2);
      return start + "-" + end;
    } else if (random.nextDouble() < 0.1 && complexityFactor > 0.5) {
      // 列表值
      int count = 2 + random.nextInt(3);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < count; i++) {
        if (i > 0) sb.append(",");
        sb.append(min + random.nextInt(max - min + 1));
      }
      return sb.toString();
    } else {
      // 具体值
      return String.valueOf(min + random.nextInt(max - min + 1));
    }
  }

  /** 生成月份中的日期值 */
  private String generateDayOfMonthValue(String complexity, Random random) {
    double complexityFactor = getComplexityFactor(complexity);

    if (random.nextDouble() < 0.5) {
      return "*";
    } else if (random.nextDouble() < 0.2 && complexityFactor > 0.5) {
      return "L"; // 最后一天
    } else if (random.nextDouble() < 0.1 && complexityFactor > 0.7) {
      int day = 1 + random.nextInt(28);
      return day + "W"; // 最近的工作日
    } else {
      return String.valueOf(1 + random.nextInt(28));
    }
  }

  /** 生成月份值 */
  private String generateMonthValue(String complexity, Random random) {
    if (random.nextDouble() < 0.6) {
      return "*";
    } else if (random.nextDouble() < 0.3) {
      // 季度
      int quarter = random.nextInt(4);
      return String.valueOf(1 + quarter * 3);
    } else {
      return String.valueOf(1 + random.nextInt(12));
    }
  }

  /** 生成星期值 */
  private String generateDayOfWeekValue(String complexity, Random random) {
    double complexityFactor = getComplexityFactor(complexity);

    if (random.nextDouble() < 0.4) {
      return "?"; // 不指定
    } else if (random.nextDouble() < 0.3) {
      return "1-5"; // 工作日
    } else if (random.nextDouble() < 0.2) {
      return "6,0"; // 周末
    } else if (random.nextDouble() < 0.1 && complexityFactor > 0.7) {
      int dayOfWeek = random.nextInt(7);
      int week = 1 + random.nextInt(4);
      return dayOfWeek + "#" + week; // 第N个星期X
    } else {
      return String.valueOf(random.nextInt(7));
    }
  }

  /** 获取复杂度因子 */
  private double getComplexityFactor(String complexity) {
    switch (complexity.toUpperCase()) {
      case "LOW":
        return 0.2;
      case "MEDIUM":
        return 0.5;
      case "HIGH":
        return 0.8;
      default:
        return 0.5;
    }
  }

  /** 生成无效Cron表达式（用于测试） */
  private String generateInvalidCronExpression(boolean includeSeconds) {
    Random random = ThreadLocalRandom.current();

    // 生成一些常见的无效表达式
    String[] invalidPatterns = {
      "60 * * * *", // 无效的分钟值
      "* 25 * * *", // 无效的小时值
      "* * 32 * *", // 无效的日期值
      "* * * 13 *", // 无效的月份值
      "* * * * 8", // 无效的星期值
      "* * 1 * 1", // 同时指定日期和星期
      "*/0 * * * *", // 无效的步长
      "1-60 * * * *", // 无效的范围
    };

    String pattern = invalidPatterns[random.nextInt(invalidPatterns.length)];
    return includeSeconds ? "0 " + pattern : pattern;
  }

  /** 将Cron表达式信息存储到上下文中 */
  private void storeCronInContext(DataForgeContext context, String cronExpression) {
    context.put("generated_cron", cronExpression);
    context.put("generated_cron_fields", cronExpression.split(" ").length);

    // 解析并存储各个字段
    String[] fields = cronExpression.split(" ");
    if (fields.length >= 5) {
      int offset = fields.length == 6 ? 0 : 1;
      if (fields.length == 6) {
        context.put("generated_cron_second", fields[0]);
      }
      context.put("generated_cron_minute", fields[1 - offset]);
      context.put("generated_cron_hour", fields[2 - offset]);
      context.put("generated_cron_day_of_month", fields[3 - offset]);
      context.put("generated_cron_month", fields[4 - offset]);
      if (fields.length > 5 - offset) {
        context.put("generated_cron_day_of_week", fields[5 - offset]);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Generator for Cron expressions with multiple complexity levels and business patterns";
  }
}
