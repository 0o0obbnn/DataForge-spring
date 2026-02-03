package com.dataforge.core;

import com.dataforge.core.config.BatchProperties;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.service.ErrorCode;
import com.dataforge.service.GenerationException;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 批量数据生成器
 *
 * <p>根据DataForge详细开发规范3.3.1节：批量生成器 (BatchGenerator) 支持高效的批量数据生成，包括多线程生成、流式输出和内存优化。
 *
 * <p>核心功能： - 多线程并行生成：支持4-8线程并行生成，提升生成速度200-500% - 批次大小控制：支持1000-50000条记录的批次生成 -
 * 流式输出：避免内存溢出，支持TB级数据生成 - 进度监控：实时显示生成进度和性能指标
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class BatchGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(BatchGenerator.class);

  private final GeneratorFactory generatorFactory;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final DataRelationEngine relationEngine;
  private final BatchProperties batchProperties;

  // 性能指标
  private final AtomicLong totalGenerated = new AtomicLong(0);
  private volatile long startTime = 0;
  private volatile boolean isGenerating = false;

  /**
   * 输出回调可能不是线程安全的（例如测试里用 ArrayList.addAll / long[] 自增）。 这里统一串行化 outputConsumer
   * 回调，避免多线程并发回调导致丢数据/计数不准。
   */
  private final Object outputConsumerLock = new Object();

  // 配置参数
  private int threadCount = 4;
  private int batchSize = 10000;
  private boolean enableProgressMonitoring = true;
  private int progressReportInterval = 5000;

  public BatchGenerator(
      GeneratorFactory generatorFactory,
      DataRelationEngine relationEngine,
      BatchProperties batchProperties) {
    this.generatorFactory = generatorFactory;
    this.relationEngine = relationEngine;
    this.batchProperties = batchProperties;

    // 从配置读取初始值
    this.threadCount = batchProperties.getThreadCount();
    this.batchSize = batchProperties.getBatchSize();

    // 创建可配置的线程池
    this.taskExecutor = createTaskExecutor();
  }

  /** 创建可动态配置的线程池 */
  private ThreadPoolTaskExecutor createTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(threadCount);
    executor.setMaxPoolSize(threadCount * 2);
    executor.setQueueCapacity(batchProperties != null ? batchProperties.getQueueCapacity() : 1000);
    executor.setThreadNamePrefix("data-forge-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 批量生成数据
   *
   * @param schema 数据模式定义
   * @param totalRecords 总记录数
   * @param outputConsumer 输出消费者（用于流式输出）
   * @return 生成统计信息
   */
  public GenerationStats generateBatch(
      DataSchema schema, long totalRecords, Consumer<List<Map<String, Object>>> outputConsumer) {

    if (isGenerating) {
      throw new IllegalStateException("Another batch generation is in progress");
    }

    try {
      isGenerating = true;
      startTime = System.currentTimeMillis();
      totalGenerated.set(0);

      LOGGER.info(
          "Starting batch generation: {} records, {} threads, batch size: {}",
          totalRecords,
          threadCount,
          batchSize);

      // 创建数据生成任务
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      // 计算每个线程的工作量
      long recordsPerThread = totalRecords / threadCount;
      long remainingRecords = totalRecords % threadCount;

      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        final long threadRecords = recordsPerThread + (i < remainingRecords ? 1 : 0);

        CompletableFuture<Void> future =
            CompletableFuture.runAsync(
                () -> {
                  generateForThread(schema, threadRecords, threadIndex, outputConsumer);
                },
                taskExecutor.getThreadPoolExecutor());

        futures.add(future);
      }

      // 等待所有线程完成
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;

      long generated = totalGenerated.get();
      GenerationStats stats =
          new GenerationStats(
              generated,
              duration,
              threadCount,
              batchSize,
              calculateThroughput(generated, duration));

      LOGGER.info(
          "Batch generation completed: {} records in {} ms, throughput: {} records/sec",
          generated,
          duration,
          stats.getThroughput());

      return stats;

    } finally {
      isGenerating = false;
    }
  }

  /** 单线程数据生成 */
  private void generateForThread(
      DataSchema schema,
      long recordCount,
      int threadIndex,
      Consumer<List<Map<String, Object>>> outputConsumer) {

    LOGGER.debug("Thread-{} starting generation of {} records", threadIndex, recordCount);

    List<Map<String, Object>> batch = new ArrayList<>(batchSize);
    DataForgeContext context = new DataForgeContext();

    // 为每个线程设置独立的随机种子，确保多线程数据的随机性
    // context.setRandomSeed(System.currentTimeMillis() + threadIndex); //
    // 暂时注释掉，需要在DataForgeContext中实现

    long processed = 0;

    while (processed < recordCount) {
      int currentBatchSize = (int) Math.min(batchSize, recordCount - processed);
      batch.clear();

      // 生成当前批次的数据
      for (int i = 0; i < currentBatchSize; i++) {
        Map<String, Object> record = generateSingleRecord(schema, context);
        batch.add(record);

        // 进度监控
        long currentGenerated = totalGenerated.get();
        if (enableProgressMonitoring && (currentGenerated + i + 1) % progressReportInterval == 0) {
          reportProgress();
        }
      }

      // 流式输出当前批次
      if (!batch.isEmpty()) {
        List<Map<String, Object>> out = new ArrayList<>(batch);
        synchronized (outputConsumerLock) {
          outputConsumer.accept(out);
          totalGenerated.addAndGet(out.size());
        }
        processed += batch.size();
      }
    }

    LOGGER.debug("Thread-{} completed generation of {} records", threadIndex, processed);
  }

  /** 生成单条记录 */
  private Map<String, Object> generateSingleRecord(DataSchema schema, DataForgeContext context) {
    Map<String, Object> record = new LinkedHashMap<>();

    // 按照字段定义顺序生成数据
    for (FieldDefinition fieldDef : schema.getFields()) {
      Object value = generateFieldValue(fieldDef, record, context);
      record.put(fieldDef.getName(), value);
    }

    // 应用数据关联规则
    if (relationEngine != null) {
      record = relationEngine.applyRelations(record, schema.getRelations());
    }

    return record;
  }

  /** 生成字段值 */
  private Object generateFieldValue(
      FieldDefinition fieldDef, Map<String, Object> partialRecord, DataForgeContext context) {

    DataGenerator<?, ?> rawGenerator = generatorFactory.getGenerator(fieldDef.getType());

    if (rawGenerator == null) {
      // 抛出明确的异常而不是返回默认值
      throw new GenerationException(
          String.format(
              "未找到类型 '%s' 的数据生成器，字段名: '%s'。请检查类型是否正确或是否已注册生成器。",
              fieldDef.getType(), fieldDef.getName()),
          ErrorCode.GENERATOR_NOT_FOUND);
    }

    @SuppressWarnings("unchecked")
    DataGenerator<Object, FieldConfig> generator =
        (DataGenerator<Object, FieldConfig>) rawGenerator;

    try {
      Object value = generator.generate(fieldDef.getConfig(), context);

      // 验证生成结果不为null（警告但允许）
      if (value == null) {
        LOGGER.warn(
            "Generator {} returned null for field {}, will use null value",
            fieldDef.getType(),
            fieldDef.getName());
      }

      return value;

    } catch (GenerationException e) {
      // 业务异常直接抛出
      throw e;
    } catch (Exception e) {
      // 包装为业务异常
      LOGGER.error(
          "Error generating value for field {}: {}", fieldDef.getName(), e.getMessage(), e);
      throw new GenerationException(
          String.format(
              "生成字段 '%s' (类型: %s) 时发生错误: %s",
              fieldDef.getName(), fieldDef.getType(), e.getMessage()),
          ErrorCode.GENERATION_FAILED,
          e);
    }
  }

  /** 报告生成进度 */
  private void reportProgress() {
    long elapsed = System.currentTimeMillis() - startTime;
    long generated = totalGenerated.get();
    long throughput = calculateThroughput(generated, elapsed);

    LOGGER.info("Progress: {} records generated, {} records/sec", generated, throughput);
  }

  /** 计算吞吐量 */
  private long calculateThroughput(long records, long durationMs) {
    return durationMs > 0 ? (records * 1000) / durationMs : 0;
  }

  /** 获取当前生成状态 */
  public GenerationStatus getStatus() {
    long elapsed = isGenerating ? System.currentTimeMillis() - startTime : 0;
    long generated = totalGenerated.get();
    long throughput = calculateThroughput(generated, elapsed);

    return new GenerationStatus(isGenerating, generated, elapsed, throughput);
  }

  // Getter/Setter 方法
  public void setThreadCount(int threadCount) {
    if (threadCount < 1 || threadCount > 16) {
      throw new IllegalArgumentException("Thread count must be between 1 and 16");
    }

    int oldCount = this.threadCount;
    this.threadCount = threadCount;

    // 动态更新线程池配置
    if (taskExecutor != null) {
      taskExecutor.setCorePoolSize(threadCount);
      taskExecutor.setMaxPoolSize(threadCount * 2);

      LOGGER.info("线程池配置已更新: {} -> {}", oldCount, threadCount);
    }
  }

  public int getThreadCount() {
    return threadCount;
  }

  /** 获取线程池执行器（用于监控） */
  public ThreadPoolTaskExecutor getTaskExecutor() {
    return taskExecutor;
  }

  public void setBatchSize(int batchSize) {
    if (batchSize < 100 || batchSize > 100000) {
      throw new IllegalArgumentException("Batch size must be between 100 and 100000");
    }
    this.batchSize = batchSize;
  }

  public void setEnableProgressMonitoring(boolean enableProgressMonitoring) {
    this.enableProgressMonitoring = enableProgressMonitoring;
  }

  public void setProgressReportInterval(int progressReportInterval) {
    this.progressReportInterval = progressReportInterval;
  }

  /** 关闭批量生成器，释放资源 */
  public void shutdown() {
    LOGGER.info("关闭批量生成器线程池");
    if (taskExecutor != null) {
      taskExecutor.shutdown();
      try {
        if (!taskExecutor.getThreadPoolExecutor().awaitTermination(60, TimeUnit.SECONDS)) {
          LOGGER.warn("线程池未在60秒内关闭，强制关闭");
          // ThreadPoolTaskExecutor没有shutdownNow方法，直接关闭底层线程池
          taskExecutor.getThreadPoolExecutor().shutdownNow();
        }
      } catch (InterruptedException e) {
        taskExecutor.getThreadPoolExecutor().shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /** Spring容器销毁时自动调用 */
  @PreDestroy
  public void destroy() {
    shutdown();
  }

  /** 数据模式定义 */
  public static class DataSchema {
    private final List<FieldDefinition> fields;
    private final List<DataRelationEngine.DataRelation> relations;

    public DataSchema(
        List<FieldDefinition> fields, List<DataRelationEngine.DataRelation> relations) {
      this.fields = fields != null ? fields : new ArrayList<>();
      this.relations = relations != null ? relations : new ArrayList<>();
    }

    public List<FieldDefinition> getFields() {
      return fields;
    }

    public List<DataRelationEngine.DataRelation> getRelations() {
      return relations;
    }
  }

  /** 字段定义 */
  public static class FieldDefinition {
    private final String name;
    private final String type;
    private final FieldConfig config;

    public FieldDefinition(String name, String type, FieldConfig config) {
      this.name = name;
      this.type = type;
      this.config = config;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public FieldConfig getConfig() {
      return config;
    }
  }

  /** 生成统计信息 */
  public static class GenerationStats {
    private final long totalRecords;
    private final long durationMs;
    private final int threadCount;
    private final int batchSize;
    private final long throughput;

    public GenerationStats(
        long totalRecords, long durationMs, int threadCount, int batchSize, long throughput) {
      this.totalRecords = totalRecords;
      this.durationMs = durationMs;
      this.threadCount = threadCount;
      this.batchSize = batchSize;
      this.throughput = throughput;
    }

    public long getTotalRecords() {
      return totalRecords;
    }

    public long getDurationMs() {
      return durationMs;
    }

    public int getThreadCount() {
      return threadCount;
    }

    public int getBatchSize() {
      return batchSize;
    }

    public long getThroughput() {
      return throughput;
    }
  }

  /** 生成状态 */
  public static class GenerationStatus {
    private final boolean isGenerating;
    private final long recordsGenerated;
    private final long elapsedMs;
    private final long currentThroughput;

    public GenerationStatus(
        boolean isGenerating, long recordsGenerated, long elapsedMs, long currentThroughput) {
      this.isGenerating = isGenerating;
      this.recordsGenerated = recordsGenerated;
      this.elapsedMs = elapsedMs;
      this.currentThroughput = currentThroughput;
    }

    public boolean isGenerating() {
      return isGenerating;
    }

    public long getRecordsGenerated() {
      return recordsGenerated;
    }

    public long getElapsedMs() {
      return elapsedMs;
    }

    public long getCurrentThroughput() {
      return currentThroughput;
    }
  }
}
