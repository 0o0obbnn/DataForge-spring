package com.dataforge.api.service;

import com.dataforge.api.context.DataForgeContext;
import com.dataforge.api.context.SimpleDataForgeContext;
import com.dataforge.api.generator.DataGenerator;
import com.dataforge.api.model.FieldConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 简单的DataForge服务实现。
 *
 * <p>
 * 该类使用 SimpleGeneratorFactory 作为默认生成器工厂。
 * 对于需要更丰富功能的场景，建议使用 com.dataforge.core.GeneratorFactory。
 *
 * @author DataForge
 * @since 1.0.0
 */
public class SimpleDataForgeService implements DataForgeService {

  private final GeneratorFactory generatorFactory;

  /**
   * 使用默认的 SimpleGeneratorFactory 构造服务。
   *
   * <p>
   * 注意：SimpleGeneratorFactory 已标记为废弃，推荐通过构造函数注入自定义 GeneratorFactory
   */
  @Deprecated
  public SimpleDataForgeService() {
    this.generatorFactory = new SimpleGeneratorFactory();
  }

  public SimpleDataForgeService(GeneratorFactory generatorFactory) {
    this.generatorFactory = generatorFactory;
  }

  @Override
  public Object generate(FieldConfig config) {
    return generate(config, createContext());
  }

  @Override
  public Object generate(FieldConfig config, DataForgeContext context) {
    Optional<DataGenerator<?, ?>> generatorOpt = generatorFactory.getGenerator(config.getType());
    if (generatorOpt.isEmpty()) {
      throw new IllegalArgumentException("Unsupported generator type: " + config.getType());
    }

    @SuppressWarnings("unchecked")
    DataGenerator<Object, FieldConfig> generator = (DataGenerator<Object, FieldConfig>) generatorOpt.get();
    return generator.generate(config, context);
  }

  @Override
  public List<Map<String, Object>> generateBatch(List<FieldConfig> configs, int count) {
    List<Map<String, Object>> results = new ArrayList<>(count);

    for (int i = 0; i < count; i++) {
      DataForgeContext context = createContext();
      Map<String, Object> row = new HashMap<>();

      for (FieldConfig config : configs) {
        Object value = generate(config, context);
        row.put(config.getName(), value);
      }

      results.add(row);
    }

    return results;
  }

  @Override
  public GeneratorFactory getGeneratorFactory() {
    return generatorFactory;
  }

  @Override
  public DataForgeContext createContext() {
    return new SimpleDataForgeContext();
  }
}
