package com.dataforge.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * DataForge服务异常基类 - 增强版本。
 *
 * <p>用于表示DataForge服务层发生的异常情况。 继承自RuntimeException，使得调用方可以选择是否捕获处理。
 * 增加了错误码、错误级别、上下文信息和异常链追踪支持，便于异常分类和处理。
 *
 * <p><strong>增强特性：</strong>
 *
 * <ul>
 *   <li>上下文信息支持：可附加键值对形式的上下文数据
 *   <li>异常发生时间记录：便于问题追踪和日志分析
 *   <li>结构化错误信息：支持机器可读的错误分类
 *   <li>流式API：支持方法链式调用添加上下文
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public abstract class DataForgeException extends RuntimeException {

  /** 序列化版本号。 */
  private static final long serialVersionUID = 1L;

  /** 错误码，用于唯一标识错误类型。 */
  private final String errorCode;

  /** 错误级别，用于表示错误的严重程度。 */
  private final ErrorLevel level;

  /** 异常发生时间。 */
  private final LocalDateTime occurredAt;

  /** 上下文信息，用于存储与异常相关的额外数据。 */
  private final Map<String, Object> context;

  /**
   * 构造函数。
   *
   * @param errorCode 错误码
   * @param message 异常消息
   * @param level 错误级别
   */
  protected DataForgeException(String errorCode, String message, ErrorLevel level) {
    super(message);
    this.errorCode = Objects.requireNonNull(errorCode, "Error code cannot be null");
    this.level = Objects.requireNonNull(level, "Error level cannot be null");
    this.occurredAt = LocalDateTime.now();
    this.context = new HashMap<>();
  }

  /**
   * 构造函数。
   *
   * @param errorCode 错误码
   * @param message 异常消息
   * @param level 错误级别
   * @param cause 原因异常
   */
  protected DataForgeException(
      String errorCode, String message, ErrorLevel level, Throwable cause) {
    super(message, cause);
    this.errorCode = Objects.requireNonNull(errorCode, "Error code cannot be null");
    this.level = Objects.requireNonNull(level, "Error level cannot be null");
    this.occurredAt = LocalDateTime.now();
    this.context = new HashMap<>();
  }

  /**
   * 添加上下文信息（流式API）。
   *
   * @param key 上下文键
   * @param value 上下文值
   * @return 当前异常实例，支持方法链调用
   */
  @SuppressWarnings("unchecked")
  public <T extends DataForgeException> T withContext(String key, Object value) {
    if (key != null && !key.trim().isEmpty()) {
      this.context.put(key, value);
    }
    // 这是安全的类型转换，因为T是当前类的子类
    T result = (T) this;
    return result;
  }

  /**
   * 批量添加上下文信息。
   *
   * @param contextMap 上下文信息映射
   * @return 当前异常实例，支持方法链调用
   */
  @SuppressWarnings("unchecked")
  public <T extends DataForgeException> T withContext(Map<String, Object> contextMap) {
    if (contextMap != null) {
      this.context.putAll(contextMap);
    }
    // 这是安全的类型转换，因为T是当前类的子类
    T result = (T) this;
    return result;
  }

  /**
   * 获取错误码。
   *
   * @return 错误码
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * 获取错误级别。
   *
   * @return 错误级别
   */
  public ErrorLevel getLevel() {
    return level;
  }

  /**
   * 获取异常发生时间。
   *
   * @return 异常发生时间
   */
  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  /**
   * 获取上下文信息的只读视图。
   *
   * @return 上下文信息的不可变映射
   */
  public Map<String, Object> getContext() {
    return Collections.unmodifiableMap(context);
  }

  /**
   * 获取指定键的上下文值。
   *
   * @param key 上下文键
   * @return 上下文值，如果不存在则返回null
   */
  public Object getContextValue(String key) {
    return context.get(key);
  }

  /**
   * 获取指定类型的上下文值。
   *
   * @param <T> 期望的值类型
   * @param key 上下文键
   * @param type 值类型
   * @return 指定类型的上下文值，如果不存在或类型不匹配则返回null
   */
  @SuppressWarnings("unchecked")
  public <T> T getContextValue(String key, Class<T> type) {
    Object value = context.get(key);
    if (value != null && type.isInstance(value)) {
      return (T) value;
    }
    return null;
  }

  /**
   * 是否为用户错误。
   *
   * @return 如果是用户错误返回true，否则返回false
   */
  public boolean isUserError() {
    return level == ErrorLevel.USER_ERROR;
  }

  /**
   * 是否为业务错误。
   *
   * @return 如果是业务错误返回true，否则返回false
   */
  public boolean isBusinessError() {
    return level == ErrorLevel.BUSINESS_ERROR;
  }

  /**
   * 是否为系统错误。
   *
   * @return 如果是系统错误返回true，否则返回false
   */
  public boolean isSystemError() {
    return level == ErrorLevel.SYSTEM_ERROR
        || level == ErrorLevel.FATAL_ERROR
        || level == ErrorLevel.CRITICAL_ERROR;
  }

  /**
   * 是否为严重错误（需要立即关注）。
   *
   * @return 如果是严重错误返回true，否则返回false
   */
  public boolean isCritical() {
    return level == ErrorLevel.CRITICAL_ERROR || level == ErrorLevel.FATAL_ERROR;
  }

  /**
   * 获取结构化的错误信息。
   *
   * @return 包含所有错误信息的映射
   */
  public Map<String, Object> getStructuredInfo() {
    Map<String, Object> info = new HashMap<>();
    info.put("errorCode", errorCode);
    info.put("level", level.name());
    info.put("message", getMessage());
    info.put("occurredAt", occurredAt);
    info.put("context", getContext());
    if (getCause() != null) {
      info.put("causedBy", getCause().getClass().getSimpleName());
      info.put("causeMessage", getCause().getMessage());
    }
    return Collections.unmodifiableMap(info);
  }

  /** 重写toString方法，提供更详细的异常信息。 */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName())
        .append("[")
        .append("errorCode=")
        .append(errorCode)
        .append(", level=")
        .append(level)
        .append(", occurredAt=")
        .append(occurredAt)
        .append(", message=")
        .append(getMessage());

    if (!context.isEmpty()) {
      sb.append(", context=").append(context);
    }

    sb.append("]");
    return sb.toString();
  }
}
