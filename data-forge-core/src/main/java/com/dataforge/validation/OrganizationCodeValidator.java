package com.dataforge.validation;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 组织机构代码校验器。
 *
 * <p>实现中国组织机构代码的校验算法，遵循GB 11714-1997标准。
 *
 * <p>组织机构代码结构（9位）： - 前8位：本体代码，由数字和大写英文字母组成（不使用I、O、S、V、Z） - 第9位：校验码，由数字或大写英文字母X表示
 *
 * <p>校验算法： 1. 将前8位字符转换为对应的数值 2. 每位数值乘以对应的权重（3,7,9,10,5,8,4,2） 3. 求和后对11取模 4. 用11减去模值得到校验码 5.
 * 如果校验码为10，则用X表示；如果为11，则用0表示
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class OrganizationCodeValidator implements Validator<String> {

  private static final Logger logger = LoggerFactory.getLogger(OrganizationCodeValidator.class);

  /** 代码字符集（不包含I、O、S、V、Z）。 */
  private static final String CODE_SET = "0123456789ABCDEFGHJKLMNPQRTUWXY";

  /** 字符到数值的映射。 */
  private static final Map<Character, Integer> CHAR_TO_VALUE = new HashMap<>();

  /** 权重数组。 */
  private static final int[] WEIGHTS = {3, 7, 9, 10, 5, 8, 4, 2};

  static {
    // 初始化字符到数值的映射
    for (int i = 0; i < CODE_SET.length(); i++) {
      CHAR_TO_VALUE.put(CODE_SET.charAt(i), i);
    }
  }

  @Override
  public boolean isValid(String data) {
    return validate(data).isValid();
  }

  @Override
  public ValidationResult validate(String data) {
    if (data == null) {
      return ValidationResult.failure("Organization code cannot be null");
    }

    // 移除所有非字母数字字符并转换为大写
    String cleanData = data.replaceAll("[^0-9A-Z]", "").toUpperCase();

    if (cleanData.isEmpty()) {
      return ValidationResult.failure("Organization code cannot be empty");
    }

    // 长度校验
    if (cleanData.length() != 9) {
      return ValidationResult.failure("Organization code must be exactly 9 characters long");
    }

    try {
      // 字符集校验
      ValidationResult charSetResult = validateCharacterSet(cleanData);
      if (!charSetResult.isValid()) {
        return charSetResult;
      }

      // 校验码校验
      ValidationResult checkCodeResult = validateCheckCode(cleanData);
      if (!checkCodeResult.isValid()) {
        return checkCodeResult;
      }

      logger.debug("Organization code validation passed for: {}", maskOrgCode(data));
      return ValidationResult.success();

    } catch (Exception e) {
      logger.error("Error during organization code validation for: {}", maskOrgCode(data), e);
      return ValidationResult.failure(
          "Error during organization code validation: " + e.getMessage());
    }
  }

  /**
   * 校验字符集。
   *
   * @param orgCode 组织机构代码
   * @return 校验结果
   */
  private ValidationResult validateCharacterSet(String orgCode) {
    // 前8位字符校验
    String first8 = orgCode.substring(0, 8);
    for (char c : first8.toCharArray()) {
      if (!CHAR_TO_VALUE.containsKey(c)) {
        return ValidationResult.failure(
            "Invalid character in first 8 positions: " + c + ". Valid characters: " + CODE_SET);
      }
    }

    // 第9位校验码字符校验（可以是0-9或X）
    char checkChar = orgCode.charAt(8);
    if (!Character.isDigit(checkChar) && checkChar != 'X') {
      return ValidationResult.failure(
          "Invalid check code character: " + checkChar + ". Must be digit (0-9) or X");
    }

    return ValidationResult.success();
  }

  /**
   * 校验校验码。
   *
   * @param orgCode 完整的9位组织机构代码
   * @return 校验结果
   */
  private ValidationResult validateCheckCode(String orgCode) {
    String first8 = orgCode.substring(0, 8);
    char actualCheckCode = orgCode.charAt(8);
    char expectedCheckCode = calculateCheckCode(first8);

    if (actualCheckCode == expectedCheckCode) {
      return ValidationResult.success();
    } else {
      return ValidationResult.failure(
          String.format(
              "Check code mismatch. Expected: %c, Actual: %c", expectedCheckCode, actualCheckCode));
    }
  }

  /**
   * 计算组织机构代码的校验码。
   *
   * <p>算法步骤： 1. 将前8位字符转换为对应的数值 2. 每位数值乘以对应的权重 3. 求和后对11取模 4. 用11减去模值得到校验码 5.
   * 如果校验码为10，则用X表示；如果为11，则用0表示
   *
   * @param first8 前8位字符
   * @return 校验码字符
   */
  public char calculateCheckCode(String first8) {
    if (first8 == null || first8.length() != 8) {
      throw new IllegalArgumentException("First 8 characters must be exactly 8 characters");
    }

    // 校验字符集
    for (char c : first8.toCharArray()) {
      if (!CHAR_TO_VALUE.containsKey(c)) {
        throw new IllegalArgumentException("Invalid character: " + c);
      }
    }

    int sum = 0;
    for (int i = 0; i < 8; i++) {
      char c = first8.charAt(i);
      int value = CHAR_TO_VALUE.get(c);
      sum += value * WEIGHTS[i];
    }

    int remainder = sum % 11;
    int checkValue = 11 - remainder;

    // 特殊处理
    if (checkValue == 10) {
      return 'X';
    } else if (checkValue == 11) {
      return '0';
    } else {
      return (char) ('0' + checkValue);
    }
  }

  /**
   * 生成完整的有效组织机构代码。
   *
   * @param first8 前8位字符
   * @return 完整的9位组织机构代码
   */
  public String generateValidOrganizationCode(String first8) {
    char checkCode = calculateCheckCode(first8);
    return first8 + checkCode;
  }

  /**
   * 生成随机的组织机构代码本体（前8位）。
   *
   * @return 8位随机本体代码
   */
  public String generateRandomBodyCode() {
    StringBuilder sb = new StringBuilder();
    java.util.Random random = new java.util.Random();

    for (int i = 0; i < 8; i++) {
      int index = random.nextInt(CODE_SET.length());
      sb.append(CODE_SET.charAt(index));
    }

    return sb.toString();
  }

  /**
   * 生成完整的随机组织机构代码。
   *
   * @return 完整的9位组织机构代码
   */
  public String generateRandomOrganizationCode() {
    String bodyCode = generateRandomBodyCode();
    return generateValidOrganizationCode(bodyCode);
  }

  /**
   * 检查字符是否为有效的组织机构代码字符。
   *
   * @param c 字符
   * @return 如果是有效字符返回true，否则返回false
   */
  public boolean isValidCodeCharacter(char c) {
    return CHAR_TO_VALUE.containsKey(c);
  }

  /**
   * 获取有效的代码字符集。
   *
   * @return 代码字符集字符串
   */
  public String getValidCharacterSet() {
    return CODE_SET;
  }

  /**
   * 掩码组织机构代码用于日志记录。
   *
   * @param orgCode 原始组织机构代码
   * @return 掩码后的组织机构代码
   */
  private String maskOrgCode(String orgCode) {
    if (orgCode == null || orgCode.length() < 6) {
      return "****";
    }

    // 显示前3位和后3位，中间用*代替
    String prefix = orgCode.substring(0, 3);
    String suffix = orgCode.substring(orgCode.length() - 3);
    int maskLength = orgCode.length() - 6;
    String mask = "*".repeat(Math.max(0, maskLength));

    return prefix + mask + suffix;
  }

  /**
   * 格式化组织机构代码为标准格式（XXXXXXXX-X）。
   *
   * @param orgCode 组织机构代码
   * @return 格式化后的代码
   */
  public String formatOrganizationCode(String orgCode) {
    if (orgCode == null || orgCode.length() != 9) {
      return orgCode;
    }

    String cleanCode = orgCode.replaceAll("[^0-9A-Z]", "").toUpperCase();
    if (cleanCode.length() != 9) {
      return orgCode;
    }

    return cleanCode.substring(0, 8) + "-" + cleanCode.charAt(8);
  }

  /**
   * 解析格式化的组织机构代码。
   *
   * @param formattedCode 格式化的代码（如：XXXXXXXX-X）
   * @return 纯代码字符串
   */
  public String parseFormattedCode(String formattedCode) {
    if (formattedCode == null) {
      return null;
    }

    return formattedCode.replaceAll("[^0-9A-Z]", "").toUpperCase();
  }

  @Override
  public String getName() {
    return "OrganizationCode";
  }

  @Override
  public String getDescription() {
    return "Chinese organization code validator (GB 11714-1997)";
  }
}
