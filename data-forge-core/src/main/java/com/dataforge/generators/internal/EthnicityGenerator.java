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
 * 民族生成器
 *
 * <p>支持的参数： - type: 民族类型 (HAN|MINOR|ANY) - han_ratio: 汉族占比 (0.0-1.0) - file: 自定义民族列表文件路径 - weights:
 * 民族权重配置 (如 "汉族:90,维吾尔族:3,回族:2") - format: 输出格式 (CHINESE|CODE)
 *
 * @author DataForge
 */
public class EthnicityGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(EthnicityGenerator.class);
  private static final Random random = new Random();

  // 中国56个民族列表（按人口数量排序）
  private static final List<String> CHINESE_ETHNICITIES =
      Arrays.asList(
          "汉族", "壮族", "维吾尔族", "回族", "满族", "苗族", "彝族", "土家族", "藏族", "蒙古族", "侗族", "布依族", "瑶族", "白族",
          "朝鲜族", "哈尼族", "哈萨克族", "黎族", "傣族", "畲族", "傈僳族", "东乡族", "仡佬族", "拉祜族", "水族", "佤族", "纳西族",
          "羌族", "土族", "仫佬族", "锡伯族", "柯尔克孜族", "景颇族", "达斡尔族", "撒拉族", "布朗族", "毛南族", "塔吉克族", "普米族",
          "阿昌族", "怒族", "鄂温克族", "京族", "基诺族", "德昂族", "保安族", "俄罗斯族", "裕固族", "乌孜别克族", "门巴族", "鄂伦春族",
          "独龙族", "塔塔尔族", "赫哲族", "高山族", "珞巴族");

  // 民族代码映射（按国家标准）
  private static final Map<String, String> ETHNICITY_CODES = new HashMap<>();

  // 现实分布权重（基于中国人口普查数据）
  private static final Map<String, Double> REALISTIC_WEIGHTS = new HashMap<>();

  static {
    initializeEthnicityCodes();
    initializeRealisticWeights();
  }

  private static void initializeEthnicityCodes() {
    // 主要民族的标准代码
    ETHNICITY_CODES.put("汉族", "01");
    ETHNICITY_CODES.put("蒙古族", "02");
    ETHNICITY_CODES.put("回族", "03");
    ETHNICITY_CODES.put("藏族", "04");
    ETHNICITY_CODES.put("维吾尔族", "05");
    ETHNICITY_CODES.put("苗族", "06");
    ETHNICITY_CODES.put("彝族", "07");
    ETHNICITY_CODES.put("壮族", "08");
    ETHNICITY_CODES.put("布依族", "09");
    ETHNICITY_CODES.put("朝鲜族", "10");
    ETHNICITY_CODES.put("满族", "11");
    ETHNICITY_CODES.put("侗族", "12");
    ETHNICITY_CODES.put("瑶族", "13");
    ETHNICITY_CODES.put("白族", "14");
    ETHNICITY_CODES.put("土家族", "15");
    ETHNICITY_CODES.put("哈尼族", "16");
    ETHNICITY_CODES.put("哈萨克族", "17");
    ETHNICITY_CODES.put("傣族", "18");
    ETHNICITY_CODES.put("黎族", "19");
    ETHNICITY_CODES.put("傈僳族", "20");
    // 其他民族使用递增编号
    for (int i = 0; i < CHINESE_ETHNICITIES.size(); i++) {
      String ethnicity = CHINESE_ETHNICITIES.get(i);
      if (!ETHNICITY_CODES.containsKey(ethnicity)) {
        ETHNICITY_CODES.put(ethnicity, String.format("%02d", i + 1));
      }
    }
  }

  private static void initializeRealisticWeights() {
    // 基于中国第七次人口普查数据的近似分布
    REALISTIC_WEIGHTS.put("汉族", 91.11); // 91.11%
    REALISTIC_WEIGHTS.put("壮族", 1.27); // 1.27%
    REALISTIC_WEIGHTS.put("维吾尔族", 0.79); // 0.79%
    REALISTIC_WEIGHTS.put("回族", 0.79); // 0.79%
    REALISTIC_WEIGHTS.put("满族", 0.78); // 0.78%
    REALISTIC_WEIGHTS.put("苗族", 0.67); // 0.67%
    REALISTIC_WEIGHTS.put("彝族", 0.65); // 0.65%
    REALISTIC_WEIGHTS.put("土家族", 0.62); // 0.62%
    REALISTIC_WEIGHTS.put("藏族", 0.47); // 0.47%
    REALISTIC_WEIGHTS.put("蒙古族", 0.45); // 0.45%

    // 其他民族使用较小的权重
    for (String ethnicity : CHINESE_ETHNICITIES) {
      if (!REALISTIC_WEIGHTS.containsKey(ethnicity)) {
        REALISTIC_WEIGHTS.put(ethnicity, 0.1); // 其他民族各占约0.1%
      }
    }
  }

  @Override
  public String getType() {
    return "ethnicity";
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
      double hanRatio = Double.parseDouble(config.getParam("han_ratio", String.class, "0.91"));
      String weightsParam = config.getParam("weights", String.class, null);
      String format = config.getParam("format", String.class, "CHINESE");

      // 加载民族数据
      List<String> ethnicities = loadEthnicities(config, type);

      // 根据权重选择民族
      String ethnicity = selectEthnicity(ethnicities, type, hanRatio, weightsParam);

      // 将民族信息存入上下文
      context.put("ethnicity", ethnicity);
      context.put("is_han", "汉族".equals(ethnicity) ? "true" : "false");

      // 格式化输出
      String result = formatEthnicity(ethnicity, format);

      logger.debug("Generated ethnicity: {}", result);
      return result;

    } catch (Exception e) {
      logger.error("Error generating ethnicity", e);
      return "汉族";
    }
  }

  private List<String> loadEthnicities(FieldConfig config, String type) {
    String customFile = config.getParam("file", String.class, null);
    if (customFile != null) {
      try {
        return DataLoader.loadDataFromFile(customFile);
      } catch (Exception e) {
        logger.warn("Failed to load custom ethnicity file: {}", customFile, e);
      }
    }

    // 使用内置民族数据
    List<String> ethnicities = new ArrayList<>();

    switch (type.toUpperCase()) {
      case "HAN":
        ethnicities.add("汉族");
        break;

      case "MINOR":
        // 少数民族（除汉族外的所有民族）
        for (String ethnicity : CHINESE_ETHNICITIES) {
          if (!"汉族".equals(ethnicity)) {
            ethnicities.add(ethnicity);
          }
        }
        break;

      case "ANY":
      default:
        ethnicities.addAll(CHINESE_ETHNICITIES);
        break;
    }

    return ethnicities;
  }

  private String selectEthnicity(
      List<String> ethnicities, String type, double hanRatio, String weightsParam) {
    // 如果只有一个选项，直接返回
    if (ethnicities.size() == 1) {
      return ethnicities.get(0);
    }

    // 如果有自定义权重，使用自定义权重
    if (weightsParam != null && !weightsParam.isEmpty()) {
      return selectWithCustomWeights(ethnicities, weightsParam);
    }

    // 如果指定了汉族比例且包含汉族
    if (ethnicities.contains("汉族") && !"MINOR".equals(type.toUpperCase())) {
      double randomValue = random.nextDouble();
      if (randomValue < hanRatio) {
        return "汉族";
      } else {
        // 从少数民族中选择
        List<String> minorities = new ArrayList<>(ethnicities);
        minorities.remove("汉族");
        if (!minorities.isEmpty()) {
          return selectWithRealisticWeights(minorities);
        }
      }
    }

    // 使用现实分布权重
    return selectWithRealisticWeights(ethnicities);
  }

  private String selectWithRealisticWeights(List<String> ethnicities) {
    // 计算总权重
    double totalWeight = 0.0;
    for (String ethnicity : ethnicities) {
      totalWeight += REALISTIC_WEIGHTS.getOrDefault(ethnicity, 0.1);
    }

    if (totalWeight <= 0) {
      // 如果没有权重，使用均匀分布
      return ethnicities.get(random.nextInt(ethnicities.size()));
    }

    // 随机选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0.0;

    for (String ethnicity : ethnicities) {
      currentWeight += REALISTIC_WEIGHTS.getOrDefault(ethnicity, 0.1);
      if (randomValue <= currentWeight) {
        return ethnicity;
      }
    }

    // 默认返回第一个
    return ethnicities.get(0);
  }

  private String selectWithCustomWeights(List<String> ethnicities, String weightsParam) {
    Map<String, Integer> customWeights = parseWeights(weightsParam);

    // 计算总权重
    double totalWeight = 0.0;
    for (String ethnicity : ethnicities) {
      totalWeight += customWeights.getOrDefault(ethnicity, 1);
    }

    // 随机选择
    double randomValue = random.nextDouble() * totalWeight;
    double currentWeight = 0.0;

    for (String ethnicity : ethnicities) {
      currentWeight += customWeights.getOrDefault(ethnicity, 1);
      if (randomValue <= currentWeight) {
        return ethnicity;
      }
    }

    // 默认返回第一个
    return ethnicities.get(0);
  }

  private Map<String, Integer> parseWeights(String weightsParam) {
    Map<String, Integer> weights = new HashMap<>();

    try {
      String[] pairs = weightsParam.split(",");
      for (String pair : pairs) {
        String[] parts = pair.split(":");
        if (parts.length == 2) {
          String ethnicity = parts[0].trim();
          int weight = Integer.parseInt(parts[1].trim());
          weights.put(ethnicity, weight);
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to parse weights: {}", weightsParam, e);
    }

    return weights;
  }

  private String formatEthnicity(String ethnicity, String format) {
    switch (format.toUpperCase()) {
      case "CHINESE":
      case "CN":
        return ethnicity;

      case "CODE":
        return ETHNICITY_CODES.getOrDefault(ethnicity, "99");

      default:
        logger.warn("Unknown ethnicity format: {}. Using CHINESE format.", format);
        return ethnicity;
    }
  }
}
