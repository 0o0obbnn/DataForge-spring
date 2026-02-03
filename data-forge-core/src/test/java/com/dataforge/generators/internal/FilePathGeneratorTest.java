package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.DataForgeContext;
import com.dataforge.model.SimpleFieldConfig;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FilePathGenerator 测试")
class FilePathGeneratorTest {

  private FilePathGenerator generator;
  private SimpleFieldConfig config;
  private DataForgeContext context;

  @BeforeEach
  void setUp() {
    generator = new FilePathGenerator();
    config = new SimpleFieldConfig();
    config.setType("filepath");
    context = new DataForgeContext();
  }

  @Nested
  @DisplayName("基本功能测试")
  class BasicFunctionalityTests {

    @Test
    @DisplayName("应生成文件路径")
    void shouldGenerateFilePath() {
      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).isNotEmpty();
    }

    @Test
    @DisplayName("应将文件路径信息存入上下文")
    void shouldStoreFilePathInContext() {
      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(context.get("file_path")).isNotNull();
      assertThat(context.get("file_os")).isNotNull();
      assertThat(context.get("file_extension")).isNotNull();
      assertThat(context.get("file_directory")).isNotNull();
      assertThat(context.get("file_name")).isNotNull();
    }
  }

  @Nested
  @DisplayName("操作系统类型测试")
  class OSTypeTests {

    @Test
    @DisplayName("应生成Windows路径")
    void shouldGenerateWindowsPath() {
      config.setParam("os", "WINDOWS");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches("^[A-Z]:\\\\.*");
      assertThat(context.get("file_os")).isEqualTo(Optional.of("WINDOWS"));
    }

    @Test
    @DisplayName("应生成Unix路径")
    void shouldGenerateUnixPath() {
      config.setParam("os", "UNIX");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).startsWith("/");
      assertThat(context.get("file_os")).isEqualTo(Optional.of("UNIX"));
    }

    @Test
    @DisplayName("应生成Mac路径")
    void shouldGenerateMacPath() {
      config.setParam("os", "MAC");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).startsWith("/");
      assertThat(context.get("file_os")).isEqualTo(Optional.of("MAC"));
    }

    @Test
    @DisplayName("应生成任意操作系统路径")
    void shouldGenerateAnyOSPath() {
      config.setParam("os", "ANY");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(context.get("file_os")).isNotNull();
    }
  }

  @Nested
  @DisplayName("路径类型测试")
  class PathTypeTests {

    @Test
    @DisplayName("应生成绝对路径")
    void shouldGenerateAbsolutePath() {
      config.setParam("type", "ABSOLUTE");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches("^[A-Z]:\\\\.*|^/.*");
    }

    @Test
    @DisplayName("应生成相对路径")
    void shouldGenerateRelativePath() {
      config.setParam("type", "RELATIVE");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).doesNotMatch("^[A-Z]:\\\\.*");
      assertThat(filePath).doesNotStartWith("/");
    }

    @Test
    @DisplayName("应生成UNC路径")
    void shouldGenerateUNCPath() {
      config.setParam("os", "WINDOWS");
      config.setParam("type", "UNC");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches("^\\\\\\\\.*\\\\.*");
    }
  }

  @Nested
  @DisplayName("目录深度测试")
  class DirectoryDepthTests {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 10})
    @DisplayName("应支持不同目录深度")
    void shouldSupportDifferentDepths(int depth) {
      config.setParam("depth", String.valueOf(depth));

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      long separatorCount = filePath.chars().filter(ch -> ch == '/' || ch == '\\').count();
      assertThat(separatorCount).isGreaterThanOrEqualTo(depth);
    }

    @Test
    @DisplayName("应处理最小深度")
    void shouldHandleMinimumDepth() {
      config.setParam("depth", "1");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
    }

    @Test
    @DisplayName("应处理最大深度")
    void shouldHandleMaximumDepth() {
      config.setParam("depth", "10");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
    }
  }

  @Nested
  @DisplayName("文件名测试")
  class FilenameTests {

    @Test
    @DisplayName("应生成包含文件名的路径")
    void shouldGeneratePathWithFilename() {
      config.setParam("include_filename", "true");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      String filename = (String) ((Optional<?>) context.get("file_name")).orElse(null);
      assertThat(filename).isNotNull();
      assertThat(filePath).endsWith(filename);
    }

    @Test
    @DisplayName("应生成不包含文件名的路径")
    void shouldGeneratePathWithoutFilename() {
      config.setParam("include_filename", "false");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).doesNotContain(".");
    }
  }

  @Nested
  @DisplayName("文件扩展名测试")
  class FileExtensionTests {

    @Test
    @DisplayName("应生成文档扩展名")
    void shouldGenerateDocumentExtension() {
      config.setParam("extension", "DOCUMENT");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches(".*\\.(txt|doc|docx|pdf|rtf|odt|pages)$");
    }

    @Test
    @DisplayName("应生成图片扩展名")
    void shouldGenerateImageExtension() {
      config.setParam("extension", "IMAGE");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches(".*\\.(jpg|jpeg|png|gif|bmp|tiff|svg|webp)$");
    }

    @Test
    @DisplayName("应生成视频扩展名")
    void shouldGenerateVideoExtension() {
      config.setParam("extension", "VIDEO");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches(".*\\.(mp4|avi|mkv|mov|wmv|flv|webm|m4v)$");
    }

    @Test
    @DisplayName("应生成音频扩展名")
    void shouldGenerateAudioExtension() {
      config.setParam("extension", "AUDIO");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches(".*\\.(mp3|wav|flac|aac|ogg|wma|m4a)$");
    }

    @Test
    @DisplayName("应生成代码扩展名")
    void shouldGenerateCodeExtension() {
      config.setParam("extension", "CODE");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches(".*\\.(java|py|js|html|css|cpp|c|h|php|rb|go|rs)$");
    }

    @Test
    @DisplayName("应生成任意扩展名")
    void shouldGenerateAnyExtension() {
      config.setParam("extension", "ANY");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches(".*\\.[a-z0-9]+$");
    }

    @Test
    @DisplayName("应生成自定义扩展名")
    void shouldGenerateCustomExtension() {
      config.setParam("extension", "txt");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).endsWith(".txt");
    }

    @Test
    @DisplayName("应生成无扩展名文件")
    void shouldGenerateFileWithoutExtension() {
      config.setParam("extension", "NONE");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).doesNotContain(".");
    }
  }

  @Nested
  @DisplayName("空格测试")
  class SpaceTests {

    @Test
    @DisplayName("应生成包含空格的路径")
    void shouldGeneratePathWithSpaces() {
      config.setParam("include_spaces", "true");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
    }

    @Test
    @DisplayName("应生成不包含空格的路径")
    void shouldGeneratePathWithoutSpaces() {
      config.setParam("include_spaces", "false");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).doesNotContain(" ");
    }
  }

  @Nested
  @DisplayName("特殊字符测试")
  class SpecialCharTests {

    @Test
    @DisplayName("应生成包含特殊字符的路径")
    void shouldGeneratePathWithSpecialChars() {
      config.setParam("include_special_chars", "true");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
    }

    @Test
    @DisplayName("应生成不包含特殊字符的路径")
    void shouldGeneratePathWithoutSpecialChars() {
      config.setParam("include_special_chars", "false");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
    }
  }

  @Nested
  @DisplayName("驱动器盘符测试")
  class DriveLetterTests {

    @ParameterizedTest
    @ValueSource(strings = {"C", "D", "E", "Z"})
    @DisplayName("应支持不同驱动器盘符")
    void shouldSupportDifferentDriveLetters(String driveLetter) {
      config.setParam("os", "WINDOWS");
      config.setParam("drive_letter", driveLetter);

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).startsWith(driveLetter + ":\\");
    }

    @Test
    @DisplayName("应随机选择驱动器盘符")
    void shouldSelectRandomDriveLetter() {
      config.setParam("os", "WINDOWS");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).matches("^[A-Z]:\\\\.*");
    }
  }

  @Nested
  @DisplayName("路径穿越测试")
  class PathTraversalTests {

    @Test
    @DisplayName("应生成Windows路径穿越payload")
    void shouldGenerateWindowsPathTraversalPayload() {
      config.setParam("os", "WINDOWS");
      config.setParam("include_spaces", "true");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
    }

    @Test
    @DisplayName("应生成Unix路径穿越payload")
    void shouldGenerateUnixPathTraversalPayload() {
      config.setParam("os", "UNIX");
      config.setParam("include_spaces", "true");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
    }
  }

  @Nested
  @DisplayName("超长路径测试")
  class LongPathTests {

    @Test
    @DisplayName("应生成超长Windows路径")
    void shouldGenerateLongWindowsPath() {
      config.setParam("os", "WINDOWS");
      config.setParam("depth", "30");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath.length()).isGreaterThan(200);
      assertThat(filePath).matches("^[A-Z]:\\\\.*");
    }

    @Test
    @DisplayName("应生成超长Unix路径")
    void shouldGenerateLongUnixPath() {
      config.setParam("os", "UNIX");
      config.setParam("depth", "30");

      String filePath = generator.generate(config, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath.length()).isGreaterThan(200);
      assertThat(filePath).startsWith("/");
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("null配置应生成默认路径")
    void shouldGenerateDefaultPathForNullConfig() {
      String filePath = generator.generate(null, context);

      assertThat(filePath).isNotNull();
      assertThat(filePath).isEqualTo("/home/user/documents/file.txt");
    }

    @Test
    @DisplayName("null上下文应不抛出异常")
    void shouldNotThrowForNullContext() {
      String filePath = generator.generate(config, null);

      assertThat(filePath).isNotNull();
    }
  }

  @Nested
  @DisplayName("生成器信息测试")
  class GeneratorInfoTests {

    @Test
    @DisplayName("应返回正确的类型")
    void shouldReturnCorrectType() {
      String type = generator.getType();

      assertThat(type).isEqualTo("filepath");
    }

    @Test
    @DisplayName("应返回正确的配置类")
    void shouldReturnCorrectConfigClass() {
      Class<?> configClass = generator.getConfigClass();

      assertThat(configClass).isEqualTo(com.dataforge.model.FieldConfig.class);
    }
  }

  @Nested
  @DisplayName("唯一性测试")
  class UniquenessTests {

    @Test
    @DisplayName("批量生成的路径应具有唯一性")
    void shouldGenerateUniquePaths() {
      int count = 100;
      java.util.Set<String> filePaths = new java.util.HashSet<>();

      for (int i = 0; i < count; i++) {
        String filePath = generator.generate(config, context);
        filePaths.add(filePath);
      }

      assertThat(filePaths).hasSize(count);
    }

    @Test
    @DisplayName("相同配置应生成不同路径")
    void shouldGenerateDifferentPathsForSameConfig() {
      String path1 = generator.generate(config, context);
      String path2 = generator.generate(config, context);

      assertThat(path1).isNotEqualTo(path2);
    }
  }
}
