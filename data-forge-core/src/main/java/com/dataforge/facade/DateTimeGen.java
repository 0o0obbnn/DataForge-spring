package com.dataforge.facade;

import java.util.List;
import java.util.Map;

/**
 * 日期时间生成器门面
 *
 * <p>提供日期时间相关数据的生成方法
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class DateTimeGen {

  private final DataGen gen;

  public DateTimeGen(DataGen gen) {
    this.gen = gen;
  }

  /** 生成日期 */
  public String date() {
    return gen.generate("date");
  }

  /** 生成日期（指定格式） */
  public String date(String format) {
    return gen.generate("date", Map.of("format", "CUSTOM", "customFormat", format));
  }

  /** 生成时间 */
  public String time() {
    return gen.generate("time");
  }

  /** 生成日期时间 */
  public String dateTime() {
    return gen.generate("timestamp", Map.of("format", "yyyy-MM-dd HH:mm:ss"));
  }

  /** 生成时间戳（毫秒） */
  public String timestamp() {
    return gen.generate("timestamp");
  }

  /** 生成时间戳（秒） */
  public String timestampSeconds() {
    return gen.generate("timestamp", Map.of("unit", "SECONDS"));
  }

  /** 生成过去的日期 */
  public String pastDate() {
    return gen.generate("date", Map.of("type", "PAST"));
  }

  /** 生成过去的日期（指定天数） */
  public String pastDate(int days) {
    return gen.generate("date", Map.of("type", "PAST", "days", days));
  }

  /** 生成未来的日期 */
  public String futureDate() {
    return gen.generate("date", Map.of("type", "FUTURE"));
  }

  /** 生成未来的日期（指定天数） */
  public String futureDate(int days) {
    return gen.generate("date", Map.of("type", "FUTURE", "days", days));
  }

  /** 生成年份 */
  public Integer year() {
    return Integer.parseInt(gen.generate("random_number", Map.of("min", 1900, "max", 2100)));
  }

  /** 生成月份 */
  public Integer month() {
    return Integer.parseInt(gen.generate("random_number", Map.of("min", 1, "max", 12)));
  }

  /** 生成星期几 */
  public String weekday() {
    return gen.generate(
        "enum",
        Map.of(
            "values",
            List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")));
  }

  /** 批量生成日期 */
  public List<String> dates(int count) {
    return gen.generateList("date", count);
  }

  /** 批量生成时间戳 */
  public List<String> timestamps(int count) {
    return gen.generateList("timestamp", count);
  }
}
