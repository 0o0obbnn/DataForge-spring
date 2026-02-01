package com.dataforge.io;

import com.dataforge.config.OutputConfig;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SQL INSERT输出策略实现。
 *
 * <p>将生成的数据转换为SQL INSERT语句输出到文件或标准输出。 支持自定义表名、字符编码等配置。 采用流式写入，支持大数据量输出而不会导致内存溢出。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class SqlOutputStrategy implements OutputStrategy {

  private static final Logger logger = LoggerFactory.getLogger(SqlOutputStrategy.class);

  /** 输出配置。 */
  private OutputConfig config;

  /** 字段名称列表。 */
  private List<String> fieldNames;

  /** 输出写入器。 */
  private PrintWriter writer;

  /** 已输出的记录数量。 */
  private long recordCount = 0;

  /** SQL表名。 */
  private String tableName;

  /** 日期时间格式化器。 */
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private static final DateTimeFormatter DATETIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  @Override
  public OutputConfig.Format getSupportedFormat() {
    return OutputConfig.Format.SQL;
  }

  @Override
  public void initialize(OutputConfig config, List<String> fieldNames) throws OutputException {
    if (config == null) {
      throw new OutputException("Output configuration cannot be null");
    }

    if (fieldNames == null || fieldNames.isEmpty()) {
      throw new OutputException("Field names cannot be null or empty for SQL output");
    }

    this.config = config;
    this.fieldNames = fieldNames;
    this.recordCount = 0;
    this.tableName = config.getSqlTableName();

    if (tableName == null || tableName.trim().isEmpty()) {
      tableName = "test_data";
    }

    try {
      // 初始化输出写入器
      initializeWriter();

      // 写入SQL注释和表结构信息
      writeSqlHeader();

      logger.info(
          "SQL output strategy initialized. Output: {}, Table: {}, Fields: {}",
          config.isFileOutput() ? config.getFile() : "STDOUT",
          tableName,
          fieldNames.size());

    } catch (Exception e) {
      throw new OutputException("Failed to initialize SQL output strategy", e);
    }
  }

  @Override
  public void writeRecord(Map<String, Object> record) throws OutputException {
    if (record == null) {
      throw new OutputException("Record cannot be null");
    }

    if (writer == null) {
      throw new OutputException("SQL output strategy not initialized");
    }

    try {
      // 构建INSERT语句
      StringBuilder sql = new StringBuilder();
      sql.append("INSERT INTO ").append(escapeIdentifier(tableName)).append(" (");

      // 添加列名
      StringJoiner columnJoiner = new StringJoiner(", ");
      for (String fieldName : fieldNames) {
        columnJoiner.add(escapeIdentifier(fieldName));
      }
      sql.append(columnJoiner.toString());

      sql.append(") VALUES (");

      // 添加值
      StringJoiner valueJoiner = new StringJoiner(", ");
      for (String fieldName : fieldNames) {
        Object value = record.get(fieldName);
        String sqlValue = formatSqlValue(value);
        valueJoiner.add(sqlValue);
      }
      sql.append(valueJoiner.toString());

      sql.append(");");

      // 写入SQL语句
      writer.println(sql.toString());
      recordCount++;

      // 定期刷新缓冲区（每1000条记录）
      if (recordCount % 1000 == 0) {
        writer.flush();
        logger.debug("Written {} SQL INSERT statements", recordCount);
      }

    } catch (Exception e) {
      throw new OutputException("Failed to write SQL INSERT statement", e);
    }
  }

  @Override
  public void writeRecords(List<Map<String, Object>> records) throws OutputException {
    if (records == null || records.isEmpty()) {
      return;
    }

    logger.debug("Writing {} SQL INSERT statements in batch", records.size());

    // 可以优化为批量INSERT语句
    if (records.size() > 1) {
      writeBatchInsert(records);
    } else {
      writeRecord(records.get(0));
    }

    // 批量写入后刷新
    flush();
  }

  @Override
  public void finish() throws OutputException {
    try {
      if (writer != null) {
        // 写入SQL尾部注释
        writeSqlFooter();

        writer.flush();

        // 如果是文件输出，关闭写入器
        if (config.isFileOutput()) {
          writer.close();
        }

        logger.info("SQL output completed. Total INSERT statements: {}", recordCount);
      }

    } catch (Exception e) {
      throw new OutputException("Failed to finish SQL output", e);
    } finally {
      writer = null;
    }
  }

  @Override
  public void flush() throws OutputException {
    if (writer != null) {
      writer.flush();
    }
  }

  @Override
  public long getWrittenRecordCount() {
    return recordCount;
  }

  /**
   * 初始化输出写入器。
   *
   * @throws IOException 当初始化失败时
   */
  private void initializeWriter() throws IOException {
    if (config.isFileOutput()) {
      // 文件输出
      String filePath = config.getFile();
      Charset charset = Charset.forName(config.getEncoding());

      // 创建父目录（如果不存在）
      java.io.File file = new java.io.File(filePath);
      java.io.File parentDir = file.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        if (!parentDir.mkdirs()) {
          throw new IOException(
              "Failed to create parent directories: " + parentDir.getAbsolutePath());
        }
      }

      // 创建文件输出流
      FileOutputStream fos = new FileOutputStream(file, config.isAppend());
      OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
      BufferedWriter bw = new BufferedWriter(osw, 8192); // 8KB缓冲区
      writer = new PrintWriter(bw);

      logger.debug(
          "Initialized SQL file writer: {}, encoding: {}, append: {}",
          filePath,
          config.getEncoding(),
          config.isAppend());
    } else {
      // 标准输出
      writer = new PrintWriter(System.out);
      logger.debug("Initialized SQL console writer");
    }
  }

  /** 写入SQL文件头部注释。 */
  private void writeSqlHeader() {
    writer.println("-- ========================================");
    writer.println("-- DataForge Generated SQL INSERT Statements");
    writer.println("-- Generated at: " + java.time.LocalDateTime.now().format(DATETIME_FORMATTER));
    writer.println("-- Table: " + tableName);
    writer.println("-- Fields: " + String.join(", ", fieldNames));
    writer.println("-- ========================================");
    writer.println();
  }

  /** 写入SQL文件尾部注释。 */
  private void writeSqlFooter() {
    writer.println();
    writer.println("-- ========================================");
    writer.println("-- Total records inserted: " + recordCount);
    writer.println(
        "-- Generation completed at: " + java.time.LocalDateTime.now().format(DATETIME_FORMATTER));
    writer.println("-- ========================================");
  }

  /**
   * 写入批量INSERT语句。
   *
   * @param records 记录列表
   */
  private void writeBatchInsert(List<Map<String, Object>> records) {
    if (records.isEmpty()) {
      return;
    }

    // 构建批量INSERT语句
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO ").append(escapeIdentifier(tableName)).append(" (");

    // 添加列名
    StringJoiner columnJoiner = new StringJoiner(", ");
    for (String fieldName : fieldNames) {
      columnJoiner.add(escapeIdentifier(fieldName));
    }
    sql.append(columnJoiner.toString());
    sql.append(") VALUES");

    // 添加多行值
    for (int i = 0; i < records.size(); i++) {
      if (i > 0) {
        sql.append(",");
      }
      sql.append("\n    (");

      Map<String, Object> record = records.get(i);
      StringJoiner valueJoiner = new StringJoiner(", ");
      for (String fieldName : fieldNames) {
        Object value = record.get(fieldName);
        String sqlValue = formatSqlValue(value);
        valueJoiner.add(sqlValue);
      }
      sql.append(valueJoiner.toString());
      sql.append(")");
    }

    sql.append(";");

    // 写入批量INSERT语句
    writer.println(sql.toString());
    recordCount += records.size();
  }

  /**
   * 转义SQL标识符（表名、列名）。
   *
   * @param identifier 标识符
   * @return 转义后的标识符
   */
  private String escapeIdentifier(String identifier) {
    if (identifier == null || identifier.isEmpty()) {
      return identifier;
    }

    // 简单的标识符转义，使用反引号
    return "`" + identifier.replace("`", "``") + "`";
  }

  /**
   * 格式化SQL值。
   *
   * <p>处理null值、字符串转义、数值、日期时间等类型。
   *
   * @param value 原始值
   * @return 格式化后的SQL值
   */
  private String formatSqlValue(Object value) {
    if (value == null) {
      return "NULL";
    }

    if (value instanceof String) {
      // 字符串需要转义单引号
      String str = (String) value;
      str = str.replace("'", "''"); // 转义单引号
      str = str.replace("\\", "\\\\"); // 转义反斜杠
      return "'" + str + "'";
    }

    if (value instanceof Number) {
      // 数值类型直接返回
      return value.toString();
    }

    if (value instanceof Boolean) {
      // 布尔值转换为数值
      return ((Boolean) value) ? "1" : "0";
    }

    if (value instanceof LocalDate) {
      // 日期格式化
      return "'" + ((LocalDate) value).format(DATE_FORMATTER) + "'";
    }

    if (value instanceof LocalDateTime) {
      // 日期时间格式化
      return "'" + ((LocalDateTime) value).format(DATETIME_FORMATTER) + "'";
    }

    if (value instanceof LocalTime) {
      // 时间格式化
      return "'" + ((LocalTime) value).format(TIME_FORMATTER) + "'";
    }

    if (value instanceof java.util.Date) {
      // java.util.Date转换
      java.util.Date date = (java.util.Date) value;
      LocalDateTime ldt =
          LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
      return "'" + ldt.format(DATETIME_FORMATTER) + "'";
    }

    if (value instanceof java.sql.Date) {
      // SQL日期
      return "'" + value.toString() + "'";
    }

    if (value instanceof java.sql.Timestamp) {
      // SQL时间戳
      return "'" + value.toString() + "'";
    }

    if (value instanceof java.sql.Time) {
      // SQL时间
      return "'" + value.toString() + "'";
    }

    // 其他类型转换为字符串并转义
    String str = value.toString();
    str = str.replace("'", "''");
    str = str.replace("\\", "\\\\");
    return "'" + str + "'";
  }

  @Override
  public String getDescription() {
    return "SQL output strategy - exports data as SQL INSERT statements";
  }
}
