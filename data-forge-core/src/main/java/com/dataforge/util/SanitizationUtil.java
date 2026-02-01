package com.dataforge.util;

import java.util.regex.Pattern;

/**
 * 敏感信息脱敏工具类。
 *
 * <p>用于对日志和错误消息中的敏感信息进行脱敏处理。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public final class SanitizationUtil {

  // 密码脱敏模式
  private static final Pattern PASSWORD_PATTERN =
      Pattern.compile("(?i)(password|pwd|pass)\\s*[:=]\\s*[^\\s&\"',}]+");

  // 密钥脱敏模式
  private static final Pattern SECRET_PATTERN =
      Pattern.compile("(?i)(secret|key|token)\\s*[:=]\\s*[^\\s&\"',}]+");

  // 邮箱脱敏模式
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

  // 手机号脱敏模式（中国大陆：11位）
  private static final Pattern PHONE_PATTERN = Pattern.compile("\\b1[3-9]\\d{9}\\b");

  // 身份证号脱敏模式（18位）
  private static final Pattern ID_CARD_PATTERN =
      Pattern.compile(
          "\\b[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}[0-9Xx]\\b");

  // 银行卡号脱敏模式（16-19位）
  private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\b\\d{16,19}\\b");

  private SanitizationUtil() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * 脱敏消息中的所有敏感信息。
   *
   * @param message 原始消息
   * @return 脱敏后的消息
   */
  public static String sanitize(String message) {
    if (message == null || message.isEmpty()) {
      return message;
    }

    String sanitized = message;

    // 脱敏密码
    sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1=*****");

    // 脱敏密钥/Token
    sanitized = SECRET_PATTERN.matcher(sanitized).replaceAll("$1=*****");

    // 脱敏邮箱
    sanitized =
        EMAIL_PATTERN
            .matcher(sanitized)
            .replaceAll(match -> SanitizationUtil.maskEmail(match.group()));

    // 脱敏手机号
    sanitized =
        PHONE_PATTERN
            .matcher(sanitized)
            .replaceAll(match -> SanitizationUtil.maskPhone(match.group()));

    // 脱敏身份证号
    sanitized =
        ID_CARD_PATTERN
            .matcher(sanitized)
            .replaceAll(match -> SanitizationUtil.maskIdCard(match.group()));

    // 脱敏银行卡号
    sanitized =
        BANK_CARD_PATTERN
            .matcher(sanitized)
            .replaceAll(match -> SanitizationUtil.maskBankCard(match.group()));

    return sanitized;
  }

  /**
   * 脱敏密码。
   *
   * @param password 密码（明文或哈希值）
   * @return 脱敏后的密码
   */
  public static String maskPassword(String password) {
    if (password == null || password.isEmpty()) {
      return password;
    }
    return "*****";
  }

  /**
   * 脱敏邮箱。
   *
   * @param email 邮箱地址
   * @return 脱敏后的邮箱（如：a***@example.com）
   */
  public static String maskEmail(String email) {
    if (email == null || email.isEmpty()) {
      return email;
    }

    int atIndex = email.indexOf('@');
    if (atIndex <= 1) {
      return email.charAt(0) + "***" + email.substring(atIndex);
    }

    return email.charAt(0) + "***" + email.substring(atIndex);
  }

  /**
   * 脱敏手机号。
   *
   * @param phone 手机号
   * @return 脱敏后的手机号（如：138****5678）
   */
  public static String maskPhone(String phone) {
    if (phone == null || phone.length() != 11) {
      return phone;
    }

    return phone.substring(0, 3) + "****" + phone.substring(7);
  }

  /**
   * 脱敏身份证号。
   *
   * @param idCard 身份证号
   * @return 脱敏后的身份证号（如：110101********1234）
   */
  public static String maskIdCard(String idCard) {
    if (idCard == null || idCard.length() < 10) {
      return idCard;
    }

    return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
  }

  /**
   * 脱敏银行卡号。
   *
   * @param bankCard 银行卡号
   * @return 脱敏后的银行卡号（如：6222**********1234）
   */
  public static String maskBankCard(String bankCard) {
    if (bankCard == null || bankCard.length() < 8) {
      return bankCard;
    }

    return bankCard.substring(0, 4) + "********" + bankCard.substring(bankCard.length() - 4);
  }

  /**
   * 脱敏IP地址（部分脱敏）。
   *
   * @param ip IP地址
   * @return 脱敏后的IP地址（如：192.168.*.*）
   */
  public static String maskIp(String ip) {
    if (ip == null || ip.isEmpty()) {
      return ip;
    }

    String[] parts = ip.split("\\.");
    if (parts.length != 4) {
      return ip;
    }

    return parts[0] + "." + parts[1] + ".*.*";
  }

  /**
   * 脱敏Token（保留开头和结尾部分）。
   *
   * @param token Token字符串
   * @return 脱敏后的Token
   */
  public static String maskToken(String token) {
    if (token == null || token.length() < 10) {
      return token == null ? null : "*****";
    }

    int showChars = Math.min(5, token.length() / 2);
    return token.substring(0, showChars) + "..." + token.substring(token.length() - showChars);
  }

  /**
   * 截断过长的消息。
   *
   * @param message 原始消息
   * @param maxLength 最大长度
   * @return 截断后的消息
   */
  public static String truncate(String message, int maxLength) {
    if (message == null || message.length() <= maxLength) {
      return message;
    }

    return message.substring(0, maxLength) + "... [truncated]";
  }
}
