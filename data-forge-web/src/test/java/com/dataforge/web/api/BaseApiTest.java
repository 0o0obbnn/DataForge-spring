package com.dataforge.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dataforge.web.security.JwtUtil;
import com.dataforge.web.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// @ExtendWith(SpringExtension.class) removed as it is included in @SpringBootTest
@org.springframework.boot.test.context.SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseApiTest {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected WebApplicationContext webApplicationContext;

  @MockBean protected RedisTemplate<String, Object> redisTemplate;

  @MockBean protected TokenBlacklistService tokenBlacklistService;

  @MockBean protected JwtUtil jwtUtil;

  @MockBean protected AuthenticationManager authenticationManager;

  @MockBean protected RedisConnectionFactory redisConnectionFactory;

  @BeforeEach
  public void setup() {
    @SuppressWarnings("unchecked")
    ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(any())).thenReturn(null);

    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  protected String getAuthToken() {
    return "Bearer mock-jwt-token";
  }
}
