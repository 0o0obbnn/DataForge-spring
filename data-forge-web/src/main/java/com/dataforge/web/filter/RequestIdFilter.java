package com.dataforge.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 请求ID追踪过滤器。
 *
 * <p>为每个HTTP请求生成唯一的请求ID，并将其添加到： 1. MDC (Mapped Diagnostic Context) 用于日志追踪 2. HTTP响应头 X-Request-ID
 *
 * <p>请求ID可用于： - 日志关联和追踪 - 问题排查和调试 - 性能分析
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
@Order(1)
public class RequestIdFilter implements Filter {

  private static final String REQUEST_ID_HEADER = "X-Request-ID";
  private static final String MDC_REQUEST_ID_KEY = "requestId";

  @Override
  public void doFilter(
      jakarta.servlet.ServletRequest request,
      jakarta.servlet.ServletResponse response,
      FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // 尝试从请求头获取请求ID，如果没有则生成新的
    String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isEmpty()) {
      requestId = UUID.randomUUID().toString();
    }

    // 将请求ID添加到MDC，用于日志追踪
    MDC.put(MDC_REQUEST_ID_KEY, requestId);

    try {
      // 将请求ID添加到响应头
      httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

      // 继续过滤器链
      chain.doFilter(request, response);
    } finally {
      // 清理MDC，避免内存泄漏
      MDC.remove(MDC_REQUEST_ID_KEY);
    }
  }
}
