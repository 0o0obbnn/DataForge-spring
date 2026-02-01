package com.dataforge.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Token黑名单服务。
 *
 * <p>用于管理已撤销的JWT Token，防止被盗用的Token继续使用。
 *
 * <p><strong>工作原理：</strong>
 *
 * <ul>
 *   <li>当用户登出或Token被撤销时，将Token的JTI（JWT ID）存入Redis黑名单
 *   <li>在解析Token时检查JTI是否在黑名单中，如果是则拒绝访问
 *   <li>黑名单记录的TTL设置为Token的剩余有效期
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Service
public class TokenBlacklistService {

  private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
  private static final String BLACKLIST_PREFIX = "token:blacklist:";

  private final RedisTemplate<String, String> redisTemplate;

  public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 将Token添加到黑名单。
   *
   * @param token 要撤销的Token
   */
  public void addToBlacklist(String token) {
    try {
      String jti = extractJti(token);
      Long ttl = calculateRemainingTtl(token);

      if (ttl != null && ttl > 0) {
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.MILLISECONDS);
        logger.info("Token added to blacklist: jti={}, ttl={}ms", jti, ttl);
      } else {
        logger.warn("Token already expired or invalid, not adding to blacklist");
      }
    } catch (Exception e) {
      logger.error("Failed to add token to blacklist", e);
      throw new SecurityException("Failed to invalidate token", e);
    }
  }

  /**
   * 检查Token是否在黑名单中。
   *
   * @param token 要检查的Token
   * @return true如果在黑名单中，false否则
   */
  public boolean isBlacklisted(String token) {
    try {
      String jti = extractJti(token);
      String key = BLACKLIST_PREFIX + jti;
      Boolean exists = redisTemplate.hasKey(key);
      return Boolean.TRUE.equals(exists);
    } catch (Exception e) {
      logger.error("Failed to check token blacklist status", e);
      // 出现异常时保守处理，拒绝访问
      return true;
    }
  }

  /**
   * 从Token中提取JTI（JWT ID）。
   *
   * @param token JWT Token
   * @return JTI字符串
   * @throws Exception 如果Token解析失败
   */
  private String extractJti(String token) throws Exception {
    Claims claims = Jwts.parser().build().parseSignedClaims(token).getPayload();

    // 如果Token没有JTI，使用主题+过期时间作为唯一标识
    String jti = claims.getId();
    if (jti == null || jti.isEmpty()) {
      String subject = claims.getSubject();
      Date expiration = claims.getExpiration();
      jti = subject + ":" + expiration.getTime();
    }

    return jti;
  }

  /**
   * 计算Token的剩余有效期。
   *
   * @param token JWT Token
   * @return 剩余有效期（毫秒），如果Token已过期返回null
   * @throws Exception 如果Token解析失败
   */
  private Long calculateRemainingTtl(String token) throws Exception {
    Claims claims = Jwts.parser().build().parseSignedClaims(token).getPayload();
    Date expiration = claims.getExpiration();
    Date now = new Date();

    if (expiration.before(now)) {
      return null;
    }

    return expiration.getTime() - now.getTime();
  }

  /**
   * 清除所有用户的Token（管理员功能）。
   *
   * <p><strong>警告：此操作会影响所有已登录用户，请谨慎使用。</strong>
   *
   * @return 清除的Token数量
   */
  public long clearAllBlacklistedTokens() {
    try {
      return redisTemplate.delete(redisTemplate.keys(BLACKLIST_PREFIX + "*"));
    } catch (Exception e) {
      logger.error("Failed to clear blacklisted tokens", e);
      throw new SecurityException("Failed to clear token blacklist", e);
    }
  }

  /**
   * 清除特定前缀的黑名单（如某用户的所有Token）。
   *
   * @param prefix Token前缀
   * @return 清除的Token数量
   */
  public long clearBlacklistByPrefix(String prefix) {
    try {
      return redisTemplate.delete(redisTemplate.keys(BLACKLIST_PREFIX + prefix + "*"));
    } catch (Exception e) {
      logger.error("Failed to clear token blacklist by prefix: {}", prefix, e);
      throw new SecurityException("Failed to clear token blacklist", e);
    }
  }
}
