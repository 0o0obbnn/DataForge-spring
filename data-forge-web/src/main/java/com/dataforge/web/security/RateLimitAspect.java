package com.dataforge.web.security;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RateLimitAspect {

  @Autowired private RedisTemplate<String, String> redisTemplate;

  // 定义切点，匹配带有@RateLimit注解的方法
  @Pointcut("@annotation(com.dataforge.web.security.RateLimit)")
  public void rateLimitPointcut() {}

  // 环绕通知，实现限流逻辑
  @Around("rateLimitPointcut()")
  public Object rateLimitAround(ProceedingJoinPoint joinPoint) throws Throwable {
    // 获取方法签名
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    // 获取@RateLimit注解
    RateLimit rateLimit = method.getAnnotation(RateLimit.class);
    if (rateLimit == null) {
      return joinPoint.proceed();
    }

    // 从HttpServletRequest中获取真实IP
    String ip = getClientIpAddress();

    // 构建限流键
    String key =
        rateLimit.prefix()
            + ":"
            + ip
            + ":"
            + method.getDeclaringClass().getName()
            + ":"
            + method.getName();

    // 获取当前请求数
    Long count = redisTemplate.opsForValue().increment(key);

    // 处理count为null的情况（虽然increment通常不会返回null，但为了类型安全）
    if (count == null) {
      count = 1L;
    }

    // 如果是第一次请求，设置过期时间
    if (count == 1) {
      redisTemplate.expire(key, rateLimit.seconds(), TimeUnit.SECONDS);
    }

    // 检查是否超过限流阈值
    if (count > rateLimit.value()) {
      // 超过限流阈值，返回429 Too Many Requests
      throw new RateLimitException("Too many requests, please try again later");
    }

    // 未超过限流阈值，继续执行方法
    return joinPoint.proceed();
  }

  /**
   * 获取客户端真实IP地址。
   *
   * <p>考虑代理和负载均衡的情况，按优先级检查以下请求头： 1. X-Forwarded-For 2. X-Real-IP 3. RemoteAddr
   *
   * @return 客户端IP地址
   */
  private String getClientIpAddress() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return "unknown";
    }

    HttpServletRequest request = attributes.getRequest();

    // 检查 X-Forwarded-For 头（可能包含多个IP，取第一个）
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null
        && !xForwardedFor.isEmpty()
        && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      // X-Forwarded-For 可能包含多个IP，用逗号分隔，取第一个
      String[] ips = xForwardedFor.split(",");
      if (ips.length > 0) {
        return ips[0].trim();
      }
    }

    // 检查 X-Real-IP 头
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
      return xRealIp.trim();
    }

    // 使用 RemoteAddr 作为最后备选
    String remoteAddr = request.getRemoteAddr();
    return remoteAddr != null ? remoteAddr : "unknown";
  }

  /** 限流异常类。 */
  public static class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
      super(message);
    }
  }
}
