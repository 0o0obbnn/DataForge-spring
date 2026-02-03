package com.dataforge.web.repository;

import com.dataforge.web.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问接口。
 *
 * <p>提供用户实体的CRUD操作和自定义查询方法。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * 根据用户名查找用户。
   *
   * @param username 用户名
   * @return 用户Optional对象
   */
  Optional<User> findByUsername(String username);

  /**
   * 根据邮箱查找用户。
   *
   * @param email 邮箱地址
   * @return 用户Optional对象
   */
  Optional<User> findByEmail(String email);

  /**
   * 检查用户名是否已存在。
   *
   * @param username 用户名
   * @return 如果存在返回true
   */
  boolean existsByUsername(String username);

  /**
   * 检查邮箱是否已存在。
   *
   * @param email 邮箱地址
   * @return 如果存在返回true
   */
  boolean existsByEmail(String email);

  /**
   * 更新用户最后登录时间。
   *
   * @param username 用户名
   * @param loginTime 登录时间
   */
  @Modifying
  @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.username = :username")
  void updateLastLoginTime(
      @Param("username") String username, @Param("loginTime") LocalDateTime loginTime);

  /**
   * 增加登录失败次数。
   *
   * @param username 用户名
   */
  @Modifying
  @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.username = :username")
  void incrementLoginAttempts(@Param("username") String username);

  /**
   * 重置登录失败次数并解锁账户。
   *
   * @param username 用户名
   */
  @Modifying
  @Query(
      "UPDATE User u SET u.loginAttempts = 0, u.accountNonLocked = true, u.lockTime = null WHERE u.username = :username")
  void resetLoginAttemptsAndUnlock(@Param("username") String username);

  /**
   * 锁定账户。
   *
   * @param username 用户名
   * @param lockTime 锁定时间
   */
  @Modifying
  @Query(
      "UPDATE User u SET u.accountNonLocked = false, u.lockTime = :lockTime WHERE u.username = :username")
  void lockAccount(@Param("username") String username, @Param("lockTime") LocalDateTime lockTime);
}
