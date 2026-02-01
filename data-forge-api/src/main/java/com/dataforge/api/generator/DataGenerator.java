package com.dataforge.api.generator;

import com.dataforge.api.context.DataForgeContext;
import com.dataforge.api.model.FieldConfig;

/**
 * 数据生成器接口。
 *
 * <p>所有数据生成器必须实现此接口。这是DataForge框架的核心SPI接口。
 *
 * @param <T> 生成数据类型
 * @param <C> 配置类型
 * @author DataForge
 * @since 1.0.0
 */
public interface DataGenerator<T, C extends FieldConfig> {

  /**
   * 获取生成器类型标识。
   *
   * @return 类型标识符，如 "name", "email", "phone" 等
   */
  String getType();

  /**
   * 生成数据。
   *
   * @param config 字段配置
   * @param context 数据生成上下文
   * @return 生成的数据
   */
  T generate(C config, DataForgeContext context);

  /**
   * 获取配置类类型。
   *
   * @return 配置类Class对象
   */
  default Class<C> getConfigClass() {
    @SuppressWarnings("unchecked")
    Class<C> clazz = (Class<C>) FieldConfig.class;
    return clazz;
  }

  /**
   * 获取生成器描述。
   *
   * @return 生成器描述信息
   */
  default String getDescription() {
    return "Data generator for " + getType();
  }

  /**
   * 检查此生成器是否支持指定的类型。
   *
   * @param type 类型标识符
   * @return 如果支持返回true
   */
  default boolean supports(String type) {
    return getType().equalsIgnoreCase(type);
  }
}
