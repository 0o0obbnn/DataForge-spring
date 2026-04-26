package com.dataforge.api.service;

import com.dataforge.api.generator.DataGenerator;
import com.dataforge.api.model.FieldConfig;
import java.util.List;
import java.util.Optional;

/**
 * 生成器工厂接口。
 *
 * <p>用于管理和获取数据生成器实例。
 *
 * @author DataForge
 * @since 1.0.0
 */
public interface GeneratorFactory {

  /**
   * 获取指定类型的生成器。
   *
   * @param type 生成器类型
   * @return 生成器的Optional
   */
  Optional<DataGenerator<?, ?>> getGenerator(String type);

  /**
   * 注册生成器。
   *
   * @param generator 生成器实例
   */
  void registerGenerator(DataGenerator<?, ?> generator);

  /**
   * 获取所有支持的类型。
   *
   * @return 类型列表
   */
  List<String> getSupportedTypes();

  /**
   * 检查是否支持指定类型。
   *
   * @param type 类型
   * @return 如果支持返回true
   */
  boolean supports(String type);

  /**
   * 创建字段配置。
   *
   * @param name 字段名
   * @param type 字段类型
   * @return 字段配置
   */
  FieldConfig createConfig(String name, String type);
}
