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
 * 视频片段生成器
 *
 * <p>根据DataForge设计文档要求，生成各种视频格式的片段用于测试视频处理系统。 支持MP4、AVI、MKV、MOV等多种视频格式。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class VideoSnippetGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(VideoSnippetGenerator.class);

  // 视频格式签名映射
  private static final Map<String, byte[]> VIDEO_SIGNATURES =
      Map.of(
          "MP4", new byte[] {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70}, // ftyp
          "AVI", new byte[] {0x52, 0x49, 0x46, 0x46}, // RIFF
          "MKV", new byte[] {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3}, // EBML
          "MOV", new byte[] {0x00, 0x00, 0x00, 0x14, 0x66, 0x74, 0x79, 0x70}, // ftyp
          "WMV", new byte[] {0x30, 0x26, (byte) 0xB2, 0x75} // ASF
          );

  @Override
  public String getType() {
    return "video";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String format = config.getParam("format", String.class, "MP4");
      int duration = Integer.parseInt(config.getParam("duration", String.class, "10")); // seconds
      int width = Integer.parseInt(config.getParam("width", String.class, "1920"));
      int height = Integer.parseInt(config.getParam("height", String.class, "1080"));
      boolean corrupted = Boolean.parseBoolean(config.getParam("corrupted", String.class, "false"));

      return generateVideo(format, duration, width, height, corrupted);

    } catch (Exception e) {
      logger.warn("Error generating video snippet: {}", e.getMessage());
      // 生成默认MP4视频
      return generateDefaultVideo();
    }
  }

  /** 生成视频片段 */
  private String generateVideo(
      String format, int duration, int width, int height, boolean corrupted) {
    byte[] signature = VIDEO_SIGNATURES.get(format.toUpperCase());
    if (signature == null) {
      logger.warn("Unsupported video format: {}, using MP4", format);
      signature = VIDEO_SIGNATURES.get("MP4");
    }

    // 计算视频数据大小（简化计算）
    // 假设每秒约1MB数据
    int dataSize = duration * 1024 * 1024;

    if (corrupted) {
      return generateCorruptedVideo(signature, dataSize);
    }

    return generateValidVideo(signature, dataSize);
  }

  /** 生成有效的视频片段 */
  private String generateValidVideo(byte[] signature, int dataSize) {
    // 创建视频数据，确保至少包含头部签名
    byte[] videoData = new byte[Math.max(dataSize, signature.length)];

    // 填充签名
    System.arraycopy(signature, 0, videoData, 0, Math.min(signature.length, videoData.length));

    // 填充视频内容
    ThreadLocalRandom.current().nextBytes(videoData);

    return Base64.getEncoder().encodeToString(videoData);
  }

  /** 生成损坏的视频片段 */
  private String generateCorruptedVideo(byte[] signature, int dataSize) {
    byte[] corruptedData = new byte[dataSize];

    // 添加部分签名以模拟损坏的文件
    int signatureLength = Math.min(signature.length, dataSize / 4);
    if (signatureLength > 0) {
      System.arraycopy(signature, 0, corruptedData, 0, signatureLength);
    }

    // 填充随机数据以模拟损坏
    ThreadLocalRandom.current().nextBytes(corruptedData);

    // 在随机位置插入无效字节
    for (int i = 0; i < corruptedData.length / 10; i++) {
      corruptedData[ThreadLocalRandom.current().nextInt(corruptedData.length)] = (byte) 0xFF;
    }

    return Base64.getEncoder().encodeToString(corruptedData);
  }

  /** 生成默认MP4视频 */
  private String generateDefaultVideo() {
    byte[] signature = VIDEO_SIGNATURES.get("MP4");
    byte[] videoData = new byte[10 * 1024 * 1024]; // 10MB视频数据
    System.arraycopy(signature, 0, videoData, 0, signature.length);
    ThreadLocalRandom.current().nextBytes(videoData);
    return Base64.getEncoder().encodeToString(videoData);
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String format = config.getParam("format", String.class, "MP4");
    int duration = Integer.parseInt(config.getParam("duration", String.class, "10"));
    int width = Integer.parseInt(config.getParam("width", String.class, "1920"));
    int height = Integer.parseInt(config.getParam("height", String.class, "1080"));

    // 验证格式
    if (!VIDEO_SIGNATURES.containsKey(format.toUpperCase())) {
      return false;
    }

    // 验证持续时间（1秒到3600秒）
    if (duration < 1 || duration > 3600) {
      return false;
    }

    // 验证分辨率（最小100x100，最大8K）
    if (width < 100 || width > 7680) {
      return false;
    }

    return height >= 100 && height <= 4320;
  }

  @Override
  public String getDescription() {
    return "生成视频片段，支持MP4、AVI、MKV、MOV、WMV格式，" + "可生成有效或损坏的视频用于异常测试，支持配置持续时间和分辨率";
  }
}
