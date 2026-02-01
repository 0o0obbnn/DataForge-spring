package com.dataforge.io;

/**
 * 输出异常类。
 *
 * <p>用于表示数据输出过程中发生的异常情况。 继承自RuntimeException，使得调用方可以选择是否捕获处理。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class OutputException extends RuntimeException {

  /** 序列化版本号。 */
  private static final long serialVersionUID = 1L;

  /**
   * 构造函数。
   *
   * @param message 异常消息
   */
  public OutputException(String message) {
    super(message);
  }

  /**
   * 构造函数。
   *
   * @param message 异常消息
   * @param cause 原因异常
   */
  public OutputException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * 构造函数。
   *
   * @param cause 原因异常
   */
  public OutputException(Throwable cause) {
    super(cause);
  }
}
