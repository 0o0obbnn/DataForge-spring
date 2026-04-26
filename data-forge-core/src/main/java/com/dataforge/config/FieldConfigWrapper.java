package com.dataforge.config;

import java.util.Map;

/**
 * 字段配置包装类。
 *
 * <p>用于处理不同类型的字段配置参数，提供灵活的参数接收机制。 该类继承自SimpleFieldConfig，主要用于配置文件的反序列化。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class FieldConfigWrapper extends SimpleFieldConfig {

  /** 默认构造函数。 */
  public FieldConfigWrapper() {
    super();
  }

  /**
   * 构造函数。
   *
   * @param name 字段名称
   * @param type 数据类型标识符
   */
  public FieldConfigWrapper(final String name, final String type) {
    super(name, type);
  }

  /**
   * 构造函数。
   *
   * @param name 字段名称
   * @param type 数据类型标识符
   * @param params 参数映射
   */
  public FieldConfigWrapper(
      final String name, final String type, final Map<String, Object> params) {
    super(name, type);
    setParams(params);
  }

  /**
   * 创建字段配置包装器的便捷方法。
   *
   * @param name 字段名称
   * @param type 数据类型标识符
   * @return 新的字段配置包装器实例
   */
  public static FieldConfigWrapper of(final String name, final String type) {
    return new FieldConfigWrapper(name, type);
  }

  /**
   * 创建字段配置包装器的便捷方法。
   *
   * @param name 字段名称
   * @param type 数据类型标识符
   * @param params 参数映射
   * @return 新的字段配置包装器实例
   */
  public static FieldConfigWrapper of(
      final String name, final String type, final Map<String, Object> params) {
    return new FieldConfigWrapper(name, type, params);
  }

  /**
   * 添加参数的便捷方法，支持链式调用。
   *
   * @param key 参数键
   * @param value 参数值
   * @return 当前实例，支持链式调用
   */
  public FieldConfigWrapper withParam(final String key, final Object value) {
    setParam(key, value);
    return this;
  }

  /**
   * 批量添加参数的便捷方法，支持链式调用。
   *
   * @param params 参数映射
   * @return 当前实例，支持链式调用
   */
  public FieldConfigWrapper withParams(final Map<String, Object> params) {
    if (params != null) {
      getParams().putAll(params);
    }
    return this;
  }

  /**
   * 设置字段描述的便捷方法，支持链式调用。
   *
   * @param description 字段描述
   * @return 当前实例，支持链式调用
   */
  public FieldConfigWrapper withDescription(final String description) {
    setDescription(description);
    return this;
  }

  /**
   * 设置是否必填的便捷方法，支持链式调用。
   *
   * @param required 是否必填
   * @return 当前实例，支持链式调用
   */
  public FieldConfigWrapper withRequired(final boolean required) {
    setRequired(required);
    return this;
  }

  /**
   * 获取字符串类型的参数值。
   *
   * @param key 参数键
   * @param defaultValue 默认值
   * @return 参数值，如果不存在或类型不匹配返回默认值
   */
  public String getStringParam(final String key, final String defaultValue) {
    return getParam(key, String.class, defaultValue);
  }

  /**
   * 获取整数类型的参数值。
   *
   * @param key 参数键
   * @param defaultValue 默认值
   * @return 参数值，如果不存在或类型不匹配返回默认值
   */
  public Integer getIntParam(final String key, final Integer defaultValue) {
    final Object value = getParam(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        // 忽略解析错误，返回默认值
      }
    }
    return defaultValue;
  }

  /**
   * 获取布尔类型的参数值。
   *
   * @param key 参数键
   * @param defaultValue 默认值
   * @return 参数值，如果不存在或类型不匹配返回默认值
   */
  public Boolean getBooleanParam(final String key, final Boolean defaultValue) {
    final Object value = getParam(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    }
    return defaultValue;
  }

  /**
   * 获取双精度浮点数类型的参数值。
   *
   * @param key 参数键
   * @param defaultValue 默认值
   * @return 参数值，如果不存在或类型不匹配返回默认值
   */
  public Double getDoubleParam(final String key, final Double defaultValue) {
    final Object value = getParam(key);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    if (value instanceof String) {
      try {
        return Double.parseDouble((String) value);
      } catch (NumberFormatException e) {
        // 忽略解析错误，返回默认值
      }
    }
    return defaultValue;
  }

  /**
   * 验证配置的有效性。
   *
   * @return 如果配置有效返回true，否则返回false
   */
  public boolean isValidConfig() {
    return getName() != null
        && !getName().trim().isEmpty()
        && getType() != null
        && !getType().trim().isEmpty();
  }
}
