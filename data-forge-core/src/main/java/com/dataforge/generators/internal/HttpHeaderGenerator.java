package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP头生成器
 *
 * <p>支持的参数： - type: 头类型 (request|response|common) 默认: common - name: 指定头名称 - format: 输出格式
 * (header|json) 默认: header
 *
 * @author DataForge
 */
public class HttpHeaderGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(HttpHeaderGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 常见HTTP头
  private static final Map<String, List<String>> COMMON_HEADERS = new HashMap<>();

  static {
    COMMON_HEADERS.put(
        "User-Agent",
        Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                + " Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)"
                + " Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)"
                + " Chrome/120.0.0.0 Safari/537.36"));

    COMMON_HEADERS.put(
        "Accept",
        Arrays.asList(
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "application/json, text/plain, */*",
            "application/json"));

    COMMON_HEADERS.put(
        "Content-Type",
        Arrays.asList(
            "text/html; charset=utf-8",
            "application/json; charset=utf-8",
            "application/xml; charset=utf-8"));

    COMMON_HEADERS.put(
        "Authorization",
        Arrays.asList(
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "Basic dXNlcm5hbWU6cGFzc3dvcmQ=",
            "API-Key sk-1234567890abcdef"));
  }

  @Override
  public String getType() {
    return "http_header";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String name = getStringParam(config, "name", null);
      String format = getStringParam(config, "format", "header");

      String headerName;
      String headerValue;

      if (name != null && !name.trim().isEmpty()) {
        headerName = name.trim();
        headerValue = generateValueForHeader(headerName);
      } else {
        Map.Entry<String, List<String>> entry = getRandomHeader();
        headerName = entry.getKey();
        List<String> values = entry.getValue();
        headerValue = values.get(random.nextInt(values.size()));
      }

      return formatOutput(headerName, headerValue, format);

    } catch (Exception e) {
      logger.error("生成HTTP头时发生错误", e);
      return "Content-Type: text/plain";
    }
  }

  private Map.Entry<String, List<String>> getRandomHeader() {
    List<Map.Entry<String, List<String>>> entries = new ArrayList<>(COMMON_HEADERS.entrySet());
    return entries.get(random.nextInt(entries.size()));
  }

  private String generateValueForHeader(String headerName) {
    switch (headerName.toLowerCase()) {
      case "content-length":
        return String.valueOf(random.nextInt(65536));
      case "x-request-id":
        return UUID.randomUUID().toString();
      default:
        return "test-value-" + random.nextInt(1000);
    }
  }

  private String formatOutput(String name, String value, String format) {
    switch (format.toLowerCase()) {
      case "json":
        return String.format("{\"%s\": \"%s\"}", name, value.replace("\"", "\\\""));
      case "header":
      default:
        return String.format("%s: %s", name, value);
    }
  }
}
