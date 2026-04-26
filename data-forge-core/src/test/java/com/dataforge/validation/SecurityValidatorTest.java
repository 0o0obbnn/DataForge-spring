package com.dataforge.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dataforge.config.ForgeConfig;
import com.dataforge.monitoring.ResourceMonitor;
import com.dataforge.security.SecurityConfiguration;
import com.dataforge.service.SecurityException;
import com.dataforge.service.ValidationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SecurityValidator 测试类。
 *
 * <p>测试安全验证器的各种验证功能。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("SecurityValidator 测试")
class SecurityValidatorTest {

  private SecurityConfiguration securityConfig;
  private ResourceMonitor resourceMonitor;
  private SecurityValidator securityValidator;

  @BeforeEach
  void setUp() {
    // 创建真实的安全配置实例
    securityConfig = new SecurityConfiguration();
    securityConfig.setMaxRecordCount(10000);
    securityConfig.setMaxThreadCount(100); // 测试环境需要更多线程
    securityConfig.setMaxFieldCount(100);
    securityConfig.setMaxFieldNameLength(255);
    securityConfig.setMaxPathLength(1000);
    securityConfig.setMaxConfigSizeMb(10);
    securityConfig.setAllowAbsolutePaths(false);
    // 禁用资源监控以避免测试环境问题
    securityConfig.setEnableResourceMonitoring(false);

    // 创建真实的资源监控实例
    resourceMonitor = new ResourceMonitor(securityConfig);

    securityValidator = new SecurityValidator(securityConfig, resourceMonitor);
  }

  @Nested
  @DisplayName("配置验证测试")
  class ConfigurationValidationTests {

    @Test
    @DisplayName("有效配置应通过验证")
    void validateConfiguration_WithValidConfig_ShouldPass() throws Exception {
      // Given
      ForgeConfig config = new ForgeConfig();
      config.setCount(100);
      config.setThreads(4);
      config.setFields(createValidFields(5));

      // When & Then - 不应抛出异常
      securityValidator.validateConfiguration(config);
    }

    @Test
    @DisplayName("超过最大记录数应抛出异常")
    void validateConfiguration_ExceedsMaxRecordCount_ShouldThrowException() {
      // Given
      ForgeConfig config = new ForgeConfig();
      config.setCount(20000); // 超过 maxRecordCount (10000)
      config.setThreads(4);
      config.setFields(createValidFields(5));

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateConfiguration(config))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("记录数量");
    }

    @Test
    @DisplayName("空字段列表应抛出异常")
    void validateConfiguration_WithEmptyFields_ShouldThrowException() {
      // Given
      ForgeConfig config = new ForgeConfig();
      config.setCount(100);
      config.setThreads(4);
      config.setFields(new ArrayList<>());

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateConfiguration(config))
          .isInstanceOf(ValidationException.class)
          .hasMessageContaining("字段配置不能为空");
    }

    @Test
    @DisplayName("超过最大字段数应抛出异常")
    void validateConfiguration_ExceedsMaxFieldCount_ShouldThrowException() {
      // Given
      ForgeConfig config = new ForgeConfig();
      config.setCount(100);
      config.setThreads(4);
      config.setFields(createValidFields(200)); // 超过 maxFieldCount (100)

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateConfiguration(config))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("字段数量");
    }

    @Test
    @DisplayName("超过最大线程数应抛出异常")
    void validateConfiguration_ExceedsMaxThreadCount_ShouldThrowException() {
      // Given
      ForgeConfig config = new ForgeConfig();
      config.setCount(100);
      config.setThreads(200); // 超过 maxThreadCount (100)
      config.setFields(createValidFields(5));

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateConfiguration(config))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("线程数量");
    }

    @Test
    @DisplayName("配置验证应成功执行")
    void validateConfiguration_ShouldSucceed() throws Exception {
      // Given
      ForgeConfig config = new ForgeConfig();
      config.setCount(100);
      config.setThreads(4);
      config.setFields(createValidFields(5));

      // When & Then - 不应抛出异常
      securityValidator.validateConfiguration(config);
    }
  }

  @Nested
  @DisplayName("输出路径验证测试")
  class OutputPathValidationTests {

    @Test
    @DisplayName("安全的相对路径应通过验证")
    void validateOutputPath_WithSafeRelativePath_ShouldPass() throws SecurityException {
      // Given
      String safePath = "output/data.csv";

      // When & Then - 不应抛出异常
      securityValidator.validateOutputPath(safePath);
    }

    @Test
    @DisplayName("包含路径遍历的路径应抛出异常")
    void validateOutputPath_WithPathTraversal_ShouldThrowException() {
      // Given
      String unsafePath = "output/../etc/passwd";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateOutputPath(unsafePath))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("path traversal");
    }

    @Test
    @DisplayName("不允许的绝对路径应抛出异常")
    void validateOutputPath_WithAbsolutePath_ShouldThrowException() {
      // Given
      String absolutePath = "/tmp/data.csv";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateOutputPath(absolutePath))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("不允许使用绝对路径");
    }

    @Test
    @DisplayName("空路径应抛出异常")
    void validateOutputPath_WithEmptyPath_ShouldThrowException() {
      // Given
      String emptyPath = "";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateOutputPath(emptyPath))
          .isInstanceOf(ValidationException.class)
          .hasMessageContaining("输出路径不能为空");
    }

    @Test
    @DisplayName("超过最大路径长度应抛出异常")
    void validateOutputPath_ExceedsMaxLength_ShouldThrowException() {
      // Given
      String longPath = "output/" + "a".repeat(2000); // 超过 maxPathLength (1000)

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateOutputPath(longPath))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    @DisplayName("包含不安全字符的路径应抛出异常")
    void validateOutputPath_WithUnsafeCharacters_ShouldThrowException() {
      // Given
      String unsafePath = "output/data|file.csv";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateOutputPath(unsafePath))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("unsafe characters");
    }

    @Test
    @DisplayName("不在允许目录中的路径应抛出异常")
    void validateOutputPath_NotInAllowedDirectory_ShouldThrowException() {
      // Given
      String unallowedPath = "unauthorized/data.csv"; // 不在允许的目录中

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateOutputPath(unallowedPath))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("输出路径不在允许的目录中");
    }
  }

  @Nested
  @DisplayName("SQL注入验证测试")
  class SqlInjectionValidationTests {

    @Test
    @DisplayName("安全的字符串应通过验证")
    void validateSqlInjection_WithSafeString_ShouldPass() throws SecurityException {
      // Given
      String safeString = "张三";

      // When & Then - 不应抛出异常
      securityValidator.validateSqlInjection(safeString);
    }

    @Test
    @DisplayName("包含SELECT关键字的字符串应抛出异常")
    void validateSqlInjection_WithSelectKeyword_ShouldThrowException() {
      // Given
      String maliciousString = "name'; SELECT * FROM users; --";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateSqlInjection(maliciousString))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("SQL注入");
    }

    @Test
    @DisplayName("包含UNION关键字的字符串应抛出异常")
    void validateSqlInjection_WithUnionKeyword_ShouldThrowException() {
      // Given
      String maliciousString = "name' UNION SELECT password FROM users; --";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateSqlInjection(maliciousString))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("SQL注入");
    }

    @Test
    @DisplayName("null字符串应通过验证")
    void validateSqlInjection_WithNullString_ShouldPass() throws SecurityException {
      // Given
      String nullString = null;

      // When & Then - 不应抛出异常
      securityValidator.validateSqlInjection(nullString);
    }

    @Test
    @DisplayName("包含EXEC关键字的字符串应抛出异常")
    void validateSqlInjection_WithExecKeyword_ShouldThrowException() {
      // Given
      String maliciousString = "name'; EXEC xp_cmdshell('dir'); --";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateSqlInjection(maliciousString))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("SQL注入");
    }
  }

  @Nested
  @DisplayName("文件名验证测试")
  class FileNameValidationTests {

    @Test
    @DisplayName("安全的文件名应通过验证")
    void validateFileName_WithSafeFileName_ShouldPass() throws SecurityException {
      // Given
      String safeFileName = "data.csv";

      // When & Then - 不应抛出异常
      securityValidator.validateFileName(safeFileName);
    }

    @Test
    @DisplayName("包含路径遍历的文件名应抛出异常")
    void validateFileName_WithPathTraversal_ShouldThrowException() {
      // Given
      String unsafeFileName = "../../etc/passwd";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateFileName(unsafeFileName))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("path traversal");
    }

    @Test
    @DisplayName("包含目录分隔符的文件名应抛出异常")
    void validateFileName_WithDirectorySeparator_ShouldThrowException() {
      // Given
      String unsafeFileName = "subdir/data.csv";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateFileName(unsafeFileName))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("path traversal");
    }

    @Test
    @DisplayName("空文件名应抛出异常")
    void validateFileName_WithEmptyFileName_ShouldThrowException() {
      // Given
      String emptyFileName = "";

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateFileName(emptyFileName))
          .isInstanceOf(ValidationException.class)
          .hasMessageContaining("文件名不能为空");
    }

    @Test
    @DisplayName("不允许的文件扩展名应抛出异常")
    void validateFileName_WithUnallowedExtension_ShouldThrowException() {
      // Given
      String unsafeFileName = "data.exe"; // .exe 不在允许的扩展名列表中

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateFileName(unsafeFileName))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("不支持的文件扩展名");
    }

    @Test
    @DisplayName("超过最大文件名长度应抛出异常")
    void validateFileName_ExceedsMaxLength_ShouldThrowException() {
      // Given
      String longFileName = "a".repeat(2000) + ".csv"; // 超过 maxPathLength (1000)

      // When & Then
      assertThatThrownBy(() -> securityValidator.validateFileName(longFileName))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    @DisplayName("允许的文件扩展名应通过验证")
    void validateFileName_WithAllowedExtensions_ShouldPass() throws SecurityException {
      // Given
      String[] allowedExtensions = {"data.csv", "output.json", "data.txt", "script.sql"};

      // When & Then - 所有都不应抛出异常
      for (String fileName : allowedExtensions) {
        securityValidator.validateFileName(fileName);
      }
    }
  }

  /**
   * 创建有效的字段配置列表。
   *
   * @param count 字段数量
   * @return 字段配置列表
   */
  private List<com.dataforge.config.FieldConfigWrapper> createValidFields(int count) {
    List<com.dataforge.config.FieldConfigWrapper> fields = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      com.dataforge.config.FieldConfigWrapper field = new com.dataforge.config.FieldConfigWrapper();
      field.setName("field" + i);
      field.setType("string");
      fields.add(field);
    }
    return fields;
  }
}
