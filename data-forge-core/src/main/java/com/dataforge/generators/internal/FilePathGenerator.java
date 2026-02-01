package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件路径生成器
 *
 * <p>支持的参数： - os: 操作系统 (WINDOWS|UNIX|MAC|ANY) - type: 路径类型 (ABSOLUTE|RELATIVE|UNC) - depth: 目录深度
 * (1-10) - include_filename: 是否包含文件名 (true|false) - extension: 文件扩展名 (如 "txt", "jpg", "ANY") -
 * include_spaces: 是否包含空格 (true|false) - include_special_chars: 是否包含特殊字符 (true|false) -
 * drive_letter: Windows驱动器盘符 (A-Z)
 *
 * @author DataForge
 */
public class FilePathGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(FilePathGenerator.class);
  private static final Random random = new Random();

  private static final String DEFAULT_CONFIG_FILE = "data/file-paths.yml";

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  /** 配置数据（从文件加载） */
  private volatile FilePathConfig filePathConfig;

  // 操作系统类型枚举
  private enum OSType {
    WINDOWS,
    UNIX,
    MAC
  }

  // 路径类型枚举
  private enum PathType {
    ABSOLUTE, // 绝对路径
    RELATIVE, // 相对路径
    UNC // Windows UNC路径
  }

  // 常见目录名
  private static final List<String> COMMON_DIRECTORIES =
      Arrays.asList(
          "home",
          "user",
          "users",
          "documents",
          "downloads",
          "desktop",
          "pictures",
          "music",
          "videos",
          "projects",
          "workspace",
          "src",
          "source",
          "code",
          "dev",
          "development",
          "test",
          "tests",
          "data",
          "files",
          "temp",
          "tmp",
          "cache",
          "logs",
          "log",
          "config",
          "configuration",
          "bin",
          "lib",
          "libs",
          "include",
          "share",
          "var",
          "opt",
          "usr",
          "etc",
          "boot",
          "root",
          "program files",
          "program files (x86)",
          "windows",
          "system32",
          "appdata",
          "local",
          "roaming",
          "public",
          "all users",
          "default",
          "profiles",
          "application data");

  // 常见文件名
  private static final List<String> COMMON_FILENAMES =
      Arrays.asList(
          "index",
          "main",
          "app",
          "application",
          "program",
          "readme",
          "license",
          "changelog",
          "config",
          "configuration",
          "settings",
          "preferences",
          "options",
          "data",
          "database",
          "log",
          "error",
          "debug",
          "info",
          "test",
          "example",
          "sample",
          "demo",
          "template",
          "backup",
          "archive",
          "export",
          "import",
          "report",
          "document",
          "file",
          "image",
          "photo",
          "picture",
          "video",
          "audio",
          "music",
          "sound",
          "text",
          "note",
          "memo");

  // 常见文件扩展名
  private static final Map<String, List<String>> FILE_EXTENSIONS = new HashMap<>();

  // Windows驱动器盘符
  private static final List<String> DRIVE_LETTERS =
      Arrays.asList(
          "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
          "U", "V", "W", "X", "Y", "Z");

  // 特殊字符（用于测试）
  private static final String SPECIAL_CHARS = "!@#$%^&()_+-=[]{}|;':\",./<>?`~";

  static {
    initializeFileExtensions();
  }

  private static void initializeFileExtensions() {
    FILE_EXTENSIONS.put(
        "DOCUMENT", Arrays.asList("txt", "doc", "docx", "pdf", "rtf", "odt", "pages"));
    FILE_EXTENSIONS.put(
        "IMAGE", Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "tiff", "svg", "webp"));
    FILE_EXTENSIONS.put(
        "VIDEO", Arrays.asList("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v"));
    FILE_EXTENSIONS.put("AUDIO", Arrays.asList("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a"));
    FILE_EXTENSIONS.put("ARCHIVE", Arrays.asList("zip", "rar", "7z", "tar", "gz", "bz2", "xz"));
    FILE_EXTENSIONS.put(
        "CODE",
        Arrays.asList("java", "py", "js", "html", "css", "cpp", "c", "h", "php", "rb", "go", "rs"));
    FILE_EXTENSIONS.put(
        "DATA", Arrays.asList("json", "xml", "csv", "sql", "db", "sqlite", "xlsx", "xls"));
    FILE_EXTENSIONS.put(
        "CONFIG", Arrays.asList("conf", "cfg", "ini", "properties", "yaml", "yml", "toml"));
    FILE_EXTENSIONS.put(
        "EXECUTABLE", Arrays.asList("exe", "msi", "dmg", "pkg", "deb", "rpm", "appimage"));
    FILE_EXTENSIONS.put(
        "WEB", Arrays.asList("html", "htm", "css", "js", "php", "asp", "jsp", "xml"));
  }

  @Override
  public String getType() {
    return "filepath";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      ensureConfigLoaded(config);

      String os = config.getParam("os", String.class, "ANY");
      String type = config.getParam("type", String.class, "ABSOLUTE");
      int depth = Integer.parseInt(config.getParam("depth", String.class, "3"));
      boolean includeFilename =
          Boolean.parseBoolean(config.getParam("include_filename", String.class, "true"));
      String extension = config.getParam("extension", String.class, "ANY");
      boolean includeSpaces =
          Boolean.parseBoolean(config.getParam("include_spaces", String.class, "false"));
      boolean includeSpecialChars =
          Boolean.parseBoolean(config.getParam("include_special_chars", String.class, "false"));
      String driveLetter = config.getParam("drive_letter", String.class, null);

      String filePath =
          generateFilePath(
              os,
              type,
              depth,
              includeFilename,
              extension,
              includeSpaces,
              includeSpecialChars,
              driveLetter);

      context.put("file_path", filePath);
      context.put("file_os", determineOS(os).name());
      context.put("file_extension", extractExtension(filePath));
      context.put("file_directory", extractDirectory(filePath));
      context.put("file_name", extractFilename(filePath));

      logger.debug("Generated file path: {}", filePath);
      return filePath;
    } catch (Exception e) {
      logger.error("Error generating file path", e);
      return "/home/user/documents/file.txt";
    }
  }

  private String generateFilePath(
      String os,
      String type,
      int depth,
      boolean includeFilename,
      String extension,
      boolean includeSpaces,
      boolean includeSpecialChars,
      String driveLetter) {

    OSType osType = determineOS(os);
    PathType pathType = determinePathType(type);

    StringBuilder path = new StringBuilder();

    // 生成路径前缀
    generatePathPrefix(path, osType, pathType, driveLetter);

    // 生成目录结构
    generateDirectoryStructure(path, osType, depth, includeSpaces, includeSpecialChars);

    // 生成文件名
    if (includeFilename) {
      generateFilename(path, osType, extension, includeSpaces, includeSpecialChars);
    }

    return path.toString();
  }

  private OSType determineOS(String os) {
    switch (os.toUpperCase()) {
      case "WINDOWS":
        return OSType.WINDOWS;
      case "UNIX":
      case "LINUX":
        return OSType.UNIX;
      case "MAC":
      case "MACOS":
        return OSType.MAC;
      case "ANY":
      default:
        OSType[] types = OSType.values();
        return types[random.nextInt(types.length)];
    }
  }

  private PathType determinePathType(String type) {
    try {
      return PathType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Unknown path type: {}. Using ABSOLUTE.", type);
      return PathType.ABSOLUTE;
    }
  }

  private void generatePathPrefix(
      StringBuilder path, OSType osType, PathType pathType, String driveLetter) {
    String osName = osType.name().toLowerCase();
    if (filePathConfig != null && filePathConfig.getDirectories() != null) {
      Map<String, DirectoryConfig> dirConfigs = filePathConfig.getDirectories();
      DirectoryConfig dirConfig = dirConfigs.get(osName);
      if (dirConfig != null
          && dirConfig.getDirectories() != null
          && !dirConfig.getDirectories().isEmpty()) {
        List<String> dirs = dirConfig.getDirectories();
        path.append(dirs.get(random.nextInt(dirs.size())));
        return;
      }
    }

    switch (pathType) {
      case ABSOLUTE:
        generateAbsolutePrefix(path, osType, driveLetter);
        break;
      case RELATIVE:
        generateRelativePrefix(path, osType);
        break;
      case UNC:
        if (osType == OSType.WINDOWS) {
          generateUNCPrefix(path);
        } else {
          generateAbsolutePrefix(path, osType, driveLetter);
        }
        break;
    }
  }

  private void generateAbsolutePrefix(StringBuilder path, OSType osType, String driveLetter) {
    switch (osType) {
      case WINDOWS:
        String drive =
            driveLetter != null
                ? driveLetter
                : DRIVE_LETTERS.get(random.nextInt(DRIVE_LETTERS.size()));
        path.append(drive).append(":\\");
        break;

      case UNIX:
        path.append("/");
        break;

      case MAC:
        path.append("/");
        break;
    }
  }

  private void generateRelativePrefix(StringBuilder path, OSType osType) {
    // 相对路径可能以 ./ 或 ../ 开始
    if (random.nextBoolean()) {
      if (random.nextBoolean()) {
        path.append("./");
      } else {
        path.append("../");
      }
    }
  }

  private void generateUNCPrefix(StringBuilder path) {
    // UNC路径格式：\\server\share
    path.append("\\\\");
    path.append(generateServerName());
    path.append("\\");
    path.append(generateShareName());
    path.append("\\");
  }

  private String generateServerName() {
    String[] serverPrefixes = {"srv", "server", "fs", "file", "nas", "storage"};
    String prefix = serverPrefixes[random.nextInt(serverPrefixes.length)];
    return prefix + (random.nextInt(99) + 1);
  }

  private String generateShareName() {
    String[] shareNames = {"shared", "public", "data", "files", "documents", "projects", "backup"};
    return shareNames[random.nextInt(shareNames.length)];
  }

  private void generateDirectoryStructure(
      StringBuilder path,
      OSType osType,
      int depth,
      boolean includeSpaces,
      boolean includeSpecialChars) {
    String separator = getSeparator(osType);
    for (int i = 0; i < depth; i++) {
      String dirName = generateDirectoryName(includeSpaces, includeSpecialChars);
      path.append(dirName);
      if (i < depth - 1 || path.charAt(path.length() - 1) != separator.charAt(0)) {
        path.append(separator);
      }
    }
  }

  private String generateDirectoryName(boolean includeSpaces, boolean includeSpecialChars) {
    String baseName = COMMON_DIRECTORIES.get(random.nextInt(COMMON_DIRECTORIES.size()));

    // 30%概率添加数字后缀
    if (random.nextDouble() < 0.3) {
      baseName += random.nextInt(100);
    }

    // 处理空格
    if (includeSpaces && random.nextDouble() < 0.3) {
      baseName = baseName.replace(" ", " ");
      if (!baseName.contains(" ")) {
        baseName += " " + (random.nextInt(10) + 1);
      }
    } else {
      baseName = baseName.replace(" ", "_");
    }

    // 处理特殊字符
    if (includeSpecialChars && random.nextDouble() < 0.2) {
      char specialChar = SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length()));
      baseName += specialChar;
    }

    return baseName;
  }

  private void generateFilename(
      StringBuilder path,
      OSType osType,
      String extension,
      boolean includeSpaces,
      boolean includeSpecialChars) {

    String separator = getSeparator(osType);

    // 确保路径以分隔符结尾
    if (path.length() > 0 && path.charAt(path.length() - 1) != separator.charAt(0)) {
      path.append(separator);
    }

    // 生成文件名
    String filename = generateBaseFilename(includeSpaces, includeSpecialChars);

    // 添加扩展名
    String fileExtension = determineFileExtension(extension);
    if (fileExtension != null && !fileExtension.isEmpty()) {
      filename += "." + fileExtension;
    }

    path.append(filename);
  }

  private String generateBaseFilename(boolean includeSpaces, boolean includeSpecialChars) {
    String baseName = COMMON_FILENAMES.get(random.nextInt(COMMON_FILENAMES.size()));

    // 50%概率添加数字或日期后缀
    if (random.nextDouble() < 0.5) {
      if (random.nextBoolean()) {
        baseName += "_" + random.nextInt(1000);
      } else {
        baseName +=
            "_"
                + String.format(
                    "%04d%02d%02d",
                    2020 + random.nextInt(5), 1 + random.nextInt(12), 1 + random.nextInt(28));
      }
    }

    // 处理空格
    if (includeSpaces && random.nextDouble() < 0.3) {
      baseName = baseName.replace("_", " ");
    }

    // 处理特殊字符
    if (includeSpecialChars && random.nextDouble() < 0.2) {
      char specialChar = SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length()));
      baseName += specialChar;
    }

    return baseName;
  }

  private String determineFileExtension(String extension) {
    if (extension == null || extension.isEmpty() || "NONE".equalsIgnoreCase(extension)) {
      return null;
    }

    if ("ANY".equalsIgnoreCase(extension)) {
      // 随机选择一个类别
      List<String> categories = new ArrayList<>(FILE_EXTENSIONS.keySet());
      String category = categories.get(random.nextInt(categories.size()));
      List<String> extensions = FILE_EXTENSIONS.get(category);
      return extensions.get(random.nextInt(extensions.size()));
    }

    // 检查是否是预定义类别
    if (FILE_EXTENSIONS.containsKey(extension.toUpperCase())) {
      List<String> extensions = FILE_EXTENSIONS.get(extension.toUpperCase());
      return extensions.get(random.nextInt(extensions.size()));
    }

    // 直接使用指定的扩展名
    return extension.toLowerCase();
  }

  private String getSeparator(OSType osType) {
    switch (osType) {
      case WINDOWS:
        return "\\";
      case UNIX:
      case MAC:
      default:
        return "/";
    }
  }

  private String extractExtension(String filePath) {
    int lastDot = filePath.lastIndexOf('.');
    int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

    if (lastDot > lastSeparator && lastDot < filePath.length() - 1) {
      return filePath.substring(lastDot + 1);
    }

    return "";
  }

  private String extractDirectory(String filePath) {
    int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

    if (lastSeparator > 0) {
      return filePath.substring(0, lastSeparator);
    }

    return "";
  }

  private String extractFilename(String filePath) {
    int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

    if (lastSeparator >= 0 && lastSeparator < filePath.length() - 1) {
      return filePath.substring(lastSeparator + 1);
    }

    return filePath;
  }

  /** 生成路径穿越攻击payload */
  public String generatePathTraversalPayload(OSType osType, int depth) {
    StringBuilder payload = new StringBuilder();
    String separator = getSeparator(osType);

    for (int i = 0; i < depth; i++) {
      payload.append("..").append(separator);
    }

    // 添加目标文件
    if (osType == OSType.WINDOWS) {
      payload.append("windows").append(separator).append("win.ini");
    } else {
      payload.append("etc").append(separator).append("passwd");
    }

    return payload.toString();
  }

  /** 生成超长路径（用于测试路径长度限制） */
  public String generateLongPath(OSType osType, int targetLength) {
    StringBuilder longPath = new StringBuilder();
    String separator = getSeparator(osType);

    // 添加前缀
    generateAbsolutePrefix(longPath, osType, null);

    // 生成长目录名
    String longDirName = "a".repeat(Math.min(255, targetLength / 10));

    while (longPath.length() < targetLength - 100) {
      longPath.append(longDirName).append(separator);
    }

    // 添加文件名
    longPath.append("file.txt");

    return longPath.toString();
  }

  /**
   * 确保配置已加载。
   *
   * @param config 配置
   */
  private void ensureConfigLoaded(FieldConfig config) {
    if (filePathConfig == null) {
      synchronized (this) {
        if (filePathConfig == null) {
          loadConfig(config);
        }
      }
    }
  }

  /**
   * 加载配置。
   *
   * @param config 配置
   */
  private void loadConfig(FieldConfig config) {
    try {
      String configFile = getStringParam(config, "paths_config_file", DEFAULT_CONFIG_FILE);

      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
      if (inputStream != null) {
        filePathConfig = yamlMapper.readValue(inputStream, FilePathConfig.class);
        logger.info("File paths config loaded from: {}", configFile);
      } else {
        logger.warn("Config file not found: {}, using fallback data", configFile);
        initializeFallbackConfig();
      }
    } catch (Exception e) {
      logger.error("Failed to load file paths config, using fallback data", e);
      initializeFallbackConfig();
    }
  }

  /** 初始化fallback配置。 */
  private void initializeFallbackConfig() {
    filePathConfig = new FilePathConfig();
  }

  /** 文件路径配置类。 */
  @SuppressWarnings("unused")
  private static class FilePathConfig {
    private Map<String, DirectoryConfig> directories;
    private Map<String, FilenameConfig> filenames;
    private Map<String, ExtensionConfig> extensions;

    public Map<String, DirectoryConfig> getDirectories() {
      return directories;
    }

    public void setDirectories(Map<String, DirectoryConfig> directories) {
      this.directories = directories;
    }

    public Map<String, FilenameConfig> getFilenames() {
      return filenames;
    }

    public void setFilenames(Map<String, FilenameConfig> filenames) {
      this.filenames = filenames;
    }

    public Map<String, ExtensionConfig> getExtensions() {
      return extensions;
    }

    public void setExtensions(Map<String, ExtensionConfig> extensions) {
      this.extensions = extensions;
    }
  }

  /** 目录配置类。 */
  @SuppressWarnings("unused")
  private static class DirectoryConfig {
    private String name;
    private List<String> directories;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getDirectories() {
      return directories;
    }

    public void setDirectories(List<String> directories) {
      this.directories = directories;
    }
  }

  /** 文件名配置类。 */
  @SuppressWarnings("unused")
  private static class FilenameConfig {
    private String name;
    private List<String> filenames;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getFilenames() {
      return filenames;
    }

    public void setFilenames(List<String> filenames) {
      this.filenames = filenames;
    }
  }

  /** 扩展名配置类。 */
  @SuppressWarnings("unused")
  private static class ExtensionConfig {
    private String name;
    private List<String> extensions;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getExtensions() {
      return extensions;
    }

    public void setExtensions(List<String> extensions) {
      this.extensions = extensions;
    }
  }
}
