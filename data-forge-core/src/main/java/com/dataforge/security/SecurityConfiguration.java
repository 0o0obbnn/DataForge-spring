package com.dataforge.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 安全配置类。
 *
 * <p>集中管理DataForge的安全相关配置，包括输入限制、资源限制、文件操作安全等。 所有安全配置都可以通过应用配置文件进行外部化配置。
 *
 * <p><strong>配置示例：</strong>
 *
 * <pre>
 * dataforge:
 *   security:
 *     max-record-count: 5000000
 *     max-thread-count: 32
 *     max-field-count: 500
 *     max-config-size-mb: 50
 *     allowed-output-directories:
 *       - "output"
 *       - "data"
 *       - "temp"
 *     allowed-file-extensions:
 *       - ".csv"
 *       - ".json"
 *       - ".txt"
 *     enable-path-traversal-protection: true
 *     enable-resource-monitoring: true
 * </pre>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "dataforge.security")
@Validated
public class SecurityConfiguration {

  /** 最大记录数量限制。 */
  @Min(1)
  @Max(100_000_000)
  private int maxRecordCount = 10_000_000;

  /** 最大线程数量限制。 */
  @Min(1)
  @Max(1000)
  private int maxThreadCount = 64;

  /** 最大字段数量限制。 */
  @Min(1)
  @Max(10000)
  private int maxFieldCount = 1000;

  /** 最大配置文件大小限制（MB）。 */
  @Min(1)
  @Max(1000)
  private int maxConfigSizeMb = 50;

  /** 最大字段名长度。 */
  @Min(1)
  @Max(500)
  private int maxFieldNameLength = 100;

  /** 最大路径长度。 */
  @Min(1)
  @Max(2000)
  private int maxPathLength = 255;

  /** 允许的输出目录列表。 */
  @NotNull
  private List<String> allowedOutputDirectories = List.of("output", "data", "temp", "export");

  /** 允许的文件扩展名列表。 */
  @NotNull
  private Set<String> allowedFileExtensions = Set.of(".csv", ".json", ".txt", ".sql", ".xml");

  /** 禁止的路径模式（正则表达式）。 */
  @NotNull
  private List<@Pattern(regexp = ".*") String> forbiddenPathPatterns =
      List.of(
          ".*\\.\\.", // 包含 ..
          ".*/etc/.*", // 系统配置目录
          ".*/root/.*", // root目录
          ".*/usr/bin/.*", // 系统二进制目录
          ".*/windows/.*", // Windows系统目录
          ".*/program files/.*" // Windows程序目录
          );

  /** 是否启用路径遍历保护。 */
  private boolean enablePathTraversalProtection = true;

  /** 是否启用资源监控。 */
  private boolean enableResourceMonitoring = true;

  /** 是否启用输入清理。 */
  private boolean enableInputSanitization = true;

  /** 是否启用详细的安全日志。 */
  private boolean enableSecurityLogging = true;

  /** 内存使用警告阈值（百分比）。 */
  @Min(10)
  @Max(95)
  private int memoryWarningThreshold = 80;

  /** 内存使用错误阈值（百分比）。 */
  @Min(50)
  @Max(99)
  private int memoryErrorThreshold = 90;

  /** 最大并发生成任务数。 */
  @Min(1)
  @Max(1000)
  private int maxConcurrentTasks = 10;

  /** 是否允许绝对路径 (测试环境可以设置为true)。 */
  private boolean allowAbsolutePaths = false;

  // Getters and Setters

  public int getMaxRecordCount() {
    return maxRecordCount;
  }

  public void setMaxRecordCount(int maxRecordCount) {
    this.maxRecordCount = maxRecordCount;
  }

  public int getMaxThreadCount() {
    return maxThreadCount;
  }

  public void setMaxThreadCount(int maxThreadCount) {
    this.maxThreadCount = maxThreadCount;
  }

  public int getMaxFieldCount() {
    return maxFieldCount;
  }

  public void setMaxFieldCount(int maxFieldCount) {
    this.maxFieldCount = maxFieldCount;
  }

  public int getMaxConfigSizeMb() {
    return maxConfigSizeMb;
  }

  public void setMaxConfigSizeMb(int maxConfigSizeMb) {
    this.maxConfigSizeMb = maxConfigSizeMb;
  }

  public int getMaxFieldNameLength() {
    return maxFieldNameLength;
  }

  public void setMaxFieldNameLength(int maxFieldNameLength) {
    this.maxFieldNameLength = maxFieldNameLength;
  }

  public int getMaxPathLength() {
    return maxPathLength;
  }

  public void setMaxPathLength(int maxPathLength) {
    this.maxPathLength = maxPathLength;
  }

  public List<String> getAllowedOutputDirectories() {
    return allowedOutputDirectories;
  }

  public void setAllowedOutputDirectories(List<String> allowedOutputDirectories) {
    this.allowedOutputDirectories = allowedOutputDirectories;
  }

  public Set<String> getAllowedFileExtensions() {
    return allowedFileExtensions;
  }

  public void setAllowedFileExtensions(Set<String> allowedFileExtensions) {
    this.allowedFileExtensions = allowedFileExtensions;
  }

  public List<String> getForbiddenPathPatterns() {
    return forbiddenPathPatterns;
  }

  public void setForbiddenPathPatterns(List<String> forbiddenPathPatterns) {
    this.forbiddenPathPatterns = forbiddenPathPatterns;
  }

  public boolean isEnablePathTraversalProtection() {
    return enablePathTraversalProtection;
  }

  public void setEnablePathTraversalProtection(boolean enablePathTraversalProtection) {
    this.enablePathTraversalProtection = enablePathTraversalProtection;
  }

  public boolean isEnableResourceMonitoring() {
    return enableResourceMonitoring;
  }

  public void setEnableResourceMonitoring(boolean enableResourceMonitoring) {
    this.enableResourceMonitoring = enableResourceMonitoring;
  }

  public boolean isEnableInputSanitization() {
    return enableInputSanitization;
  }

  public void setEnableInputSanitization(boolean enableInputSanitization) {
    this.enableInputSanitization = enableInputSanitization;
  }

  public boolean isEnableSecurityLogging() {
    return enableSecurityLogging;
  }

  public void setEnableSecurityLogging(boolean enableSecurityLogging) {
    this.enableSecurityLogging = enableSecurityLogging;
  }

  public int getMemoryWarningThreshold() {
    return memoryWarningThreshold;
  }

  public void setMemoryWarningThreshold(int memoryWarningThreshold) {
    this.memoryWarningThreshold = memoryWarningThreshold;
  }

  public int getMemoryErrorThreshold() {
    return memoryErrorThreshold;
  }

  public void setMemoryErrorThreshold(int memoryErrorThreshold) {
    this.memoryErrorThreshold = memoryErrorThreshold;
  }

  public int getMaxConcurrentTasks() {
    return maxConcurrentTasks;
  }

  public void setMaxConcurrentTasks(int maxConcurrentTasks) {
    this.maxConcurrentTasks = maxConcurrentTasks;
  }

  public boolean isAllowAbsolutePaths() {
    return allowAbsolutePaths;
  }

  public void setAllowAbsolutePaths(boolean allowAbsolutePaths) {
    this.allowAbsolutePaths = allowAbsolutePaths;
  }

  /** 获取最大配置文件大小（字节）。 */
  public long getMaxConfigSizeBytes() {
    return (long) maxConfigSizeMb * 1024 * 1024;
  }

  /** 验证配置的一致性。 */
  public void validate() {
    if (memoryErrorThreshold <= memoryWarningThreshold) {
      throw new IllegalArgumentException(
          "Memory error threshold must be greater than warning threshold");
    }

    if (maxThreadCount > Runtime.getRuntime().availableProcessors() * 8) {
      throw new IllegalArgumentException(
          "Max thread count is too high compared to available processors");
    }
  }

  @Override
  public String toString() {
    return "SecurityConfiguration{"
        + "maxRecordCount="
        + maxRecordCount
        + ", maxThreadCount="
        + maxThreadCount
        + ", maxFieldCount="
        + maxFieldCount
        + ", maxConfigSizeMb="
        + maxConfigSizeMb
        + ", allowAbsolutePaths="
        + allowAbsolutePaths
        + ", enablePathTraversalProtection="
        + enablePathTraversalProtection
        + ", enableResourceMonitoring="
        + enableResourceMonitoring
        + '}';
  }
}
