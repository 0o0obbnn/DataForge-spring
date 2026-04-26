package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 二进制数据生成器
 *
 * <p>生成随机二进制数据和Base64编码数据，用于文件上传测试、 数据传输测试、编码解码测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>size: 数据大小（字节）默认: 1024
 *   <li>format: 输出格式 (BINARY|BASE64|HEX|BYTES_ARRAY) 默认: BASE64
 *   <li>pattern: 数据模式 (RANDOM|ZEROS|ONES|ALTERNATING|CUSTOM) 默认: RANDOM
 *   <li>custom_byte: 自定义字节值（0-255）默认: 0
 *   <li>include_nulls: 是否包含空字节 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class BinaryDataGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(BinaryDataGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  private static final int MAX_SIZE = 10 * 1024 * 1024; // 10MB限制
  private static final int MIN_SIZE = 1;

  @Override
  public String getType() {
    return "binary_data";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      int size = getIntParam(config, "size", 1024);
      String format = getStringParam(config, "format", "BASE64").toUpperCase();
      String pattern = getStringParam(config, "pattern", "RANDOM").toUpperCase();
      int customByte = getIntParam(config, "custom_byte", 0);
      boolean includeNulls = getBooleanParam(config, "include_nulls", false);

      // 验证大小限制
      size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));

      byte[] binaryData = generateBinaryData(size, pattern, customByte, includeNulls);
      String result = formatBinaryData(binaryData, format);

      // 存储到上下文
      context.put("binary_data_size", size);
      context.put("binary_data_format", format);
      context.put("binary_data_pattern", pattern);

      return result;

    } catch (Exception e) {
      logger.error("Failed to generate binary data", e);
      return generateFallbackData();
    }
  }

  private byte[] generateBinaryData(
      int size, String pattern, int customByte, boolean includeNulls) {
    byte[] data = new byte[size];

    switch (pattern) {
      case "ZEROS":
        // 数组默认就是全零
        break;
      case "ONES":
        for (int i = 0; i < size; i++) {
          data[i] = (byte) 0xFF;
        }
        break;
      case "ALTERNATING":
        for (int i = 0; i < size; i++) {
          data[i] = (byte) (i % 2 == 0 ? 0xAA : 0x55);
        }
        break;
      case "CUSTOM":
        byte customByteValue = (byte) (customByte & 0xFF);
        for (int i = 0; i < size; i++) {
          data[i] = customByteValue;
        }
        break;
      case "RANDOM":
      default:
        if (includeNulls) {
          random.nextBytes(data);
        } else {
          // 生成不包含空字节的随机数据
          for (int i = 0; i < size; i++) {
            int value;
            do {
              value = random.nextInt(256);
            } while (value == 0);
            data[i] = (byte) value;
          }
        }
        break;
    }

    return data;
  }

  private String formatBinaryData(byte[] data, String format) {
    switch (format) {
      case "BINARY":
        return formatAsBinary(data);
      case "HEX":
        return formatAsHex(data);
      case "BYTES_ARRAY":
        return formatAsBytesArray(data);
      case "BASE64":
      default:
        return formatAsBase64(data);
    }
  }

  private String formatAsBinary(byte[] data) {
    StringBuilder binary = new StringBuilder();
    int maxBytes = Math.min(data.length, 100); // 限制输出长度

    for (int i = 0; i < maxBytes; i++) {
      if (i > 0 && i % 8 == 0) {
        binary.append(" ");
      }
      binary.append(String.format("%8s", Integer.toBinaryString(data[i] & 0xFF)).replace(' ', '0'));
    }

    if (data.length > maxBytes) {
      binary.append("...(").append(data.length - maxBytes).append(" more bytes)");
    }

    return binary.toString();
  }

  private String formatAsHex(byte[] data) {
    StringBuilder hex = new StringBuilder();
    int maxBytes = Math.min(data.length, 1000); // 限制输出长度

    for (int i = 0; i < maxBytes; i++) {
      if (i > 0 && i % 16 == 0) {
        hex.append("\n");
      } else if (i > 0 && i % 2 == 0) {
        hex.append(" ");
      }
      hex.append(String.format("%02X", data[i] & 0xFF));
    }

    if (data.length > maxBytes) {
      hex.append("\n...(").append(data.length - maxBytes).append(" more bytes)");
    }

    return hex.toString();
  }

  private String formatAsBytesArray(byte[] data) {
    StringBuilder array = new StringBuilder("[");
    int maxBytes = Math.min(data.length, 100); // 限制输出长度

    for (int i = 0; i < maxBytes; i++) {
      if (i > 0) array.append(", ");
      array.append(data[i] & 0xFF);
    }

    if (data.length > maxBytes) {
      array.append(", ...(").append(data.length - maxBytes).append(" more)");
    }

    array.append("]");
    return array.toString();
  }

  private String formatAsBase64(byte[] data) {
    // 简单的Base64编码实现
    StringBuilder base64 = new StringBuilder();
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    for (int i = 0; i < data.length; i += 3) {
      int b1 = data[i] & 0xFF;
      int b2 = (i + 1 < data.length) ? data[i + 1] & 0xFF : 0;
      int b3 = (i + 2 < data.length) ? data[i + 2] & 0xFF : 0;

      int combined = (b1 << 16) | (b2 << 8) | b3;

      base64.append(chars.charAt((combined >> 18) & 0x3F));
      base64.append(chars.charAt((combined >> 12) & 0x3F));
      base64.append((i + 1 < data.length) ? chars.charAt((combined >> 6) & 0x3F) : '=');
      base64.append((i + 2 < data.length) ? chars.charAt(combined & 0x3F) : '=');

      // 每76个字符换行（标准Base64格式）
      if (base64.length() % 76 == 0) {
        base64.append("\n");
      }
    }

    return base64.toString();
  }

  private String generateFallbackData() {
    // 生成简单的Base64数据作为fallback
    byte[] fallbackData = new byte[64];
    random.nextBytes(fallbackData);
    return formatAsBase64(fallbackData);
  }
}
