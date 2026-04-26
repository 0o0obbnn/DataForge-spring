package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 度量单位生成器
 *
 * <p>根据DataForge设计文档要求，生成各种度量单位用于测试度量系统。 支持长度、质量、时间、电流、温度、物质的量、发光强度等国际单位制（SI）基本单位， 以及常用的导出单位和自定义单位。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class MeasurementUnitGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(MeasurementUnitGenerator.class);

  // SI基本单位
  private static final List<String> SI_BASE_UNITS =
      Arrays.asList(
          "m", // 长度：米
          "kg", // 质量：千克
          "s", // 时间：秒
          "A", // 电流：安培
          "K", // 温度：开尔文
          "mol", // 物质的量：摩尔
          "cd" // 发光强度：坎德拉
          );

  // 常用导出单位
  private static final List<String> DERIVED_UNITS =
      Arrays.asList(
          "N", // 力：牛顿 (kg⋅m/s²)
          "Pa", // 压力：帕斯卡 (N/m²)
          "J", // 能量：焦耳 (N⋅m)
          "W", // 功率：瓦特 (J/s)
          "C", // 电荷：库仑 (A⋅s)
          "V", // 电压：伏特 (W/A)
          "F", // 电容：法拉 (C/V)
          "Ω", // 电阻：欧姆 (V/A)
          "S", // 电导：西门子 (A/V)
          "Wb", // 磁通量：韦伯 (V⋅s)
          "T", // 磁感应强度：特斯拉 (Wb/m²)
          "H", // 电感：亨利 (Wb/A)
          "lm", // 光通量：流明 (cd⋅sr)
          "lx", // 照度：勒克斯 (lm/m²)
          "Bq", // 放射性活度：贝克勒尔 (1/s)
          "Gy", // 吸收剂量：戈瑞 (J/kg)
          "Sv", // 剂量当量：希沃特 (J/kg)
          "kat" // 催化活性：卡塔尔 (mol/s)
          );

  // 常用前缀单位
  private static final List<String> PREFIXED_UNITS =
      Arrays.asList(
          "mm",
          "cm",
          "dm",
          "km", // 长度前缀
          "mg",
          "g",
          "t", // 质量前缀
          "ms",
          "min",
          "h",
          "day", // 时间前缀
          "mA",
          "kA", // 电流前缀
          "mK",
          "°C",
          "°F", // 温度前缀
          "mmol",
          "kmol", // 物质的量前缀
          "mcd",
          "kcd" // 发光强度前缀
          );

  // 自定义常用单位
  private static final List<String> CUSTOMARY_UNITS =
      Arrays.asList(
          "in",
          "ft",
          "yd",
          "mi", // 英制长度
          "oz",
          "lb",
          "ton", // 英制质量
          "acre",
          "ha", // 面积
          "gal",
          "L",
          "mL", // 体积
          "mph",
          "knot", // 速度
          "cal",
          "kcal", // 能量
          "hp", // 功率
          "psi", // 压力
          "Hz", // 频率
          "%" // 百分比
          );

  // 所有单位集合
  private static final List<String> ALL_UNITS =
      Arrays.asList(
          "m", "kg", "s", "A", "K", "mol", "cd", "N", "Pa", "J", "W", "C", "V", "F", "Ω", "S", "Wb",
          "T", "H", "lm", "lx", "Bq", "Gy", "Sv", "kat", "mm", "cm", "dm", "km", "mg", "g", "t",
          "ms", "min", "h", "day", "mA", "kA", "mK", "°C", "°F", "mmol", "kmol", "mcd", "kcd", "in",
          "ft", "yd", "mi", "oz", "lb", "ton", "acre", "ha", "gal", "L", "mL", "mph", "knot", "cal",
          "kcal", "hp", "psi", "Hz", "%");

  @Override
  public String getType() {
    return "measurement_unit";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String category = config.getParam("category", String.class, "ALL");
      boolean withValue = Boolean.parseBoolean(config.getParam("withValue", String.class, "false"));
      String valueRange = config.getParam("valueRange", String.class, "1,100");

      String unit = generateUnit(category);

      if (withValue) {
        return generateValueWithUnit(unit, valueRange);
      }

      return unit;

    } catch (Exception e) {
      logger.warn("Error generating measurement unit: {}", e.getMessage());
      // 生成默认单位
      return generateDefaultUnit();
    }
  }

  /** 根据类别生成单位 */
  private String generateUnit(String category) {
    List<String> units;

    switch (category.toUpperCase()) {
      case "SI_BASE":
        units = SI_BASE_UNITS;
        break;
      case "DERIVED":
        units = DERIVED_UNITS;
        break;
      case "PREFIXED":
        units = PREFIXED_UNITS;
        break;
      case "CUSTOMARY":
        units = CUSTOMARY_UNITS;
        break;
      case "ALL":
      default:
        units = ALL_UNITS;
        break;
    }

    return units.get(ThreadLocalRandom.current().nextInt(units.size()));
  }

  /** 生成带数值的单位 */
  private String generateValueWithUnit(String unit, String valueRange) {
    try {
      String[] rangeParts = valueRange.split(",");
      if (rangeParts.length == 2) {
        double minValue = Double.parseDouble(rangeParts[0]);
        double maxValue = Double.parseDouble(rangeParts[1]);
        double value = minValue + (maxValue - minValue) * ThreadLocalRandom.current().nextDouble();
        return String.format("%.2f %s", value, unit);
      }
    } catch (NumberFormatException e) {
      logger.warn("Invalid value range format, using default value");
    }

    // 默认值
    double value = 1 + 99 * ThreadLocalRandom.current().nextDouble();
    return String.format("%.2f %s", value, unit);
  }

  /** 生成默认单位 */
  private String generateDefaultUnit() {
    return ALL_UNITS.get(ThreadLocalRandom.current().nextInt(ALL_UNITS.size()));
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String category = config.getParam("category", String.class, "ALL");
    String valueRange = config.getParam("valueRange", String.class, "1,100");

    // 验证类别
    String[] validCategories = {"SI_BASE", "DERIVED", "PREFIXED", "CUSTOMARY", "ALL"};
    boolean validCategory = false;
    for (String validCat : validCategories) {
      if (validCat.equalsIgnoreCase(category)) {
        validCategory = true;
        break;
      }
    }

    if (!validCategory) {
      return false;
    }

    // 验证数值范围格式
    try {
      String[] rangeParts = valueRange.split(",");
      if (rangeParts.length != 2) {
        return false;
      }

      double minValue = Double.parseDouble(rangeParts[0]);
      double maxValue = Double.parseDouble(rangeParts[1]);
      return minValue < maxValue;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public String getDescription() {
    return "生成度量单位，支持SI基本单位、导出单位、前缀单位和常用自定义单位，" + "可生成纯单位或带数值的单位表达式，适用于度量系统测试";
  }
}
