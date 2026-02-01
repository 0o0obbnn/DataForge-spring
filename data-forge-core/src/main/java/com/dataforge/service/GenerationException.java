package com.dataforge.service;

/**
 * 数据生成异常类。
 *
 * <p>当数据生成过程中发生错误时抛出此异常。 属于系统错误类型，通常需要检查系统状态和生成器实现。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class GenerationException extends DataForgeException {

  /** 序列化版本号。 */
  private static final long serialVersionUID = 1L;

  /** 发生错误的记录索引。 */
  private final Integer recordIndex;

  /**
   * 构造函数。
   *
   * @param message 异常消息
   */
  public GenerationException(String message) {
    super("GENERATION_ERROR", message, ErrorLevel.SYSTEM_ERROR);
    this.recordIndex = null;
  }

  /**
   * 构造函数。
   *
   * @param message 异常消息
   * @param cause 原因异常
   */
  public GenerationException(String message, Throwable cause) {
    super("GENERATION_ERROR", message, ErrorLevel.SYSTEM_ERROR, cause);
    this.recordIndex = null;
  }

  /**
   * 构造函数，用于记录生成错误。
   *
   * @param recordIndex 记录索引
   * @param message 错误消息
   */
  public GenerationException(int recordIndex, String message) {
    super(
        "GENERATION_RECORD_ERROR",
        String.format("记录 %d 生成失败: %s", recordIndex, message),
        ErrorLevel.SYSTEM_ERROR);
    this.recordIndex = recordIndex;
  }

  /**
   * 构造函数，用于字段生成错误。
   *
   * @param fieldName 字段名称
   * @param recordIndex 记录索引
   * @param message 错误消息
   */
  public GenerationException(String fieldName, int recordIndex, String message) {
    super(
        "GENERATION_FIELD_ERROR",
        String.format("记录 %d 的字段 %s 生成失败: %s", recordIndex, fieldName, message),
        ErrorLevel.SYSTEM_ERROR);
    this.recordIndex = recordIndex;
  }

  /**
   * 构造函数，用于记录生成错误。
   *
   * @param recordIndex 记录索引
   * @param message 错误消息
   * @param cause 原因异常
   */
  public GenerationException(int recordIndex, String message, Throwable cause) {
    super(
        "GENERATION_RECORD_ERROR",
        String.format("记录 %d 生成失败: %s", recordIndex, message),
        ErrorLevel.SYSTEM_ERROR,
        cause);
    this.recordIndex = recordIndex;
  }

  /**
   * 构造函数，用于背压异常。
   *
   * @param message 异常消息
   * @param cause 原因异常
   */
  public GenerationException(String message, Throwable cause, boolean backpressure) {
    super("BACKPRESSURE_ERROR", message, ErrorLevel.SYSTEM_ERROR, cause);
    this.recordIndex = null;
  }

  /**
   * 获取发生错误的记录索引。
   *
   * @return 记录索引，如果没有特定记录则返回null
   */
  public Integer getRecordIndex() {
    return recordIndex;
  }
}
