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
 * 文件头生成器
 *
 * <p>生成各种文件格式的魔数（Magic Number）和文件头，用于文件类型检测、 安全测试、文件上传验证等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>file_type: 文件类型 (IMAGE|DOCUMENT|ARCHIVE|EXECUTABLE|AUDIO|VIDEO|CUSTOM) 默认: IMAGE
 *   <li>format: 输出格式 (HEX|BYTES|BASE64) 默认: HEX
 *   <li>specific_type: 具体类型 (如PNG|JPG|PDF|ZIP等)
 *   <li>include_extension: 是否包含扩展名 默认: false
 *   <li>malicious: 是否生成恶意文件头 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class FileHeaderGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(FileHeaderGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 文件头映射
  private static final Map<String, FileHeader> FILE_HEADERS = new HashMap<>();

  static {
    // 图像文件
    FILE_HEADERS.put(
        "PNG",
        new FileHeader(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, "png"));
    FILE_HEADERS.put(
        "JPG", new FileHeader(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "jpg"));
    FILE_HEADERS.put("GIF", new FileHeader(new byte[] {0x47, 0x49, 0x46, 0x38}, "gif"));
    FILE_HEADERS.put("BMP", new FileHeader(new byte[] {0x42, 0x4D}, "bmp"));
    FILE_HEADERS.put("WEBP", new FileHeader(new byte[] {0x52, 0x49, 0x46, 0x46}, "webp"));

    // 文档文件
    FILE_HEADERS.put("PDF", new FileHeader(new byte[] {0x25, 0x50, 0x44, 0x46}, "pdf"));
    FILE_HEADERS.put(
        "DOC", new FileHeader(new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}, "doc"));
    FILE_HEADERS.put("DOCX", new FileHeader(new byte[] {0x50, 0x4B, 0x03, 0x04}, "docx"));
    FILE_HEADERS.put(
        "XLS", new FileHeader(new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}, "xls"));
    FILE_HEADERS.put("XLSX", new FileHeader(new byte[] {0x50, 0x4B, 0x03, 0x04}, "xlsx"));

    // 压缩文件
    FILE_HEADERS.put("ZIP", new FileHeader(new byte[] {0x50, 0x4B, 0x03, 0x04}, "zip"));
    FILE_HEADERS.put("RAR", new FileHeader(new byte[] {0x52, 0x61, 0x72, 0x21}, "rar"));
    FILE_HEADERS.put("7Z", new FileHeader(new byte[] {0x37, 0x7A, (byte) 0xBC, (byte) 0xAF}, "7z"));
    FILE_HEADERS.put("TAR", new FileHeader(new byte[] {0x75, 0x73, 0x74, 0x61, 0x72}, "tar"));

    // 可执行文件
    FILE_HEADERS.put("EXE", new FileHeader(new byte[] {0x4D, 0x5A}, "exe"));
    FILE_HEADERS.put("DLL", new FileHeader(new byte[] {0x4D, 0x5A}, "dll"));
    FILE_HEADERS.put("ELF", new FileHeader(new byte[] {0x7F, 0x45, 0x4C, 0x46}, ""));

    // 音频文件
    FILE_HEADERS.put("MP3", new FileHeader(new byte[] {(byte) 0xFF, (byte) 0xFB}, "mp3"));
    FILE_HEADERS.put("WAV", new FileHeader(new byte[] {0x52, 0x49, 0x46, 0x46}, "wav"));
    FILE_HEADERS.put("FLAC", new FileHeader(new byte[] {0x66, 0x4C, 0x61, 0x43}, "flac"));

    // 视频文件
    FILE_HEADERS.put(
        "MP4", new FileHeader(new byte[] {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}, "mp4"));
    FILE_HEADERS.put("AVI", new FileHeader(new byte[] {0x52, 0x49, 0x46, 0x46}, "avi"));
    FILE_HEADERS.put(
        "MKV", new FileHeader(new byte[] {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}, "mkv"));
  }

  // 文件头信息类
  private static class FileHeader {
    final byte[] magic;
    final String extension;

    FileHeader(byte[] magic, String extension) {
      this.magic = magic;
      this.extension = extension;
    }
  }

  @Override
  public String getType() {
    return "file_header";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String fileType = getStringParam(config, "file_type", "IMAGE").toUpperCase();
      String format = getStringParam(config, "format", "HEX").toUpperCase();
      String specificType = getStringParam(config, "specific_type", null);
      boolean includeExtension = getBooleanParam(config, "include_extension", false);
      boolean malicious = getBooleanParam(config, "malicious", false);

      FileHeader header = selectFileHeader(fileType, specificType);
      byte[] headerBytes = header.magic;

      if (malicious) {
        headerBytes = generateMaliciousHeader(headerBytes);
      }

      String result = formatHeader(headerBytes, format);

      if (includeExtension && !header.extension.isEmpty()) {
        result += " (." + header.extension + ")";
      }

      // 存储到上下文
      context.put("file_type", fileType);
      context.put("file_extension", header.extension);
      context.put("file_header", result);

      return result;

    } catch (Exception e) {
      logger.error("Failed to generate file header", e);
      return "89504E470D0A1A0A"; // PNG header as fallback
    }
  }

  private FileHeader selectFileHeader(String fileType, String specificType) {
    if (specificType != null) {
      FileHeader specific = FILE_HEADERS.get(specificType.toUpperCase());
      if (specific != null) {
        return specific;
      }
    }

    String[] typeKeys;
    switch (fileType) {
      case "IMAGE":
        typeKeys = new String[] {"PNG", "JPG", "GIF", "BMP", "WEBP"};
        break;
      case "DOCUMENT":
        typeKeys = new String[] {"PDF", "DOC", "DOCX", "XLS", "XLSX"};
        break;
      case "ARCHIVE":
        typeKeys = new String[] {"ZIP", "RAR", "7Z", "TAR"};
        break;
      case "EXECUTABLE":
        typeKeys = new String[] {"EXE", "DLL", "ELF"};
        break;
      case "AUDIO":
        typeKeys = new String[] {"MP3", "WAV", "FLAC"};
        break;
      case "VIDEO":
        typeKeys = new String[] {"MP4", "AVI", "MKV"};
        break;
      default:
        typeKeys = new String[] {"PNG", "JPG", "PDF", "ZIP"};
        break;
    }

    String selectedKey = typeKeys[random.nextInt(typeKeys.length)];
    return FILE_HEADERS.get(selectedKey);
  }

  private byte[] generateMaliciousHeader(byte[] originalHeader) {
    // 生成恶意文件头的几种方式
    int maliciousType = random.nextInt(3);

    switch (maliciousType) {
      case 0:
        // 截断文件头
        if (originalHeader.length > 2) {
          byte[] truncated = new byte[originalHeader.length - 1];
          System.arraycopy(originalHeader, 0, truncated, 0, truncated.length);
          return truncated;
        }
        break;
      case 1:
        // 修改部分字节
        byte[] modified = originalHeader.clone();
        if (modified.length > 0) {
          modified[modified.length - 1] = (byte) (modified[modified.length - 1] ^ 0xFF);
        }
        return modified;
      case 2:
        // 添加额外字节
        byte[] extended = new byte[originalHeader.length + 4];
        System.arraycopy(originalHeader, 0, extended, 0, originalHeader.length);
        for (int i = originalHeader.length; i < extended.length; i++) {
          extended[i] = (byte) random.nextInt(256);
        }
        return extended;
    }

    return originalHeader;
  }

  private String formatHeader(byte[] headerBytes, String format) {
    switch (format) {
      case "HEX":
        return bytesToHex(headerBytes);
      case "BYTES":
        return bytesToString(headerBytes);
      case "BASE64":
        return bytesToBase64(headerBytes);
      default:
        return bytesToHex(headerBytes);
    }
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder();
    for (byte b : bytes) {
      hex.append(String.format("%02X", b & 0xFF));
    }
    return hex.toString();
  }

  private String bytesToString(byte[] bytes) {
    StringBuilder str = new StringBuilder("[");
    for (int i = 0; i < bytes.length; i++) {
      if (i > 0) str.append(", ");
      str.append(bytes[i] & 0xFF);
    }
    str.append("]");
    return str.toString();
  }

  private String bytesToBase64(byte[] bytes) {
    // 简单的Base64编码实现
    StringBuilder base64 = new StringBuilder();
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    for (int i = 0; i < bytes.length; i += 3) {
      int b1 = bytes[i] & 0xFF;
      int b2 = (i + 1 < bytes.length) ? bytes[i + 1] & 0xFF : 0;
      int b3 = (i + 2 < bytes.length) ? bytes[i + 2] & 0xFF : 0;

      int combined = (b1 << 16) | (b2 << 8) | b3;

      base64.append(chars.charAt((combined >> 18) & 0x3F));
      base64.append(chars.charAt((combined >> 12) & 0x3F));
      base64.append((i + 1 < bytes.length) ? chars.charAt((combined >> 6) & 0x3F) : '=');
      base64.append((i + 2 < bytes.length) ? chars.charAt(combined & 0x3F) : '=');
    }

    return base64.toString();
  }
}
