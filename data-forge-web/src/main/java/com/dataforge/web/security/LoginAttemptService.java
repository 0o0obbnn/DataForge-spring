package com.dataforge.web.security;

import com.dataforge.web.entity.User;
import com.dataforge.web.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录尝试服务。
 *
 * <p>管理用户登录失败次数和账户锁定策略，支持基于内存和Redis的两种实现模式。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

  private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

  private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
  private static final String ACCOUNT_LOCK_PREFIX = "account:lock:";

  private final UserRepository userRepository;
  private final RedisTemplate<String, String> redisTemplate;

  @Value("${app.security.max-login-attempts:5}")
  private int maxLoginAttempts;

  @Value("${app.security.lock-duration-minutes:30}")
  private int lockDurationMinutes;

  /**
   * 记录登录失败。
   *
   * @param username 用户名
   */
  @Transactional
  public void recordFailedAttempt(String username) {
    logger.warn("Recording failed login attempt for user: {}", username);

    // 更新数据库中的失败次数
    userRepository.incrementLoginAttempts(username);

    // 获取用户检查是否需要锁定
    userRepository
        .findByUsername(username)
        .ifPresent(
            user -> {
              int attempts = user.getLoginAttempts() != null ? user.getLoginAttempts() + 1 : 1;

              // 如果达到最大尝试次数，锁定账户
              if (attempts >= maxLoginAttempts) {
                lockAccount(user);
              }

              // 同时更新Redis缓存（失败时降级到仅数据库）
              String attemptKey = LOGIN_ATTEMPT_PREFIX + username;
              try {
                redisTemplate.opsForValue().increment(attemptKey);
                redisTemplate.expire(attemptKey, lockDurationMinutes, TimeUnit.MINUTES);
              } catch (Exception ex) {
                logger.warn("Redis unavailable when recording failed attempt for user: {}", username);
              }
            });
  }

  /**
   * 记录登录成功。
   *
   * @param username 用户名
   */
  @Transactional
  public void recordSuccessAttempt(String username) {
    logger.info("Recording successful login for user: {}", username);

    // 重置数据库中的失败次数
    userRepository.resetLoginAttemptsAndUnlock(username);

    // 更新最后登录时间
    userRepository.updateLastLoginTime(username, LocalDateTime.now());

    // 清除Redis缓存（失败时降级）
    try {
      redisTemplate.delete(LOGIN_ATTEMPT_PREFIX + username);
      redisTemplate.delete(ACCOUNT_LOCK_PREFIX + username);
    } catch (Exception ex) {
      logger.warn("Redis unavailable when recording success attempt for user: {}", username);
    }
  }

  /**
   * 检查账户是否被锁定。
   *
   * @param username 用户名
   * @return 如果被锁定返回true
   */
  public boolean isAccountLocked(String username) {
    String lockKey = ACCOUNT_LOCK_PREFIX + username;
    boolean redisLocked = false;
    try {
      redisLocked = Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    } catch (Exception ex) {
      logger.warn("Redis unavailable when checking lock status for user: {}", username);
    }
    final boolean redisLockedFinal = redisLocked;

    // 以数据库为最终真值，避免 Redis 残留锁导致账号“假锁定”
    return userRepository
        .findByUsername(username)
        .map(
            user -> {
              if (redisLockedFinal && user.isAccountNonLocked()) {
                logger.info("Found stale Redis lock for user: {}, clearing it", username);
                try {
                  redisTemplate.delete(lockKey);
                  redisTemplate.delete(LOGIN_ATTEMPT_PREFIX + username);
                } catch (Exception ex) {
                  logger.warn("Redis unavailable when clearing stale lock for user: {}", username);
                }
                return false;
              }

              if (redisLockedFinal) {
                return true;
              }

              if (!user.isAccountNonLocked()) {
                if (user.getLockTime() != null) {
                  long minutesLocked =
                      ChronoUnit.MINUTES.between(user.getLockTime(), LocalDateTime.now());
                  if (minutesLocked >= lockDurationMinutes) {
                    unlockAccount(user);
                    return false;
                  }
                }
                return true;
              }
              return false;
            })
        .orElse(redisLockedFinal);
  }

  /**
   * 获取剩余锁定时间（分钟）。
   *
   * @param username 用户名
   * @return 剩余锁定时间，如果未锁定返回0
   */
  public long getRemainingLockTime(String username) {
    String lockKey = ACCOUNT_LOCK_PREFIX + username;
    try {
      Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
      if (ttl != null && ttl > 0) {
        return ttl;
      }
    } catch (Exception ex) {
      logger.warn("Redis unavailable when reading remaining lock time for user: {}", username);
    }

    // 检查数据库
    return userRepository
        .findByUsername(username)
        .map(
            user -> {
              if (!user.isAccountNonLocked() && user.getLockTime() != null) {
                long minutesLocked =
                    ChronoUnit.MINUTES.between(user.getLockTime(), LocalDateTime.now());
                long remaining = lockDurationMinutes - minutesLocked;
                return Math.max(remaining, 0);
              }
              return 0L;
            })
        .orElse(0L);
  }

  /**
   * 锁定账户。
   *
   * @param user 用户实体
   */
  private void lockAccount(User user) {
    logger.warn(
        "Locking account for user: {} due to too many failed login attempts", user.getUsername());

    // 更新数据库
    userRepository.lockAccount(user.getUsername(), LocalDateTime.now());

    // 设置Redis缓存（失败时降级）
    String lockKey = ACCOUNT_LOCK_PREFIX + user.getUsername();
    try {
      redisTemplate.opsForValue().set(lockKey, "locked", lockDurationMinutes, TimeUnit.MINUTES);
    } catch (Exception ex) {
      logger.warn("Redis unavailable when locking account for user: {}", user.getUsername());
    }
  }

  /**
   * 解锁账户。
   *
   * @param user 用户实体
   */
  private void unlockAccount(User user) {
    logger.info("Auto-unlocking account for user: {}", user.getUsername());

    // 更新数据库
    userRepository.resetLoginAttemptsAndUnlock(user.getUsername());

    // 清除Redis缓存（失败时降级）
    try {
      redisTemplate.delete(ACCOUNT_LOCK_PREFIX + user.getUsername());
      redisTemplate.delete(LOGIN_ATTEMPT_PREFIX + user.getUsername());
    } catch (Exception ex) {
      logger.warn("Redis unavailable when unlocking user: {}", user.getUsername());
    }
  }

  /**
   * 手动解锁账户（管理员使用）。
   *
   * @param username 用户名
   */
  @Transactional
  public void manualUnlock(String username) {
    logger.info("Manually unlocking account for user: {}", username);

    userRepository.resetLoginAttemptsAndUnlock(username);
    try {
      redisTemplate.delete(ACCOUNT_LOCK_PREFIX + username);
      redisTemplate.delete(LOGIN_ATTEMPT_PREFIX + username);
    } catch (Exception ex) {
      logger.warn("Redis unavailable when manually unlocking user: {}", username);
    }
  }

  /**
   * 获取当前登录失败次数。
   *
   * @param username 用户名
   * @return 失败次数
   */
  public int getCurrentAttempts(String username) {
    String attemptKey = LOGIN_ATTEMPT_PREFIX + username;
    try {
      String attempts = redisTemplate.opsForValue().get(attemptKey);
      if (attempts != null) {
        try {
          return Integer.parseInt(attempts);
        } catch (NumberFormatException e) {
          logger.warn("Invalid attempt count in Redis for user: {}", username);
        }
      }
    } catch (Exception ex) {
      logger.warn("Redis unavailable when reading attempt count for user: {}", username);
    }

    // 再检查数据库
    return userRepository
        .findByUsername(username)
        .map(user -> user.getLoginAttempts() != null ? user.getLoginAttempts() : 0)
        .orElse(0);
  }
}
