package com.dataforge.generators.internal.idcard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 身份证校验辅助类。
 *
 * <p>提供身份证号码生成和校验的辅助方法。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class IdCardValidationHelper {

  /** 身份证权重系数。 */
  private static final int[] IDCARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

  /** 身份证校验码映射。 */
  private static final String IDCARD_CHECK_DIGITS = "10X98765432";

  /** 出生日期起始位置。 */
  public static final int IDCARD_BIRTH_DATE_START = 6;

  /** 出生日期结束位置。 */
  public static final int IDCARD_BIRTH_DATE_END = 14;

  /** 顺序码起始位置。 */
  public static final int IDCARD_SEQUENCE_START = 14;

  /** 顺序码结束位置。 */
  public static final int IDCARD_SEQUENCE_END = 17;

  /** 校验码位置。 */
  public static final int IDCARD_CHECK_DIGIT_INDEX = 17;

  /** 地区代码长度。 */
  public static final int IDCARD_REGION_CODE_LENGTH = 6;

  /** 身份证号码长度。 */
  public static final int IDCARD_LENGTH = 18;

  /**
   * 计算身份证校验码。
   *
   * @param prefix 前17位
   * @return 校验码
   */
  public static String calculateCheckDigit(String prefix) {
    int sum = 0;
    for (int i = 0; i < 17; i++) {
      sum += (prefix.charAt(i) - '0') * IDCARD_WEIGHTS[i];
    }
    return String.valueOf(IDCARD_CHECK_DIGITS.charAt(sum % 11));
  }

  /**
   * 验证身份证号码是否有效。
   *
   * @param idCard 身份证号码
   * @return 如果有效返回 true
   */
  public static boolean isValidIdCard(String idCard) {
    if (idCard == null || idCard.length() != IDCARD_LENGTH) {
      return false;
    }

    // 检查前17位是否都是数字
    for (int i = 0; i < 17; i++) {
      if (!Character.isDigit(idCard.charAt(i))) {
        return false;
      }
    }

    // 验证校验码
    String prefix = idCard.substring(0, 17);
    String checkDigit = calculateCheckDigit(prefix);
    return checkDigit.equalsIgnoreCase(String.valueOf(idCard.charAt(17)));
  }

  /**
   * 生成出生日期。
   *
   * @param birthDateRange 出生日期范围，格式：yyyy-MM-dd,yyyy-MM-dd
   * @return 出生日期字符串，格式：yyyyMMdd
   */
  public static String generateBirthDate(String birthDateRange) {
    String[] dates = birthDateRange.split(",");
    LocalDate startDate = LocalDate.parse(dates[0].trim());
    LocalDate endDate = dates.length > 1 ? LocalDate.parse(dates[1].trim()) : LocalDate.now();

    long startEpochDay = startDate.toEpochDay();
    long endEpochDay = endDate.toEpochDay();
    long randomDay =
        startEpochDay + ThreadLocalRandom.current().nextLong(endEpochDay - startEpochDay);

    return LocalDate.ofEpochDay(randomDay).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
  }

  /**
   * 生成顺序码。
   *
   * @param gender 性别：MALE, FEMALE, ANY
   * @return 3位顺序码
   */
  public static String generateSequenceCode(String gender) {
    int sequence = ThreadLocalRandom.current().nextInt(1, 1000);

    if (gender != null && !"ANY".equalsIgnoreCase(gender)) {
      boolean isMale = "MALE".equalsIgnoreCase(gender) || "M".equalsIgnoreCase(gender);
      if (isMale && sequence % 2 == 0) {
        sequence++;
      } else if (!isMale && sequence % 2 == 1) {
        sequence++;
      }
    }

    return String.format("%03d", sequence);
  }

  /**
   * 从身份证号码提取出生日期。
   *
   * @param idCard 身份证号码
   * @return 出生日期字符串，格式：yyyyMMdd
   */
  public static String extractBirthDate(String idCard) {
    if (idCard == null || idCard.length() < IDCARD_BIRTH_DATE_END) {
      return null;
    }
    return idCard.substring(IDCARD_BIRTH_DATE_START, IDCARD_BIRTH_DATE_END);
  }

  /**
   * 从身份证号码提取性别。
   *
   * @param idCard 身份证号码
   * @return 性别：男/女
   */
  public static String extractGender(String idCard) {
    if (idCard == null || idCard.length() < IDCARD_SEQUENCE_END) {
      return null;
    }
    try {
      int sequenceCode =
          Integer.parseInt(idCard.substring(IDCARD_SEQUENCE_START, IDCARD_SEQUENCE_END));
      return (sequenceCode % 2 == 0) ? "女" : "男";
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * 从身份证号码提取地区代码。
   *
   * @param idCard 身份证号码
   * @return 地区代码
   */
  public static String extractRegionCode(String idCard) {
    if (idCard == null || idCard.length() < IDCARD_REGION_CODE_LENGTH) {
      return null;
    }
    return idCard.substring(0, IDCARD_REGION_CODE_LENGTH);
  }

  /**
   * 计算年龄。
   *
   * @param birthDate 出生日期，格式：yyyyMMdd
   * @return 年龄
   */
  public static int calculateAge(String birthDate) {
    if (birthDate == null || birthDate.length() != 8) {
      return 0;
    }
    try {
      int year = Integer.parseInt(birthDate.substring(0, 4));
      int currentYear = LocalDate.now().getYear();
      return currentYear - year;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * 掩码身份证号码。
   *
   * @param idCard 身份证号码
   * @return 掩码后的号码
   */
  public static String maskIdCard(String idCard) {
    if (idCard == null || idCard.length() != IDCARD_LENGTH) {
      return idCard;
    }
    return idCard.substring(0, 6) + "********" + idCard.substring(14);
  }
}
