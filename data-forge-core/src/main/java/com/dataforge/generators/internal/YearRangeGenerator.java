package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 年份区间生成器
 *
 * <p>支持功能： - 多种年份区间格式（连字符、斜杠、TO格式等） - 区间长度控制 - 历史/未来年份区间 - 与教育、工作经历等字段关联 - 重叠区间生成 - 无效区间生成（用于测试）
 *
 * <p>配置参数： - format: 输出格式（HYPHEN、SLASH、TO、PARENTHESES、CUSTOM） - separator: 自定义分隔符 - minYear: 最小年份 -
 * maxYear: 最大年份 - minDuration: 最小区间长度（年） - maxDuration: 最大区间长度（年） - type:
 * 区间类型（PAST、CURRENT、FUTURE、MIXED） - valid: 是否生成有效区间 - allowOverlap: 是否允许重叠区间
 *
 * @author DataForge
 * @version 1.0
 */
public class YearRangeGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  @Override
  public String getType() {
    return "yearrange";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    // 解析配置参数
    String format = getStringParam(config, "format", "HYPHEN");
    String separator = getStringParam(config, "separator", "-");
    int minYear = getIntParam(config, "minYear", 1990);
    int maxYear = getIntParam(config, "maxYear", LocalDate.now().getYear());
    int minDuration = getIntParam(config, "minDuration", 1);
    int maxDuration = getIntParam(config, "maxDuration", 5);
    String type = getStringParam(config, "type", "MIXED");
    boolean valid = getBooleanParam(config, "valid", true);

    try {
      // 检查上下文中是否有相关年份信息
      YearRange contextRange = getYearRangeFromContext(context, type, minDuration, maxDuration);
      if (contextRange != null) {
        return formatYearRange(contextRange, format, separator);
      }

      // 生成随机年份区间
      YearRange randomRange =
          generateRandomYearRange(minYear, maxYear, minDuration, maxDuration, type, valid);

      // 将生成的区间存储到上下文中
      storeYearRangeInContext(context, randomRange);

      return formatYearRange(randomRange, format, separator);

    } catch (Exception e) {
      // 如果解析失败，生成默认年份区间
      int currentYear = LocalDate.now().getYear();
      YearRange defaultRange = new YearRange(currentYear - 2, currentYear);
      return formatYearRange(defaultRange, format, separator);
    }
  }

  /** 从上下文中获取相关年份信息 */
  private YearRange getYearRangeFromContext(
      DataForgeContext context, String type, int minDuration, int maxDuration) {
    // 尝试从上下文中获取已生成的年份区间
    YearRange existingRange = context.get("generated_year_range", YearRange.class).orElse(null);
    if (existingRange != null) {
      return existingRange;
    }

    // 尝试从年龄或出生年份推导
    Integer age = context.get("generated_age", Integer.class).orElse(null);
    Integer birthYear = context.get("generated_birth_year", Integer.class).orElse(null);

    if (age != null || birthYear != null) {
      int currentYear = LocalDate.now().getYear();
      int actualBirthYear =
          birthYear != null ? birthYear : (currentYear - (age != null ? age : 25));

      // 根据类型生成相关的年份区间
      return generateContextualYearRange(
          actualBirthYear, currentYear, type, minDuration, maxDuration);
    }

    return null;
  }

  /** 根据上下文生成相关的年份区间 */
  private YearRange generateContextualYearRange(
      int birthYear, int currentYear, String type, int minDuration, int maxDuration) {
    Random random = ThreadLocalRandom.current();
    int duration = minDuration + random.nextInt(maxDuration - minDuration + 1);

    switch (type.toUpperCase()) {
      case "EDUCATION":
        // 教育经历：通常在18-30岁之间
        int eduStartAge = 18 + random.nextInt(5);
        int eduStartYear = birthYear + eduStartAge;
        return new YearRange(eduStartYear, eduStartYear + duration);

      case "WORK":
        // 工作经历：通常在22岁之后
        int workStartAge = 22 + random.nextInt(8);
        int workStartYear = birthYear + workStartAge;
        return new YearRange(workStartYear, Math.min(workStartYear + duration, currentYear));

      case "PAST":
        // 过去的年份区间
        int pastEndYear = currentYear - 1;
        int pastStartYear = pastEndYear - duration;
        return new YearRange(pastStartYear, pastEndYear);

      default:
        return null;
    }
  }

  /** 生成随机年份区间 */
  private YearRange generateRandomYearRange(
      int minYear, int maxYear, int minDuration, int maxDuration, String type, boolean valid) {
    Random random = ThreadLocalRandom.current();
    int currentYear = LocalDate.now().getYear();

    if (!valid) {
      // 生成无效区间用于测试
      return generateInvalidYearRange(random, minYear, maxYear);
    }

    int duration = minDuration + random.nextInt(maxDuration - minDuration + 1);
    int startYear, endYear;

    switch (type.toUpperCase()) {
      case "PAST":
        endYear = Math.min(maxYear, currentYear - 1);
        startYear = Math.max(minYear, endYear - duration);
        break;

      case "CURRENT":
        startYear = Math.max(minYear, currentYear - duration / 2);
        endYear = Math.min(maxYear, currentYear + duration / 2);
        break;

      case "FUTURE":
        startYear = Math.max(minYear, currentYear + 1);
        endYear = Math.min(maxYear, startYear + duration);
        break;

      case "MIXED":
      default:
        // 确保区间在有效范围内
        int maxPossibleStart = maxYear - duration;
        startYear = minYear + random.nextInt(Math.max(1, maxPossibleStart - minYear + 1));
        endYear = startYear + duration;

        // 调整以确保在范围内
        if (endYear > maxYear) {
          endYear = maxYear;
          startYear = endYear - duration;
        }
        break;
    }

    return new YearRange(startYear, endYear);
  }

  /** 生成无效年份区间（用于测试） */
  private YearRange generateInvalidYearRange(Random random, int minYear, int maxYear) {
    // 生成开始年份大于结束年份的无效区间
    int year1 = minYear + random.nextInt(maxYear - minYear + 1);
    int year2 = minYear + random.nextInt(maxYear - minYear + 1);

    // 确保year1 > year2（无效）
    if (year1 <= year2) {
      int temp = year1;
      year1 = year2 + 1;
      year2 = temp;
    }

    return new YearRange(year1, year2);
  }

  /** 将年份区间信息存储到上下文中 */
  private void storeYearRangeInContext(DataForgeContext context, YearRange range) {
    context.put("generated_year_range", range);
    context.put("generated_start_year", range.startYear);
    context.put("generated_end_year", range.endYear);
    context.put("generated_year_duration", range.endYear - range.startYear);
    context.put("generated_year_span", range.endYear - range.startYear + 1);
  }

  /** 格式化年份区间 */
  private String formatYearRange(YearRange range, String format, String separator) {
    switch (format.toUpperCase()) {
      case "HYPHEN":
        return String.format("%d-%d", range.startYear, range.endYear);

      case "SLASH":
        return String.format("%d/%d", range.startYear, range.endYear);

      case "TO":
        return String.format("%d to %d", range.startYear, range.endYear);

      case "PARENTHESES":
        return String.format("(%d-%d)", range.startYear, range.endYear);

      case "VERBOSE":
        return String.format("From %d to %d", range.startYear, range.endYear);

      case "CHINESE":
        return String.format("%d年至%d年", range.startYear, range.endYear);

      case "CUSTOM":
        return String.format("%d%s%d", range.startYear, separator, range.endYear);

      default:
        // 随机选择一种格式
        String[] formats = {"%d-%d", "%d/%d", "%d to %d", "(%d-%d)"};
        String randomFormat = formats[ThreadLocalRandom.current().nextInt(formats.length)];
        return String.format(randomFormat, range.startYear, range.endYear);
    }
  }

  /** 年份区间内部类 */
  private static class YearRange {
    final int startYear;
    final int endYear;

    YearRange(int startYear, int endYear) {
      this.startYear = startYear;
      this.endYear = endYear;
    }
  }

  @Override
  public String getDescription() {
    return "Generator for year ranges with multiple formats and duration control";
  }
}
