package com.dataforge.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据遮蔽引擎
 *
 * <p>根据DataForge详细开发规范4.1.1节：数据遮蔽引擎 (DataMaskingEngine) 实现敏感数据的自动识别和遮蔽，确保生成的测试数据不泄露真实信息。
 *
 * <p>核心功能： - 身份证号遮蔽：保留前6位和后4位，中间8位用*遮蔽 - 手机号遮蔽：保留前3位和后4位，中间4位用*遮蔽 - 邮箱遮蔽：用户名部分保留前2位和后1位，中间用*遮蔽 -
 * 银行卡号遮蔽：保留前4位和后4位，中间用*遮蔽 - 姓名遮蔽：姓保留，名用*遮蔽 - 自定义遮蔽规则：支持正则表达式配置
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class DataMaskingEngine {

  private static final Logger logger = LoggerFactory.getLogger(DataMaskingEngine.class);

  // 遮蔽规则映射
  private final Map<String, MaskingRule> maskingRules;

  // 敏感数据检测模式
  private static final Map<String, Pattern> SENSITIVE_PATTERNS = new ConcurrentHashMap<>();

  static {
    // 初始化敏感数据检测模式
    SENSITIVE_PATTERNS.put("ID_CARD", Pattern.compile("\\d{17}[\\dXx]"));
    SENSITIVE_PATTERNS.put("PHONE", Pattern.compile("1[3-9]\\d{9}"));
    SENSITIVE_PATTERNS.put("EMAIL", Pattern.compile("[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}"));
    SENSITIVE_PATTERNS.put("BANK_CARD", Pattern.compile("\\d{13,19}"));
    SENSITIVE_PATTERNS.put("CHINESE_NAME", Pattern.compile("[\\u4e00-\\u9fa5]{2,4}"));
  }

  public DataMaskingEngine() {
    this.maskingRules = new ConcurrentHashMap<>();
    initializeDefaultRules();
  }

  /** 初始化默认遮蔽规则 */
  private void initializeDefaultRules() {
    // 身份证号遮蔽规则
    maskingRules.put(
        "ID_CARD",
        new MaskingRule(
            "身份证号",
            value -> {
              if (value == null || value.length() != 18) {
                return value;
              }
              return value.substring(0, 6) + "********" + value.substring(14);
            },
            SENSITIVE_PATTERNS.get("ID_CARD")));

    // 手机号遮蔽规则
    maskingRules.put(
        "PHONE",
        new MaskingRule(
            "手机号",
            value -> {
              if (value == null || value.length() != 11) {
                return value;
              }
              return value.substring(0, 3) + "****" + value.substring(7);
            },
            SENSITIVE_PATTERNS.get("PHONE")));

    // 邮箱遮蔽规则
    maskingRules.put(
        "EMAIL",
        new MaskingRule(
            "邮箱",
            value -> {
              if (value == null || !value.contains("@")) {
                return value;
              }
              String[] parts = value.split("@");
              String username = parts[0];
              String domain = parts[1];

              if (username.length() <= 2) {
                // For very short usernames like "ab", show "a*b"
                return username.charAt(0) + "*" + username.substring(1) + "@" + domain;
              } else if (username.length() == 3) {
                // For 3-char usernames, show first char, *, last char
                return username.charAt(0) + "*" + username.substring(2) + "@" + domain;
              } else {
                // For longer usernames, show first 2 chars, asterisks for middle, last char
                // Use username.length() - 2 to ensure more aggressive masking
                return username.substring(0, 2)
                    + "*".repeat(Math.max(1, username.length() - 2))
                    + username.substring(username.length() - 1)
                    + "@"
                    + domain;
              }
            },
            SENSITIVE_PATTERNS.get("EMAIL")));

    // 银行卡号遮蔽规则
    maskingRules.put(
        "BANK_CARD",
        new MaskingRule(
            "银行卡号",
            value -> {
              if (value == null || value.length() < 8) {
                return value;
              }
              return value.substring(0, 4)
                  + "*".repeat(value.length() - 8)
                  + value.substring(value.length() - 4);
            },
            SENSITIVE_PATTERNS.get("BANK_CARD")));

    // 姓名遮蔽规则
    maskingRules.put(
        "CHINESE_NAME",
        new MaskingRule(
            "中文姓名",
            value -> {
              if (value == null || value.length() < 2) {
                return value;
              }
              return value.charAt(0) + "*".repeat(value.length() - 1);
            },
            SENSITIVE_PATTERNS.get("CHINESE_NAME")));

    logger.info("Initialized {} default masking rules", maskingRules.size());
  }

  /** 遮蔽单个值 */
  public String maskValue(String value, String dataType) {
    if (value == null || value.trim().isEmpty()) {
      return value;
    }

    MaskingRule rule = maskingRules.get(dataType);
    if (rule != null) {
      try {
        String maskedValue = rule.getMaskingFunction().apply(value);
        logger.debug("Masked {} value: {} -> {}", dataType, value, maskedValue);
        return maskedValue;
      } catch (Exception e) {
        logger.error("Failed to mask {} value: {}", dataType, value, e);
        return value; // 出错时返回原值
      }
    }

    return value;
  }

  /** 自动检测并遮蔽敏感数据 */
  public String autoMaskValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      return value;
    }

    // 遍历所有检测模式，找到匹配的进行遮蔽
    for (Map.Entry<String, MaskingRule> entry : maskingRules.entrySet()) {
      String dataType = entry.getKey();
      MaskingRule rule = entry.getValue();

      if (rule.getDetectionPattern().matcher(value).matches()) {
        return maskValue(value, dataType);
      }
    }

    return value; // 没有匹配的敏感数据模式，返回原值
  }

  /** 遮蔽数据记录 */
  public Map<String, Object> maskRecord(
      Map<String, Object> record, Map<String, String> fieldTypeMapping) {
    Map<String, Object> maskedRecord = new ConcurrentHashMap<>(record);

    for (Map.Entry<String, Object> entry : record.entrySet()) {
      String fieldName = entry.getKey();
      Object fieldValue = entry.getValue();

      if (fieldValue instanceof String) {
        String stringValue = (String) fieldValue;

        // 优先使用字段类型映射
        String dataType = fieldTypeMapping.get(fieldName);
        if (dataType != null) {
          maskedRecord.put(fieldName, maskValue(stringValue, dataType));
        } else {
          // 自动检测敏感数据
          maskedRecord.put(fieldName, autoMaskValue(stringValue));
        }
      }
    }

    return maskedRecord;
  }

  /** 添加自定义遮蔽规则 */
  public void addCustomRule(
      String dataType,
      String description,
      Function<String, String> maskingFunction,
      Pattern detectionPattern) {
    MaskingRule rule = new MaskingRule(description, maskingFunction, detectionPattern);
    maskingRules.put(dataType, rule);
    logger.info("Added custom masking rule for data type: {}", dataType);
  }

  /** 移除遮蔽规则 */
  public void removeRule(String dataType) {
    MaskingRule removed = maskingRules.remove(dataType);
    if (removed != null) {
      logger.info("Removed masking rule for data type: {}", dataType);
    }
  }

  /** 获取所有遮蔽规则 */
  public Map<String, String> getAllRules() {
    Map<String, String> result = new ConcurrentHashMap<>();
    for (Map.Entry<String, MaskingRule> entry : maskingRules.entrySet()) {
      result.put(entry.getKey(), entry.getValue().getDescription());
    }
    return result;
  }

  /** 统计遮蔽信息 */
  public MaskingStats getMaskingStats() {
    return new MaskingStats(maskingRules.size(), SENSITIVE_PATTERNS.size());
  }

  /** 遮蔽规则类 */
  public static class MaskingRule {
    private final String description;
    private final Function<String, String> maskingFunction;
    private final Pattern detectionPattern;

    public MaskingRule(
        String description, Function<String, String> maskingFunction, Pattern detectionPattern) {
      this.description = description;
      this.maskingFunction = maskingFunction;
      this.detectionPattern = detectionPattern;
    }

    public String getDescription() {
      return description;
    }

    public Function<String, String> getMaskingFunction() {
      return maskingFunction;
    }

    public Pattern getDetectionPattern() {
      return detectionPattern;
    }
  }

  /** 遮蔽统计信息类 */
  public static class MaskingStats {
    private final int ruleCount;
    private final int patternCount;

    public MaskingStats(int ruleCount, int patternCount) {
      this.ruleCount = ruleCount;
      this.patternCount = patternCount;
    }

    public int getRuleCount() {
      return ruleCount;
    }

    public int getPatternCount() {
      return patternCount;
    }

    @Override
    public String toString() {
      return String.format("MaskingStats{rules=%d, patterns=%d}", ruleCount, patternCount);
    }
  }
}
