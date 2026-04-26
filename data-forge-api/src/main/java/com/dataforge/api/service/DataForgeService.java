package com.dataforge.api.service;

import com.dataforge.api.context.DataForgeContext;
import com.dataforge.api.model.FieldConfig;
import java.util.List;
import java.util.Map;

/**
 * DataForge服务接口。
 *
 * <p>提供数据生成的核心服务。
 *
 * @author DataForge
 * @since 1.0.0
 */
public interface DataForgeService {

  /**
   * 生成单条数据。
   *
   * @param config 字段配置
   * @return 生成的数据
   */
  Object generate(FieldConfig config);

  /**
   * 生成单条数据（带上下文）。
   *
   * @param config 字段配置
   * @param context 上下文
   * @return 生成的数据
   */
  Object generate(FieldConfig config, DataForgeContext context);

  /**
   * 批量生成数据。
   *
   * @param configs 字段配置列表
   * @param count 生成数量
   * @return 生成的数据列表
   */
  List<Map<String, Object>> generateBatch(List<FieldConfig> configs, int count);

  /**
   * 获取生成器工厂。
   *
   * @return 生成器工厂
   */
  GeneratorFactory getGeneratorFactory();

  /**
   * 创建新的上下文。
   *
   * @return 新上下文
   */
  DataForgeContext createContext();
}
