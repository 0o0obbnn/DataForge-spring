package com.dataforge.web.security;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RateLimitAspect {

  private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

  /** IPv4/IPv6 基本格式校验，防止注入畸形值作为 Redis Key。 允许：点分十进制、冒号分隔十六进制、及 "unknown"。 */
  private static final Pattern VALID_IP_PATTERN = Pattern.compile("^([0-9a-fA-F:.]+|unknown)$");

  /**
   * 可信反向代理 IP 集合（从环境变量 TRUSTED_PROXIES 读取，逗号分隔）。
   *
   * <p>只有请求实际来自这些 IP 时，才信任其携带的 X-Forwarded-For / X-Real-IP 头。 生产环境请将 Nginx/LB 的出口 IP 配置到此处，例如：
   * {@code TRUSTED_PROXIES=10.0.0.1,10.0.0.2}
   */
  private static final Set<String> TRUSTED_PROXIES = buildTrustedProxies();

  private static Set<String> buildTrustedProxies() {
    String envValue = System.getenv("TRUSTED_PROXIES");
    if (envValue == null || envValue.isBlank()) {
      // 默认信任本机回环地址（单机部署场景）
      return Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");
    }
    return Set.of(envValue.split(","));
  }

  private final RedisTemplate<String, String> redisTemplate;

  public RateLimitAspect(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

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
   * 获取客户端真实 IP 地址（可信代理模式）。
   *
   * <p>安全策略：只有请求确实来自 {@link #TRUSTED_PROXIES} 中的 IP 时， 才信任其携带的 {@code X-Forwarded-For} / {@code
   * X-Real-IP} 请求头。 否则直接使用 TCP 连接的 {@code RemoteAddr}，防止客户端伪造请求头绕过限流。
   *
   * <p>可信代理通过环境变量 {@code TRUSTED_PROXIES}（逗号分隔 IP 列表）配置。
   *
   * @return 客户端 IP 地址（已校验格式），无法获取时返回 "unknown"
   */
  private String getClientIpAddress() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return "unknown";
    }

    HttpServletRequest request = attributes.getRequest();
    String remoteAddr = request.getRemoteAddr();

    // 只有请求来自可信代理，才信任其 X-Forwarded-For / X-Real-IP 头
    if (remoteAddr != null && TRUSTED_PROXIES.contains(remoteAddr.trim())) {
      // 优先取 X-Forwarded-For 最左侧的 IP（最接近真实客户端）
      String xForwardedFor = request.getHeader("X-Forwarded-For");
      if (xForwardedFor != null && !xForwardedFor.isBlank()) {
        String candidate = xForwardedFor.split(",")[0].trim();
        if (isValidIp(candidate)) {
          return candidate;
        }
        log.warn("Invalid IP format in X-Forwarded-For header, falling back to RemoteAddr");
      }

      // 次选 X-Real-IP
      String xRealIp = request.getHeader("X-Real-IP");
      if (xRealIp != null && !xRealIp.isBlank() && isValidIp(xRealIp.trim())) {
        return xRealIp.trim();
      }
    }

    // 非可信代理来源，或代理头无效：直接使用 TCP 连接 IP，不可被客户端伪造
    return remoteAddr != null ? remoteAddr : "unknown";
  }

  /**
   * 校验 IP 地址格式，防止畸形值被写入 Redis Key。
   *
   * @param ip 待校验的 IP 字符串
   * @return true 表示格式合法
   */
  private boolean isValidIp(String ip) {
    return ip != null && VALID_IP_PATTERN.matcher(ip).matches();
  }

  /** 限流异常类。 */
  public static class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
      super(message);
    }
  }
}
