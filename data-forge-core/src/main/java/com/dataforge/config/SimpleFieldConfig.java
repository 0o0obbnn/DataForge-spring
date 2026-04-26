package com.dataforge.config;

import com.dataforge.model.FieldConfig;
import java.util.Map;

/**
 * 简单字段配置实现类
 *
 * <p>用于Jackson反序列化，因为Jackson无法直接实例化抽象类
 *
 * @author DataForge
 * @since 1.0.0
 */
public class SimpleFieldConfig extends FieldConfig {

  /** 默认构造函数 */
  public SimpleFieldConfig() {
    super();
  }

  /**
   * 构造函数
   *
   * @param name 字段名称
   * @param type 数据类型标识符
   */
  public SimpleFieldConfig(final String name, final String type) {
    super(name, type);
  }

  /**
   * 构造函数
   *
   * @param name 字段名称
   * @param type 数据类型标识符
   * @param params 参数映射
   */
  public SimpleFieldConfig(final String name, final String type, final Map<String, Object> params) {
    super(name, type);
    setParams(params);
  }
}
