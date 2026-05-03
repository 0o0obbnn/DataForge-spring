package com.dataforge.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dataforge.web.entity.User;
import com.dataforge.web.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DisplayName("LoginAttemptService 测试")
class LoginAttemptServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private RedisTemplate<String, String> redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  @InjectMocks private LoginAttemptService loginAttemptService;

  private static final String TEST_USERNAME = "testuser";
  private static final int MAX_ATTEMPTS = 5;
  private static final int LOCK_DURATION_MINUTES = 30;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    setPrivateField(loginAttemptService, "maxLoginAttempts", MAX_ATTEMPTS);
    setPrivateField(loginAttemptService, "lockDurationMinutes", LOCK_DURATION_MINUTES);
  }

  private void setPrivateField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to set private field: " + fieldName, e);
    }
  }

  @Nested
  @DisplayName("记录登录失败测试")
  class RecordFailedAttemptTests {

    @Test
    @DisplayName("应成功记录登录失败")
    void shouldRecordFailedAttemptSuccessfully() {
      User user = createUser(TEST_USERNAME, 0, true);

      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      loginAttemptService.recordFailedAttempt(TEST_USERNAME);

      verify(userRepository).incrementLoginAttempts(TEST_USERNAME);
      verify(valueOperations).increment(eq("login:attempt:" + TEST_USERNAME));
      verify(redisTemplate)
          .expire(eq("login:attempt:" + TEST_USERNAME), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("达到最大尝试次数时应锁定账户")
    void shouldLockAccountWhenMaxAttemptsReached() {
      User user = createUser(TEST_USERNAME, MAX_ATTEMPTS - 1, true);

      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      loginAttemptService.recordFailedAttempt(TEST_USERNAME);

      verify(userRepository).lockAccount(eq(TEST_USERNAME), any(LocalDateTime.class));
      verify(valueOperations)
          .set(eq("account:lock:" + TEST_USERNAME), eq("locked"), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("用户不存在时应不执行操作")
    void shouldDoNothingWhenUserNotExists() {
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

      loginAttemptService.recordFailedAttempt(TEST_USERNAME);

      verify(userRepository).incrementLoginAttempts(TEST_USERNAME);
      verify(valueOperations, never()).increment(anyString());
    }

    @Test
    @DisplayName("应正确递增失败次数")
    void shouldCorrectlyIncrementAttempts() {
      User user = createUser(TEST_USERNAME, 2, true);

      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      loginAttemptService.recordFailedAttempt(TEST_USERNAME);

      verify(userRepository).incrementLoginAttempts(TEST_USERNAME);
      verify(valueOperations).increment(eq("login:attempt:" + TEST_USERNAME));
    }
  }

  @Nested
  @DisplayName("记录登录成功测试")
  class RecordSuccessAttemptTests {

    @Test
    @DisplayName("应成功记录登录成功")
    void shouldRecordSuccessAttemptSuccessfully() {
      loginAttemptService.recordSuccessAttempt(TEST_USERNAME);

      verify(userRepository).resetLoginAttemptsAndUnlock(TEST_USERNAME);
      verify(userRepository).updateLastLoginTime(eq(TEST_USERNAME), any(LocalDateTime.class));
      verify(redisTemplate).delete("login:attempt:" + TEST_USERNAME);
      verify(redisTemplate).delete("account:lock:" + TEST_USERNAME);
    }

    @Test
    @DisplayName("应清除所有相关的Redis缓存")
    void shouldClearAllRelatedRedisCache() {
      loginAttemptService.recordSuccessAttempt(TEST_USERNAME);

      verify(redisTemplate).delete("login:attempt:" + TEST_USERNAME);
      verify(redisTemplate).delete("account:lock:" + TEST_USERNAME);
    }
  }

  @Nested
  @DisplayName("检查账户锁定状态测试")
  class IsAccountLockedTests {

    @Test
    @DisplayName("Redis中存在锁定时应返回true")
    void shouldReturnTrueWhenLockedInRedis() {
      when(redisTemplate.hasKey("account:lock:" + TEST_USERNAME)).thenReturn(true);

      boolean isLocked = loginAttemptService.isAccountLocked(TEST_USERNAME);

      assertThat(isLocked).isTrue();
    }

    @Test
    @DisplayName("数据库中账户被锁定时应返回true")
    void shouldReturnTrueWhenLockedInDatabase() {
      User user = createUser(TEST_USERNAME, 0, false);
      when(redisTemplate.hasKey("account:lock:" + TEST_USERNAME)).thenReturn(false);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      boolean isLocked = loginAttemptService.isAccountLocked(TEST_USERNAME);

      assertThat(isLocked).isTrue();
    }

    @Test
    @DisplayName("账户未锁定时应返回false")
    void shouldReturnFalseWhenAccountNotLocked() {
      User user = createUser(TEST_USERNAME, 0, true);
      when(redisTemplate.hasKey("account:lock:" + TEST_USERNAME)).thenReturn(false);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      boolean isLocked = loginAttemptService.isAccountLocked(TEST_USERNAME);

      assertThat(isLocked).isFalse();
    }

    @Test
    @DisplayName("锁定时间过期时应自动解锁")
    void shouldAutoUnlockWhenLockTimeExpired() {
      User user = createUser(TEST_USERNAME, 0, false);
      user.setLockTime(LocalDateTime.now().minusMinutes(LOCK_DURATION_MINUTES + 1));

      when(redisTemplate.hasKey("account:lock:" + TEST_USERNAME)).thenReturn(false);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      boolean isLocked = loginAttemptService.isAccountLocked(TEST_USERNAME);

      assertThat(isLocked).isFalse();
      verify(userRepository).resetLoginAttemptsAndUnlock(TEST_USERNAME);
    }

    @Test
    @DisplayName("用户不存在时应返回false")
    void shouldReturnFalseWhenUserNotExists() {
      when(redisTemplate.hasKey("account:lock:" + TEST_USERNAME)).thenReturn(false);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

      boolean isLocked = loginAttemptService.isAccountLocked(TEST_USERNAME);

      assertThat(isLocked).isFalse();
    }
  }

  @Nested
  @DisplayName("获取剩余锁定时间测试")
  class GetRemainingLockTimeTests {

    @Test
    @DisplayName("应从Redis获取剩余锁定时间")
    void shouldGetRemainingLockTimeFromRedis() {
      when(redisTemplate.getExpire("account:lock:" + TEST_USERNAME, TimeUnit.MINUTES))
          .thenReturn(15L);

      long remainingTime = loginAttemptService.getRemainingLockTime(TEST_USERNAME);

      assertThat(remainingTime).isEqualTo(15);
    }

    @Test
    @DisplayName("应从数据库获取剩余锁定时间")
    void shouldGetRemainingLockTimeFromDatabase() {
      User user = createUser(TEST_USERNAME, 0, false);
      user.setLockTime(LocalDateTime.now().minusMinutes(10));

      when(redisTemplate.getExpire("account:lock:" + TEST_USERNAME, TimeUnit.MINUTES))
          .thenReturn(null);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      long remainingTime = loginAttemptService.getRemainingLockTime(TEST_USERNAME);

      assertThat(remainingTime).isEqualTo(LOCK_DURATION_MINUTES - 10);
    }

    @Test
    @DisplayName("账户未锁定时应返回0")
    void shouldReturnZeroWhenAccountNotLocked() {
      User user = createUser(TEST_USERNAME, 0, true);

      when(redisTemplate.getExpire("account:lock:" + TEST_USERNAME, TimeUnit.MINUTES))
          .thenReturn(null);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      long remainingTime = loginAttemptService.getRemainingLockTime(TEST_USERNAME);

      assertThat(remainingTime).isEqualTo(0);
    }

    @Test
    @DisplayName("用户不存在时应返回0")
    void shouldReturnZeroWhenUserNotExists() {
      when(redisTemplate.getExpire("account:lock:" + TEST_USERNAME, TimeUnit.MINUTES))
          .thenReturn(null);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

      long remainingTime = loginAttemptService.getRemainingLockTime(TEST_USERNAME);

      assertThat(remainingTime).isEqualTo(0);
    }

    @Test
    @DisplayName("锁定时间已过期时应返回0")
    void shouldReturnZeroWhenLockTimeExpired() {
      User user = createUser(TEST_USERNAME, 0, false);
      user.setLockTime(LocalDateTime.now().minusMinutes(LOCK_DURATION_MINUTES + 5));

      when(redisTemplate.getExpire("account:lock:" + TEST_USERNAME, TimeUnit.MINUTES))
          .thenReturn(null);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      long remainingTime = loginAttemptService.getRemainingLockTime(TEST_USERNAME);

      assertThat(remainingTime).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("手动解锁账户测试")
  class ManualUnlockTests {

    @Test
    @DisplayName("应成功手动解锁账户")
    void shouldManualUnlockSuccessfully() {
      loginAttemptService.manualUnlock(TEST_USERNAME);

      verify(userRepository).resetLoginAttemptsAndUnlock(TEST_USERNAME);
      verify(redisTemplate).delete("account:lock:" + TEST_USERNAME);
      verify(redisTemplate).delete("login:attempt:" + TEST_USERNAME);
    }

    @Test
    @DisplayName("应清除所有相关的Redis缓存")
    void shouldClearAllRedisCacheOnManualUnlock() {
      loginAttemptService.manualUnlock(TEST_USERNAME);

      verify(redisTemplate, times(1)).delete("account:lock:" + TEST_USERNAME);
      verify(redisTemplate, times(1)).delete("login:attempt:" + TEST_USERNAME);
    }
  }

  @Nested
  @DisplayName("获取当前尝试次数测试")
  class GetCurrentAttemptsTests {

    @Test
    @DisplayName("应从Redis获取当前尝试次数")
    void shouldGetCurrentAttemptsFromRedis() {
      when(redisTemplate.opsForValue().get("login:attempt:" + TEST_USERNAME)).thenReturn("3");

      int attempts = loginAttemptService.getCurrentAttempts(TEST_USERNAME);

      assertThat(attempts).isEqualTo(3);
    }

    @Test
    @DisplayName("应从数据库获取当前尝试次数")
    void shouldGetCurrentAttemptsFromDatabase() {
      User user = createUser(TEST_USERNAME, 2, true);

      when(redisTemplate.opsForValue().get("login:attempt:" + TEST_USERNAME)).thenReturn(null);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      int attempts = loginAttemptService.getCurrentAttempts(TEST_USERNAME);

      assertThat(attempts).isEqualTo(2);
    }

    @Test
    @DisplayName("用户不存在时应返回0")
    void shouldReturnZeroWhenUserNotExists() {
      when(redisTemplate.opsForValue().get("login:attempt:" + TEST_USERNAME)).thenReturn(null);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

      int attempts = loginAttemptService.getCurrentAttempts(TEST_USERNAME);

      assertThat(attempts).isEqualTo(0);
    }

    @Test
    @DisplayName("数据库中尝试次数为null时应返回0")
    void shouldReturnZeroWhenAttemptsIsNullInDatabase() {
      User user = createUser(TEST_USERNAME, null, true);

      when(redisTemplate.opsForValue().get("login:attempt:" + TEST_USERNAME)).thenReturn(null);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      int attempts = loginAttemptService.getCurrentAttempts(TEST_USERNAME);

      assertThat(attempts).isEqualTo(0);
    }

    @Test
    @DisplayName("Redis中数据格式错误时应回退到数据库")
    void shouldFallbackToDatabaseWhenRedisDataInvalid() {
      User user = createUser(TEST_USERNAME, 2, true);

      when(redisTemplate.opsForValue().get("login:attempt:" + TEST_USERNAME)).thenReturn("invalid");
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      int attempts = loginAttemptService.getCurrentAttempts(TEST_USERNAME);

      assertThat(attempts).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("边界条件测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("应处理最大尝试次数为1的情况")
    void shouldHandleMaxAttemptsOfOne() {
      User user = createUser(TEST_USERNAME, 0, true);
      setPrivateField(loginAttemptService, "maxLoginAttempts", 1);

      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      loginAttemptService.recordFailedAttempt(TEST_USERNAME);

      verify(userRepository).lockAccount(eq(TEST_USERNAME), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("应处理锁定时间为0的情况")
    void shouldHandleZeroLockDuration() {
      User user = createUser(TEST_USERNAME, MAX_ATTEMPTS, false);
      setPrivateField(loginAttemptService, "lockDurationMinutes", 0);

      when(redisTemplate.hasKey("account:lock:" + TEST_USERNAME)).thenReturn(false);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));

      boolean isLocked = loginAttemptService.isAccountLocked(TEST_USERNAME);

      assertThat(isLocked).isFalse();
    }

    @Test
    @DisplayName("应处理Redis返回null的情况")
    void shouldHandleNullRedisResponse() {
      when(redisTemplate.hasKey("account:lock:" + TEST_USERNAME)).thenReturn(null);
      when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

      boolean isLocked = loginAttemptService.isAccountLocked(TEST_USERNAME);

      assertThat(isLocked).isFalse();
    }
  }

  private User createUser(String username, Integer loginAttempts, boolean accountNonLocked) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(username + "@example.com");
    user.setPassword("password");
    user.setRole("USER");
    user.setEnabled(true);
    user.setAccountNonExpired(true);
    user.setAccountNonLocked(accountNonLocked);
    user.setCredentialsNonExpired(true);
    user.setLoginAttempts(loginAttempts);
    user.setLockTime(accountNonLocked ? null : LocalDateTime.now());
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    return user;
  }
}
