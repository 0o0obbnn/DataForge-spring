package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MIME类型生成器
 *
 * <p>支持的参数： - category: MIME类型类别 (TEXT|IMAGE|APPLICATION|AUDIO|VIDEO|ANY) - subtype: 子类型
 * (指定具体子类型或随机) - charset: 字符集 (仅对text类型有效) - include_parameters: 是否包含参数 (true|false) -
 * custom_types: 自定义MIME类型列表
 *
 * @author DataForge
 */
public class MimeTypeGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(MimeTypeGenerator.class);
  private static final Random random = new Random();

  // MIME类型类别枚举
  private enum MimeCategory {
    TEXT,
    IMAGE,
    APPLICATION,
    AUDIO,
    VIDEO,
    MULTIPART,
    MESSAGE,
    MODEL
  }

  // MIME类型映射
  private static final Map<MimeCategory, List<String>> MIME_TYPES = new HashMap<>();

  // 常见字符集
  private static final List<String> CHARSETS =
      Arrays.asList(
          "UTF-8",
          "UTF-16",
          "UTF-32",
          "ISO-8859-1",
          "ISO-8859-15",
          "Windows-1252",
          "ASCII",
          "US-ASCII",
          "GB2312",
          "GBK",
          "GB18030",
          "Big5",
          "Shift_JIS",
          "EUC-JP");

  // 常见参数
  private static final Map<String, List<String>> MIME_PARAMETERS = new HashMap<>();

  static {
    initializeMimeTypes();
    initializeMimeParameters();
  }

  private static void initializeMimeTypes() {
    // Text类型
    MIME_TYPES.put(
        MimeCategory.TEXT,
        Arrays.asList(
            "plain",
            "html",
            "css",
            "javascript",
            "xml",
            "json",
            "csv",
            "markdown",
            "rtf",
            "calendar",
            "vcard",
            "yaml",
            "x-python",
            "x-java-source",
            "x-c",
            "x-shellscript",
            "x-sql",
            "x-log",
            "x-diff",
            "x-patch"));

    // Image类型
    MIME_TYPES.put(
        MimeCategory.IMAGE,
        Arrays.asList(
            "jpeg",
            "png",
            "gif",
            "webp",
            "svg+xml",
            "bmp",
            "tiff",
            "x-icon",
            "x-ms-bmp",
            "vnd.adobe.photoshop",
            "x-portable-pixmap",
            "x-portable-graymap",
            "x-portable-bitmap",
            "x-xbitmap",
            "x-xpixmap",
            "avif",
            "heic",
            "heif"));

    // Application类型
    MIME_TYPES.put(
        MimeCategory.APPLICATION,
        Arrays.asList(
            "json",
            "xml",
            "pdf",
            "zip",
            "gzip",
            "x-tar",
            "x-rar-compressed",
            "x-7z-compressed",
            "octet-stream",
            "x-executable",
            "x-sharedlib",
            "x-msdownload",
            "vnd.ms-excel",
            "vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "vnd.ms-powerpoint",
            "vnd.openxmlformats-officedocument.presentationml.presentation",
            "msword",
            "vnd.openxmlformats-officedocument.wordprocessingml.document",
            "x-www-form-urlencoded",
            "x-javascript",
            "x-httpd-php",
            "x-python-code",
            "java-archive",
            "x-java-serialized-object",
            "x-java-vm"));

    // Audio类型
    MIME_TYPES.put(
        MimeCategory.AUDIO,
        Arrays.asList(
            "mpeg",
            "wav",
            "x-wav",
            "ogg",
            "flac",
            "aac",
            "x-ms-wma",
            "x-m4a",
            "webm",
            "x-aiff",
            "x-au",
            "x-pn-realaudio",
            "x-pn-realaudio-plugin",
            "vnd.rn-realaudio",
            "x-realaudio",
            "basic",
            "midi",
            "x-midi"));

    // Video类型
    MIME_TYPES.put(
        MimeCategory.VIDEO,
        Arrays.asList(
            "mp4",
            "mpeg",
            "quicktime",
            "x-msvideo",
            "x-ms-wmv",
            "webm",
            "ogg",
            "x-flv",
            "3gpp",
            "x-matroska",
            "x-ms-asf",
            "x-dv",
            "x-sgi-movie",
            "vnd.rn-realvideo",
            "x-pn-realvideo",
            "x-pn-realvideo-plugin"));

    // Multipart类型
    MIME_TYPES.put(
        MimeCategory.MULTIPART,
        Arrays.asList(
            "form-data",
            "byteranges",
            "alternative",
            "digest",
            "parallel",
            "related",
            "report",
            "signed",
            "encrypted",
            "mixed"));

    // Message类型
    MIME_TYPES.put(
        MimeCategory.MESSAGE,
        Arrays.asList(
            "rfc822",
            "partial",
            "external-body",
            "news",
            "http",
            "s-http",
            "imdn+xml",
            "tracking-status",
            "global",
            "global-headers",
            "global-disposition-notification",
            "global-delivery-status"));

    // Model类型
    MIME_TYPES.put(
        MimeCategory.MODEL,
        Arrays.asList(
            "iges",
            "mesh",
            "vrml",
            "x3d+binary",
            "x3d+fastinfoset",
            "x3d+vrml",
            "x3d+xml",
            "x3d-vrml",
            "gltf+json",
            "gltf-binary",
            "stl",
            "obj"));
  }

  private static void initializeMimeParameters() {
    // Text类型参数
    MIME_PARAMETERS.put("text", Arrays.asList("charset", "boundary", "format"));

    // Application类型参数
    MIME_PARAMETERS.put("application", Arrays.asList("charset", "boundary", "name", "filename"));

    // Multipart类型参数
    MIME_PARAMETERS.put("multipart", Arrays.asList("boundary", "start", "start-info", "type"));

    // Message类型参数
    MIME_PARAMETERS.put("message", Arrays.asList("boundary", "charset"));
  }

  @Override
  public String getType() {
    return "mimetype";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String category = config.getParam("category", String.class, "ANY");
      String subtype = config.getParam("subtype", String.class, null);
      String charset = config.getParam("charset", String.class, null);
      boolean includeParameters =
          Boolean.parseBoolean(config.getParam("include_parameters", String.class, "false"));
      String customTypes = config.getParam("custom_types", String.class, null);

      // 生成MIME类型
      String mimeType =
          generateMimeType(category, subtype, charset, includeParameters, customTypes);

      // 将MIME类型信息存入上下文
      context.put("mime_type", mimeType);
      context.put("mime_category", extractCategory(mimeType));
      context.put("mime_subtype", extractSubtype(mimeType));

      logger.debug("Generated MIME type: {}", mimeType);
      return mimeType;

    } catch (Exception e) {
      logger.error("Error generating MIME type", e);
      return "text/plain";
    }
  }

  private String generateMimeType(
      String category,
      String subtype,
      String charset,
      boolean includeParameters,
      String customTypes) {

    // 处理自定义类型
    if (customTypes != null && !customTypes.isEmpty()) {
      String[] types = customTypes.split(",");
      return types[random.nextInt(types.length)].trim();
    }

    // 确定类别
    MimeCategory mimeCategory = determineMimeCategory(category);

    // 确定子类型
    String finalSubtype = subtype != null ? subtype : selectRandomSubtype(mimeCategory);

    // 构建基本MIME类型
    String baseMimeType = mimeCategory.name().toLowerCase() + "/" + finalSubtype;

    // 添加参数
    if (includeParameters) {
      String parameters = generateParameters(mimeCategory, charset);
      if (!parameters.isEmpty()) {
        baseMimeType += "; " + parameters;
      }
    }

    return baseMimeType;
  }

  private MimeCategory determineMimeCategory(String category) {
    if ("ANY".equalsIgnoreCase(category)) {
      MimeCategory[] categories = MimeCategory.values();
      return categories[random.nextInt(categories.length)];
    }

    try {
      return MimeCategory.valueOf(category.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Unknown MIME category: {}. Using TEXT.", category);
      return MimeCategory.TEXT;
    }
  }

  private String selectRandomSubtype(MimeCategory category) {
    List<String> subtypes = MIME_TYPES.get(category);
    if (subtypes == null || subtypes.isEmpty()) {
      return "plain";
    }
    return subtypes.get(random.nextInt(subtypes.size()));
  }

  private String generateParameters(MimeCategory category, String charset) {
    List<String> parameters = new ArrayList<>();

    // 添加字符集参数（主要用于text类型）
    if (category == MimeCategory.TEXT || category == MimeCategory.APPLICATION) {
      String finalCharset = charset != null ? charset : selectRandomCharset();
      if (random.nextDouble() < 0.7) { // 70%概率添加charset
        parameters.add("charset=" + finalCharset);
      }
    }

    // 添加其他参数
    List<String> availableParams = MIME_PARAMETERS.get(category.name().toLowerCase());
    if (availableParams != null && random.nextDouble() < 0.3) { // 30%概率添加其他参数
      String param = availableParams.get(random.nextInt(availableParams.size()));

      if (!"charset".equals(param)) { // 避免重复添加charset
        String value = generateParameterValue(param);
        parameters.add(param + "=" + value);
      }
    }

    return String.join("; ", parameters);
  }

  private String selectRandomCharset() {
    return CHARSETS.get(random.nextInt(CHARSETS.size()));
  }

  private String generateParameterValue(String parameter) {
    switch (parameter) {
      case "boundary":
        return "----WebKitFormBoundary" + generateRandomString(16);

      case "name":
      case "filename":
        return "\"file" + random.nextInt(1000) + ".dat\"";

      case "format":
        return random.nextBoolean() ? "fixed" : "flowed";

      case "start":
        return "<part1@example.com>";

      case "start-info":
        return "text/html";

      case "type":
        return "text/html";

      default:
        return "value" + random.nextInt(100);
    }
  }

  private String generateRandomString(int length) {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < length; i++) {
      result.append(chars.charAt(random.nextInt(chars.length())));
    }

    return result.toString();
  }

  private String extractCategory(String mimeType) {
    int slashIndex = mimeType.indexOf('/');
    if (slashIndex > 0) {
      return mimeType.substring(0, slashIndex);
    }
    return "unknown";
  }

  private String extractSubtype(String mimeType) {
    int slashIndex = mimeType.indexOf('/');
    int semicolonIndex = mimeType.indexOf(';');

    if (slashIndex > 0) {
      int endIndex = semicolonIndex > slashIndex ? semicolonIndex : mimeType.length();
      return mimeType.substring(slashIndex + 1, endIndex);
    }

    return "unknown";
  }

  /** 根据文件扩展名生成对应的MIME类型 */
  public String generateMimeTypeByExtension(String extension) {
    if (extension == null || extension.isEmpty()) {
      return "application/octet-stream";
    }

    extension = extension.toLowerCase();

    // 常见扩展名到MIME类型的映射
    Map<String, String> extensionMimeMap = new HashMap<>();
    extensionMimeMap.put("txt", "text/plain");
    extensionMimeMap.put("html", "text/html");
    extensionMimeMap.put("css", "text/css");
    extensionMimeMap.put("js", "text/javascript");
    extensionMimeMap.put("json", "application/json");
    extensionMimeMap.put("xml", "application/xml");
    extensionMimeMap.put("pdf", "application/pdf");
    extensionMimeMap.put("zip", "application/zip");
    extensionMimeMap.put("jpg", "image/jpeg");
    extensionMimeMap.put("jpeg", "image/jpeg");
    extensionMimeMap.put("png", "image/png");
    extensionMimeMap.put("gif", "image/gif");
    extensionMimeMap.put("svg", "image/svg+xml");
    extensionMimeMap.put("mp3", "audio/mpeg");
    extensionMimeMap.put("wav", "audio/wav");
    extensionMimeMap.put("mp4", "video/mp4");
    extensionMimeMap.put("avi", "video/x-msvideo");

    return extensionMimeMap.getOrDefault(extension, "application/octet-stream");
  }

  /** 验证MIME类型格式 */
  public boolean validateMimeType(String mimeType) {
    if (mimeType == null || mimeType.isEmpty()) {
      return false;
    }

    // 基本格式检查：type/subtype
    String[] parts = mimeType.split(";")[0].split("/");
    if (parts.length != 2) {
      return false;
    }

    String type = parts[0].trim();
    String subtype = parts[1].trim();

    // 检查是否为空
    if (type.isEmpty() || subtype.isEmpty()) {
      return false;
    }

    // 检查字符是否合法（简化版本）
    return type.matches("[a-zA-Z][a-zA-Z0-9]*[a-zA-Z0-9\\-]*")
        && subtype.matches("[a-zA-Z0-9][a-zA-Z0-9\\-+.]*");
  }

  /** 生成Web安全的MIME类型（排除可执行类型） */
  public String generateWebSafeMimeType() {
    MimeCategory[] safeCategories = {
      MimeCategory.TEXT, MimeCategory.IMAGE, MimeCategory.AUDIO, MimeCategory.VIDEO
    };
    MimeCategory category = safeCategories[random.nextInt(safeCategories.length)];

    String subtype = selectRandomSubtype(category);
    return category.name().toLowerCase() + "/" + subtype;
  }

  /** 生成恶意MIME类型（用于安全测试） */
  public String generateMaliciousMimeType() {
    String[] maliciousTypes = {
      "application/x-executable",
      "application/x-msdownload",
      "application/x-msdos-program",
      "application/x-winexe",
      "application/x-javascript",
      "text/javascript",
      "application/javascript",
      "text/html",
      "application/x-httpd-php"
    };

    return maliciousTypes[random.nextInt(maliciousTypes.length)];
  }
}
