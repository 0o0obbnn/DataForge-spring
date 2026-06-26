package com.dataforge.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * TokenBlacklistService 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("TokenBlacklistService 测试")
class TokenBlacklistServiceTest {

  private RedisTemplate<String, String> redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private TokenBlacklistService tokenBlacklistService;
  private JwtProperties jwtProperties;

  private static final String TEST_SECRET =
      "myTestSecretKeyThatIsLongEnoughForHS256Algorithm1234567890";
  private static final SecretKey SIGN_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    redisTemplate = mock(RedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    jwtProperties = new JwtProperties();
    jwtProperties.setSecret(TEST_SECRET);
    jwtProperties.setExpiration(3600000);
    jwtProperties.setRefreshExpiration(604800000);

    tokenBlacklistService = new TokenBlacklistService(jwtProperties, redisTemplate);
  }

  @Nested
  @DisplayName("添加 Token 到黑名单测试")
  class AddToBlacklistTests {

    @Test
    @DisplayName("添加有效 Token 到黑名单应成功")
    void addValidTokenToBlacklistShouldSucceed() {
      // Given
      String token = createValidToken("testuser", 3600000); // 1 hour

      // When
      tokenBlacklistService.addToBlacklist(token);

      // Then
      verify(valueOperations).set(anyString(), eq("1"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("添加已过期 Token 不应抛出异常")
    void addExpiredTokenShouldNotThrowException() {
      // Given
      String token = createExpiredToken("testuser");

      // When & Then - 不应该抛出异常
      tokenBlacklistService.addToBlacklist(token);

      // 已过期的Token不会被添加到Redis
      verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("添加无效 Token 应抛出异常")
    void addInvalidTokenShouldThrowException() {
      // Given
      String invalidToken = "invalid.token.format";

      // Then
      assertThatThrownBy(() -> tokenBlacklistService.addToBlacklist(invalidToken))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("Failed to invalidate token");
    }
  }

  @Nested
  @DisplayName("检查 Token 黑名单状态测试")
  class IsBlacklistedTests {

    @Test
    @DisplayName("检查已在黑名单中的 Token 应返回 true")
    void checkBlacklistedTokenShouldReturnTrue() {
      // Given
      String token = createValidToken("testuser", 3600000);
      when(redisTemplate.hasKey(anyString())).thenReturn(true);

      // When
      boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);

      // Then
      assertThat(isBlacklisted).isTrue();
    }

    @Test
    @DisplayName("检查不在黑名单中的 Token 应返回 false")
    void checkNonBlacklistedTokenShouldReturnFalse() {
      // Given
      String token = createValidToken("testuser", 3600000);
      when(redisTemplate.hasKey(anyString())).thenReturn(false);

      // When
      boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);

      // Then
      assertThat(isBlacklisted).isFalse();
    }

    @Test
    @DisplayName("检查黑名单时发生异常应返回 true（保守处理）")
    void checkBlacklistWithExceptionShouldReturnTrue() {
      // Given
      String token = createValidToken("testuser", 3600000);
      when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

      // When
      boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);

      // Then
      assertThat(isBlacklisted).isTrue();
    }

    @Test
    @DisplayName("检查无效 Token 应返回 true（保守处理）")
    void checkInvalidTokenShouldReturnTrue() {
      // Given
      String invalidToken = "invalid.token.format";

      // When
      boolean isBlacklisted = tokenBlacklistService.isBlacklisted(invalidToken);

      // Then - 无效Token视为在黑名单中
      assertThat(isBlacklisted).isTrue();
    }
  }

  @Nested
  @DisplayName("清除黑名单测试")
  class ClearBlacklistTests {

    @Test
    @DisplayName("清除所有黑名单 Token 应成功")
    void clearAllBlacklistedTokensShouldSucceed() {
      // Given
      Set<String> keys = Set.of("token:blacklist:jti1", "token:blacklist:jti2");
      when(redisTemplate.keys("token:blacklist:*")).thenReturn(keys);
      when(redisTemplate.delete(keys)).thenReturn(2L);

      // When
      long deletedCount = tokenBlacklistService.clearAllBlacklistedTokens();

      // Then
      assertThat(deletedCount).isEqualTo(2);
      verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("清除所有黑名单时发生异常应抛出 SecurityException")
    void clearAllWithExceptionShouldThrowSecurityException() {
      // Given
      when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis error"));

      // Then
      assertThatThrownBy(() -> tokenBlacklistService.clearAllBlacklistedTokens())
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("Failed to clear token blacklist");
    }

    @Test
    @DisplayName("按前缀清除黑名单应成功")
    void clearBlacklistByPrefixShouldSucceed() {
      // Given
      String prefix = "user123";
      Set<String> keys = Set.of("token:blacklist:user123:token1", "token:blacklist:user123:token2");
      when(redisTemplate.keys("token:blacklist:user123*")).thenReturn(keys);
      when(redisTemplate.delete(keys)).thenReturn(2L);

      // When
      long deletedCount = tokenBlacklistService.clearBlacklistByPrefix(prefix);

      // Then
      assertThat(deletedCount).isEqualTo(2);
      verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("按前缀清除黑名单时发生异常应抛出 SecurityException")
    void clearByPrefixWithExceptionShouldThrowSecurityException() {
      // Given
      when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis error"));

      // Then
      assertThatThrownBy(() -> tokenBlacklistService.clearBlacklistByPrefix("user123"))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("Failed to clear token blacklist");
    }
  }

  @Nested
  @DisplayName("边界情况测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("添加 Token 时 Redis 异常应抛出 SecurityException")
    void addToBlacklistWithRedisExceptionShouldThrowSecurityException() {
      // Given
      String token = createValidToken("testuser", 3600000);
      doThrow(new RuntimeException("Redis connection failed"))
          .when(valueOperations)
          .set(anyString(), anyString(), anyLong(), any());

      // Then
      assertThatThrownBy(() -> tokenBlacklistService.addToBlacklist(token))
          .isInstanceOf(SecurityException.class)
          .hasMessageContaining("Failed to invalidate token");
    }

    @Test
    @DisplayName("检查黑名单时 Redis 返回 null 应返回 false")
    void checkBlacklistWithNullResultShouldReturnFalse() {
      // Given
      String token = createValidToken("testuser", 3600000);
      when(redisTemplate.hasKey(anyString())).thenReturn(null);

      // When
      boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);

      // Then
      assertThat(isBlacklisted).isFalse();
    }
  }

  // Helper methods
  private String createValidToken(String username, long expirationMillis) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expirationMillis))
        .id(java.util.UUID.randomUUID().toString())
        .signWith(SIGN_KEY)
        .compact();
  }

  private String createExpiredToken(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
        .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago (expired)
        .id(java.util.UUID.randomUUID().toString())
        .signWith(SIGN_KEY)
        .compact();
  }
}
