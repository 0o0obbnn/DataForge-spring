package com.dataforge.api.service;

import com.dataforge.api.context.DataForgeContext;
import com.dataforge.api.generator.DataGenerator;
import com.dataforge.api.model.FieldConfig;

/**
 * 测试用 "number" 类型生成器，供 API 模块测试时通过 SPI 加载。
 *
 * <p>仅用于测试，不用于生产。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class TestNumberGenerator implements DataGenerator<String, FieldConfig> {

  @Override
  public String getType() {
    return "number";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    return String.valueOf(System.nanoTime() % 10000);
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }
}
