package com.dataforge.web.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * 多级缓存管理器，结合本地缓存（Caffeine）和分布式缓存（Redis）。
 *
 * <p>缓存策略： 1. 优先从本地缓存获取数据 2. 本地缓存未命中时，从Redis缓存获取数据 3. Redis缓存未命中时，从数据源获取数据 4.
 * 从数据源获取到数据后，同时更新本地缓存和Redis缓存
 */
@Component
public class MultiLevelCacheManager {

  private final RedisTemplate<String, Object> redisTemplate;

  // 本地缓存实例
  private final Cache<String, Object> localCache;

  // 本地缓存默认过期时间
  private static final Duration LOCAL_CACHE_EXPIRY = Duration.ofMinutes(10);

  // Redis缓存默认过期时间（秒）
  private static final long REDIS_CACHE_EXPIRY = 3600;

  // 本地缓存最大大小
  private static final int LOCAL_CACHE_MAX_SIZE = 1000;

  /** 构造函数，初始化本地缓存；Redis 可选，为 null 时仅使用本地缓存。 */
  public MultiLevelCacheManager(
      @Autowired(required = false) @Nullable RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
    this.localCache =
        Caffeine.newBuilder()
            .maximumSize(LOCAL_CACHE_MAX_SIZE)
            .expireAfterWrite(LOCAL_CACHE_EXPIRY)
            .recordStats() // 启用统计信息记录
            .build();
  }

  /**
   * 获取缓存数据。
   *
   * @param key 缓存键
   * @param <T> 缓存值类型
   * @return 缓存值，如果不存在则返回null
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T get(String key) {
    // 1. 从本地缓存获取
    T value = (T) localCache.getIfPresent(key);
    if (value != null) {
      return value;
    }

    // 2. 从Redis缓存获取（如果可用）
    if (redisTemplate != null) {
      value = (T) redisTemplate.opsForValue().get(key);
      if (value != null) {
        // 更新本地缓存
        localCache.put(key, value);
      }
    }

    return value;
  }

  /**
   * 设置缓存数据。
   *
   * @param key 缓存键
   * @param value 缓存值
   */
  public void put(String key, Object value) {
    put(key, value, REDIS_CACHE_EXPIRY);
  }

  /**
   * 设置缓存数据，指定过期时间。
   *
   * @param key 缓存键
   * @param value 缓存值（可以为null）
   * @param expireSeconds Redis缓存过期时间（秒）
   */
  public void put(String key, @Nullable Object value, long expireSeconds) {
    // 1. 更新本地缓存
    localCache.put(key, value);

    // 2. 更新Redis缓存（如果可用）
    if (redisTemplate != null) {
      redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }
  }

  /**
   * 删除缓存数据。
   *
   * @param key 缓存键
   */
  public void evict(String key) {
    // 1. 删除本地缓存
    localCache.invalidate(key);

    // 2. 删除Redis缓存（如果可用）
    if (redisTemplate != null) {
      redisTemplate.delete(key);
    }
  }

  /** 清空所有缓存。 */
  public void clear() {
    // 1. 清空本地缓存
    localCache.invalidateAll();

    // 2. 清空Redis缓存（如果可用）
    if (redisTemplate != null) {
      redisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().flushDb();
    }
  }

  /**
   * 检查缓存中是否存在指定的键。
   *
   * @param key 缓存键
   * @return 如果存在则返回true，否则返回false
   */
  public boolean containsKey(String key) {
    // 1. 检查本地缓存
    if (localCache.getIfPresent(key) != null) {
      return true;
    }

    // 2. 检查Redis缓存（如果可用）
    if (redisTemplate != null) {
      return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    return false;
  }

  /**
   * 获取本地缓存统计信息。
   *
   * @return Map<String, Object> 本地缓存统计信息
   */
  public Map<String, Object> getLocalCacheStats() {
    Map<String, Object> stats = new HashMap<>();
    CacheStats cacheStats = localCache.stats();

    stats.put("size", localCache.estimatedSize());
    stats.put("maxSize", LOCAL_CACHE_MAX_SIZE);
    stats.put("hitCount", cacheStats.hitCount());
    stats.put("missCount", cacheStats.missCount());
    stats.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));
    stats.put("evictionCount", cacheStats.evictionCount());
    stats.put("loadSuccessCount", cacheStats.loadSuccessCount());
    stats.put("loadFailureCount", cacheStats.loadFailureCount());
    stats.put("averageLoadPenalty", String.format("%.2f ns", cacheStats.averageLoadPenalty()));

    return stats;
  }

  /**
   * 获取多级缓存的整体统计信息。
   *
   * @return Map<String, Object> 多级缓存统计信息
   */
  public Map<String, Object> getMultiLevelCacheStats() {
    Map<String, Object> stats = new HashMap<>();

    // 本地缓存层统计
    stats.put("localCache", getLocalCacheStats());

    // Redis缓存层配置信息
    Map<String, Object> redisInfo = new HashMap<>();
    redisInfo.put("defaultExpiry", REDIS_CACHE_EXPIRY + " seconds");
    redisInfo.put("type", "distributed");
    stats.put("redisCache", redisInfo);

    return stats;
  }
}
