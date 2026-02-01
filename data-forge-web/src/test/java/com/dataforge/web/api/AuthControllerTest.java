package com.dataforge.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dataforge.web.controller.AuthController;
import com.dataforge.web.security.JwtUtil;
import com.dataforge.web.security.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AuthController 测试类。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private AuthenticationManager authenticationManager;

  @MockBean private JwtUtil jwtUtil;

  @MockBean private TokenBlacklistService tokenBlacklistService;

  @MockBean private org.springframework.data.redis.core.RedisTemplate<?, ?> redisTemplate;

  @MockBean private com.dataforge.service.DataForgeService dataForgeService;

  @MockBean private com.dataforge.web.service.AsyncDataGenerationService asyncDataGenerationService;

  @MockBean private com.dataforge.web.service.GenerationHistoryService generationHistoryService;

  @MockBean private com.dataforge.web.service.MetricsService metricsService;

  @MockBean private com.dataforge.web.service.DataTemplateService dataTemplateService;

  @MockBean private com.dataforge.core.GeneratorFactory generatorFactory;

  @MockBean private com.dataforge.core.CacheManager cacheManager;

  @MockBean private com.dataforge.web.cache.MultiLevelCacheManager multiLevelCacheManager;

  @Test
  void testLoginSuccess() throws Exception {
    // 准备测试数据
    AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
    loginRequest.setUsername("testuser");
    loginRequest.setPassword("testpass");

    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
    when(authentication.getName()).thenReturn("testuser");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtUtil.generateToken("testuser")).thenReturn("mock-jwt-token");
    when(jwtUtil.generateRefreshToken("testuser")).thenReturn("mock-refresh-token");
    when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);

    // 执行测试
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("Login successful"))
        .andExpect(jsonPath("$.data.accessToken").value("mock-jwt-token"))
        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.data.username").value("testuser"));
  }

  @Test
  void testLoginWithInvalidCredentials() throws Exception {
    // 准备测试数据
    AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
    loginRequest.setUsername("testuser");
    loginRequest.setPassword("wrongpass");

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    // 执行测试
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401))
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
  }

  @Test
  void testLoginWithMissingFields() throws Exception {
    // 准备测试数据 - 缺少密码
    AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
    loginRequest.setUsername("testuser");
    // password 为 null

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    // 执行测试
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  void testLoginWithServerError() throws Exception {
    // 准备测试数据
    AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
    loginRequest.setUsername("testuser");
    loginRequest.setPassword("testpass");

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new RuntimeException("Database connection failed"));

    // 执行测试
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(500))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Login failed")));
  }
}
