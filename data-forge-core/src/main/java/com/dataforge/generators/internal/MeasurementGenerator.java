package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 度量单位生成器
 *
 * <p>支持生成各种物理量的数值和单位组合，包括长度、重量、体积、 温度、速度、压力等，用于物联网、科学计算、工程测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>category: 度量类别
 *       (LENGTH|WEIGHT|VOLUME|TEMPERATURE|SPEED|PRESSURE|ENERGY|POWER|AREA|TIME|FREQUENCY|ANY) 默认:
 *       ANY
 *   <li>unit_system: 单位制 (METRIC|IMPERIAL|US|SI|ANY) 默认: METRIC
 *   <li>unit: 指定单位 默认: 随机选择
 *   <li>value_min: 数值最小值 默认: 0.1
 *   <li>value_max: 数值最大值 默认: 1000.0
 *   <li>precision: 小数位数 默认: 2
 *   <li>format: 输出格式 (VALUE_UNIT|UNIT_VALUE|JSON|VERBOSE) 默认: VALUE_UNIT
 *   <li>include_conversion: 是否包含单位转换 默认: false
 *   <li>conversion_target: 转换目标单位
 *   <li>realistic_ranges: 是否使用现实范围 默认: true
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class MeasurementGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(MeasurementGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  private static final String DEFAULT_CONFIG_FILE = "data/measurements.yml";

  /** 配置数据（从文件加载） */
  private volatile MeasurementConfig measurementConfig;

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  // 度量类别枚举
  public enum MeasurementCategory {
    LENGTH("长度"),
    WEIGHT("重量"),
    VOLUME("体积"),
    TEMPERATURE("温度"),
    SPEED("速度"),
    PRESSURE("压力"),
    ENERGY("能量"),
    POWER("功率"),
    AREA("面积"),
    TIME("时间"),
    FREQUENCY("频率");

    private final String description;

    MeasurementCategory(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 单位制枚举
  public enum UnitSystem {
    METRIC("公制"),
    IMPERIAL("英制"),
    US("美制"),
    SI("国际单位制");

    private final String description;

    UnitSystem(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 输出格式枚举
  public enum OutputFormat {
    VALUE_UNIT("数值 单位"),
    UNIT_VALUE("单位 数值"),
    JSON("JSON格式"),
    VERBOSE("详细格式");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 单位信息类
  public static class UnitInfo {
    private final String symbol;
    private final String name;
    private final MeasurementCategory category;
    private final UnitSystem system;
    private final double baseConversionFactor;
    private final String baseUnit;
    private final double minRealistic;
    private final double maxRealistic;

    public UnitInfo(
        String symbol,
        String name,
        MeasurementCategory category,
        UnitSystem system,
        double baseConversionFactor,
        String baseUnit,
        double minRealistic,
        double maxRealistic) {
      this.symbol = symbol;
      this.name = name;
      this.category = category;
      this.system = system;
      this.baseConversionFactor = baseConversionFactor;
      this.baseUnit = baseUnit;
      this.minRealistic = minRealistic;
      this.maxRealistic = maxRealistic;
    }

    // Getters
    public String getSymbol() {
      return symbol;
    }

    public String getName() {
      return name;
    }

    public MeasurementCategory getCategory() {
      return category;
    }

    public UnitSystem getSystem() {
      return system;
    }

    public double getBaseConversionFactor() {
      return baseConversionFactor;
    }

    public String getBaseUnit() {
      return baseUnit;
    }

    public double getMinRealistic() {
      return minRealistic;
    }

    public double getMaxRealistic() {
      return maxRealistic;
    }
  }

  // 单位数据库
  private static final Map<String, UnitInfo> UNITS = new HashMap<>();
  private static final Map<MeasurementCategory, List<UnitInfo>> UNITS_BY_CATEGORY = new HashMap<>();
  private static final Map<UnitSystem, List<UnitInfo>> UNITS_BY_SYSTEM = new HashMap<>();

  static {
    initializeUnits();
  }

  private static void initializeUnits() {
    // 长度单位
    addUnit("mm", "毫米", MeasurementCategory.LENGTH, UnitSystem.METRIC, 0.001, "m", 0.1, 1000);
    addUnit("cm", "厘米", MeasurementCategory.LENGTH, UnitSystem.METRIC, 0.01, "m", 1, 100);
    addUnit("m", "米", MeasurementCategory.LENGTH, UnitSystem.METRIC, 1.0, "m", 0.1, 1000);
    addUnit("km", "千米", MeasurementCategory.LENGTH, UnitSystem.METRIC, 1000, "m", 0.1, 10000);
    addUnit("in", "英寸", MeasurementCategory.LENGTH, UnitSystem.IMPERIAL, 0.0254, "m", 0.1, 100);
    addUnit("ft", "英尺", MeasurementCategory.LENGTH, UnitSystem.IMPERIAL, 0.3048, "m", 0.1, 1000);
    addUnit("yd", "码", MeasurementCategory.LENGTH, UnitSystem.IMPERIAL, 0.9144, "m", 0.1, 1000);
    addUnit("mi", "英里", MeasurementCategory.LENGTH, UnitSystem.IMPERIAL, 1609.34, "m", 0.1, 1000);

    // 重量单位
    addUnit("mg", "毫克", MeasurementCategory.WEIGHT, UnitSystem.METRIC, 0.000001, "kg", 1, 1000);
    addUnit("g", "克", MeasurementCategory.WEIGHT, UnitSystem.METRIC, 0.001, "kg", 1, 1000);
    addUnit("kg", "千克", MeasurementCategory.WEIGHT, UnitSystem.METRIC, 1.0, "kg", 0.1, 1000);
    addUnit("t", "吨", MeasurementCategory.WEIGHT, UnitSystem.METRIC, 1000, "kg", 0.1, 100);
    addUnit("oz", "盎司", MeasurementCategory.WEIGHT, UnitSystem.IMPERIAL, 0.0283495, "kg", 1, 100);
    addUnit("lb", "磅", MeasurementCategory.WEIGHT, UnitSystem.IMPERIAL, 0.453592, "kg", 0.1, 1000);
    addUnit("st", "英石", MeasurementCategory.WEIGHT, UnitSystem.IMPERIAL, 6.35029, "kg", 1, 50);

    // 体积单位
    addUnit("ml", "毫升", MeasurementCategory.VOLUME, UnitSystem.METRIC, 0.000001, "m³", 1, 1000);
    addUnit("l", "升", MeasurementCategory.VOLUME, UnitSystem.METRIC, 0.001, "m³", 0.1, 1000);
    addUnit("m³", "立方米", MeasurementCategory.VOLUME, UnitSystem.METRIC, 1.0, "m³", 0.001, 1000);
    addUnit("fl oz", "液体盎司", MeasurementCategory.VOLUME, UnitSystem.US, 0.0000295735, "m³", 1, 100);
    addUnit("cup", "杯", MeasurementCategory.VOLUME, UnitSystem.US, 0.000236588, "m³", 0.1, 10);
    addUnit("pt", "品脱", MeasurementCategory.VOLUME, UnitSystem.US, 0.000473176, "m³", 0.1, 10);
    addUnit("qt", "夸脱", MeasurementCategory.VOLUME, UnitSystem.US, 0.000946353, "m³", 0.1, 10);
    addUnit("gal", "加仑", MeasurementCategory.VOLUME, UnitSystem.US, 0.00378541, "m³", 0.1, 100);

    // 温度单位
    addUnit("°C", "摄氏度", MeasurementCategory.TEMPERATURE, UnitSystem.METRIC, 1.0, "°C", -50, 100);
    addUnit("°F", "华氏度", MeasurementCategory.TEMPERATURE, UnitSystem.IMPERIAL, 1.0, "°F", -58, 212);
    addUnit("K", "开尔文", MeasurementCategory.TEMPERATURE, UnitSystem.SI, 1.0, "K", 223, 373);
    addUnit("°R", "兰氏度", MeasurementCategory.TEMPERATURE, UnitSystem.IMPERIAL, 1.0, "°R", 402, 672);

    // 速度单位
    addUnit("m/s", "米每秒", MeasurementCategory.SPEED, UnitSystem.METRIC, 1.0, "m/s", 0.1, 100);
    addUnit("km/h", "千米每小时", MeasurementCategory.SPEED, UnitSystem.METRIC, 0.277778, "m/s", 1, 300);
    addUnit("mph", "英里每小时", MeasurementCategory.SPEED, UnitSystem.IMPERIAL, 0.44704, "m/s", 1, 200);
    addUnit("ft/s", "英尺每秒", MeasurementCategory.SPEED, UnitSystem.IMPERIAL, 0.3048, "m/s", 1, 300);
    addUnit("knot", "节", MeasurementCategory.SPEED, UnitSystem.METRIC, 0.514444, "m/s", 1, 500);

    // 压力单位
    addUnit("Pa", "帕斯卡", MeasurementCategory.PRESSURE, UnitSystem.SI, 1.0, "Pa", 1000, 200000);
    addUnit("kPa", "千帕", MeasurementCategory.PRESSURE, UnitSystem.SI, 1000, "Pa", 1, 200);
    addUnit("MPa", "兆帕", MeasurementCategory.PRESSURE, UnitSystem.SI, 1000000, "Pa", 0.1, 100);
    addUnit("bar", "巴", MeasurementCategory.PRESSURE, UnitSystem.METRIC, 100000, "Pa", 0.1, 10);
    addUnit("atm", "大气压", MeasurementCategory.PRESSURE, UnitSystem.METRIC, 101325, "Pa", 0.1, 10);
    addUnit(
        "psi", "磅每平方英寸", MeasurementCategory.PRESSURE, UnitSystem.IMPERIAL, 6895, "Pa", 1, 1000);

    // 能量单位
    addUnit("J", "焦耳", MeasurementCategory.ENERGY, UnitSystem.SI, 1.0, "J", 1, 1000000);
    addUnit("kJ", "千焦", MeasurementCategory.ENERGY, UnitSystem.SI, 1000, "J", 1, 1000);
    addUnit("MJ", "兆焦", MeasurementCategory.ENERGY, UnitSystem.SI, 1000000, "J", 0.1, 100);
    addUnit("cal", "卡路里", MeasurementCategory.ENERGY, UnitSystem.METRIC, 4.184, "J", 1, 10000);
    addUnit("kcal", "千卡", MeasurementCategory.ENERGY, UnitSystem.METRIC, 4184, "J", 1, 5000);
    addUnit("Wh", "瓦时", MeasurementCategory.ENERGY, UnitSystem.SI, 3600, "J", 1, 10000);
    addUnit("kWh", "千瓦时", MeasurementCategory.ENERGY, UnitSystem.SI, 3600000, "J", 0.1, 1000);

    // 功率单位
    addUnit("W", "瓦特", MeasurementCategory.POWER, UnitSystem.SI, 1.0, "W", 1, 10000);
    addUnit("kW", "千瓦", MeasurementCategory.POWER, UnitSystem.SI, 1000, "W", 0.1, 1000);
    addUnit("MW", "兆瓦", MeasurementCategory.POWER, UnitSystem.SI, 1000000, "W", 0.1, 100);
    addUnit("hp", "马力", MeasurementCategory.POWER, UnitSystem.IMPERIAL, 745.7, "W", 1, 1000);

    // 面积单位
    addUnit("mm²", "平方毫米", MeasurementCategory.AREA, UnitSystem.METRIC, 0.000001, "m²", 1, 10000);
    addUnit("cm²", "平方厘米", MeasurementCategory.AREA, UnitSystem.METRIC, 0.0001, "m²", 1, 1000);
    addUnit("m²", "平方米", MeasurementCategory.AREA, UnitSystem.METRIC, 1.0, "m²", 0.1, 10000);
    addUnit("km²", "平方千米", MeasurementCategory.AREA, UnitSystem.METRIC, 1000000, "m²", 0.1, 1000);
    addUnit("ha", "公顷", MeasurementCategory.AREA, UnitSystem.METRIC, 10000, "m²", 0.1, 1000);
    addUnit(
        "in²", "平方英寸", MeasurementCategory.AREA, UnitSystem.IMPERIAL, 0.00064516, "m²", 1, 1000);
    addUnit("ft²", "平方英尺", MeasurementCategory.AREA, UnitSystem.IMPERIAL, 0.092903, "m²", 1, 10000);
    addUnit("yd²", "平方码", MeasurementCategory.AREA, UnitSystem.IMPERIAL, 0.836127, "m²", 1, 1000);
    addUnit("acre", "英亩", MeasurementCategory.AREA, UnitSystem.IMPERIAL, 4046.86, "m²", 0.1, 1000);

    // 时间单位
    addUnit("ms", "毫秒", MeasurementCategory.TIME, UnitSystem.SI, 0.001, "s", 1, 10000);
    addUnit("s", "秒", MeasurementCategory.TIME, UnitSystem.SI, 1.0, "s", 0.1, 3600);
    addUnit("min", "分钟", MeasurementCategory.TIME, UnitSystem.SI, 60, "s", 1, 1440);
    addUnit("h", "小时", MeasurementCategory.TIME, UnitSystem.SI, 3600, "s", 0.1, 168);
    addUnit("day", "天", MeasurementCategory.TIME, UnitSystem.SI, 86400, "s", 1, 365);

    // 频率单位
    addUnit("Hz", "赫兹", MeasurementCategory.FREQUENCY, UnitSystem.SI, 1.0, "Hz", 1, 1000000);
    addUnit("kHz", "千赫", MeasurementCategory.FREQUENCY, UnitSystem.SI, 1000, "Hz", 1, 1000);
    addUnit("MHz", "兆赫", MeasurementCategory.FREQUENCY, UnitSystem.SI, 1000000, "Hz", 1, 1000);
    addUnit("GHz", "吉赫", MeasurementCategory.FREQUENCY, UnitSystem.SI, 1000000000, "Hz", 0.1, 100);
  }

  private static void addUnit(
      String symbol,
      String name,
      MeasurementCategory category,
      UnitSystem system,
      double conversionFactor,
      String baseUnit,
      double minRealistic,
      double maxRealistic) {
    UnitInfo unit =
        new UnitInfo(
            symbol, name, category, system, conversionFactor, baseUnit, minRealistic, maxRealistic);
    UNITS.put(symbol, unit);

    UNITS_BY_CATEGORY.computeIfAbsent(category, k -> new ArrayList<>()).add(unit);
    UNITS_BY_SYSTEM.computeIfAbsent(system, k -> new ArrayList<>()).add(unit);
  }

  @Override
  public String getType() {
    return "measurement";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      ensureConfigLoaded(config);

      UnitInfo unit = selectUnit(config);
      BigDecimal value = generateValue(unit, config);
      String formatStr = getStringParam(config, "format", "VALUE_UNIT");
      OutputFormat format = parseOutputFormat(formatStr);
      String result = formatOutput(value, unit, format, config);

      context.put("measurement_value", value);
      context.put("measurement_unit", unit.getSymbol());
      context.put("measurement_category", unit.getCategory().name());
      context.put("measurement_system", unit.getSystem().name());

      return result;
    } catch (Exception e) {
      logger.error("Failed to generate measurement", e);
      return "1.0 m";
    }
  }

  /** 选择单位 */
  private UnitInfo selectUnit(FieldConfig config) {
    String unit = getStringParam(config, "unit", null);
    String categoryStr = getStringParam(config, "category", "ANY");
    String systemStr = getStringParam(config, "unit_system", "ANY");

    // 如果指定了具体单位
    if (unit != null && UNITS.containsKey(unit)) {
      return UNITS.get(unit);
    }

    // 构建候选列表
    List<UnitInfo> candidates = new ArrayList<>(UNITS.values());

    // 按类别过滤
    if (!"ANY".equalsIgnoreCase(categoryStr)) {
      try {
        MeasurementCategory category = MeasurementCategory.valueOf(categoryStr.toUpperCase());
        List<UnitInfo> categoryUnits = UNITS_BY_CATEGORY.get(category);
        if (categoryUnits != null) {
          candidates.retainAll(categoryUnits);
        }
      } catch (IllegalArgumentException e) {
        logger.warn("Invalid measurement category: {}", categoryStr);
      }
    }

    // 按单位制过滤
    if (!"ANY".equalsIgnoreCase(systemStr)) {
      try {
        UnitSystem system = UnitSystem.valueOf(systemStr.toUpperCase());
        List<UnitInfo> systemUnits = UNITS_BY_SYSTEM.get(system);
        if (systemUnits != null) {
          candidates.retainAll(systemUnits);
        }
      } catch (IllegalArgumentException e) {
        logger.warn("Invalid unit system: {}", systemStr);
      }
    }

    // 如果没有候选单位，使用默认
    if (candidates.isEmpty()) {
      return UNITS.get("m");
    }

    // 随机选择
    return candidates.get(random.nextInt(candidates.size()));
  }

  /** 生成数值 */
  private BigDecimal generateValue(UnitInfo unit, FieldConfig config) {
    boolean useRealistic = getBooleanParam(config, "realistic_ranges", true);

    double min, max;
    if (useRealistic) {
      min = unit.getMinRealistic();
      max = unit.getMaxRealistic();
    } else {
      min = getDoubleParam(config, "value_min", 0.1);
      max = getDoubleParam(config, "value_max", 1000.0);
    }

    // 生成随机值
    double value = min + random.nextDouble() * (max - min);

    // 应用精度
    int precision = getIntParam(config, "precision", 2);
    return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);
  }

  /** 解析输出格式 */
  private OutputFormat parseOutputFormat(String formatStr) {
    try {
      return OutputFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output format: {}, using VALUE_UNIT as default", formatStr);
      return OutputFormat.VALUE_UNIT;
    }
  }

  /** 格式化输出 */
  private String formatOutput(
      BigDecimal value, UnitInfo unit, OutputFormat format, FieldConfig config) {
    switch (format) {
      case VALUE_UNIT:
        return value.toPlainString() + " " + unit.getSymbol();
      case UNIT_VALUE:
        return unit.getSymbol() + " " + value.toPlainString();
      case JSON:
        return formatAsJson(value, unit, config);
      case VERBOSE:
        return formatAsVerbose(value, unit, config);
      default:
        return value.toPlainString() + " " + unit.getSymbol();
    }
  }

  /** 格式化为JSON */
  private String formatAsJson(BigDecimal value, UnitInfo unit, FieldConfig config) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"value\":").append(value.toPlainString()).append(",");
    json.append("\"unit\":\"").append(unit.getSymbol()).append("\",");
    json.append("\"unit_name\":\"").append(unit.getName()).append("\",");
    json.append("\"category\":\"").append(unit.getCategory().name()).append("\",");
    json.append("\"system\":\"").append(unit.getSystem().name()).append("\"");

    // 如果需要包含转换
    if (getBooleanParam(config, "include_conversion", false)) {
      String conversionResult = performConversion(value, unit, config);
      if (conversionResult != null) {
        json.append(",\"conversion\":\"").append(conversionResult).append("\"");
      }
    }

    json.append("}");
    return json.toString();
  }

  /** 格式化为详细格式 */
  private String formatAsVerbose(BigDecimal value, UnitInfo unit, FieldConfig config) {
    StringBuilder verbose = new StringBuilder();
    verbose.append(value.toPlainString()).append(" ");
    verbose.append(unit.getName()).append(" (").append(unit.getSymbol()).append(")");
    verbose.append(" [").append(unit.getCategory().getDescription()).append("]");
    verbose.append(" [").append(unit.getSystem().getDescription()).append("]");

    // 如果需要包含转换
    if (getBooleanParam(config, "include_conversion", false)) {
      String conversionResult = performConversion(value, unit, config);
      if (conversionResult != null) {
        verbose.append(" = ").append(conversionResult);
      }
    }

    return verbose.toString();
  }

  /** 执行单位转换 */
  private String performConversion(BigDecimal value, UnitInfo fromUnit, FieldConfig config) {
    String targetUnitStr = getStringParam(config, "conversion_target", null);
    if (targetUnitStr == null) {
      return null;
    }

    UnitInfo targetUnit = UNITS.get(targetUnitStr);
    if (targetUnit == null || !fromUnit.getCategory().equals(targetUnit.getCategory())) {
      return null;
    }

    // 执行转换（通过基准单位）
    double baseValue = value.doubleValue() * fromUnit.getBaseConversionFactor();
    double targetValue = baseValue / targetUnit.getBaseConversionFactor();

    int precision = getIntParam(config, "precision", 2);
    BigDecimal convertedValue =
        BigDecimal.valueOf(targetValue).setScale(precision, RoundingMode.HALF_UP);

    return convertedValue.toPlainString() + " " + targetUnit.getSymbol();
  }

  /** 获取所有支持的单位 */
  public static Set<String> getSupportedUnits() {
    return UNITS.keySet();
  }

  /** 获取指定类别的单位 */
  public static List<UnitInfo> getUnitsByCategory(MeasurementCategory category) {
    return UNITS_BY_CATEGORY.getOrDefault(category, new ArrayList<>());
  }

  /** 获取指定单位制的单位 */
  public static List<UnitInfo> getUnitsBySystem(UnitSystem system) {
    return UNITS_BY_SYSTEM.getOrDefault(system, new ArrayList<>());
  }

  /** 验证单位 */
  public static boolean isValidUnit(String unit) {
    return UNITS.containsKey(unit);
  }

  /** 获取单位信息 */
  public static UnitInfo getUnitInfo(String unit) {
    return UNITS.get(unit);
  }

  /**
   * 确保配置已加载。
   *
   * @param config 配置
   */
  private void ensureConfigLoaded(FieldConfig config) {
    if (measurementConfig == null) {
      synchronized (this) {
        if (measurementConfig == null) {
          loadConfig(config);
        }
      }
    }
  }

  /**
   * 加载配置。
   *
   * @param config 配置
   */
  private void loadConfig(FieldConfig config) {
    try {
      String configFile = getStringParam(config, "units_config_file", DEFAULT_CONFIG_FILE);

      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
      if (inputStream != null) {
        measurementConfig = yamlMapper.readValue(inputStream, MeasurementConfig.class);
        logger.info("Measurement config loaded from: {}", configFile);
      } else {
        logger.warn("Config file not found: {}, using fallback data", configFile);
        initializeFallbackConfig();
      }
    } catch (Exception e) {
      logger.error("Failed to load measurement config, using fallback data", e);
      initializeFallbackConfig();
    }
  }

  /** 初始化fallback配置。 */
  private void initializeFallbackConfig() {
    measurementConfig = new MeasurementConfig();
  }

  /** 度量配置类。 */
  @SuppressWarnings("unused")
  private static class MeasurementConfig {
    private Map<String, CategoryConfig> categories;

    public Map<String, CategoryConfig> getCategories() {
      return categories;
    }

    public void setCategories(Map<String, CategoryConfig> categories) {
      this.categories = categories;
    }
  }

  /** 类别配置类。 */
  @SuppressWarnings("unused")
  private static class CategoryConfig {
    private String name;
    private String description;
    private List<UnitConfig> units;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public List<UnitConfig> getUnits() {
      return units;
    }

    public void setUnits(List<UnitConfig> units) {
      this.units = units;
    }
  }

  /** 单位配置类。 */
  @SuppressWarnings("unused")
  private static class UnitConfig {
    private String name;
    private String symbol;
    private String system;
    private Double to_base;
    private String base_unit;
    private Double min_realistic;
    private Double max_realistic;
    private Integer weight;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getSymbol() {
      return symbol;
    }

    public void setSymbol(String symbol) {
      this.symbol = symbol;
    }

    public String getSystem() {
      return system;
    }

    public void setSystem(String system) {
      this.system = system;
    }

    public Double getTo_base() {
      return to_base;
    }

    public void setTo_base(Double to_base) {
      this.to_base = to_base;
    }

    public String getBase_unit() {
      return base_unit;
    }

    public void setBase_unit(String base_unit) {
      this.base_unit = base_unit;
    }

    public Double getMin_realistic() {
      return min_realistic;
    }

    public void setMin_realistic(Double min_realistic) {
      this.min_realistic = min_realistic;
    }

    public Double getMax_realistic() {
      return max_realistic;
    }

    public void setMax_realistic(Double max_realistic) {
      this.max_realistic = max_realistic;
    }

    public Integer getWeight() {
      return weight;
    }

    public void setWeight(Integer weight) {
      this.weight = weight;
    }
  }
}
