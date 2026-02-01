package com.dataforge.web.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新Token请求。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "刷新Token请求")
public class RefreshTokenRequest {

  @NotBlank(message = "Refresh token cannot be blank")
  @Schema(
      description = "Refresh Token",
      example = "eyJhbGciOiJIUzI1NiJ9...",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String refreshToken;
}
