package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 枚举生成器
 *
 * <p>从用户提供的值列表中随机选择，支持权重分布， 用于状态枚举、分类选择、选项列表等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>values: 枚举值列表，逗号分隔 (必需)
 *   <li>weights: 权重列表，逗号分隔，与values对应
 *   <li>case_style: 大小写风格 (LOWER|UPPER|TITLE|ORIGINAL) 默认: ORIGINAL
 *   <li>allow_empty: 是否允许空值 默认: false
 *   <li>empty_ratio: 空值概率 (0.0-1.0) 默认: 0.1
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class EnumGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(EnumGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 大小写风格枚举
  public enum CaseStyle {
    LOWER("小写"),
    UPPER("大写"),
    TITLE("首字母大写"),
    ORIGINAL("保持原样");

    private final String description;

    CaseStyle(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @Override
  public String getType() {
    return "enum";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 检查是否允许空值
      boolean allowEmpty = getBooleanParam(config, "allow_empty", false);
      if (allowEmpty) {
        double emptyRatio = getDoubleParam(config, "empty_ratio", 0.1);
        if (random.nextDouble() < emptyRatio) {
          return "";
        }
      }

      // 获取枚举值列表
      String valuesStr = getStringParam(config, "values", null);
      if (valuesStr == null || valuesStr.trim().isEmpty()) {
        logger.warn("No values provided for enum generator, returning default value");
        return "DEFAULT";
      }

      List<String> values = parseValues(valuesStr);
      if (values.isEmpty()) {
        logger.warn("Empty values list for enum generator, returning default value");
        return "DEFAULT";
      }

      // 获取权重列表
      String weightsStr = getStringParam(config, "weights", null);
      String selectedValue;

      if (weightsStr != null && !weightsStr.trim().isEmpty()) {
        selectedValue = selectWithWeights(values, weightsStr);
      } else {
        selectedValue = selectRandomly(values);
      }

      // 应用大小写风格
      String caseStyleStr = getStringParam(config, "case_style", "ORIGINAL");
      CaseStyle caseStyle = parseCaseStyle(caseStyleStr);

      return applyCaseStyle(selectedValue, caseStyle);

    } catch (Exception e) {
      logger.error("Failed to generate enum value", e);
      return "DEFAULT";
    }
  }

  /** 解析值列表 */
  private List<String> parseValues(String valuesStr) {
    return Arrays.stream(valuesStr.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  /** 随机选择值 */
  private String selectRandomly(List<String> values) {
    return values.get(random.nextInt(values.size()));
  }

  /** 根据权重选择值 */
  private String selectWithWeights(List<String> values, String weightsStr) {
    List<Double> weights = parseWeights(weightsStr);

    // 如果权重数量与值数量不匹配，使用随机选择
    if (weights.size() != values.size()) {
      logger.warn(
          "Weights count ({}) doesn't match values count ({}), using random selection",
          weights.size(),
          values.size());
      return selectRandomly(values);
    }

    // 计算权重总和
    double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();
    if (totalWeight <= 0) {
      logger.warn("Total weight is zero or negative, using random selection");
      return selectRandomly(values);
    }

    // 根据权重选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0;

    for (int i = 0; i < values.size(); i++) {
      currentWeight += weights.get(i);
      if (randomValue <= currentWeight) {
        return values.get(i);
      }
    }

    // 如果没有选中（理论上不应该发生），返回最后一个值
    return values.get(values.size() - 1);
  }

  /** 解析权重列表 */
  private List<Double> parseWeights(String weightsStr) {
    return Arrays.stream(weightsStr.split(","))
        .map(String::trim)
        .map(
            s -> {
              try {
                return Double.parseDouble(s);
              } catch (NumberFormatException e) {
                logger.warn("Invalid weight value: {}, using 1.0 as default", s);
                return 1.0;
              }
            })
        .collect(Collectors.toList());
  }

  /** 解析大小写风格 */
  private CaseStyle parseCaseStyle(String caseStyleStr) {
    try {
      return CaseStyle.valueOf(caseStyleStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid case style: {}, using ORIGINAL as default", caseStyleStr);
      return CaseStyle.ORIGINAL;
    }
  }

  /** 应用大小写风格 */
  private String applyCaseStyle(String text, CaseStyle caseStyle) {
    if (text == null || text.isEmpty()) {
      return text;
    }

    switch (caseStyle) {
      case LOWER:
        return text.toLowerCase();
      case UPPER:
        return text.toUpperCase();
      case TITLE:
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
      case ORIGINAL:
      default:
        return text;
    }
  }
}
