package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 验证码生成器
 *
 * <p>支持的参数： - type: 验证码类型 (EMAIL|SMS|CAPTCHA|NUMERIC|ALPHANUMERIC) - length: 验证码长度 (默认6) - chars:
 * 字符集 (NUMERIC|ALPHANUMERIC|ALPHANUMERIC_UPPER|ALPHANUMERIC_LOWER|CUSTOM) - custom_chars: 自定义字符集 -
 * exclude_ambiguous: 是否排除易混淆字符 (true|false) - case_sensitive: 是否区分大小写 (true|false) -
 * expiry_minutes: 有效期分钟数
 *
 * @author DataForge
 */
public class VerificationCodeGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(VerificationCodeGenerator.class);
  private static final Random random = new Random();
  private static final SecureRandom secureRandom = new SecureRandom();

  // 验证码类型枚举
  private enum CodeType {
    EMAIL, // 邮箱验证码
    SMS, // 短信验证码
    CAPTCHA, // 图形验证码
    NUMERIC, // 纯数字验证码
    ALPHANUMERIC // 字母数字验证码
  }

  // 字符集定义
  private static final String NUMERIC_CHARS = "0123456789";
  private static final String ALPHA_LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz";
  private static final String ALPHA_UPPER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String ALPHANUMERIC_CHARS =
      NUMERIC_CHARS + ALPHA_LOWER_CHARS + ALPHA_UPPER_CHARS;

  // 易混淆字符（排除0O、1lI等）
  private static final String AMBIGUOUS_CHARS = "0O1lI";
  private static final String CLEAR_NUMERIC_CHARS = "23456789";
  private static final String CLEAR_ALPHA_CHARS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
  private static final String CLEAR_ALPHANUMERIC_CHARS = CLEAR_NUMERIC_CHARS + CLEAR_ALPHA_CHARS;

  // 验证码模板
  private static final Map<CodeType, CodeTemplate> CODE_TEMPLATES = new HashMap<>();

  static {
    initializeCodeTemplates();
  }

  private static void initializeCodeTemplates() {
    CODE_TEMPLATES.put(CodeType.EMAIL, new CodeTemplate(6, NUMERIC_CHARS, true));
    CODE_TEMPLATES.put(CodeType.SMS, new CodeTemplate(6, NUMERIC_CHARS, true));
    CODE_TEMPLATES.put(CodeType.CAPTCHA, new CodeTemplate(4, CLEAR_ALPHANUMERIC_CHARS, false));
    CODE_TEMPLATES.put(CodeType.NUMERIC, new CodeTemplate(6, NUMERIC_CHARS, true));
    CODE_TEMPLATES.put(CodeType.ALPHANUMERIC, new CodeTemplate(6, ALPHANUMERIC_CHARS, false));
  }

  // 验证码模板类
  private static class CodeTemplate {
    final int defaultLength;
    final String defaultChars;
    final boolean excludeAmbiguous;

    CodeTemplate(int defaultLength, String defaultChars, boolean excludeAmbiguous) {
      this.defaultLength = defaultLength;
      this.defaultChars = defaultChars;
      this.excludeAmbiguous = excludeAmbiguous;
    }
  }

  @Override
  public String getType() {
    return "verification_code";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String type = config.getParam("type", String.class, "SMS");
      int length = Integer.parseInt(config.getParam("length", String.class, "6"));
      String chars = config.getParam("chars", String.class, "AUTO");
      String customChars = config.getParam("custom_chars", String.class, null);
      boolean excludeAmbiguous =
          Boolean.parseBoolean(config.getParam("exclude_ambiguous", String.class, "true"));
      boolean caseSensitive =
          Boolean.parseBoolean(config.getParam("case_sensitive", String.class, "false"));
      int expiryMinutes = Integer.parseInt(config.getParam("expiry_minutes", String.class, "5"));

      // 生成验证码
      String verificationCode =
          generateVerificationCode(
              type, length, chars, customChars, excludeAmbiguous, caseSensitive);

      // 计算过期时间
      long expiryTime = System.currentTimeMillis() + (expiryMinutes * 60 * 1000L);

      // 将验证码信息存入上下文
      context.put("verification_code", verificationCode);
      context.put("verification_type", type);
      context.put("verification_expiry", expiryTime);
      context.put("verification_length", verificationCode.length());

      logger.debug(
          "Generated verification code: {}*** (type: {}, length: {})",
          verificationCode.substring(0, Math.min(2, verificationCode.length())),
          type,
          verificationCode.length());
      return verificationCode;

    } catch (Exception e) {
      logger.error("Error generating verification code", e);
      return "123456";
    }
  }

  private String generateVerificationCode(
      String type,
      int length,
      String chars,
      String customChars,
      boolean excludeAmbiguous,
      boolean caseSensitive) {

    // 确定验证码类型
    CodeType codeType = determineCodeType(type);
    CodeTemplate template = CODE_TEMPLATES.get(codeType);

    // 确定长度
    int finalLength = length > 0 ? length : template.defaultLength;

    // 确定字符集
    String charset =
        determineCharset(chars, customChars, template, excludeAmbiguous, caseSensitive);

    // 生成验证码
    StringBuilder code = new StringBuilder();
    for (int i = 0; i < finalLength; i++) {
      int index = secureRandom.nextInt(charset.length());
      code.append(charset.charAt(index));
    }

    String result = code.toString();

    // 应用大小写规则
    if (!caseSensitive && containsAlpha(result)) {
      result = applyCase(result, codeType);
    }

    return result;
  }

  private CodeType determineCodeType(String type) {
    try {
      return CodeType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Unknown verification code type: {}. Using SMS.", type);
      return CodeType.SMS;
    }
  }

  private String determineCharset(
      String chars,
      String customChars,
      CodeTemplate template,
      boolean excludeAmbiguous,
      boolean caseSensitive) {

    String charset;

    if (customChars != null && !customChars.isEmpty()) {
      charset = customChars;
    } else if ("AUTO".equals(chars)) {
      charset = template.defaultChars;
      excludeAmbiguous = template.excludeAmbiguous;
    } else {
      switch (chars.toUpperCase()) {
        case "NUMERIC":
          charset = NUMERIC_CHARS;
          break;
        case "ALPHANUMERIC":
          charset = ALPHANUMERIC_CHARS;
          break;
        case "ALPHANUMERIC_UPPER":
          charset = NUMERIC_CHARS + ALPHA_UPPER_CHARS;
          break;
        case "ALPHANUMERIC_LOWER":
          charset = NUMERIC_CHARS + ALPHA_LOWER_CHARS;
          break;
        default:
          charset = template.defaultChars;
          break;
      }
    }

    // 排除易混淆字符
    if (excludeAmbiguous) {
      charset = removeAmbiguousChars(charset);
    }

    return charset;
  }

  private String removeAmbiguousChars(String charset) {
    StringBuilder result = new StringBuilder();
    for (char c : charset.toCharArray()) {
      if (AMBIGUOUS_CHARS.indexOf(c) == -1) {
        result.append(c);
      }
    }
    return result.toString();
  }

  private boolean containsAlpha(String text) {
    for (char c : text.toCharArray()) {
      if (Character.isLetter(c)) {
        return true;
      }
    }
    return false;
  }

  private String applyCase(String code, CodeType codeType) {
    switch (codeType) {
      case EMAIL:
      case SMS:
        // 邮箱和短信验证码通常使用大写
        return code.toUpperCase();

      case CAPTCHA:
        // 图形验证码保持混合大小写
        return code;

      case NUMERIC:
        // 数字验证码不涉及大小写
        return code;

      case ALPHANUMERIC:
      default:
        // 字母数字验证码使用大写
        return code.toUpperCase();
    }
  }

  /** 验证验证码是否有效 */
  public boolean validateCode(String inputCode, String expectedCode, boolean caseSensitive) {
    if (inputCode == null || expectedCode == null) {
      return false;
    }

    if (caseSensitive) {
      return inputCode.equals(expectedCode);
    } else {
      return inputCode.equalsIgnoreCase(expectedCode);
    }
  }

  /** 检查验证码是否过期 */
  public boolean isExpired(long expiryTime) {
    return System.currentTimeMillis() > expiryTime;
  }

  /** 生成带格式的验证码（如：123-456） */
  public String generateFormattedCode(String code, String separator, int groupSize) {
    if (separator == null || groupSize <= 0 || code.length() <= groupSize) {
      return code;
    }

    StringBuilder formatted = new StringBuilder();
    for (int i = 0; i < code.length(); i += groupSize) {
      if (i > 0) {
        formatted.append(separator);
      }

      int endIndex = Math.min(i + groupSize, code.length());
      formatted.append(code.substring(i, endIndex));
    }

    return formatted.toString();
  }

  /** 生成数学验证码（如：3+5=?） */
  public String generateMathCode() {
    int a = random.nextInt(10);
    int b = random.nextInt(10);
    int operator = random.nextInt(2); // 0: +, 1: -

    if (operator == 0) {
      return String.format("%d+%d=%d", a, b, a + b);
    } else {
      // 确保结果为正数
      if (a < b) {
        int temp = a;
        a = b;
        b = temp;
      }
      return String.format("%d-%d=%d", a, b, a - b);
    }
  }

  /** 生成中文验证码 */
  public String generateChineseCode(int length) {
    String[] chineseNumbers = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    StringBuilder code = new StringBuilder();

    for (int i = 0; i < length; i++) {
      code.append(chineseNumbers[random.nextInt(chineseNumbers.length)]);
    }

    return code.toString();
  }
}
