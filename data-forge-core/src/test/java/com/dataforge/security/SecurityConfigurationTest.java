package com.dataforge.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SecurityConfiguration 测试类。
 *
 * <p>测试安全配置类的各种功能和默认值。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("SecurityConfiguration 测试")
class SecurityConfigurationTest {

  private SecurityConfiguration config;

  @BeforeEach
  void setUp() {
    config = new SecurityConfiguration();
  }

  @Nested
  @DisplayName("默认值测试")
  class DefaultValueTests {

    @Test
    @DisplayName("maxRecordCount 默认值应为 10,000,000")
    void maxRecordCount_DefaultValue_ShouldBe10Million() {
      // Then
      assertThat(config.getMaxRecordCount()).isEqualTo(10_000_000);
    }

    @Test
    @DisplayName("maxThreadCount 默认值应为 128")
    void maxThreadCount_DefaultValue_ShouldBe128() {
      // Then
      assertThat(config.getMaxThreadCount()).isEqualTo(128);
    }

    @Test
    @DisplayName("maxFieldCount 默认值应为 1000")
    void maxFieldCount_DefaultValue_ShouldBe1000() {
      // Then
      assertThat(config.getMaxFieldCount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("maxConfigSizeMb 默认值应为 50")
    void maxConfigSizeMb_DefaultValue_ShouldBe50() {
      // Then
      assertThat(config.getMaxConfigSizeMb()).isEqualTo(50);
    }

    @Test
    @DisplayName("maxFieldNameLength 默认值应为 100")
    void maxFieldNameLength_DefaultValue_ShouldBe100() {
      // Then
      assertThat(config.getMaxFieldNameLength()).isEqualTo(100);
    }

    @Test
    @DisplayName("maxPathLength 默认值应为 255")
    void maxPathLength_DefaultValue_ShouldBe255() {
      // Then
      assertThat(config.getMaxPathLength()).isEqualTo(255);
    }

    @Test
    @DisplayName("allowedOutputDirectories 默认值应包含 output, data, temp, export")
    void allowedOutputDirectories_DefaultValue_ShouldContainDefaultDirectories() {
      // Then
      assertThat(config.getAllowedOutputDirectories())
          .containsExactlyInAnyOrder("output", "data", "temp", "export");
    }

    @Test
    @DisplayName("allowedFileExtensions 默认值应包含 .csv, .json, .txt, .sql, .xml")
    void allowedFileExtensions_DefaultValue_ShouldContainDefaultExtensions() {
      // Then
      assertThat(config.getAllowedFileExtensions())
          .containsExactlyInAnyOrder(".csv", ".json", ".txt", ".sql", ".xml");
    }

    @Test
    @DisplayName("forbiddenPathPatterns 默认值应包含系统目录模式")
    void forbiddenPathPatterns_DefaultValue_ShouldContainSystemDirectoryPatterns() {
      // Then - 检查是否包含预期的路径模式
      assertThat(config.getForbiddenPathPatterns()).isNotEmpty();
      assertThat(config.getForbiddenPathPatterns()).contains(".*/etc/.*");
      assertThat(config.getForbiddenPathPatterns()).contains(".*/root/.*");
      assertThat(config.getForbiddenPathPatterns()).contains(".*/usr/bin/.*");
      // 路径遍历模式（包含点号）
      assertThat(config.getForbiddenPathPatterns()).anyMatch(pattern -> pattern.contains("."));
    }

    @Test
    @DisplayName("enablePathTraversalProtection 默认值应为 true")
    void enablePathTraversalProtection_DefaultValue_ShouldBeTrue() {
      // Then
      assertThat(config.isEnablePathTraversalProtection()).isTrue();
    }

    @Test
    @DisplayName("enableResourceMonitoring 默认值应为 true")
    void enableResourceMonitoring_DefaultValue_ShouldBeTrue() {
      // Then
      assertThat(config.isEnableResourceMonitoring()).isTrue();
    }

    @Test
    @DisplayName("enableInputSanitization 默认值应为 true")
    void enableInputSanitization_DefaultValue_ShouldBeTrue() {
      // Then
      assertThat(config.isEnableInputSanitization()).isTrue();
    }

    @Test
    @DisplayName("enableSecurityLogging 默认值应为 true")
    void enableSecurityLogging_DefaultValue_ShouldBeTrue() {
      // Then
      assertThat(config.isEnableSecurityLogging()).isTrue();
    }

    @Test
    @DisplayName("memoryWarningThreshold 默认值应为 80")
    void memoryWarningThreshold_DefaultValue_ShouldBe80() {
      // Then
      assertThat(config.getMemoryWarningThreshold()).isEqualTo(80);
    }

    @Test
    @DisplayName("memoryErrorThreshold 默认值应为 90")
    void memoryErrorThreshold_DefaultValue_ShouldBe90() {
      // Then
      assertThat(config.getMemoryErrorThreshold()).isEqualTo(90);
    }

    @Test
    @DisplayName("maxConcurrentTasks 默认值应为 10")
    void maxConcurrentTasks_DefaultValue_ShouldBe10() {
      // Then
      assertThat(config.getMaxConcurrentTasks()).isEqualTo(10);
    }

    @Test
    @DisplayName("allowAbsolutePaths 默认值应为 false")
    void allowAbsolutePaths_DefaultValue_ShouldBeFalse() {
      // Then
      assertThat(config.isAllowAbsolutePaths()).isFalse();
    }
  }

  @Nested
  @DisplayName("Getter/Setter 测试")
  class GetterSetterTests {

    @Test
    @DisplayName("setMaxRecordCount 应正确设置值")
    void setMaxRecordCount_ShouldSetCorrectValue() {
      // When
      config.setMaxRecordCount(5000);

      // Then
      assertThat(config.getMaxRecordCount()).isEqualTo(5000);
    }

    @Test
    @DisplayName("setMaxThreadCount 应正确设置值")
    void setMaxThreadCount_ShouldSetCorrectValue() {
      // When
      config.setMaxThreadCount(32);

      // Then
      assertThat(config.getMaxThreadCount()).isEqualTo(32);
    }

    @Test
    @DisplayName("setMaxFieldCount 应正确设置值")
    void setMaxFieldCount_ShouldSetCorrectValue() {
      // When
      config.setMaxFieldCount(500);

      // Then
      assertThat(config.getMaxFieldCount()).isEqualTo(500);
    }

    @Test
    @DisplayName("setMaxConfigSizeMb 应正确设置值")
    void setMaxConfigSizeMb_ShouldSetCorrectValue() {
      // When
      config.setMaxConfigSizeMb(100);

      // Then
      assertThat(config.getMaxConfigSizeMb()).isEqualTo(100);
    }

    @Test
    @DisplayName("setMaxFieldNameLength 应正确设置值")
    void setMaxFieldNameLength_ShouldSetCorrectValue() {
      // When
      config.setMaxFieldNameLength(200);

      // Then
      assertThat(config.getMaxFieldNameLength()).isEqualTo(200);
    }

    @Test
    @DisplayName("setMaxPathLength 应正确设置值")
    void setMaxPathLength_ShouldSetCorrectValue() {
      // When
      config.setMaxPathLength(500);

      // Then
      assertThat(config.getMaxPathLength()).isEqualTo(500);
    }

    @Test
    @DisplayName("setAllowedOutputDirectories 应正确设置值")
    void setAllowedOutputDirectories_ShouldSetCorrectValue() {
      // When
      config.setAllowedOutputDirectories(List.of("custom1", "custom2"));

      // Then
      assertThat(config.getAllowedOutputDirectories())
          .containsExactlyInAnyOrder("custom1", "custom2");
    }

    @Test
    @DisplayName("setAllowedFileExtensions 应正确设置值")
    void setAllowedFileExtensions_ShouldSetCorrectValue() {
      // When
      config.setAllowedFileExtensions(Set.of(".pdf", ".docx"));

      // Then
      assertThat(config.getAllowedFileExtensions()).containsExactlyInAnyOrder(".pdf", ".docx");
    }

    @Test
    @DisplayName("setForbiddenPathPatterns 应正确设置值")
    void setForbiddenPathPatterns_ShouldSetCorrectValue() {
      // When
      config.setForbiddenPathPatterns(List.of(".*pattern1.*", ".*pattern2.*"));

      // Then
      assertThat(config.getForbiddenPathPatterns())
          .containsExactlyInAnyOrder(".*pattern1.*", ".*pattern2.*");
    }

    @Test
    @DisplayName("setEnablePathTraversalProtection 应正确设置值")
    void setEnablePathTraversalProtection_ShouldSetCorrectValue() {
      // When
      config.setEnablePathTraversalProtection(false);

      // Then
      assertThat(config.isEnablePathTraversalProtection()).isFalse();
    }

    @Test
    @DisplayName("setEnableResourceMonitoring 应正确设置值")
    void setEnableResourceMonitoring_ShouldSetCorrectValue() {
      // When
      config.setEnableResourceMonitoring(false);

      // Then
      assertThat(config.isEnableResourceMonitoring()).isFalse();
    }

    @Test
    @DisplayName("setEnableInputSanitization 应正确设置值")
    void setEnableInputSanitization_ShouldSetCorrectValue() {
      // When
      config.setEnableInputSanitization(false);

      // Then
      assertThat(config.isEnableInputSanitization()).isFalse();
    }

    @Test
    @DisplayName("setEnableSecurityLogging 应正确设置值")
    void setEnableSecurityLogging_ShouldSetCorrectValue() {
      // When
      config.setEnableSecurityLogging(false);

      // Then
      assertThat(config.isEnableSecurityLogging()).isFalse();
    }

    @Test
    @DisplayName("setMemoryWarningThreshold 应正确设置值")
    void setMemoryWarningThreshold_ShouldSetCorrectValue() {
      // When
      config.setMemoryWarningThreshold(70);

      // Then
      assertThat(config.getMemoryWarningThreshold()).isEqualTo(70);
    }

    @Test
    @DisplayName("setMemoryErrorThreshold 应正确设置值")
    void setMemoryErrorThreshold_ShouldSetCorrectValue() {
      // When
      config.setMemoryErrorThreshold(95);

      // Then
      assertThat(config.getMemoryErrorThreshold()).isEqualTo(95);
    }

    @Test
    @DisplayName("setMaxConcurrentTasks 应正确设置值")
    void setMaxConcurrentTasks_ShouldSetCorrectValue() {
      // When
      config.setMaxConcurrentTasks(20);

      // Then
      assertThat(config.getMaxConcurrentTasks()).isEqualTo(20);
    }

    @Test
    @DisplayName("setAllowAbsolutePaths 应正确设置值")
    void setAllowAbsolutePaths_ShouldSetCorrectValue() {
      // When
      config.setAllowAbsolutePaths(true);

      // Then
      assertThat(config.isAllowAbsolutePaths()).isTrue();
    }
  }

  @Nested
  @DisplayName("验证方法测试")
  class ValidationTests {

    @Test
    @DisplayName("有效配置应通过验证")
    void validate_WithValidConfig_ShouldPass() {
      // Given - 使用默认配置
      config.setMemoryErrorThreshold(90);
      config.setMemoryWarningThreshold(80);
      config.setMaxThreadCount(32);

      // When & Then - 不应抛出异常
      config.validate();
    }

    @Test
    @DisplayName("错误阈值小于等于警告阈值应抛出异常")
    void validate_WhenErrorThresholdLessThanOrEqualWarningThreshold_ShouldThrowException() {
      // Given
      config.setMemoryWarningThreshold(80);
      config.setMemoryErrorThreshold(80); // 等于警告阈值

      // When & Then
      assertThatThrownBy(() -> config.validate())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Memory error threshold must be greater than warning threshold");
    }

    @Test
    @DisplayName("错误阈值小于警告阈值应抛出异常")
    void validate_WhenErrorThresholdLessThanWarningThreshold_ShouldThrowException() {
      // Given
      config.setMemoryWarningThreshold(80);
      config.setMemoryErrorThreshold(70); // 小于警告阈值

      // When & Then
      assertThatThrownBy(() -> config.validate())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Memory error threshold must be greater than warning threshold");
    }

    @Test
    @DisplayName("线程数超过CPU核数的8倍应抛出异常")
    void validate_WhenThreadCountExceeds8TimesCpuCores_ShouldThrowException() {
      // Given
      int availableProcessors = Runtime.getRuntime().availableProcessors();
      config.setMaxThreadCount(availableProcessors * 9); // 超过8倍

      // When & Then
      assertThatThrownBy(() -> config.validate())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max thread count is too high compared to available processors");
    }

    @Test
    @DisplayName("线程数等于CPU核数的8倍应通过验证")
    void validate_WhenThreadCountEquals8TimesCpuCores_ShouldPass() {
      // Given
      int availableProcessors = Runtime.getRuntime().availableProcessors();
      config.setMaxThreadCount(availableProcessors * 8); // 等于8倍

      // When & Then - 不应抛出异常
      config.validate();
    }
  }

  @Nested
  @DisplayName("计算方法测试")
  class CalculationMethodTests {

    @Test
    @DisplayName("getMaxConfigSizeBytes 应正确转换为字节")
    void getMaxConfigSizeBytes_ShouldReturnCorrectBytes() {
      // Given
      config.setMaxConfigSizeMb(50);

      // When
      long bytes = config.getMaxConfigSizeBytes();

      // Then
      assertThat(bytes).isEqualTo(50L * 1024 * 1024);
    }

    @Test
    @DisplayName("getMaxConfigSizeBytes 应处理不同的MB值")
    void getMaxConfigSizeBytes_ShouldHandleDifferentMbValues() {
      // Given
      config.setMaxConfigSizeMb(100);

      // When
      long bytes = config.getMaxConfigSizeBytes();

      // Then
      assertThat(bytes).isEqualTo(100L * 1024 * 1024);
    }

    @Test
    @DisplayName("getMaxConfigSizeBytes 应处理1MB")
    void getMaxConfigSizeBytes_ShouldHandle1Mb() {
      // Given
      config.setMaxConfigSizeMb(1);

      // When
      long bytes = config.getMaxConfigSizeBytes();

      // Then
      assertThat(bytes).isEqualTo(1024L * 1024);
    }
  }

  @Nested
  @DisplayName("toString 方法测试")
  class ToStringTests {

    @Test
    @DisplayName("toString 应包含关键配置值")
    void toString_ShouldContainKeyConfigurationValues() {
      // When
      String result = config.toString();

      // Then
      assertThat(result).contains("maxRecordCount");
      assertThat(result).contains("maxThreadCount");
      assertThat(result).contains("maxFieldCount");
      assertThat(result).contains("maxConfigSizeMb");
      assertThat(result).contains("allowAbsolutePaths");
      assertThat(result).contains("enablePathTraversalProtection");
      assertThat(result).contains("enableResourceMonitoring");
    }

    @Test
    @DisplayName("toString 应包含实际配置值")
    void toString_ShouldContainActualConfigurationValues() {
      // Given
      config.setMaxRecordCount(5000);
      config.setMaxThreadCount(32);

      // When
      String result = config.toString();

      // Then
      assertThat(result).contains("maxRecordCount=5000");
      assertThat(result).contains("maxThreadCount=32");
    }
  }
}
