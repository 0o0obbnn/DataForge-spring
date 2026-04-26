package com.dataforge.web.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    // 设置响应状态码为401未授权
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    // 设置响应内容类型
    response.setContentType("application/json");
    // 写入错误信息
    response
        .getWriter()
        .write(
            "{\"error\": \"Unauthorized\", \"message\": \"Full authentication is required to access this resource\"}");
  }
}
