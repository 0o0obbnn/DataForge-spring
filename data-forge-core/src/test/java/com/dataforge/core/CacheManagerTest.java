package com.dataforge.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.dataforge.core.CacheManager.CacheConfig;
import com.dataforge.core.CacheManager.CacheLoader;
import com.dataforge.core.CacheManager.CacheMetrics;
import com.dataforge.core.CacheManager.CacheStatistics;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CacheManager 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("CacheManager 测试")
class CacheManagerTest {

  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    cacheManager = new CacheManager();
  }

  @Nested
  @DisplayName("基本缓存操作测试")
  class BasicCacheOperationsTests {

    @Test
    @DisplayName("存储和获取缓存值应成功")
    void putAndGetShouldSucceed() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      String key = "testKey";
      String value = "testValue";

      // When
      cacheManager.put(cacheName, key, value);
      String result = cacheManager.get(cacheName, key, String.class);

      // Then
      assertThat(result).isEqualTo(value);
    }

    @Test
    @DisplayName("获取不存在的缓存应返回null")
    void getFromNonExistentCacheShouldReturnNull() {
      // When
      String result = cacheManager.get("nonExistentCache", "key", String.class);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("获取不存在的键应返回null")
    void getNonExistentKeyShouldReturnNull() {
      // When
      String result = cacheManager.get(CacheManager.NAMES_CACHE, "nonExistentKey", String.class);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("类型不匹配的获取应返回null")
    void getWithMismatchedTypeShouldReturnNull() {
      // Given
      cacheManager.put(CacheManager.NAMES_CACHE, "key", 123);

      // When
      String result = cacheManager.get(CacheManager.NAMES_CACHE, "key", String.class);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("删除缓存值应成功")
    void evictShouldRemoveValue() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      String key = "evictKey";
      cacheManager.put(cacheName, key, "value");

      // When
      cacheManager.evict(cacheName, key);
      String result = cacheManager.get(cacheName, key, String.class);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("清空缓存应移除所有值")
    void clearShouldRemoveAllValues() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      cacheManager.put(cacheName, "key1", "value1");
      cacheManager.put(cacheName, "key2", "value2");

      // When
      cacheManager.clear(cacheName);

      // Then
      assertThat(cacheManager.get(cacheName, "key1", String.class)).isNull();
      assertThat(cacheManager.get(cacheName, "key2", String.class)).isNull();
    }
  }

  @Nested
  @DisplayName("批量操作测试")
  class BatchOperationsTests {

    @Test
    @DisplayName("批量存储缓存值应成功")
    void putAllShouldSucceed() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      Map<String, Object> entries = new HashMap<>();
      entries.put("key1", "value1");
      entries.put("key2", "value2");
      entries.put("key3", "value3");

      // When
      cacheManager.putAll(cacheName, entries);

      // Then
      assertThat(cacheManager.get(cacheName, "key1", String.class)).isEqualTo("value1");
      assertThat(cacheManager.get(cacheName, "key2", String.class)).isEqualTo("value2");
      assertThat(cacheManager.get(cacheName, "key3", String.class)).isEqualTo("value3");
    }

    @Test
    @DisplayName("缓存预热应成功")
    void warmupShouldSucceed() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      Map<String, Object> data = new HashMap<>();
      data.put("warmup1", "value1");
      data.put("warmup2", "value2");

      // When
      cacheManager.warmup(cacheName, data);

      // Then
      assertThat(cacheManager.get(cacheName, "warmup1", String.class)).isEqualTo("value1");
      assertThat(cacheManager.get(cacheName, "warmup2", String.class)).isEqualTo("value2");
    }
  }

  @Nested
  @DisplayName("缓存加载器测试")
  class CacheLoaderTests {

    @Test
    @DisplayName("使用加载器获取缓存值应成功")
    void getWithLoaderShouldSucceed() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      String key = "loaderKey";
      CacheLoader<String> loader = k -> "loadedValue";

      // When
      String result = cacheManager.get(cacheName, key, String.class, loader);

      // Then
      assertThat(result).isEqualTo("loadedValue");

      // Verify it was cached
      String cachedResult = cacheManager.get(cacheName, key, String.class);
      assertThat(cachedResult).isEqualTo("loadedValue");
    }

    @Test
    @DisplayName("已存在的缓存值不应调用加载器")
    void existingValueShouldNotCallLoader() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      String key = "existingKey";
      cacheManager.put(cacheName, key, "existingValue");

      CacheLoader<String> loader =
          k -> {
            throw new RuntimeException("Should not be called");
          };

      // When
      String result = cacheManager.get(cacheName, key, String.class, loader);

      // Then
      assertThat(result).isEqualTo("existingValue");
    }

    @Test
    @DisplayName("加载器异常应返回null")
    void loaderExceptionShouldReturnNull() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      String key = "errorKey";
      CacheLoader<String> loader =
          k -> {
            throw new Exception("Load error");
          };

      // When
      String result = cacheManager.get(cacheName, key, String.class, loader);

      // Then
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("缓存配置测试")
  class CacheConfigTests {

    @Test
    @DisplayName("创建自定义缓存应成功")
    void createCustomCacheShouldSucceed() {
      // Given
      String cacheName = "customCache";
      CacheConfig config =
          CacheConfig.builder()
              .maxSize(100)
              .expireAfterWrite(Duration.ofMinutes(5))
              .enableStats(true)
              .build();

      // When
      cacheManager.createCache(cacheName, config);
      cacheManager.put(cacheName, "key", "value");

      // Then
      assertThat(cacheManager.get(cacheName, "key", String.class)).isEqualTo("value");
    }

    @Test
    @DisplayName("CacheConfig builder 应正确设置默认值")
    void cacheConfigBuilderShouldSetDefaults() {
      // When
      CacheConfig config = CacheConfig.builder().build();

      // Then
      assertThat(config.getMaxSize()).isEqualTo(10000);
      assertThat(config.getExpireAfterWrite()).isEqualTo(Duration.ofHours(2));
      assertThat(config.getExpireAfterAccess()).isEqualTo(Duration.ofMinutes(30));
      assertThat(config.isEnableStats()).isFalse();
    }

    @Test
    @DisplayName("CacheConfig toString 应返回有效信息")
    void cacheConfigToStringShouldReturnValidInfo() {
      // Given
      CacheConfig config = CacheConfig.builder().maxSize(500).enableStats(true).build();

      // When
      String result = config.toString();

      // Then
      assertThat(result)
          .contains("CacheConfig")
          .contains("maxSize=500")
          .contains("enableStats=true");
    }
  }

  @Nested
  @DisplayName("缓存统计测试")
  class CacheStatisticsTests {

    @Test
    @DisplayName("获取缓存统计应返回有效数据")
    void getStatsShouldReturnValidData() {
      // Given
      String cacheName = CacheManager.NAMES_CACHE;
      cacheManager.put(cacheName, "key1", "value1");
      cacheManager.put(cacheName, "key2", "value2");

      // When
      CacheStatistics stats = cacheManager.getStats(cacheName);

      // Then
      assertThat(stats).isNotNull();
      assertThat(stats.getName()).isEqualTo(cacheName);
      assertThat(stats.getSize()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("获取不存在的缓存统计应返回null")
    void getStatsForNonExistentCacheShouldReturnNull() {
      // When
      CacheStatistics stats = cacheManager.getStats("nonExistentCache");

      // Then
      assertThat(stats).isNull();
    }

    @Test
    @DisplayName("获取所有缓存统计应返回列表")
    void getAllStatsShouldReturnList() {
      // When
      List<CacheStatistics> allStats = cacheManager.getAllStats();

      // Then
      assertThat(allStats).isNotNull();
      // 默认有5个缓存
      assertThat(allStats.size()).isGreaterThanOrEqualTo(5);
    }
  }

  @Nested
  @DisplayName("缓存指标测试")
  class CacheMetricsTests {

    @Test
    @DisplayName("获取缓存指标应返回有效数据")
    void getMetricsShouldReturnValidData() {
      // Given
      cacheManager.put(CacheManager.NAMES_CACHE, "key1", "value1");
      cacheManager.put(CacheManager.REGIONS_CACHE, "key2", "value2");

      // When
      CacheMetrics metrics = cacheManager.getMetrics();

      // Then
      assertThat(metrics).isNotNull();
      assertThat(metrics.getCacheCount()).isGreaterThanOrEqualTo(5);
      assertThat(metrics.getTotalSize()).isGreaterThanOrEqualTo(0);
      assertThat(metrics.getOverallHitRate()).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("缓存键生成测试")
  class CacheKeyGenerationTests {

    @Test
    @DisplayName("生成缓存键应正确拼接")
    void generateKeyShouldConcatenateCorrectly() {
      // When
      String key = CacheManager.generateKey("prefix", "param1", "param2", 123);

      // Then
      assertThat(key).isEqualTo("prefix:param1:param2:123");
    }

    @Test
    @DisplayName("生成缓存键应处理null参数")
    void generateKeyShouldHandleNullParams() {
      // When
      String key = CacheManager.generateKey("prefix", "param1", null);

      // Then
      assertThat(key).isEqualTo("prefix:param1:null");
    }

    @Test
    @DisplayName("生成缓存键应处理空参数列表")
    void generateKeyShouldHandleEmptyParams() {
      // When
      String key = CacheManager.generateKey("prefix");

      // Then
      assertThat(key).isEqualTo("prefix");
    }
  }

  @Nested
  @DisplayName("预定义缓存测试")
  class PredefinedCachesTests {

    @Test
    @DisplayName("NAMES_CACHE 应存在")
    void namesCacheShouldExist() {
      // When
      cacheManager.put(CacheManager.NAMES_CACHE, "test", "value");
      String result = cacheManager.get(CacheManager.NAMES_CACHE, "test", String.class);

      // Then
      assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("REGIONS_CACHE 应存在")
    void regionsCacheShouldExist() {
      // When
      cacheManager.put(CacheManager.REGIONS_CACHE, "test", "value");
      String result = cacheManager.get(CacheManager.REGIONS_CACHE, "test", String.class);

      // Then
      assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("BANK_BINS_CACHE 应存在")
    void bankBinsCacheShouldExist() {
      // When
      cacheManager.put(CacheManager.BANK_BINS_CACHE, "test", "value");
      String result = cacheManager.get(CacheManager.BANK_BINS_CACHE, "test", String.class);

      // Then
      assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("GENERATION_CACHE 应存在")
    void generationCacheShouldExist() {
      // When
      cacheManager.put(CacheManager.GENERATION_CACHE, "test", "value");
      String result = cacheManager.get(CacheManager.GENERATION_CACHE, "test", String.class);

      // Then
      assertThat(result).isEqualTo("value");
    }

    @Test
    @DisplayName("SEQUENCE_CACHE 应存在")
    void sequenceCacheShouldExist() {
      // When
      cacheManager.put(CacheManager.SEQUENCE_CACHE, "test", "value");
      String result = cacheManager.get(CacheManager.SEQUENCE_CACHE, "test", String.class);

      // Then
      assertThat(result).isEqualTo("value");
    }
  }

  @Nested
  @DisplayName("CacheStatistics 类测试")
  class CacheStatisticsClassTests {

    @Test
    @DisplayName("CacheStatistics 应正确存储所有值")
    void cacheStatisticsShouldStoreAllValues() {
      // Given
      String name = "testCache";
      long size = 100;
      long hitCount = 50;
      long missCount = 10;
      double hitRate = 0.83;
      long evictionCount = 5;
      long loadCount = 20;
      double averageLoadTime = 1.5;

      // When
      CacheStatistics stats =
          new CacheStatistics(
              name, size, hitCount, missCount, hitRate, evictionCount, loadCount, averageLoadTime);

      // Then
      assertThat(stats.getName()).isEqualTo(name);
      assertThat(stats.getSize()).isEqualTo(size);
      assertThat(stats.getHitCount()).isEqualTo(hitCount);
      assertThat(stats.getMissCount()).isEqualTo(missCount);
      assertThat(stats.getHitRate()).isEqualTo(hitRate);
      assertThat(stats.getEvictionCount()).isEqualTo(evictionCount);
      assertThat(stats.getLoadCount()).isEqualTo(loadCount);
      assertThat(stats.getAverageLoadTime()).isEqualTo(averageLoadTime);
    }
  }

  @Nested
  @DisplayName("CacheMetrics 类测试")
  class CacheMetricsClassTests {

    @Test
    @DisplayName("CacheMetrics 应正确存储所有值")
    void cacheMetricsShouldStoreAllValues() {
      // Given
      int cacheCount = 5;
      long totalSize = 1000;
      double overallHitRate = 0.85;
      long totalHits = 850;
      long totalMisses = 150;
      long totalEvictions = 50;

      // When
      CacheMetrics metrics =
          new CacheMetrics(
              cacheCount, totalSize, overallHitRate, totalHits, totalMisses, totalEvictions);

      // Then
      assertThat(metrics.getCacheCount()).isEqualTo(cacheCount);
      assertThat(metrics.getTotalSize()).isEqualTo(totalSize);
      assertThat(metrics.getOverallHitRate()).isEqualTo(overallHitRate);
      assertThat(metrics.getTotalHits()).isEqualTo(totalHits);
      assertThat(metrics.getTotalMisses()).isEqualTo(totalMisses);
      assertThat(metrics.getTotalEvictions()).isEqualTo(totalEvictions);
    }
  }
}
