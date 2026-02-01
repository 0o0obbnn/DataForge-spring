package com.dataforge.validation;

/**
 * 数据校验器接口。
 *
 * <p>定义了数据校验的统一契约。所有具体的校验器都应该实现此接口。
 *
 * @param <T> 要校验的数据类型
 * @author DataForge Team
 * @since 1.0.0
 */
public interface Validator<T> {

  /**
   * 校验数据是否有效。
   *
   * @param data 要校验的数据
   * @return 如果数据有效返回true，否则返回false
   */
  boolean isValid(T data);

  /**
   * 校验数据并返回详细的校验结果。
   *
   * @param data 要校验的数据
   * @return 校验结果
   */
  ValidationResult validate(T data);

  /**
   * 获取校验器的名称。
   *
   * @return 校验器名称
   */
  String getName();

  /**
   * 获取校验器的描述。
   *
   * @return 校验器描述
   */
  String getDescription();
}
