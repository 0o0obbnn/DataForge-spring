package com.dataforge.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT工具类 - 增强版。
 *
 * <p>支持Access Token和Refresh Token两种Token类型。
 *
 * <ul>
 *   <li><strong>Access Token</strong>: 用于API认证，有效期短（默认1小时）
 *   <li><strong>Refresh Token</strong>: 用于刷新Access Token，有效期长（默认7天）
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class JwtUtil {

  private final JwtProperties jwtProperties;

  public JwtUtil(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * 生成Access Token。
   *
   * @param username 用户名
   * @return JWT Access Token
   */
  public String generateToken(String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "access");
    return createToken(claims, username, jwtProperties.getExpiration());
  }

  /**
   * 生成Refresh Token。
   *
   * <p>Refresh Token具有更长的有效期，用于刷新Access Token。
   *
   * @param username 用户名
   * @return JWT Refresh Token
   */
  public String generateRefreshToken(String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    claims.put("jti", UUID.randomUUID().toString());
    return createToken(claims, username, jwtProperties.getRefreshExpiration());
  }

  /**
   * 创建JWT Token。
   *
   * @param claims 自定义声明
   * @param subject 主题（用户名）
   * @param expirationTime 过期时间（毫秒）
   * @return JWT Token字符串
   */
  private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expirationTime))
        .signWith(getSignKey())
        .compact();
  }

  /**
   * 获取签名密钥。
   *
   * @return HMAC-SHA密钥
   */
  private SecretKey getSignKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
  }

  /**
   * 从JWT Token中提取用户名。
   *
   * @param token JWT Token
   * @return 用户名
   */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * 从JWT Token中提取过期时间。
   *
   * @param token JWT Token
   * @return 过期时间
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * 从JWT Token中提取JTI（JWT ID）。
   *
   * <p>JTI用于唯一标识Token，可用于实现Token黑名单。
   *
   * @param token JWT Token
   * @return JTI字符串，如果不存在返回null
   */
  public String extractJti(String token) {
    return extractClaim(token, claims -> claims.get("jti", String.class));
  }

  /**
   * 从JWT Token中提取Token类型。
   *
   * @param token JWT Token
   * @return Token类型（access或refresh）
   */
  public String extractTokenType(String token) {
    return extractClaim(token, claims -> claims.get("type", String.class));
  }

  /**
   * 从JWT Token中提取指定声明。
   *
   * @param token JWT Token
   * @param claimsResolver 声明提取函数
   * @param <T> 声明值的类型
   * @return 声明值
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * 从JWT Token中提取所有声明。
   *
   * @param token JWT Token
   * @return Claims对象
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload();
  }

  /**
   * 检查JWT Token是否已过期。
   *
   * @param token JWT Token
   * @return true如果已过期，false否则
   */
  public boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * 检查Token是否为Refresh Token。
   *
   * @param token JWT Token
   * @return true如果是Refresh Token，false否则
   */
  public boolean isRefreshToken(String token) {
    String type = extractTokenType(token);
    return "refresh".equals(type);
  }

  /**
   * 检查Token是否为Access Token。
   *
   * @param token JWT Token
   * @return true如果是Access Token，false否则
   */
  public boolean isAccessToken(String token) {
    String type = extractTokenType(token);
    return "access".equals(type);
  }

  /**
   * 验证JWT Token。
   *
   * @param token JWT Token
   * @param username 期望的用户名
   * @return true如果Token有效，false否则
   */
  public boolean validateToken(String token, String username) {
    final String extractedUsername = extractUsername(token);
    return (extractedUsername.equals(username) && !isTokenExpired(token));
  }

  /**
   * 获取Access Token过期时间（毫秒）。
   *
   * @return 过期时间（毫秒）
   */
  public long getAccessTokenExpiration() {
    return jwtProperties.getExpiration();
  }

  /**
   * 获取Refresh Token过期时间（毫秒）。
   *
   * @return 过期时间（毫秒）
   */
  public long getRefreshTokenExpiration() {
    return jwtProperties.getRefreshExpiration();
  }
}
