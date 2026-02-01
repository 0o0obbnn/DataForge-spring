package com.dataforge.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * DataForge主配置类。
 *
 * <p>使用Spring Boot的@ConfigurationProperties注解， 支持从application.yml或外部配置文件加载配置。
 *
 * <p>配置示例：
 *
 * <pre>
 * dataforge:
 *   count: 100
 *   output:
 *     format: csv
 *     file: "output/test-data.csv"
 *   fields:
 *     - name: "userId"
 *       type: "uuid"
 *       params:
 *         type: "UUID4"
 *     - name: "name"
 *       type: "name"
 *       params:
 *         type: "CN"
 *         gender: "ANY"
 * </pre>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "dataforge")
@Validated
public class ForgeConfig {

  /** 要生成的记录数量，默认为10。 */
  @Min(value = 1, message = "Count must be at least 1")
  @Max(value = 1_000_000, message = "Count cannot exceed 1,000,000")
  private int count = 10;

  /** 执行模式：PLATFORM（使用平台线程）或 VIRTUAL（使用虚拟线程），默认为 PLATFORM。 */
  @Pattern(
      regexp = "PLATFORM|VIRTUAL",
      message = "ExecutionMode must be either PLATFORM or VIRTUAL")
  private String executionMode = "PLATFORM";

  /** 输出配置。 */
  @NotNull(message = "Output configuration cannot be null")
  @Valid
  private OutputConfig output = new OutputConfig();

  /** 字段配置列表。 */
  @NotNull(message = "Fields configuration cannot be null")
  @Size(min = 1, max = 100, message = "Fields count must be between 1 and 100")
  @Valid
  private List<FieldConfigWrapper> fields = new ArrayList<>();

  /** 是否启用数据校验，默认为true。 */
  private boolean validate = true;

  /** 并发线程数，默认为1（单线程）。 */
  @Min(value = 1, message = "Thread count must be at least 1")
  @Max(value = 16, message = "Thread count cannot exceed 16")
  private int threads = 1;

  /** 随机种子，用于可重现的数据生成。如果不设置，使用系统时间作为种子。 */
  private Long seed;

  /**
   * 获取要生成的记录数量。
   *
   * @return 记录数量
   */
  public int getCount() {
    return count;
  }

  /**
   * 设置要生成的记录数量。
   *
   * @param count 记录数量
   */
  public void setCount(final int count) {
    this.count = count;
  }

  /**
   * 获取执行模式。
   *
   * @return 执行模式（PLATFORM 或 VIRTUAL）
   */
  public String getExecutionMode() {
    return executionMode;
  }

  /**
   * 设置执行模式。
   *
   * @param executionMode 执行模式：PLATFORM（使用平台线程）或 VIRTUAL（使用虚拟线程）
   */
  public void setExecutionMode(final String executionMode) {
    if (!"PLATFORM".equalsIgnoreCase(executionMode) && !"VIRTUAL".equalsIgnoreCase(executionMode)) {
      throw new IllegalArgumentException("ExecutionMode must be either PLATFORM or VIRTUAL");
    }
    this.executionMode = executionMode.toUpperCase();
  }

  /**
   * 获取输出配置。
   *
   * @return 输出配置
   */
  public OutputConfig getOutput() {
    return output;
  }

  /**
   * 设置输出配置。
   *
   * @param output 输出配置
   */
  public void setOutput(final OutputConfig output) {
    this.output = output;
  }

  /**
   * 获取字段配置列表。
   *
   * @return 字段配置列表
   */
  public List<FieldConfigWrapper> getFields() {
    return fields;
  }

  /**
   * 设置字段配置列表。
   *
   * @param fields 字段配置列表
   */
  public void setFields(final List<FieldConfigWrapper> fields) {
    this.fields = fields != null ? fields : new ArrayList<>();
  }

  /**
   * 检查是否启用数据校验。
   *
   * @return 如果启用校验返回true，否则返回false
   */
  public boolean isValidate() {
    return validate;
  }

  /**
   * 设置是否启用数据校验。
   *
   * @param validate 是否启用校验
   */
  public void setValidate(final boolean validate) {
    this.validate = validate;
  }

  /**
   * 获取并发线程数。
   *
   * @return 线程数
   */
  public int getThreads() {
    return threads;
  }

  /**
   * 设置并发线程数。
   *
   * @param threads 线程数
   */
  public void setThreads(final int threads) {
    this.threads = threads;
  }

  /**
   * 获取随机种子。
   *
   * @return 随机种子，如果未设置返回null
   */
  public Long getSeed() {
    return seed;
  }

  /**
   * 设置随机种子。
   *
   * @param seed 随机种子
   */
  public void setSeed(final Long seed) {
    this.seed = seed;
  }

  /**
   * 添加字段配置。
   *
   * @param field 字段配置
   */
  public void addField(final FieldConfigWrapper field) {
    if (field != null) {
      this.fields.add(field);
    }
  }

  /**
   * 检查配置是否有效。
   *
   * @return 如果配置有效返回true，否则返回false
   */
  public boolean isValid() {
    return count > 0 && output != null && fields != null && !fields.isEmpty() && threads > 0;
  }

  @Override
  public String toString() {
    return String.format(
        "ForgeConfig{count=%d, output=%s, fields=%d, validate=%s, threads=%d, seed=%s}",
        count, output, fields.size(), validate, threads, seed);
  }
}
