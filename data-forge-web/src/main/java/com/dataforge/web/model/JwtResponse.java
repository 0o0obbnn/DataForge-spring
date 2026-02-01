package com.dataforge.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT认证响应。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JWT认证响应，包含access token和refresh token")
public class JwtResponse {

  /** Access Token，用于API认证，有效期较短（如1小时） */
  @Schema(description = "Access Token，用于API认证", example = "eyJhbGciOiJIUzI1NiJ9...")
  private String accessToken;

  /** Refresh Token，用于刷新Access Token，有效期较长（如7天） */
  @Schema(description = "Refresh Token，用于刷新Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
  private String refreshToken;

  /** Token类型 */
  @Schema(description = "Token类型", example = "Bearer")
  private String tokenType;

  /** Access Token过期时间（毫秒） */
  @Schema(description = "Access Token过期时间（毫秒）", example = "3600000")
  private Long expiresIn;

  /** 用户名 */
  @Schema(description = "用户名", example = "admin")
  private String username;

  /**
   * 创建默认Token响应。
   *
   * @param accessToken Access Token
   * @param refreshToken Refresh Token
   * @param username 用户名
   * @param expiresIn Access Token过期时间（毫秒）
   * @return JwtResponse
   */
  public static JwtResponse of(
      String accessToken, String refreshToken, String username, Long expiresIn) {
    return JwtResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(expiresIn)
        .username(username)
        .build();
  }
}
