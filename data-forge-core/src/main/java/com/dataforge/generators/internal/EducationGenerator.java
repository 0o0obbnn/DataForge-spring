package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 学历生成器
 *
 * <p>支持的参数： - levels: 学历层次范围 (PRIMARY|JUNIOR_HIGH|HIGH_SCHOOL|COLLEGE|BACHELOR|MASTER|PHD|ANY) -
 * distribution: 学历分布方式 (UNIFORM|WEIGHTED|REALISTIC) - weights: 自定义权重配置 (如 "本科:50,硕士:30,博士:10") -
 * age_related: 是否与年龄关联 (true|false)
 *
 * @author DataForge
 */
public class EducationGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(EducationGenerator.class);
  private static final Random random = new Random();

  // 学历层次定义
  private static final Map<String, String> EDUCATION_LEVELS = new LinkedHashMap<>();

  // 现实分布权重（基于中国教育统计）
  private static final Map<String, Integer> REALISTIC_WEIGHTS = new HashMap<>();

  // 年龄与学历的合理性映射
  private static final Map<String, Integer> MIN_AGE_FOR_EDUCATION = new HashMap<>();

  static {
    initializeEducationLevels();
    initializeRealisticWeights();
    initializeAgeMapping();
  }

  private static void initializeEducationLevels() {
    EDUCATION_LEVELS.put("PRIMARY", "小学");
    EDUCATION_LEVELS.put("JUNIOR_HIGH", "初中");
    EDUCATION_LEVELS.put("HIGH_SCHOOL", "高中");
    EDUCATION_LEVELS.put("COLLEGE", "大专");
    EDUCATION_LEVELS.put("BACHELOR", "本科");
    EDUCATION_LEVELS.put("MASTER", "硕士");
    EDUCATION_LEVELS.put("PHD", "博士");
  }

  private static void initializeRealisticWeights() {
    // 基于中国成年人口教育结构的近似分布
    REALISTIC_WEIGHTS.put("小学", 15);
    REALISTIC_WEIGHTS.put("初中", 25);
    REALISTIC_WEIGHTS.put("高中", 20);
    REALISTIC_WEIGHTS.put("大专", 15);
    REALISTIC_WEIGHTS.put("本科", 20);
    REALISTIC_WEIGHTS.put("硕士", 4);
    REALISTIC_WEIGHTS.put("博士", 1);
  }

  private static void initializeAgeMapping() {
    // 各学历层次的最小合理年龄
    MIN_AGE_FOR_EDUCATION.put("小学", 12);
    MIN_AGE_FOR_EDUCATION.put("初中", 15);
    MIN_AGE_FOR_EDUCATION.put("高中", 18);
    MIN_AGE_FOR_EDUCATION.put("大专", 20);
    MIN_AGE_FOR_EDUCATION.put("本科", 22);
    MIN_AGE_FOR_EDUCATION.put("硕士", 25);
    MIN_AGE_FOR_EDUCATION.put("博士", 28);
  }

  @Override
  public String getType() {
    return "education";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String levels = config.getParam("levels", String.class, "ANY");
      String distribution = config.getParam("distribution", String.class, "REALISTIC");
      String weightsParam = config.getParam("weights", String.class, null);
      boolean ageRelated =
          Boolean.parseBoolean(config.getParam("age_related", String.class, "true"));

      // 获取可选的学历列表
      List<String> availableEducations = getAvailableEducations(levels);

      // 如果启用年龄关联，根据年龄过滤学历
      if (ageRelated) {
        Integer age = context.get("age", Integer.class).orElse(null);
        if (age != null) {
          availableEducations = filterByAge(availableEducations, age);
        }
      }

      // 根据分布方式选择学历
      String education = selectEducation(availableEducations, distribution, weightsParam);

      logger.debug("Generated education: {}", education);
      return education;

    } catch (Exception e) {
      logger.error("Error generating education", e);
      return "本科";
    }
  }

  private List<String> getAvailableEducations(String levels) {
    List<String> educations = new ArrayList<>();

    if ("ANY".equals(levels)) {
      educations.addAll(EDUCATION_LEVELS.values());
    } else {
      String[] levelArray = levels.split("\\|");
      for (String level : levelArray) {
        String education = EDUCATION_LEVELS.get(level.trim());
        if (education != null) {
          educations.add(education);
        }
      }
    }

    // 如果没有找到合适的学历，使用默认列表
    if (educations.isEmpty()) {
      educations.addAll(EDUCATION_LEVELS.values());
    }

    return educations;
  }

  private List<String> filterByAge(List<String> educations, int age) {
    List<String> filteredEducations = new ArrayList<>();

    for (String education : educations) {
      Integer minAge = MIN_AGE_FOR_EDUCATION.get(education);
      if (minAge != null && age >= minAge) {
        filteredEducations.add(education);
      }
    }

    // 如果过滤后没有合适的学历，返回最低学历
    if (filteredEducations.isEmpty()) {
      if (age >= 12) {
        filteredEducations.add("小学");
      }
      if (age >= 15) {
        filteredEducations.add("初中");
      }
      if (age >= 18) {
        filteredEducations.add("高中");
      }
    }

    return filteredEducations.isEmpty() ? Arrays.asList("小学") : filteredEducations;
  }

  private String selectEducation(
      List<String> educations, String distribution, String weightsParam) {
    switch (distribution) {
      case "UNIFORM":
        return educations.get(random.nextInt(educations.size()));

      case "WEIGHTED":
        if (weightsParam != null && !weightsParam.isEmpty()) {
          return selectWithCustomWeights(educations, weightsParam);
        }
        // 如果没有自定义权重，使用现实分布
        return selectWithRealisticWeights(educations);

      case "REALISTIC":
      default:
        return selectWithRealisticWeights(educations);
    }
  }

  private String selectWithRealisticWeights(List<String> educations) {
    // 计算总权重
    int totalWeight = 0;
    for (String education : educations) {
      totalWeight += REALISTIC_WEIGHTS.getOrDefault(education, 1);
    }

    // 随机选择
    int randomValue = random.nextInt(totalWeight);
    int currentWeight = 0;

    for (String education : educations) {
      currentWeight += REALISTIC_WEIGHTS.getOrDefault(education, 1);
      if (randomValue < currentWeight) {
        return education;
      }
    }

    // 默认返回第一个
    return educations.get(0);
  }

  private String selectWithCustomWeights(List<String> educations, String weightsParam) {
    Map<String, Integer> customWeights = parseWeights(weightsParam);

    // 计算总权重
    int totalWeight = 0;
    for (String education : educations) {
      totalWeight += customWeights.getOrDefault(education, 1);
    }

    // 随机选择
    int randomValue = random.nextInt(totalWeight);
    int currentWeight = 0;

    for (String education : educations) {
      currentWeight += customWeights.getOrDefault(education, 1);
      if (randomValue < currentWeight) {
        return education;
      }
    }

    // 默认返回第一个
    return educations.get(0);
  }

  private Map<String, Integer> parseWeights(String weightsParam) {
    Map<String, Integer> weights = new HashMap<>();

    try {
      String[] pairs = weightsParam.split(",");
      for (String pair : pairs) {
        String[] parts = pair.split(":");
        if (parts.length == 2) {
          String education = parts[0].trim();
          int weight = Integer.parseInt(parts[1].trim());
          weights.put(education, weight);
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to parse weights: {}", weightsParam, e);
    }

    return weights;
  }
}
