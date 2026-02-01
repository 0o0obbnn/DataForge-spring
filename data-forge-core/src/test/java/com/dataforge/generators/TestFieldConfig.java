package com.dataforge.generators;

import com.dataforge.model.FieldConfig;
import java.util.Map;

/**
 * 测试用的字段配置类。
 *
 * <p>该类用于在测试环境中模拟字段配置，提供基本的配置功能 而不依赖于完整的配置体系。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class TestFieldConfig extends FieldConfig {

  private final String name;
  private final String type;
  private final Map<String, Object> params;

  /**
   * 构造函数。
   *
   * @param name 字段名称
   * @param type 字段类型
   * @param params 参数映射
   */
  public TestFieldConfig(String name, String type, Map<String, Object> params) {
    this.name = name;
    this.type = type;
    this.params = params;
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
    return params;
  }

  @Override
  public Object getParam(String key) {
    return params != null ? params.get(key) : null;
  }

  /**
   * 设置参数值（用于测试）。
   *
   * @param key 参数键
   * @param value 参数值
   */
  public void set(String key, Object value) {
    if (params != null) {
      params.put(key, value);
    }
  }

  @Override
  public String toString() {
    return "TestFieldConfig{"
        + "name='"
        + name
        + '\''
        + ", type='"
        + type
        + '\''
        + ", params="
        + params
        + '}';
  }
}
