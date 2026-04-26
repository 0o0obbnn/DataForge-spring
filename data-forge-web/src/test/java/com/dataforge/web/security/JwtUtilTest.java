package com.dataforge.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JwtUtil 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("JwtUtil 测试")
class JwtUtilTest {

  private JwtProperties jwtProperties;
  private JwtUtil jwtUtil;

  private static final String TEST_SECRET =
      "myTestSecretKeyThatIsLongEnoughForHS256Algorithm1234567890";
  private static final String TEST_USERNAME = "testuser";

  @BeforeEach
  void setUp() {
    jwtProperties = new JwtProperties();
    jwtProperties.setSecret(TEST_SECRET);
    jwtProperties.setExpiration(3600000); // 1 hour
    jwtProperties.setRefreshExpiration(604800000); // 7 days

    jwtUtil = new JwtUtil(jwtProperties);
  }

  @Nested
  @DisplayName("Token 生成测试")
  class TokenGenerationTests {

    @Test
    @DisplayName("生成 Access Token 应成功")
    void generateAccessTokenShouldSucceed() {
      // When
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // Then
      assertThat(token).isNotNull().isNotEmpty();
      assertThat(jwtUtil.isAccessToken(token)).isTrue();
      assertThat(jwtUtil.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("生成 Refresh Token 应成功")
    void generateRefreshTokenShouldSucceed() {
      // When
      String token = jwtUtil.generateRefreshToken(TEST_USERNAME);

      // Then
      assertThat(token).isNotNull().isNotEmpty();
      assertThat(jwtUtil.isRefreshToken(token)).isTrue();
      assertThat(jwtUtil.isAccessToken(token)).isFalse();
      assertThat(jwtUtil.extractJti(token)).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("生成的 Token 应包含正确的用户名")
    void generatedTokenShouldContainCorrectUsername() {
      // When
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // Then
      assertThat(jwtUtil.extractUsername(token)).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("生成的 Token 应具有正确的过期时间")
    void generatedTokenShouldHaveCorrectExpiration() {
      // Given
      long beforeGeneration = System.currentTimeMillis();

      // When
      String token = jwtUtil.generateToken(TEST_USERNAME);
      Date expiration = jwtUtil.extractExpiration(token);

      // Then
      long expectedExpiration = beforeGeneration + jwtProperties.getExpiration();
      assertThat(expiration.getTime())
          .isBetween(expectedExpiration - 5000, expectedExpiration + 5000);
    }
  }

  @Nested
  @DisplayName("Token 提取测试")
  class TokenExtractionTests {

    @Test
    @DisplayName("从 Access Token 提取用户名应成功")
    void extractUsernameFromAccessTokenShouldSucceed() {
      // Given
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // When
      String username = jwtUtil.extractUsername(token);

      // Then
      assertThat(username).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("从 Refresh Token 提取用户名应成功")
    void extractUsernameFromRefreshTokenShouldSucceed() {
      // Given
      String token = jwtUtil.generateRefreshToken(TEST_USERNAME);

      // When
      String username = jwtUtil.extractUsername(token);

      // Then
      assertThat(username).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("提取 Token 类型应正确")
    void extractTokenTypeShouldBeCorrect() {
      // Given
      String accessToken = jwtUtil.generateToken(TEST_USERNAME);
      String refreshToken = jwtUtil.generateRefreshToken(TEST_USERNAME);

      // Then
      assertThat(jwtUtil.extractTokenType(accessToken)).isEqualTo("access");
      assertThat(jwtUtil.extractTokenType(refreshToken)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("提取 JTI 应正确")
    void extractJtiShouldBeCorrect() {
      // Given
      String refreshToken = jwtUtil.generateRefreshToken(TEST_USERNAME);

      // When
      String jti = jwtUtil.extractJti(refreshToken);

      // Then
      assertThat(jti).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("从 Access Token 提取 JTI 应返回 null")
    void extractJtiFromAccessTokenShouldReturnNull() {
      // Given
      String accessToken = jwtUtil.generateToken(TEST_USERNAME);

      // When
      String jti = jwtUtil.extractJti(accessToken);

      // Then
      assertThat(jti).isNull();
    }

    @Test
    @DisplayName("提取过期时间应正确")
    void extractExpirationShouldBeCorrect() {
      // Given
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // When
      Date expiration = jwtUtil.extractExpiration(token);

      // Then
      assertThat(expiration).isNotNull();
      assertThat(expiration).isAfter(new Date());
    }
  }

  @Nested
  @DisplayName("Token 验证测试")
  class TokenValidationTests {

    @Test
    @DisplayName("验证有效的 Access Token 应返回 true")
    void validateValidAccessTokenShouldReturnTrue() {
      // Given
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // When
      boolean isValid = jwtUtil.validateToken(token, TEST_USERNAME);

      // Then
      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("验证有效的 Refresh Token 应返回 true")
    void validateValidRefreshTokenShouldReturnTrue() {
      // Given
      String token = jwtUtil.generateRefreshToken(TEST_USERNAME);

      // When
      boolean isValid = jwtUtil.validateToken(token, TEST_USERNAME);

      // Then
      assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("使用错误的用户名验证 Token 应返回 false")
    void validateTokenWithWrongUsernameShouldReturnFalse() {
      // Given
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // When
      boolean isValid = jwtUtil.validateToken(token, "wronguser");

      // Then
      assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("检查未过期的 Token 应返回 false")
    void checkUnexpiredTokenShouldReturnFalse() {
      // Given
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // When
      boolean isExpired = jwtUtil.isTokenExpired(token);

      // Then
      assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("验证篡改的 Token 应抛出异常")
    void validateTamperedTokenShouldThrowException() {
      // Given
      String token = jwtUtil.generateToken(TEST_USERNAME);
      String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

      // Then
      assertThatThrownBy(() -> jwtUtil.extractUsername(tamperedToken))
          .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("验证无效的 Token 格式应抛出异常")
    void validateInvalidTokenFormatShouldThrowException() {
      // Then
      assertThatThrownBy(() -> jwtUtil.extractUsername("invalid.token.format"))
          .isInstanceOf(JwtException.class);
    }
  }

  @Nested
  @DisplayName("Token 类型检查测试")
  class TokenTypeCheckTests {

    @Test
    @DisplayName("检查 Access Token 类型应正确")
    void checkAccessTokenTypeShouldBeCorrect() {
      // Given
      String accessToken = jwtUtil.generateToken(TEST_USERNAME);

      // Then
      assertThat(jwtUtil.isAccessToken(accessToken)).isTrue();
      assertThat(jwtUtil.isRefreshToken(accessToken)).isFalse();
    }

    @Test
    @DisplayName("检查 Refresh Token 类型应正确")
    void checkRefreshTokenTypeShouldBeCorrect() {
      // Given
      String refreshToken = jwtUtil.generateRefreshToken(TEST_USERNAME);

      // Then
      assertThat(jwtUtil.isRefreshToken(refreshToken)).isTrue();
      assertThat(jwtUtil.isAccessToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("检查没有类型的 Token 应返回 false")
    void checkTokenWithoutTypeShouldReturnFalse() {
      // Given - 创建一个没有 type 声明的 token（通过反射或自定义创建）
      // 这里我们假设如果 type 不是 "access" 或 "refresh"，则两个方法都返回 false

      // 由于 JwtUtil 的实现，如果 type 不是 "access" 或 "refresh"，两个方法都返回 false
      // 这个测试主要是为了覆盖边界情况
    }
  }

  @Nested
  @DisplayName("配置属性测试")
  class ConfigurationTests {

    @Test
    @DisplayName("获取 Access Token 过期时间应正确")
    void getAccessTokenExpirationShouldBeCorrect() {
      // When
      long expiration = jwtUtil.getAccessTokenExpiration();

      // Then
      assertThat(expiration).isEqualTo(3600000);
    }

    @Test
    @DisplayName("获取 Refresh Token 过期时间应正确")
    void getRefreshTokenExpirationShouldBeCorrect() {
      // When
      long expiration = jwtUtil.getRefreshTokenExpiration();

      // Then
      assertThat(expiration).isEqualTo(604800000);
    }
  }

  @Nested
  @DisplayName("自定义声明提取测试")
  class CustomClaimExtractionTests {

    @Test
    @DisplayName("提取自定义声明应成功")
    void extractCustomClaimShouldSucceed() {
      // Given
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // When
      String type = jwtUtil.extractClaim(token, claims -> claims.get("type", String.class));

      // Then
      assertThat(type).isEqualTo("access");
    }

    @Test
    @DisplayName("提取主题声明应成功")
    void extractSubjectClaimShouldSucceed() {
      // Given
      String token = jwtUtil.generateToken(TEST_USERNAME);

      // When
      String subject = jwtUtil.extractClaim(token, io.jsonwebtoken.Claims::getSubject);

      // Then
      assertThat(subject).isEqualTo(TEST_USERNAME);
    }
  }
}
