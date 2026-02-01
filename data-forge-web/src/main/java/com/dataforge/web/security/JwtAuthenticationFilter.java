package com.dataforge.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT认证过滤器 - 增强版。
 *
 * <p>支持Token黑名单检查，防止被盗用或已撤销的Token继续使用。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  @Autowired private JwtUtil jwtUtil;

  @Autowired private UserDetailsService userDetailsService;

  @Autowired private TokenBlacklistService tokenBlacklistService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    // 从请求头中获取Authorization头
    final String authorizationHeader = request.getHeader("Authorization");

    String username = null;
    String jwt = null;

    // 检查Authorization头格式是否正确（Bearer token）
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      jwt = authorizationHeader.substring(7);
      try {
        username = jwtUtil.extractUsername(jwt);
      } catch (Exception e) {
        logger.warn("Failed to extract username from token: {}", e.getMessage());
      }
    }

    // 如果获取到了用户名，并且当前没有认证上下文
    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        // 检查Token是否在黑名单中
        if (tokenBlacklistService.isBlacklisted(jwt)) {
          logger.warn("Token is blacklisted for user: {}", username);
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
          return;
        }

        // 检查Token是否过期
        if (jwtUtil.isTokenExpired(jwt)) {
          logger.debug("Token expired for user: {}", username);
          response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
          return;
        }

        // 加载用户详情
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        // 验证令牌
        if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
          // 创建认证令牌
          UsernamePasswordAuthenticationToken authenticationToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());

          // 设置认证详情
          authenticationToken.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request));

          // 设置认证上下文
          SecurityContextHolder.getContext().setAuthentication(authenticationToken);

          logger.debug("Successfully authenticated user: {}", username);
        } else {
          logger.warn("Token validation failed for user: {}", username);
        }
      } catch (Exception e) {
        logger.error("Authentication failed for user: {}", username, e);
      }
    }

    // 继续执行过滤器链
    filterChain.doFilter(request, response);
  }
}
