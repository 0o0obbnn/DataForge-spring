package com.dataforge.web.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 测试用 Redis 配置类。
 *
 * <p>提供 Mock 的 Redis 模板用于测试，避免测试依赖真实的 Redis 服务。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@TestConfiguration
public class TestRedisConfiguration {

  /**
   * 提供 Mock 的 RedisTemplate。
   *
   * @return Mock 的 RedisTemplate
   */
  @Bean
  @Primary
  @SuppressWarnings("unchecked")
  public RedisTemplate<String, Object> redisTemplate() {
    return Mockito.mock(RedisTemplate.class);
  }

  /**
   * 提供 Mock 的 StringRedisTemplate。
   *
   * @return Mock 的 StringRedisTemplate
   */
  @Bean(name = "stringRedisTemplate")
  public StringRedisTemplate stringRedisTemplate() {
    return Mockito.mock(StringRedisTemplate.class);
  }

  /**
   * 提供 Mock 的 RedisConnectionFactory。
   *
   * <p>RedisConfig需要此Bean来创建RedisTemplate，所以必须mock它。
   *
   * @return Mock 的 RedisConnectionFactory
   */
  @Bean
  @Primary
  public org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory() {
    return Mockito.mock(org.springframework.data.redis.connection.RedisConnectionFactory.class);
  }

  /**
   * 提供简单的 MeterRegistry 用于测试。
   *
   * @return SimpleMeterRegistry 实例
   */
  @Bean
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }
}
