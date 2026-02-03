package com.dataforge.api.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的字段配置实现。
 *
 * @author DataForge
 * @since 1.0.0
 */
public class SimpleFieldConfig implements FieldConfig {

  private final String name;
  private final String type;
  private final Map<String, Object> params;

  public SimpleFieldConfig(String name, String type) {
    this.name = name;
    this.type = type;
    this.params = new HashMap<>();
  }

  public SimpleFieldConfig(String name, String type, Map<String, Object> params) {
    this.name = name;
    this.type = type;
    this.params = params != null ? new HashMap<>(params) : new HashMap<>();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public Map<String, Object> getParams() {
    return Collections.unmodifiableMap(new HashMap<>(params));
  }

  @Override
  public void setParam(String key, Object value) {
    params.put(key, value);
  }

  /**
   * 创建配置构建器。
   *
   * @param name 字段名
   * @param type 字段类型
   * @return 配置构建器
   */
  public static Builder builder(String name, String type) {
    return new Builder(name, type);
  }

  /**
   * 配置构建器。
   */
  public static class Builder {
    private final String name;
    private final String type;
    private final Map<String, Object> params = new HashMap<>();

    private Builder(String name, String type) {
      this.name = name;
      this.type = type;
    }

    public Builder param(String key, Object value) {
      params.put(key, value);
      return this;
    }

    public SimpleFieldConfig build() {
      return new SimpleFieldConfig(name, type, params);
    }
  }
}
