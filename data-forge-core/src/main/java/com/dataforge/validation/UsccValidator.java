package com.dataforge.validation;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 统一社会信用代码校验器。
 *
 * <p>实现中国统一社会信用代码（USCC）的校验算法，遵循GB32100-2015标准。
 *
 * <p>统一社会信用代码结构（18位）： - 第1位：登记管理部门代码 - 第2位：机构类别代码 - 第3-8位：登记管理机关行政区划码 - 第9-17位：主体标识码（组织机构代码） -
 * 第18位：校验码
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class UsccValidator implements Validator<String> {

  private static final Logger logger = LoggerFactory.getLogger(UsccValidator.class);

  /** 代码字符集。 */
  private static final String CODE_SET = "0123456789ABCDEFGHJKLMNPQRTUWXY";

  /** 字符到数值的映射。 */
  private static final Map<Character, Integer> CHAR_TO_VALUE = new HashMap<>();

  /** 权重数组。 */
  private static final int[] WEIGHTS = {
    1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28
  };

  static {
    // 初始化字符到数值的映射
    for (int i = 0; i < CODE_SET.length(); i++) {
      CHAR_TO_VALUE.put(CODE_SET.charAt(i), i);
    }
  }

  /** 登记管理部门代码映射。 */
  private static final Map<Character, String> REGISTRATION_DEPT = new HashMap<>();

  /** 机构类别代码映射。 */
  private static final Map<Character, String> ORGANIZATION_TYPE = new HashMap<>();

  static {
    // 登记管理部门代码
    REGISTRATION_DEPT.put('1', "机构编制");
    REGISTRATION_DEPT.put('5', "民政");
    REGISTRATION_DEPT.put('9', "工商");
    REGISTRATION_DEPT.put('Y', "其他");

    // 机构类别代码
    ORGANIZATION_TYPE.put('1', "企业");
    ORGANIZATION_TYPE.put('2', "事业单位");
    ORGANIZATION_TYPE.put('3', "社会团体");
    ORGANIZATION_TYPE.put('9', "其他");
    ORGANIZATION_TYPE.put('Y', "其他");
  }

  @Override
  public boolean isValid(String data) {
    return validate(data).isValid();
  }

  @Override
  public ValidationResult validate(String data) {
    if (data == null) {
      return ValidationResult.failure("USCC cannot be null");
    }

    // 移除所有非字母数字字符并转换为大写
    String cleanData = data.replaceAll("[^0-9A-Z]", "").toUpperCase();

    if (cleanData.isEmpty()) {
      return ValidationResult.failure("USCC cannot be empty");
    }

    // 长度校验
    if (cleanData.length() != 18) {
      return ValidationResult.failure("USCC must be exactly 18 characters long");
    }

    try {
      // 字符集校验
      ValidationResult charSetResult = validateCharacterSet(cleanData);
      if (!charSetResult.isValid()) {
        return charSetResult;
      }

      // 登记管理部门代码校验
      ValidationResult deptResult = validateRegistrationDepartment(cleanData.charAt(0));
      if (!deptResult.isValid()) {
        return deptResult;
      }

      // 机构类别代码校验
      ValidationResult typeResult = validateOrganizationType(cleanData.charAt(1));
      if (!typeResult.isValid()) {
        return typeResult;
      }

      // 行政区划代码校验
      ValidationResult regionResult = validateRegionCode(cleanData.substring(2, 8));
      if (!regionResult.isValid()) {
        return regionResult;
      }

      // 校验码校验
      ValidationResult checkCodeResult = validateCheckCode(cleanData);
      if (!checkCodeResult.isValid()) {
        return checkCodeResult;
      }

      logger.debug("USCC validation passed for: {}", maskUscc(data));
      return ValidationResult.success();

    } catch (Exception e) {
      logger.error("Error during USCC validation for: {}", maskUscc(data), e);
      return ValidationResult.failure("Error during USCC validation: " + e.getMessage());
    }
  }

  /**
   * 校验字符集。
   *
   * @param uscc USCC代码
   * @return 校验结果
   */
  private ValidationResult validateCharacterSet(String uscc) {
    for (char c : uscc.toCharArray()) {
      if (!CHAR_TO_VALUE.containsKey(c)) {
        return ValidationResult.failure(
            "Invalid character: " + c + ". Valid characters: " + CODE_SET);
      }
    }
    return ValidationResult.success();
  }

  /**
   * 校验登记管理部门代码。
   *
   * @param deptCode 登记管理部门代码
   * @return 校验结果
   */
  private ValidationResult validateRegistrationDepartment(char deptCode) {
    if (!REGISTRATION_DEPT.containsKey(deptCode)) {
      return ValidationResult.failure("Invalid registration department code: " + deptCode);
    }
    return ValidationResult.success();
  }

  /**
   * 校验机构类别代码。
   *
   * @param typeCode 机构类别代码
   * @return 校验结果
   */
  private ValidationResult validateOrganizationType(char typeCode) {
    if (!ORGANIZATION_TYPE.containsKey(typeCode)) {
      return ValidationResult.failure("Invalid organization type code: " + typeCode);
    }
    return ValidationResult.success();
  }

  /**
   * 校验行政区划代码。
   *
   * @param regionCode 6位行政区划代码
   * @return 校验结果
   */
  private ValidationResult validateRegionCode(String regionCode) {
    if (regionCode.length() != 6) {
      return ValidationResult.failure("Region code must be 6 characters");
    }

    // 检查是否全为数字
    if (!regionCode.matches("\\d{6}")) {
      return ValidationResult.failure("Region code must contain only digits");
    }

    // 基本的行政区划代码格式校验
    int provinceCode = Integer.parseInt(regionCode.substring(0, 2));
    if (provinceCode < 11 || provinceCode > 82) {
      return ValidationResult.failure("Invalid province code: " + provinceCode);
    }

    return ValidationResult.success();
  }

  /**
   * 校验校验码。
   *
   * @param uscc 完整的18位USCC代码
   * @return 校验结果
   */
  private ValidationResult validateCheckCode(String uscc) {
    String first17 = uscc.substring(0, 17);
    char actualCheckCode = uscc.charAt(17);
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
   * 计算USCC的校验码。
   *
   * <p>算法步骤： 1. 将前17位字符转换为对应的数值 2. 每位数值乘以对应的权重 3. 求和后对31取模 4. 用31减去模值得到校验码对应的数值 5. 将数值转换为对应的字符
   *
   * @param first17 前17位字符
   * @return 校验码字符
   */
  public char calculateCheckCode(String first17) {
    if (first17 == null || first17.length() != 17) {
      throw new IllegalArgumentException("First 17 characters must be exactly 17 characters");
    }

    // 校验字符集
    for (char c : first17.toCharArray()) {
      if (!CHAR_TO_VALUE.containsKey(c)) {
        throw new IllegalArgumentException("Invalid character: " + c);
      }
    }

    int sum = 0;
    for (int i = 0; i < 17; i++) {
      char c = first17.charAt(i);
      int value = CHAR_TO_VALUE.get(c);
      sum += value * WEIGHTS[i];
    }

    int remainder = sum % 31;
    int checkValue = (31 - remainder) % 31;

    return CODE_SET.charAt(checkValue);
  }

  /**
   * 生成完整的有效USCC代码。
   *
   * @param first17 前17位字符
   * @return 完整的18位USCC代码
   */
  public String generateValidUscc(String first17) {
    char checkCode = calculateCheckCode(first17);
    return first17 + checkCode;
  }

  /**
   * 解析USCC代码信息。
   *
   * @param uscc USCC代码
   * @return 解析结果
   */
  public UsccInfo parseUscc(String uscc) {
    if (uscc == null || uscc.length() != 18) {
      return null;
    }

    String cleanUscc = uscc.replaceAll("[^0-9A-Z]", "").toUpperCase();
    if (cleanUscc.length() != 18) {
      return null;
    }

    try {
      UsccInfo info = new UsccInfo();
      info.setUscc(cleanUscc);
      info.setRegistrationDepartment(REGISTRATION_DEPT.get(cleanUscc.charAt(0)));
      info.setOrganizationType(ORGANIZATION_TYPE.get(cleanUscc.charAt(1)));
      info.setRegionCode(cleanUscc.substring(2, 8));
      info.setOrganizationCode(cleanUscc.substring(8, 17));
      info.setCheckCode(cleanUscc.charAt(17));

      return info;
    } catch (Exception e) {
      logger.warn("Failed to parse USCC: {}", maskUscc(uscc), e);
      return null;
    }
  }

  /**
   * 掩码USCC代码用于日志记录。
   *
   * @param uscc 原始USCC代码
   * @return 掩码后的USCC代码
   */
  private String maskUscc(String uscc) {
    if (uscc == null || uscc.length() < 8) {
      return "****";
    }

    // 显示前4位和后4位，中间用*代替
    String prefix = uscc.substring(0, 4);
    String suffix = uscc.substring(uscc.length() - 4);
    int maskLength = uscc.length() - 8;
    String mask = "*".repeat(Math.max(0, maskLength));

    return prefix + mask + suffix;
  }

  @Override
  public String getName() {
    return "USCC";
  }

  @Override
  public String getDescription() {
    return "Unified Social Credit Code validator (GB32100-2015)";
  }

  /** USCC信息类。 */
  public static class UsccInfo {
    private String uscc;
    private String registrationDepartment;
    private String organizationType;
    private String regionCode;
    private String organizationCode;
    private char checkCode;

    // Getters and Setters
    public String getUscc() {
      return uscc;
    }

    public void setUscc(String uscc) {
      this.uscc = uscc;
    }

    public String getRegistrationDepartment() {
      return registrationDepartment;
    }

    public void setRegistrationDepartment(String registrationDepartment) {
      this.registrationDepartment = registrationDepartment;
    }

    public String getOrganizationType() {
      return organizationType;
    }

    public void setOrganizationType(String organizationType) {
      this.organizationType = organizationType;
    }

    public String getRegionCode() {
      return regionCode;
    }

    public void setRegionCode(String regionCode) {
      this.regionCode = regionCode;
    }

    public String getOrganizationCode() {
      return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
      this.organizationCode = organizationCode;
    }

    public char getCheckCode() {
      return checkCode;
    }

    public void setCheckCode(char checkCode) {
      this.checkCode = checkCode;
    }

    @Override
    public String toString() {
      return "UsccInfo{"
          + "uscc='"
          + uscc
          + '\''
          + ", registrationDepartment='"
          + registrationDepartment
          + '\''
          + ", organizationType='"
          + organizationType
          + '\''
          + ", regionCode='"
          + regionCode
          + '\''
          + ", organizationCode='"
          + organizationCode
          + '\''
          + ", checkCode="
          + checkCode
          + '}';
    }
  }
}
