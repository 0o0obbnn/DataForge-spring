package com.dataforge.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * BatchGenerator 配置属性
 *
 * <p>从 application.yml 读取配置，支持动态刷新。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "dataforge.batch")
public class BatchProperties {

  @Min(1)
  @Max(64)
  private int threadCount = 4;

  @Min(100)
  @Max(100000)
  private int batchSize = 10000;

  @Min(100)
  @Max(10000)
  private int queueCapacity = 1000;

  private ExecutionMode executionMode = ExecutionMode.PLATFORM;

  public int getThreadCount() {
    return threadCount;
  }

  public void setThreadCount(int threadCount) {
    this.threadCount = threadCount;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public int getQueueCapacity() {
    return queueCapacity;
  }

  public void setQueueCapacity(int queueCapacity) {
    this.queueCapacity = queueCapacity;
  }

  public ExecutionMode getExecutionMode() {
    return executionMode;
  }

  public void setExecutionMode(ExecutionMode executionMode) {
    this.executionMode = executionMode;
  }

  /** 执行模式枚举 */
  public enum ExecutionMode {
    /** 平台线程模式 */
    PLATFORM,
    /** 虚拟线程模式 */
    VIRTUAL
  }
}
