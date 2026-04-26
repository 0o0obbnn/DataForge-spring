package com.dataforge.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类。
 *
 * <p>为DataForge提供高性能的缓存支持，包括生成器缓存、配置缓存等。 使用Caffeine作为缓存实现，提供出色的性能和内存效率。
 *
 * <p><strong>缓存策略：</strong>
 *
 * <ul>
 *   <li>生成器缓存：缓存已创建的生成器实例，避免重复实例化
 *   <li>配置缓存：缓存解析后的配置对象，提高配置加载性能
 *   <li>元数据缓存：缓存字段元数据和验证结果
 *   <li>结果缓存：缓存小数据量的生成结果
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "dataforge.cache")
public class CacheConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(CacheConfiguration.class);

  /** 是否启用缓存。 */
  private boolean enabled = true;

  /** 缓存TTL（秒）。 */
  private int ttl = 300;

  // 缓存配置参数
  private int maxGeneratorCacheSize = 1000;
  private int maxConfigCacheSize = 500;
  private int maxMetadataCacheSize = 2000;
  private int maxResultCacheSize = 100;

  private Duration generatorCacheExpiration = Duration.ofHours(2);
  private Duration configCacheExpiration = Duration.ofHours(1);
  private Duration metadataCacheExpiration = Duration.ofMinutes(30);
  private Duration resultCacheExpiration = Duration.ofMinutes(10);

  /**
   * 生成器缓存。
   *
   * <p>缓存生成器实例，避免重复创建和初始化开销。
   */
  @Bean
  public Cache<String, Object> generatorCache() {
    return Caffeine.newBuilder()
        .maximumSize(maxGeneratorCacheSize)
        .expireAfterWrite(generatorCacheExpiration)
        .recordStats()
        .removalListener(new CacheRemovalListener("GeneratorCache"))
        .build();
  }

  /**
   * 配置缓存。
   *
   * <p>缓存解析后的配置对象，提高配置加载性能。
   */
  @Bean
  public Cache<String, Object> configCache() {
    return Caffeine.newBuilder()
        .maximumSize(maxConfigCacheSize)
        .expireAfterWrite(configCacheExpiration)
        .recordStats()
        .removalListener(new CacheRemovalListener("ConfigCache"))
        .build();
  }

  /**
   * 元数据缓存。
   *
   * <p>缓存字段元数据、验证结果等信息。
   */
  @Bean
  public Cache<String, Object> metadataCache() {
    return Caffeine.newBuilder()
        .maximumSize(maxMetadataCacheSize)
        .expireAfterAccess(metadataCacheExpiration)
        .recordStats()
        .removalListener(new CacheRemovalListener("MetadataCache"))
        .build();
  }

  /**
   * 结果缓存。
   *
   * <p>缓存小数据量的生成结果，用于重复请求优化。
   */
  @Bean
  public Cache<String, Object> resultCache() {
    return Caffeine.newBuilder()
        .expireAfterWrite(resultCacheExpiration)
        .recordStats()
        .removalListener(new CacheRemovalListener("ResultCache"))
        .weigher(
            (String key, Object value) -> {
              // 根据对象大小计算权重
              return estimateObjectSize(value);
            })
        .maximumWeight(50 * 1024 * 1024) // 50MB最大缓存大小
        .build();
  }

  /** 缓存移除监听器。 */
  private static class CacheRemovalListener implements RemovalListener<String, Object> {
    private final String cacheName;

    public CacheRemovalListener(String cacheName) {
      this.cacheName = cacheName;
    }

    @Override
    public void onRemoval(String key, Object value, RemovalCause cause) {
      if (cause.wasEvicted()) {
        logger.debug("{} cache entry evicted: key={}, cause={}", cacheName, key, cause);
      } else {
        logger.trace("{} cache entry removed: key={}, cause={}", cacheName, key, cause);
      }
    }
  }

  /** 估算对象大小（简化实现）。 */
  private int estimateObjectSize(Object obj) {
    if (obj == null) return 0;

    if (obj instanceof String) {
      return ((String) obj).length() * 2; // 每个字符2字节
    } else if (obj instanceof byte[]) {
      return ((byte[]) obj).length;
    } else {
      // 其他对象的粗略估算
      return 1024; // 默认1KB
    }
  }

  // Getters and Setters for configuration properties

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getTtl() {
    return ttl;
  }

  public void setTtl(int ttl) {
    this.ttl = ttl;
  }

  public int getMaxGeneratorCacheSize() {
    return maxGeneratorCacheSize;
  }

  public void setMaxGeneratorCacheSize(int maxGeneratorCacheSize) {
    this.maxGeneratorCacheSize = maxGeneratorCacheSize;
  }

  public int getMaxConfigCacheSize() {
    return maxConfigCacheSize;
  }

  public void setMaxConfigCacheSize(int maxConfigCacheSize) {
    this.maxConfigCacheSize = maxConfigCacheSize;
  }

  public int getMaxMetadataCacheSize() {
    return maxMetadataCacheSize;
  }

  public void setMaxMetadataCacheSize(int maxMetadataCacheSize) {
    this.maxMetadataCacheSize = maxMetadataCacheSize;
  }

  public int getMaxResultCacheSize() {
    return maxResultCacheSize;
  }

  public void setMaxResultCacheSize(int maxResultCacheSize) {
    this.maxResultCacheSize = maxResultCacheSize;
  }

  public Duration getGeneratorCacheExpiration() {
    return generatorCacheExpiration;
  }

  public void setGeneratorCacheExpiration(Duration generatorCacheExpiration) {
    this.generatorCacheExpiration = generatorCacheExpiration;
  }

  public Duration getConfigCacheExpiration() {
    return configCacheExpiration;
  }

  public void setConfigCacheExpiration(Duration configCacheExpiration) {
    this.configCacheExpiration = configCacheExpiration;
  }

  public Duration getMetadataCacheExpiration() {
    return metadataCacheExpiration;
  }

  public void setMetadataCacheExpiration(Duration metadataCacheExpiration) {
    this.metadataCacheExpiration = metadataCacheExpiration;
  }

  public Duration getResultCacheExpiration() {
    return resultCacheExpiration;
  }

  public void setResultCacheExpiration(Duration resultCacheExpiration) {
    this.resultCacheExpiration = resultCacheExpiration;
  }
}
