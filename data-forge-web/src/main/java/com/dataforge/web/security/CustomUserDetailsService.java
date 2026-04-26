package com.dataforge.web.security;

import com.dataforge.web.entity.User;
import com.dataforge.web.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 自定义用户详情服务。
 *
 * <p>从数据库加载用户信息进行认证，支持账户锁定检查和登录失败记录。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

  private final UserRepository userRepository;
  private final LoginAttemptService loginAttemptService;

  /**
   * 根据用户名加载用户信息。
   *
   * <p>首先检查账户是否被锁定，然后从数据库查询用户信息。 如果用户不存在或账户被禁用，将抛出相应的异常。
   *
   * @param username 用户名
   * @return UserDetails 用户详情
   * @throws UsernameNotFoundException 用户不存在时抛出
   * @throws org.springframework.security.authentication.LockedException 账户被锁定时抛出
   * @throws org.springframework.security.authentication.DisabledException 账户被禁用时抛出
   */
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    logger.debug("Loading user by username: {}", username);

    // 检查账户是否被锁定
    if (loginAttemptService.isAccountLocked(username)) {
      long remainingMinutes = loginAttemptService.getRemainingLockTime(username);
      logger.warn(
          "Account is locked for user: {}, remaining lock time: {} minutes",
          username,
          remainingMinutes);
      throw new org.springframework.security.authentication.LockedException(
          String.format("Account is locked. Please try again after %d minutes", remainingMinutes));
    }

    // 从数据库查询用户
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  logger.warn("User not found with username: {}", username);
                  return new UsernameNotFoundException("User not found with username: " + username);
                });

    // 检查账户是否被禁用
    if (!user.isEnabled()) {
      logger.warn("Account is disabled for user: {}", username);
      throw new org.springframework.security.authentication.DisabledException(
          "Account is disabled");
    }

    // 检查账户是否过期
    if (!user.isAccountNonExpired()) {
      logger.warn("Account has expired for user: {}", username);
      throw new org.springframework.security.authentication.AccountExpiredException(
          "Account has expired");
    }

    // 检查凭证是否过期
    if (!user.isCredentialsNonExpired()) {
      logger.warn("Credentials have expired for user: {}", username);
      throw new org.springframework.security.authentication.CredentialsExpiredException(
          "Credentials have expired");
    }

    logger.debug("Successfully loaded user: {}", username);
    return user;
  }

  /**
   * 根据邮箱加载用户信息。
   *
   * <p>用于支持邮箱登录的场景。
   *
   * @param email 邮箱地址
   * @return UserDetails 用户详情
   * @throws UsernameNotFoundException 用户不存在时抛出
   */
  @Transactional(readOnly = true)
  public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
    logger.debug("Loading user by email: {}", email);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () -> {
                  logger.warn("User not found with email: {}", email);
                  return new UsernameNotFoundException("User not found with email: " + email);
                });

    // 检查账户是否被锁定
    if (loginAttemptService.isAccountLocked(user.getUsername())) {
      long remainingMinutes = loginAttemptService.getRemainingLockTime(user.getUsername());
      throw new org.springframework.security.authentication.LockedException(
          String.format("Account is locked. Please try again after %d minutes", remainingMinutes));
    }

    return user;
  }

  /**
   * 检查用户是否存在。
   *
   * @param username 用户名
   * @return 如果存在返回true
   */
  public boolean userExists(String username) {
    return userRepository.existsByUsername(username);
  }

  /**
   * 检查邮箱是否已注册。
   *
   * @param email 邮箱地址
   * @return 如果已注册返回true
   */
  public boolean emailExists(String email) {
    return userRepository.existsByEmail(email);
  }

  /**
   * 根据用户名查找用户（可选）。
   *
   * @param username 用户名
   * @return 用户Optional对象
   */
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }
}
