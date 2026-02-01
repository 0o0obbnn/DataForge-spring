package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 手机号码生成器（重构版本）。
 *
 * <p>使用 {@link BaseDataLoadingGenerator} 基类，简化数据加载逻辑。 生成符合中国大陆运营商号段规则的11位手机号码。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class PhoneGeneratorRefactored extends BaseDataLoadingGenerator<String>
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(PhoneGeneratorRefactored.class);

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

  /** 配置数据（从文件加载）。 */
  private PhoneConfig phoneConfig;

  /** 无效前缀列表（从文件加载）。 */
  private List<String> invalidPrefixes;

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  public String getType() {
    return "phone";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 使用基类的延迟加载机制
      ensureDataLoaded();

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

  // ==================== BaseDataLoadingGenerator 抽象方法实现 ====================

  @Override
  protected String getDataFilePath() {
    return DEFAULT_CONFIG_FILE;
  }

  @Override
  protected void parseData(List<String> lines) {
    try {
      String yamlContent = String.join("\n", lines);
      phoneConfig = yamlMapper.readValue(yamlContent, PhoneConfig.class);

      if (phoneConfig != null && phoneConfig.getInvalidPrefixes() != null) {
        invalidPrefixes = phoneConfig.getInvalidPrefixes();
      }

      logger.info("Phone prefixes config loaded successfully");
    } catch (Exception e) {
      logger.error("Failed to parse phone config, using fallback", e);
      initializeFallbackData();
    }
  }

  @Override
  protected void initializeFallbackData() {
    phoneConfig = new PhoneConfig();
    phoneConfig.setMobilePrefixes(CHINA_MOBILE_PREFIXES);
    phoneConfig.setUnicomPrefixes(CHINA_UNICOM_PREFIXES);
    phoneConfig.setTelecomPrefixes(CHINA_TELECOM_PREFIXES);
    phoneConfig.setVirtualPrefixes(VIRTUAL_OPERATOR_PREFIXES);
    invalidPrefixes = Arrays.asList("123", "456", "789");

    logger.info("Phone prefixes fallback data initialized");
  }

  // ==================== 私有辅助方法 ====================

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

    String prefix = prefixes.get(ThreadLocalRandom.current().nextInt(prefixes.size()));

    if ("CN".equalsIgnoreCase(region)) {
      StringBuilder phone = new StringBuilder(prefix);
      for (int i = 0; i < 8; i++) {
        phone.append(ThreadLocalRandom.current().nextInt(10));
      }
      return phone.toString();
    }

    return generateInternationalPhone(prefix, region);
  }

  private String generateInvalidPhone() {
    if (invalidPrefixes != null && !invalidPrefixes.isEmpty()) {
      String prefix =
          invalidPrefixes.get(ThreadLocalRandom.current().nextInt(invalidPrefixes.size()));
      StringBuilder phone = new StringBuilder(prefix);
      int remainingLength = 11 - prefix.length();
      for (int i = 0; i < remainingLength; i++) {
        phone.append(ThreadLocalRandom.current().nextInt(10));
      }
      return phone.toString();
    }

    // 生成一个明显无效的号码
    return "123" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
  }

  private String generateInternationalPhone(String prefix, String region) {
    // 简化实现，实际应根据地区生成不同格式的号码
    StringBuilder phone = new StringBuilder(prefix);
    for (int i = 0; i < 8; i++) {
      phone.append(ThreadLocalRandom.current().nextInt(10));
    }
    return phone.toString();
  }

  private List<String> selectPrefixesByOperator(String operator) {
    if (operator == null || "ANY".equalsIgnoreCase(operator)) {
      return ALL_VALID_PREFIXES;
    }

    return switch (operator.toUpperCase()) {
      case "MOBILE", "CMCC", "中国移动" -> CHINA_MOBILE_PREFIXES;
      case "UNICOM", "CUCC", "中国联通" -> CHINA_UNICOM_PREFIXES;
      case "TELECOM", "CTCC", "中国电信" -> CHINA_TELECOM_PREFIXES;
      case "VIRTUAL", "MVNO" -> VIRTUAL_OPERATOR_PREFIXES;
      default -> ALL_VALID_PREFIXES;
    };
  }

  private List<String> parseAllowedPrefixes(String prefixParam) {
    if (prefixParam == null || prefixParam.trim().isEmpty()) {
      return null;
    }

    return Arrays.asList(prefixParam.split(","));
  }

  /** 手机号段配置类。 */
  @SuppressWarnings("unused")
  private static class PhoneConfig {
    private List<String> mobilePrefixes;
    private List<String> unicomPrefixes;
    private List<String> telecomPrefixes;
    private List<String> virtualPrefixes;
    private List<String> invalidPrefixes;

    // Getter 和 Setter 保留用于 YAML 反序列化和后续扩展
    public List<String> getMobilePrefixes() {
      return mobilePrefixes;
    }

    public void setMobilePrefixes(List<String> mobilePrefixes) {
      this.mobilePrefixes = mobilePrefixes;
    }

    public List<String> getUnicomPrefixes() {
      return unicomPrefixes;
    }

    public void setUnicomPrefixes(List<String> unicomPrefixes) {
      this.unicomPrefixes = unicomPrefixes;
    }

    public List<String> getTelecomPrefixes() {
      return telecomPrefixes;
    }

    public void setTelecomPrefixes(List<String> telecomPrefixes) {
      this.telecomPrefixes = telecomPrefixes;
    }

    public List<String> getVirtualPrefixes() {
      return virtualPrefixes;
    }

    public void setVirtualPrefixes(List<String> virtualPrefixes) {
      this.virtualPrefixes = virtualPrefixes;
    }

    public List<String> getInvalidPrefixes() {
      return invalidPrefixes;
    }

    public void setInvalidPrefixes(List<String> invalidPrefixes) {
      this.invalidPrefixes = invalidPrefixes;
    }
  }
}
