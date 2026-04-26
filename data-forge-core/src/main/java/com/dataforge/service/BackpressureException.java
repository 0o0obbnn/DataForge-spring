package com.dataforge.service;

/**
 * 背压异常。
 *
 * <p>当数据生成速度超过处理能力时抛出，表示系统正在承受过载压力。 该异常用于触发背压控制机制，减缓数据生成速度或停止生成。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class BackpressureException extends GenerationException {
  /**
   * 构造函数。
   *
   * @param message 异常消息
   */
  public BackpressureException(String message) {
    super(message);
  }

  /**
   * 构造函数。
   *
   * @param message 异常消息
   * @param cause 异常原因
   */
  public BackpressureException(String message, Throwable cause) {
    super(message, cause);
  }
}
