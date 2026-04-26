package com.dataforge.service;

/**
 * 错误级别枚举。
 *
 * <p>用于分类异常的严重程度，帮助系统进行不同级别的错误处理。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public enum ErrorLevel {

  /** 用户错误 - 由用户输入或配置问题导致的错误。 通常可以通过修正输入来解决。 */
  USER_ERROR("用户错误", 1),

  /** 业务错误 - 业务逻辑验证失败导致的错误。 通常需要调整业务参数或流程。 */
  BUSINESS_ERROR("业务错误", 2),

  /** 系统错误 - 系统内部错误，如网络、I/O、内存等。 通常需要系统管理员介入处理。 */
  SYSTEM_ERROR("系统错误", 3),

  /** 致命错误 - 严重的系统错误，可能导致服务不可用。 需要立即处理和告警。 */
  FATAL_ERROR("致命错误", 4),

  /** 严重错误 - 最高级别的错误，可能导致数据丢失或系统崩溃。 需要立即处理和告警。 */
  CRITICAL_ERROR("严重错误", 5);

  private final String description;
  private final int severity;

  ErrorLevel(String description, int severity) {
    this.description = description;
    this.severity = severity;
  }

  /**
   * 获取错误级别描述。
   *
   * @return 错误级别描述
   */
  public String getDescription() {
    return description;
  }

  /**
   * 获取错误严重程度。
   *
   * @return 错误严重程度（数值越大越严重）
   */
  public int getSeverity() {
    return severity;
  }

  /**
   * 检查是否为用户错误。
   *
   * @return 如果是用户错误返回true，否则返回false
   */
  public boolean isUserError() {
    return this == USER_ERROR;
  }

  /**
   * 检查是否为系统错误。
   *
   * @return 如果是系统错误返回true，否则返回false
   */
  public boolean isSystemError() {
    return this == SYSTEM_ERROR || this == FATAL_ERROR || this == CRITICAL_ERROR;
  }
}
