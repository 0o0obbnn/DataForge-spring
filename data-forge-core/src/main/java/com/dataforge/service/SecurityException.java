package com.dataforge.service;

/**
 * 安全异常类。
 *
 * <p>当检测到安全威胁、恶意输入或违反安全策略时抛出此异常。 属于严重错误类型，需要立即关注和处理。
 *
 * <p><strong>触发场景包括：</strong>
 *
 * <ul>
 *   <li>路径遍历攻击尝试
 *   <li>输入数据超出安全限制
 *   <li>恶意配置文件
 *   <li>资源耗尽攻击
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class SecurityException extends DataForgeException {

  /** 序列化版本号。 */
  private static final long serialVersionUID = 1L;

  /** 安全威胁类型。 */
  public enum ThreatType {
    /** 路径遍历攻击 */
    PATH_TRAVERSAL,
    /** 输入数据过大 */
    INPUT_SIZE_LIMIT,
    /** 资源耗尽攻击 */
    RESOURCE_EXHAUSTION,
    /** 恶意配置 */
    MALICIOUS_CONFIG,
    /** 访问控制违规 */
    ACCESS_VIOLATION,
    /** 其他安全威胁 */
    OTHER
  }

  /** 威胁类型。 */
  private final ThreatType threatType;

  /**
   * 构造函数。
   *
   * @param message 异常消息
   */
  public SecurityException(String message) {
    super("SECURITY_ERROR", message, ErrorLevel.CRITICAL_ERROR);
    this.threatType = ThreatType.OTHER;
  }

  /**
   * 构造函数。
   *
   * @param message 异常消息
   * @param threatType 威胁类型
   */
  public SecurityException(String message, ThreatType threatType) {
    super("SECURITY_" + threatType.name(), message, ErrorLevel.CRITICAL_ERROR);
    this.threatType = threatType;
  }

  /**
   * 构造函数。
   *
   * @param message 异常消息
   * @param threatType 威胁类型
   * @param cause 原因异常
   */
  public SecurityException(String message, ThreatType threatType, Throwable cause) {
    super("SECURITY_" + threatType.name(), message, ErrorLevel.CRITICAL_ERROR, cause);
    this.threatType = threatType;
  }

  /**
   * 获取威胁类型。
   *
   * @return 威胁类型
   */
  public ThreatType getThreatType() {
    return threatType;
  }

  /**
   * 创建路径遍历攻击异常。
   *
   * @param suspiciousPath 可疑路径
   * @return 安全异常实例
   */
  public static SecurityException pathTraversal(String suspiciousPath) {
    return new SecurityException(
            "Detected path traversal attempt: " + suspiciousPath, ThreatType.PATH_TRAVERSAL)
        .withContext("suspiciousPath", suspiciousPath);
  }

  /**
   * 创建输入大小限制异常。
   *
   * @param actualSize 实际大小
   * @param maxSize 最大允许大小
   * @return 安全异常实例
   */
  public static SecurityException inputSizeLimit(long actualSize, long maxSize) {
    return new SecurityException(
            String.format("Input size %d exceeds maximum allowed size %d", actualSize, maxSize),
            ThreatType.INPUT_SIZE_LIMIT)
        .withContext("actualSize", actualSize)
        .withContext("maxSize", maxSize);
  }

  /**
   * 创建资源耗尽攻击异常。
   *
   * @param resourceType 资源类型
   * @param requestedAmount 请求的资源量
   * @param maxAllowed 最大允许量
   * @return 安全异常实例
   */
  public static SecurityException resourceExhaustion(
      String resourceType, long requestedAmount, long maxAllowed) {
    return new SecurityException(
            String.format(
                "Resource exhaustion detected for %s: requested %d, max allowed %d",
                resourceType, requestedAmount, maxAllowed),
            ThreatType.RESOURCE_EXHAUSTION)
        .withContext("resourceType", resourceType)
        .withContext("requestedAmount", requestedAmount)
        .withContext("maxAllowed", maxAllowed);
  }
}
