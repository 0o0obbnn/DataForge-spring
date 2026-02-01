package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 压缩包文件生成器
 *
 * <p>根据DataForge设计文档要求实现ZIP、TAR、7z、RAR格式的压缩包文件头生成， 用于测试文件类型识别和安全扫描。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class ArchiveFileGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(ArchiveFileGenerator.class);

  // 压缩包格式签名映射
  private static final Map<String, byte[]> ARCHIVE_SIGNATURES =
      Map.of(
          "ZIP", new byte[] {0x50, 0x4B, 0x03, 0x04},
          "TAR", new byte[0], // TAR使用512字节头部，没有固定签名
          "7Z", new byte[] {0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C},
          "RAR", new byte[] {0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00});

  @Override
  public String getType() {
    return "archive";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String format = config.getParam("format", String.class, "ZIP");
      int length = Integer.parseInt(config.getParam("length", String.class, "512"));
      boolean corrupted = Boolean.parseBoolean(config.getParam("corrupted", String.class, "false"));
      boolean nested = Boolean.parseBoolean(config.getParam("nested", String.class, "false"));

      return generateArchive(format, length, corrupted, nested);

    } catch (Exception e) {
      logger.warn("Error generating archive: {}", e.getMessage());
      // 生成默认ZIP压缩包
      return generateDefaultArchive();
    }
  }

  /** 生成压缩包 */
  private String generateArchive(String format, int length, boolean corrupted, boolean nested) {
    byte[] signature = ARCHIVE_SIGNATURES.get(format.toUpperCase());
    if (signature == null) {
      logger.warn("Unsupported archive format: {}, using ZIP", format);
      signature = ARCHIVE_SIGNATURES.get("ZIP");
    }

    if (corrupted) {
      return generateCorruptedArchive(signature, length);
    }

    return generateValidArchive(signature, length, nested);
  }

  /** 生成有效的压缩包 */
  private String generateValidArchive(byte[] signature, int length, boolean nested) {
    byte[] archiveData = new byte[length];

    // 填充签名
    if (signature.length > 0) {
      System.arraycopy(
          signature, 0, archiveData, 0, Math.min(signature.length, archiveData.length));
    }

    // 对于TAR格式，使用512字节头部
    if (signature.length == 0) { // TAR
      // 填充TAR头部（简化版）
      for (int i = 0; i < Math.min(512, length); i++) {
        archiveData[i] = (byte) (i % 256);
      }
    }

    // 如果是嵌套压缩包，添加嵌套标识
    if (nested) {
      String nestedMarker = "NESTED_ARCHIVE";
      byte[] markerBytes = nestedMarker.getBytes();
      int markerPosition = Math.max(signature.length, 100);
      if (markerPosition + markerBytes.length < length) {
        System.arraycopy(markerBytes, 0, archiveData, markerPosition, markerBytes.length);
      }
    }

    // 填充剩余内容
    ThreadLocalRandom.current().nextBytes(archiveData);

    return Base64.getEncoder().encodeToString(archiveData);
  }

  /** 生成损坏的压缩包 */
  private String generateCorruptedArchive(byte[] signature, int length) {
    byte[] corruptedData = new byte[length];

    // 添加部分签名以模拟损坏的文件
    int signatureLength = Math.min(signature.length, length / 2);
    if (signatureLength > 0) {
      System.arraycopy(signature, 0, corruptedData, 0, signatureLength);
    }

    // 填充随机数据以模拟损坏
    ThreadLocalRandom.current().nextBytes(corruptedData);

    return Base64.getEncoder().encodeToString(corruptedData);
  }

  /** 生成默认ZIP压缩包 */
  private String generateDefaultArchive() {
    byte[] signature = ARCHIVE_SIGNATURES.get("ZIP");
    byte[] archiveData = new byte[512];
    System.arraycopy(signature, 0, archiveData, 0, signature.length);
    ThreadLocalRandom.current().nextBytes(archiveData);
    return Base64.getEncoder().encodeToString(archiveData);
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String format = config.getParam("format", String.class, "ZIP");
    int length = Integer.parseInt(config.getParam("length", String.class, "512"));

    // 验证格式
    if (!ARCHIVE_SIGNATURES.containsKey(format.toUpperCase())) {
      return false;
    }

    // 验证长度
    return length > 0 && length <= 10 * 1024 * 1024; // 最大10MB
  }

  @Override
  public String getDescription() {
    return "生成压缩包文件头，支持ZIP、TAR、7z、RAR格式，" + "可生成有效或损坏的压缩包用于异常测试，支持嵌套压缩包模拟";
  }
}
