package com.dataforge.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a priority for a DataGenerator. Generators with a higher value will be
 * preferred when multiple generators are found for the same type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Priority {
  /**
   * The priority value. Higher values indicate higher priority.
   *
   * @return the priority value
   */
  int value() default 0;
}
