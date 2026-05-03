package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * 字符串生成器。
 *
 * <p>生成各种类型的字符串数据，支持自定义长度、字符集和格式。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class StringGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  /** 默认字符串长度。 */
  private static final int DEFAULT_LENGTH = 10;

  /** 默认字符集。 */
  private static final String DEFAULT_CHARSET = "ALPHANUMERIC";

  /** 字母字符集。 */
  private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  /** 数字字符集。 */
  private static final String DIGITS = "0123456789";

  /** 字母数字字符集。 */
  private static final String ALPHANUMERIC = LETTERS + DIGITS;

  /** 特殊字符集。 */
  private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

  /** 所有字符集。 */
  private static final String ALL_CHARS = ALPHANUMERIC + SPECIAL_CHARS;

  @Override
  public String getType() {
    return "string";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      int length = getIntParam(config, "length", DEFAULT_LENGTH);
      String charset = getStringParam(config, "charset", DEFAULT_CHARSET).toUpperCase();
      String prefix = getStringParam(config, "prefix", "");
      String suffix = getStringParam(config, "suffix", "");
      String customChars = getStringParam(config, "custom_chars", "");

      // 确保长度在合理范围内
      if (length < 1) {
        length = DEFAULT_LENGTH;
      }
      if (length > 1000) {
        length = 1000;
      }

      // 选择字符集
      String chars = getCharacterSet(charset, customChars);

      // 生成字符串
      StringBuilder result = new StringBuilder();
      result.append(prefix);

      int remainingLength = length - prefix.length() - suffix.length();
      if (remainingLength > 0) {
        for (int i = 0; i < remainingLength; i++) {
          int index = ThreadLocalRandom.current().nextInt(chars.length());
          result.append(chars.charAt(index));
        }
      }

      result.append(suffix);

      String generated = result.toString();

      // 将生成的字符串放入上下文
      if (context != null) {
        context.put("string", generated);
      }

      return generated;

    } catch (Exception e) {
      // 发生异常时返回默认字符串
      return generateDefaultString();
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  /**
   * 获取字符集。
   *
   * @param charset 字符集类型
   * @param customChars 自定义字符
   * @return 字符集字符串
   */
  private String getCharacterSet(String charset, String customChars) {
    if (customChars != null && !customChars.trim().isEmpty()) {
      return customChars;
    }

    return switch (charset) {
      case "LETTERS" -> LETTERS;
      case "DIGITS" -> DIGITS;
      case "ALPHANUMERIC" -> ALPHANUMERIC;
      case "SPECIAL" -> SPECIAL_CHARS;
      case "ALL" -> ALL_CHARS;
      default -> ALPHANUMERIC;
    };
  }

  /**
   * 生成默认字符串。
   *
   * @return 默认字符串
   */
  private String generateDefaultString() {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < DEFAULT_LENGTH; i++) {
      int index = ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length());
      result.append(ALPHANUMERIC.charAt(index));
    }
    return result.toString();
  }

  @Override
  public String getDescription() {
    return "String generator - generates random strings with customizable length, charset, and format";
  }
}
