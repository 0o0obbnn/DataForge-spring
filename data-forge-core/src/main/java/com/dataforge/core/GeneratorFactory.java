package com.dataforge.core;

import com.dataforge.core.monitoring.MonitorPerformance;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.service.ConfigurationException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据生成器工厂类。
 *
 * <p>使用Java ServiceLoader (SPI) 机制动态加载所有DataGenerator实现。 该工厂负责管理和提供数据生成器实例，支持运行时发现和注册新的生成器。
 *
 * <p>工厂采用单例模式，确保生成器的统一管理和高效访问。 所有注册的生成器都会被缓存，避免重复创建实例。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class GeneratorFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorFactory.class);

  /** 生成器缓存，键为类型名称，值为生成器实例。 使用ConcurrentHashMap确保线程安全。 */
  private final Map<String, DataGenerator<?, ?>> generators = new ConcurrentHashMap<>();

  /** 生成器类信息（用于原型模式创建新实例）。 */
  private final Map<String, Class<? extends DataGenerator<?, ?>>> generatorClasses =
      new ConcurrentHashMap<>();

  /** 生成器状态类型映射（true = 无状态，false = 有状态）。 */
  private final Map<String, Boolean> generatorStateType = new ConcurrentHashMap<>();

  /** 生成器类型到优先级的映射，用于处理重复类型的情况。 */
  private final Map<String, Integer> generatorPriorities = new ConcurrentHashMap<>();

  /** 生成器使用统计，用于性能监控和缓存优化。 */
  private final Map<String, AtomicLong> usageStats = new ConcurrentHashMap<>();

  /** 初始化失败的生成器列表，用于错误报告和调试。 */
  private final Map<String, String> failedGenerators = new ConcurrentHashMap<>();

  /** 读写锁，用于保护初始化过程和动态注册操作。 */
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  /** 是否已初始化标志。 */
  private volatile boolean initialized = false;

  /** 初始化开始时间，用于性能监控。 */
  private volatile long initStartTime = 0;

  /** 初始化完成时间，用于性能监控。 */
  private volatile long initEndTime = 0;

  /** 构造函数，自动初始化生成器。 */
  public GeneratorFactory() {
    initialize();
  }

  /**
   * 初始化生成器工厂，加载所有可用的数据生成器。
   *
   * <p>该方法使用ServiceLoader机制扫描classpath中所有实现了 DataGenerator接口的类，并将其注册到工厂中。
   *
   * <p>增强特性：
   *
   * <ul>
   *   <li>详细的错误报告和异常处理
   *   <li>生成器优先级支持
   *   <li>初始化性能监控
   *   <li>失败生成器追踪
   * </ul>
   *
   * @throws ConfigurationException 当关键生成器加载失败时
   */
  @MonitorPerformance(value = "generator.factory.initialize", slowThreshold = 2000)
  private synchronized void initialize() throws ConfigurationException {
    if (initialized) {
      return;
    }

    lock.writeLock().lock();
    try {
      // 双重检查
      if (initialized) {
        return;
      }

      initStartTime = System.currentTimeMillis();
      LOGGER.info("Initializing GeneratorFactory...");

      // 清理之前的状态
      generators.clear();
      generatorPriorities.clear();
      usageStats.clear();
      failedGenerators.clear();

      // 使用ServiceLoader加载所有DataGenerator实现
      @SuppressWarnings("rawtypes")
      ServiceLoader<DataGenerator> serviceLoader = ServiceLoader.load(DataGenerator.class);

      int loadedCount = 0;
      int failedCount = 0;
      int duplicateCount = 0;

      for (DataGenerator<?, ?> generator : serviceLoader) {
        try {
          String result = registerGeneratorInternal(generator, false);
          switch (result) {
            case "LOADED":
              loadedCount++;
              break;
            case "DUPLICATE":
              duplicateCount++;
              break;
            case "FAILED":
              failedCount++;
              break;
          }
        } catch (Exception e) {
          failedCount++;
          String className = generator != null ? generator.getClass().getName() : "Unknown";
          String errorMsg = "Failed to register generator: " + e.getMessage();
          failedGenerators.put(className, errorMsg);
          LOGGER.error("Failed to register generator: {}", className, e);
        }
      }

      initEndTime = System.currentTimeMillis();
      long initDuration = initEndTime - initStartTime;

      initialized = true;

      // 记录初始化结果
      LOGGER.info(
          "GeneratorFactory initialized successfully in {}ms. Statistics: loaded={}, failed={},"
              + " duplicates={}, total={}. Available types: {}",
          initDuration,
          loadedCount,
          failedCount,
          duplicateCount,
          generators.size(),
          generators.keySet());

      // 如果有失败的生成器，记录警告
      if (failedCount > 0) {
        LOGGER.warn("Some generators failed to load. Failed generators: {}", failedGenerators);
      }

      // 如果没有加载到任何生成器，抛出异常
      if (loadedCount == 0) {
        throw new ConfigurationException(
            "No generators were successfully loaded. This may indicate a classpath or SPI"
                + " configuration issue.");
      }

    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * 内部生成器注册方法，支持优先级和重复检测。
   *
   * @param generator 要注册的生成器
   * @param allowOverride 是否允许覆盖已存在的生成器
   * @return 注册结果：LOADED/DUPLICATE/FAILED
   */
  private String registerGeneratorInternal(DataGenerator<?, ?> generator, boolean allowOverride) {
    if (generator == null) {
      return "FAILED";
    }

    try {
      String type = generator.getType();
      if (type == null || type.trim().isEmpty()) {
        LOGGER.warn(
            "Generator {} returned null or empty type, skipping", generator.getClass().getName());
        return "FAILED";
      }

      type = type.trim();
      String className = generator.getClass().getName();

      // 检查是否已存在相同类型的生成器
      if (generators.containsKey(type)) {
        DataGenerator<?, ?> existing = generators.get(type);

        if (!allowOverride) {
          LOGGER.warn(
              "Duplicate generator type '{}' found. Existing: {}, New: {}. Keeping existing.",
              type,
              existing.getClass().getName(),
              className);
          return "DUPLICATE";
        }

        // 处理优先级逻辑
        int existingPriority = generatorPriorities.getOrDefault(type, 0);
        int newPriority = getGeneratorPriority(generator);

        if (newPriority <= existingPriority) {
          LOGGER.info(
              "Generator type '{}' already exists with higher priority. Existing: {} (priority={}),"
                  + " New: {} (priority={}). Keeping existing.",
              type,
              existing.getClass().getName(),
              existingPriority,
              className,
              newPriority);
          return "DUPLICATE";
        }

        LOGGER.info(
            "Replacing generator type '{}' with higher priority. Old: {} (priority={}), New: {}"
                + " (priority={})",
            type,
            existing.getClass().getName(),
            existingPriority,
            className,
            newPriority);
      }

      // 注册生成器
      generators.put(type, generator);
      generatorPriorities.put(type, getGeneratorPriority(generator));
      usageStats.put(type, new AtomicLong(0));

      // 保存生成器类信息和状态类型
      @SuppressWarnings("unchecked")
      Class<? extends DataGenerator<?, ?>> generatorClass =
          (Class<? extends DataGenerator<?, ?>>) generator.getClass();
      generatorClasses.put(type, generatorClass);
      generatorStateType.put(type, generator.isStateless());

      LOGGER.debug(
          "Registered generator: type='{}', class='{}', priority={}",
          type,
          className,
          getGeneratorPriority(generator));

      return "LOADED";

    } catch (Exception e) {
      LOGGER.error("Failed to register generator: {}", generator.getClass().getName(), e);
      return "FAILED";
    }
  }

  /**
   * 获取生成器的优先级。 可以通过注解或配置文件来定义优先级。
   *
   * @param generator 生成器实例
   * @return 优先级（数值越大优先级越高）
   */
  private int getGeneratorPriority(DataGenerator<?, ?> generator) {
    // 检查是否有@Priority注解
    if (generator.getClass().isAnnotationPresent(Priority.class)) {
      return generator.getClass().getAnnotation(Priority.class).value();
    }

    // 检查配置文件中的优先级设置
    String type = generator.getType();
    if (type != null) {
      String priorityStr = System.getProperty("generator.priority." + type);
      if (priorityStr != null) {
        try {
          return Integer.parseInt(priorityStr);
        } catch (NumberFormatException e) {
          // 忽略无效的配置值
          LOGGER.debug(
              "Invalid priority configuration for generator type '{}': {}", type, priorityStr, e);
        }
      }
    }

    // 默认优先级为 0
    return 0;
  }

  /**
   * 根据类型获取数据生成器。
   *
   * <p>增强特性：
   *
   * <ul>
   *   <li>使用统计记录
   *   <li>读锁保护，提高并发性能
   *   <li>更好的错误信息
   * </ul>
   *
   * @param type 数据类型标识符
   * @return 对应的数据生成器，如果不存在返回null
   * @throws IllegalArgumentException 当type为null或空字符串时
   */
  public DataGenerator<?, ?> getGenerator(String type) {
    if (type == null || type.trim().isEmpty()) {
      throw new IllegalArgumentException("Generator type cannot be null or empty");
    }

    String normalizedType = type.trim();

    lock.readLock().lock();
    try {
      DataGenerator<?, ?> generator = generators.get(normalizedType);

      if (generator != null) {
        // 记录使用统计
        AtomicLong counter = usageStats.get(normalizedType);
        if (counter != null) {
          counter.incrementAndGet();
        }

        // 检查生成器状态类型
        Boolean isStateless = generatorStateType.get(normalizedType);
        if (isStateless != null && !isStateless) {
          // 有状态生成器：返回新实例（原型模式）
          Class<? extends DataGenerator<?, ?>> generatorClass =
              generatorClasses.get(normalizedType);
          if (generatorClass != null) {
            try {
              generator = generatorClass.getDeclaredConstructor().newInstance();
              LOGGER.trace(
                  "Created new instance for stateful generator: {}, usage count: {}",
                  normalizedType,
                  counter != null ? counter.get() : 0);
            } catch (Exception e) {
              LOGGER.error(
                  "Failed to create new instance for stateful generator: {}", normalizedType, e);
              // 返回缓存的实例作为降级方案
              generator = generators.get(normalizedType);
            }
          }
        }

        LOGGER.trace(
            "Retrieved generator for type: {}, stateless={}, usage count: {}",
            normalizedType,
            isStateless,
            counter != null ? counter.get() : 0);
      } else {
        LOGGER.debug(
            "No generator found for type: {}. Available types: {}",
            normalizedType,
            generators.keySet());
      }

      return generator;

    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * 检查是否存在指定类型的生成器。
   *
   * @param type 数据类型标识符
   * @return 如果存在返回true，否则返回false
   */
  public boolean hasGenerator(String type) {
    if (type == null || type.trim().isEmpty()) {
      return false;
    }
    return generators.containsKey(type.trim());
  }

  /**
   * 获取所有已注册的生成器类型。
   *
   * @return 生成器类型集合的副本
   */
  public java.util.Set<String> getAvailableTypes() {
    return new java.util.HashSet<>(generators.keySet());
  }

  /**
   * 获取已注册的生成器数量。
   *
   * @return 生成器数量
   */
  public int getGeneratorCount() {
    return generators.size();
  }

  /**
   * 手动注册数据生成器。
   *
   * <p>该方法主要用于测试或动态注册生成器的场景。 在正常情况下，生成器应该通过SPI机制自动发现和注册。
   *
   * @param generator 要注册的数据生成器
   * @throws IllegalArgumentException 当generator为null或其类型为null/空字符串时
   * @throws IllegalStateException 当指定类型的生成器已存在时
   */
  public void registerGenerator(DataGenerator<?, ?> generator) {
    if (generator == null) {
      throw new IllegalArgumentException("Generator cannot be null");
    }

    String type = generator.getType();
    if (type == null || type.trim().isEmpty()) {
      throw new IllegalArgumentException("Generator type cannot be null or empty");
    }

    if (generators.containsKey(type)) {
      throw new IllegalStateException("Generator for type '" + type + "' already exists");
    }

    generators.put(type, generator);
    LOGGER.info(
        "Manually registered generator: type='{}', class='{}'",
        type,
        generator.getClass().getName());
  }

  /**
   * 注销指定类型的数据生成器。
   *
   * <p>该方法主要用于测试或动态管理生成器的场景。
   *
   * @param type 要注销的生成器类型
   * @return 被注销的生成器，如果不存在返回null
   */
  public DataGenerator<?, ?> unregisterGenerator(String type) {
    if (type == null || type.trim().isEmpty()) {
      return null;
    }

    DataGenerator<?, ?> removed = generators.remove(type.trim());
    if (removed != null) {
      LOGGER.info(
          "Unregistered generator: type='{}', class='{}'", type, removed.getClass().getName());
    }

    return removed;
  }

  /**
   * 重新初始化生成器工厂。
   *
   * <p>清除所有已注册的生成器，重新扫描和加载。 该方法主要用于开发和测试场景。
   */
  public synchronized void reinitialize() {
    LOGGER.info("Reinitializing GeneratorFactory...");
    generators.clear();
    initialized = false;
    initialize();
  }

  /**
   * 获取生成器的详细信息。
   *
   * @return 包含所有生成器信息的映射
   */
  public Map<String, String> getGeneratorInfo() {
    lock.readLock().lock();
    try {
      Map<String, String> info = new HashMap<>();
      for (Map.Entry<String, DataGenerator<?, ?>> entry : generators.entrySet()) {
        DataGenerator<?, ?> generator = entry.getValue();
        String type = entry.getKey();
        AtomicLong usageCount = usageStats.get(type);
        int priority = generatorPriorities.getOrDefault(type, 0);

        info.put(
            type,
            String.format(
                "%s - %s (priority=%d, usage=%d)",
                generator.getClass().getSimpleName(),
                generator.getDescription(),
                priority,
                usageCount != null ? usageCount.get() : 0));
      }
      return info;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * 获取生成器使用统计信息。
   *
   * @return 包含使用次数的映射
   */
  public Map<String, Long> getUsageStatistics() {
    lock.readLock().lock();
    try {
      Map<String, Long> stats = new HashMap<>();
      for (Map.Entry<String, AtomicLong> entry : usageStats.entrySet()) {
        stats.put(entry.getKey(), entry.getValue().get());
      }
      return stats;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * 获取初始化统计信息。
   *
   * @return 初始化统计信息
   */
  public Map<String, Object> getInitializationStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("initialized", initialized);
    stats.put("initStartTime", initStartTime);
    stats.put("initEndTime", initEndTime);
    stats.put("initializationTime", initEndTime - initStartTime); // 别名，用于测试兼容
    stats.put("initDurationMs", initEndTime - initStartTime);
    stats.put("generatorCount", generators.size()); // 别名，用于测试兼容
    stats.put("loadedGeneratorCount", generators.size());
    stats.put("successfulCount", generators.size()); // 别名，用于测试兼容
    stats.put("failedCount", failedGenerators.size()); // 别名，用于测试兼容
    stats.put("failedGeneratorCount", failedGenerators.size());
    stats.put("failedGenerators", new HashMap<>(failedGenerators));
    return stats;
  }

  /**
   * 获取最受欢迎的生成器类型（按使用次数排序）。
   *
   * @param limit 返回的最大数量
   * @return 排序后的生成器类型列表
   */
  public java.util.List<String> getMostUsedGeneratorTypes(int limit) {
    lock.readLock().lock();
    try {
      return usageStats.entrySet().stream()
          .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
          .limit(Math.max(1, limit))
          .map(Map.Entry::getKey)
          .collect(java.util.stream.Collectors.toList());
    } finally {
      lock.readLock().unlock();
    }
  }

  /** 重置使用统计。 */
  public void resetUsageStatistics() {
    lock.writeLock().lock();
    try {
      usageStats.values().forEach(counter -> counter.set(0));
      LOGGER.info("Usage statistics reset");
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * 检查工厂是否已初始化。
   *
   * @return 如果已初始化返回true，否则返回false
   */
  public boolean isInitialized() {
    return initialized;
  }

  /**
   * 获取生成器使用统计信息（别名方法）。
   *
   * @return 包含使用次数的映射
   */
  public Map<String, Long> getUsageStats() {
    return getUsageStatistics();
  }

  /**
   * 检查工厂健康状态。
   *
   * @return 健康检查结果
   */
  public Map<String, Object> healthCheck() {
    Map<String, Object> health = new HashMap<>();

    try {
      lock.readLock().lock();
      try {
        health.put("status", initialized ? "UP" : "DOWN");
        health.put("generatorCount", generators.size());
        health.put("availableTypes", generators.keySet());
        health.put("failedGenerators", failedGenerators.size());

        if (!failedGenerators.isEmpty()) {
          health.put("failedGeneratorDetails", failedGenerators);
        }

        // 检查是否有核心生成器
        String[] coreTypes = {"uuid", "name", "phone", "email", "boolean"};
        java.util.List<String> missingCoreTypes = new java.util.ArrayList<>();
        for (String coreType : coreTypes) {
          if (!generators.containsKey(coreType)) {
            missingCoreTypes.add(coreType);
          }
        }

        if (!missingCoreTypes.isEmpty()) {
          health.put("missingCoreGenerators", missingCoreTypes);
          health.put("status", "DEGRADED");
        }

      } finally {
        lock.readLock().unlock();
      }

    } catch (Exception e) {
      health.put("status", "ERROR");
      health.put("error", e.getMessage());
      LOGGER.error("Health check failed", e);
    }

    return health;
  }

  @Override
  public String toString() {
    lock.readLock().lock();
    try {
      long totalUsage = usageStats.values().stream().mapToLong(AtomicLong::get).sum();

      return String.format(
          "GeneratorFactory{initialized=%s, generatorCount=%d, types=%s, totalUsage=%d,"
              + " failedCount=%d, initDurationMs=%d}",
          initialized,
          generators.size(),
          generators.keySet(),
          totalUsage,
          failedGenerators.size(),
          initEndTime - initStartTime);
    } finally {
      lock.readLock().unlock();
    }
  }
}
