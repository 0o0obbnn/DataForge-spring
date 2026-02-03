package com.dataforge.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dataforge.core.GeneratorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomHealthIndicator 测试")
class CustomHealthIndicatorTest {

  @Mock private GeneratorFactory generatorFactory;

  @Mock private RedisConnectionFactory redisConnectionFactory;

  @Mock private RedisConnection redisConnection;

  @InjectMocks private CustomHealthIndicator customHealthIndicator;

  @BeforeEach
  void setUp() {
    customHealthIndicator = new CustomHealthIndicator(generatorFactory, redisConnectionFactory);
  }

  @Nested
  @DisplayName("生成器工厂健康检查测试")
  class GeneratorFactoryHealthTests {

    @Test
    @DisplayName("生成器数量大于0时应返回UP状态")
    void shouldReturnUpWhenGeneratorsExist() throws Exception {
      when(generatorFactory.getGeneratorCount()).thenReturn(10);
      when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
      when(redisConnection.ping()).thenReturn("PONG");

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry("generatorFactory", "OK");
      assertThat(health.getDetails()).containsEntry("generatorCount", 10);
    }

    @Test
    @DisplayName("生成器数量为0时应返回DOWN状态")
    void shouldReturnDownWhenNoGenerators() {
      when(generatorFactory.getGeneratorCount()).thenReturn(0);

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
      assertThat(health.getDetails()).containsEntry("generatorFactory", "No generators registered");
    }
  }

  @Nested
  @DisplayName("Redis健康检查测试")
  class RedisHealthTests {

    @Test
    @DisplayName("Redis连接正常时应返回UP状态")
    void shouldReturnUpWhenRedisIsUp() throws Exception {
      when(generatorFactory.getGeneratorCount()).thenReturn(10);
      when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
      when(redisConnection.ping()).thenReturn("PONG");

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry("redis", "UP");
      verify(redisConnection).close();
    }

    @Test
    @DisplayName("Redis连接失败时应返回DOWN状态")
    void shouldReturnDownWhenRedisIsDown() throws Exception {
      when(generatorFactory.getGeneratorCount()).thenReturn(10);
      when(redisConnectionFactory.getConnection())
          .thenThrow(new RuntimeException("Connection failed"));

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
      assertThat(health.getDetails()).containsKey("redis");
      String redisStatus = (String) health.getDetails().get("redis");
      assertThat(redisStatus).contains("DOWN");
    }

    @Test
    @DisplayName("Redis返回非PONG响应时应返回DOWN状态")
    void shouldReturnDownWhenRedisReturnsNonPong() throws Exception {
      when(generatorFactory.getGeneratorCount()).thenReturn(10);
      when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
      when(redisConnection.ping()).thenReturn("ERROR");

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
      assertThat(health.getDetails()).containsEntry("redis", "Connection failed");
    }
  }

  @Nested
  @DisplayName("Redis未配置测试")
  class RedisNotConfiguredTests {

    @Test
    @DisplayName("Redis未配置时应返回UP状态")
    void shouldReturnUpWhenRedisNotConfigured() {
      CustomHealthIndicator indicator = new CustomHealthIndicator(generatorFactory, null);
      when(generatorFactory.getGeneratorCount()).thenReturn(10);

      Health health = indicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry("redis", "Not configured");
    }
  }

  @Nested
  @DisplayName("综合健康检查测试")
  class CombinedHealthTests {

    @Test
    @DisplayName("所有组件正常时应返回UP状态")
    void shouldReturnUpWhenAllComponentsAreHealthy() throws Exception {
      when(generatorFactory.getGeneratorCount()).thenReturn(10);
      when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
      when(redisConnection.ping()).thenReturn("PONG");

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry("generatorFactory", "OK");
      assertThat(health.getDetails()).containsEntry("generatorCount", 10);
      assertThat(health.getDetails()).containsEntry("redis", "UP");
    }

    @Test
    @DisplayName("任一组件异常时应返回DOWN状态")
    void shouldReturnDownWhenAnyComponentIsUnhealthy() {
      when(generatorFactory.getGeneratorCount()).thenReturn(0);

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("生成器数量为1时应返回UP状态")
    void shouldReturnUpWhenGeneratorCountIsOne() throws Exception {
      when(generatorFactory.getGeneratorCount()).thenReturn(1);
      when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
      when(redisConnection.ping()).thenReturn("PONG");

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry("generatorCount", 1);
    }

    @Test
    @DisplayName("生成器数量很大时应正确处理")
    void shouldHandleLargeGeneratorCount() throws Exception {
      when(generatorFactory.getGeneratorCount()).thenReturn(1000);
      when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
      when(redisConnection.ping()).thenReturn("PONG");

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry("generatorCount", 1000);
    }

    @Test
    @DisplayName("Redis连接关闭失败时应继续处理")
    void shouldContinueWhenRedisCloseFails() throws Exception {
      when(generatorFactory.getGeneratorCount()).thenReturn(10);
      when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
      when(redisConnection.ping()).thenReturn("PONG");
      doThrow(new RuntimeException("Close failed")).when(redisConnection).close();

      Health health = customHealthIndicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
    }
  }
}
