package com.dataforge.config;

/**
 * CLI参数解析工具类。
 *
 * <p>提供类型安全的参数解析和友好的错误消息，用于命令行参数验证。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public final class CliArgumentParser {

  private CliArgumentParser() {
    // 工具类，禁止实例化
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * 解析整数参数。
   *
   * @param paramName 参数名称（用于错误消息）
   * @param value 参数值
   * @return 解析后的整数
   * @throws IllegalArgumentException 当value无法解析为整数时
   */
  public static int parseInt(String paramName, String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          String.format("Invalid %s value: '%s'. Must be a valid integer.", paramName, value), e);
    }
  }

  /**
   * 解析长整数参数。
   *
   * @param paramName 参数名称（用于错误消息）
   * @param value 参数值
   * @return 解析后的长整数
   * @throws IllegalArgumentException 当value无法解析为长整数时
   */
  public static long parseLong(String paramName, String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          String.format("Invalid %s value: '%s'. Must be a valid long.", paramName, value), e);
    }
  }

  /**
   * 解析布尔参数。
   *
   * @param paramName 参数名称（用于错误消息）
   * @param value 参数值
   * @return 解析后的布尔值
   * @throws IllegalArgumentException 当value无效时
   */
  public static boolean parseBoolean(String paramName, String value) {
    try {
      return Boolean.parseBoolean(value);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid %s value: '%s'. Must be 'true' or 'false'.", paramName, value),
          e);
    }
  }

  /**
   * 解析带范围验证的整数。
   *
   * @param paramName 参数名称
   * @param value 参数值
   * @param min 最小值（包含）
   * @param max 最大值（包含）
   * @return 解析并验证后的整数
   * @throws IllegalArgumentException 当值无效或超出范围时
   */
  public static int parseIntInRange(String paramName, String value, int min, int max) {
    int parsed = parseInt(paramName, value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException(
          String.format(
              "%s must be between %,d and %,d, got: %,d", paramName, min, max, parsed));
    }
    return parsed;
  }

  /**
   * 解析带范围验证的长整数。
   *
   * @param paramName 参数名称
   * @param value 参数值
   * @param min 最小值（包含）
   * @param max 最大值（包含）
   * @return 解析并验证后的长整数
   * @throws IllegalArgumentException 当值无效或超出范围时
   */
  public static long parseLongInRange(String paramName, String value, long min, long max) {
    long parsed = parseLong(paramName, value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException(
          String.format(
              "%s must be between %,d and %,d, got: %,d", paramName, min, max, parsed));
    }
    return parsed;
  }
}
