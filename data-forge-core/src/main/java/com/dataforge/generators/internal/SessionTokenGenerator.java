package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;

/**
 * 会话令牌生成器
 *
 * <p>支持的参数： - type: 令牌类型 (RANDOM_STRING|JWT|OAUTH|CSRF)，默认 RANDOM_STRING - length: 令牌长度，默认32 -
 * encoding: 编码方式 (BASE64|HEX|ALPHANUMERIC)，默认BASE64 - prefix: 令牌前缀，默认无
 *
 * @author DataForge
 * @since 1.0.0
 */
public class SessionTokenGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private final Random secureRandom = new SecureRandom();

  @Override
  public String getType() {
    return "session_token";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    String type = getStringParam(config, "type", "RANDOM_STRING");
    int length = getIntParam(config, "length", 32);
    String encoding = getStringParam(config, "encoding", "BASE64");
    String prefix = getStringParam(config, "prefix", "");

    String token;
    switch (type.toUpperCase()) {
      case "JWT":
        token = generateJwtToken();
        break;
      case "OAUTH":
        token = generateOAuthToken(length, encoding);
        break;
      case "CSRF":
        token = generateCsrfToken(length, encoding);
        break;
      default:
        token = generateRandomString(length, encoding);
        break;
    }

    // 添加前缀
    if (!prefix.isEmpty()) {
      token = prefix + token;
    }

    // 存储到上下文中供其他字段使用
    context.put("session_token", token);
    context.put("token_type", type);

    return token;
  }

  /** 生成JWT令牌（简化版） */
  private String generateJwtToken() {
    String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    String payload = "{\"sub\":\"user123\",\"iat\":" + Instant.now().getEpochSecond() + "}";

    String encodedHeader =
        Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes());
    String encodedPayload =
        Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());

    // 生成模拟签名
    byte[] signature = new byte[32];
    secureRandom.nextBytes(signature);
    String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

    return encodedHeader + "." + encodedPayload + "." + encodedSignature;
  }

  /** 生成OAuth访问令牌 */
  private String generateOAuthToken(int length, String encoding) {
    return generateRandomString(Math.max(length, 40), encoding);
  }

  /** 生成CSRF令牌 */
  private String generateCsrfToken(int length, String encoding) {
    return generateRandomString(length, "HEX");
  }

  /** 生成随机字符串 */
  private String generateRandomString(int length, String encoding) {
    switch (encoding.toUpperCase()) {
      case "BASE64":
        return generateBase64String(length);
      case "HEX":
        return generateHexString(length);
      case "ALPHANUMERIC":
        return generateAlphanumericString(length);
      default:
        return generateBase64String(length);
    }
  }

  /** 生成Base64编码字符串 */
  private String generateBase64String(int length) {
    int byteLength = (length * 3 + 3) / 4;
    byte[] bytes = new byte[byteLength];
    secureRandom.nextBytes(bytes);

    String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return encoded.length() > length ? encoded.substring(0, length) : encoded;
  }

  /** 生成十六进制字符串 */
  private String generateHexString(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(Integer.toHexString(secureRandom.nextInt(16)));
    }
    return sb.toString();
  }

  /** 生成字母数字字符串 */
  private String generateAlphanumericString(int length) {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
    }
    return sb.toString();
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }
}
