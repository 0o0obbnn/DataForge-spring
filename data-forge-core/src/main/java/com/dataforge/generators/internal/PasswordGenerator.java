package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 密码生成器
 *
 * <p>支持功能： 1. 指定长度和复杂度的密码生成 2. 支持多种复杂度级别 3. 支持常见弱密码生成（用于安全测试） 4. 支持密码强度评估 5. 支持自定义字符集
 *
 * <p>参数配置： - length: 密码长度范围 "min,max"（默认"8,16"） - complexity: 复杂度级别
 * LOW|MEDIUM|HIGH|CUSTOM（默认MEDIUM） - custom_chars: 自定义字符集（当complexity=CUSTOM时使用） - include_weak:
 * 是否包含常见弱密码（默认false） - weak_ratio: 弱密码占比 0.0-1.0（默认0.1） - require_uppercase: 是否必须包含大写字母（默认false） -
 * require_lowercase: 是否必须包含小写字母（默认false） - require_digits: 是否必须包含数字（默认false） - require_special:
 * 是否必须包含特殊字符（默认false） - exclude_ambiguous: 是否排除易混淆字符（默认false）
 *
 * @author DataForge
 * @since 1.0.0
 */
public class PasswordGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger log = LoggerFactory.getLogger(PasswordGenerator.class);

  private static final String TYPE = "password";
  private static final String DEFAULT_LENGTH = "8,16";
  private static final String DEFAULT_COMPLEXITY = "MEDIUM";
  private static final boolean DEFAULT_INCLUDE_WEAK = false;
  private static final double DEFAULT_WEAK_RATIO = 0.1;

  // 字符集定义
  private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String DIGIT_CHARS = "0123456789";
  private static final String SIMPLE_SPECIAL_CHARS = "!@#$%^&*";

  // 易混淆字符
  private static final String AMBIGUOUS_CHARS = "0O1lI|";

  // 常见弱密码列表
  private static final List<String> WEAK_PASSWORDS =
      Arrays.asList(
          "123456",
          "password",
          "123456789",
          "12345678",
          "12345",
          "1234567",
          "1234567890",
          "qwerty",
          "abc123",
          "111111",
          "123123",
          "admin",
          "letmein",
          "welcome",
          "monkey",
          "dragon",
          "pass",
          "master",
          "hello",
          "freedom",
          "whatever",
          "qazwsx",
          "trustno1",
          "jordan",
          "harley",
          "1234",
          "robert",
          "matthew",
          "jordan23",
          "daniel",
          "andrew",
          "joshua",
          "1qaz2wsx",
          "shadow",
          "hunter",
          "michael",
          "tigger",
          "123qwe",
          "football",
          "password1",
          "123456a",
          "a123456",
          "123abc",
          "abc123456",
          "password123",
          "admin123",
          "root123",
          "test123",
          "user123",
          "guest123",
          "demo123",
          "temp123",
          "pass123");

  // 复杂度级别定义
  public enum ComplexityLevel {
    LOW, // 纯数字或纯字母
    MEDIUM, // 数字+字母
    HIGH, // 数字+大小写字母+特殊字符
    CUSTOM // 自定义字符集
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    // 解析配置参数
    String lengthStr = getStringParam(config, "length", DEFAULT_LENGTH);
    String complexityStr = getStringParam(config, "complexity", DEFAULT_COMPLEXITY);
    String customChars = getStringParam(config, "custom_chars", "");
    boolean includeWeak = getBooleanParam(config, "include_weak", DEFAULT_INCLUDE_WEAK);
    double weakRatio = getDoubleParam(config, "weak_ratio", DEFAULT_WEAK_RATIO);
    boolean requireUppercase = getBooleanParam(config, "require_uppercase", false);
    boolean requireLowercase = getBooleanParam(config, "require_lowercase", false);
    boolean requireDigits = getBooleanParam(config, "require_digits", false);
    boolean requireSpecial = getBooleanParam(config, "require_special", false);
    boolean excludeAmbiguous = getBooleanParam(config, "exclude_ambiguous", false);

    // 解析长度范围
    int[] lengthRange = parseLengthRange(lengthStr);
    int minLength = lengthRange[0];
    int maxLength = lengthRange[1];

    // 解析复杂度级别
    ComplexityLevel complexity;
    try {
      complexity = ComplexityLevel.valueOf(complexityStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid complexity level: {}. Using MEDIUM.", complexityStr);
      complexity = ComplexityLevel.MEDIUM;
    }

    // 参数校验
    weakRatio = Math.max(0.0, Math.min(1.0, weakRatio));

    // 决定是否生成弱密码
    ThreadLocalRandom random = ThreadLocalRandom.current();
    if (includeWeak && random.nextDouble() < weakRatio) {
      return generateWeakPassword(minLength, maxLength);
    }

    // 生成强密码
    return generateStrongPassword(
        minLength,
        maxLength,
        complexity,
        customChars,
        requireUppercase,
        requireLowercase,
        requireDigits,
        requireSpecial,
        excludeAmbiguous);
  }

  /** 解析长度范围 */
  private int[] parseLengthRange(String lengthStr) {
    try {
      if (lengthStr.contains(",")) {
        String[] parts = lengthStr.split(",");
        int min = Integer.parseInt(parts[0].trim());
        int max = Integer.parseInt(parts[1].trim());
        return new int[] {Math.max(1, min), Math.max(min, max)};
      } else {
        int length = Integer.parseInt(lengthStr.trim());
        return new int[] {Math.max(1, length), Math.max(1, length)};
      }
    } catch (Exception e) {
      log.warn("Invalid length parameter: {}. Using default.", lengthStr);
      return new int[] {8, 16};
    }
  }

  /** 生成弱密码 */
  private String generateWeakPassword(int minLength, int maxLength) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    // 从弱密码列表中选择合适长度的密码
    List<String> suitablePasswords = new ArrayList<>();
    for (String weakPassword : WEAK_PASSWORDS) {
      if (weakPassword.length() >= minLength && weakPassword.length() <= maxLength) {
        suitablePasswords.add(weakPassword);
      }
    }

    if (!suitablePasswords.isEmpty()) {
      return suitablePasswords.get(random.nextInt(suitablePasswords.size()));
    }

    // 如果没有合适的弱密码，生成简单的数字密码
    int length = random.nextInt(minLength, maxLength + 1);
    StringBuilder password = new StringBuilder();

    // 生成简单的重复数字或连续数字
    if (random.nextBoolean()) {
      // 重复数字
      char digit = (char) ('0' + random.nextInt(10));
      for (int i = 0; i < length; i++) {
        password.append(digit);
      }
    } else {
      // 连续数字
      int start = random.nextInt(10 - Math.min(length, 9));
      for (int i = 0; i < length; i++) {
        password.append((char) ('0' + (start + i) % 10));
      }
    }

    return password.toString();
  }

  /** 生成强密码 */
  private String generateStrongPassword(
      int minLength,
      int maxLength,
      ComplexityLevel complexity,
      String customChars,
      boolean requireUppercase,
      boolean requireLowercase,
      boolean requireDigits,
      boolean requireSpecial,
      boolean excludeAmbiguous) {

    ThreadLocalRandom random = ThreadLocalRandom.current();
    int length = random.nextInt(minLength, maxLength + 1);

    // 构建字符集
    String charSet = buildCharSet(complexity, customChars, excludeAmbiguous);

    // 构建必需字符集合
    List<String> requiredCharSets =
        buildRequiredCharSets(
            complexity,
            requireUppercase,
            requireLowercase,
            requireDigits,
            requireSpecial,
            excludeAmbiguous);

    // 生成密码
    StringBuilder password = new StringBuilder();

    // 首先确保包含所有必需的字符类型
    for (String requiredSet : requiredCharSets) {
      if (password.length() < length && !requiredSet.isEmpty()) {
        password.append(requiredSet.charAt(random.nextInt(requiredSet.length())));
      }
    }

    // 填充剩余长度
    while (password.length() < length) {
      password.append(charSet.charAt(random.nextInt(charSet.length())));
    }

    // 打乱字符顺序
    return shuffleString(password.toString(), random);
  }

  /** 构建字符集 */
  private String buildCharSet(
      ComplexityLevel complexity, String customChars, boolean excludeAmbiguous) {
    StringBuilder charSet = new StringBuilder();

    switch (complexity) {
      case LOW:
        // 随机选择纯数字或纯字母
        if (ThreadLocalRandom.current().nextBoolean()) {
          charSet.append(DIGIT_CHARS);
        } else {
          charSet.append(LOWERCASE_CHARS);
        }
        break;

      case MEDIUM:
        charSet.append(LOWERCASE_CHARS).append(UPPERCASE_CHARS).append(DIGIT_CHARS);
        break;

      case HIGH:
        charSet
            .append(LOWERCASE_CHARS)
            .append(UPPERCASE_CHARS)
            .append(DIGIT_CHARS)
            .append(SIMPLE_SPECIAL_CHARS);
        break;

      case CUSTOM:
        if (!customChars.isEmpty()) {
          charSet.append(customChars);
        } else {
          // 如果没有提供自定义字符集，使用MEDIUM级别
          charSet.append(LOWERCASE_CHARS).append(UPPERCASE_CHARS).append(DIGIT_CHARS);
        }
        break;
    }

    // 排除易混淆字符
    if (excludeAmbiguous) {
      String result = charSet.toString();
      for (char ambiguous : AMBIGUOUS_CHARS.toCharArray()) {
        result = result.replace(String.valueOf(ambiguous), "");
      }
      return result;
    }

    return charSet.toString();
  }

  /** 构建必需字符集合 */
  private List<String> buildRequiredCharSets(
      ComplexityLevel complexity,
      boolean requireUppercase,
      boolean requireLowercase,
      boolean requireDigits,
      boolean requireSpecial,
      boolean excludeAmbiguous) {

    List<String> requiredSets = new ArrayList<>();

    // 根据复杂度级别自动确定要求
    if (complexity == ComplexityLevel.MEDIUM || complexity == ComplexityLevel.HIGH) {
      requireLowercase = true;
      requireUppercase = true;
      requireDigits = true;
    }

    if (complexity == ComplexityLevel.HIGH) {
      requireSpecial = true;
    }

    // 构建必需字符集
    if (requireLowercase) {
      String chars =
          excludeAmbiguous
              ? LOWERCASE_CHARS.replaceAll("[" + AMBIGUOUS_CHARS + "]", "")
              : LOWERCASE_CHARS;
      requiredSets.add(chars);
    }

    if (requireUppercase) {
      String chars =
          excludeAmbiguous
              ? UPPERCASE_CHARS.replaceAll("[" + AMBIGUOUS_CHARS + "]", "")
              : UPPERCASE_CHARS;
      requiredSets.add(chars);
    }

    if (requireDigits) {
      String chars =
          excludeAmbiguous ? DIGIT_CHARS.replaceAll("[" + AMBIGUOUS_CHARS + "]", "") : DIGIT_CHARS;
      requiredSets.add(chars);
    }

    if (requireSpecial) {
      requiredSets.add(SIMPLE_SPECIAL_CHARS);
    }

    return requiredSets;
  }

  /** 打乱字符串 */
  private String shuffleString(String input, ThreadLocalRandom random) {
    List<Character> characters = new ArrayList<>();
    for (char c : input.toCharArray()) {
      characters.add(c);
    }

    Collections.shuffle(characters, random);

    StringBuilder result = new StringBuilder();
    for (char c : characters) {
      result.append(c);
    }

    return result.toString();
  }
}
