package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 整数生成器
 *
 * <p>生成各种范围的整数数据，支持自定义最小值、最大值和分布类型。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>min: 最小值（默认0）
 *   <li>max: 最大值（默认100）
 *   <li>distribution: 分布类型 (UNIFORM|NORMAL) 默认: UNIFORM
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class IntegerGenerator extends BaseGenerator implements DataGenerator<Integer, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(IntegerGenerator.class);

  /** 默认最小值。 */
  private static final int DEFAULT_MIN = 0;

  /** 默认最大值。 */
  private static final int DEFAULT_MAX = 100;

  /** 默认分布类型。 */
  private static final String DEFAULT_DISTRIBUTION = "UNIFORM";

  @Override
  public String getType() {
    return "integer";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public Integer generate(FieldConfig config, DataForgeContext context) {
    // 获取配置参数
    int min = getIntParam(config, "min", DEFAULT_MIN);
    int max = getIntParam(config, "max", DEFAULT_MAX);
    String distribution = getStringParam(config, "distribution", DEFAULT_DISTRIBUTION);

    // 确保范围有效
    if (min > max) {
      logger.warn("Min ({}) > Max ({}), swapping values", min, max);
      int temp = min;
      min = max;
      max = temp;
    }

    // 根据分布类型生成整数
    switch (distribution.toUpperCase()) {
      case "NORMAL":
        return generateNormalInteger(min, max);
      case "UNIFORM":
      default:
        return generateUniformInteger(min, max);
    }
  }

  /**
   * 生成均匀分布的整数
   *
   * @param min 最小值
   * @param max 最大值
   * @return 随机整数
   */
  private Integer generateUniformInteger(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  /**
   * 生成正态分布的整数（使用Box-Muller变换）
   *
   * @param min 最小值
   * @param max 最大值
   * @return 随机整数
   */
  private Integer generateNormalInteger(int min, int max) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    // 计算均值和标准差
    double mean = (min + max) / 2.0;
    double stdDev = (max - min) / 6.0; // 使用6sigma规则

    // Box-Muller变换生成正态分布随机数
    double u1 = random.nextDouble();
    double u2 = random.nextDouble();
    double z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);

    // 转换为目标范围
    double value = mean + z0 * stdDev;

    // 确保在范围内
    int result = (int) Math.round(value);
    return Math.max(min, Math.min(max, result));
  }
}
