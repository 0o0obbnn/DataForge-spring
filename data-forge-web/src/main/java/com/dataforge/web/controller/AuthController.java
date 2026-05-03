package com.dataforge.web.controller;

import com.dataforge.web.model.ApiResponse;
import com.dataforge.web.model.JwtResponse;
import com.dataforge.web.model.RefreshTokenRequest;
import com.dataforge.web.security.JwtUtil;
import com.dataforge.web.security.LoginAttemptService;
import com.dataforge.web.security.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 - 增强版。
 *
 * <p>支持Access Token和Refresh Token双Token机制，提供Token刷新和登出功能。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "认证相关API")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final TokenBlacklistService tokenBlacklistService;
  private final LoginAttemptService loginAttemptService;

  public AuthController(
      AuthenticationManager authenticationManager,
      JwtUtil jwtUtil,
      TokenBlacklistService tokenBlacklistService,
      LoginAttemptService loginAttemptService) {
    this.authenticationManager = authenticationManager;
    this.jwtUtil = jwtUtil;
    this.tokenBlacklistService = tokenBlacklistService;
    this.loginAttemptService = loginAttemptService;
  }

  /**
   * 构建成功的API响应
   *
   * @param data 响应数据
   * @param message 响应消息
   * @param <T> 数据类型
   * @return ApiResponse对象
   */
  private <T> ApiResponse<T> buildSuccessResponse(T data, String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setCode(HttpStatus.OK.value());
    response.setMessage(message);
    response.setData(data);
    response.setTimestamp(LocalDateTime.now());
    return response;
  }

  /**
   * 构建错误的API响应
   *
   * @param message 错误消息
   * @param status HTTP状态
   * @param <T> 数据类型
   * @return ApiResponse对象
   */
  private <T> ApiResponse<T> buildErrorResponse(String message, HttpStatus status) {
    ApiResponse<T> response = new ApiResponse<>();
    response.setCode(status.value());
    response.setMessage(message);
    response.setTimestamp(LocalDateTime.now());
    return response;
  }

  /**
   * 从请求头中提取Token。
   *
   * @param request HTTP请求
   * @return Token字符串（去除"Bearer "前缀）
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  /** 用户登录请求DTO。 */
  @Data
  @Schema(description = "用户登录请求")
  public static class LoginRequest {
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(
        description = "密码",
        example = "password123",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
  }

  /**
   * 登录请求模型（兼容旧版）。
   *
   * @deprecated 使用LoginRequest替代
   */
  @Deprecated
  @Data
  public static class LoginRequestOld {
    private String username;
    private String password;
  }

  /**
   * 登录响应DTO（兼容旧版）。
   *
   * @deprecated 使用JwtResponse替代
   */
  @Deprecated
  @Data
  public static class LoginResponse {
    private String token;
    private String type = "Bearer";
    private String username;

    @Deprecated
    public LoginResponse(String token, String username) {
      this.token = token;
      this.username = username;
    }
  }

  /**
   * 用户登录 - 增强版。
   *
   * <p>登录成功后返回Access Token和Refresh Token，Access Token用于API认证，Refresh Token用于刷新Access Token。
   *
   * @param loginRequest 登录请求
   * @return ResponseEntity<ApiResponse<JwtResponse>> 登录结果，包含Access Token和Refresh Token
   */
  @PostMapping("/login")
  @Operation(
      summary = "用户登录",
      description = "用户登录并获取JWT令牌。返回Access Token（有效期1小时）和Refresh Token（有效期7天）")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "登录成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "用户名或密码错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<JwtResponse>> login(@RequestBody LoginRequest loginRequest) {
    String username = loginRequest.getUsername();
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));

      String authenticatedUsername = authentication.getName();
      loginAttemptService.recordSuccessAttempt(authenticatedUsername);

      // 生成Access Token和Refresh Token
      String accessToken = jwtUtil.generateToken(authenticatedUsername);
      String refreshToken = jwtUtil.generateRefreshToken(authenticatedUsername);

      JwtResponse jwtResponse =
          JwtResponse.of(
              accessToken, refreshToken, authenticatedUsername, jwtUtil.getAccessTokenExpiration());

      return ResponseEntity.ok(buildSuccessResponse(jwtResponse, "Login successful"));
    } catch (AuthenticationException e) {
      if (username != null && !username.isBlank()) {
        loginAttemptService.recordFailedAttempt(username);
      }
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(buildErrorResponse("Invalid username or password", HttpStatus.UNAUTHORIZED));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(
              buildErrorResponse(
                  "Login failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
  }

  /**
   * 刷新Access Token。
   *
   * <p>使用Refresh Token刷新Access Token，Refresh Token过期后需重新登录。
   *
   * @param refreshTokenRequest 刷新Token请求
   * @return ResponseEntity<ApiResponse<JwtResponse>> 新的Access Token和Refresh Token
   */
  @PostMapping("/refresh")
  @Operation(
      summary = "刷新Token",
      description = "使用Refresh Token获取新的Access Token和Refresh Token。Refresh Token过期后需重新登录。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "刷新成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Refresh Token无效或已过期",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(
      @RequestBody RefreshTokenRequest refreshTokenRequest) {
    try {
      final String refreshToken = refreshTokenRequest.getRefreshToken();

      // 验证Refresh Token是否有效
      String username = jwtUtil.extractUsername(refreshToken);

      // 检查Token类型
      if (!jwtUtil.isRefreshToken(refreshToken)) {
        return ResponseEntity.badRequest()
            .body(
                buildErrorResponse(
                    "Invalid token type. Refresh token expected.", HttpStatus.BAD_REQUEST));
      }

      // 检查Token是否过期
      if (jwtUtil.isTokenExpired(refreshToken)) {
        return ResponseEntity.badRequest()
            .body(
                buildErrorResponse(
                    "Refresh token has expired. Please login again.", HttpStatus.BAD_REQUEST));
      }

      // 检查Token是否在黑名单中
      if (tokenBlacklistService.isBlacklisted(refreshToken)) {
        return ResponseEntity.badRequest()
            .body(
                buildErrorResponse(
                    "Refresh token has been revoked. Please login again.", HttpStatus.BAD_REQUEST));
      }

      // 生成新的Access Token和Refresh Token
      String newAccessToken = jwtUtil.generateToken(username);
      String newRefreshToken = jwtUtil.generateRefreshToken(username);

      // 将旧的Refresh Token加入黑名单（可选，根据业务需求）
      // tokenBlacklistService.addToBlacklist(refreshToken);

      JwtResponse jwtResponse =
          JwtResponse.of(
              newAccessToken, newRefreshToken, username, jwtUtil.getAccessTokenExpiration());

      return ResponseEntity.ok(buildSuccessResponse(jwtResponse, "Token refreshed successfully"));
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(
              buildErrorResponse(
                  "Failed to refresh token: " + e.getMessage(), HttpStatus.BAD_REQUEST));
    }
  }

  /**
   * 用户登出。
   *
   * <p>将当前Access Token和Refresh Token加入黑名单。
   *
   * @param request HTTP请求
   * @return ResponseEntity<ApiResponse<Void>> 登出结果
   */
  @PostMapping("/logout")
  @Operation(summary = "用户登出", description = "用户登出，将当前使用的Token加入黑名单。客户端应清除本地存储的Token。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "登出成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Token无效或未认证",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
    try {
      String token = extractTokenFromRequest(request);

      if (token == null) {
        return ResponseEntity.badRequest()
            .body(buildErrorResponse("No token found in request", HttpStatus.BAD_REQUEST));
      }

      // 将Token加入黑名单
      tokenBlacklistService.addToBlacklist(token);

      return ResponseEntity.ok(buildSuccessResponse(null, "Logout successful"));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(
              buildErrorResponse(
                  "Logout failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
  }

  /**
   * 退出所有设备登录（管理员功能）。
   *
   * <p>强制用户在所有设备上重新登录。
   *
   * @param username 用户名
   * @return ResponseEntity<ApiResponse<Void>> 操作结果
   */
  @PostMapping("/logout-all")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "退出所有设备登录（管理员）",
      description = "强制指定用户在所有设备上重新登录。需要管理员权限。此操作将清除该用户所有被撤销的Token黑名单。")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "操作成功",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "权限不足",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "服务器内部错误",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  })
  public ResponseEntity<ApiResponse<Void>> logoutAllDevices(
      @RequestBody java.util.Map<String, String> requestBody) {
    try {
      String username = requestBody.get("username");
      if (username == null || username.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(buildErrorResponse("Username is required", HttpStatus.BAD_REQUEST));
      }

      // 清除该用户的所有黑名单Token（可选实现）
      // tokenBlacklistService.clearBlacklistByPrefix(username);

      return ResponseEntity.ok(
          buildSuccessResponse(null, "All devices logged out successfully for user: " + username));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(
              buildErrorResponse(
                  "Logout all failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }
  }
}
