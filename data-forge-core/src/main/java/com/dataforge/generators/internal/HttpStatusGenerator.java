package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP状态码生成器
 *
 * <p>支持生成标准HTTP状态码，用于API测试、Web服务测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>category: 状态码类别 (1XX|2XX|3XX|4XX|5XX|ALL) 默认: ALL
 *   <li>format: 输出格式 (CODE|MESSAGE|BOTH) 默认: CODE
 *   <li>common_only: 是否只生成常见状态码 默认: true
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class HttpStatusGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(HttpStatusGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // HTTP状态码映射
  private static final Map<Integer, String> STATUS_CODES = new HashMap<>();

  static {
    // 1xx 信息性状态码
    STATUS_CODES.put(100, "Continue");
    STATUS_CODES.put(101, "Switching Protocols");

    // 2xx 成功状态码
    STATUS_CODES.put(200, "OK");
    STATUS_CODES.put(201, "Created");
    STATUS_CODES.put(202, "Accepted");
    STATUS_CODES.put(204, "No Content");
    STATUS_CODES.put(206, "Partial Content");

    // 3xx 重定向状态码
    STATUS_CODES.put(300, "Multiple Choices");
    STATUS_CODES.put(301, "Moved Permanently");
    STATUS_CODES.put(302, "Found");
    STATUS_CODES.put(304, "Not Modified");
    STATUS_CODES.put(307, "Temporary Redirect");
    STATUS_CODES.put(308, "Permanent Redirect");

    // 4xx 客户端错误状态码
    STATUS_CODES.put(400, "Bad Request");
    STATUS_CODES.put(401, "Unauthorized");
    STATUS_CODES.put(403, "Forbidden");
    STATUS_CODES.put(404, "Not Found");
    STATUS_CODES.put(405, "Method Not Allowed");
    STATUS_CODES.put(409, "Conflict");
    STATUS_CODES.put(410, "Gone");
    STATUS_CODES.put(422, "Unprocessable Entity");
    STATUS_CODES.put(429, "Too Many Requests");

    // 5xx 服务器错误状态码
    STATUS_CODES.put(500, "Internal Server Error");
    STATUS_CODES.put(501, "Not Implemented");
    STATUS_CODES.put(502, "Bad Gateway");
    STATUS_CODES.put(503, "Service Unavailable");
    STATUS_CODES.put(504, "Gateway Timeout");
  }

  // 常见状态码
  private static final int[] COMMON_CODES = {200, 201, 400, 401, 403, 404, 500, 502, 503};

  @Override
  public String getType() {
    return "http_status";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String category = getStringParam(config, "category", "ALL");
      String format = getStringParam(config, "format", "CODE");
      boolean commonOnly = getBooleanParam(config, "common_only", true);

      int statusCode = generateStatusCode(category, commonOnly);
      return formatOutput(statusCode, format);

    } catch (Exception e) {
      logger.error("Failed to generate HTTP status code", e);
      return "200";
    }
  }

  private int generateStatusCode(String category, boolean commonOnly) {
    if (commonOnly) {
      return COMMON_CODES[random.nextInt(COMMON_CODES.length)];
    }

    Integer[] codes =
        STATUS_CODES.keySet().stream()
            .filter(code -> matchesCategory(code, category))
            .toArray(Integer[]::new);

    if (codes.length == 0) {
      return 200; // fallback
    }

    return codes[random.nextInt(codes.length)];
  }

  private boolean matchesCategory(int code, String category) {
    switch (category.toUpperCase()) {
      case "1XX":
        return code >= 100 && code < 200;
      case "2XX":
        return code >= 200 && code < 300;
      case "3XX":
        return code >= 300 && code < 400;
      case "4XX":
        return code >= 400 && code < 500;
      case "5XX":
        return code >= 500 && code < 600;
      case "ALL":
        return true;
      default:
        return true;
    }
  }

  private String formatOutput(int statusCode, String format) {
    String message = STATUS_CODES.get(statusCode);

    switch (format.toUpperCase()) {
      case "CODE":
        return String.valueOf(statusCode);
      case "MESSAGE":
        return message != null ? message : "Unknown";
      case "BOTH":
        return statusCode + " " + (message != null ? message : "Unknown");
      default:
        return String.valueOf(statusCode);
    }
  }
}
