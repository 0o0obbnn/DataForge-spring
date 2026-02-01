package com.dataforge.generators.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于定义数据生成器的优先级。
 *
 * <p>该注解可以应用于实现DataGenerator接口的类，用于指定其优先级。 数值越大优先级越高，在发生类型冲突时优先级高的生成器会被保留。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Priority {
  /**
   * 优先级值，数值越大优先级越高。
   *
   * @return 优先级数值
   */
  int value() default 0;
}
