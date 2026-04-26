package com.dataforge.service;

import com.dataforge.config.FieldConfigWrapper;
import com.dataforge.config.ForgeConfig;
import com.dataforge.config.OutputConfig;
import com.dataforge.core.DataForgeContext;
import com.dataforge.core.GeneratorFactory;
import com.dataforge.core.monitoring.MonitorPerformance;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.io.OutputStrategy;
import com.dataforge.model.FieldConfig;
import com.dataforge.validation.SecurityValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * DataForge核心服务类 - 增强版本。
 *
 * <p>作为编排核心逻辑的主服务，负责协调数据生成器、输出策略和配置管理。 该服务是整个数据生成流程的控制中心，具备企业级的健壮性和安全性。
 *
 * <p><strong>主要职责：</strong>
 *
 * <ul>
 *   <li>解析和验证配置
 *   <li>协调数据生成器执行数据生成
 *   <li>管理数据生成上下文和字段关联
 *   <li>控制数据输出流程
 *   <li>支持高性能并发数据生成
 *   <li>提供全面的安全防护
 * </ul>
 *
 * <p><strong>增强特性：</strong>
 *
 * <ul>
 *   <li>移除了ThreadLocal内存泄露风险
 *   <li>增强的并发控制和资源管理
 *   <li>完善的错误处理和恢复机制
 *   <li>全面的安全输入验证
 *   <li>性能监控和度量指标
 *   <li>优雅的资源清理
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Service
public class DataForgeService {

  private static final Logger logger = LoggerFactory.getLogger(DataForgeService.class);

  // 性能和监控相关
  private final AtomicLong totalRecordsGenerated = new AtomicLong(0);
  private final AtomicLong totalErrors = new AtomicLong(0);
  private final AtomicLong totalBytesGenerated = new AtomicLong(0);

  private final GeneratorFactory generatorFactory;
  private final List<OutputStrategy> outputStrategies;
  private final SecurityValidator securityValidator;

  /**
   * 构造函数，注入依赖。
   *
   * @param generatorFactory 生成器工厂
   * @param outputStrategies 输出策略列表
   * @param securityValidator 安全验证器
   */
  public DataForgeService(
      GeneratorFactory generatorFactory,
      List<OutputStrategy> outputStrategies,
      SecurityValidator securityValidator) {
    this.generatorFactory =
        Objects.requireNonNull(generatorFactory, "GeneratorFactory cannot be null");
    this.outputStrategies = outputStrategies != null ? outputStrategies : new ArrayList<>();
    this.securityValidator =
        Objects.requireNonNull(securityValidator, "SecurityValidator cannot be null");

    logger.info(
        "DataForgeService initialized with {} generators, {} output strategies",
        generatorFactory.getGeneratorCount(),
        this.outputStrategies.size());
  }

  /**
   * 根据配置生成数据 - 增强版本。
   *
   * <p>这是数据生成的主入口方法，负责整个生成流程的协调和控制。 增加了全面的安全验证、错误处理和资源管理。
   *
   * @param config 生成配置
   * @throws DataForgeException 当生成过程中发生错误时
   */
  @MonitorPerformance(value = "dataforge.generateData", slowThreshold = 5000)
  public void generateData(ForgeConfig config) throws DataForgeException {
    Objects.requireNonNull(config, "Configuration cannot be null");

    logger.info("Starting enhanced data generation with config: {}", config);

    long startTime = System.currentTimeMillis();
    AtomicReference<OutputStrategy> outputStrategyRef = new AtomicReference<>();

    try {
      // 1. 严格安全验证
      securityValidator.validateConfiguration(config);

      // 2. 业务逻辑验证
      validateBusinessLogic(config);

      // 3. 准备输出策略
      OutputStrategy outputStrategy = prepareOutputStrategy(config.getOutput());
      outputStrategyRef.set(outputStrategy);

      // 4. 提取字段名称
      List<String> fieldNames = extractFieldNames(config.getFields());

      // 5. 安全初始化输出策略
      initializeOutputStrategy(outputStrategy, config.getOutput(), fieldNames);

      // 6. 执行数据生成（增强的并发控制）
      if (config.getThreads() > 1) {
        generateDataConcurrentlyEnhanced(config, outputStrategy, fieldNames);
      } else {
        generateDataSequentiallyEnhanced(config, outputStrategy, fieldNames);
      }

      // 7. 安全完成输出
      finishOutputStrategy(outputStrategy);

      long duration = System.currentTimeMillis() - startTime;
      logger.info(
          "Data generation completed successfully. Generated {} records in {} ms, {} errors"
              + " occurred",
          config.getCount(),
          duration,
          totalErrors.get());

    } catch (SecurityException e) {
      totalErrors.incrementAndGet();
      logger.error("Security violation detected during data generation", e);
      throw e;
    } catch (Exception e) {
      totalErrors.incrementAndGet();
      logger.error("Data generation failed for config: {}", config, e);

      // 确保输出策略被正确清理
      // 注意：不要调用finish()，因为它可能已经在正常流程中被调用过了
      // 只是让GC回收资源，OutputStrategy是prototype scope的，每次请求都会创建新实例

      if (e instanceof DataForgeException) {
        throw e;
      }
      throw new GenerationException("Data generation failed: " + e.getMessage(), e)
          .withContext("configHash", config.hashCode())
          .withContext("totalErrors", totalErrors.get());
    }
  }

  /** 业务逻辑验证。 */
  private void validateBusinessLogic(ForgeConfig config) throws ConfigurationException {
    if (!config.isValid()) {
      throw new ConfigurationException("Configuration validation failed: " + config);
    }

    if (config.getFields().isEmpty()) {
      throw ConfigurationException.missingRequired("fields");
    }

    // 验证所有字段都有对应的生成器
    for (FieldConfigWrapper field : config.getFields()) {
      if (!generatorFactory.hasGenerator(field.getType())) {
        throw new ConfigurationException(
                field.getName(), "No generator found for field type: " + field.getType())
            .withContext("fieldType", field.getType())
            .withContext("availableGenerators", generatorFactory.getAvailableTypes());
      }
    }

    logger.debug("Business logic validation passed");
  }

  /** 准备输出策略。 */
  private OutputStrategy prepareOutputStrategy(OutputConfig outputConfig)
      throws ConfigurationException {
    for (OutputStrategy strategy : outputStrategies) {
      if (strategy.supports(outputConfig)) {
        logger.debug(
            "Selected output strategy: {} for format: {}",
            strategy.getClass().getSimpleName(),
            outputConfig.getFormat());

        // For stateful strategies like JsonOutputStrategy, create a new instance
        // to avoid thread-safety issues in concurrent scenarios
        if (strategy.getClass().getSimpleName().equals("JsonOutputStrategy")) {
          try {
            return strategy.getClass().getDeclaredConstructor().newInstance();
          } catch (Exception e) {
            logger.warn("Failed to create new instance, using shared instance", e);
            return strategy;
          }
        }

        return strategy;
      }
    }

    throw new ConfigurationException(
            "No output strategy found for format: " + outputConfig.getFormat())
        .withContext("requestedFormat", outputConfig.getFormat())
        .withContext("availableFormats", getAvailableOutputFormats());
  }

  /** 提取字段名称列表。 */
  private List<String> extractFieldNames(List<FieldConfigWrapper> fields) {
    List<String> fieldNames = new ArrayList<>(fields.size());
    for (FieldConfigWrapper field : fields) {
      fieldNames.add(field.getName());
    }
    return fieldNames;
  }

  /** 安全初始化输出策略。 */
  private void initializeOutputStrategy(
      OutputStrategy outputStrategy, OutputConfig outputConfig, List<String> fieldNames)
      throws DataForgeException {
    try {
      outputStrategy.initialize(outputConfig, fieldNames);
    } catch (Exception e) {
      throw new GenerationException("Failed to initialize output strategy", e)
          .withContext("outputFormat", outputConfig.getFormat())
          .withContext("fieldCount", fieldNames.size());
    }
  }

  /** 安全完成输出策略。 */
  private void finishOutputStrategy(OutputStrategy outputStrategy) throws DataForgeException {
    try {
      outputStrategy.finish();
    } catch (Exception e) {
      throw new GenerationException("Failed to finish output strategy", e);
    }
  }

  /** 顺序生成数据 - 增强版本。 */
  private void generateDataSequentiallyEnhanced(
      ForgeConfig config, OutputStrategy outputStrategy, List<String> fieldNames)
      throws DataForgeException {

    logger.debug("Starting enhanced sequential data generation for {} records", config.getCount());

    Random random = createSafeRandom(config.getSeed());
    int batchSize = calculateOptimalBatchSize(config.getCount());

    for (int i = 0; i < config.getCount(); i++) {
      try (DataForgeContext context = new DataForgeContext()) {
        context.setCurrentRecordIndex(i);

        // 生成单条记录
        Map<String, Object> record = generateSingleRecord(config.getFields(), context, random);

        // 输出记录
        outputStrategy.writeRecord(record);
        totalRecordsGenerated.incrementAndGet();

        // 定期刷新输出
        if ((i + 1) % batchSize == 0) {
          outputStrategy.flush();
          logger.debug("Generated {} records", i + 1);
        }

      } catch (Exception e) {
        totalErrors.incrementAndGet();
        throw new GenerationException(i, "Failed to generate record at index " + i, e)
            .withContext("recordIndex", i)
            .withContext("totalGenerated", totalRecordsGenerated.get());
      }
    }
  }

  /** 并发生成数据 - 完全重写的增强版本。 */
  private void generateDataConcurrentlyEnhanced(
      ForgeConfig config, OutputStrategy outputStrategy, List<String> fieldNames)
      throws DataForgeException {

    final boolean useVirtualThreads = "VIRTUAL".equalsIgnoreCase(config.getExecutionMode());

    logger.info(
        "Starting {} concurrent data generation for {} records with {} threads",
        useVirtualThreads ? "virtual thread" : "platform thread",
        config.getCount(),
        config.getThreads());

    final int threads =
        Math.max(1, Math.min(config.getThreads(), Runtime.getRuntime().availableProcessors() * 2));
    final int total = config.getCount();

    // 计算最优队列容量（增强版，基于字段配置）
    final int queueCapacity = calculateOptimalQueueCapacity(total, threads, config.getFields());
    final BlockingQueue<Map<String, Object>> resultQueue = new ArrayBlockingQueue<>(queueCapacity);

    // 创建安全的线程工厂
    ThreadFactory producerThreadFactory = createThreadFactory("dataforge-producer");
    ThreadFactory consumerThreadFactory = createThreadFactory("dataforge-consumer");

    // 根据执行模式选择 ExecutorService
    ExecutorService producerPool;
    ExecutorService consumerPool;

    if (useVirtualThreads) {
      // 使用虚拟线程（Java 21 特性）
      producerPool = Executors.newVirtualThreadPerTaskExecutor();
      consumerPool = Executors.newVirtualThreadPerTaskExecutor();
    } else {
      // 使用传统平台线程池
      producerPool = Executors.newFixedThreadPool(threads, producerThreadFactory);
      consumerPool = Executors.newSingleThreadExecutor(consumerThreadFactory);
    }

    try {
      // 启动消费者
      CompletableFuture<Void> consumerFuture = startConsumer(resultQueue, outputStrategy, total);

      // 启动生产者
      List<CompletableFuture<Void>> producerFutures =
          startProducers(config, resultQueue, total, threads, producerPool);

      // 等待所有生产者完成
      CompletableFuture<Void> allProducers =
          CompletableFuture.allOf(producerFutures.toArray(new CompletableFuture[0]));

      allProducers.join();

      // 发送结束信号（使用非阻塞方法避免InterruptedException）
      if (!resultQueue.offer(new HashMap<String, Object>())) { // 发送一个空Map作为结束信号
        throw new GenerationException("Failed to send poison pill to consumer queue");
      }

      // 等待消费者完成
      consumerFuture.join();

    } catch (CompletionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof DataForgeException) {
        throw (DataForgeException) cause;
      }
      throw new GenerationException("Concurrent data generation failed", cause);
    } catch (Exception e) {
      throw new GenerationException("Concurrent data generation failed", e);
    } finally {
      // 根据执行模式选择关闭策略
      if (useVirtualThreads) {
        // 虚拟线程池直接关闭（更快速）
        producerPool.close();
        consumerPool.close();
        logger.info("Virtual thread executors closed");
      } else {
        // 平台线程池需要优雅关闭
        shutdownExecutorGracefully(producerPool, "Producer", 30, TimeUnit.SECONDS);
        shutdownExecutorGracefully(consumerPool, "Consumer", 15, TimeUnit.SECONDS);
      }
    }
  }

  /** 启动消费者。 */
  private CompletableFuture<Void> startConsumer(
      BlockingQueue<Map<String, Object>> resultQueue,
      OutputStrategy outputStrategy,
      int totalRecords) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            long written = 0;
            int batchSize = calculateOptimalBatchSize(totalRecords);

            while (written < totalRecords) {
              Object item = resultQueue.take();

              // 检查是否是结束信号（空Map）
              if (item instanceof Map && ((Map<?, ?>) item).isEmpty()) {
                break;
              }

              // 添加类型检查，避免ClassCastException
              if (!(item instanceof Map)) {
                logger.error("Unexpected item type in result queue: {}", item.getClass().getName());
                continue;
              }

              // 安全的类型转换，避免ClassCastException
              Map<String, Object> record = convertToTypedMap(item);
              outputStrategy.writeRecord(record);
              written++;

              if (written % batchSize == 0) {
                outputStrategy.flush();
                logger.debug("Written {} records", written);
              }
            }

            logger.debug("Consumer completed, wrote {} records", written);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Consumer was interrupted", e);
          } catch (Exception e) {
            throw new RuntimeException("Consumer failed", e);
          }
        });
  }

  /** 将原始Map转换为类型安全的Map<String, Object> */
  private Map<String, Object> convertToTypedMap(Object item) {
    if (!(item instanceof Map)) {
      throw new IllegalArgumentException("Expected Map but got " + item.getClass().getName());
    }

    Map<?, ?> rawMap = (Map<?, ?>) item;
    Map<String, Object> typedMap = new HashMap<>(rawMap.size());

    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
      if (entry.getKey() instanceof String) {
        typedMap.put((String) entry.getKey(), entry.getValue());
      } else {
        // 如果key不是String，转换为String表示
        typedMap.put(String.valueOf(entry.getKey()), entry.getValue());
      }
    }

    return typedMap;
  }

  /** 启动生产者。 */
  private List<CompletableFuture<Void>> startProducers(
      ForgeConfig config,
      BlockingQueue<Map<String, Object>> resultQueue,
      int total,
      int threads,
      ExecutorService producerPool) {
    List<CompletableFuture<Void>> producerFutures = new ArrayList<>();

    // 计算每个线程的工作负载
    int baseCount = total / threads;
    int remainder = total % threads;
    int startIndex = 0;

    for (int t = 0; t < threads; t++) {
      int count = baseCount + (t < remainder ? 1 : 0);
      if (count <= 0) {
        continue;
      }

      final int start = startIndex;
      final int threadId = t;

      // 为每个线程创建独立的随机数生成器
      Random threadRandom =
          createSafeRandom(config.getSeed() != null ? config.getSeed() + threadId : null);

      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                try {
                  generateRecordsInThread(
                      config.getFields(), resultQueue, start, count, threadRandom);
                } catch (Exception e) {
                  throw new RuntimeException("Producer thread failed", e);
                }
              },
              producerPool);

      producerFutures.add(future);
      startIndex += count;
    }

    return producerFutures;
  }

  /** 在单个线程中生成记录。 */
  private void generateRecordsInThread(
      List<FieldConfigWrapper> fields,
      BlockingQueue<Map<String, Object>> resultQueue,
      int start,
      int count,
      Random random) {
    for (int i = 0; i < count; i++) {
      try (DataForgeContext context = new DataForgeContext()) {
        context.setCurrentRecordIndex(start + i);

        Map<String, Object> record = generateSingleRecord(fields, context, random);

        // 使用非阻塞的offer方法避免InterruptedException
        if (!resultQueue.offer(record)) {
          throw new RuntimeException("Failed to add record to queue, queue is full");
        }

        totalRecordsGenerated.incrementAndGet();

      } catch (Exception e) {
        totalErrors.incrementAndGet();
        logger.error("Failed to generate record in thread at index {}", start + i, e);
        // 继续处理其他记录
      }
    }
  }

  /** 生成单条记录 - 增强版本。 */
  @SuppressWarnings("unchecked")
  private Map<String, Object> generateSingleRecord(
      List<FieldConfigWrapper> fields, DataForgeContext context, Random random) {
    Map<String, Object> record = new HashMap<>(fields.size());

    // 按字段配置顺序生成数据，确保关联性正确处理
    for (FieldConfigWrapper field : fields) {
      try {
        DataGenerator<Object, FieldConfig> generator =
            (DataGenerator<Object, FieldConfig>) generatorFactory.getGenerator(field.getType());

        if (generator == null) {
          logger.warn("No generator found for field type: {}, using null value", field.getType());
          record.put(field.getName(), null);
          continue;
        }

        // 生成字段值
        Object value = generator.generate(field, context);
        record.put(field.getName(), value);

        logger.trace("Generated field: {}={}", field.getName(), value);

      } catch (Exception e) {
        logger.error("Failed to generate field: {}", field.getName(), e);
        // 继续处理其他字段，将当前字段设为null
        record.put(field.getName(), null);
        totalErrors.incrementAndGet();
      }
    }

    return record;
  }

  /** 创建线程安全的随机数生成器。 */
  private Random createSafeRandom(Long seed) {
    return seed != null ? new Random(seed) : ThreadLocalRandom.current();
  }

  /** 计算最优批处理大小。 */
  private int calculateOptimalBatchSize(int totalRecords) {
    if (totalRecords < 100) {
      return 10;
    }
    if (totalRecords < 1000) {
      return 50;
    }
    if (totalRecords < 10000) {
      return 100;
    }
    return 500;
  }

  /** 计算最优队列容量 - 基于字段配置估算。 */
  private int calculateOptimalQueueCapacity(
      int total, int threads, List<FieldConfigWrapper> fields) {
    long availableMemory = Runtime.getRuntime().freeMemory();
    long maxQueuedMemory = availableMemory / 4; // 最多占用25%空闲内存

    // 估算单条记录大小（基于字段配置）
    int estimatedRecordSize = estimateRecordSize(fields);
    logger.debug("Estimated record size: {} bytes", estimatedRecordSize);

    // 基于内存限制的容量计算
    int memoryBasedCapacity;
    if (estimatedRecordSize > 0) {
      memoryBasedCapacity =
          (int) Math.min(Integer.MAX_VALUE, maxQueuedMemory / estimatedRecordSize);
    } else {
      memoryBasedCapacity = Integer.MAX_VALUE; // 无法估算，使用默认上限
    }

    // 基于工作负载的容量计算
    int workloadBasedCapacity = Math.max(1000, total / threads / 10);

    // 取两者较小值，但设置合理下限和上限
    int baseCapacity = Math.min(10_000, Math.max(100, workloadBasedCapacity));
    int finalCapacity = Math.min(memoryBasedCapacity, baseCapacity);

    logger.info(
        "Queue capacity calculation: workload={}, memory={}, final={}",
        workloadBasedCapacity,
        memoryBasedCapacity,
        finalCapacity);

    return finalCapacity;
  }

  /** 估算单条记录大小（基于字段类型）。 */
  private int estimateRecordSize(List<FieldConfigWrapper> fields) {
    return fields.stream()
        .mapToInt(
            f -> {
              switch (f.getType().toLowerCase()) {
                case "uuid":
                  return 36;
                case "idcard":
                  return 18;
                case "email":
                  return 50;
                case "phone":
                  return 15;
                case "name":
                  return 20;
                case "address":
                  return 100;
                case "url":
                  return 80;
                case "json":
                case "yaml":
                  return 500;
                case "longtext":
                case "richtext":
                  return 1000;
                case "binary":
                  return 1024;
                case "timestamp":
                  return 20;
                case "integer":
                  return 11;
                case "decimal":
                  return 20;
                case "boolean":
                  return 5;
                default:
                  return 50; // 默认估算值
              }
            })
        .sum();
  }

  /** 创建安全的线程工厂。 */
  private ThreadFactory createThreadFactory(String namePrefix) {
    return new ThreadFactory() {
      private int counter = 0;

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix + "-" + (++counter));
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler(
            (t, e) -> logger.error("Uncaught exception in thread {}", t.getName(), e));
        return thread;
      }
    };
  }

  /** 优雅关闭线程池 - 增强版本。 */
  private void shutdownExecutorGracefully(
      ExecutorService executor, String name, long timeout, TimeUnit unit) {
    logger.debug("开始关闭 {} 线程池", name);

    executor.shutdown();
    try {
      // 第一阶段：优雅关闭
      if (!executor.awaitTermination(timeout, unit)) {
        logger.warn("{} 线程池在 {} {} 内未能优雅关闭，尝试强制关闭", name, timeout, unit);

        List<Runnable> pendingTasks = executor.shutdownNow();
        if (!pendingTasks.isEmpty()) {
          logger.warn("{} 线程池有 {} 个任务未完成", name, pendingTasks.size());
        }

        // 第二阶段：强制关闭
        long forceTimeout = Math.max(5, unit.toMillis(timeout) / 2);
        if (!executor.awaitTermination(forceTimeout, TimeUnit.MILLISECONDS)) {
          logger.error("{} 线程池强制关闭失败，可能存在资源泄露", name);
        } else {
          logger.info("{} 线程池已强制关闭", name);
        }
      } else {
        logger.info("{} 线程池已优雅关闭", name);
      }
    } catch (InterruptedException e) {
      logger.warn("{} 线程池关闭被中断", name);
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  // ==================== 公共API方法 ====================

  /**
   * 获取可用的数据生成器类型。
   *
   * @return 生成器类型集合
   */
  public Set<String> getAvailableGeneratorTypes() {
    return generatorFactory.getAvailableTypes();
  }

  /**
   * 获取生成器的详细信息。
   *
   * @return 生成器信息映射
   */
  public Map<String, String> getGeneratorInfo() {
    return generatorFactory.getGeneratorInfo();
  }

  /**
   * 获取可用的输出格式。
   *
   * @return 输出格式列表
   */
  public List<OutputConfig.Format> getAvailableOutputFormats() {
    List<OutputConfig.Format> formats = new ArrayList<>();
    for (OutputStrategy strategy : outputStrategies) {
      formats.add(strategy.getSupportedFormat());
    }
    return formats;
  }

  /**
   * 获取服务健康状态和统计信息。
   *
   * @return 健康状态映射
   */
  public Map<String, Object> getHealthStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("totalRecordsGenerated", totalRecordsGenerated.get());
    status.put("totalErrors", totalErrors.get());
    status.put("totalBytesGenerated", totalBytesGenerated.get());
    status.put("availableGenerators", generatorFactory.getGeneratorCount());
    status.put("availableOutputStrategies", outputStrategies.size());
    status.put("availableProcessors", Runtime.getRuntime().availableProcessors());
    status.put("freeMemory", Runtime.getRuntime().freeMemory());
    status.put("maxMemory", Runtime.getRuntime().maxMemory());
    return status;
  }

  /** 重置统计计数器。 */
  public void resetCounters() {
    totalRecordsGenerated.set(0);
    totalErrors.set(0);
    totalBytesGenerated.set(0);
    logger.info("统计计数器已重置");
  }
}
