package com.dataforge.io;

import com.dataforge.config.OutputConfig;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 控制台输出策略实现。
 *
 * <p>将生成的数据以表格形式输出到控制台（标准输出）。 支持自动列宽调整和美观的表格格式。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class ConsoleOutputStrategy implements OutputStrategy {

  private static final Logger logger = LoggerFactory.getLogger(ConsoleOutputStrategy.class);

  /** 字段名称列表。 */
  private List<String> fieldNames;

  /** 已输出的记录数量。 */
  private long recordCount = 0;

  /** 是否已输出表头。 */
  private boolean headerPrinted = false;

  /** 列分隔符。 */
  private static final String COLUMN_SEPARATOR = " | ";

  /** 默认列宽。 */
  private static final int DEFAULT_COLUMN_WIDTH = 15;

  @Override
  public OutputConfig.Format getSupportedFormat() {
    return OutputConfig.Format.CONSOLE;
  }

  @Override
  public void initialize(OutputConfig config, List<String> fieldNames) throws OutputException {
    if (fieldNames == null || fieldNames.isEmpty()) {
      throw new OutputException("Field names cannot be null or empty for console output");
    }

    this.fieldNames = fieldNames;
    this.recordCount = 0;
    this.headerPrinted = false;

    logger.debug(
        "Console output strategy initialized with {} fields: {}", fieldNames.size(), fieldNames);
  }

  @Override
  public void writeRecord(Map<String, Object> record) throws OutputException {
    if (record == null) {
      throw new OutputException("Record cannot be null");
    }

    try {
      // 首次输出时打印表头
      if (!headerPrinted) {
        printHeader();
        printSeparator();
        headerPrinted = true;
      }

      // 输出数据行
      printDataRow(record);
      recordCount++;

    } catch (Exception e) {
      throw new OutputException("Failed to write record to console", e);
    }
  }

  @Override
  public void finish() throws OutputException {
    try {
      if (headerPrinted) {
        printSeparator();
        System.out.println("Total records: " + recordCount);
      } else {
        System.out.println("No records to display.");
      }

      // 刷新输出流
      System.out.flush();

      logger.info("Console output completed. Total records: {}", recordCount);

    } catch (Exception e) {
      throw new OutputException("Failed to finish console output", e);
    }
  }

  @Override
  public void flush() throws OutputException {
    System.out.flush();
  }

  @Override
  public long getWrittenRecordCount() {
    return recordCount;
  }

  /** 打印表头。 */
  private void printHeader() {
    StringJoiner joiner = new StringJoiner(COLUMN_SEPARATOR);
    for (String fieldName : fieldNames) {
      joiner.add(formatColumn(fieldName, DEFAULT_COLUMN_WIDTH));
    }
    System.out.println(joiner.toString());
  }

  /** 打印分隔线。 */
  private void printSeparator() {
    StringJoiner joiner = new StringJoiner(COLUMN_SEPARATOR);
    for (String fieldName : fieldNames) {
      joiner.add("-".repeat(Math.max(DEFAULT_COLUMN_WIDTH, fieldName.length())));
    }
    System.out.println(joiner.toString());
  }

  /**
   * 打印数据行。
   *
   * @param record 记录数据
   */
  private void printDataRow(Map<String, Object> record) {
    StringJoiner joiner = new StringJoiner(COLUMN_SEPARATOR);
    for (String fieldName : fieldNames) {
      Object value = record.get(fieldName);
      String displayValue = formatValue(value);
      joiner.add(formatColumn(displayValue, DEFAULT_COLUMN_WIDTH));
    }
    System.out.println(joiner.toString());
  }

  /**
   * 格式化列内容，确保固定宽度。
   *
   * @param content 列内容
   * @param width 列宽
   * @return 格式化后的列内容
   */
  private String formatColumn(String content, int width) {
    if (content == null) {
      content = "";
    }

    // 如果内容超过列宽，截断并添加省略号
    if (content.length() > width) {
      return content.substring(0, width - 3) + "...";
    }

    // 左对齐，右侧填充空格
    return String.format("%-" + width + "s", content);
  }

  /**
   * 格式化字段值为字符串。
   *
   * @param value 字段值
   * @return 格式化后的字符串
   */
  private String formatValue(Object value) {
    if (value == null) {
      return "null";
    }

    String str = value.toString();

    // 处理换行符，替换为空格
    str = str.replace("\n", " ").replace("\r", " ");

    // 处理制表符，替换为空格
    str = str.replace("\t", " ");

    return str.trim();
  }

  @Override
  public String getDescription() {
    return "Console output strategy - displays data in tabular format to standard output";
  }
}
