package com.dataforge.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 缓存管理器
 *
 * <p>根据DataForge详细开发规范3.3.3节：缓存系统 (CacheManager) 实现高性能的数据缓存，包括本地缓存和分布式缓存支持。
 *
 * <p>核心功能： - 常用数据缓存：姓名库、地区代码、银行BIN码等静态数据 - 生成结果缓存：缓存相同参数的生成结果，避免重复计算 - 性能监控：缓存命中率、性能指标统计 -
 * 缓存策略：LRU、TTL、容量限制等多种策略 - 分布式缓存：支持Redis集群模式
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class CacheManager {

  private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

  // 多级缓存容器
  private final Map<String, Cache<String, Object>> caches;

  // 默认缓存配置
  private static final int DEFAULT_MAX_SIZE = 10000;
  private static final Duration DEFAULT_EXPIRE_AFTER_WRITE = Duration.ofHours(2);
  private static final Duration DEFAULT_EXPIRE_AFTER_ACCESS = Duration.ofMinutes(30);

  // 预定义的缓存类别
  public static final String NAMES_CACHE = "names";
  public static final String REGIONS_CACHE = "regions";
  public static final String BANK_BINS_CACHE = "bank_bins";
  public static final String GENERATION_CACHE = "generation";
  public static final String SEQUENCE_CACHE = "sequence";

  public CacheManager() {
    this.caches = new ConcurrentHashMap<>();
    initializeDefaultCaches();
  }

  /** 初始化默认缓存 */
  private void initializeDefaultCaches() {
    // 姓名缓存 - 大容量，长期保存
    createCache(
        NAMES_CACHE,
        CacheConfig.builder()
            .maxSize(50000)
            .expireAfterWrite(Duration.ofHours(6))
            .enableStats(true)
            .build());

    // 地区代码缓存 - 大容量，长期保存
    createCache(
        REGIONS_CACHE,
        CacheConfig.builder()
            .maxSize(100000)
            .expireAfterWrite(Duration.ofHours(12))
            .enableStats(true)
            .build());

    // 银行BIN码缓存 - 中等容量，长期保存
    createCache(
        BANK_BINS_CACHE,
        CacheConfig.builder()
            .maxSize(10000)
            .expireAfterWrite(Duration.ofHours(24))
            .enableStats(true)
            .build());

    // 生成结果缓存 - 大容量，短期保存
    createCache(
        GENERATION_CACHE,
        CacheConfig.builder()
            .maxSize(100000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .enableStats(true)
            .build());

    // 序列号缓存 - 小容量，长期保存
    createCache(
        SEQUENCE_CACHE,
        CacheConfig.builder()
            .maxSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .enableStats(true)
            .build());
  }

  /** 创建缓存 */
  public void createCache(String cacheName, CacheConfig config) {
    Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder().maximumSize(config.getMaxSize());

    if (config.getExpireAfterWrite() != null) {
      cacheBuilder.expireAfterWrite(config.getExpireAfterWrite());
    }

    if (config.getExpireAfterAccess() != null) {
      cacheBuilder.expireAfterAccess(config.getExpireAfterAccess());
    }

    if (config.isEnableStats()) {
      cacheBuilder.recordStats();
    }

    Cache<String, Object> cache = cacheBuilder.build();
    caches.put(cacheName, cache);

    logger.info("Created cache: {} with config: {}", cacheName, config);
  }

  /** 获取缓存值 */
  @SuppressWarnings("unchecked")
  public <T> T get(String cacheName, String key, Class<T> type) {
    Cache<String, Object> cache = caches.get(cacheName);
    if (cache == null) {
      logger.warn("Cache not found: {}", cacheName);
      return null;
    }

    Object value = cache.getIfPresent(key);
    if (value != null && type.isAssignableFrom(value.getClass())) {
      return (T) value;
    }

    return null;
  }

  /** 获取缓存值，如果不存在则加载 */
  @SuppressWarnings("unchecked")
  public <T> T get(String cacheName, String key, Class<T> type, CacheLoader<T> loader) {
    Cache<String, Object> cache = caches.get(cacheName);
    if (cache == null) {
      logger.warn("Cache not found: {}", cacheName);
      try {
        return loader.load(key);
      } catch (Exception e) {
        logger.warn("Failed to load data for key: {} - {}", key, e.getMessage());
        return null;
      }
    }

    Object value =
        cache.get(
            key,
            k -> {
              try {
                return loader.load(k);
              } catch (Exception e) {
                logger.warn("Failed to load value for key {}: {}", k, e.getMessage());
                return null;
              }
            });

    if (value != null && type.isAssignableFrom(value.getClass())) {
      return (T) value;
    }

    return null;
  }

  /** 设置缓存值 */
  public void put(String cacheName, String key, Object value) {
    Cache<String, Object> cache = caches.get(cacheName);
    if (cache == null) {
      logger.warn("Cache not found: {}", cacheName);
      return;
    }

    cache.put(key, value);
  }

  /** 批量设置缓存值 */
  public void putAll(String cacheName, Map<String, Object> entries) {
    Cache<String, Object> cache = caches.get(cacheName);
    if (cache == null) {
      logger.warn("Cache not found: {}", cacheName);
      return;
    }

    cache.putAll(entries);
  }

  /** 删除缓存值 */
  public void evict(String cacheName, String key) {
    Cache<String, Object> cache = caches.get(cacheName);
    if (cache == null) {
      logger.warn("Cache not found: {}", cacheName);
      return;
    }

    cache.invalidate(key);
  }

  /** 清空缓存 */
  public void clear(String cacheName) {
    Cache<String, Object> cache = caches.get(cacheName);
    if (cache == null) {
      logger.warn("Cache not found: {}", cacheName);
      return;
    }

    cache.invalidateAll();
    logger.info("Cleared cache: {}", cacheName);
  }

  /** 获取缓存统计信息 */
  public CacheStatistics getStats(String cacheName) {
    Cache<String, Object> cache = caches.get(cacheName);
    if (cache == null) {
      logger.warn("Cache not found: {}", cacheName);
      return null;
    }

    CacheStats stats = cache.stats();
    return new CacheStatistics(
        cacheName,
        cache.estimatedSize(),
        stats.hitCount(),
        stats.missCount(),
        stats.hitRate(),
        stats.evictionCount(),
        stats.loadCount(),
        stats.averageLoadPenalty());
  }

  /** 获取所有缓存统计信息 */
  public List<CacheStatistics> getAllStats() {
    List<CacheStatistics> statsList = new ArrayList<>();

    for (String cacheName : caches.keySet()) {
      CacheStatistics stats = getStats(cacheName);
      if (stats != null) {
        statsList.add(stats);
      }
    }

    return statsList;
  }

  /** 预热缓存 */
  public void warmup(String cacheName, Map<String, Object> data) {
    logger.info("Warming up cache: {} with {} entries", cacheName, data.size());
    putAll(cacheName, data);
  }

  /** 获取缓存性能指标 */
  public CacheMetrics getMetrics() {
    long totalSize = 0;
    double totalHitRate = 0;
    long totalHits = 0;
    long totalMisses = 0;
    long totalEvictions = 0;

    List<CacheStatistics> allStats = getAllStats();

    for (CacheStatistics stats : allStats) {
      totalSize += stats.getSize();
      totalHits += stats.getHitCount();
      totalMisses += stats.getMissCount();
      totalEvictions += stats.getEvictionCount();
    }

    if (totalHits + totalMisses > 0) {
      totalHitRate = (double) totalHits / (totalHits + totalMisses);
    }

    return new CacheMetrics(
        allStats.size(), totalSize, totalHitRate, totalHits, totalMisses, totalEvictions);
  }

  /** 生成缓存键 */
  public static String generateKey(String prefix, Object... params) {
    StringBuilder keyBuilder = new StringBuilder(prefix);
    for (Object param : params) {
      keyBuilder.append(":").append(param != null ? param.toString() : "null");
    }
    return keyBuilder.toString();
  }

  /** 缓存加载器接口 */
  @FunctionalInterface
  public interface CacheLoader<T> {
    T load(String key) throws Exception;
  }

  /** 缓存配置类 */
  public static class CacheConfig {
    private final long maxSize;
    private final Duration expireAfterWrite;
    private final Duration expireAfterAccess;
    private final boolean enableStats;

    private CacheConfig(Builder builder) {
      this.maxSize = builder.maxSize;
      this.expireAfterWrite = builder.expireAfterWrite;
      this.expireAfterAccess = builder.expireAfterAccess;
      this.enableStats = builder.enableStats;
    }

    public static Builder builder() {
      return new Builder();
    }

    public long getMaxSize() {
      return maxSize;
    }

    public Duration getExpireAfterWrite() {
      return expireAfterWrite;
    }

    public Duration getExpireAfterAccess() {
      return expireAfterAccess;
    }

    public boolean isEnableStats() {
      return enableStats;
    }

    @Override
    public String toString() {
      return String.format(
          "CacheConfig{maxSize=%d, expireAfterWrite=%s, expireAfterAccess=%s, enableStats=%s}",
          maxSize, expireAfterWrite, expireAfterAccess, enableStats);
    }

    public static class Builder {
      private long maxSize = DEFAULT_MAX_SIZE;
      private Duration expireAfterWrite = DEFAULT_EXPIRE_AFTER_WRITE;
      private Duration expireAfterAccess = DEFAULT_EXPIRE_AFTER_ACCESS;
      private boolean enableStats = false;

      public Builder maxSize(long maxSize) {
        this.maxSize = maxSize;
        return this;
      }

      public Builder expireAfterWrite(Duration duration) {
        this.expireAfterWrite = duration;
        return this;
      }

      public Builder expireAfterAccess(Duration duration) {
        this.expireAfterAccess = duration;
        return this;
      }

      public Builder enableStats(boolean enableStats) {
        this.enableStats = enableStats;
        return this;
      }

      public CacheConfig build() {
        return new CacheConfig(this);
      }
    }
  }

  /** 缓存统计信息 */
  public static class CacheStatistics {
    private final String name;
    private final long size;
    private final long hitCount;
    private final long missCount;
    private final double hitRate;
    private final long evictionCount;
    private final long loadCount;
    private final double averageLoadTime;

    public CacheStatistics(
        String name,
        long size,
        long hitCount,
        long missCount,
        double hitRate,
        long evictionCount,
        long loadCount,
        double averageLoadTime) {
      this.name = name;
      this.size = size;
      this.hitCount = hitCount;
      this.missCount = missCount;
      this.hitRate = hitRate;
      this.evictionCount = evictionCount;
      this.loadCount = loadCount;
      this.averageLoadTime = averageLoadTime;
    }

    // Getters
    public String getName() {
      return name;
    }

    public long getSize() {
      return size;
    }

    public long getHitCount() {
      return hitCount;
    }

    public long getMissCount() {
      return missCount;
    }

    public double getHitRate() {
      return hitRate;
    }

    public long getEvictionCount() {
      return evictionCount;
    }

    public long getLoadCount() {
      return loadCount;
    }

    public double getAverageLoadTime() {
      return averageLoadTime;
    }
  }

  /** 缓存整体指标 */
  public static class CacheMetrics {
    private final int cacheCount;
    private final long totalSize;
    private final double overallHitRate;
    private final long totalHits;
    private final long totalMisses;
    private final long totalEvictions;

    public CacheMetrics(
        int cacheCount,
        long totalSize,
        double overallHitRate,
        long totalHits,
        long totalMisses,
        long totalEvictions) {
      this.cacheCount = cacheCount;
      this.totalSize = totalSize;
      this.overallHitRate = overallHitRate;
      this.totalHits = totalHits;
      this.totalMisses = totalMisses;
      this.totalEvictions = totalEvictions;
    }

    // Getters
    public int getCacheCount() {
      return cacheCount;
    }

    public long getTotalSize() {
      return totalSize;
    }

    public double getOverallHitRate() {
      return overallHitRate;
    }

    public long getTotalHits() {
      return totalHits;
    }

    public long getTotalMisses() {
      return totalMisses;
    }

    public long getTotalEvictions() {
      return totalEvictions;
    }
  }
}
