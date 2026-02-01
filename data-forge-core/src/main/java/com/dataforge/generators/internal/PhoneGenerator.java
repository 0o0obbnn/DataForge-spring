package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 手机号码生成器。
 *
 * <p>生成符合中国大陆运营商号段规则的11位手机号码。 支持指定运营商前缀和生成有效/无效号码。 支持通过配置文件加载号段数据。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class PhoneGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(PhoneGenerator.class);

  private static final String DEFAULT_CONFIG_FILE = "data/phone-prefixes.yml";

  /** 中国移动号段前缀。 */
  private static final List<String> CHINA_MOBILE_PREFIXES =
      Arrays.asList(
          "134", "135", "136", "137", "138", "139", "147", "148", "150", "151", "152", "157", "158",
          "159", "172", "178", "182", "183", "184", "187", "188", "195", "197", "198");

  /** 中国联通号段前缀。 */
  private static final List<String> CHINA_UNICOM_PREFIXES =
      Arrays.asList(
          "130", "131", "132", "145", "146", "155", "156", "166", "167", "171", "175", "176", "185",
          "186", "196");

  /** 中国电信号段前缀。 */
  private static final List<String> CHINA_TELECOM_PREFIXES =
      Arrays.asList(
          "133", "149", "153", "173", "174", "177", "180", "181", "189", "191", "193", "199");

  /** 虚拟运营商号段前缀。 */
  private static final List<String> VIRTUAL_OPERATOR_PREFIXES =
      Arrays.asList("162", "165", "167", "170", "171");

  /** 所有有效号段前缀。 */
  private static final List<String> ALL_VALID_PREFIXES;

  static {
    ALL_VALID_PREFIXES = new java.util.ArrayList<>();
    ALL_VALID_PREFIXES.addAll(CHINA_MOBILE_PREFIXES);
    ALL_VALID_PREFIXES.addAll(CHINA_UNICOM_PREFIXES);
    ALL_VALID_PREFIXES.addAll(CHINA_TELECOM_PREFIXES);
    ALL_VALID_PREFIXES.addAll(VIRTUAL_OPERATOR_PREFIXES);
  }

  /** 配置数据（从文件加载） */
  private volatile PhoneConfig phoneConfig;

  /** 无效前缀列表（从文件加载） */
  private volatile List<String> invalidPrefixes;

  private final Random random = new Random();
  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  public String getType() {
    return "phone";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      ensureConfigLoaded(config);

      String region = getStringParam(config, "region", "CN");

      if (!"CN".equalsIgnoreCase(region)) {
        logger.warn("Unsupported region: {}, using CN", region);
      }

      boolean valid = getBooleanParam(config, "valid", true);

      if (!valid) {
        return generateInvalidPhone();
      }

      String prefixParam = getStringParam(config, "prefix", null);
      List<String> allowedPrefixes = parseAllowedPrefixes(prefixParam);

      String operator = getStringParam(config, "operator", "ANY");

      return generateValidPhone(allowedPrefixes, operator, region);

    } catch (Exception e) {
      logger.error("Failed to generate phone number", e);
      return "13800138000";
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  /**
   * 生成有效的手机号码。
   *
   * @param allowedPrefixes 允许的前缀列表
   * @param operator 运营商类型
   * @param region 地区
   * @return 有效的手机号码
   */
  private String generateValidPhone(List<String> allowedPrefixes, String operator, String region) {
    List<String> prefixes = selectPrefixesByOperator(operator);

    if (allowedPrefixes != null && !allowedPrefixes.isEmpty()) {
      prefixes =
          prefixes.stream()
              .filter(allowedPrefixes::contains)
              .collect(java.util.stream.Collectors.toList());
    }

    if (prefixes.isEmpty()) {
      logger.warn("No valid prefixes found, using all valid prefixes");
      prefixes = ALL_VALID_PREFIXES;
    }

    String prefix = prefixes.get(random.nextInt(prefixes.size()));

    if ("CN".equalsIgnoreCase(region)) {
      StringBuilder phone = new StringBuilder(prefix);
      for (int i = 0; i < 8; i++) {
        phone.append(random.nextInt(10));
      }
      return phone.toString();
    }

    return generateInternationalPhone(prefix, region);
  }

  /**
   * 生成无效的手机号码。
   *
   * @return 无效的手机号码
   */
  private String generateInvalidPhone() {
    int type = random.nextInt(4);

    return switch (type) {
      case 0 -> generateWrongLengthPhone();
      case 1 -> generateWrongPrefixPhone();
      case 2 -> generateNonNumericPhone();
      default -> generateOtherInvalidPhone();
    };
  }

  /**
   * 生成长度错误的手机号码。
   *
   * @return 长度错误的手机号码
   */
  private String generateWrongLengthPhone() {
    int length =
        random.nextBoolean()
            ? random.nextInt(5) + 5
            : // 5-9位
            random.nextInt(5) + 12; // 12-16位

    StringBuilder phone = new StringBuilder();
    for (int i = 0; i < length; i++) {
      phone.append(random.nextInt(10));
    }

    return phone.toString();
  }

  /**
   * 生成前缀错误的手机号码。
   *
   * @return 前缀错误的手机号码
   */
  private String generateWrongPrefixPhone() {
    List<String> invalidPrefixList = invalidPrefixes;
    if (invalidPrefixList == null || invalidPrefixList.isEmpty()) {
      String[] defaultInvalidPrefixes = {
        "100", "101", "102", "110", "111", "120", "121", "122", "123", "124", "125", "126", "127",
        "128", "129"
      };
      invalidPrefixList = Arrays.asList(defaultInvalidPrefixes);
    }

    String prefix = invalidPrefixList.get(random.nextInt(invalidPrefixList.size()));

    StringBuilder phone = new StringBuilder(prefix);
    for (int i = 0; i < 8; i++) {
      phone.append(random.nextInt(10));
    }

    return phone.toString();
  }

  /**
   * 生成包含非数字字符的手机号码。
   *
   * @return 包含非数字字符的手机号码
   */
  private String generateNonNumericPhone() {
    String validPrefix = ALL_VALID_PREFIXES.get(random.nextInt(ALL_VALID_PREFIXES.size()));
    StringBuilder phone = new StringBuilder(validPrefix);

    // 在后8位中随机插入字母
    for (int i = 0; i < 8; i++) {
      if (random.nextInt(4) == 0) { // 25%概率插入字母
        phone.append((char) ('A' + random.nextInt(26)));
      } else {
        phone.append(random.nextInt(10));
      }
    }

    return phone.toString();
  }

  /**
   * 生成其他类型的无效手机号码。
   *
   * @return 其他无效手机号码
   */
  private String generateOtherInvalidPhone() {
    // 生成全0或全相同数字的号码
    if (random.nextBoolean()) {
      return "00000000000";
    } else {
      int digit = random.nextInt(10);
      return String.valueOf(digit).repeat(11);
    }
  }

  /**
   * 根据运营商类型选择前缀。
   *
   * @param operator 运营商类型
   * @return 前缀列表
   */
  private List<String> selectPrefixesByOperator(String operator) {
    return switch (operator.toUpperCase()) {
      case "MOBILE", "CHINA_MOBILE" -> CHINA_MOBILE_PREFIXES;
      case "UNICOM", "CHINA_UNICOM" -> CHINA_UNICOM_PREFIXES;
      case "TELECOM", "CHINA_TELECOM" -> CHINA_TELECOM_PREFIXES;
      case "VIRTUAL" -> VIRTUAL_OPERATOR_PREFIXES;
      default -> ALL_VALID_PREFIXES;
    };
  }

  /**
   * 解析允许的前缀参数。
   *
   * @param prefixParam 前缀参数字符串
   * @return 前缀列表
   */
  private List<String> parseAllowedPrefixes(String prefixParam) {
    if (prefixParam == null || prefixParam.trim().isEmpty()) {
      return null;
    }

    return Arrays.stream(prefixParam.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(java.util.stream.Collectors.toList());
  }

  @Override
  public String getDescription() {
    return "Phone number generator - generates Chinese mobile phone numbers with operator support";
  }

  /**
   * 确保配置已加载。
   *
   * @param config 配置
   */
  private void ensureConfigLoaded(FieldConfig config) {
    if (phoneConfig == null) {
      synchronized (this) {
        if (phoneConfig == null) {
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
      String configFile = getStringParam(config, "prefixes_file", DEFAULT_CONFIG_FILE);

      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
      if (inputStream != null) {
        phoneConfig = yamlMapper.readValue(inputStream, PhoneConfig.class);
        invalidPrefixes = phoneConfig.getInvalidPrefixes();
        logger.info("Phone config loaded from: {}", configFile);
      } else {
        logger.warn("Config file not found: {}, using fallback data", configFile);
        initializeFallbackConfig();
      }
    } catch (Exception e) {
      logger.error("Failed to load phone config, using fallback data", e);
      initializeFallbackConfig();
    }
  }

  /** 初始化fallback配置。 */
  private void initializeFallbackConfig() {
    phoneConfig = new PhoneConfig();
    invalidPrefixes =
        Arrays.asList("100", "101", "102", "110", "111", "120", "121", "122", "123", "124");
  }

  /**
   * 生成国际手机号码。
   *
   * @param prefix 前缀
   * @param region 地区
   * @return 国际手机号码
   */
  private String generateInternationalPhone(String prefix, String region) {
    if (phoneConfig == null || phoneConfig.getRegions() == null) {
      return prefix + String.valueOf(random.nextInt(100000000)).substring(0, 8);
    }

    Map<String, RegionConfig> regions = phoneConfig.getRegions();
    RegionConfig regionConfig = regions.get(region.toUpperCase());

    if (regionConfig == null) {
      return prefix + String.valueOf(random.nextInt(100000000)).substring(0, 8);
    }

    int length = regionConfig.getLength();
    String format = regionConfig.getFormat();

    StringBuilder phone = new StringBuilder(prefix);
    int remainingDigits = length - prefix.length();

    for (int i = 0; i < remainingDigits; i++) {
      phone.append(random.nextInt(10));
    }

    if (format != null && !format.isEmpty()) {
      return formatPhoneNumber(phone.toString(), format);
    }

    return phone.toString();
  }

  /**
   * 格式化手机号码。
   *
   * @param phone 手机号码
   * @param format 格式
   * @return 格式化后的手机号码
   */
  private String formatPhoneNumber(String phone, String format) {
    if (format.contains("#")) {
      int digitIndex = 0;
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < format.length(); i++) {
        char c = format.charAt(i);
        if (c == '#') {
          if (digitIndex < phone.length()) {
            result.append(phone.charAt(digitIndex));
            digitIndex++;
          }
        } else {
          result.append(c);
        }
      }
      return result.toString();
    }
    return phone;
  }

  /** 手机配置类。 */
  public static class PhoneConfig {
    private Map<String, OperatorConfig> operators;
    private Map<String, RegionConfig> regions;
    private List<String> invalidPrefixes;

    public Map<String, OperatorConfig> getOperators() {
      return operators;
    }

    public void setOperators(Map<String, OperatorConfig> operators) {
      this.operators = operators;
    }

    public Map<String, RegionConfig> getRegions() {
      return regions;
    }

    public void setRegions(Map<String, RegionConfig> regions) {
      this.regions = regions;
    }

    public List<String> getInvalidPrefixes() {
      return invalidPrefixes;
    }

    public void setInvalidPrefixes(List<String> invalidPrefixes) {
      this.invalidPrefixes = invalidPrefixes;
    }
  }

  /** 运营商配置类。 */
  public static class OperatorConfig {
    private String name;
    private List<String> prefixes;
    private int weight;
    private String description;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getPrefixes() {
      return prefixes;
    }

    public void setPrefixes(List<String> prefixes) {
      this.prefixes = prefixes;
    }

    public int getWeight() {
      return weight;
    }

    public void setWeight(int weight) {
      this.weight = weight;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  /** 地区配置类。 */
  public static class RegionConfig {
    private String name;
    private int length;
    private String format;
    private String description;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getLength() {
      return length;
    }

    public void setLength(int length) {
      this.length = length;
    }

    public String getFormat() {
      return format;
    }

    public void setFormat(String format) {
      this.format = format;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}
