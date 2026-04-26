package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 端口号生成器
 *
 * <p>支持的参数： - type: 端口类型 (WELL_KNOWN|REGISTERED|DYNAMIC|COMMON|ANY) - min: 最小端口号 (0-65535) - max:
 * 最大端口号 (0-65535) - protocol: 协议类型 (TCP|UDP|BOTH) - service: 服务类型 (HTTP|HTTPS|FTP|SSH|SMTP|DNS|ANY)
 * - exclude_reserved: 是否排除保留端口 (true|false)
 *
 * @author DataForge
 */
public class PortGenerator extends BaseGenerator implements DataGenerator<Integer, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(PortGenerator.class);
  private static final Random random = new Random();

  // 端口范围定义
  private static final int WELL_KNOWN_MIN = 0;
  private static final int WELL_KNOWN_MAX = 1023;
  private static final int REGISTERED_MIN = 1024;
  private static final int REGISTERED_MAX = 49151;
  private static final int DYNAMIC_MIN = 49152;
  private static final int DYNAMIC_MAX = 65535;

  // 常见服务端口映射
  private static final Map<String, List<ServicePort>> SERVICE_PORTS = new HashMap<>();

  // 常用端口列表
  private static final List<Integer> COMMON_PORTS =
      Arrays.asList(
          21, 22, 23, 25, 53, 80, 110, 143, 443, 993, 995, 3000, 3306, 5432, 6379, 8000, 8080, 8443,
          8888, 9000);

  // 保留端口列表（通常不应该使用）
  private static final Set<Integer> RESERVED_PORTS =
      new HashSet<>(
          Arrays.asList(
              0, 7, 9, 13, 17, 19, 37, 42, 43, 67, 68, 69, 70, 79, 87, 95, 101, 102, 103, 104, 109,
              111, 113, 115, 117, 119, 123, 135, 137, 138, 139, 143, 161, 162, 163, 164, 174, 177,
              178, 179, 191, 194, 199, 201, 202, 204, 206, 209, 210, 213, 220, 245, 347, 363, 369,
              370, 372, 389, 427, 434, 435, 443, 444, 445, 464, 465, 497, 500, 512, 513, 514, 515,
              526, 530, 531, 532, 533, 540, 556, 563, 587, 601, 636, 639, 646, 647, 648, 652, 654,
              665, 666, 674, 691, 692, 695, 696, 704, 711, 712, 720, 749, 750, 751, 752, 754, 760,
              782, 783, 829, 860, 873, 888, 898, 900, 901, 902, 903, 911, 912, 981, 987, 990, 991,
              992, 993, 995, 996, 997, 998, 999));

  // 服务端口信息类
  private static class ServicePort {
    final int port;
    final String protocol;
    final String description;

    ServicePort(int port, String protocol, String description) {
      this.port = port;
      this.protocol = protocol;
      this.description = description;
    }
  }

  static {
    initializeServicePorts();
  }

  private static void initializeServicePorts() {
    // HTTP服务
    SERVICE_PORTS.put(
        "HTTP",
        Arrays.asList(
            new ServicePort(80, "TCP", "HTTP"),
            new ServicePort(8080, "TCP", "HTTP Alternate"),
            new ServicePort(8000, "TCP", "HTTP Alternate"),
            new ServicePort(3000, "TCP", "HTTP Development"),
            new ServicePort(8888, "TCP", "HTTP Alternate")));

    // HTTPS服务
    SERVICE_PORTS.put(
        "HTTPS",
        Arrays.asList(
            new ServicePort(443, "TCP", "HTTPS"),
            new ServicePort(8443, "TCP", "HTTPS Alternate"),
            new ServicePort(9443, "TCP", "HTTPS Alternate")));

    // FTP服务
    SERVICE_PORTS.put(
        "FTP",
        Arrays.asList(
            new ServicePort(21, "TCP", "FTP Control"),
            new ServicePort(20, "TCP", "FTP Data"),
            new ServicePort(990, "TCP", "FTPS")));

    // SSH服务
    SERVICE_PORTS.put(
        "SSH",
        Arrays.asList(
            new ServicePort(22, "TCP", "SSH"), new ServicePort(2222, "TCP", "SSH Alternate")));

    // SMTP服务
    SERVICE_PORTS.put(
        "SMTP",
        Arrays.asList(
            new ServicePort(25, "TCP", "SMTP"),
            new ServicePort(587, "TCP", "SMTP Submission"),
            new ServicePort(465, "TCP", "SMTPS")));

    // DNS服务
    SERVICE_PORTS.put(
        "DNS", Arrays.asList(new ServicePort(53, "UDP", "DNS"), new ServicePort(53, "TCP", "DNS")));

    // 数据库服务
    SERVICE_PORTS.put(
        "DATABASE",
        Arrays.asList(
            new ServicePort(3306, "TCP", "MySQL"),
            new ServicePort(5432, "TCP", "PostgreSQL"),
            new ServicePort(1433, "TCP", "SQL Server"),
            new ServicePort(1521, "TCP", "Oracle"),
            new ServicePort(27017, "TCP", "MongoDB"),
            new ServicePort(6379, "TCP", "Redis")));

    // Web服务器
    SERVICE_PORTS.put(
        "WEBSERVER",
        Arrays.asList(
            new ServicePort(80, "TCP", "Apache/Nginx"),
            new ServicePort(443, "TCP", "Apache/Nginx HTTPS"),
            new ServicePort(8080, "TCP", "Tomcat"),
            new ServicePort(9000, "TCP", "PHP-FPM"),
            new ServicePort(3000, "TCP", "Node.js")));

    // 邮件服务
    SERVICE_PORTS.put(
        "MAIL",
        Arrays.asList(
            new ServicePort(25, "TCP", "SMTP"),
            new ServicePort(110, "TCP", "POP3"),
            new ServicePort(143, "TCP", "IMAP"),
            new ServicePort(993, "TCP", "IMAPS"),
            new ServicePort(995, "TCP", "POP3S")));

    // 游戏服务
    SERVICE_PORTS.put(
        "GAMING",
        Arrays.asList(
            new ServicePort(25565, "TCP", "Minecraft"),
            new ServicePort(27015, "UDP", "Steam"),
            new ServicePort(3724, "TCP", "World of Warcraft"),
            new ServicePort(6112, "TCP", "Battle.net")));
  }

  @Override
  public String getType() {
    return "port";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public Integer generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String type = config.getParam("type", String.class, "ANY");
      int min = Integer.parseInt(config.getParam("min", String.class, "0"));
      int max = Integer.parseInt(config.getParam("max", String.class, "65535"));
      String protocol = config.getParam("protocol", String.class, "BOTH");
      String service = config.getParam("service", String.class, "ANY");
      boolean excludeReserved =
          Boolean.parseBoolean(config.getParam("exclude_reserved", String.class, "true"));

      // 生成端口号
      int port = generatePort(type, min, max, protocol, service, excludeReserved);

      // 将端口信息存入上下文
      context.put("port", port);
      context.put("port_type", determinePortType(port));
      context.put("port_protocol", protocol);
      context.put("port_service", getServiceName(port));

      logger.debug("Generated port: {}", port);
      return port;

    } catch (Exception e) {
      logger.error("Error generating port", e);
      return 8080;
    }
  }

  private int generatePort(
      String type, int min, int max, String protocol, String service, boolean excludeReserved) {
    // 处理服务类型
    if (!"ANY".equalsIgnoreCase(service)) {
      return generateServicePort(service, protocol);
    }

    // 处理端口类型
    if ("COMMON".equalsIgnoreCase(type)) {
      return generateCommonPort(excludeReserved);
    }

    // 确定端口范围
    int[] range = determinePortRange(type, min, max);
    int finalMin = range[0];
    int finalMax = range[1];

    // 生成端口
    int port;
    int attempts = 0;
    do {
      port = finalMin + random.nextInt(finalMax - finalMin + 1);
      attempts++;
    } while (excludeReserved && RESERVED_PORTS.contains(port) && attempts < 100);

    return port;
  }

  private int generateServicePort(String service, String protocol) {
    List<ServicePort> servicePorts = SERVICE_PORTS.get(service.toUpperCase());
    if (servicePorts == null || servicePorts.isEmpty()) {
      // 如果服务不存在，返回常用端口
      return COMMON_PORTS.get(random.nextInt(COMMON_PORTS.size()));
    }

    // 过滤协议
    List<ServicePort> filteredPorts = new ArrayList<>();
    for (ServicePort servicePort : servicePorts) {
      if ("BOTH".equalsIgnoreCase(protocol) || protocol.equalsIgnoreCase(servicePort.protocol)) {
        filteredPorts.add(servicePort);
      }
    }

    if (filteredPorts.isEmpty()) {
      filteredPorts = servicePorts; // 如果过滤后为空，使用所有端口
    }

    ServicePort selectedPort = filteredPorts.get(random.nextInt(filteredPorts.size()));
    return selectedPort.port;
  }

  private int generateCommonPort(boolean excludeReserved) {
    List<Integer> availablePorts = new ArrayList<>(COMMON_PORTS);

    if (excludeReserved) {
      availablePorts.removeAll(RESERVED_PORTS);
    }

    if (availablePorts.isEmpty()) {
      return 8080; // 默认端口
    }

    return availablePorts.get(random.nextInt(availablePorts.size()));
  }

  private int[] determinePortRange(String type, int min, int max) {
    switch (type.toUpperCase()) {
      case "WELL_KNOWN":
        return new int[] {Math.max(min, WELL_KNOWN_MIN), Math.min(max, WELL_KNOWN_MAX)};

      case "REGISTERED":
        return new int[] {Math.max(min, REGISTERED_MIN), Math.min(max, REGISTERED_MAX)};

      case "DYNAMIC":
        return new int[] {Math.max(min, DYNAMIC_MIN), Math.min(max, DYNAMIC_MAX)};

      case "ANY":
      default:
        return new int[] {Math.max(min, 0), Math.min(max, 65535)};
    }
  }

  private String determinePortType(int port) {
    if (port >= WELL_KNOWN_MIN && port <= WELL_KNOWN_MAX) {
      return "WELL_KNOWN";
    } else if (port >= REGISTERED_MIN && port <= REGISTERED_MAX) {
      return "REGISTERED";
    } else if (port >= DYNAMIC_MIN && port <= DYNAMIC_MAX) {
      return "DYNAMIC";
    } else {
      return "UNKNOWN";
    }
  }

  private String getServiceName(int port) {
    // 查找端口对应的服务名
    for (Map.Entry<String, List<ServicePort>> entry : SERVICE_PORTS.entrySet()) {
      for (ServicePort servicePort : entry.getValue()) {
        if (servicePort.port == port) {
          return entry.getKey() + " (" + servicePort.description + ")";
        }
      }
    }

    // 检查是否为常用端口
    if (COMMON_PORTS.contains(port)) {
      return "COMMON";
    }

    return "UNKNOWN";
  }

  /** 验证端口号是否有效 */
  public boolean validatePort(int port) {
    return port >= 0 && port <= 65535;
  }

  /** 检查端口是否为保留端口 */
  public boolean isReservedPort(int port) {
    return RESERVED_PORTS.contains(port);
  }

  /** 检查端口是否为知名端口 */
  public boolean isWellKnownPort(int port) {
    return port >= WELL_KNOWN_MIN && port <= WELL_KNOWN_MAX;
  }

  /** 生成端口范围 */
  public String generatePortRange(int count) {
    List<Integer> ports = new ArrayList<>();
    Set<Integer> usedPorts = new HashSet<>();

    while (ports.size() < count) {
      int port = generatePort("ANY", 1024, 65535, "BOTH", "ANY", true);
      if (!usedPorts.contains(port)) {
        ports.add(port);
        usedPorts.add(port);
      }
    }

    Collections.sort(ports);

    if (count == 2) {
      return ports.get(0) + "-" + ports.get(1);
    } else {
      return ports.toString().replaceAll("[\\[\\]]", "");
    }
  }

  /** 生成防火墙规则端口 */
  public String generateFirewallPort() {
    // 防火墙规则通常使用常见服务端口
    String[] services = {"HTTP", "HTTPS", "SSH", "FTP", "SMTP", "DNS"};
    String service = services[random.nextInt(services.length)];

    int port = generateServicePort(service, "BOTH");
    return String.valueOf(port);
  }

  /** 生成恶意端口（用于安全测试） */
  public int generateMaliciousPort() {
    // 一些常见的恶意软件使用的端口
    int[] maliciousPorts = {
      1337, 31337, 12345, 54321, 9999, 6666, 1234, 4321,
      6969, 1981, 1999, 2001, 5555, 7777, 8888, 9876
    };

    return maliciousPorts[random.nextInt(maliciousPorts.length)];
  }

  /** 生成高权限端口（需要管理员权限） */
  public int generatePrivilegedPort() {
    // 小于1024的端口通常需要管理员权限
    return random.nextInt(1024);
  }

  /** 生成开发环境常用端口 */
  public int generateDevelopmentPort() {
    int[] devPorts = {3000, 3001, 4000, 5000, 8000, 8080, 8888, 9000, 9090};
    return devPorts[random.nextInt(devPorts.length)];
  }
}
