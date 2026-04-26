package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 婚姻状况生成器
 *
 * <p>支持的参数： - status: 指定状态 (SINGLE|MARRIED|DIVORCED|WIDOWED|ANY) - married_ratio: 已婚占比 (0.0-1.0) -
 * divorced_ratio: 离异占比 (0.0-1.0) - widowed_ratio: 丧偶占比 (0.0-1.0) - age_related: 是否与年龄关联
 * (true|false) - format: 输出格式 (CHINESE|ENGLISH|CODE)
 *
 * @author DataForge
 */
public class MaritalStatusGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(MaritalStatusGenerator.class);
  private static final Random random = new Random();

  // 婚姻状况枚举
  private enum MaritalStatus {
    SINGLE, // 未婚
    MARRIED, // 已婚
    DIVORCED, // 离异
    WIDOWED // 丧偶
  }

  // 中文输出映射
  private static final Map<MaritalStatus, String> CHINESE_NAMES = new HashMap<>();

  // 英文输出映射
  private static final Map<MaritalStatus, String> ENGLISH_NAMES = new HashMap<>();

  // 代码输出映射
  private static final Map<MaritalStatus, String> CODE_NAMES = new HashMap<>();

  // 现实分布权重（基于中国人口统计）
  private static final Map<MaritalStatus, Double> REALISTIC_WEIGHTS = new HashMap<>();

  // 年龄与婚姻状况的合理性映射
  private static final Map<MaritalStatus, Integer> MIN_AGE_FOR_STATUS = new HashMap<>();

  static {
    initializeMappings();
    initializeRealisticWeights();
    initializeAgeMapping();
  }

  private static void initializeMappings() {
    // 中文映射
    CHINESE_NAMES.put(MaritalStatus.SINGLE, "未婚");
    CHINESE_NAMES.put(MaritalStatus.MARRIED, "已婚");
    CHINESE_NAMES.put(MaritalStatus.DIVORCED, "离异");
    CHINESE_NAMES.put(MaritalStatus.WIDOWED, "丧偶");

    // 英文映射
    ENGLISH_NAMES.put(MaritalStatus.SINGLE, "Single");
    ENGLISH_NAMES.put(MaritalStatus.MARRIED, "Married");
    ENGLISH_NAMES.put(MaritalStatus.DIVORCED, "Divorced");
    ENGLISH_NAMES.put(MaritalStatus.WIDOWED, "Widowed");

    // 代码映射
    CODE_NAMES.put(MaritalStatus.SINGLE, "S");
    CODE_NAMES.put(MaritalStatus.MARRIED, "M");
    CODE_NAMES.put(MaritalStatus.DIVORCED, "D");
    CODE_NAMES.put(MaritalStatus.WIDOWED, "W");
  }

  private static void initializeRealisticWeights() {
    // 基于中国成年人口婚姻状况的近似分布
    REALISTIC_WEIGHTS.put(MaritalStatus.SINGLE, 0.25); // 25% 未婚
    REALISTIC_WEIGHTS.put(MaritalStatus.MARRIED, 0.65); // 65% 已婚
    REALISTIC_WEIGHTS.put(MaritalStatus.DIVORCED, 0.08); // 8% 离异
    REALISTIC_WEIGHTS.put(MaritalStatus.WIDOWED, 0.02); // 2% 丧偶
  }

  private static void initializeAgeMapping() {
    // 各婚姻状况的最小合理年龄
    MIN_AGE_FOR_STATUS.put(MaritalStatus.SINGLE, 16); // 16岁以上可以未婚
    MIN_AGE_FOR_STATUS.put(MaritalStatus.MARRIED, 18); // 18岁以上可以结婚
    MIN_AGE_FOR_STATUS.put(MaritalStatus.DIVORCED, 20); // 20岁以上可能离异
    MIN_AGE_FOR_STATUS.put(MaritalStatus.WIDOWED, 25); // 25岁以上可能丧偶
  }

  @Override
  public String getType() {
    return "marital";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String status = config.getParam("status", String.class, "ANY");
      double marriedRatio =
          Double.parseDouble(config.getParam("married_ratio", String.class, "0.65"));
      double divorcedRatio =
          Double.parseDouble(config.getParam("divorced_ratio", String.class, "0.08"));
      double widowedRatio =
          Double.parseDouble(config.getParam("widowed_ratio", String.class, "0.02"));
      boolean ageRelated =
          Boolean.parseBoolean(config.getParam("age_related", String.class, "true"));
      String format = config.getParam("format", String.class, "CHINESE");

      // 获取可选的婚姻状况列表
      List<MaritalStatus> availableStatuses = getAvailableStatuses(status);

      // 如果启用年龄关联，根据年龄过滤婚姻状况
      if (ageRelated) {
        Integer age = context.get("age", Integer.class).orElse(null);
        if (age != null) {
          availableStatuses = filterByAge(availableStatuses, age);
        }
      }

      // 根据权重选择婚姻状况
      MaritalStatus maritalStatus =
          selectMaritalStatus(availableStatuses, marriedRatio, divorcedRatio, widowedRatio);

      // 将婚姻状况信息存入上下文
      context.put("marital_status", maritalStatus.name());

      // 格式化输出
      String result = formatMaritalStatus(maritalStatus, format);

      logger.debug("Generated marital status: {}", result);
      return result;

    } catch (Exception e) {
      logger.error("Error generating marital status", e);
      return "未婚";
    }
  }

  private List<MaritalStatus> getAvailableStatuses(String status) {
    List<MaritalStatus> statuses = new ArrayList<>();

    if ("ANY".equals(status)) {
      statuses.addAll(Arrays.asList(MaritalStatus.values()));
    } else {
      try {
        MaritalStatus specificStatus = MaritalStatus.valueOf(status);
        statuses.add(specificStatus);
      } catch (IllegalArgumentException e) {
        logger.warn("Unknown marital status: {}. Using all statuses.", status);
        statuses.addAll(Arrays.asList(MaritalStatus.values()));
      }
    }

    return statuses;
  }

  private List<MaritalStatus> filterByAge(List<MaritalStatus> statuses, int age) {
    List<MaritalStatus> filteredStatuses = new ArrayList<>();

    for (MaritalStatus status : statuses) {
      Integer minAge = MIN_AGE_FOR_STATUS.get(status);
      if (minAge != null && age >= minAge) {
        filteredStatuses.add(status);
      }
    }

    // 如果过滤后没有合适的状态，返回最基本的状态
    if (filteredStatuses.isEmpty()) {
      if (age >= 16) {
        filteredStatuses.add(MaritalStatus.SINGLE);
      }
    }

    return filteredStatuses.isEmpty() ? Arrays.asList(MaritalStatus.SINGLE) : filteredStatuses;
  }

  private MaritalStatus selectMaritalStatus(
      List<MaritalStatus> availableStatuses,
      double marriedRatio,
      double divorcedRatio,
      double widowedRatio) {

    // 如果只有一个选项，直接返回
    if (availableStatuses.size() == 1) {
      return availableStatuses.get(0);
    }

    // 构建权重映射
    Map<MaritalStatus, Double> weights = new HashMap<>();
    double singleRatio = 1.0 - marriedRatio - divorcedRatio - widowedRatio;
    singleRatio = Math.max(0.0, singleRatio); // 确保不为负数

    for (MaritalStatus status : availableStatuses) {
      switch (status) {
        case SINGLE:
          weights.put(status, singleRatio);
          break;
        case MARRIED:
          weights.put(status, marriedRatio);
          break;
        case DIVORCED:
          weights.put(status, divorcedRatio);
          break;
        case WIDOWED:
          weights.put(status, widowedRatio);
          break;
      }
    }

    // 计算总权重
    double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();

    if (totalWeight <= 0) {
      // 如果权重为0，使用现实分布
      return selectWithRealisticWeights(availableStatuses);
    }

    // 随机选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0.0;

    for (MaritalStatus status : availableStatuses) {
      currentWeight += weights.getOrDefault(status, 0.0);
      if (randomValue <= currentWeight) {
        return status;
      }
    }

    // 默认返回第一个
    return availableStatuses.get(0);
  }

  private MaritalStatus selectWithRealisticWeights(List<MaritalStatus> availableStatuses) {
    // 计算总权重
    double totalWeight = 0.0;
    for (MaritalStatus status : availableStatuses) {
      totalWeight += REALISTIC_WEIGHTS.getOrDefault(status, 0.0);
    }

    // 随机选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0.0;

    for (MaritalStatus status : availableStatuses) {
      currentWeight += REALISTIC_WEIGHTS.getOrDefault(status, 0.0);
      if (randomValue <= currentWeight) {
        return status;
      }
    }

    // 默认返回第一个
    return availableStatuses.get(0);
  }

  private String formatMaritalStatus(MaritalStatus status, String format) {
    switch (format.toUpperCase()) {
      case "CHINESE":
      case "CN":
        return CHINESE_NAMES.get(status);

      case "ENGLISH":
      case "EN":
        return ENGLISH_NAMES.get(status);

      case "CODE":
        return CODE_NAMES.get(status);

      default:
        logger.warn("Unknown marital status format: {}. Using CHINESE format.", format);
        return CHINESE_NAMES.get(status);
    }
  }
}
