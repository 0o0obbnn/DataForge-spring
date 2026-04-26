package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MAC地址生成器
 *
 * <p>支持的参数： - format: 输出格式 (COLON|HYPHEN|DOT|NONE) - case: 大小写 (UPPER|LOWER|MIXED) - vendor: 厂商OUI
 * (如 "VMWARE", "INTEL") - type: 地址类型 (UNICAST|MULTICAST|BROADCAST|ANY)
 *
 * @author DataForge
 */
public class MacAddressGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(MacAddressGenerator.class);
  private static final Random random = new Random();

  // 常见厂商OUI（前3字节）
  private static final Map<String, String> VENDOR_OUIS = new HashMap<>();

  static {
    initializeVendorOuis();
  }

  private static void initializeVendorOuis() {
    VENDOR_OUIS.put("INTEL", "00:15:17");
    VENDOR_OUIS.put("CISCO", "00:1B:0D");
    VENDOR_OUIS.put("APPLE", "00:1B:63");
    VENDOR_OUIS.put("DELL", "00:14:22");
    VENDOR_OUIS.put("HP", "00:1F:29");
    VENDOR_OUIS.put("VMWARE", "00:50:56");
    VENDOR_OUIS.put("MICROSOFT", "00:15:5D");
    VENDOR_OUIS.put("BROADCOM", "00:10:18");
    VENDOR_OUIS.put("REALTEK", "00:E0:4C");
    VENDOR_OUIS.put("QUALCOMM", "00:03:7F");
    VENDOR_OUIS.put("SAMSUNG", "00:16:32");
    VENDOR_OUIS.put("HUAWEI", "00:E0:FC");
    VENDOR_OUIS.put("XIAOMI", "34:CE:00");
    VENDOR_OUIS.put("TPLINK", "50:C7:BF");
    VENDOR_OUIS.put("NETGEAR", "20:4E:7F");
  }

  @Override
  public String getType() {
    return "mac";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String format = config.getParam("format", String.class, "COLON");
      String caseParam = config.getParam("case", String.class, "LOWER");
      String vendor = config.getParam("vendor", String.class, null);
      String type = config.getParam("type", String.class, "UNICAST");

      // 生成MAC地址
      String macAddress = generateMacAddress(vendor, type);

      // 应用格式和大小写
      macAddress = applyFormat(macAddress, format);
      macAddress = applyCase(macAddress, caseParam);

      // 将MAC信息存入上下文
      context.put("mac_address", macAddress);
      context.put("mac_vendor", extractVendor(macAddress));

      logger.debug("Generated MAC address: {}", macAddress);
      return macAddress;

    } catch (Exception e) {
      logger.error("Error generating MAC address", e);
      return "00:50:56:12:34:56";
    }
  }

  private String generateMacAddress(String vendor, String type) {
    byte[] macBytes = new byte[6];

    // 处理厂商OUI
    if (vendor != null && !vendor.isEmpty()) {
      String oui = getVendorOui(vendor);
      if (oui != null) {
        String[] ouiParts = oui.split(":");
        for (int i = 0; i < 3 && i < ouiParts.length; i++) {
          macBytes[i] = (byte) Integer.parseInt(ouiParts[i], 16);
        }
      } else {
        generateRandomOui(macBytes);
      }
    } else {
      generateRandomOui(macBytes);
    }

    // 设置地址类型标志位
    setAddressTypeFlags(macBytes, type);

    // 生成后3字节（设备标识符）
    for (int i = 3; i < 6; i++) {
      macBytes[i] = (byte) random.nextInt(256);
    }

    // 转换为十六进制字符串
    StringBuilder mac = new StringBuilder();
    for (int i = 0; i < macBytes.length; i++) {
      if (i > 0) {
        mac.append(":");
      }
      mac.append(String.format("%02x", macBytes[i] & 0xFF));
    }

    return mac.toString();
  }

  private String getVendorOui(String vendor) {
    return VENDOR_OUIS.get(vendor.toUpperCase());
  }

  private void generateRandomOui(byte[] macBytes) {
    // 生成随机的前3字节
    for (int i = 0; i < 3; i++) {
      macBytes[i] = (byte) random.nextInt(256);
    }
  }

  private void setAddressTypeFlags(byte[] macBytes, String type) {
    // MAC地址的第一个字节的最低位是组播位（I/G位）
    // 第二低位是本地管理位（U/L位）

    byte firstByte = macBytes[0];

    // 设置组播位
    switch (type.toUpperCase()) {
      case "MULTICAST":
        firstByte |= 0x01; // 设置组播位
        break;
      case "BROADCAST":
        // 广播地址是特殊的组播地址
        Arrays.fill(macBytes, (byte) 0xFF);
        return;
      case "UNICAST":
      default:
        firstByte &= 0xFE; // 清除组播位
        break;
    }

    macBytes[0] = firstByte;
  }

  private String applyFormat(String macAddress, String format) {
    // 移除所有分隔符
    String cleanMac = macAddress.replace(":", "").replace("-", "").replace(".", "");

    switch (format.toUpperCase()) {
      case "COLON":
        return formatWithSeparator(cleanMac, ":", 2);

      case "HYPHEN":
        return formatWithSeparator(cleanMac, "-", 2);

      case "DOT":
        return formatWithSeparator(cleanMac, ".", 4);

      case "NONE":
        return cleanMac;

      default:
        logger.warn("Unknown MAC format: {}. Using COLON format.", format);
        return formatWithSeparator(cleanMac, ":", 2);
    }
  }

  private String formatWithSeparator(String cleanMac, String separator, int groupSize) {
    StringBuilder formatted = new StringBuilder();

    for (int i = 0; i < cleanMac.length(); i += groupSize) {
      if (i > 0) {
        formatted.append(separator);
      }

      int endIndex = Math.min(i + groupSize, cleanMac.length());
      formatted.append(cleanMac.substring(i, endIndex));
    }

    return formatted.toString();
  }

  private String applyCase(String macAddress, String caseParam) {
    switch (caseParam.toUpperCase()) {
      case "UPPER":
        return macAddress.toUpperCase();

      case "LOWER":
        return macAddress.toLowerCase();

      case "MIXED":
        StringBuilder mixed = new StringBuilder();
        for (int i = 0; i < macAddress.length(); i++) {
          char c = macAddress.charAt(i);
          if (Character.isLetter(c)) {
            mixed.append(
                random.nextBoolean() ? Character.toUpperCase(c) : Character.toLowerCase(c));
          } else {
            mixed.append(c);
          }
        }
        return mixed.toString();

      default:
        logger.warn("Unknown MAC case: {}. Using LOWER case.", caseParam);
        return macAddress.toLowerCase();
    }
  }

  private String extractVendor(String macAddress) {
    // 提取前3字节作为OUI
    String cleanMac = macAddress.replace(":", "").replace("-", "").replace(".", "");
    if (cleanMac.length() >= 6) {
      String oui = cleanMac.substring(0, 6);
      oui = oui.substring(0, 2) + ":" + oui.substring(2, 4) + ":" + oui.substring(4, 6);

      // 查找对应的厂商
      for (Map.Entry<String, String> entry : VENDOR_OUIS.entrySet()) {
        if (entry.getValue().equalsIgnoreCase(oui)) {
          return entry.getKey();
        }
      }
    }

    return "UNKNOWN";
  }
}
