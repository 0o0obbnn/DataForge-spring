package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Random;

/**
 * Unicode边界字符生成器 生成包含Unicode字符集边界情况的文本
 *
 * <p>支持的参数： - type: Unicode字符类型 (CONTROL|EMOJI|COMBINING|ZERO_WIDTH|ALL，默认ALL) - length: 生成字符串的长度
 * (默认10) - frequency: 特殊字符在生成文本中的出现频率 (0-1，默认0.3) - includeNormal: 是否包含普通字符 (默认true)
 */
public class UnicodeGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Random RANDOM = new Random();

  // 控制字符
  private static final char[] CONTROL_CHARS = {
    '\u0000', '\u0001', '\u0007', '\u0008', '\u0009', '\n', '\u000B', '\f', '\r', '\u001B', '\u007F'
  };

  // 零宽字符
  private static final char[] ZERO_WIDTH_CHARS = {'\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF'};

  // 组合字符
  private static final char[] COMBINING_CHARS = {
    '\u0300', '\u0301', '\u0302', '\u0303', '\u0304', '\u0305', '\u0306', '\u0307', '\u0308',
    '\u0309', '\u030A'
  };

  // Emoji字符
  private static final String[] EMOJI_CHARS = {
    "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊",
    "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉",
    "🔥", "💯", "💥", "💫", "⭐", "🌟", "✨", "⚡", "☄️", "💥"
  };

  // 普通字符
  private static final String NORMAL_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  @Override
  public String getType() {
    return "unicode";
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
        // 生成特殊Unicode字符
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

  /** 生成特殊Unicode字符 */
  private String generateSpecialChar(String type) {
    switch (type.toUpperCase()) {
      case "CONTROL":
        return String.valueOf(CONTROL_CHARS[RANDOM.nextInt(CONTROL_CHARS.length)]);
      case "EMOJI":
        return EMOJI_CHARS[RANDOM.nextInt(EMOJI_CHARS.length)];
      case "COMBINING":
        return generateCombiningChar();
      case "ZERO_WIDTH":
        return String.valueOf(ZERO_WIDTH_CHARS[RANDOM.nextInt(ZERO_WIDTH_CHARS.length)]);
      case "ALL":
      default:
        return generateRandomSpecialChar();
    }
  }

  /** 生成组合字符 */
  private String generateCombiningChar() {
    // 基础字符 + 组合字符
    char baseChar = (char) ('A' + RANDOM.nextInt(26));
    char combiningChar = COMBINING_CHARS[RANDOM.nextInt(COMBINING_CHARS.length)];
    return String.valueOf(baseChar) + String.valueOf(combiningChar);
  }

  /** 随机生成特殊字符 */
  private String generateRandomSpecialChar() {
    int charType = RANDOM.nextInt(4);
    switch (charType) {
      case 0:
        return String.valueOf(CONTROL_CHARS[RANDOM.nextInt(CONTROL_CHARS.length)]);
      case 1:
        return EMOJI_CHARS[RANDOM.nextInt(EMOJI_CHARS.length)];
      case 2:
        return generateCombiningChar();
      case 3:
      default:
        return String.valueOf(ZERO_WIDTH_CHARS[RANDOM.nextInt(ZERO_WIDTH_CHARS.length)]);
    }
  }
}
