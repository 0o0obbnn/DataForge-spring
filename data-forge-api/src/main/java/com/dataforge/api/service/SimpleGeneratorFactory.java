package com.dataforge.api.service;

import com.dataforge.api.generator.DataGenerator;
import com.dataforge.api.model.FieldConfig;
import com.dataforge.api.model.SimpleFieldConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的生成器工厂实现。
 *
 * <p>
 * 使用SPI机制加载生成器，支持动态注册。
 *
 * @author DataForge
 * @since 1.0.0
 * @deprecated 建议使用 {@link com.dataforge.core.GeneratorFactory}，它提供更完整的功能，
 *             包括优先级支持、使用统计和性能监控。此实现仅在需要轻量级场景时使用。
 */
@Deprecated(since = "1.1.0", forRemoval = false)
public class SimpleGeneratorFactory implements GeneratorFactory {

  private final Map<String, DataGenerator<?, ?>> generators = new ConcurrentHashMap<>();

  @Deprecated
  public SimpleGeneratorFactory() {
    loadGeneratorsFromSpi();
  }

  /**
   * 从SPI加载生成器。
   */
  private void loadGeneratorsFromSpi() {
    @SuppressWarnings("rawtypes")
    ServiceLoader<DataGenerator> loader = ServiceLoader.load(DataGenerator.class);
    for (DataGenerator<?, ?> generator : loader) {
      registerGenerator(generator);
    }
  }

  @Override
  @Deprecated
  public Optional<DataGenerator<?, ?>> getGenerator(String type) {
    return Optional.ofNullable(generators.get(type.toLowerCase()));
  }

  @Override
  @Deprecated
  public void registerGenerator(DataGenerator<?, ?> generator) {
    generators.put(generator.getType().toLowerCase(), generator);
  }

  @Override
  @Deprecated
  public List<String> getSupportedTypes() {
    return new ArrayList<>(generators.keySet());
  }

  @Override
  @Deprecated
  public boolean supports(String type) {
    return generators.containsKey(type.toLowerCase());
  }

  @Override
  @Deprecated
  public FieldConfig createConfig(String name, String type) {
    return new SimpleFieldConfig(name, type);
  }
}
