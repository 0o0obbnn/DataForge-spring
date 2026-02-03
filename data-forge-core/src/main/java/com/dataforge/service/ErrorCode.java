package com.dataforge.service;

/**
 * DataForge 错误码枚举
 *
 * <p>定义系统中所有可能的错误类型，用于错误分类和处理。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public enum ErrorCode {
  // 生成器相关 (1xxx)
  GENERATOR_NOT_FOUND(1001, "生成器未找到"),
  GENERATION_FAILED(1002, "数据生成失败"),
  GENERATOR_CONFIG_INVALID(1003, "生成器配置无效"),

  // 配置相关 (2xxx)
  CONFIG_PARSE_ERROR(2001, "配置解析错误"),
  CONFIG_VALIDATION_ERROR(2002, "配置验证失败"),

  // 输出相关 (3xxx)
  OUTPUT_WRITE_ERROR(3001, "输出写入错误"),
  OUTPUT_FORMAT_ERROR(3002, "输出格式错误"),

  // 资源相关 (4xxx)
  RESOURCE_LIMIT_EXCEEDED(4001, "资源限制超出"),
  TASK_ALREADY_RUNNING(4002, "任务已在运行");

  private final int code;
  private final String message;

  ErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
