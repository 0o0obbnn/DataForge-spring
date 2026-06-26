package com.dataforge.web.controller;

import com.dataforge.core.CacheManager;
import com.dataforge.core.CacheManager.CacheMetrics;
import com.dataforge.core.CacheManager.CacheStatistics;
import com.dataforge.core.GeneratorFactory;
import com.dataforge.web.cache.MultiLevelCacheManager;
import com.dataforge.web.model.ApiResponse;
import com.dataforge.web.model.HealthStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "系统健康检查相关API")
public class HealthCheckController {

  private final GeneratorFactory generatorFactory;
  private final CacheManager cacheManager;
  private final MultiLevelCacheManager multiLevelCacheManager;
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private RedisTemplate<String, Object> redisTemplate;

  @Value("${spring.application.version:1.0.0}")
  private String version;

  public HealthCheckController(
      GeneratorFactory generatorFactory,
      CacheManager cacheManager,
      MultiLevelCacheManager multiLevelCacheManager) {
    this.generatorFactory = generatorFactory;
    this.cacheManager = cacheManager;
    this.multiLevelCacheManager = multiLevelCacheManager;
  }

  // JVM 管理 Bean
  private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

  // 应用启动时间
  private final long startTime = System.currentTimeMillis();

  /**
   * 系统健康状态检查。
   *
   * @return ResponseEntity<ApiResponse<HealthStatus>> 系统健康状态
   */
  @GetMapping
  @Operation(summary = "系统健康检查", description = "返回系统健康状态信息，包括生成器数量、运行时间、内存使用、线程状态、缓存状态等")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "系统健康",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<HealthStatus>> healthCheck() {

    // 收集系统内存信息
    Map<String, Object> memoryUsage = getMemoryUsage();

    // 收集线程信息
    Map<String, Object> threadUsage = getThreadUsage();

    // 构建健康状态响应
    HealthStatus healthStatus =
        HealthStatus.builder()
            .status("UP")
            .generatorCount(generatorFactory.getGeneratorCount())
            .uptime(System.currentTimeMillis() - startTime)
            .memoryUsage(memoryUsage)
            .threadUsage(threadUsage)
            .cacheStatus(getCacheStatus())
            .version(version)
            .build();

    return ResponseEntity.ok(ApiResponse.success(healthStatus));
  }

  /**
   * 健康检查端点（兼容性端点）。
   *
   * @return ResponseEntity<ApiResponse<HealthStatus>> 系统健康状态
   */
  @GetMapping("/check")
  @Operation(summary = "健康检查兼容性端点", description = "返回系统健康状态信息，用于向后兼容")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "系统健康",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<HealthStatus>> check() {
    return healthCheck();
  }

  /**
   * 获取系统内存使用情况。
   *
   * @return Map<String, Object> 内存使用信息
   */
  private Map<String, Object> getMemoryUsage() {
    Map<String, Object> memoryInfo = new HashMap<>();

    MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
    MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

    memoryInfo.put(
        "heap",
        Map.of(
            "used", heapMemoryUsage.getUsed(),
            "committed", heapMemoryUsage.getCommitted(),
            "max", heapMemoryUsage.getMax(),
            "init", heapMemoryUsage.getInit()));

    memoryInfo.put(
        "nonHeap",
        Map.of(
            "used", nonHeapMemoryUsage.getUsed(),
            "committed", nonHeapMemoryUsage.getCommitted(),
            "max", nonHeapMemoryUsage.getMax(),
            "init", nonHeapMemoryUsage.getInit()));

    return memoryInfo;
  }

  /**
   * 获取系统线程使用情况。
   *
   * @return Map<String, Object> 线程使用信息
   */
  private Map<String, Object> getThreadUsage() {
    Map<String, Object> threadInfo = new HashMap<>();

    threadInfo.put("totalThreads", threadMXBean.getThreadCount());
    threadInfo.put("daemonThreads", threadMXBean.getDaemonThreadCount());
    threadInfo.put("peakThreads", threadMXBean.getPeakThreadCount());
    threadInfo.put("startedThreads", threadMXBean.getTotalStartedThreadCount());
    threadInfo.put(
        "deadlockedThreads",
        threadMXBean.findDeadlockedThreads() != null
            ? threadMXBean.findDeadlockedThreads().length
            : 0);

    return threadInfo;
  }

  /**
   * 获取缓存状态信息。
   *
   * @return Map<String, Object> 缓存状态信息
   */
  private Map<String, Object> getCacheStatus() {
    Map<String, Object> cacheInfo = new HashMap<>();

    try {
      // 1. 检查 Redis 连接状态
      boolean redisAvailable = checkRedisConnection();
      cacheInfo.put("redisStatus", redisAvailable ? "UP" : "DOWN");

      // 2. 获取核心缓存管理器的统计信息（用于数据生成器的缓存）
      CacheMetrics cacheMetrics = cacheManager.getMetrics();
      Map<String, Object> metricsInfo = new HashMap<>();
      metricsInfo.put("cacheCount", cacheMetrics.getCacheCount());
      metricsInfo.put("totalSize", cacheMetrics.getTotalSize());
      metricsInfo.put(
          "overallHitRate", String.format("%.2f%%", cacheMetrics.getOverallHitRate() * 100));
      metricsInfo.put("totalHits", cacheMetrics.getTotalHits());
      metricsInfo.put("totalMisses", cacheMetrics.getTotalMisses());
      metricsInfo.put("totalEvictions", cacheMetrics.getTotalEvictions());
      cacheInfo.put("generatorCacheMetrics", metricsInfo);

      // 3. 获取各个生成器缓存的详细统计信息
      List<CacheStatistics> allStats = cacheManager.getAllStats();
      List<Map<String, Object>> cacheDetails = new ArrayList<>();

      for (CacheStatistics stats : allStats) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("name", stats.getName());
        detail.put("size", stats.getSize());
        detail.put("hitCount", stats.getHitCount());
        detail.put("missCount", stats.getMissCount());
        detail.put("hitRate", String.format("%.2f%%", stats.getHitRate() * 100));
        detail.put("evictionCount", stats.getEvictionCount());
        detail.put("loadCount", stats.getLoadCount());
        detail.put("averageLoadTime", String.format("%.2f ns", stats.getAverageLoadTime()));
        cacheDetails.add(detail);
      }
      cacheInfo.put("generatorCaches", cacheDetails);

      // 4. 获取多级缓存管理器的统计信息（用于Web层的缓存）
      Map<String, Object> multiLevelStats = multiLevelCacheManager.getMultiLevelCacheStats();
      cacheInfo.put("webLayerCache", multiLevelStats);

      // 5. 设置整体状态
      cacheInfo.put("status", redisAvailable ? "UP" : "DEGRADED");

    } catch (Exception e) {
      // 缓存监控失败时，返回降级状态
      cacheInfo.put("status", "ERROR");
      cacheInfo.put("error", e.getMessage());
    }

    return cacheInfo;
  }

  /**
   * 检查 Redis 连接状态。
   *
   * @return boolean Redis 是否可用
   */
  private boolean checkRedisConnection() {
    if (redisTemplate == null) {
      return false;
    }
    try {
      RedisConnection connection = redisTemplate.getRequiredConnectionFactory().getConnection();
      String pong = connection.ping();
      connection.close();
      return "PONG".equalsIgnoreCase(pong);
    } catch (Exception e) {
      return false;
    }
  }
}
