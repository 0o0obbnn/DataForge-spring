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
 * 音频片段生成器
 *
 * <p>根据DataForge设计文档要求，生成各种音频格式的片段用于测试音频处理系统。 支持WAV、MP3、FLAC、AAC等多种音频格式。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class AudioSnippetGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(AudioSnippetGenerator.class);

  // 音频格式签名映射
  private static final Map<String, byte[]> AUDIO_SIGNATURES =
      Map.of(
          "WAV", new byte[] {0x52, 0x49, 0x46, 0x46}, // RIFF
          "MP3", new byte[] {0x49, 0x44, 0x33}, // ID3
          "FLAC", new byte[] {0x66, 0x4C, 0x61, 0x43}, // fLaC
          "AAC", new byte[] {(byte) 0xFF, (byte) 0xF1}, // ADTS header
          "OGG", new byte[] {0x4F, 0x67, 0x67, 0x53} // OggS
          );

  @Override
  public String getType() {
    return "audio";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String format = config.getParam("format", String.class, "WAV");
      int duration = Integer.parseInt(config.getParam("duration", String.class, "10")); // seconds
      int sampleRate = Integer.parseInt(config.getParam("sampleRate", String.class, "44100")); // Hz
      boolean corrupted = Boolean.parseBoolean(config.getParam("corrupted", String.class, "false"));

      return generateAudio(format, duration, sampleRate, corrupted);

    } catch (Exception e) {
      logger.warn("Error generating audio snippet: {}", e.getMessage());
      // 生成默认WAV音频
      return generateDefaultAudio();
    }
  }

  /** 生成音频片段 */
  private String generateAudio(String format, int duration, int sampleRate, boolean corrupted) {
    byte[] signature = AUDIO_SIGNATURES.get(format.toUpperCase());
    if (signature == null) {
      logger.warn("Unsupported audio format: {}, using WAV", format);
      signature = AUDIO_SIGNATURES.get("WAV");
    }

    // 计算音频数据大小（简化计算）
    // 假设16位立体声（2通道）
    int dataSize = duration * sampleRate * 2 * 2; // seconds * samples/sec * bytes/sample * channels

    if (corrupted) {
      return generateCorruptedAudio(signature, dataSize);
    }

    return generateValidAudio(signature, dataSize);
  }

  /** 生成有效的音频片段 */
  private String generateValidAudio(byte[] signature, int dataSize) {
    // 创建音频数据，确保至少包含头部签名
    byte[] audioData = new byte[Math.max(dataSize, signature.length)];

    // 填充签名
    System.arraycopy(signature, 0, audioData, 0, Math.min(signature.length, audioData.length));

    // 填充音频内容
    ThreadLocalRandom.current().nextBytes(audioData);

    return Base64.getEncoder().encodeToString(audioData);
  }

  /** 生成损坏的音频片段 */
  private String generateCorruptedAudio(byte[] signature, int dataSize) {
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

  /** 生成默认WAV音频 */
  private String generateDefaultAudio() {
    byte[] signature = AUDIO_SIGNATURES.get("WAV");
    byte[] audioData = new byte[44100 * 2 * 2]; // 1秒的16位立体声数据
    System.arraycopy(signature, 0, audioData, 0, signature.length);
    ThreadLocalRandom.current().nextBytes(audioData);
    return Base64.getEncoder().encodeToString(audioData);
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String format = config.getParam("format", String.class, "WAV");
    int duration = Integer.parseInt(config.getParam("duration", String.class, "10"));
    int sampleRate = Integer.parseInt(config.getParam("sampleRate", String.class, "44100"));

    // 验证格式
    if (!AUDIO_SIGNATURES.containsKey(format.toUpperCase())) {
      return false;
    }

    // 验证持续时间（1秒到3600秒）
    if (duration < 1 || duration > 3600) {
      return false;
    }

    // 验证采样率（8000Hz到192000Hz）
    return sampleRate >= 8000 && sampleRate <= 192000;
  }

  @Override
  public String getDescription() {
    return "生成音频片段，支持WAV、MP3、FLAC、AAC、OGG格式，" + "可生成有效或损坏的音频用于异常测试，支持配置持续时间和采样率";
  }
}
