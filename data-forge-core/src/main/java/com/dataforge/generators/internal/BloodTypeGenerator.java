package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 血型生成器
 *
 * <p>支持的参数： - group: 血型组别 (A|B|AB|O|ANY) - rh: Rh因子 (POSITIVE|NEGATIVE|ANY) - distribution: 分布方式
 * (UNIFORM|WEIGHTED|REALISTIC) - format: 输出格式 (STANDARD|FULL|CODE) - weights: 自定义权重配置 (如
 * "A:30,B:25,AB:5,O:40")
 *
 * @author DataForge
 */
public class BloodTypeGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(BloodTypeGenerator.class);
  private static final Random random = new Random();

  // 血型组别枚举
  private enum BloodGroup {
    A,
    B,
    AB,
    O
  }

  // Rh因子枚举
  private enum RhFactor {
    POSITIVE,
    NEGATIVE
  }

  // 血型信息类
  private static class BloodType {
    final BloodGroup group;
    final RhFactor rh;

    BloodType(BloodGroup group, RhFactor rh) {
      this.group = group;
      this.rh = rh;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      BloodType bloodType = (BloodType) obj;
      return group == bloodType.group && rh == bloodType.rh;
    }

    @Override
    public int hashCode() {
      return Objects.hash(group, rh);
    }
  }

  // 现实分布权重（基于中国人口血型分布）
  private static final Map<BloodType, Double> REALISTIC_WEIGHTS = new HashMap<>();

  static {
    initializeRealisticWeights();
  }

  private static void initializeRealisticWeights() {
    // 基于中国人口血型分布的近似数据
    // ABO血型分布：A型28%, B型24%, AB型9%, O型39%
    // Rh阳性约99.7%, Rh阴性约0.3%

    REALISTIC_WEIGHTS.put(new BloodType(BloodGroup.A, RhFactor.POSITIVE), 0.279); // A+ 27.9%
    REALISTIC_WEIGHTS.put(new BloodType(BloodGroup.A, RhFactor.NEGATIVE), 0.001); // A- 0.1%
    REALISTIC_WEIGHTS.put(new BloodType(BloodGroup.B, RhFactor.POSITIVE), 0.239); // B+ 23.9%
    REALISTIC_WEIGHTS.put(new BloodType(BloodGroup.B, RhFactor.NEGATIVE), 0.001); // B- 0.1%
    REALISTIC_WEIGHTS.put(new BloodType(BloodGroup.AB, RhFactor.POSITIVE), 0.089); // AB+ 8.9%
    REALISTIC_WEIGHTS.put(new BloodType(BloodGroup.AB, RhFactor.NEGATIVE), 0.001); // AB- 0.1%
    REALISTIC_WEIGHTS.put(new BloodType(BloodGroup.O, RhFactor.POSITIVE), 0.389); // O+ 38.9%
    REALISTIC_WEIGHTS.put(new BloodType(BloodGroup.O, RhFactor.NEGATIVE), 0.001); // O- 0.1%
  }

  @Override
  public String getType() {
    return "bloodtype";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String group = config.getParam("group", String.class, "ANY");
      String rh = config.getParam("rh", String.class, "ANY");
      String distribution = config.getParam("distribution", String.class, "REALISTIC");
      String format = config.getParam("format", String.class, "STANDARD");
      String weightsParam = config.getParam("weights", String.class, null);

      // 获取可选的血型列表
      List<BloodType> availableBloodTypes = getAvailableBloodTypes(group, rh);

      // 根据分布方式选择血型
      BloodType bloodType = selectBloodType(availableBloodTypes, distribution, weightsParam);

      // 将血型信息存入上下文
      context.put("blood_group", bloodType.group.name());
      context.put("rh_factor", bloodType.rh.name());

      // 格式化输出
      String result = formatBloodType(bloodType, format);

      logger.debug("Generated blood type: {}", result);
      return result;

    } catch (Exception e) {
      logger.error("Error generating blood type", e);
      return "A+";
    }
  }

  private List<BloodType> getAvailableBloodTypes(String group, String rh) {
    List<BloodType> bloodTypes = new ArrayList<>();

    // 确定血型组别
    List<BloodGroup> groups = new ArrayList<>();
    if ("ANY".equals(group)) {
      groups.addAll(Arrays.asList(BloodGroup.values()));
    } else {
      try {
        BloodGroup specificGroup = BloodGroup.valueOf(group);
        groups.add(specificGroup);
      } catch (IllegalArgumentException e) {
        logger.warn("Unknown blood group: {}. Using all groups.", group);
        groups.addAll(Arrays.asList(BloodGroup.values()));
      }
    }

    // 确定Rh因子
    List<RhFactor> rhFactors = new ArrayList<>();
    if ("ANY".equals(rh)) {
      rhFactors.addAll(Arrays.asList(RhFactor.values()));
    } else {
      try {
        RhFactor specificRh = RhFactor.valueOf(rh);
        rhFactors.add(specificRh);
      } catch (IllegalArgumentException e) {
        logger.warn("Unknown Rh factor: {}. Using all factors.", rh);
        rhFactors.addAll(Arrays.asList(RhFactor.values()));
      }
    }

    // 组合血型组别和Rh因子
    for (BloodGroup bg : groups) {
      for (RhFactor rf : rhFactors) {
        bloodTypes.add(new BloodType(bg, rf));
      }
    }

    return bloodTypes;
  }

  private BloodType selectBloodType(
      List<BloodType> availableBloodTypes, String distribution, String weightsParam) {
    switch (distribution.toUpperCase()) {
      case "UNIFORM":
        return availableBloodTypes.get(random.nextInt(availableBloodTypes.size()));

      case "WEIGHTED":
        if (weightsParam != null && !weightsParam.isEmpty()) {
          return selectWithCustomWeights(availableBloodTypes, weightsParam);
        }
        // 如果没有自定义权重，使用现实分布
        return selectWithRealisticWeights(availableBloodTypes);

      case "REALISTIC":
      default:
        return selectWithRealisticWeights(availableBloodTypes);
    }
  }

  private BloodType selectWithRealisticWeights(List<BloodType> availableBloodTypes) {
    // 计算总权重
    double totalWeight = 0.0;
    for (BloodType bloodType : availableBloodTypes) {
      totalWeight += REALISTIC_WEIGHTS.getOrDefault(bloodType, 0.0);
    }

    if (totalWeight <= 0) {
      // 如果没有权重，使用均匀分布
      return availableBloodTypes.get(random.nextInt(availableBloodTypes.size()));
    }

    // 随机选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0.0;

    for (BloodType bloodType : availableBloodTypes) {
      currentWeight += REALISTIC_WEIGHTS.getOrDefault(bloodType, 0.0);
      if (randomValue <= currentWeight) {
        return bloodType;
      }
    }

    // 默认返回第一个
    return availableBloodTypes.get(0);
  }

  private BloodType selectWithCustomWeights(
      List<BloodType> availableBloodTypes, String weightsParam) {
    Map<String, Integer> customWeights = parseWeights(weightsParam);

    // 计算总权重
    double totalWeight = 0.0;
    for (BloodType bloodType : availableBloodTypes) {
      String key = bloodType.group.name();
      totalWeight += customWeights.getOrDefault(key, 1);
    }

    // 随机选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0.0;

    for (BloodType bloodType : availableBloodTypes) {
      String key = bloodType.group.name();
      currentWeight += customWeights.getOrDefault(key, 1);
      if (randomValue <= currentWeight) {
        return bloodType;
      }
    }

    // 默认返回第一个
    return availableBloodTypes.get(0);
  }

  private Map<String, Integer> parseWeights(String weightsParam) {
    Map<String, Integer> weights = new HashMap<>();

    try {
      String[] pairs = weightsParam.split(",");
      for (String pair : pairs) {
        String[] parts = pair.split(":");
        if (parts.length == 2) {
          String bloodGroup = parts[0].trim().toUpperCase();
          int weight = Integer.parseInt(parts[1].trim());
          weights.put(bloodGroup, weight);
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to parse weights: {}", weightsParam, e);
    }

    return weights;
  }

  private String formatBloodType(BloodType bloodType, String format) {
    switch (format.toUpperCase()) {
      case "STANDARD":
        // 标准格式：A+, B-, AB+, O-
        String rhSymbol = bloodType.rh == RhFactor.POSITIVE ? "+" : "-";
        return bloodType.group.name() + rhSymbol;

      case "FULL":
        // 完整格式：A型Rh阳性, B型Rh阴性
        String groupName = bloodType.group.name() + "型";
        String rhName = bloodType.rh == RhFactor.POSITIVE ? "Rh阳性" : "Rh阴性";
        return groupName + rhName;

      case "CODE":
        // 代码格式：AP, BN, ABP, ON
        String rhCode = bloodType.rh == RhFactor.POSITIVE ? "P" : "N";
        return bloodType.group.name() + rhCode;

      default:
        logger.warn("Unknown blood type format: {}. Using STANDARD format.", format);
        String defaultRhSymbol = bloodType.rh == RhFactor.POSITIVE ? "+" : "-";
        return bloodType.group.name() + defaultRhSymbol;
    }
  }
}
