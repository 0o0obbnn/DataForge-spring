package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * 图像文件头生成器
 *
 * <p>根据DataForge设计文档第10.1节：图像文件头 (Image File Header) 生成特定图像文件格式的魔数或文件头字节序列， 用于测试文件类型识别和安全扫描。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class ImageFileHeaderGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  // 图像文件魔数映射
  private static final Map<String, byte[]> MAGIC_NUMBERS =
      Map.of(
          "PNG",
          new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
          "JPEG",
          new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
          "GIF87A",
          "GIF87a".getBytes(StandardCharsets.US_ASCII),
          "GIF89A",
          "GIF89a".getBytes(StandardCharsets.US_ASCII),
          "BMP",
          new byte[] {0x42, 0x4D},
          "WEBP",
          "RIFF".getBytes(StandardCharsets.US_ASCII));

  @Override
  public String getType() {
    return "image_header";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    String format = config.getParam("format", String.class, "PNG");
    boolean valid = Boolean.parseBoolean(config.getParam("valid", String.class, "true"));
    int length = Integer.parseInt(config.getParam("length", String.class, "8"));

    if (!valid) {
      return generateCorruptedHeader(format, length);
    }

    byte[] magicNumber = MAGIC_NUMBERS.get(format.toUpperCase());
    if (magicNumber == null) {
      throw new IllegalArgumentException("Unsupported image format: " + format);
    }

    return Base64.getEncoder()
        .encodeToString(Arrays.copyOf(magicNumber, Math.min(length, magicNumber.length)));
  }

  private String generateCorruptedHeader(String format, int length) {
    // 生成损坏的文件头用于异常测试
    byte[] corrupted = new byte[length];
    ThreadLocalRandom.current().nextBytes(corrupted);
    return Base64.getEncoder().encodeToString(corrupted);
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String format = config.getParam("format", String.class, "PNG");
    if (!"ANY".equalsIgnoreCase(format) && !MAGIC_NUMBERS.containsKey(format.toUpperCase())) {
      return false;
    }

    return true;
  }

  @Override
  public String getDescription() {
    return "生成图像文件头魔数，支持PNG、JPEG、GIF、BMP、WEBP等格式，" + "可生成有效或损坏的文件头用于测试文件类型识别和安全扫描";
  }
}
