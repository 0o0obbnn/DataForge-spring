package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 驾驶证号生成器
 *
 * <p>支持生成驾驶证号码，用于交通管理、身份验证等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>country: 国家 (CN|US|CUSTOM) 默认: CN
 *   <li>link_idcard: 是否关联身份证号 默认: true
 *   <li>issue_date: 签发日期 (yyyy-MM-dd格式)
 *   <li>valid_years: 有效年限 默认: 6
 *   <li>license_type: 驾照类型 (C1|C2|B1|B2|A1|A2|A3) 默认: C1
 *   <li>format: 输出格式 (NUMBER_ONLY|WITH_INFO|JSON) 默认: NUMBER_ONLY
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class DriverLicenseGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(DriverLicenseGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 驾照类型权重
  private static final String[] LICENSE_TYPES = {"C1", "C2", "B1", "B2", "A1", "A2", "A3"};
  private static final double[] TYPE_WEIGHTS = {0.6, 0.25, 0.05, 0.03, 0.03, 0.03, 0.01};

  @Override
  public String getType() {
    return "driver_license";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String country = getStringParam(config, "country", "CN");
      boolean linkIdCard = getBooleanParam(config, "link_idcard", true);
      String format = getStringParam(config, "format", "NUMBER_ONLY");

      String driverLicenseNumber;

      switch (country.toUpperCase()) {
        case "CN":
          driverLicenseNumber = generateChineseDriverLicense(config, context, linkIdCard);
          break;
        case "US":
          driverLicenseNumber = generateUSDriverLicense(config, context);
          break;
        case "CUSTOM":
          driverLicenseNumber = generateCustomDriverLicense(config);
          break;
        default:
          logger.warn("Unknown country: {}, using CN", country);
          driverLicenseNumber = generateChineseDriverLicense(config, context, linkIdCard);
          break;
      }

      // 存储到上下文
      String licenseType = getStringParam(config, "license_type", selectLicenseType());
      LocalDate issueDate = parseIssueDate(config);
      int validYears = getIntParam(config, "valid_years", 6);

      context.put("driver_license_number", driverLicenseNumber);
      context.put("driver_license_type", licenseType);
      context.put("driver_license_issue_date", issueDate);
      context.put("driver_license_expire_date", issueDate.plusYears(validYears));

      return formatDriverLicense(driverLicenseNumber, format, licenseType, issueDate, validYears);

    } catch (Exception e) {
      logger.error("Failed to generate driver license", e);
      return generateFallbackDriverLicense();
    }
  }

  private String generateChineseDriverLicense(
      FieldConfig config, DataForgeContext context, boolean linkIdCard) {
    if (linkIdCard) {
      // 尝试从上下文获取身份证号
      String idCard = context.get("generated_idcard", String.class).orElse(null);
      if (idCard != null && idCard.length() == 18) {
        // 中国驾驶证号通常与身份证号相同
        return idCard;
      }
    }

    // 生成独立的18位驾驶证号（格式与身份证相同）
    StringBuilder license = new StringBuilder();

    // 地区码（6位）
    String regionCode = String.format("%06d", 110000 + random.nextInt(900000));
    license.append(regionCode);

    // 出生日期（8位）
    LocalDate birthDate = generateBirthDate();
    license.append(birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

    // 顺序码（3位）
    license.append(String.format("%03d", 1 + random.nextInt(999)));

    // 校验位（1位）
    license.append(calculateCheckDigit(license.toString()));

    return license.toString();
  }

  private String generateUSDriverLicense(FieldConfig config, DataForgeContext context) {
    // 美国驾照格式因州而异，这里生成一个通用格式
    StringBuilder license = new StringBuilder();

    // 字母前缀（1-2位）
    license.append((char) ('A' + random.nextInt(26)));
    if (random.nextBoolean()) {
      license.append((char) ('A' + random.nextInt(26)));
    }

    // 数字部分（6-8位）
    int numLength = 6 + random.nextInt(3);
    for (int i = 0; i < numLength; i++) {
      license.append(random.nextInt(10));
    }

    return license.toString();
  }

  private String generateCustomDriverLicense(FieldConfig config) {
    int length = getIntParam(config, "length", 12);
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    StringBuilder license = new StringBuilder();
    for (int i = 0; i < length; i++) {
      license.append(chars.charAt(random.nextInt(chars.length())));
    }

    return license.toString();
  }

  private LocalDate generateBirthDate() {
    // 生成18-70岁的出生日期
    int age = 18 + random.nextInt(53);
    return LocalDate.now().minusYears(age).minusDays(random.nextInt(365));
  }

  private String calculateCheckDigit(String number) {
    // 身份证校验位算法
    int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    String[] checkCodes = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};

    int sum = 0;
    for (int i = 0; i < 17; i++) {
      sum += Character.getNumericValue(number.charAt(i)) * weights[i];
    }

    return checkCodes[sum % 11];
  }

  private String selectLicenseType() {
    double rand = random.nextDouble();
    double cumulative = 0.0;

    for (int i = 0; i < LICENSE_TYPES.length; i++) {
      cumulative += TYPE_WEIGHTS[i];
      if (rand <= cumulative) {
        return LICENSE_TYPES[i];
      }
    }

    return LICENSE_TYPES[0]; // 默认C1
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

    // 默认生成最近1-5年内的签发日期
    return LocalDate.now().minusDays(random.nextInt(1825));
  }

  private String formatDriverLicense(
      String number, String format, String type, LocalDate issueDate, int validYears) {
    switch (format.toUpperCase()) {
      case "WITH_INFO":
        return String.format(
            "%s (类型:%s, 签发:%s, 有效期:%d年)", number, type, issueDate.toString(), validYears);
      case "JSON":
        return String.format(
            "{\"number\":\"%s\",\"type\":\"%s\",\"issueDate\":\"%s\",\"validYears\":%d,\"expireDate\":\"%s\"}",
            number,
            type,
            issueDate.toString(),
            validYears,
            issueDate.plusYears(validYears).toString());
      case "NUMBER_ONLY":
      default:
        return number;
    }
  }

  private String generateFallbackDriverLicense() {
    return "110000"
        + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        + String.format("%03d", random.nextInt(1000))
        + "1";
  }
}
