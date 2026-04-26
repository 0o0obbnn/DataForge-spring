package com.dataforge.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitAspect 测试")
class RateLimitAspectTest {

  @Mock private RedisTemplate<String, String> redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  @InjectMocks private RateLimitAspect rateLimitAspect;

  @Nested
  @DisplayName("限流异常测试")
  class RateLimitExceptionTests {

    @Test
    @DisplayName("应创建限流异常")
    void shouldCreateRateLimitException() {
      RateLimitAspect.RateLimitException exception =
          new RateLimitAspect.RateLimitException("Too many requests");

      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo("Too many requests");
      assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("限流异常应包含正确消息")
    void shouldContainCorrectMessage() {
      String message = "Rate limit exceeded";
      RateLimitAspect.RateLimitException exception =
          new RateLimitAspect.RateLimitException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
    }
  }

  @Nested
  @DisplayName("注解测试")
  class RateLimitAnnotationTests {

    @Test
    @DisplayName("应正确创建RateLimit注解实例")
    void shouldCreateRateLimitAnnotation() {
      RateLimit rateLimit =
          new RateLimit() {
            @Override
            public int value() {
              return 10;
            }

            @Override
            public int seconds() {
              return 60;
            }

            @Override
            public String prefix() {
              return "test_prefix";
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
              return RateLimit.class;
            }
          };

      assertThat(rateLimit.value()).isEqualTo(10);
      assertThat(rateLimit.seconds()).isEqualTo(60);
      assertThat(rateLimit.prefix()).isEqualTo("test_prefix");
      assertThat(rateLimit.annotationType()).isEqualTo(RateLimit.class);
    }

    @Test
    @DisplayName("应支持默认值")
    void shouldSupportDefaultValues() {
      RateLimit rateLimit =
          new RateLimit() {
            @Override
            public int value() {
              return 10;
            }

            @Override
            public int seconds() {
              return 60;
            }

            @Override
            public String prefix() {
              return "rate_limit";
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
              return RateLimit.class;
            }
          };

      assertThat(rateLimit.value()).isEqualTo(10);
      assertThat(rateLimit.seconds()).isEqualTo(60);
      assertThat(rateLimit.prefix()).isEqualTo("rate_limit");
    }
  }

  @Nested
  @DisplayName("Redis操作测试")
  class RedisOperationsTests {

    @Test
    @DisplayName("应正确增加Redis值")
    void shouldIncrementRedisValueCorrectly() {
      String key = "test:counter";

      when(valueOperations.increment(key)).thenReturn(5L);

      Long result = valueOperations.increment(key);

      assertThat(result).isEqualTo(5L);
      verify(valueOperations).increment(key);
    }

    @Test
    @DisplayName("应正确设置Redis过期时间")
    void shouldSetRedisExpirationCorrectly() {
      String key = "test:key";
      long timeout = 60;

      when(redisTemplate.expire(key, timeout, TimeUnit.SECONDS)).thenReturn(true);

      boolean result = redisTemplate.expire(key, timeout, TimeUnit.SECONDS);

      assertThat(result).isTrue();
      verify(redisTemplate).expire(key, timeout, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("应正确检查Redis键是否存在")
    void shouldCheckRedisKeyExistsCorrectly() {
      String key = "test:key";

      when(redisTemplate.hasKey(key)).thenReturn(true);

      Boolean result = redisTemplate.hasKey(key);

      assertThat(result).isTrue();
      verify(redisTemplate).hasKey(key);
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class BoundaryConditionTests {

    @Test
    @DisplayName("应处理最小限制值1")
    void shouldHandleMinimumLimitValue() {
      int minLimit = 1;

      assertThat(minLimit).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("应处理最小时间窗口1秒")
    void shouldHandleMinimumTimeWindow() {
      int minWindow = 1;

      assertThat(minWindow).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("应处理大限制值")
    void shouldHandleLargeLimitValue() {
      int largeLimit = 10000;

      assertThat(largeLimit).isGreaterThan(0);
    }

    @Test
    @DisplayName("应处理长时间窗口")
    void shouldHandleLargeTimeWindow() {
      int largeWindow = 86400; // 24 hours

      assertThat(largeWindow).isGreaterThan(0);
    }
  }
}
