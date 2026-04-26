package com.dataforge.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户实体类。
 *
 * <p>存储用户认证和基本信息，实现Spring Security的UserDetails接口。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"password"})
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Column(unique = true, nullable = false, length = 50)
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  @Column(unique = true, nullable = false, length = 100)
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password must be at least 6 characters")
  @Column(nullable = false, length = 255)
  private String password;

  @Size(max = 100)
  @Column(length = 100)
  private String fullName;

  @Column(length = 20)
  private String role;

  @Column(nullable = false)
  @Builder.Default
  private boolean enabled = true;

  @Column(nullable = false)
  @Builder.Default
  private boolean accountNonExpired = true;

  @Column(nullable = false)
  @Builder.Default
  private boolean accountNonLocked = true;

  @Column(nullable = false)
  @Builder.Default
  private boolean credentialsNonExpired = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "login_attempts")
  @Builder.Default
  private Integer loginAttempts = 0;

  @Column(name = "lock_time")
  private LocalDateTime lockTime;

  /** 在持久化前自动设置创建时间。 */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  /** 在更新前自动设置更新时间。 */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    if (role == null || role.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
  }

  @Override
  public boolean isAccountNonExpired() {
    return accountNonExpired;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return credentialsNonExpired;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * 增加登录失败次数。
   *
   * @return 当前失败次数
   */
  public int incrementLoginAttempts() {
    if (this.loginAttempts == null) {
      this.loginAttempts = 0;
    }
    this.loginAttempts++;
    return this.loginAttempts;
  }

  /** 重置登录失败次数。 */
  public void resetLoginAttempts() {
    this.loginAttempts = 0;
    this.lockTime = null;
  }

  /** 锁定账户。 */
  public void lockAccount() {
    this.accountNonLocked = false;
    this.lockTime = LocalDateTime.now();
  }

  /** 解锁账户。 */
  public void unlockAccount() {
    this.accountNonLocked = true;
    this.lockTime = null;
    this.loginAttempts = 0;
  }

  /** 更新最后登录时间。 */
  public void updateLastLoginTime() {
    this.lastLoginAt = LocalDateTime.now();
  }
}
