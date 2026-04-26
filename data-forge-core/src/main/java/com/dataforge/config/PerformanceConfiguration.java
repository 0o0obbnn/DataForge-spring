package com.dataforge.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 性能优化配置类。
 *
 * <p>集中管理DataForge的性能相关配置，包括线程池、批处理、内存管理等设置。 这些配置可以根据不同的部署环境和硬件条件进行调优。
 *
 * <p><strong>配置示例：</strong>
 *
 * <pre>
 * dataforge:
 *   performance:
 *     async-pool-size: 20
 *     batch-size: 1000
 *     enable-batch-processing: true
 *     memory-optimization: true
 *     gc-threshold-percent: 85
 *     max-queue-size: 50000
 *     result-streaming: true
 * </pre>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "dataforge.performance")
@EnableAsync
@EnableScheduling
public class PerformanceConfiguration {

  /** 是否启用性能优化。 */
  private boolean enabled = true;

  /** 异步任务线程池大小。 */
  @Min(2)
  @Max(200)
  private int asyncPoolSize = 20;

  /** 批处理大小。 */
  @Min(10)
  @Max(10000)
  private int batchSize = 1000;

  /** 是否启用批处理。 */
  private boolean enableBatchProcessing = true;

  /** 是否启用内存优化。 */
  private boolean memoryOptimization = true;

  /** GC触发阈值（内存使用百分比）。 */
  @Min(50)
  @Max(95)
  private int gcThresholdPercent = 85;

  /** 最大队列大小。 */
  @Min(1000)
  @Max(1000000)
  private int maxQueueSize = 50000;

  /** 是否启用结果流式处理。 */
  private boolean resultStreaming = true;

  /** 预热模式（系统启动时进行性能预热）。 */
  private boolean enableWarmup = false;

  /** 是否启用性能监控。 */
  private boolean enableMetrics = true;

  /** 慢查询阈值（毫秒）。 */
  @Min(100)
  @Max(60000)
  private long slowQueryThresholdMs = 5000;

  /** 内存警告阈值（百分比）。 */
  @Min(50)
  @Max(95)
  private int memoryWarningThreshold = 80;

  /** 是否启用自适应批处理大小。 */
  private boolean adaptiveBatchSize = false;

  /** 工作队列类型。 */
  public enum QueueType {
    /** 数组阻塞队列 */
    ARRAY_BLOCKING,
    /** 链表阻塞队列 */
    LINKED_BLOCKING,
    /** 优先级队列 */
    PRIORITY,
    /** 同步队列 */
    SYNCHRONOUS
  }

  /** 工作队列类型。 */
  private QueueType queueType = QueueType.ARRAY_BLOCKING;

  /**
   * 计算最优的线程池大小。
   *
   * @return 建议的线程池大小
   */
  public int calculateOptimalThreadPoolSize() {
    int cpuCores = Runtime.getRuntime().availableProcessors();

    // CPU密集型任务：CPU核数 + 1
    // I/O密集型任务：CPU核数 * 2
    // 混合型任务：CPU核数 * 1.5
    return Math.min(asyncPoolSize, (int) (cpuCores * 1.5) + 1);
  }

  /**
   * 计算最优的批处理大小。
   *
   * @param totalItems 总项目数
   * @param availableMemory 可用内存（字节）
   * @return 建议的批处理大小
   */
  public int calculateOptimalBatchSize(int totalItems, long availableMemory) {
    if (!adaptiveBatchSize) {
      return batchSize;
    }

    // 基于可用内存调整批处理大小
    long memoryPerItem = 1024; // 假设每项1KB
    int memoryBasedBatch = (int) (availableMemory / memoryPerItem / 10); // 使用10%的可用内存

    // 限制在配置范围内
    int adaptiveBatch = Math.min(memoryBasedBatch, batchSize);
    adaptiveBatch = Math.max(adaptiveBatch, 10); // 最小批处理大小

    return Math.min(adaptiveBatch, totalItems);
  }

  /**
   * 是否应该触发垃圾回收。
   *
   * @return 如果应该触发GC返回true
   */
  public boolean shouldTriggerGc() {
    if (!memoryOptimization) {
      return false;
    }

    Runtime runtime = Runtime.getRuntime();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;

    double usagePercent = (double) usedMemory / totalMemory * 100;
    return usagePercent >= gcThresholdPercent;
  }

  /**
   * 获取性能配置摘要。
   *
   * @return 配置摘要字符串
   */
  public String getConfigSummary() {
    return String.format(
        "PerformanceConfig{asyncPool=%d, batch=%d, memOpt=%s, queue=%s, streaming=%s}",
        asyncPoolSize, batchSize, memoryOptimization, queueType, resultStreaming);
  }

  // Getters and Setters

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getAsyncPoolSize() {
    return asyncPoolSize;
  }

  public void setAsyncPoolSize(int asyncPoolSize) {
    this.asyncPoolSize = asyncPoolSize;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public boolean isEnableBatchProcessing() {
    return enableBatchProcessing;
  }

  public void setEnableBatchProcessing(boolean enableBatchProcessing) {
    this.enableBatchProcessing = enableBatchProcessing;
  }

  public boolean isMemoryOptimization() {
    return memoryOptimization;
  }

  public void setMemoryOptimization(boolean memoryOptimization) {
    this.memoryOptimization = memoryOptimization;
  }

  public int getGcThresholdPercent() {
    return gcThresholdPercent;
  }

  public void setGcThresholdPercent(int gcThresholdPercent) {
    this.gcThresholdPercent = gcThresholdPercent;
  }

  public int getMaxQueueSize() {
    return maxQueueSize;
  }

  public void setMaxQueueSize(int maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
  }

  public boolean isResultStreaming() {
    return resultStreaming;
  }

  public void setResultStreaming(boolean resultStreaming) {
    this.resultStreaming = resultStreaming;
  }

  public boolean isEnableWarmup() {
    return enableWarmup;
  }

  public void setEnableWarmup(boolean enableWarmup) {
    this.enableWarmup = enableWarmup;
  }

  public boolean isEnableMetrics() {
    return enableMetrics;
  }

  public void setEnableMetrics(boolean enableMetrics) {
    this.enableMetrics = enableMetrics;
  }

  public long getSlowQueryThresholdMs() {
    return slowQueryThresholdMs;
  }

  public void setSlowQueryThresholdMs(long slowQueryThresholdMs) {
    this.slowQueryThresholdMs = slowQueryThresholdMs;
  }

  public int getMemoryWarningThreshold() {
    return memoryWarningThreshold;
  }

  public void setMemoryWarningThreshold(int memoryWarningThreshold) {
    this.memoryWarningThreshold = memoryWarningThreshold;
  }

  public boolean isAdaptiveBatchSize() {
    return adaptiveBatchSize;
  }

  public void setAdaptiveBatchSize(boolean adaptiveBatchSize) {
    this.adaptiveBatchSize = adaptiveBatchSize;
  }

  public QueueType getQueueType() {
    return queueType;
  }

  public void setQueueType(QueueType queueType) {
    this.queueType = queueType;
  }
}
