package com.dataforge.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 中国大陆居民身份证号码校验器。
 *
 * <p>实现中国大陆18位身份证号码的校验算法，包括： 1. 长度校验（18位） 2. 地区代码校验 3. 出生日期校验 4. 校验位算法校验
 *
 * <p>身份证号码结构：XXXXXX YYYYMMDD SSS C - 前6位：地区代码 - 第7-14位：出生日期（YYYYMMDD） - 第15-17位：顺序码SSS（奇数为男性，偶数为女性）
 * - 第18位：校验码C
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class IdCardValidator implements Validator<String> {

  private static final Logger logger = LoggerFactory.getLogger(IdCardValidator.class);

  /** 校验位权重数组。 */
  private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

  /** 校验位对应表。 */
  private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

  /** 常见地区代码映射（部分）。 实际应用中应该使用完整的行政区划代码表。 */
  private static final Map<String, String> REGION_CODES = new HashMap<>();

  static {
    // 初始化部分地区代码（实际应用中应该加载完整的行政区划代码表）
    REGION_CODES.put("110000", "北京市");
    REGION_CODES.put("110100", "北京市市辖区");
    REGION_CODES.put("110101", "北京市东城区");
    REGION_CODES.put("110102", "北京市西城区");
    REGION_CODES.put("120000", "天津市");
    REGION_CODES.put("130000", "河北省");
    REGION_CODES.put("140000", "山西省");
    REGION_CODES.put("150000", "内蒙古自治区");
    REGION_CODES.put("210000", "辽宁省");
    REGION_CODES.put("220000", "吉林省");
    REGION_CODES.put("230000", "黑龙江省");
    REGION_CODES.put("310000", "上海市");
    REGION_CODES.put("320000", "江苏省");
    REGION_CODES.put("330000", "浙江省");
    REGION_CODES.put("330100", "浙江省杭州市");
    REGION_CODES.put("330106", "浙江省杭州市西湖区");
    REGION_CODES.put("340000", "安徽省");
    REGION_CODES.put("350000", "福建省");
    REGION_CODES.put("360000", "江西省");
    REGION_CODES.put("370000", "山东省");
    REGION_CODES.put("410000", "河南省");
    REGION_CODES.put("420000", "湖北省");
    REGION_CODES.put("430000", "湖南省");
    REGION_CODES.put("440000", "广东省");
    REGION_CODES.put("450000", "广西壮族自治区");
    REGION_CODES.put("460000", "海南省");
    REGION_CODES.put("500000", "重庆市");
    REGION_CODES.put("510000", "四川省");
    REGION_CODES.put("520000", "贵州省");
    REGION_CODES.put("530000", "云南省");
    REGION_CODES.put("540000", "西藏自治区");
    REGION_CODES.put("610000", "陕西省");
    REGION_CODES.put("620000", "甘肃省");
    REGION_CODES.put("630000", "青海省");
    REGION_CODES.put("640000", "宁夏回族自治区");
    REGION_CODES.put("650000", "新疆维吾尔自治区");
    REGION_CODES.put("710000", "台湾省");
    REGION_CODES.put("810000", "香港特别行政区");
    REGION_CODES.put("820000", "澳门特别行政区");
  }

  @Override
  public boolean isValid(String data) {
    return validate(data).isValid();
  }

  @Override
  public ValidationResult validate(String data) {
    if (data == null) {
      return ValidationResult.failure("ID card number cannot be null");
    }

    // 移除所有非字母数字字符
    String cleanData = data.replaceAll("[^0-9Xx]", "").toUpperCase();

    if (cleanData.isEmpty()) {
      return ValidationResult.failure("ID card number cannot be empty");
    }

    // 长度校验
    if (cleanData.length() != 18) {
      return ValidationResult.failure("ID card number must be exactly 18 characters long");
    }

    try {
      // 前17位必须是数字
      String first17 = cleanData.substring(0, 17);
      if (!first17.matches("\\d{17}")) {
        return ValidationResult.failure("First 17 characters must be digits");
      }

      // 地区代码校验
      ValidationResult regionResult = validateRegionCode(first17.substring(0, 6));
      if (!regionResult.isValid()) {
        return regionResult;
      }

      // 出生日期校验
      ValidationResult birthDateResult = validateBirthDate(first17.substring(6, 14));
      if (!birthDateResult.isValid()) {
        return birthDateResult;
      }

      // 校验位校验
      ValidationResult checkCodeResult = validateCheckCode(cleanData);
      if (!checkCodeResult.isValid()) {
        return checkCodeResult;
      }

      logger.debug("ID card validation passed for: {}", maskIdCard(data));
      return ValidationResult.success();

    } catch (Exception e) {
      logger.error("Error during ID card validation for: {}", maskIdCard(data), e);
      return ValidationResult.failure("Error during ID card validation: " + e.getMessage());
    }
  }

  /**
   * 校验地区代码。
   *
   * @param regionCode 6位地区代码
   * @return 校验结果
   */
  private ValidationResult validateRegionCode(String regionCode) {
    if (regionCode.length() != 6) {
      return ValidationResult.failure("Region code must be 6 digits");
    }

    if (!regionCode.matches("\\d{6}")) {
      return ValidationResult.failure("Region code must contain only digits");
    }

    // 基本的地区代码格式校验
    // 第1-2位：省、自治区、直辖市代码（11-82）
    int provinceCode = Integer.parseInt(regionCode.substring(0, 2));
    if (provinceCode < 11 || provinceCode > 82) {
      return ValidationResult.failure("Invalid province code: " + provinceCode);
    }

    // 检查是否为已知的地区代码（可选，因为完整的地区代码表很大）
    String provincePrefix = regionCode.substring(0, 2) + "0000";
    if (REGION_CODES.containsKey(provincePrefix)) {
      logger.debug("Region code {} belongs to {}", regionCode, REGION_CODES.get(provincePrefix));
    }

    return ValidationResult.success();
  }

  /**
   * 校验出生日期。
   *
   * @param birthDateStr 8位出生日期字符串（YYYYMMDD）
   * @return 校验结果
   */
  private ValidationResult validateBirthDate(String birthDateStr) {
    if (birthDateStr.length() != 8) {
      return ValidationResult.failure("Birth date must be 8 digits");
    }

    if (!birthDateStr.matches("\\d{8}")) {
      return ValidationResult.failure("Birth date must contain only digits");
    }

    try {
      // 解析日期
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      LocalDate birthDate = LocalDate.parse(birthDateStr, formatter);

      // 检查日期范围（1900年1月1日到当前日期）
      LocalDate minDate = LocalDate.of(1900, 1, 1);
      LocalDate maxDate = LocalDate.now();

      if (birthDate.isBefore(minDate)) {
        return ValidationResult.failure("Birth date cannot be before " + minDate);
      }

      if (birthDate.isAfter(maxDate)) {
        return ValidationResult.failure("Birth date cannot be in the future");
      }

      logger.debug("Birth date validation passed: {}", birthDate);
      return ValidationResult.success();

    } catch (DateTimeParseException e) {
      return ValidationResult.failure("Invalid birth date format: " + birthDateStr);
    }
  }

  /**
   * 校验校验位。
   *
   * @param idCard 完整的18位身份证号码
   * @return 校验结果
   */
  private ValidationResult validateCheckCode(String idCard) {
    String first17 = idCard.substring(0, 17);
    char actualCheckCode = idCard.charAt(17);
    char expectedCheckCode = calculateCheckCode(first17);

    if (actualCheckCode == expectedCheckCode) {
      return ValidationResult.success();
    } else {
      return ValidationResult.failure(
          String.format(
              "Check code mismatch. Expected: %c, Actual: %c", expectedCheckCode, actualCheckCode));
    }
  }

  /**
   * 计算身份证号码的校验位。
   *
   * @param first17 前17位数字
   * @return 校验位字符
   */
  public char calculateCheckCode(String first17) {
    if (first17 == null || first17.length() != 17 || !first17.matches("\\d{17}")) {
      throw new IllegalArgumentException("First 17 characters must be exactly 17 digits");
    }

    int sum = 0;
    for (int i = 0; i < 17; i++) {
      int digit = Character.getNumericValue(first17.charAt(i));
      sum += digit * WEIGHTS[i];
    }

    int remainder = sum % 11;
    return CHECK_CODES[remainder];
  }

  /**
   * 生成完整的有效身份证号码。
   *
   * @param first17 前17位数字
   * @return 完整的18位身份证号码
   */
  public String generateValidIdCard(String first17) {
    char checkCode = calculateCheckCode(first17);
    return first17 + checkCode;
  }

  /**
   * 从身份证号码中提取性别。
   *
   * @param idCard 身份证号码
   * @return "M"表示男性，"F"表示女性，null表示无法确定
   */
  public String extractGender(String idCard) {
    if (idCard == null || idCard.length() < 17) {
      return null;
    }

    try {
      // 第17位数字决定性别
      int genderDigit = Character.getNumericValue(idCard.charAt(16));
      return (genderDigit % 2 == 1) ? "M" : "F";
    } catch (Exception e) {
      logger.warn("Failed to extract gender from ID card: {}", maskIdCard(idCard), e);
      return null;
    }
  }

  /**
   * 从身份证号码中提取出生日期。
   *
   * @param idCard 身份证号码
   * @return 出生日期，如果无法解析则返回null
   */
  public LocalDate extractBirthDate(String idCard) {
    if (idCard == null || idCard.length() < 14) {
      return null;
    }

    try {
      String birthDateStr = idCard.substring(6, 14);
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      return LocalDate.parse(birthDateStr, formatter);
    } catch (Exception e) {
      logger.warn("Failed to extract birth date from ID card: {}", maskIdCard(idCard), e);
      return null;
    }
  }

  /**
   * 从身份证号码中提取地区代码。
   *
   * @param idCard 身份证号码
   * @return 6位地区代码，如果无法提取则返回null
   */
  public String extractRegionCode(String idCard) {
    if (idCard == null || idCard.length() < 6) {
      return null;
    }

    return idCard.substring(0, 6);
  }

  /**
   * 掩码身份证号码用于日志记录。
   *
   * @param idCard 原始身份证号码
   * @return 掩码后的身份证号码
   */
  private String maskIdCard(String idCard) {
    if (idCard == null || idCard.length() < 8) {
      return "****";
    }

    // 显示前4位和后4位，中间用*代替
    String prefix = idCard.substring(0, 4);
    String suffix = idCard.substring(idCard.length() - 4);
    int maskLength = idCard.length() - 8;
    String mask = "*".repeat(Math.max(0, maskLength));

    return prefix + mask + suffix;
  }

  @Override
  public String getName() {
    return "IdCard";
  }

  @Override
  public String getDescription() {
    return "Chinese mainland resident ID card validator (18-digit)";
  }
}
