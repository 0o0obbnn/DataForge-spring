package com.dataforge.web.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * JWT配置属性类。
 *
 * <p>集中管理JWT相关的配置属性，支持从application.yml加载配置。
 *
 * <p><strong>配置示例：</strong>
 *
 * <pre>
 * app:
 *   jwt:
 *     secret: your-secret-key-here
 *     expiration: 3600000
 *     refresh-expiration: 604800000
 * </pre>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

  // JWT时间常量（毫秒）
  private static final long ONE_SECOND = 1000;
  private static final long ONE_HOUR = 3600000;
  private static final long SEVEN_DAYS = 604800000;

  /** JWT签名密钥。 */
  @NotBlank(message = "JWT secret cannot be blank")
  private String secret;

  /** Access Token过期时间（毫秒），默认1小时。 */
  @Min(value = ONE_SECOND, message = "JWT expiration must be at least 1000ms")
  private long expiration = ONE_HOUR;

  /** Refresh Token过期时间（毫秒），默认7天。 */
  @Min(value = ONE_SECOND, message = "JWT refresh expiration must be at least 1000ms")
  private long refreshExpiration = SEVEN_DAYS;

  /**
   * 获取JWT签名密钥。
   *
   * @return JWT签名密钥
   */
  public String getSecret() {
    return secret;
  }

  /**
   * 设置JWT签名密钥。
   *
   * @param secret JWT签名密钥
   */
  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * 获取Access Token过期时间（毫秒）。
   *
   * @return Access Token过期时间（毫秒）
   */
  public long getExpiration() {
    return expiration;
  }

  /**
   * 设置Access Token过期时间（毫秒）。
   *
   * @param expiration Access Token过期时间（毫秒）
   */
  public void setExpiration(long expiration) {
    this.expiration = expiration;
  }

  /**
   * 获取Refresh Token过期时间（毫秒）。
   *
   * @return Refresh Token过期时间（毫秒）
   */
  public long getRefreshExpiration() {
    return refreshExpiration;
  }

  /**
   * 设置Refresh Token过期时间（毫秒）。
   *
   * @param refreshExpiration Refresh Token过期时间（毫秒）
   */
  public void setRefreshExpiration(long refreshExpiration) {
    this.refreshExpiration = refreshExpiration;
  }

  @Override
  public String toString() {
    return "JwtProperties{"
        + "expiration="
        + expiration
        + ", refreshExpiration="
        + refreshExpiration
        + '}';
  }
}
