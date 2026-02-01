package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket连接生成器
 *
 * <p>支持生成WebSocket连接相关的配置信息，包括WebSocket URL、 连接参数、协议版本、扩展配置等，用于WebSocket应用测试、 实时通信功能验证、性能测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>format: 输出格式 (URL|JSON|CONFIG) 默认: URL
 *   <li>protocol: 协议类型 (WS|WSS|RANDOM) 默认: WS
 *   <li>host: 主机地址（如果不指定则随机生成）
 *   <li>port: 端口号 默认: 8080
 *   <li>path: 路径 默认: /websocket
 *   <li>subprotocol: 子协议 (NONE|CHAT|ECHO|CUSTOM) 默认: NONE
 *   <li>version: WebSocket版本 默认: 13
 *   <li>include_query: 是否包含查询参数 默认: false
 *   <li>include_headers: 是否包含连接头信息 默认: false
 *   <li>timeout: 连接超时时间（秒）默认: 30
 *   <li>heartbeat: 心跳间隔（秒）默认: 60
 *   <li>compression: 是否启用压缩 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class WebSocketGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 输出格式枚举
  public enum OutputFormat {
    URL("WebSocket URL格式"),
    JSON("JSON配置格式"),
    CONFIG("配置对象格式");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 协议类型枚举
  public enum Protocol {
    WS("WebSocket协议"),
    WSS("安全WebSocket协议"),
    RANDOM("随机协议");

    private final String description;

    Protocol(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 子协议枚举
  public enum SubProtocol {
    NONE("无子协议"),
    CHAT("聊天协议"),
    ECHO("回声协议"),
    CUSTOM("自定义协议");

    private final String description;

    SubProtocol(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 常见主机地址
  private static final List<String> COMMON_HOSTS =
      Arrays.asList(
          "localhost",
          "127.0.0.1",
          "0.0.0.0",
          "ws.example.com",
          "api.example.com",
          "chat.example.com",
          "realtime.example.com",
          "socket.example.com");

  // 常见路径
  private static final List<String> COMMON_PATHS =
      Arrays.asList(
          "/websocket",
          "/ws",
          "/socket",
          "/chat",
          "/realtime",
          "/api/ws",
          "/v1/websocket",
          "/stream",
          "/events",
          "/notifications");

  // 常见查询参数
  private static final List<String> COMMON_QUERY_PARAMS =
      Arrays.asList(
          "token",
          "userId",
          "sessionId",
          "roomId",
          "channelId",
          "version",
          "format",
          "compression",
          "heartbeat");

  // WebSocket配置信息类
  public static class WebSocketConfig {
    private final String protocol;
    private final String host;
    private final int port;
    private final String path;
    private final String subprotocol;
    private final int version;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final int timeout;
    private final int heartbeat;
    private final boolean compression;

    public WebSocketConfig(
        String protocol,
        String host,
        int port,
        String path,
        String subprotocol,
        int version,
        Map<String, String> queryParams,
        Map<String, String> headers,
        int timeout,
        int heartbeat,
        boolean compression) {
      this.protocol = protocol;
      this.host = host;
      this.port = port;
      this.path = path;
      this.subprotocol = subprotocol;
      this.version = version;
      this.queryParams = queryParams != null ? queryParams : new HashMap<>();
      this.headers = headers != null ? headers : new HashMap<>();
      this.timeout = timeout;
      this.heartbeat = heartbeat;
      this.compression = compression;
    }

    // Getters
    public String getProtocol() {
      return protocol;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public String getPath() {
      return path;
    }

    public String getSubprotocol() {
      return subprotocol;
    }

    public int getVersion() {
      return version;
    }

    public Map<String, String> getQueryParams() {
      return queryParams;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    public int getTimeout() {
      return timeout;
    }

    public int getHeartbeat() {
      return heartbeat;
    }

    public boolean isCompression() {
      return compression;
    }
  }

  @Override
  public String getType() {
    return "websocket";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取输出格式
      String formatStr = getStringParam(config, "format", "URL");
      OutputFormat format = parseOutputFormat(formatStr);

      // 生成WebSocket配置
      WebSocketConfig wsConfig = generateWebSocketConfig(config);

      // 格式化输出
      return formatWebSocketConfig(wsConfig, format);

    } catch (Exception e) {
      logger.error("Failed to generate WebSocket configuration", e);
      // 返回一个默认的WebSocket URL作为fallback
      return "ws://localhost:8080/websocket";
    }
  }

  /** 解析输出格式 */
  private OutputFormat parseOutputFormat(String formatStr) {
    try {
      return OutputFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output format: {}, using URL as default", formatStr);
      return OutputFormat.URL;
    }
  }

  /** 生成WebSocket配置 */
  private WebSocketConfig generateWebSocketConfig(FieldConfig config) {
    // 获取协议
    String protocolStr = getStringParam(config, "protocol", "WS");
    Protocol protocolEnum = parseProtocol(protocolStr);
    String protocol =
        (protocolEnum == Protocol.RANDOM)
            ? (random.nextBoolean() ? "ws" : "wss")
            : protocolEnum.name().toLowerCase();

    // 获取主机地址
    String host = getStringParam(config, "host", null);
    if (host == null) {
      host = COMMON_HOSTS.get(random.nextInt(COMMON_HOSTS.size()));
    }

    // 获取端口
    int port = getIntParam(config, "port", protocol.equals("wss") ? 443 : 8080);

    // 获取路径
    String path = getStringParam(config, "path", null);
    if (path == null) {
      path = COMMON_PATHS.get(random.nextInt(COMMON_PATHS.size()));
    }

    // 获取子协议
    String subprotocolStr = getStringParam(config, "subprotocol", "NONE");
    SubProtocol subProtocolEnum = parseSubProtocol(subprotocolStr);
    String subprotocol = generateSubProtocol(subProtocolEnum);

    // 获取版本
    int version = getIntParam(config, "version", 13);

    // 生成查询参数
    Map<String, String> queryParams = null;
    boolean includeQuery = getBooleanParam(config, "include_query", false);
    if (includeQuery) {
      queryParams = generateQueryParams();
    }

    // 生成连接头信息
    Map<String, String> headers = null;
    boolean includeHeaders = getBooleanParam(config, "include_headers", false);
    if (includeHeaders) {
      headers = generateHeaders();
    }

    // 获取其他配置
    int timeout = getIntParam(config, "timeout", 30);
    int heartbeat = getIntParam(config, "heartbeat", 60);
    boolean compression = getBooleanParam(config, "compression", false);

    return new WebSocketConfig(
        protocol,
        host,
        port,
        path,
        subprotocol,
        version,
        queryParams,
        headers,
        timeout,
        heartbeat,
        compression);
  }

  /** 解析协议类型 */
  private Protocol parseProtocol(String protocolStr) {
    try {
      return Protocol.valueOf(protocolStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid protocol: {}, using WS as default", protocolStr);
      return Protocol.WS;
    }
  }

  /** 解析子协议 */
  private SubProtocol parseSubProtocol(String subprotocolStr) {
    try {
      return SubProtocol.valueOf(subprotocolStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid subprotocol: {}, using NONE as default", subprotocolStr);
      return SubProtocol.NONE;
    }
  }

  /** 生成子协议 */
  private String generateSubProtocol(SubProtocol subProtocol) {
    switch (subProtocol) {
      case CHAT:
        return "chat";
      case ECHO:
        return "echo";
      case CUSTOM:
        return "custom-protocol-v" + (1 + random.nextInt(5));
      case NONE:
      default:
        return null;
    }
  }

  /** 生成查询参数 */
  private Map<String, String> generateQueryParams() {
    Map<String, String> params = new HashMap<>();
    int paramCount = 1 + random.nextInt(4); // 1-4个参数

    List<String> availableParams = new ArrayList<>(COMMON_QUERY_PARAMS);
    Collections.shuffle(availableParams);

    for (int i = 0; i < Math.min(paramCount, availableParams.size()); i++) {
      String key = availableParams.get(i);
      String value = generateQueryParamValue(key);
      params.put(key, value);
    }

    return params;
  }

  /** 生成查询参数值 */
  private String generateQueryParamValue(String key) {
    switch (key) {
      case "token":
        return "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." + generateRandomString(32);
      case "userId":
        return String.valueOf(1000 + random.nextInt(9000));
      case "sessionId":
        return UUID.randomUUID().toString();
      case "roomId":
        return "room_" + (1 + random.nextInt(100));
      case "channelId":
        return "channel_" + generateRandomString(8);
      case "version":
        return "v" + (1 + random.nextInt(3));
      case "format":
        return random.nextBoolean() ? "json" : "binary";
      case "compression":
        return random.nextBoolean() ? "gzip" : "none";
      case "heartbeat":
        return String.valueOf(30 + random.nextInt(120));
      default:
        return generateRandomString(8);
    }
  }

  /** 生成连接头信息 */
  private Map<String, String> generateHeaders() {
    Map<String, String> headers = new HashMap<>();

    headers.put("Origin", "https://example.com");
    headers.put("User-Agent", "DataForge-WebSocket-Client/1.0");

    if (random.nextBoolean()) {
      headers.put("Authorization", "Bearer " + generateRandomString(32));
    }

    if (random.nextBoolean()) {
      headers.put("X-Client-Version", "1." + random.nextInt(10) + "." + random.nextInt(10));
    }

    return headers;
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

  /** 格式化WebSocket配置 */
  private String formatWebSocketConfig(WebSocketConfig config, OutputFormat format) {
    switch (format) {
      case URL:
        return formatAsUrl(config);
      case JSON:
        return formatAsJson(config);
      case CONFIG:
        return formatAsConfig(config);
      default:
        return formatAsUrl(config);
    }
  }

  /** 格式化为URL格式 */
  private String formatAsUrl(WebSocketConfig config) {
    StringBuilder url = new StringBuilder();
    url.append(config.getProtocol()).append("://");
    url.append(config.getHost());

    // 只有在非默认端口时才添加端口号
    boolean isDefaultPort =
        (config.getProtocol().equals("ws") && config.getPort() == 80)
            || (config.getProtocol().equals("wss") && config.getPort() == 443);
    if (!isDefaultPort) {
      url.append(":").append(config.getPort());
    }

    url.append(config.getPath());

    // 添加查询参数
    if (config.getQueryParams() != null && !config.getQueryParams().isEmpty()) {
      url.append("?");
      boolean first = true;
      for (Map.Entry<String, String> entry : config.getQueryParams().entrySet()) {
        if (!first) url.append("&");
        url.append(entry.getKey()).append("=").append(entry.getValue());
        first = false;
      }
    }

    return url.toString();
  }

  /** 格式化为JSON格式 */
  private String formatAsJson(WebSocketConfig config) {
    StringBuilder json = new StringBuilder("{");
    json.append("\"url\":\"").append(formatAsUrl(config)).append("\",");
    json.append("\"protocol\":\"").append(config.getProtocol()).append("\",");
    json.append("\"host\":\"").append(config.getHost()).append("\",");
    json.append("\"port\":").append(config.getPort()).append(",");
    json.append("\"path\":\"").append(config.getPath()).append("\",");
    json.append("\"version\":").append(config.getVersion()).append(",");
    json.append("\"timeout\":").append(config.getTimeout()).append(",");
    json.append("\"heartbeat\":").append(config.getHeartbeat()).append(",");
    json.append("\"compression\":").append(config.isCompression());

    if (config.getSubprotocol() != null) {
      json.append(",\"subprotocol\":\"").append(config.getSubprotocol()).append("\"");
    }

    if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
      json.append(",\"headers\":{");
      boolean first = true;
      for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
        if (!first) json.append(",");
        json.append("\"")
            .append(entry.getKey())
            .append("\":\"")
            .append(entry.getValue())
            .append("\"");
        first = false;
      }
      json.append("}");
    }

    json.append("}");
    return json.toString();
  }

  /** 格式化为配置格式 */
  private String formatAsConfig(WebSocketConfig config) {
    StringBuilder configStr = new StringBuilder();
    configStr.append("WebSocket Configuration:\n");
    configStr.append("  URL: ").append(formatAsUrl(config)).append("\n");
    configStr.append("  Protocol: ").append(config.getProtocol().toUpperCase()).append("\n");
    configStr.append("  Host: ").append(config.getHost()).append("\n");
    configStr.append("  Port: ").append(config.getPort()).append("\n");
    configStr.append("  Path: ").append(config.getPath()).append("\n");
    configStr.append("  Version: ").append(config.getVersion()).append("\n");
    configStr.append("  Timeout: ").append(config.getTimeout()).append("s\n");
    configStr.append("  Heartbeat: ").append(config.getHeartbeat()).append("s\n");
    configStr.append("  Compression: ").append(config.isCompression() ? "Enabled" : "Disabled");

    if (config.getSubprotocol() != null) {
      configStr.append("\n  Subprotocol: ").append(config.getSubprotocol());
    }

    return configStr.toString();
  }
}
