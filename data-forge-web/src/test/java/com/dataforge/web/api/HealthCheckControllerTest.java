package com.dataforge.web.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dataforge.core.CacheManager;
import com.dataforge.core.GeneratorFactory;
import com.dataforge.service.DataForgeService;
import com.dataforge.web.cache.MultiLevelCacheManager;
import com.dataforge.web.controller.HealthCheckController;
import com.dataforge.web.repository.UserRepository;
import com.dataforge.web.security.CustomUserDetailsService;
import com.dataforge.web.security.JwtUtil;
import com.dataforge.web.security.LoginAttemptService;
import com.dataforge.web.security.TokenBlacklistService;
import com.dataforge.web.service.AsyncDataGenerationService;
import com.dataforge.web.service.DataTemplateService;
import com.dataforge.web.service.GenerationHistoryService;
import com.dataforge.web.service.MetricsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HealthCheckController 测试类。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@WebMvcTest(HealthCheckController.class)
class HealthCheckControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GeneratorFactory generatorFactory;

  @MockBean private CacheManager cacheManager;

  @MockBean private MultiLevelCacheManager multiLevelCacheManager;

  @MockBean private RedisTemplate<String, String> redisTemplateString;

  @MockBean private RedisTemplate<String, Object> redisTemplateObject;

  @MockBean private RedisConnectionFactory redisConnectionFactory;

  @MockBean private TokenBlacklistService tokenBlacklistService;

  @MockBean private JwtUtil jwtUtil;

  @MockBean private AuthenticationManager authenticationManager;

  // Security dependencies required by CustomUserDetailsService
  @MockBean private UserRepository userRepository;

  @MockBean private LoginAttemptService loginAttemptService;

  @MockBean private CustomUserDetailsService customUserDetailsService;

  @MockBean private DataForgeService dataForgeService;

  @MockBean private AsyncDataGenerationService asyncDataGenerationService;

  @MockBean private GenerationHistoryService generationHistoryService;

  @MockBean private MetricsService metricsService;

  @MockBean private DataTemplateService dataTemplateService;

  @Test
  void testHealthCheck() throws Exception {
    // Mock 依赖
    when(generatorFactory.getGeneratorCount()).thenReturn(60);

    CacheManager.CacheMetrics metrics = org.mockito.Mockito.mock(CacheManager.CacheMetrics.class);
    when(metrics.getCacheCount()).thenReturn(10);
    when(metrics.getTotalSize()).thenReturn(100L);
    when(metrics.getOverallHitRate()).thenReturn(0.85);
    when(metrics.getTotalHits()).thenReturn(850L);
    when(metrics.getTotalMisses()).thenReturn(150L);
    when(metrics.getTotalEvictions()).thenReturn(5L);
    when(cacheManager.getMetrics()).thenReturn(metrics);

    List<CacheManager.CacheStatistics> stats = new ArrayList<>();
    when(cacheManager.getAllStats()).thenReturn(stats);

    Map<String, Object> multiLevelStats = new HashMap<>();
    multiLevelStats.put("localCache", new HashMap<>());
    multiLevelStats.put("redisCache", new HashMap<>());
    when(multiLevelCacheManager.getMultiLevelCacheStats()).thenReturn(multiLevelStats);

    RedisConnection connection = org.mockito.Mockito.mock(RedisConnection.class);
    when(connection.ping()).thenReturn("PONG");
    when(redisConnectionFactory.getConnection()).thenReturn(connection);

    // 执行测试
    mockMvc
        .perform(get("/api/v1/health"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.status").value("UP"))
        .andExpect(jsonPath("$.data.generatorCount").value(60))
        .andExpect(jsonPath("$.data.uptime").exists())
        .andExpect(jsonPath("$.data.memoryUsage").exists())
        .andExpect(jsonPath("$.data.threadUsage").exists())
        .andExpect(jsonPath("$.data.cacheStatus").exists())
        .andExpect(jsonPath("$.data.version").exists());
  }

  @Test
  void testHealthCheckCompatibilityEndpoint() throws Exception {
    // Mock 依赖
    when(generatorFactory.getGeneratorCount()).thenReturn(60);

    CacheManager.CacheMetrics metrics = org.mockito.Mockito.mock(CacheManager.CacheMetrics.class);
    when(metrics.getCacheCount()).thenReturn(10);
    when(metrics.getTotalSize()).thenReturn(100L);
    when(metrics.getOverallHitRate()).thenReturn(0.85);
    when(metrics.getTotalHits()).thenReturn(850L);
    when(metrics.getTotalMisses()).thenReturn(150L);
    when(metrics.getTotalEvictions()).thenReturn(5L);
    when(cacheManager.getMetrics()).thenReturn(metrics);

    List<CacheManager.CacheStatistics> stats = new ArrayList<>();
    when(cacheManager.getAllStats()).thenReturn(stats);

    Map<String, Object> multiLevelStats = new HashMap<>();
    when(multiLevelCacheManager.getMultiLevelCacheStats()).thenReturn(multiLevelStats);

    RedisConnection connection = org.mockito.Mockito.mock(RedisConnection.class);
    when(connection.ping()).thenReturn("PONG");
    when(redisConnectionFactory.getConnection()).thenReturn(connection);

    // 执行测试
    mockMvc
        .perform(get("/api/v1/health/check"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.status").value("UP"));
  }

  @Test
  void testHealthCheckWithRedisDown() throws Exception {
    // Mock 依赖
    when(generatorFactory.getGeneratorCount()).thenReturn(60);

    CacheManager.CacheMetrics metrics = org.mockito.Mockito.mock(CacheManager.CacheMetrics.class);
    when(metrics.getCacheCount()).thenReturn(10);
    when(metrics.getTotalSize()).thenReturn(100L);
    when(metrics.getOverallHitRate()).thenReturn(0.85);
    when(metrics.getTotalHits()).thenReturn(850L);
    when(metrics.getTotalMisses()).thenReturn(150L);
    when(metrics.getTotalEvictions()).thenReturn(5L);
    when(cacheManager.getMetrics()).thenReturn(metrics);

    List<CacheManager.CacheStatistics> stats = new ArrayList<>();
    when(cacheManager.getAllStats()).thenReturn(stats);

    Map<String, Object> multiLevelStats = new HashMap<>();
    when(multiLevelCacheManager.getMultiLevelCacheStats()).thenReturn(multiLevelStats);

    // Redis 连接失败
    when(redisConnectionFactory.getConnection())
        .thenThrow(new RuntimeException("Redis connection failed"));

    // 执行测试
    mockMvc
        .perform(get("/api/v1/health"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.status").value("UP"))
        .andExpect(
            jsonPath("$.data.cacheStatus.status")
                .value(
                    org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("DEGRADED"), org.hamcrest.Matchers.is("ERROR"))));
  }
}
