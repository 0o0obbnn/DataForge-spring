package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 护照号生成器
 *
 * <p>支持生成各国护照号码，用于身份验证、出入境系统等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>country: 国家代码 (CN|US|UK|JP|DE|FR|CA|AU|CUSTOM) 默认: CN
 *   <li>format: 输出格式 (PLAIN|FORMATTED) 默认: PLAIN
 *   <li>valid: 是否生成有效格式 默认: true
 *   <li>prefix: 自定义前缀（仅CUSTOM类型）
 *   <li>length: 自定义长度（仅CUSTOM类型）默认: 9
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class PassportGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(PassportGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 护照配置
  private static final Map<String, PassportConfig> PASSPORT_CONFIGS = new HashMap<>();

  static {
    PASSPORT_CONFIGS.put("CN", new PassportConfig("E", 8, "ALPHANUMERIC", "中国护照"));
    PASSPORT_CONFIGS.put("US", new PassportConfig("", 9, "NUMERIC", "美国护照"));
    PASSPORT_CONFIGS.put("UK", new PassportConfig("", 9, "NUMERIC", "英国护照"));
    PASSPORT_CONFIGS.put("JP", new PassportConfig("", 9, "ALPHANUMERIC", "日本护照"));
    PASSPORT_CONFIGS.put("DE", new PassportConfig("", 10, "ALPHANUMERIC", "德国护照"));
    PASSPORT_CONFIGS.put("FR", new PassportConfig("", 9, "ALPHANUMERIC", "法国护照"));
    PASSPORT_CONFIGS.put("CA", new PassportConfig("", 8, "ALPHANUMERIC", "加拿大护照"));
    PASSPORT_CONFIGS.put("AU", new PassportConfig("", 9, "ALPHANUMERIC", "澳大利亚护照"));
    PASSPORT_CONFIGS.put("CUSTOM", new PassportConfig("", 9, "ALPHANUMERIC", "自定义护照"));
  }

  // 护照配置类
  private static class PassportConfig {
    final String prefix;
    final int length;
    final String charType;
    final String description;

    PassportConfig(String prefix, int length, String charType, String description) {
      this.prefix = prefix;
      this.length = length;
      this.charType = charType;
      this.description = description;
    }
  }

  @Override
  public String getType() {
    return "passport";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String country = getStringParam(config, "country", "CN").toUpperCase();
      PassportConfig passportConfig = PASSPORT_CONFIGS.get(country);
      if (passportConfig == null) {
        logger.warn("Unknown country: {}, using CN", country);
        passportConfig = PASSPORT_CONFIGS.get("CN");
        country = "CN";
      }

      String format = getStringParam(config, "format", "PLAIN");
      boolean valid = getBooleanParam(config, "valid", true);

      String passportNumber = generatePassportNumber(passportConfig, config, valid);

      // 存储到上下文
      context.put("passport_country", country);
      context.put("passport_type", passportConfig.description);

      return formatPassportNumber(passportNumber, format, country);

    } catch (Exception e) {
      logger.error("Failed to generate passport number", e);
      return "E" + String.format("%08d", random.nextInt(100000000));
    }
  }

  private String generatePassportNumber(
      PassportConfig config, FieldConfig fieldConfig, boolean valid) {
    StringBuilder passport = new StringBuilder();

    // 前缀处理
    String prefix = config.prefix;
    if ("CUSTOM".equals(getStringParam(fieldConfig, "country", "CN").toUpperCase())) {
      prefix = getStringParam(fieldConfig, "prefix", "");
    }

    if (!prefix.isEmpty()) {
      passport.append(prefix);
    }

    // 主体部分长度
    int length = config.length;
    if ("CUSTOM".equals(getStringParam(fieldConfig, "country", "CN").toUpperCase())) {
      length = getIntParam(fieldConfig, "length", 9);
    }

    // 减去前缀长度
    int mainLength = length - prefix.length();
    if (mainLength <= 0) {
      mainLength = 6; // 最小长度
    }

    // 生成主体部分
    String chars = getCharacterSet(config.charType);
    for (int i = 0; i < mainLength; i++) {
      passport.append(chars.charAt(random.nextInt(chars.length())));
    }

    // 如果需要生成无效护照号
    if (!valid) {
      return generateInvalidPassport(passport.toString());
    }

    return passport.toString();
  }

  private String getCharacterSet(String charType) {
    switch (charType.toUpperCase()) {
      case "NUMERIC":
        return "0123456789";
      case "ALPHANUMERIC":
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
      case "ALPHA":
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
      default:
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    }
  }

  private String generateInvalidPassport(String validPassport) {
    // 生成无效护照号的几种方式
    int invalidType = random.nextInt(3);

    switch (invalidType) {
      case 0:
        // 长度不正确
        return validPassport + "X";
      case 1:
        // 包含非法字符
        return validPassport.substring(0, Math.max(1, validPassport.length() - 1)) + "@";
      case 2:
        // 全零或重复字符
        return "0".repeat(validPassport.length());
      default:
        return validPassport;
    }
  }

  private String formatPassportNumber(String passportNumber, String format, String country) {
    switch (format.toUpperCase()) {
      case "FORMATTED":
        return formatWithSpaces(passportNumber, country);
      case "PLAIN":
      default:
        return passportNumber;
    }
  }

  private String formatWithSpaces(String passportNumber, String country) {
    // 根据不同国家的格式添加空格
    switch (country) {
      case "CN":
        // 中国护照：E + 8位数字，格式：E 12345678
        if (passportNumber.length() >= 2) {
          return passportNumber.substring(0, 1) + " " + passportNumber.substring(1);
        }
        break;
      case "US":
      case "UK":
        // 美英护照：每3位添加空格
        if (passportNumber.length() >= 6) {
          return passportNumber.substring(0, 3)
              + " "
              + passportNumber.substring(3, 6)
              + " "
              + passportNumber.substring(6);
        }
        break;
      default:
        // 其他国家：每4位添加空格
        if (passportNumber.length() >= 4) {
          StringBuilder formatted = new StringBuilder();
          for (int i = 0; i < passportNumber.length(); i += 4) {
            if (i > 0) formatted.append(" ");
            formatted.append(passportNumber.substring(i, Math.min(i + 4, passportNumber.length())));
          }
          return formatted.toString();
        }
        break;
    }

    return passportNumber;
  }
}
