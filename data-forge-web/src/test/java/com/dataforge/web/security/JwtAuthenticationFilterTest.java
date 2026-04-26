package com.dataforge.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * JwtAuthenticationFilter 测试类
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@DisplayName("JwtAuthenticationFilter 测试")
class JwtAuthenticationFilterTest {

  private JwtUtil jwtUtil;
  private UserDetailsService userDetailsService;
  private TokenBlacklistService tokenBlacklistService;
  private JwtAuthenticationFilter filter;

  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;

  private static final String TEST_SECRET =
      "myTestSecretKeyThatIsLongEnoughForHS256Algorithm1234567890";
  private static final SecretKey SIGN_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
  private static final String TEST_USERNAME = "testuser";

  @BeforeEach
  void setUp() {
    // Setup JwtProperties
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret(TEST_SECRET);
    jwtProperties.setExpiration(3600000);
    jwtProperties.setRefreshExpiration(604800000);

    jwtUtil = new JwtUtil(jwtProperties);
    userDetailsService = mock(UserDetailsService.class);
    tokenBlacklistService = mock(TokenBlacklistService.class);

    filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, tokenBlacklistService);

    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);

    // Clear security context before each test
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("Token 提取测试")
  class TokenExtractionTests {

    @Test
    @DisplayName("请求没有 Authorization 头应继续过滤器链")
    void requestWithoutAuthHeaderShouldContinueChain() throws ServletException, IOException {
      // Given
      when(request.getHeader("Authorization")).thenReturn(null);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
      verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Authorization 头不是 Bearer 格式应继续过滤器链")
    void requestWithNonBearerAuthHeaderShouldContinueChain() throws ServletException, IOException {
      // Given
      when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
      verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("有效的 Bearer Token 应进行认证")
    void validBearerTokenShouldAuthenticate() throws ServletException, IOException {
      // Given
      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

      UserDetails userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
      when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
      verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
    }
  }

  @Nested
  @DisplayName("Token 黑名单检查测试")
  class TokenBlacklistTests {

    @Test
    @DisplayName("黑名单中的 Token 应返回 401")
    void blacklistedTokenShouldReturn401() throws ServletException, IOException {
      // Given
      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response)
          .sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Token has been revoked"));
      verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("不在黑名单中的 Token 应继续认证")
    void nonBlacklistedTokenShouldContinueAuthentication() throws ServletException, IOException {
      // Given
      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

      UserDetails userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
      when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response, never()).sendError(any(int.class), anyString());
      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("Token 过期检查测试")
  class TokenExpirationTests {

    @Test
    @DisplayName("过期的 Token 应返回 401")
    void expiredTokenShouldReturn401() throws ServletException, IOException {
      // Given
      String token = createExpiredToken(TEST_USERNAME);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("Token expired"));
      verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("未过期的 Token 应继续认证")
    void nonExpiredTokenShouldContinueAuthentication() throws ServletException, IOException {
      // Given
      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

      UserDetails userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
      when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(response, never()).sendError(any(int.class), anyString());
      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("Token 验证测试")
  class TokenValidationTests {

    @Test
    @DisplayName("无效的 Token 格式应继续过滤器链但不认证")
    void invalidTokenFormatShouldContinueChainWithoutAuth() throws ServletException, IOException {
      // Given
      when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.format");

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
      verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("用户名不匹配应不设置认证")
    void mismatchedUsernameShouldNotSetAuthentication() throws ServletException, IOException {
      // Given
      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

      UserDetails userDetails = new User("differentuser", "password", Collections.emptyList());
      when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
  }

  @Nested
  @DisplayName("认证上下文测试")
  class AuthenticationContextTests {

    @Test
    @DisplayName("成功认证后应设置 SecurityContext")
    void successfulAuthShouldSetSecurityContext() throws ServletException, IOException {
      // Given
      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

      UserDetails userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
      when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
      assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
          .isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("已有认证上下文时不应重复认证")
    void existingAuthContextShouldNotReAuthenticate() throws ServletException, IOException {
      // Given - 先设置一个已有的认证
      org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
          new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
              "existinguser", null, Collections.emptyList());
      SecurityContextHolder.getContext().setAuthentication(existingAuth);

      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then - 不应加载用户详情，因为已有认证
      verify(userDetailsService, never()).loadUserByUsername(anyString());
      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("异常处理测试")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("UserDetailsService 异常应继续过滤器链")
    void userDetailsServiceExceptionShouldContinueChain() throws ServletException, IOException {
      // Given
      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
      when(userDetailsService.loadUserByUsername(TEST_USERNAME))
          .thenThrow(new RuntimeException("User not found"));

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("TokenBlacklistService 异常应继续过滤器链")
    void tokenBlacklistServiceExceptionShouldContinueChain() throws ServletException, IOException {
      // Given
      String token = createValidToken(TEST_USERNAME, 3600000);
      when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
      when(tokenBlacklistService.isBlacklisted(token))
          .thenThrow(new RuntimeException("Redis error"));

      // When
      filter.doFilterInternal(request, response, filterChain);

      // Then
      verify(filterChain).doFilter(request, response);
    }
  }

  // Helper methods
  private String createValidToken(String username, long expirationMillis) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expirationMillis))
        .claim("type", "access")
        .signWith(SIGN_KEY)
        .compact();
  }

  private String createExpiredToken(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date(System.currentTimeMillis() - 7200000))
        .expiration(new Date(System.currentTimeMillis() - 3600000))
        .claim("type", "access")
        .signWith(SIGN_KEY)
        .compact();
  }
}
