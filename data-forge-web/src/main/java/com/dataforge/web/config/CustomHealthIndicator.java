package com.dataforge.web.config;

import com.dataforge.core.GeneratorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * 自定义健康检查指示器。
 *
 * <p>提供应用的健康状态检查，包括： - 生成器工厂状态 - Redis连接状态 - 数据库连接状态（通过JPA自动检查）
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {

  @Autowired private GeneratorFactory generatorFactory;

  @Autowired(required = false)
  private RedisConnectionFactory redisConnectionFactory;

  @Override
  public Health health() {
    Health.Builder builder = Health.up();

    // 检查生成器工厂
    int generatorCount = generatorFactory.getGeneratorCount();
    if (generatorCount == 0) {
      builder.down().withDetail("generatorFactory", "No generators registered");
    } else {
      builder.withDetail("generatorFactory", "OK").withDetail("generatorCount", generatorCount);
    }

    // 检查Redis连接
    if (redisConnectionFactory != null) {
      try {
        RedisConnection connection = redisConnectionFactory.getConnection();
        String pong = connection.ping();
        connection.close();
        if ("PONG".equalsIgnoreCase(pong)) {
          builder.withDetail("redis", "UP");
        } else {
          builder.down().withDetail("redis", "Connection failed");
        }
      } catch (Exception e) {
        builder.down().withDetail("redis", "DOWN: " + e.getMessage());
      }
    } else {
      builder.withDetail("redis", "Not configured");
    }

    return builder.build();
  }
}
