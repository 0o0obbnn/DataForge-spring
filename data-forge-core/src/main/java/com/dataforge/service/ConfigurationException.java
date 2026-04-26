package com.dataforge.service;

/**
 * 配置异常类 - 增强版本。
 *
 * <p>当配置文件格式错误、参数无效或配置不完整时抛出此异常。 属于用户错误类型，通常可以通过修正配置文件来解决。
 *
 * <p><strong>增强特性：</strong>
 *
 * <ul>
 *   <li>支持配置路径定位：精确指出错误配置的位置
 *   <li>配置上下文信息：提供相关的配置片段
 *   <li>建议修复方案：给出可能的修复建议
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class ConfigurationException extends DataForgeException {

  /** 序列化版本号。 */
  private static final long serialVersionUID = 1L;

  /** 配置错误类型。 */
  public enum ConfigErrorType {
    /** 缺少必需的配置项 */
    MISSING_REQUIRED,
    /** 配置值格式错误 */
    INVALID_FORMAT,
    /** 配置值超出范围 */
    OUT_OF_RANGE,
    /** 配置冲突 */
    CONFLICT,
    /** 不支持的配置选项 */
    UNSUPPORTED,
    /** 其他配置错误 */
    OTHER
  }

  /** 配置错误相关的字段名称。 */
  private final String fieldName;

  /** 配置路径（用于嵌套配置定位）。 */
  private final String configPath;

  /** 配置错误类型。 */
  private final ConfigErrorType errorType;

  /**
   * 构造函数。
   *
   * @param message 异常消息
   */
  public ConfigurationException(String message) {
    super("CONFIG_ERROR", message, ErrorLevel.USER_ERROR);
    this.fieldName = null;
    this.configPath = null;
    this.errorType = ConfigErrorType.OTHER;
  }

  /**
   * 构造函数。
   *
   * @param message 异常消息
   * @param cause 原因异常
   */
  public ConfigurationException(String message, Throwable cause) {
    super("CONFIG_ERROR", message, ErrorLevel.USER_ERROR, cause);
    this.fieldName = null;
    this.configPath = null;
    this.errorType = ConfigErrorType.OTHER;
  }

  /**
   * 构造函数，用于字段配置错误。
   *
   * @param fieldName 字段名称
   * @param message 错误消息
   */
  public ConfigurationException(String fieldName, String message) {
    super(
        "CONFIG_FIELD_ERROR",
        String.format("字段 '%s' 配置错误: %s", fieldName, message),
        ErrorLevel.USER_ERROR);
    this.fieldName = fieldName;
    this.configPath = null;
    this.errorType = ConfigErrorType.OTHER;
  }

  /**
   * 构造函数，用于字段配置错误（带原因异常）。
   *
   * @param fieldName 字段名称
   * @param message 错误消息
   * @param cause 原因异常
   */
  public ConfigurationException(String fieldName, String message, Throwable cause) {
    super(
        "CONFIG_FIELD_ERROR",
        String.format("字段 '%s' 配置错误: %s", fieldName, message),
        ErrorLevel.USER_ERROR,
        cause);
    this.fieldName = fieldName;
    this.configPath = null;
    this.errorType = ConfigErrorType.OTHER;
  }

  /**
   * 构造函数，用于详细的配置错误。
   *
   * @param fieldName 字段名称
   * @param configPath 配置路径
   * @param errorType 错误类型
   * @param message 错误消息
   */
  public ConfigurationException(
      String fieldName, String configPath, ConfigErrorType errorType, String message) {
    super(
        "CONFIG_" + errorType.name(),
        buildDetailedMessage(fieldName, configPath, message),
        ErrorLevel.USER_ERROR);
    this.fieldName = fieldName;
    this.configPath = configPath;
    this.errorType = errorType;
  }

  /**
   * 获取配置错误相关的字段名称。
   *
   * @return 字段名称，如果没有特定字段则返回null
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * 获取配置路径。
   *
   * @return 配置路径，如果没有则返回null
   */
  public String getConfigPath() {
    return configPath;
  }

  /**
   * 获取配置错误类型。
   *
   * @return 配置错误类型
   */
  public ConfigErrorType getErrorType() {
    return errorType;
  }

  /**
   * 创建缺少必需配置项异常。
   *
   * @param fieldName 字段名称
   * @return 配置异常实例
   */
  public static ConfigurationException missingRequired(String fieldName) {
    return new ConfigurationException(fieldName, null, ConfigErrorType.MISSING_REQUIRED, "必需的配置项缺失")
        .withContext("requiredField", fieldName);
  }

  /**
   * 创建配置值格式错误异常。
   *
   * @param fieldName 字段名称
   * @param invalidValue 无效值
   * @param expectedFormat 期望格式
   * @return 配置异常实例
   */
  public static ConfigurationException invalidFormat(
      String fieldName, Object invalidValue, String expectedFormat) {
    return new ConfigurationException(
            fieldName,
            null,
            ConfigErrorType.INVALID_FORMAT,
            String.format("配置值格式错误，期望: %s", expectedFormat))
        .withContext("invalidValue", invalidValue)
        .withContext("expectedFormat", expectedFormat);
  }

  /**
   * 创建配置值超出范围异常。
   *
   * @param fieldName 字段名称
   * @param value 超出范围的值
   * @param minValue 最小值
   * @param maxValue 最大值
   * @return 配置异常实例
   */
  public static ConfigurationException outOfRange(
      String fieldName, Object value, Object minValue, Object maxValue) {
    return new ConfigurationException(
            fieldName,
            null,
            ConfigErrorType.OUT_OF_RANGE,
            String.format("配置值超出允许范围 [%s, %s]", minValue, maxValue))
        .withContext("value", value)
        .withContext("minValue", minValue)
        .withContext("maxValue", maxValue);
  }

  /**
   * 创建配置冲突异常。
   *
   * @param fieldName1 冲突字段1
   * @param fieldName2 冲突字段2
   * @param reason 冲突原因
   * @return 配置异常实例
   */
  public static ConfigurationException conflict(
      String fieldName1, String fieldName2, String reason) {
    return new ConfigurationException(
            null,
            null,
            ConfigErrorType.CONFLICT,
            String.format("配置项 '%s' 和 '%s' 存在冲突: %s", fieldName1, fieldName2, reason))
        .withContext("conflictField1", fieldName1)
        .withContext("conflictField2", fieldName2)
        .withContext("conflictReason", reason);
  }

  /** 构建详细的错误消息。 */
  private static String buildDetailedMessage(String fieldName, String configPath, String message) {
    StringBuilder sb = new StringBuilder();

    if (configPath != null) {
      sb.append("配置路径 '").append(configPath).append("'");
    }

    if (fieldName != null) {
      if (sb.length() > 0) {
        sb.append(" 中的");
      }
      sb.append("字段 '").append(fieldName).append("'");
    }

    if (sb.length() > 0) {
      sb.append(" 配置错误: ");
    }

    sb.append(message);
    return sb.toString();
  }
}
