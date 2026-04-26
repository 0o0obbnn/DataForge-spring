package com.dataforge.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Luhn算法校验器。
 *
 * <p>实现Luhn算法（也称为模10算法）用于校验银行卡号、IMEI号等数字序列的有效性。 Luhn算法是一种简单的校验和算法，广泛用于各种识别号码的校验。
 *
 * <p>算法步骤： 1. 从右到左，对偶数位数字乘以2 2. 如果乘积大于9，则将乘积的各位数字相加 3. 将所有数字相加 4. 如果总和能被10整除，则校验通过
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class LuhnValidator implements Validator<String> {

  private static final Logger logger = LoggerFactory.getLogger(LuhnValidator.class);

  @Override
  public boolean isValid(String data) {
    return validate(data).isValid();
  }

  @Override
  public ValidationResult validate(String data) {
    if (data == null) {
      return ValidationResult.failure("Input cannot be null");
    }

    // 移除所有非数字字符
    String cleanData = data.replaceAll("\\D", "");

    if (cleanData.isEmpty()) {
      return ValidationResult.failure("Input must contain at least one digit");
    }

    if (cleanData.length() < 2) {
      return ValidationResult.failure("Input must contain at least 2 digits for Luhn validation");
    }

    try {
      boolean isValid = performLuhnCheck(cleanData);

      if (isValid) {
        logger.debug("Luhn validation passed for input: {}", maskSensitiveData(data));
        return ValidationResult.success();
      } else {
        logger.debug("Luhn validation failed for input: {}", maskSensitiveData(data));
        return ValidationResult.failure("Luhn checksum validation failed");
      }

    } catch (Exception e) {
      logger.error("Error during Luhn validation for input: {}", maskSensitiveData(data), e);
      return ValidationResult.failure("Error during Luhn validation: " + e.getMessage());
    }
  }

  /**
   * 执行Luhn算法校验。
   *
   * @param digits 纯数字字符串
   * @return 如果校验通过返回true，否则返回false
   */
  private boolean performLuhnCheck(String digits) {
    int sum = 0;
    boolean alternate = false;

    // 从右到左遍历数字
    for (int i = digits.length() - 1; i >= 0; i--) {
      int digit = Character.getNumericValue(digits.charAt(i));

      if (alternate) {
        digit *= 2;
        if (digit > 9) {
          digit = (digit % 10) + 1; // 等价于将两位数的各位相加
        }
      }

      sum += digit;
      alternate = !alternate;
    }

    return (sum % 10) == 0;
  }

  /**
   * 生成符合Luhn算法的校验位。
   *
   * @param partialNumber 不包含校验位的部分号码
   * @return 校验位（0-9）
   */
  public int generateCheckDigit(String partialNumber) {
    if (partialNumber == null || partialNumber.isEmpty()) {
      throw new IllegalArgumentException("Partial number cannot be null or empty");
    }

    // 移除所有非数字字符
    String cleanNumber = partialNumber.replaceAll("\\D", "");

    if (cleanNumber.isEmpty()) {
      throw new IllegalArgumentException("Partial number must contain at least one digit");
    }

    // 在末尾添加一个0作为临时校验位
    String tempNumber = cleanNumber + "0";

    // 计算当前的校验和
    int sum = 0;
    boolean alternate = false;

    for (int i = tempNumber.length() - 1; i >= 0; i--) {
      int digit = Character.getNumericValue(tempNumber.charAt(i));

      if (alternate) {
        digit *= 2;
        if (digit > 9) {
          digit = (digit % 10) + 1;
        }
      }

      sum += digit;
      alternate = !alternate;
    }

    // 计算需要的校验位
    int checkDigit = (10 - (sum % 10)) % 10;

    logger.debug(
        "Generated Luhn check digit {} for partial number: {}",
        checkDigit,
        maskSensitiveData(partialNumber));

    return checkDigit;
  }

  /**
   * 生成完整的符合Luhn算法的号码。
   *
   * @param partialNumber 不包含校验位的部分号码
   * @return 包含校验位的完整号码
   */
  public String generateValidNumber(String partialNumber) {
    int checkDigit = generateCheckDigit(partialNumber);
    String cleanNumber = partialNumber.replaceAll("\\D", "");
    return cleanNumber + checkDigit;
  }

  /**
   * 校验银行卡号。
   *
   * <p>银行卡号通常为13-19位数字，最后一位为校验位。
   *
   * @param cardNumber 银行卡号
   * @return 校验结果
   */
  public ValidationResult validateBankCard(String cardNumber) {
    if (cardNumber == null) {
      return ValidationResult.failure("Bank card number cannot be null");
    }

    String cleanNumber = cardNumber.replaceAll("\\D", "");

    if (cleanNumber.length() < 13 || cleanNumber.length() > 19) {
      return ValidationResult.failure("Bank card number must be 13-19 digits long");
    }

    return validate(cleanNumber);
  }

  /**
   * 校验IMEI号。
   *
   * <p>IMEI号为15位数字，最后一位为校验位。
   *
   * @param imei IMEI号
   * @return 校验结果
   */
  public ValidationResult validateIMEI(String imei) {
    if (imei == null) {
      return ValidationResult.failure("IMEI cannot be null");
    }

    String cleanIMEI = imei.replaceAll("\\D", "");

    if (cleanIMEI.length() != 15) {
      return ValidationResult.failure("IMEI must be exactly 15 digits long");
    }

    return validate(cleanIMEI);
  }

  /**
   * 掩码敏感数据用于日志记录。
   *
   * @param data 原始数据
   * @return 掩码后的数据
   */
  private String maskSensitiveData(String data) {
    if (data == null || data.length() <= 4) {
      return "****";
    }

    String cleanData = data.replaceAll("\\D", "");
    if (cleanData.length() <= 4) {
      return "****";
    }

    // 显示前4位和后4位，中间用*代替
    String prefix = cleanData.substring(0, 4);
    String suffix = cleanData.substring(cleanData.length() - 4);
    int maskLength = cleanData.length() - 8;
    String mask = "*".repeat(Math.max(0, maskLength));

    return prefix + mask + suffix;
  }

  @Override
  public String getName() {
    return "Luhn";
  }

  @Override
  public String getDescription() {
    return "Luhn algorithm validator for bank cards, IMEI numbers and other identification numbers";
  }
}
