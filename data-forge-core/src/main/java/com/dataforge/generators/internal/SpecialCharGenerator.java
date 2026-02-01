package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Random;

/**
 * 特殊字符生成器 生成包含各种特殊字符的文本
 *
 * <p>支持的参数： - type: 特殊字符类型 (WHITESPACE|SYMBOLS|ESCAPED|ALL，默认ALL) - length: 生成字符串的长度 (默认10) -
 * frequency: 特殊字符在生成文本中的出现频率 (0-1，默认0.3) - includeNormal: 是否包含普通字符 (默认true)
 */
public class SpecialCharGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Random RANDOM = new Random();

  // 空白字符
  private static final String WHITESPACE_CHARS = " \t\n\r\f";

  // 常见符号
  private static final String COMMON_SYMBOLS = "!@#$%^&*()_+-=[]{}|;:'\",.<>/?`~";

  // 转义字符
  private static final String ESCAPED_CHARS = "\\\'\"\0\b\t\n\f\r";

  // 普通字符
  private static final String NORMAL_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  @Override
  public String getType() {
    return "specialchar";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    String type = getStringParam(config, "type", "ALL");
    int length = getIntParam(config, "length", 10);
    double frequency = getDoubleParam(config, "frequency", 0.3);
    boolean includeNormal = getBooleanParam(config, "includeNormal", true);

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < length; i++) {
      if (RANDOM.nextDouble() < frequency) {
        // 生成特殊字符
        String specialChar = generateSpecialChar(type);
        result.append(specialChar);
      } else if (includeNormal) {
        // 生成普通字符
        char normalChar = NORMAL_CHARS.charAt(RANDOM.nextInt(NORMAL_CHARS.length()));
        result.append(normalChar);
      } else {
        // 如果不包含普通字符，继续生成特殊字符
        String specialChar = generateSpecialChar(type);
        result.append(specialChar);
      }
    }

    return result.toString();
  }

  /** 生成特殊字符 */
  private String generateSpecialChar(String type) {
    switch (type.toUpperCase()) {
      case "WHITESPACE":
        return String.valueOf(WHITESPACE_CHARS.charAt(RANDOM.nextInt(WHITESPACE_CHARS.length())));
      case "SYMBOLS":
        return String.valueOf(COMMON_SYMBOLS.charAt(RANDOM.nextInt(COMMON_SYMBOLS.length())));
      case "ESCAPED":
        return String.valueOf(ESCAPED_CHARS.charAt(RANDOM.nextInt(ESCAPED_CHARS.length())));
      case "ALL":
      default:
        return generateRandomSpecialChar();
    }
  }

  /** 随机生成特殊字符 */
  private String generateRandomSpecialChar() {
    int charType = RANDOM.nextInt(3);

    switch (charType) {
      case 0:
        return String.valueOf(WHITESPACE_CHARS.charAt(RANDOM.nextInt(WHITESPACE_CHARS.length())));
      case 1:
        return String.valueOf(COMMON_SYMBOLS.charAt(RANDOM.nextInt(COMMON_SYMBOLS.length())));
      case 2:
      default:
        return String.valueOf(ESCAPED_CHARS.charAt(RANDOM.nextInt(ESCAPED_CHARS.length())));
    }
  }
}
