package com.dataforge.web.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** API限流注解，用于标记需要限流的方法。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

  /** 时间窗口内允许的最大请求数。 */
  int value() default 10;

  /** 时间窗口大小（秒）。 */
  int seconds() default 60;

  /** 限流的键前缀。 */
  String prefix() default "rate_limit";
}
