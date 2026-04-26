package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cookie生成器
 *
 * <p>支持生成HTTP Cookie字符串，包括Cookie名称、值以及各种属性， 用于Web应用测试、会话管理测试、浏览器兼容性测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>format: 输出格式 (HEADER|JSON|SIMPLE) 默认: HEADER
 *   <li>name: Cookie名称（如果不指定则随机生成）
 *   <li>value: Cookie值（如果不指定则随机生成）
 *   <li>domain: 域名
 *   <li>path: 路径 默认: /
 *   <li>expires_days: 过期天数（从现在开始）
 *   <li>max_age: 最大存活时间（秒）
 *   <li>secure: 是否设置Secure标志 默认: false
 *   <li>http_only: 是否设置HttpOnly标志 默认: false
 *   <li>same_site: SameSite属性 (STRICT|LAX|NONE) 默认: LAX
 *   <li>session: 是否为会话Cookie 默认: false
 *   <li>value_type: 值类型 (STRING|UUID|TOKEN|JSON) 默认: STRING
 *   <li>value_length: 值长度（仅对STRING类型有效）默认: 32
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class CookieGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(CookieGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 输出格式枚举
  public enum OutputFormat {
    HEADER("HTTP头格式"),
    JSON("JSON格式"),
    SIMPLE("简单格式");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // SameSite属性枚举
  public enum SameSite {
    STRICT,
    LAX,
    NONE
  }

  // 值类型枚举
  public enum ValueType {
    STRING("随机字符串"),
    UUID("UUID格式"),
    TOKEN("令牌格式"),
    JSON("JSON格式");

    private final String description;

    ValueType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 常见Cookie名称
  private static final List<String> COMMON_COOKIE_NAMES =
      Arrays.asList(
          "sessionid",
          "JSESSIONID",
          "PHPSESSID",
          "ASP.NET_SessionId",
          "auth_token",
          "access_token",
          "refresh_token",
          "csrf_token",
          "user_id",
          "username",
          "remember_me",
          "language",
          "theme",
          "cart_id",
          "visitor_id",
          "tracking_id",
          "analytics_id");

  // 常见域名
  private static final List<String> COMMON_DOMAINS =
      Arrays.asList(
          "example.com",
          "test.com",
          "localhost",
          "127.0.0.1",
          "app.example.com",
          "api.example.com",
          "www.example.com");

  // Cookie信息类
  public static class CookieInfo {
    private final String name;
    private final String value;
    private final String domain;
    private final String path;
    private final LocalDateTime expires;
    private final Integer maxAge;
    private final boolean secure;
    private final boolean httpOnly;
    private final SameSite sameSite;

    public CookieInfo(
        String name,
        String value,
        String domain,
        String path,
        LocalDateTime expires,
        Integer maxAge,
        boolean secure,
        boolean httpOnly,
        SameSite sameSite) {
      this.name = name;
      this.value = value;
      this.domain = domain;
      this.path = path;
      this.expires = expires;
      this.maxAge = maxAge;
      this.secure = secure;
      this.httpOnly = httpOnly;
      this.sameSite = sameSite;
    }

    // Getters
    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public String getDomain() {
      return domain;
    }

    public String getPath() {
      return path;
    }

    public LocalDateTime getExpires() {
      return expires;
    }

    public Integer getMaxAge() {
      return maxAge;
    }

    public boolean isSecure() {
      return secure;
    }

    public boolean isHttpOnly() {
      return httpOnly;
    }

    public SameSite getSameSite() {
      return sameSite;
    }
  }

  @Override
  public String getType() {
    return "cookie";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取输出格式
      String formatStr = getStringParam(config, "format", "HEADER");
      OutputFormat format = parseOutputFormat(formatStr);

      // 生成Cookie信息
      CookieInfo cookieInfo = generateCookieInfo(config);

      // 格式化输出
      return formatCookie(cookieInfo, format);

    } catch (Exception e) {
      logger.error("Failed to generate cookie", e);
      // 返回一个默认Cookie作为fallback
      return "sessionid=abc123; Path=/";
    }
  }

  /** 解析输出格式 */
  private OutputFormat parseOutputFormat(String formatStr) {
    try {
      return OutputFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output format: {}, using HEADER as default", formatStr);
      return OutputFormat.HEADER;
    }
  }

  /** 生成Cookie信息 */
  private CookieInfo generateCookieInfo(FieldConfig config) {
    // 生成Cookie名称
    String name = getStringParam(config, "name", null);
    if (name == null) {
      name = COMMON_COOKIE_NAMES.get(random.nextInt(COMMON_COOKIE_NAMES.size()));
    }

    // 生成Cookie值
    String value = getStringParam(config, "value", null);
    if (value == null) {
      String valueTypeStr = getStringParam(config, "value_type", "STRING");
      ValueType valueType = parseValueType(valueTypeStr);
      value = generateCookieValue(valueType, config);
    }

    // 获取域名
    String domain = getStringParam(config, "domain", null);
    if (domain == null) {
      domain = COMMON_DOMAINS.get(random.nextInt(COMMON_DOMAINS.size()));
    }

    // 获取路径
    String path = getStringParam(config, "path", "/");

    // 计算过期时间
    LocalDateTime expires = null;
    Integer maxAge = null;
    boolean isSession = getBooleanParam(config, "session", false);

    if (!isSession) {
      String expiresDaysStr = getStringParam(config, "expires_days", null);
      String maxAgeStr = getStringParam(config, "max_age", null);

      if (expiresDaysStr != null) {
        int expiresDays = Integer.parseInt(expiresDaysStr);
        expires = LocalDateTime.now().plusDays(expiresDays);
      } else if (maxAgeStr != null) {
        maxAge = Integer.parseInt(maxAgeStr);
      } else {
        // 默认7天后过期
        expires = LocalDateTime.now().plusDays(7);
      }
    }

    // 获取安全属性
    boolean secure = getBooleanParam(config, "secure", false);
    boolean httpOnly = getBooleanParam(config, "http_only", false);

    // 获取SameSite属性
    String sameSiteStr = getStringParam(config, "same_site", "LAX");
    SameSite sameSite = parseSameSite(sameSiteStr);

    return new CookieInfo(name, value, domain, path, expires, maxAge, secure, httpOnly, sameSite);
  }

  /** 解析值类型 */
  private ValueType parseValueType(String valueTypeStr) {
    try {
      return ValueType.valueOf(valueTypeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid value type: {}, using STRING as default", valueTypeStr);
      return ValueType.STRING;
    }
  }

  /** 解析SameSite属性 */
  private SameSite parseSameSite(String sameSiteStr) {
    try {
      return SameSite.valueOf(sameSiteStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid SameSite value: {}, using LAX as default", sameSiteStr);
      return SameSite.LAX;
    }
  }

  /** 生成Cookie值 */
  private String generateCookieValue(ValueType valueType, FieldConfig config) {
    switch (valueType) {
      case UUID:
        return UUID.randomUUID().toString();
      case TOKEN:
        return generateToken();
      case JSON:
        return generateJsonValue();
      case STRING:
      default:
        int length = getIntParam(config, "value_length", 32);
        return generateRandomString(length);
    }
  }

  /** 生成随机字符串 */
  private String generateRandomString(int length) {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }

    return sb.toString();
  }

  /** 生成令牌格式的值 */
  private String generateToken() {
    // 生成类似JWT的令牌格式
    String header =
        Base64.getEncoder().encodeToString("{\"typ\":\"JWT\",\"alg\":\"HS256\"}".getBytes());
    String payload =
        Base64.getEncoder()
            .encodeToString(
                String.format(
                        "{\"sub\":\"user%d\",\"iat\":%d}",
                        random.nextInt(10000), System.currentTimeMillis() / 1000)
                    .getBytes());
    String signature = generateRandomString(43);

    return header + "." + payload + "." + signature;
  }

  /** 生成JSON格式的值 */
  private String generateJsonValue() {
    Map<String, Object> jsonData = new HashMap<>();
    jsonData.put("userId", random.nextInt(10000));
    jsonData.put("sessionId", generateRandomString(16));
    jsonData.put("timestamp", System.currentTimeMillis());
    jsonData.put("preferences", Map.of("theme", "dark", "language", "en"));

    // 简单的JSON序列化
    StringBuilder json = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, Object> entry : jsonData.entrySet()) {
      if (!first) json.append(",");
      json.append("\"").append(entry.getKey()).append("\":");

      Object value = entry.getValue();
      if (value instanceof String) {
        json.append("\"").append(value).append("\"");
      } else if (value instanceof Map) {
        json.append("{\"theme\":\"dark\",\"language\":\"en\"}");
      } else {
        json.append(value);
      }
      first = false;
    }
    json.append("}");

    return Base64.getEncoder().encodeToString(json.toString().getBytes());
  }

  /** 格式化Cookie */
  private String formatCookie(CookieInfo cookieInfo, OutputFormat format) {
    switch (format) {
      case HEADER:
        return formatAsHeader(cookieInfo);
      case JSON:
        return formatAsJson(cookieInfo);
      case SIMPLE:
        return formatAsSimple(cookieInfo);
      default:
        return formatAsHeader(cookieInfo);
    }
  }

  /** 格式化为HTTP头格式 */
  private String formatAsHeader(CookieInfo cookieInfo) {
    StringBuilder cookie = new StringBuilder();
    cookie.append(cookieInfo.getName()).append("=").append(cookieInfo.getValue());

    if (cookieInfo.getDomain() != null) {
      cookie.append("; Domain=").append(cookieInfo.getDomain());
    }

    if (cookieInfo.getPath() != null) {
      cookie.append("; Path=").append(cookieInfo.getPath());
    }

    if (cookieInfo.getExpires() != null) {
      String expiresStr =
          cookieInfo
              .getExpires()
              .atZone(ZoneOffset.UTC)
              .format(DateTimeFormatter.RFC_1123_DATE_TIME);
      cookie.append("; Expires=").append(expiresStr);
    }

    if (cookieInfo.getMaxAge() != null) {
      cookie.append("; Max-Age=").append(cookieInfo.getMaxAge());
    }

    if (cookieInfo.isSecure()) {
      cookie.append("; Secure");
    }

    if (cookieInfo.isHttpOnly()) {
      cookie.append("; HttpOnly");
    }

    if (cookieInfo.getSameSite() != null) {
      cookie.append("; SameSite=").append(cookieInfo.getSameSite().name());
    }

    return cookie.toString();
  }

  /** 格式化为JSON格式 */
  private String formatAsJson(CookieInfo cookieInfo) {
    StringBuilder json = new StringBuilder("{");
    json.append("\"name\":\"").append(cookieInfo.getName()).append("\",");
    json.append("\"value\":\"").append(cookieInfo.getValue()).append("\"");

    if (cookieInfo.getDomain() != null) {
      json.append(",\"domain\":\"").append(cookieInfo.getDomain()).append("\"");
    }

    if (cookieInfo.getPath() != null) {
      json.append(",\"path\":\"").append(cookieInfo.getPath()).append("\"");
    }

    if (cookieInfo.getExpires() != null) {
      json.append(",\"expires\":\"").append(cookieInfo.getExpires().toString()).append("\"");
    }

    if (cookieInfo.getMaxAge() != null) {
      json.append(",\"maxAge\":").append(cookieInfo.getMaxAge());
    }

    json.append(",\"secure\":").append(cookieInfo.isSecure());
    json.append(",\"httpOnly\":").append(cookieInfo.isHttpOnly());

    if (cookieInfo.getSameSite() != null) {
      json.append(",\"sameSite\":\"").append(cookieInfo.getSameSite().name()).append("\"");
    }

    json.append("}");
    return json.toString();
  }

  /** 格式化为简单格式 */
  private String formatAsSimple(CookieInfo cookieInfo) {
    return cookieInfo.getName() + "=" + cookieInfo.getValue();
  }
}
