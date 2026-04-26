package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 宗教信仰生成器
 *
 * <p>支持的参数： - type: 宗教类型 (BUDDHISM|CHRISTIANITY|ISLAM|TAOISM|NONE|ANY) - none_ratio: 无信仰者占比
 * (0.0-1.0) - file: 自定义宗教列表文件路径 - weights: 宗教权重配置 (如 "无:70,佛教:15,基督教:10") - format: 输出格式
 * (CHINESE|ENGLISH|CODE)
 *
 * @author DataForge
 */
public class ReligionGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(ReligionGenerator.class);
  private static final Random random = new Random();

  // 宗教信仰枚举
  private enum Religion {
    NONE, // 无信仰
    BUDDHISM, // 佛教
    CHRISTIANITY, // 基督教
    ISLAM, // 伊斯兰教
    TAOISM, // 道教
    CONFUCIANISM, // 儒教
    HINDUISM, // 印度教
    JUDAISM, // 犹太教
    FOLK_BELIEF, // 民间信仰
    OTHER // 其他
  }

  // 宗教信息类
  private static class ReligionInfo {
    final Religion religion;
    final String chineseName;
    final String englishName;
    final String code;

    ReligionInfo(Religion religion, String chineseName, String englishName, String code) {
      this.religion = religion;
      this.chineseName = chineseName;
      this.englishName = englishName;
      this.code = code;
    }
  }

  // 宗教信息映射
  private static final Map<Religion, ReligionInfo> RELIGION_INFO = new HashMap<>();

  // 现实分布权重（基于中国宗教信仰调查数据）
  private static final Map<Religion, Double> REALISTIC_WEIGHTS = new HashMap<>();

  static {
    initializeReligionInfo();
    initializeRealisticWeights();
  }

  private static void initializeReligionInfo() {
    RELIGION_INFO.put(Religion.NONE, new ReligionInfo(Religion.NONE, "无", "None", "00"));
    RELIGION_INFO.put(
        Religion.BUDDHISM, new ReligionInfo(Religion.BUDDHISM, "佛教", "Buddhism", "01"));
    RELIGION_INFO.put(
        Religion.CHRISTIANITY,
        new ReligionInfo(Religion.CHRISTIANITY, "基督教", "Christianity", "02"));
    RELIGION_INFO.put(Religion.ISLAM, new ReligionInfo(Religion.ISLAM, "伊斯兰教", "Islam", "03"));
    RELIGION_INFO.put(Religion.TAOISM, new ReligionInfo(Religion.TAOISM, "道教", "Taoism", "04"));
    RELIGION_INFO.put(
        Religion.CONFUCIANISM, new ReligionInfo(Religion.CONFUCIANISM, "儒教", "Confucianism", "05"));
    RELIGION_INFO.put(
        Religion.HINDUISM, new ReligionInfo(Religion.HINDUISM, "印度教", "Hinduism", "06"));
    RELIGION_INFO.put(Religion.JUDAISM, new ReligionInfo(Religion.JUDAISM, "犹太教", "Judaism", "07"));
    RELIGION_INFO.put(
        Religion.FOLK_BELIEF, new ReligionInfo(Religion.FOLK_BELIEF, "民间信仰", "Folk Belief", "08"));
    RELIGION_INFO.put(Religion.OTHER, new ReligionInfo(Religion.OTHER, "其他", "Other", "99"));
  }

  private static void initializeRealisticWeights() {
    // 基于中国宗教信仰状况的近似数据
    REALISTIC_WEIGHTS.put(Religion.NONE, 70.0); // 70% 无信仰
    REALISTIC_WEIGHTS.put(Religion.BUDDHISM, 15.0); // 15% 佛教
    REALISTIC_WEIGHTS.put(Religion.FOLK_BELIEF, 8.0); // 8% 民间信仰
    REALISTIC_WEIGHTS.put(Religion.CHRISTIANITY, 3.0); // 3% 基督教
    REALISTIC_WEIGHTS.put(Religion.TAOISM, 2.0); // 2% 道教
    REALISTIC_WEIGHTS.put(Religion.ISLAM, 1.5); // 1.5% 伊斯兰教
    REALISTIC_WEIGHTS.put(Religion.CONFUCIANISM, 0.3); // 0.3% 儒教
    REALISTIC_WEIGHTS.put(Religion.HINDUISM, 0.1); // 0.1% 印度教
    REALISTIC_WEIGHTS.put(Religion.JUDAISM, 0.05); // 0.05% 犹太教
    REALISTIC_WEIGHTS.put(Religion.OTHER, 0.05); // 0.05% 其他
  }

  @Override
  public String getType() {
    return "religion";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String type = config.getParam("type", String.class, "ANY");
      double noneRatio = Double.parseDouble(config.getParam("none_ratio", String.class, "0.7"));
      String weightsParam = config.getParam("weights", String.class, null);
      String format = config.getParam("format", String.class, "CHINESE");

      // 加载宗教数据
      List<Religion> religions = loadReligions(config, type);

      // 根据权重选择宗教
      Religion religion = selectReligion(religions, noneRatio, weightsParam);

      // 将宗教信息存入上下文
      context.put("religion", religion.name());
      context.put("has_religion", religion != Religion.NONE ? "true" : "false");

      // 格式化输出
      String result = formatReligion(religion, format);

      logger.debug("Generated religion: {}", result);
      return result;

    } catch (Exception e) {
      logger.error("Error generating religion", e);
      return "无";
    }
  }

  private List<Religion> loadReligions(FieldConfig config, String type) {
    String customFile = config.getParam("file", String.class, null);
    if (customFile != null) {
      try {
        List<String> customReligions = DataLoader.loadDataFromFile(customFile);
        List<Religion> religions = new ArrayList<>();

        for (String religionName : customReligions) {
          // 尝试匹配已知宗教
          Religion religion = findReligionByName(religionName);
          if (religion != null) {
            religions.add(religion);
          }
        }

        if (!religions.isEmpty()) {
          return religions;
        }
      } catch (Exception e) {
        logger.warn("Failed to load custom religion file: {}", customFile, e);
      }
    }

    // 使用内置宗教数据
    List<Religion> religions = new ArrayList<>();

    switch (type.toUpperCase()) {
      case "BUDDHISM":
        religions.add(Religion.BUDDHISM);
        break;

      case "CHRISTIANITY":
        religions.add(Religion.CHRISTIANITY);
        break;

      case "ISLAM":
        religions.add(Religion.ISLAM);
        break;

      case "TAOISM":
        religions.add(Religion.TAOISM);
        break;

      case "NONE":
        religions.add(Religion.NONE);
        break;

      case "ANY":
      default:
        religions.addAll(Arrays.asList(Religion.values()));
        break;
    }

    return religions;
  }

  private Religion findReligionByName(String name) {
    for (ReligionInfo info : RELIGION_INFO.values()) {
      if (info.chineseName.equals(name) || info.englishName.equalsIgnoreCase(name)) {
        return info.religion;
      }
    }
    return null;
  }

  private Religion selectReligion(List<Religion> religions, double noneRatio, String weightsParam) {
    // 如果只有一个选项，直接返回
    if (religions.size() == 1) {
      return religions.get(0);
    }

    // 如果有自定义权重，使用自定义权重
    if (weightsParam != null && !weightsParam.isEmpty()) {
      return selectWithCustomWeights(religions, weightsParam);
    }

    // 如果指定了无信仰比例且包含无信仰选项
    if (religions.contains(Religion.NONE)) {
      double randomValue = random.nextDouble();
      if (randomValue < noneRatio) {
        return Religion.NONE;
      } else {
        // 从有信仰的宗教中选择
        List<Religion> religiousOptions = new ArrayList<>(religions);
        religiousOptions.remove(Religion.NONE);
        if (!religiousOptions.isEmpty()) {
          return selectWithRealisticWeights(religiousOptions);
        }
      }
    }

    // 使用现实分布权重
    return selectWithRealisticWeights(religions);
  }

  private Religion selectWithRealisticWeights(List<Religion> religions) {
    // 计算总权重
    double totalWeight = 0.0;
    for (Religion religion : religions) {
      totalWeight += REALISTIC_WEIGHTS.getOrDefault(religion, 0.1);
    }

    if (totalWeight <= 0) {
      // 如果没有权重，使用均匀分布
      return religions.get(random.nextInt(religions.size()));
    }

    // 随机选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0.0;

    for (Religion religion : religions) {
      currentWeight += REALISTIC_WEIGHTS.getOrDefault(religion, 0.1);
      if (randomValue <= currentWeight) {
        return religion;
      }
    }

    // 默认返回第一个
    return religions.get(0);
  }

  private Religion selectWithCustomWeights(List<Religion> religions, String weightsParam) {
    Map<String, Integer> customWeights = parseWeights(weightsParam);

    // 计算总权重
    double totalWeight = 0.0;
    for (Religion religion : religions) {
      ReligionInfo info = RELIGION_INFO.get(religion);
      int weight =
          customWeights.getOrDefault(
              info.chineseName, customWeights.getOrDefault(info.englishName, 1));
      totalWeight += weight;
    }

    // 随机选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0.0;

    for (Religion religion : religions) {
      ReligionInfo info = RELIGION_INFO.get(religion);
      int weight =
          customWeights.getOrDefault(
              info.chineseName, customWeights.getOrDefault(info.englishName, 1));
      currentWeight += weight;
      if (randomValue <= currentWeight) {
        return religion;
      }
    }

    // 默认返回第一个
    return religions.get(0);
  }

  private Map<String, Integer> parseWeights(String weightsParam) {
    Map<String, Integer> weights = new HashMap<>();

    try {
      String[] pairs = weightsParam.split(",");
      for (String pair : pairs) {
        String[] parts = pair.split(":");
        if (parts.length == 2) {
          String religion = parts[0].trim();
          int weight = Integer.parseInt(parts[1].trim());
          weights.put(religion, weight);
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to parse weights: {}", weightsParam, e);
    }

    return weights;
  }

  private String formatReligion(Religion religion, String format) {
    ReligionInfo info = RELIGION_INFO.get(religion);

    switch (format.toUpperCase()) {
      case "CHINESE":
      case "CN":
        return info.chineseName;

      case "ENGLISH":
      case "EN":
        return info.englishName;

      case "CODE":
        return info.code;

      default:
        logger.warn("Unknown religion format: {}. Using CHINESE format.", format);
        return info.chineseName;
    }
  }
}
