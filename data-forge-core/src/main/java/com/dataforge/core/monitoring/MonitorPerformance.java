package com.dataforge.core.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 性能监控注解。
 *
 * <p>标记需要进行性能监控的方法。被此注解标记的方法会自动收集：
 *
 * <ul>
 *   <li>方法调用次数
 *   <li>方法执行时间
 *   <li>成功/失败统计
 *   <li>慢查询检测
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorPerformance {

  /**
   * 指标名称，如果为空则使用方法的全限定名。
   *
   * @return 指标名称
   */
  String value() default "";

  /**
   * 慢查询阈值（毫秒）。 超过此阈值的方法调用会被记录为慢查询。
   *
   * @return 慢查询阈值
   */
  long slowThreshold() default 1000;

  /**
   * 是否记录方法参数（用于调试）。
   *
   * @return 是否记录参数
   */
  boolean logParameters() default false;

  /**
   * 是否记录方法返回值（用于调试）。
   *
   * @return 是否记录返回值
   */
  boolean logReturnValue() default false;
}
