package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 车牌号生成器。
 *
 * <p>生成符合中国大陆车牌号规则的车牌号码。 支持燃油车牌和新能源车牌，可指定省份和城市。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 车牌类型 (NEW_ENERGY|FUEL|BOTH) 默认: BOTH
 *   <li>province: 省份（全名或简称）默认: 随机
 *   <li>city: 城市代码 (A-Z) 默认: 随机
 *   <li>include_io: 是否包含I和O字符 默认: false
 *   <li>valid: 是否生成有效车牌 默认: true
 *   <li>config_file: 自定义配置文件路径 默认: data/license-plates.yml
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class LicensePlateGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(LicensePlateGenerator.class);

  private static final String DEFAULT_CONFIG_FILE = "data/license-plates.yml";

  /** 配置数据（从文件加载） */
  private volatile LicensePlateConfig licensePlateConfig;

  private final Random random = new Random();
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  public String getType() {
    return "licenseplate";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 确保配置已加载
      ensureConfigLoaded(config);

      // 从参数中获取车牌类型
      String plateType = getStringParam(config, "type", "BOTH");

      // 从参数中获取省份
      String province = getStringParam(config, "province", null);

      // 从参数中获取城市代码
      String city = getStringParam(config, "city", null);

      // 从参数中获取是否包含I和O
      boolean includeIO = getBooleanParam(config, "include_io", false);

      // 从参数中获取是否生成有效车牌
      boolean valid = getBooleanParam(config, "valid", true);

      if (!valid) {
        return generateInvalidLicensePlate();
      }

      return generateValidLicensePlate(plateType, province, city, includeIO);

    } catch (Exception e) {
      logger.error("Failed to generate license plate", e);
      // 返回一个默认车牌号作为fallback
      return "京A12345";
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  /**
   * 生成有效的车牌号。
   *
   * @param plateType 车牌类型
   * @param province 省份
   * @param city 城市代码
   * @param includeIO 是否包含I和O
   * @return 有效的车牌号
   */
  private String generateValidLicensePlate(
      String plateType, String province, String city, boolean includeIO) {
    // 确定车牌类型
    boolean isNewEnergy = determineNewEnergyType(plateType);

    // 生成省份简称
    String provinceCode = selectProvinceCode(province);

    // 生成城市代码
    String cityCode = selectCityCode(city, provinceCode);

    // 生成车牌号码部分
    String numberPart = generateNumberPart(isNewEnergy, includeIO);

    return provinceCode + cityCode + numberPart;
  }

  /**
   * 确定是否为新能源车牌。
   *
   * @param plateType 车牌类型参数
   * @return 是否为新能源车牌
   */
  private boolean determineNewEnergyType(String plateType) {
    return switch (plateType.toUpperCase()) {
      case "NEW_ENERGY", "ELECTRIC", "GREEN" -> true;
      case "FUEL", "TRADITIONAL", "BLUE" -> false;
      default -> random.nextBoolean(); // BOTH或其他情况随机选择
    };
  }

  /**
   * 选择省份代码。
   *
   * @param province 指定的省份
   * @return 省份简称
   */
  private String selectProvinceCode(String province) {
    if (licensePlateConfig == null || licensePlateConfig.getCountries() == null) {
      // 使用fallback数据
      return selectProvinceCodeFallback(province);
    }

    Map<String, CountryConfig> countries = licensePlateConfig.getCountries();
    CountryConfig cn = countries.get("CN");

    if (cn == null || cn.getProvinces() == null) {
      return selectProvinceCodeFallback(province);
    }

    if (province != null && !province.trim().isEmpty()) {
      String cleanProvince = province.trim();
      List<ProvinceConfig> provinces = cn.getProvinces();

      // 先检查是否是省份简称
      for (ProvinceConfig p : provinces) {
        if (p.getCode().equals(cleanProvince)) {
          return p.getCode();
        }
      }

      // 再检查是否是省份全名
      for (ProvinceConfig p : provinces) {
        if (p.getName().equals(cleanProvince)) {
          return p.getCode();
        }
      }

      // 最后模糊匹配
      for (ProvinceConfig p : provinces) {
        if (p.getName().contains(cleanProvince) || cleanProvince.contains(p.getName())) {
          return p.getCode();
        }
      }

      logger.warn("Unknown province: {}, using random province", province);
    }

    // 使用权重随机选择省份
    return selectProvinceByWeight(cn.getProvinces());
  }

  /**
   * 使用权重随机选择省份。
   *
   * @param provinces 省份列表
   * @return 选中的省份代码
   */
  private String selectProvinceByWeight(List<ProvinceConfig> provinces) {
    int totalWeight = provinces.stream().mapToInt(p -> p.getWeight()).sum();
    int randomValue = random.nextInt(totalWeight);

    int currentWeight = 0;
    for (ProvinceConfig p : provinces) {
      currentWeight += p.getWeight();
      if (randomValue < currentWeight) {
        return p.getCode();
      }
    }

    // Fallback
    return provinces.get(0).getCode();
  }

  /** Fallback省份选择方法。 */
  private String selectProvinceCodeFallback(String province) {
    List<String> fallbackProvinces =
        List.of(
            "京", "津", "沪", "渝", "冀", "豫", "云", "辽", "黑", "湘", "皖", "鲁", "新", "苏", "浙", "赣", "鄂",
            "桂", "甘", "晋", "蒙", "陕", "吉", "闽", "贵", "粤", "青", "藏", "川", "宁", "琼", "港", "澳", "台");

    if (province != null && !province.trim().isEmpty()) {
      String cleanProvince = province.trim();
      if (fallbackProvinces.contains(cleanProvince)) {
        return cleanProvince;
      }
      logger.warn("Unknown province: {}, using random province", province);
    }

    return fallbackProvinces.get(random.nextInt(fallbackProvinces.size()));
  }

  /**
   * 选择城市代码。
   *
   * @param city 指定的城市代码
   * @param provinceCode 省份代码
   * @return 城市代码字母
   */
  private String selectCityCode(String city, String provinceCode) {
    if (licensePlateConfig == null || licensePlateConfig.getCountries() == null) {
      // 使用fallback数据
      return selectCityCodeFallback(city);
    }

    Map<String, CountryConfig> countries = licensePlateConfig.getCountries();
    CountryConfig cn = countries.get("CN");

    if (cn == null || cn.getProvinces() == null) {
      return selectCityCodeFallback(city);
    }

    // 查找对应省份
    ProvinceConfig targetProvince = null;
    for (ProvinceConfig p : cn.getProvinces()) {
      if (p.getCode().equals(provinceCode)) {
        targetProvince = p;
        break;
      }
    }

    if (targetProvince == null
        || targetProvince.getCities() == null
        || targetProvince.getCities().isEmpty()) {
      return selectCityCodeFallback(city);
    }

    List<String> cities = targetProvince.getCities();

    if (city != null && !city.trim().isEmpty()) {
      String cleanCity = city.trim().toUpperCase();
      if (cities.contains(cleanCity)) {
        return cleanCity;
      }
      logger.warn("Invalid city code: {}, using random city code", city);
    }

    // 随机选择城市代码
    return cities.get(random.nextInt(cities.size()));
  }

  /** Fallback城市代码选择方法。 */
  private String selectCityCodeFallback(String city) {
    List<String> fallbackCities =
        List.of(
            "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S",
            "T", "U", "V", "W", "X", "Y", "Z");

    if (city != null && !city.trim().isEmpty()) {
      String cleanCity = city.trim().toUpperCase();
      if (fallbackCities.contains(cleanCity)) {
        return cleanCity;
      }
      logger.warn("Invalid city code: {}, using random city code", city);
    }

    return fallbackCities.get(random.nextInt(fallbackCities.size()));
  }

  /**
   * 生成车牌号码部分。
   *
   * @param isNewEnergy 是否为新能源车牌
   * @param includeIO 是否包含I和O
   * @return 号码部分
   */
  private String generateNumberPart(boolean isNewEnergy, boolean includeIO) {
    if (isNewEnergy) {
      return generateNewEnergyNumberPart(includeIO);
    } else {
      return generateTraditionalNumberPart(includeIO);
    }
  }

  /**
   * 生成新能源车牌号码部分（6位）。
   *
   * @param includeIO 是否包含I和O
   * @return 新能源车牌号码部分
   */
  private String generateNewEnergyNumberPart(boolean includeIO) {
    StringBuilder numberPart = new StringBuilder();

    // 获取字符集
    String plateChars = getPlateChars();
    String charSet = includeIO ? plateChars + "IO" : plateChars;

    // 获取新能源前缀
    List<String> prefixes = getNewEnergyPrefixes();
    String prefix = prefixes.get(random.nextInt(prefixes.size()));
    numberPart.append(prefix);

    // 生成后5位
    for (int i = 0; i < 5; i++) {
      numberPart.append(charSet.charAt(random.nextInt(charSet.length())));
    }

    return numberPart.toString();
  }

  /**
   * 生成传统车牌号码部分（5位）。
   *
   * @param includeIO 是否包含I和O
   * @return 传统车牌号码部分
   */
  private String generateTraditionalNumberPart(boolean includeIO) {
    StringBuilder numberPart = new StringBuilder();

    // 获取字符集
    String plateChars = getPlateChars();
    String charSet = includeIO ? plateChars + "IO" : plateChars;

    // 传统车牌：5位字母数字组合
    for (int i = 0; i < 5; i++) {
      numberPart.append(charSet.charAt(random.nextInt(charSet.length())));
    }

    return numberPart.toString();
  }

  /** 获取车牌字符集。 */
  private String getPlateChars() {
    if (licensePlateConfig != null
        && licensePlateConfig.getSettings() != null
        && licensePlateConfig.getSettings().getCharacters() != null) {
      return licensePlateConfig.getSettings().getCharacters().getPlateChars();
    }
    // Fallback
    return "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
  }

  /** 获取新能源车牌前缀。 */
  private List<String> getNewEnergyPrefixes() {
    if (licensePlateConfig != null
        && licensePlateConfig.getSettings() != null
        && licensePlateConfig.getSettings().getNewEnergy() != null) {
      return licensePlateConfig.getSettings().getNewEnergy().getPrefixes();
    }
    // Fallback
    return List.of("D", "F");
  }

  /**
   * 生成无效的车牌号。
   *
   * @return 无效的车牌号
   */
  private String generateInvalidLicensePlate() {
    int type = random.nextInt(4);

    return switch (type) {
      case 0 -> generateWrongLengthPlate();
      case 1 -> generateWrongFormatPlate();
      case 2 -> generateInvalidCharsPlate();
      default -> generateOtherInvalidPlate();
    };
  }

  /**
   * 生成长度错误的车牌号。
   *
   * @return 长度错误的车牌号
   */
  private String generateWrongLengthPlate() {
    int length =
        random.nextBoolean()
            ? random.nextInt(4) + 2
            : // 2-5位
            random.nextInt(5) + 9; // 9-13位

    String plateChars = getPlateChars();
    StringBuilder plate = new StringBuilder();

    for (int i = 0; i < length; i++) {
      if (i == 0) {
        // 第一位用省份简称
        plate.append(selectProvinceCode(null));
      } else {
        plate.append(plateChars.charAt(random.nextInt(plateChars.length())));
      }
    }

    return plate.toString();
  }

  /**
   * 生成格式错误的车牌号。
   *
   * @return 格式错误的车牌号
   */
  private String generateWrongFormatPlate() {
    return "1A12345"; // 第一位不是汉字
  }

  /**
   * 生成包含非法字符的车牌号。
   *
   * @return 包含非法字符的车牌号
   */
  private String generateInvalidCharsPlate() {
    String province = selectProvinceCode(null);
    String city = selectCityCodeFallback(null);

    // 在号码部分包含非法字符
    String[] invalidChars = {"!", "@", "#", "$", "%"};
    String invalidChar = invalidChars[random.nextInt(invalidChars.length)];

    return province + city + "123" + invalidChar + "5";
  }

  /**
   * 生成其他类型的无效车牌号。
   *
   * @return 其他无效车牌号
   */
  private String generateOtherInvalidPlate() {
    // 生成全数字或全字母的车牌号
    if (random.nextBoolean()) {
      return "1234567"; // 全数字
    } else {
      return "ABCDEFG"; // 全字母
    }
  }

  /**
   * 确保配置已加载。
   *
   * @param config 字段配置
   */
  private void ensureConfigLoaded(FieldConfig config) {
    if (licensePlateConfig == null) {
      synchronized (this) {
        if (licensePlateConfig == null) {
          loadConfig(config);
        }
      }
    }
  }

  /**
   * 加载配置。
   *
   * @param config 字段配置
   */
  private void loadConfig(FieldConfig config) {
    try {
      String configFile = getStringParam(config, "config_file", DEFAULT_CONFIG_FILE);

      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
      if (inputStream != null) {
        licensePlateConfig = yamlMapper.readValue(inputStream, LicensePlateConfig.class);
        logger.info("License plate config loaded from: {}", configFile);
      } else {
        logger.warn("Config file not found: {}, using fallback data", configFile);
        initializeFallbackConfig();
      }
    } catch (Exception e) {
      logger.error("Failed to load license plate config, using fallback data", e);
      initializeFallbackConfig();
    }
  }

  /** 初始化fallback配置。 */
  private void initializeFallbackConfig() {
    licensePlateConfig = new LicensePlateConfig();
    logger.info("License plate fallback config initialized");
  }

  @Override
  public String getDescription() {
    return "License plate generator - generates Chinese license plate numbers with fuel/electric"
        + " vehicle support";
  }

  /** 车牌配置类。 */
  public static class LicensePlateConfig {
    private Map<String, CountryConfig> countries;
    private SettingsConfig settings;
    private Map<String, Object> outputFormats;

    public Map<String, CountryConfig> getCountries() {
      return countries;
    }

    public void setCountries(Map<String, CountryConfig> countries) {
      this.countries = countries;
    }

    public SettingsConfig getSettings() {
      return settings;
    }

    public void setSettings(SettingsConfig settings) {
      this.settings = settings;
    }

    public Map<String, Object> getOutputFormats() {
      return outputFormats;
    }

    public void setOutputFormats(Map<String, Object> outputFormats) {
      this.outputFormats = outputFormats;
    }
  }

  /** 国家配置类。 */
  public static class CountryConfig {
    private String name;
    private boolean defaultCountry;
    private String description;
    private String format;
    private List<ProvinceConfig> provinces;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isDefaultCountry() {
      return defaultCountry;
    }

    public void setDefault_country(boolean defaultCountry) {
      this.defaultCountry = defaultCountry;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getFormat() {
      return format;
    }

    public void setFormat(String format) {
      this.format = format;
    }

    public List<ProvinceConfig> getProvinces() {
      return provinces;
    }

    public void setProvinces(List<ProvinceConfig> provinces) {
      this.provinces = provinces;
    }
  }

  /** 省份配置类。 */
  public static class ProvinceConfig {
    private String code;
    private String name;
    private int weight;
    private List<String> cities;

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getWeight() {
      return weight;
    }

    public void setWeight(int weight) {
      this.weight = weight;
    }

    public List<String> getCities() {
      return cities;
    }

    public void setCities(List<String> cities) {
      this.cities = cities;
    }
  }

  /** 设置配置类。 */
  public static class SettingsConfig {
    private CharactersConfig characters;
    private NewEnergyConfig newEnergy;
    private TraditionalConfig traditional;
    private List<String> cityCodes;

    public CharactersConfig getCharacters() {
      return characters;
    }

    public void setCharacters(CharactersConfig characters) {
      this.characters = characters;
    }

    public NewEnergyConfig getNewEnergy() {
      return newEnergy;
    }

    public void setNewEnergy(NewEnergyConfig newEnergy) {
      this.newEnergy = newEnergy;
    }

    public TraditionalConfig getTraditional() {
      return traditional;
    }

    public void setTraditional(TraditionalConfig traditional) {
      this.traditional = traditional;
    }

    public List<String> getCityCodes() {
      return cityCodes;
    }

    public void setCityCodes(List<String> cityCodes) {
      this.cityCodes = cityCodes;
    }
  }

  /** 字符集配置类。 */
  public static class CharactersConfig {
    private String plateChars;
    private String optionalChars;

    public String getPlateChars() {
      return plateChars;
    }

    public void setPlateChars(String plateChars) {
      this.plateChars = plateChars;
    }

    public String getOptionalChars() {
      return optionalChars;
    }

    public void setOptionalChars(String optionalChars) {
      this.optionalChars = optionalChars;
    }
  }

  /** 新能源车配置类。 */
  public static class NewEnergyConfig {
    private List<String> prefixes;
    private int numberLength;

    public List<String> getPrefixes() {
      return prefixes;
    }

    public void setPrefixes(List<String> prefixes) {
      this.prefixes = prefixes;
    }

    public int getNumberLength() {
      return numberLength;
    }

    public void setNumberLength(int numberLength) {
      this.numberLength = numberLength;
    }
  }

  /** 传统车配置类。 */
  public static class TraditionalConfig {
    private int numberLength;

    public int getNumberLength() {
      return numberLength;
    }

    public void setNumberLength(int numberLength) {
      this.numberLength = numberLength;
    }
  }
}
