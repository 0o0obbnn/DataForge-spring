package com.dataforge.validation;

import com.dataforge.config.FieldConfigWrapper;
import com.dataforge.config.ForgeConfig;
import com.dataforge.monitoring.ResourceMonitor;
import com.dataforge.security.SecurityConfiguration;
import com.dataforge.service.SecurityException;
import com.dataforge.service.ValidationException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 安全验证器。
 *
 * <p>负责对用户输入进行安全性验证，防范各种安全威胁包括但不限于：
 *
 * <ul>
 *   <li>路径遍历攻击
 *   <li>资源耗尽攻击
 *   <li>恶意输入注入
 *   <li>配置参数越界
 * </ul>
 *
 * <p><strong>验证原则：</strong>
 *
 * <ul>
 *   <li>默认拒绝：对不符合安全要求的输入一律拒绝
 *   <li>最小权限：只允许必要的操作
 *   <li>深度防御：多层验证确保安全
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class SecurityValidator {

  private static final Logger logger = LoggerFactory.getLogger(SecurityValidator.class);

  private final SecurityConfiguration securityConfig;
  private final ResourceMonitor resourceMonitor;

  // 安全模式
  private static final Pattern SAFE_FIELD_NAME_PATTERN =
      Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,255}$");
  private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9._/\\\\:-]+$");

  public SecurityValidator(SecurityConfiguration securityConfig, ResourceMonitor resourceMonitor) {
    this.securityConfig = securityConfig;
    this.resourceMonitor = resourceMonitor;
    logger.info("SecurityValidator initialized with config: {}", securityConfig);
  }

  /**
   * 验证配置的安全性。
   *
   * @param config 要验证的配置
   * @throws SecurityException 当检测到安全威胁时
   * @throws ValidationException 当验证失败时
   */
  public void validateConfiguration(ForgeConfig config)
      throws SecurityException, ValidationException {
    logger.debug("开始安全验证配置");

    // 0. 检查系统资源状态
    resourceMonitor.checkMemoryUsage();
    resourceMonitor.checkThreadUsage();

    // 1. 验证记录数量限制
    validateRecordCount(config.getCount());

    // 2. 验证线程数量限制
    validateThreadCount(config.getThreads());

    // 3. 验证字段配置
    validateFields(config.getFields());

    // 4. 验证输出配置
    if (config.getOutput() != null) {
      validateOutputConfiguration(config.getOutput());
    }

    // 5. 验证整体配置大小
    validateConfigurationSize(config);

    logger.debug("配置安全验证通过");
  }

  /** 验证记录数量。 */
  private void validateRecordCount(int count) throws SecurityException {
    if (count <= 0) {
      throw new ValidationException("记录数量必须大于0");
    }

    if (count > securityConfig.getMaxRecordCount()) {
      throw SecurityException.resourceExhaustion("记录数量", count, securityConfig.getMaxRecordCount());
    }

    logger.trace("记录数量验证通过: {}", count);
  }

  /** 验证线程数量。 */
  private void validateThreadCount(int threads) throws SecurityException {
    if (threads <= 0) {
      throw new ValidationException("线程数量必须大于0");
    }

    if (threads > securityConfig.getMaxThreadCount()) {
      throw SecurityException.resourceExhaustion(
          "线程数量", threads, securityConfig.getMaxThreadCount());
    }

    int availableProcessors = Runtime.getRuntime().availableProcessors();
    if (threads > availableProcessors * 4) {
      logger.warn("线程数量 {} 超过建议值 {} (4倍CPU核数)", threads, availableProcessors * 4);
    }

    logger.trace("线程数量验证通过: {}", threads);
  }

  /** 验证字段配置列表。 */
  private void validateFields(java.util.List<FieldConfigWrapper> fields)
      throws SecurityException, ValidationException {
    if (fields == null || fields.isEmpty()) {
      throw new ValidationException("字段配置不能为空");
    }

    if (fields.size() > securityConfig.getMaxFieldCount()) {
      throw SecurityException.resourceExhaustion(
          "字段数量", fields.size(), securityConfig.getMaxFieldCount());
    }

    for (FieldConfigWrapper field : fields) {
      validateField(field);
    }

    logger.trace("字段配置验证通过，共 {} 个字段", fields.size());
  }

  /** 验证单个字段配置。 */
  private void validateField(FieldConfigWrapper field)
      throws SecurityException, ValidationException {
    if (field == null) {
      throw new ValidationException("字段配置不能为null");
    }

    // 验证字段名称
    validateFieldName(field.getName());

    // 验证字段类型
    validateFieldType(field.getType());

    logger.trace("字段验证通过: {} ({})", field.getName(), field.getType());
  }

  /** 验证字段名称。 */
  private void validateFieldName(String fieldName) throws SecurityException, ValidationException {
    if (fieldName == null || fieldName.trim().isEmpty()) {
      throw new ValidationException("字段名称不能为空");
    }

    if (fieldName.length() > securityConfig.getMaxFieldNameLength()) {
      throw SecurityException.inputSizeLimit(
          fieldName.length(), securityConfig.getMaxFieldNameLength());
    }

    if (!SAFE_FIELD_NAME_PATTERN.matcher(fieldName).matches()) {
      throw new SecurityException(
              "字段名称包含不安全字符: " + fieldName, SecurityException.ThreatType.MALICIOUS_CONFIG)
          .withContext("fieldName", fieldName);
    }

    // 检查敏感字段名
    String lowerFieldName = fieldName.toLowerCase();
    if (lowerFieldName.contains("password")
        || lowerFieldName.contains("secret")
        || lowerFieldName.contains("token")
        || lowerFieldName.contains("key")) {
      logger.warn("检测到可能的敏感字段名: {}", fieldName);
    }
  }

  /** 验证字段类型。 */
  private void validateFieldType(String fieldType) throws ValidationException {
    if (fieldType == null || fieldType.trim().isEmpty()) {
      throw new ValidationException("字段类型不能为空");
    }

    // 这里可以扩展支持的字段类型白名单验证
    if (fieldType.length() > 50) {
      throw new ValidationException("字段类型名称过长: " + fieldType);
    }
  }

  /** 验证输出配置。 */
  private void validateOutputConfiguration(com.dataforge.config.OutputConfig outputConfig)
      throws SecurityException, ValidationException {

    // 验证输出路径
    if (outputConfig.getPath() != null) {
      validateOutputPath(outputConfig.getPath());
    }

    // 验证输出格式
    if (outputConfig.getFormat() == null) {
      throw new ValidationException("输出格式不能为空");
    }

    logger.trace("输出配置验证通过");
  }

  /** 验证输出路径，防范路径遍历攻击。 */
  public void validateOutputPath(String path) throws SecurityException {
    if (path == null || path.trim().isEmpty()) {
      throw new ValidationException("输出路径不能为空");
    }

    if (path.length() > securityConfig.getMaxPathLength()) {
      throw SecurityException.inputSizeLimit(path.length(), securityConfig.getMaxPathLength());
    }

    // 使用正则表达式验证路径字符的安全性
    if (!SAFE_PATH_PATTERN.matcher(path).matches()) {
      throw new SecurityException(
              "Output path contains unsafe characters: " + path,
              SecurityException.ThreatType.MALICIOUS_CONFIG)
          .withContext("path", path);
    }

    // 检查禁止的路径模式
    for (String forbiddenPattern : securityConfig.getForbiddenPathPatterns()) {
      if (path.matches(forbiddenPattern)) {
        throw SecurityException.pathTraversal(path)
            .withContext("forbiddenPattern", forbiddenPattern);
      }
    }

    // 检查绝对路径（在生产环境中通常不允许，测试环境可配置允许）
    if ((path.startsWith("/") || path.matches("^[A-Za-z]:\\\\.*"))
        && !securityConfig.isAllowAbsolutePaths()) {
      throw new SecurityException("不允许使用绝对路径: " + path, SecurityException.ThreatType.PATH_TRAVERSAL)
          .withContext("path", path);
    }

    // 检查允许的输出目录
    // 如果允许绝对路径，则跳过目录检查
    if (!securityConfig.isAllowAbsolutePaths()) {
      boolean allowedDirectory = false;
      for (String allowedDir : securityConfig.getAllowedOutputDirectories()) {
        if (path.startsWith(allowedDir)) {
          allowedDirectory = true;
          break;
        }
      }

      if (!allowedDirectory) {
        throw new SecurityException(
                "输出路径不在允许的目录中: " + path, SecurityException.ThreatType.ACCESS_VIOLATION)
            .withContext("path", path)
            .withContext("allowedDirectories", securityConfig.getAllowedOutputDirectories());
      }
    }

    // 规范化路径并进行额外检查
    try {
      Path normalizedPath = Paths.get(path).normalize();
      String normalizedStr = normalizedPath.toString();

      // 检查规范化后是否仍然安全
      if (normalizedStr.contains("..")
          || (normalizedStr.startsWith("/") && !securityConfig.isAllowAbsolutePaths())) {
        throw SecurityException.pathTraversal(path);
      }

    } catch (Exception e) {
      throw new SecurityException(
              "路径格式无效: " + path, SecurityException.ThreatType.MALICIOUS_CONFIG, e)
          .withContext("path", path);
    }

    logger.trace("输出路径验证通过: {}", path);
  }

  /** 验证配置整体大小。 */
  private void validateConfigurationSize(ForgeConfig config) throws SecurityException {
    // 这里可以实现配置对象的大小估算
    // 简化实现：基于字段数量和记录数量估算
    long estimatedSize =
        (long) config.getFields().size() * config.getCount() * 100; // 每个字段每条记录估算100字节

    if (estimatedSize > securityConfig.getMaxConfigSizeBytes()) {
      throw SecurityException.inputSizeLimit(estimatedSize, securityConfig.getMaxConfigSizeBytes());
    }

    logger.trace("配置大小验证通过，估算大小: {} 字节", estimatedSize);
  }

  /** 验证字符串是否包含SQL注入风险。 */
  public void validateSqlInjection(String input) throws SecurityException {
    if (input == null) return;

    String lowerInput = input.toLowerCase();
    String[] sqlKeywords = {
      "select", "insert", "update", "delete", "drop", "create", "alter", "union", "script", "exec",
      "execute", "sp_", "xp_"
    };

    for (String keyword : sqlKeywords) {
      if (lowerInput.contains(keyword)) {
        throw new SecurityException("检测到可能的SQL注入尝试", SecurityException.ThreatType.MALICIOUS_CONFIG)
            .withContext("suspiciousInput", input)
            .withContext("detectedKeyword", keyword);
      }
    }
  }

  /** 验证文件名是否安全。 */
  public void validateFileName(String fileName) throws SecurityException {
    if (fileName == null || fileName.trim().isEmpty()) {
      throw new ValidationException("文件名不能为空");
    }

    // 检查文件名长度
    if (fileName.length() > securityConfig.getMaxPathLength()) {
      throw SecurityException.inputSizeLimit(fileName.length(), securityConfig.getMaxPathLength());
    }

    // 检查危险字符
    if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
      throw SecurityException.pathTraversal(fileName);
    }

    // 检查文件扩展名白名单
    boolean validExtension = false;
    for (String ext : securityConfig.getAllowedFileExtensions()) {
      if (fileName.toLowerCase().endsWith(ext)) {
        validExtension = true;
        break;
      }
    }

    if (!validExtension) {
      throw new SecurityException(
              "不支持的文件扩展名: " + fileName, SecurityException.ThreatType.MALICIOUS_CONFIG)
          .withContext("fileName", fileName)
          .withContext("allowedExtensions", securityConfig.getAllowedFileExtensions());
    }

    logger.trace("文件名验证通过: {}", fileName);
  }
}
