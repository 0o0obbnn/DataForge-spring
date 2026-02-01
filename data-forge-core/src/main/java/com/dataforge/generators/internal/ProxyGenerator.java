package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代理配置生成器
 *
 * <p>支持生成各种代理服务器配置信息，包括HTTP代理、SOCKS代理、 透明代理等，用于网络测试、爬虫开发、负载均衡测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>format: 输出格式 (URL|JSON|CONFIG|PAC) 默认: URL
 *   <li>type: 代理类型 (HTTP|HTTPS|SOCKS4|SOCKS5|TRANSPARENT|RANDOM) 默认: HTTP
 *   <li>host: 代理服务器地址（如果不指定则随机生成）
 *   <li>port: 代理服务器端口（如果不指定则根据类型生成）
 *   <li>username: 代理用户名（可选）
 *   <li>password: 代理密码（可选）
 *   <li>auth_required: 是否需要认证 默认: false
 *   <li>anonymous: 是否为匿名代理 默认: false
 *   <li>country: 代理服务器所在国家（可选）
 *   <li>city: 代理服务器所在城市（可选）
 *   <li>speed: 代理速度等级 (SLOW|MEDIUM|FAST|RANDOM) 默认: MEDIUM
 *   <li>reliability: 可靠性等级 (LOW|MEDIUM|HIGH|RANDOM) 默认: MEDIUM
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class ProxyGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(ProxyGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 输出格式枚举
  public enum OutputFormat {
    URL("代理URL格式"),
    JSON("JSON配置格式"),
    CONFIG("配置对象格式"),
    PAC("PAC脚本格式");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 代理类型枚举
  public enum ProxyType {
    HTTP("HTTP代理"),
    HTTPS("HTTPS代理"),
    SOCKS4("SOCKS4代理"),
    SOCKS5("SOCKS5代理"),
    TRANSPARENT("透明代理"),
    RANDOM("随机类型");

    private final String description;

    ProxyType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 速度等级枚举
  public enum SpeedLevel {
    SLOW("慢速"),
    MEDIUM("中速"),
    FAST("快速"),
    RANDOM("随机");

    private final String description;

    SpeedLevel(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 可靠性等级枚举
  public enum ReliabilityLevel {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高"),
    RANDOM("随机");

    private final String description;

    ReliabilityLevel(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 常见代理服务器地址
  private static final List<String> COMMON_PROXY_HOSTS =
      Arrays.asList(
          "proxy.example.com",
          "proxy1.example.com",
          "proxy2.example.com",
          "cache.example.com",
          "gateway.example.com",
          "forward.example.com",
          "squid.example.com",
          "nginx.example.com",
          "haproxy.example.com");

  // 代理端口映射
  private static final Map<ProxyType, List<Integer>> PROXY_PORTS = new HashMap<>();

  static {
    PROXY_PORTS.put(ProxyType.HTTP, Arrays.asList(8080, 3128, 8000, 8888, 9999));
    PROXY_PORTS.put(ProxyType.HTTPS, Arrays.asList(8443, 3129, 8001, 8889, 9998));
    PROXY_PORTS.put(ProxyType.SOCKS4, Arrays.asList(1080, 1081, 9050, 9051));
    PROXY_PORTS.put(ProxyType.SOCKS5, Arrays.asList(1080, 1081, 1082, 9050, 9051));
    PROXY_PORTS.put(ProxyType.TRANSPARENT, Arrays.asList(8080, 3128, 8000));
  }

  // 国家和城市映射
  private static final Map<String, List<String>> COUNTRY_CITIES = new HashMap<>();

  static {
    COUNTRY_CITIES.put(
        "US", Arrays.asList("New York", "Los Angeles", "Chicago", "Houston", "Phoenix"));
    COUNTRY_CITIES.put(
        "UK", Arrays.asList("London", "Manchester", "Birmingham", "Leeds", "Glasgow"));
    COUNTRY_CITIES.put("DE", Arrays.asList("Berlin", "Munich", "Hamburg", "Cologne", "Frankfurt"));
    COUNTRY_CITIES.put("JP", Arrays.asList("Tokyo", "Osaka", "Yokohama", "Nagoya", "Sapporo"));
    COUNTRY_CITIES.put(
        "CN", Arrays.asList("Beijing", "Shanghai", "Guangzhou", "Shenzhen", "Hangzhou"));
  }

  // 代理配置信息类
  public static class ProxyConfig {
    private final ProxyType type;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean authRequired;
    private final boolean anonymous;
    private final String country;
    private final String city;
    private final SpeedLevel speed;
    private final ReliabilityLevel reliability;

    public ProxyConfig(
        ProxyType type,
        String host,
        int port,
        String username,
        String password,
        boolean authRequired,
        boolean anonymous,
        String country,
        String city,
        SpeedLevel speed,
        ReliabilityLevel reliability) {
      this.type = type;
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
      this.authRequired = authRequired;
      this.anonymous = anonymous;
      this.country = country;
      this.city = city;
      this.speed = speed;
      this.reliability = reliability;
    }

    // Getters
    public ProxyType getType() {
      return type;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }

    public boolean isAuthRequired() {
      return authRequired;
    }

    public boolean isAnonymous() {
      return anonymous;
    }

    public String getCountry() {
      return country;
    }

    public String getCity() {
      return city;
    }

    public SpeedLevel getSpeed() {
      return speed;
    }

    public ReliabilityLevel getReliability() {
      return reliability;
    }
  }

  @Override
  public String getType() {
    return "proxy";
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

      // 生成代理配置
      ProxyConfig proxyConfig = generateProxyConfig(config);

      // 格式化输出
      return formatProxyConfig(proxyConfig, format);

    } catch (Exception e) {
      logger.error("Failed to generate proxy configuration", e);
      // 返回一个默认的代理配置作为fallback
      return "http://proxy.example.com:8080";
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

  /** 生成代理配置 */
  private ProxyConfig generateProxyConfig(FieldConfig config) {
    // 获取代理类型
    String typeStr = getStringParam(config, "type", "HTTP");
    ProxyType type = parseProxyType(typeStr);
    if (type == ProxyType.RANDOM) {
      ProxyType[] types = {ProxyType.HTTP, ProxyType.HTTPS, ProxyType.SOCKS4, ProxyType.SOCKS5};
      type = types[random.nextInt(types.length)];
    }

    // 获取主机地址
    String host = getStringParam(config, "host", null);
    if (host == null) {
      host = generateProxyHost();
    }

    // 获取端口
    int port = getIntParam(config, "port", -1);
    if (port == -1) {
      port = generateProxyPort(type);
    }

    // 获取认证信息
    boolean authRequired = getBooleanParam(config, "auth_required", false);
    String username = null;
    String password = null;

    if (authRequired) {
      username = getStringParam(config, "username", generateUsername());
      password = getStringParam(config, "password", generatePassword());
    }

    // 获取其他属性
    boolean anonymous = getBooleanParam(config, "anonymous", false);
    String country = getStringParam(config, "country", null);
    String city = getStringParam(config, "city", null);

    // 如果指定了国家但没有指定城市，随机选择该国家的城市
    if (country != null && city == null) {
      List<String> cities = COUNTRY_CITIES.get(country.toUpperCase());
      if (cities != null && !cities.isEmpty()) {
        city = cities.get(random.nextInt(cities.size()));
      }
    }

    // 如果都没有指定，随机选择国家和城市
    if (country == null && city == null) {
      String[] countries = COUNTRY_CITIES.keySet().toArray(new String[0]);
      country = countries[random.nextInt(countries.length)];
      List<String> cities = COUNTRY_CITIES.get(country);
      city = cities.get(random.nextInt(cities.size()));
    }

    // 获取速度和可靠性等级
    String speedStr = getStringParam(config, "speed", "MEDIUM");
    SpeedLevel speed = parseSpeedLevel(speedStr);
    if (speed == SpeedLevel.RANDOM) {
      SpeedLevel[] speeds = {SpeedLevel.SLOW, SpeedLevel.MEDIUM, SpeedLevel.FAST};
      speed = speeds[random.nextInt(speeds.length)];
    }

    String reliabilityStr = getStringParam(config, "reliability", "MEDIUM");
    ReliabilityLevel reliability = parseReliabilityLevel(reliabilityStr);
    if (reliability == ReliabilityLevel.RANDOM) {
      ReliabilityLevel[] reliabilities = {
        ReliabilityLevel.LOW, ReliabilityLevel.MEDIUM, ReliabilityLevel.HIGH
      };
      reliability = reliabilities[random.nextInt(reliabilities.length)];
    }

    return new ProxyConfig(
        type,
        host,
        port,
        username,
        password,
        authRequired,
        anonymous,
        country,
        city,
        speed,
        reliability);
  }

  /** 解析代理类型 */
  private ProxyType parseProxyType(String typeStr) {
    try {
      return ProxyType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid proxy type: {}, using HTTP as default", typeStr);
      return ProxyType.HTTP;
    }
  }

  /** 解析速度等级 */
  private SpeedLevel parseSpeedLevel(String speedStr) {
    try {
      return SpeedLevel.valueOf(speedStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid speed level: {}, using MEDIUM as default", speedStr);
      return SpeedLevel.MEDIUM;
    }
  }

  /** 解析可靠性等级 */
  private ReliabilityLevel parseReliabilityLevel(String reliabilityStr) {
    try {
      return ReliabilityLevel.valueOf(reliabilityStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid reliability level: {}, using MEDIUM as default", reliabilityStr);
      return ReliabilityLevel.MEDIUM;
    }
  }

  /** 生成代理主机地址 */
  private String generateProxyHost() {
    if (random.nextBoolean()) {
      // 使用预定义的主机名
      return COMMON_PROXY_HOSTS.get(random.nextInt(COMMON_PROXY_HOSTS.size()));
    } else {
      // 生成IP地址
      return generateRandomIP();
    }
  }

  /** 生成随机IP地址 */
  private String generateRandomIP() {
    // 生成公网IP地址范围
    int[] ranges = {
      1, 126, // A类地址范围（排除127.x.x.x）
      128, 191, // B类地址范围
      192, 223 // C类地址范围
    };

    int rangeIndex = random.nextInt(3) * 2;
    int firstOctet =
        ranges[rangeIndex] + random.nextInt(ranges[rangeIndex + 1] - ranges[rangeIndex] + 1);

    int secondOctet = random.nextInt(256);
    int thirdOctet = random.nextInt(256);
    int fourthOctet = 1 + random.nextInt(254); // 避免0和255

    return String.format("%d.%d.%d.%d", firstOctet, secondOctet, thirdOctet, fourthOctet);
  }

  /** 生成代理端口 */
  private int generateProxyPort(ProxyType type) {
    List<Integer> ports = PROXY_PORTS.get(type);
    if (ports != null && !ports.isEmpty()) {
      return ports.get(random.nextInt(ports.size()));
    }
    return 8080; // 默认端口
  }

  /** 生成用户名 */
  private String generateUsername() {
    String[] prefixes = {"user", "proxy", "client", "guest", "test"};
    String prefix = prefixes[random.nextInt(prefixes.length)];
    int number = 1000 + random.nextInt(9000);
    return prefix + number;
  }

  /** 生成密码 */
  private String generatePassword() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    StringBuilder password = new StringBuilder();
    int length = 8 + random.nextInt(8); // 8-15位密码

    for (int i = 0; i < length; i++) {
      password.append(chars.charAt(random.nextInt(chars.length())));
    }

    return password.toString();
  }

  /** 格式化代理配置 */
  private String formatProxyConfig(ProxyConfig config, OutputFormat format) {
    switch (format) {
      case URL:
        return formatAsUrl(config);
      case JSON:
        return formatAsJson(config);
      case CONFIG:
        return formatAsConfig(config);
      case PAC:
        return formatAsPac(config);
      default:
        return formatAsUrl(config);
    }
  }

  /** 格式化为URL格式 */
  private String formatAsUrl(ProxyConfig config) {
    StringBuilder url = new StringBuilder();

    // 协议部分
    switch (config.getType()) {
      case HTTP:
        url.append("http://");
        break;
      case HTTPS:
        url.append("https://");
        break;
      case SOCKS4:
        url.append("socks4://");
        break;
      case SOCKS5:
        url.append("socks5://");
        break;
      case TRANSPARENT:
        url.append("http://");
        break;
      case RANDOM:
        // 随机选择一个代理类型
        ProxyType[] types = {ProxyType.HTTP, ProxyType.HTTPS, ProxyType.SOCKS4, ProxyType.SOCKS5};
        ProxyType randomType = types[random.nextInt(types.length)];
        switch (randomType) {
          case HTTP:
            url.append("http://");
            break;
          case HTTPS:
            url.append("https://");
            break;
          case SOCKS4:
            url.append("socks4://");
            break;
          case SOCKS5:
            url.append("socks5://");
            break;
          default:
            // 不应该到达这里，因为types数组只包含上述4种类型
            url.append("http://");
            break;
        }
        break;
    }

    // 认证信息
    if (config.isAuthRequired() && config.getUsername() != null && config.getPassword() != null) {
      url.append(config.getUsername()).append(":").append(config.getPassword()).append("@");
    }

    // 主机和端口
    url.append(config.getHost()).append(":").append(config.getPort());

    return url.toString();
  }

  /** 格式化为JSON格式 */
  private String formatAsJson(ProxyConfig config) {
    StringBuilder json = new StringBuilder("{");
    json.append("\"type\":\"").append(config.getType().name().toLowerCase()).append("\",");
    json.append("\"host\":\"").append(config.getHost()).append("\",");
    json.append("\"port\":").append(config.getPort()).append(",");
    json.append("\"authRequired\":").append(config.isAuthRequired()).append(",");
    json.append("\"anonymous\":").append(config.isAnonymous());

    if (config.getUsername() != null) {
      json.append(",\"username\":\"").append(config.getUsername()).append("\"");
    }

    if (config.getPassword() != null) {
      json.append(",\"password\":\"").append(config.getPassword()).append("\"");
    }

    if (config.getCountry() != null) {
      json.append(",\"country\":\"").append(config.getCountry()).append("\"");
    }

    if (config.getCity() != null) {
      json.append(",\"city\":\"").append(config.getCity()).append("\"");
    }

    json.append(",\"speed\":\"").append(config.getSpeed().name().toLowerCase()).append("\"");
    json.append(",\"reliability\":\"")
        .append(config.getReliability().name().toLowerCase())
        .append("\"");
    json.append(",\"url\":\"").append(formatAsUrl(config)).append("\"");

    json.append("}");
    return json.toString();
  }

  /** 格式化为配置格式 */
  private String formatAsConfig(ProxyConfig config) {
    StringBuilder configStr = new StringBuilder();
    configStr.append("Proxy Configuration:\n");
    configStr.append("  Type: ").append(config.getType().name()).append("\n");
    configStr.append("  Host: ").append(config.getHost()).append("\n");
    configStr.append("  Port: ").append(config.getPort()).append("\n");
    configStr.append("  URL: ").append(formatAsUrl(config)).append("\n");
    configStr
        .append("  Authentication: ")
        .append(config.isAuthRequired() ? "Required" : "Not Required")
        .append("\n");
    configStr.append("  Anonymous: ").append(config.isAnonymous() ? "Yes" : "No").append("\n");

    if (config.getCountry() != null) {
      configStr.append("  Country: ").append(config.getCountry()).append("\n");
    }

    if (config.getCity() != null) {
      configStr.append("  City: ").append(config.getCity()).append("\n");
    }

    configStr.append("  Speed: ").append(config.getSpeed().getDescription()).append("\n");
    configStr.append("  Reliability: ").append(config.getReliability().getDescription());

    return configStr.toString();
  }

  /** 格式化为PAC脚本格式 */
  private String formatAsPac(ProxyConfig config) {
    return String.format(
        "function FindProxyForURL(url, host) {\n" + "    return \"PROXY %s:%d\";\n" + "}",
        config.getHost(), config.getPort());
  }
}
