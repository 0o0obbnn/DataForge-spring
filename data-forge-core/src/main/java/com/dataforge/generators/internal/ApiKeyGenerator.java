package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API密钥生成器
 *
 * <p>支持的参数： - type: 密钥类型 (JWT|BEARER|BASIC|CUSTOM) - length: 密钥长度 (默认32) - format: 输出格式
 * (BASE64|HEX|ALPHANUMERIC) - prefix: 密钥前缀 (如 "sk_", "pk_") - include_checksum: 是否包含校验和
 * (true|false) - secure: 是否使用安全随机数生成器 (true|false)
 *
 * @author DataForge
 */
public class ApiKeyGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(ApiKeyGenerator.class);
  private static final Random random = new Random();
  private static final SecureRandom secureRandom = new SecureRandom();

  // 常见API密钥前缀
  private static final Map<String, List<String>> KEY_PREFIXES = new HashMap<>();

  // Base64字符集
  private static final String BASE64_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

  // 十六进制字符集
  private static final String HEX_CHARS = "0123456789abcdef";

  // 字母数字字符集
  private static final String ALPHANUMERIC_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  static {
    initializeKeyPrefixes();
  }

  private static void initializeKeyPrefixes() {
    // 不同服务的API密钥前缀
    KEY_PREFIXES.put("STRIPE", Arrays.asList("sk_", "pk_", "rk_"));
    KEY_PREFIXES.put("GITHUB", Arrays.asList("ghp_", "gho_", "ghu_", "ghs_", "ghr_"));
    KEY_PREFIXES.put("OPENAI", Arrays.asList("sk-"));
    KEY_PREFIXES.put("AWS", Arrays.asList("AKIA", "ASIA"));
    KEY_PREFIXES.put("GOOGLE", Arrays.asList("AIza"));
    KEY_PREFIXES.put("SLACK", Arrays.asList("xoxb-", "xoxp-", "xoxa-"));
    KEY_PREFIXES.put("DISCORD", Arrays.asList("Bot ", "Bearer "));
    KEY_PREFIXES.put("TWITTER", Arrays.asList(""));
    KEY_PREFIXES.put("GENERIC", Arrays.asList("api_", "key_", "token_", ""));
  }

  @Override
  public String getType() {
    return "apikey";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String type = config.getParam("type", String.class, "CUSTOM");
      int length = Integer.parseInt(config.getParam("length", String.class, "32"));
      String format = config.getParam("format", String.class, "ALPHANUMERIC");
      String prefix = config.getParam("prefix", String.class, null);
      boolean includeChecksum =
          Boolean.parseBoolean(config.getParam("include_checksum", String.class, "false"));
      boolean secure = Boolean.parseBoolean(config.getParam("secure", String.class, "true"));

      // 生成API密钥
      String apiKey = generateApiKey(type, length, format, prefix, includeChecksum, secure);

      // 将API密钥信息存入上下文
      context.put("api_key", apiKey);
      context.put("api_key_type", type);
      context.put("api_key_format", format);

      logger.debug("Generated API key: {}***", apiKey.substring(0, Math.min(8, apiKey.length())));
      return apiKey;

    } catch (Exception e) {
      logger.error("Error generating API key", e);
      return "sk_test_" + generateRandomString(32, ALPHANUMERIC_CHARS, false);
    }
  }

  private String generateApiKey(
      String type,
      int length,
      String format,
      String prefix,
      boolean includeChecksum,
      boolean secure) {

    StringBuilder apiKey = new StringBuilder();

    // 1. 添加前缀
    String keyPrefix = determinePrefix(type, prefix);
    if (keyPrefix != null && !keyPrefix.isEmpty()) {
      apiKey.append(keyPrefix);
    }

    // 2. 生成主体部分
    int mainLength = length - apiKey.length();
    if (includeChecksum) {
      mainLength -= 4; // 为校验和预留4个字符
    }

    if (mainLength > 0) {
      String mainPart = generateMainPart(mainLength, format, secure);
      apiKey.append(mainPart);
    }

    // 3. 添加校验和（如果需要）
    if (includeChecksum) {
      String checksum = generateChecksum(apiKey.toString());
      apiKey.append(checksum);
    }

    // 4. 根据类型进行特殊处理
    return processApiKeyByType(apiKey.toString(), type);
  }

  private String determinePrefix(String type, String customPrefix) {
    if (customPrefix != null) {
      return customPrefix;
    }

    switch (type.toUpperCase()) {
      case "JWT":
        return ""; // JWT通常没有前缀

      case "BEARER":
        return "Bearer ";

      case "BASIC":
        return "Basic ";

      case "CUSTOM":
      default:
        // 随机选择一个通用前缀
        List<String> genericPrefixes = KEY_PREFIXES.get("GENERIC");
        return genericPrefixes.get(random.nextInt(genericPrefixes.size()));
    }
  }

  private String generateMainPart(int length, String format, boolean secure) {
    switch (format.toUpperCase()) {
      case "BASE64":
        return generateRandomString(length, BASE64_CHARS, secure);

      case "HEX":
        return generateRandomString(length, HEX_CHARS, secure);

      case "ALPHANUMERIC":
      default:
        return generateRandomString(length, ALPHANUMERIC_CHARS, secure);
    }
  }

  private String generateRandomString(int length, String charset, boolean secure) {
    StringBuilder result = new StringBuilder();
    Random rng = secure ? secureRandom : random;

    for (int i = 0; i < length; i++) {
      result.append(charset.charAt(rng.nextInt(charset.length())));
    }

    return result.toString();
  }

  private String generateChecksum(String input) {
    // 简单的校验和算法
    int sum = 0;
    for (char c : input.toCharArray()) {
      sum += c;
    }

    return String.format("%04x", sum % 0x10000);
  }

  private String processApiKeyByType(String apiKey, String type) {
    switch (type.toUpperCase()) {
      case "JWT":
        return generateJwtLikeToken(apiKey);

      case "BEARER":
      case "BASIC":
        return apiKey; // 已经包含了前缀

      case "CUSTOM":
      default:
        return apiKey;
    }
  }

  private String generateJwtLikeToken(String payload) {
    // 生成类似JWT的三段式token
    String header = generateJwtSegment(20);
    String body = generateJwtSegment(Math.max(40, payload.length()));
    String signature = generateJwtSegment(16);

    return header + "." + body + "." + signature;
  }

  private String generateJwtSegment(int length) {
    // JWT使用URL安全的Base64编码
    String urlSafeBase64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    return generateRandomString(length, urlSafeBase64Chars, true);
  }

  /** 生成特定服务风格的API密钥 */
  public String generateServiceApiKey(String service, int length) {
    List<String> prefixes = KEY_PREFIXES.get(service.toUpperCase());
    if (prefixes == null || prefixes.isEmpty()) {
      prefixes = KEY_PREFIXES.get("GENERIC");
    }

    String prefix = prefixes.get(random.nextInt(prefixes.size()));
    String mainPart = generateRandomString(length - prefix.length(), ALPHANUMERIC_CHARS, true);

    return prefix + mainPart;
  }

  /** 验证API密钥格式 */
  public boolean validateApiKeyFormat(String apiKey, String expectedType) {
    if (apiKey == null || apiKey.isEmpty()) {
      return false;
    }

    switch (expectedType.toUpperCase()) {
      case "JWT":
        return apiKey.split("\\.").length == 3;

      case "BEARER":
        return apiKey.startsWith("Bearer ");

      case "BASIC":
        return apiKey.startsWith("Basic ");

      case "CUSTOM":
      default:
        return apiKey.length() >= 16; // 最小长度要求
    }
  }

  /** 生成API密钥对（公钥/私钥） */
  public Map<String, String> generateApiKeyPair() {
    Map<String, String> keyPair = new HashMap<>();

    String publicKey = "pk_" + generateRandomString(32, ALPHANUMERIC_CHARS, true);
    String secretKey = "sk_" + generateRandomString(32, ALPHANUMERIC_CHARS, true);

    keyPair.put("public_key", publicKey);
    keyPair.put("secret_key", secretKey);

    return keyPair;
  }

  /** 生成带有过期时间的临时token */
  public String generateTemporaryToken(int validityMinutes) {
    long expirationTime = System.currentTimeMillis() + (validityMinutes * 60 * 1000L);
    String token = generateRandomString(24, ALPHANUMERIC_CHARS, true);

    // 将过期时间编码到token中（简化版本）
    String expiration = Long.toHexString(expirationTime);

    return "tmp_" + token + "_" + expiration;
  }
}
