package com.dataforge.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 输出配置类。
 *
 * <p>定义数据输出的格式、目标和相关参数。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class OutputConfig {

  /** 输出格式枚举。 */
  public enum Format {
    /** 控制台输出 */
    CONSOLE,
    /** CSV格式 */
    CSV,
    /** JSON格式 */
    JSON,
    /** XML格式 */
    XML,
    /** TSV格式 */
    TSV,
    /** SQL INSERT语句 */
    SQL;

    @JsonCreator
    public static Format fromString(final String value) {
      if (value == null) {
        return null;
      }
      try {
        return Format.valueOf(value.toUpperCase(java.util.Locale.ENGLISH));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid format: "
                + value
                + ". Valid values are: "
                + java.util.Arrays.toString(Format.values()),
            e);
      }
    }

    @JsonValue
    public String toValue() {
      return this.name().toLowerCase(java.util.Locale.ENGLISH);
    }
  }

  /** 输出格式，默认为CONSOLE。 */
  private Format format = Format.CONSOLE;

  /** 输出文件路径，当format不为CONSOLE时使用。 */
  private String file;

  /** 是否追加到文件末尾，默认为false（覆盖）。 */
  private boolean append = false;

  /** 字符编码，默认为UTF-8。 */
  private String encoding = "UTF-8";

  /** CSV分隔符，默认为逗号。 */
  private String csvDelimiter = ",";

  /** CSV是否包含标题行，默认为true。 */
  private boolean csvIncludeHeader = true;

  /** CSV安全模式，防止公式注入，默认为false。 */
  private boolean csvSafeMode = false;

  /** JSON是否格式化输出，默认为true。 */
  private boolean jsonPrettyPrint = true;

  /** SQL表名，当format为SQL时使用。 */
  private String sqlTableName = "test_data";

  /** MIME类型，用于HTTP响应头。 */
  private String mimeType;

  /**
   * 获取输出格式。
   *
   * @return 输出格式
   */
  public Format getFormat() {
    return format;
  }

  /**
   * 设置输出格式。
   *
   * @param format 输出格式
   */
  public void setFormat(final Format format) {
    this.format = format;
  }

  /**
   * 获取输出文件路径。
   *
   * @return 文件路径
   */
  public String getFile() {
    return file;
  }

  /**
   * 设置输出文件路径。
   *
   * @param file 文件路径
   */
  public void setFile(final String file) {
    this.file = file;
  }

  /**
   * 检查是否追加到文件末尾。
   *
   * @return 如果追加返回true，否则返回false
   */
  public boolean isAppend() {
    return append;
  }

  /**
   * 设置是否追加到文件末尾。
   *
   * @param append 是否追加
   */
  public void setAppend(final boolean append) {
    this.append = append;
  }

  /**
   * 获取字符编码。
   *
   * @return 字符编码
   */
  public String getEncoding() {
    return encoding;
  }

  /**
   * 设置字符编码。
   *
   * @param encoding 字符编码
   */
  public void setEncoding(final String encoding) {
    this.encoding = encoding;
  }

  /**
   * 获取CSV分隔符。
   *
   * @return CSV分隔符
   */
  public String getCsvDelimiter() {
    return csvDelimiter;
  }

  /**
   * 设置CSV分隔符。
   *
   * @param csvDelimiter CSV分隔符
   */
  public void setCsvDelimiter(final String csvDelimiter) {
    this.csvDelimiter = csvDelimiter;
  }

  /**
   * 检查CSV是否包含标题行。
   *
   * @return 如果包含标题行返回true，否则返回false
   */
  public boolean isCsvIncludeHeader() {
    return csvIncludeHeader;
  }

  /**
   * 设置CSV是否包含标题行。
   *
   * @param csvIncludeHeader 是否包含标题行
   */
  public void setCsvIncludeHeader(final boolean csvIncludeHeader) {
    this.csvIncludeHeader = csvIncludeHeader;
  }

  /**
   * 检查CSV是否启用安全模式。
   *
   * @return 如果启用安全模式返回true，否则返回false
   */
  public boolean isCsvSafeMode() {
    return csvSafeMode;
  }

  /**
   * 设置CSV是否启用安全模式。
   *
   * @param csvSafeMode 是否启用安全模式
   */
  public void setCsvSafeMode(final boolean csvSafeMode) {
    this.csvSafeMode = csvSafeMode;
  }

  /**
   * 检查JSON是否格式化输出。
   *
   * @return 如果格式化输出返回true，否则返回false
   */
  public boolean isJsonPrettyPrint() {
    return jsonPrettyPrint;
  }

  /**
   * 设置JSON是否格式化输出。
   *
   * @param jsonPrettyPrint 是否格式化输出
   */
  public void setJsonPrettyPrint(final boolean jsonPrettyPrint) {
    this.jsonPrettyPrint = jsonPrettyPrint;
  }

  /**
   * 获取SQL表名。
   *
   * @return SQL表名
   */
  public String getSqlTableName() {
    return sqlTableName;
  }

  /**
   * 设置SQL表名。
   *
   * @param sqlTableName SQL表名
   */
  public void setSqlTableName(final String sqlTableName) {
    this.sqlTableName = sqlTableName;
  }

  /**
   * 获取MIME类型。
   *
   * @return MIME类型
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * 设置MIME类型。
   *
   * @param mimeType MIME类型
   */
  public void setMimeType(final String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * 获取输出文件路径（兼容旧的getPath调用）。
   *
   * @return 文件路径
   */
  public String getPath() {
    return getFile();
  }

  /**
   * 检查是否为文件输出模式。
   *
   * @return 如果是文件输出则返回true，否则返回false
   */
  public boolean isFileOutput() {
    return format != Format.CONSOLE && file != null && !file.trim().isEmpty();
  }

  @Override
  public String toString() {
    return String.format(
        "OutputConfig{format=%s, file='%s', append=%s, encoding='%s'}",
        format, file, append, encoding);
  }
}
