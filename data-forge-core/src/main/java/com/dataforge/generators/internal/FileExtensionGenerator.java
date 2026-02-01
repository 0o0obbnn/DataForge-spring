package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文件扩展名生成器
 *
 * <p>根据DataForge设计文档要求，生成各种文件类型的扩展名用于测试文件类型处理系统。 支持常见文件类型如文档、图片、音频、视频、压缩包、可执行文件等。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class FileExtensionGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(FileExtensionGenerator.class);

  // 文档类文件扩展名
  private static final List<String> DOCUMENT_EXTENSIONS =
      Arrays.asList(
          "txt", "doc", "docx", "pdf", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf",
          "csv", "xml", "json", "yaml", "yml");

  // 图片类文件扩展名
  private static final List<String> IMAGE_EXTENSIONS =
      Arrays.asList(
          "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "svg", "ico", "psd", "raw",
          "cr2", "nef", "arw");

  // 音频类文件扩展名
  private static final List<String> AUDIO_EXTENSIONS =
      Arrays.asList(
          "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus", "aiff", "au", "mid", "midi",
          "m3u", "pls");

  // 视频类文件扩展名
  private static final List<String> VIDEO_EXTENSIONS =
      Arrays.asList(
          "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp", "ogv",
          "vob", "rm", "rmvb");

  // 压缩包类文件扩展名
  private static final List<String> ARCHIVE_EXTENSIONS =
      Arrays.asList(
          "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz", "iso", "cab", "ace", "arj");

  // 可执行文件扩展名
  private static final List<String> EXECUTABLE_EXTENSIONS =
      Arrays.asList(
          "exe", "msi", "bat", "cmd", "sh", "bin", "app", "jar", "deb", "rpm", "apk", "dmg");

  // 系统文件扩展名
  private static final List<String> SYSTEM_EXTENSIONS =
      Arrays.asList(
          "dll", "sys", "ini", "cfg", "log", "tmp", "temp", "bak", "config", "dat", "db", "sqlite");

  // 所有文件扩展名集合
  private static final List<String> ALL_EXTENSIONS =
      Arrays.asList(
          "txt", "doc", "docx", "pdf", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf",
          "csv", "xml", "json", "yaml", "yml", "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif",
          "webp", "svg", "ico", "psd", "raw", "cr2", "nef", "arw", "mp3", "wav", "flac", "aac",
          "ogg", "wma", "m4a", "opus", "aiff", "au", "mid", "midi", "m3u", "pls", "mp4", "avi",
          "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp", "ogv", "vob", "rm",
          "rmvb", "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz", "iso", "cab", "ace", "arj",
          "exe", "msi", "bat", "cmd", "sh", "bin", "app", "jar", "deb", "rpm", "apk", "dmg", "dll",
          "sys", "ini", "cfg", "log", "tmp", "temp", "bak", "config", "dat", "db", "sqlite");

  @Override
  public String getType() {
    return "file_extension";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String category = config.getParam("category", String.class, "ALL");
      boolean uppercase = Boolean.parseBoolean(config.getParam("uppercase", String.class, "false"));
      double invalidProbability =
          Double.parseDouble(config.getParam("invalidProbability", String.class, "0.0"));

      // 根据概率生成无效扩展名
      if (ThreadLocalRandom.current().nextDouble() < invalidProbability) {
        return generateInvalidExtension();
      }

      return generateValidExtension(category, uppercase);

    } catch (Exception e) {
      logger.warn("Error generating file extension: {}", e.getMessage());
      // 生成默认扩展名
      return generateDefaultExtension();
    }
  }

  /** 生成有效的文件扩展名 */
  private String generateValidExtension(String category, boolean uppercase) {
    List<String> extensions;

    switch (category.toUpperCase()) {
      case "DOCUMENT":
        extensions = DOCUMENT_EXTENSIONS;
        break;
      case "IMAGE":
        extensions = IMAGE_EXTENSIONS;
        break;
      case "AUDIO":
        extensions = AUDIO_EXTENSIONS;
        break;
      case "VIDEO":
        extensions = VIDEO_EXTENSIONS;
        break;
      case "ARCHIVE":
        extensions = ARCHIVE_EXTENSIONS;
        break;
      case "EXECUTABLE":
        extensions = EXECUTABLE_EXTENSIONS;
        break;
      case "SYSTEM":
        extensions = SYSTEM_EXTENSIONS;
        break;
      case "ALL":
      default:
        extensions = ALL_EXTENSIONS;
        break;
    }

    String extension = extensions.get(ThreadLocalRandom.current().nextInt(extensions.size()));
    return uppercase ? extension.toUpperCase() : extension.toLowerCase();
  }

  /** 生成无效的文件扩展名（用于异常测试） */
  private String generateInvalidExtension() {
    String[] invalidExtensions = {
      "", " ", "..", "...", "exe.", ".exe.", "ex e", "ex\t", "ex\n", "exe1", "exe0", "exe!", "exe@",
      "exe#", "exe$", "exe%", "exe^", "exe&", "exe*", "exe(", "exe)", "exe-", "exe_", "exe=",
      "exe+", "con", "prn", "aux", "nul", // Windows保留文件名
      "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2",
      "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
    };

    return invalidExtensions[ThreadLocalRandom.current().nextInt(invalidExtensions.length)];
  }

  /** 生成默认文件扩展名 */
  private String generateDefaultExtension() {
    return ALL_EXTENSIONS.get(ThreadLocalRandom.current().nextInt(ALL_EXTENSIONS.size()));
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String category = config.getParam("category", String.class, "ALL");
    double invalidProbability =
        Double.parseDouble(config.getParam("invalidProbability", String.class, "0.0"));

    // 验证类别
    String[] validCategories = {
      "DOCUMENT", "IMAGE", "AUDIO", "VIDEO", "ARCHIVE", "EXECUTABLE", "SYSTEM", "ALL"
    };
    boolean validCategory = false;
    for (String validCat : validCategories) {
      if (validCat.equalsIgnoreCase(category)) {
        validCategory = true;
        break;
      }
    }

    if (!validCategory) {
      return false;
    }

    // 验证无效概率（0.0到1.0之间）
    return invalidProbability >= 0.0 && invalidProbability <= 1.0;
  }

  @Override
  public String getDescription() {
    return "生成文件扩展名，支持文档、图片、音频、视频、压缩包、可执行文件等类型，" + "可生成有效或无效扩展名用于异常测试，支持大小写配置";
  }
}
