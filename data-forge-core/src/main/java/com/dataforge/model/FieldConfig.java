package com.dataforge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

/**
 * 字段配置基类。
 *
 * <p>所有具体的字段配置类都应该继承此基类。 提供了通用的字段配置属性和参数处理机制。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public abstract class FieldConfig {

  /** 字段名称，在输出中作为列名或字段名使用。 */
  @NotBlank(message = "Field name cannot be blank")
  @Size(max = 255, message = "Field name length cannot exceed 255 characters")
  @Pattern(
      regexp = "[a-zA-Z_][a-zA-Z0-9_]*",
      message =
          "Field name must start with a letter or underscore, and contain only letters, numbers, and underscores")
  private String name;

  /** 数据类型标识符，用于匹配对应的数据生成器。 */
  @NotBlank(message = "Field type cannot be blank")
  @Size(max = 100, message = "Field type length cannot exceed 100 characters")
  private String type;

  /** 字段描述，可选，用于文档和调试。 */
  private String description;

  /** 是否为必填字段，默认为true。 */
  private boolean required = true;

  /** 生成参数映射，用于存储生成器特定的配置参数。 使用 Map<String, Object> 来接收灵活的参数配置。 */
  private Map<String, Object> params = new HashMap<>();

  /** 默认构造函数。 */
  protected FieldConfig() {}

  /**
   * 构造函数。
   *
   * @param name 字段名称
   * @param type 数据类型标识符
   */
  protected FieldConfig(String name, String type) {
    this.name = name;
    this.type = type;
  }

  /**
   * 获取字段名称。
   *
   * @return 字段名称
   */
  public String getName() {
    return name;
  }

  /**
   * 设置字段名称。
   *
   * @param name 字段名称
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * 获取数据类型标识符。
   *
   * @return 数据类型标识符
   */
  public String getType() {
    return type;
  }

  /**
   * 设置数据类型标识符。
   *
   * @param type 数据类型标识符
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * 获取字段描述。
   *
   * @return 字段描述
   */
  public String getDescription() {
    return description;
  }

  /**
   * 设置字段描述。
   *
   * @param description 字段描述
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * 检查是否为必填字段。
   *
   * @return 如果是必填字段返回true，否则返回false
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * 设置是否为必填字段。
   *
   * @param required 是否必填
   */
  public void setRequired(boolean required) {
    this.required = required;
  }

  /**
   * 获取生成参数映射。
   *
   * @return 参数映射
   */
  public Map<String, Object> getParams() {
    return params;
  }

  /**
   * 设置生成参数映射。
   *
   * @param params 参数映射
   */
  public void setParams(Map<String, Object> params) {
    this.params = params != null ? params : new HashMap<>();
  }

  /**
   * 获取指定参数的值。
   *
   * @param key 参数键
   * @return 参数值，如果不存在返回null
   */
  public Object getParam(String key) {
    return params.get(key);
  }

  /**
   * 获取指定参数的值，并转换为指定类型。
   *
   * @param <T> 期望的参数类型
   * @param key 参数键
   * @param type 期望的参数类型
   * @param defaultValue 默认值
   * @return 参数值，如果不存在或类型不匹配返回默认值
   */
  public <T> T getParam(String key, Class<T> type, T defaultValue) {
    Object value = params.get(key);
    if (value != null && type.isInstance(value)) {
      return type.cast(value);
    }
    return defaultValue;
  }

  /**
   * 设置参数值。
   *
   * @param key 参数键
   * @param value 参数值
   */
  public void setParam(String key, Object value) {
    params.put(key, value);
  }

  /**
   * 检查是否包含指定参数。
   *
   * @param key 参数键
   * @return 如果包含该参数返回true，否则返回false
   */
  public boolean hasParam(String key) {
    return params.containsKey(key);
  }

  /**
   * 移除指定参数。
   *
   * @param key 参数键
   * @return 被移除的参数值，如果不存在返回null
   */
  public Object removeParam(String key) {
    return params.remove(key);
  }

  @Override
  public String toString() {
    return String.format(
        "%s{name='%s', type='%s', required=%s, params=%s}",
        getClass().getSimpleName(), name, type, required, params);
  }
}
