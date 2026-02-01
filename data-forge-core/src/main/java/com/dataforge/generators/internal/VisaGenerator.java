package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 签证号生成器
 *
 * <p>支持生成各国签证号码，用于出入境系统、签证申请等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>country: 签发国家 (US|UK|CN|JP|DE|FR|CA|AU|CUSTOM) 默认: US
 *   <li>visa_type: 签证类型 (TOURIST|BUSINESS|STUDENT|WORK|TRANSIT) 默认: TOURIST
 *   <li>format: 输出格式 (NUMBER_ONLY|WITH_INFO|JSON) 默认: NUMBER_ONLY
 *   <li>valid: 是否生成有效格式 默认: true
 *   <li>issue_date: 签发日期
 *   <li>valid_months: 有效期（月）默认: 12
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class VisaGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(VisaGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 签证配置
  private static final Map<String, VisaConfig> VISA_CONFIGS = new HashMap<>();

  static {
    VISA_CONFIGS.put("US", new VisaConfig("", 8, "ALPHANUMERIC", "美国签证"));
    VISA_CONFIGS.put("UK", new VisaConfig("GBR", 9, "ALPHANUMERIC", "英国签证"));
    VISA_CONFIGS.put("CN", new VisaConfig("L", 9, "ALPHANUMERIC", "中国签证"));
    VISA_CONFIGS.put("JP", new VisaConfig("", 12, "ALPHANUMERIC", "日本签证"));
    VISA_CONFIGS.put("DE", new VisaConfig("D", 9, "ALPHANUMERIC", "德国签证"));
    VISA_CONFIGS.put("FR", new VisaConfig("", 11, "ALPHANUMERIC", "法国签证"));
    VISA_CONFIGS.put("CA", new VisaConfig("", 10, "ALPHANUMERIC", "加拿大签证"));
    VISA_CONFIGS.put("AU", new VisaConfig("", 13, "NUMERIC", "澳大利亚签证"));
    VISA_CONFIGS.put("CUSTOM", new VisaConfig("", 10, "ALPHANUMERIC", "自定义签证"));
  }

  // 签证类型前缀
  private static final Map<String, String> VISA_TYPE_PREFIXES = new HashMap<>();

  static {
    VISA_TYPE_PREFIXES.put("TOURIST", "B");
    VISA_TYPE_PREFIXES.put("BUSINESS", "B");
    VISA_TYPE_PREFIXES.put("STUDENT", "F");
    VISA_TYPE_PREFIXES.put("WORK", "H");
    VISA_TYPE_PREFIXES.put("TRANSIT", "C");
  }

  // 签证配置类
  private static class VisaConfig {
    final String prefix;
    final int length;
    final String charType;

    VisaConfig(String prefix, int length, String charType, String description) {
      this.prefix = prefix;
      this.length = length;
      this.charType = charType;
    }
  }

  @Override
  public String getType() {
    return "visa";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String country = getStringParam(config, "country", "US").toUpperCase();
      String visaType = getStringParam(config, "visa_type", "TOURIST").toUpperCase();
      String format = getStringParam(config, "format", "NUMBER_ONLY").toUpperCase();
      boolean valid = getBooleanParam(config, "valid", true);
      int validMonths = getIntParam(config, "valid_months", 12);

      VisaConfig visaConfig = VISA_CONFIGS.get(country);
      if (visaConfig == null) {
        logger.warn("Unknown country: {}, using US", country);
        visaConfig = VISA_CONFIGS.get("US");
        country = "US";
      }

      String visaNumber = generateVisaNumber(visaConfig, visaType, config, valid);
      LocalDate issueDate = parseIssueDate(config);
      LocalDate expiryDate = issueDate.plusMonths(validMonths);

      // 存储到上下文
      context.put("visa_country", country);
      context.put("visa_type", visaType);
      context.put("visa_number", visaNumber);
      context.put("visa_issue_date", issueDate);
      context.put("visa_expiry_date", expiryDate);

      return formatVisaNumber(visaNumber, format, country, visaType, issueDate, expiryDate);

    } catch (Exception e) {
      logger.error("Failed to generate visa number", e);
      return generateFallbackVisa();
    }
  }

  private String generateVisaNumber(
      VisaConfig config, String visaType, FieldConfig fieldConfig, boolean valid) {
    StringBuilder visa = new StringBuilder();

    // 国家前缀
    if (!config.prefix.isEmpty()) {
      visa.append(config.prefix);
    }

    // 签证类型前缀（主要用于美国签证）
    String typePrefix = VISA_TYPE_PREFIXES.get(visaType);
    if (typePrefix != null
        && "US".equals(getStringParam(fieldConfig, "country", "US").toUpperCase())) {
      visa.append(typePrefix);
    }

    // 主体部分长度
    int mainLength = config.length - visa.length();
    if (mainLength <= 0) {
      mainLength = 6; // 最小长度
    }

    // 生成主体部分
    String chars = getCharacterSet(config.charType);
    for (int i = 0; i < mainLength; i++) {
      visa.append(chars.charAt(random.nextInt(chars.length())));
    }

    // 如果需要生成无效签证号
    if (!valid) {
      return generateInvalidVisa(visa.toString());
    }

    return visa.toString();
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

  private String generateInvalidVisa(String validVisa) {
    // 生成无效签证号的几种方式
    int invalidType = random.nextInt(3);

    switch (invalidType) {
      case 0:
        // 长度不正确
        return validVisa + "X";
      case 1:
        // 包含非法字符
        return validVisa.substring(0, Math.max(1, validVisa.length() - 1)) + "@";
      case 2:
        // 全零或重复字符
        return "0".repeat(validVisa.length());
      default:
        return validVisa;
    }
  }

  private LocalDate parseIssueDate(FieldConfig config) {
    String issueDateStr = getStringParam(config, "issue_date", null);
    if (issueDateStr != null) {
      try {
        return LocalDate.parse(issueDateStr);
      } catch (Exception e) {
        logger.warn("Invalid issue date format: {}", issueDateStr);
      }
    }

    // 默认生成最近1-30天内的签发日期
    return LocalDate.now().minusDays(random.nextInt(30));
  }

  private String formatVisaNumber(
      String number,
      String format,
      String country,
      String visaType,
      LocalDate issueDate,
      LocalDate expiryDate) {
    switch (format) {
      case "WITH_INFO":
        return String.format(
            "%s (%s %s签证, 签发:%s, 到期:%s)",
            number,
            country,
            getVisaTypeName(visaType),
            issueDate.toString(),
            expiryDate.toString());
      case "JSON":
        return String.format(
            "{\"number\":\"%s\",\"country\":\"%s\",\"type\":\"%s\",\"issueDate\":\"%s\",\"expiryDate\":\"%s\"}",
            number, country, visaType, issueDate.toString(), expiryDate.toString());
      case "NUMBER_ONLY":
      default:
        return number;
    }
  }

  private String getVisaTypeName(String visaType) {
    switch (visaType) {
      case "TOURIST":
        return "旅游";
      case "BUSINESS":
        return "商务";
      case "STUDENT":
        return "学生";
      case "WORK":
        return "工作";
      case "TRANSIT":
        return "过境";
      default:
        return visaType;
    }
  }

  private String generateFallbackVisa() {
    return "B" + String.format("%07d", random.nextInt(10000000));
  }
}
