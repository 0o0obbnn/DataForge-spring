package com.dataforge.api.model;

import java.util.Map;
import java.util.Optional;

/**
 * 字段配置接口。
 *
 * <p>定义数据生成器所需的配置信息。
 *
 * @author DataForge
 * @since 1.0.0
 */
public interface FieldConfig {

  /**
   * 获取字段名称。
   *
   * @return 字段名称
   */
  String getName();

  /**
   * 获取字段类型。
   *
   * @return 字段类型
   */
  String getType();

  /**
   * 获取参数映射。
   *
   * @return 参数映射表
   */
  Map<String, Object> getParams();

  /**
   * 获取参数。
   *
   * @param key 参数键
   * @param type 参数类型
   * @param defaultValue 默认值
   * @param <T> 参数类型
   * @return 参数值
   */
  @SuppressWarnings("unchecked")
  default <T> T getParam(String key, Class<T> type, T defaultValue) {
    Object value = getParams().get(key);
    if (value == null) {
      return defaultValue;
    }
    if (type.isInstance(value)) {
      return (T) value;
    }
    return defaultValue;
  }

  /**
   * 获取参数（返回Optional）。
   *
   * @param key 参数键
   * @param type 参数类型
   * @param <T> 参数类型
   * @return 参数值的Optional
   */
  default <T> Optional<T> getParamOptional(String key, Class<T> type) {
    return Optional.ofNullable(getParam(key, type, null));
  }

  /**
   * 设置参数。
   *
   * @param key 参数键
   * @param value 参数值
   */
  void setParam(String key, Object value);

  /**
   * 检查是否包含指定参数。
   *
   * @param key 参数键
   * @return 如果存在返回true
   */
  default boolean hasParam(String key) {
    return getParams().containsKey(key);
  }
}
