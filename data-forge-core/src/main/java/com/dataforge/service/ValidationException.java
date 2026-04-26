package com.dataforge.service;

/**
 * 数据校验异常类。
 *
 * <p>当数据校验失败时抛出此异常。 属于业务错误类型，通常需要检查业务规则和数据合法性。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class ValidationException extends DataForgeException {

  /** 序列化版本号。 */
  private static final long serialVersionUID = 1L;

  /** 校验失败的字段名称。 */
  private final String fieldName;

  /** 校验失败的字段值。 */
  private final Object fieldValue;

  /**
   * 构造函数。
   *
   * @param message 异常消息
   */
  public ValidationException(String message) {
    super("VALIDATION_ERROR", message, ErrorLevel.BUSINESS_ERROR);
    this.fieldName = null;
    this.fieldValue = null;
  }

  /**
   * 构造函数（带原因异常）。
   *
   * @param message 异常消息
   * @param cause 原因异常
   */
  public ValidationException(String message, RuntimeException cause) {
    super("VALIDATION_ERROR", message, ErrorLevel.BUSINESS_ERROR, cause);
    this.fieldName = null;
    this.fieldValue = null;
  }

  /**
   * 构造函数。
   *
   * @param fieldName 字段名称
   * @param message 异常消息
   */
  public ValidationException(String fieldName, String message) {
    super(
        "VALIDATION_FIELD_ERROR",
        String.format("字段 %s 校验失败: %s", fieldName, message),
        ErrorLevel.BUSINESS_ERROR);
    this.fieldName = fieldName;
    this.fieldValue = null;
  }

  /**
   * 构造函数。
   *
   * @param fieldName 字段名称
   * @param fieldValue 字段值
   * @param message 异常消息
   */
  public ValidationException(String fieldName, Object fieldValue, String message) {
    super(
        "VALIDATION_FIELD_VALUE_ERROR",
        String.format("字段 %s 的值 %s 校验失败: %s", fieldName, fieldValue, message),
        ErrorLevel.BUSINESS_ERROR);
    this.fieldName = fieldName;
    this.fieldValue = fieldValue;
  }

  /**
   * 获取校验失败的字段名称。
   *
   * @return 字段名称，如果没有特定字段则返回null
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * 获取校验失败的字段值。
   *
   * @return 字段值，如果没有具体值则返回null
   */
  public Object getFieldValue() {
    return fieldValue;
  }
}
