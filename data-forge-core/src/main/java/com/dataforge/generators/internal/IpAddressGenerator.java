package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP地址生成器
 *
 * <p>支持的参数： - version: IP版本 (IPV4|IPV6|ANY) - type: IP类型 (PUBLIC|PRIVATE|LOOPBACK|MULTICAST|ANY) -
 * subnet: 子网范围 (如 "192.168.1.0/24") - format: 输出格式 (STANDARD|COMPRESSED|FULL)
 *
 * @author DataForge
 */
public class IpAddressGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(IpAddressGenerator.class);
  private static final Random random = new Random();

  // IPv4私网地址范围
  private static final List<String> IPV4_PRIVATE_RANGES =
      Arrays.asList(
          "10.0.0.0/8", // 10.0.0.0 - 10.255.255.255
          "172.16.0.0/12", // 172.16.0.0 - 172.31.255.255
          "192.168.0.0/16" // 192.168.0.0 - 192.168.255.255
          );

  @Override
  public String getType() {
    return "ip";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String version = config.getParam("version", String.class, "IPV4");
      String type = config.getParam("type", String.class, "ANY");
      String subnet = config.getParam("subnet", String.class, null);
      String format = config.getParam("format", String.class, "STANDARD");

      // 生成IP地址
      String ipAddress;
      if (subnet != null && !subnet.isEmpty()) {
        ipAddress = generateFromSubnet(subnet);
      } else if ("IPV6".equalsIgnoreCase(version)) {
        ipAddress = generateIpv6(type, format);
      } else {
        ipAddress = generateIpv4(type);
      }

      // 将IP信息存入上下文
      context.put("ip_address", ipAddress);
      context.put("ip_version", version);

      logger.debug("Generated IP address: {}", ipAddress);
      return ipAddress;

    } catch (Exception e) {
      logger.error("Error generating IP address", e);
      return "192.168.1.1";
    }
  }

  private String generateFromSubnet(String subnet) {
    try {
      String[] parts = subnet.split("/");
      if (parts.length != 2) {
        return generateIpv4("PRIVATE");
      }

      String networkAddress = parts[0];
      int prefixLength = Integer.parseInt(parts[1]);

      if (networkAddress.contains(":")) {
        return generateIpv6FromSubnet(networkAddress, prefixLength);
      } else {
        return generateIpv4FromSubnet(networkAddress, prefixLength);
      }

    } catch (Exception e) {
      logger.warn("Failed to generate IP from subnet: {}", subnet, e);
      return generateIpv4("PRIVATE");
    }
  }

  private String generateIpv4FromSubnet(String networkAddress, int prefixLength) {
    String[] octets = networkAddress.split("\\.");
    if (octets.length != 4) {
      return generateIpv4("PRIVATE");
    }

    // 计算主机位数
    int hostBits = 32 - prefixLength;
    int maxHosts = Math.max(1, (1 << hostBits) - 2);

    // 生成随机主机号
    int hostId = random.nextInt(maxHosts) + 1;

    // 将网络地址转换为整数
    long networkInt = 0;
    for (int i = 0; i < 4; i++) {
      networkInt = (networkInt << 8) + Integer.parseInt(octets[i]);
    }

    // 添加主机号
    long ipInt = networkInt + hostId;

    // 转换回点分十进制
    return String.format(
        "%d.%d.%d.%d",
        (ipInt >> 24) & 0xFF, (ipInt >> 16) & 0xFF, (ipInt >> 8) & 0xFF, ipInt & 0xFF);
  }

  private String generateIpv4(String type) {
    switch (type.toUpperCase()) {
      case "PRIVATE":
        return generateIpv4Private();

      case "LOOPBACK":
        return "127." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);

      case "MULTICAST":
        return (224 + random.nextInt(16))
            + "."
            + random.nextInt(256)
            + "."
            + random.nextInt(256)
            + "."
            + random.nextInt(256);

      case "PUBLIC":
        return generateIpv4Public();

      case "ANY":
      default:
        return random.nextBoolean() ? generateIpv4Private() : generateIpv4Public();
    }
  }

  private String generateIpv4Private() {
    String range = IPV4_PRIVATE_RANGES.get(random.nextInt(IPV4_PRIVATE_RANGES.size()));

    if (range.equals("10.0.0.0/8")) {
      return "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
    } else if (range.equals("172.16.0.0/12")) {
      return "172."
          + (16 + random.nextInt(16))
          + "."
          + random.nextInt(256)
          + "."
          + random.nextInt(256);
    } else { // 192.168.0.0/16
      return "192.168." + random.nextInt(256) + "." + random.nextInt(256);
    }
  }

  private String generateIpv4Public() {
    int a, b, c, d;

    do {
      a = 1 + random.nextInt(223); // 避免0和224-255
      b = random.nextInt(256);
      c = random.nextInt(256);
      d = random.nextInt(256);
    } while (isReservedIpv4(a, b, c, d));

    return a + "." + b + "." + c + "." + d;
  }

  private boolean isReservedIpv4(int a, int b, int c, int d) {
    // 检查是否为保留地址
    if (a == 127) {
      return true; // 127.x.x.x
    }
    if (a == 10) {
      return true; // 10.x.x.x
    }
    if (a == 172 && b >= 16 && b <= 31) {
      return true; // 172.16-31.x.x
    }
    if (a == 192 && b == 168) {
      return true; // 192.168.x.x
    }
    if (a == 169 && b == 254) {
      return true; // 169.254.x.x
    }

    return false;
  }

  private String generateIpv6(String type, String format) {
    StringBuilder ipv6 = new StringBuilder();

    switch (type.toUpperCase()) {
      case "PRIVATE":
        ipv6.append("fd").append(String.format("%02x", random.nextInt(256)));
        break;

      case "LOOPBACK":
        return formatIpv6("::1", format);

      case "MULTICAST":
        ipv6.append("ff").append(String.format("%02x", random.nextInt(256)));
        break;

      case "LINK_LOCAL":
        ipv6.append("fe80");
        break;

      case "PUBLIC":
      case "ANY":
      default:
        // 生成全球单播地址
        ipv6.append(String.format("%04x", 0x2000 + random.nextInt(0x2000)));
        break;
    }

    // 补充剩余部分
    for (int i = 1; i < 8; i++) {
      ipv6.append(":").append(String.format("%04x", random.nextInt(0x10000)));
    }

    return formatIpv6(ipv6.toString(), format);
  }

  private String generateIpv6FromSubnet(String networkAddress, int prefixLength) {
    // 简化的IPv6子网生成
    String[] parts = networkAddress.split(":");
    StringBuilder ipv6 = new StringBuilder();

    // 保留网络部分，随机生成主机部分
    int networkParts = prefixLength / 16;

    for (int i = 0; i < 8; i++) {
      if (i > 0) {
        ipv6.append(":");
      }

      if (i < networkParts && i < parts.length) {
        ipv6.append(parts[i]);
      } else {
        ipv6.append(String.format("%04x", random.nextInt(0x10000)));
      }
    }

    return ipv6.toString();
  }

  private String formatIpv6(String ipv6, String format) {
    switch (format.toUpperCase()) {
      case "COMPRESSED":
        return compressIpv6(ipv6);

      case "FULL":
        return expandIpv6(ipv6);

      case "STANDARD":
      default:
        return ipv6;
    }
  }

  private String compressIpv6(String ipv6) {
    // 简化的IPv6压缩算法
    return ipv6.replaceAll("(^|:)0+([0-9a-fA-F])", "$1$2").replaceAll("::+", "::");
  }

  private String expandIpv6(String ipv6) {
    // 简化的IPv6展开算法
    String[] parts = ipv6.split(":");
    StringBuilder expanded = new StringBuilder();

    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        expanded.append(":");
      }

      String part = parts[i];
      if (part.length() < 4) {
        part = String.format("%04s", part).replace(' ', '0');
      }
      expanded.append(part);
    }

    return expanded.toString();
  }
}
