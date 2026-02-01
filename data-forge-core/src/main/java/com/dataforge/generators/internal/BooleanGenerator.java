package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 布尔值生成器
 *
 * <p>支持生成各种格式的布尔值，用于开关状态、标志位、条件判断等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>format: 输出格式 (BOOLEAN|BINARY|YN|YES_NO|ON_OFF|ENABLED_DISABLED) 默认: BOOLEAN
 *   <li>true_ratio: true值的概率 (0.0-1.0) 默认: 0.5
 *   <li>case_style: 大小写风格 (LOWER|UPPER|TITLE) 默认: LOWER
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class BooleanGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(BooleanGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 输出格式枚举
  public enum BooleanFormat {
    BOOLEAN("true/false"),
    BINARY("1/0"),
    YN("Y/N"),
    YES_NO("Yes/No"),
    ON_OFF("On/Off"),
    ENABLED_DISABLED("Enabled/Disabled");

    private final String description;

    BooleanFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 大小写风格枚举
  public enum CaseStyle {
    LOWER("小写"),
    UPPER("大写"),
    TITLE("首字母大写");

    private final String description;

    CaseStyle(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @Override
  public String getType() {
    return "boolean";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取true的概率
      double trueRatio = getDoubleParam(config, "true_ratio", 0.5);
      boolean value = random.nextDouble() < trueRatio;

      // 获取输出格式
      String formatStr = getStringParam(config, "format", "BOOLEAN");
      BooleanFormat format = parseBooleanFormat(formatStr);

      // 获取大小写风格
      String caseStyleStr = getStringParam(config, "case_style", "LOWER");
      CaseStyle caseStyle = parseCaseStyle(caseStyleStr);

      // 生成布尔值字符串
      String result = formatBoolean(value, format);

      // 应用大小写风格
      return applyCaseStyle(result, caseStyle);

    } catch (Exception e) {
      logger.error("Failed to generate boolean value", e);
      return "true"; // 默认返回true
    }
  }

  /** 解析布尔格式 */
  private BooleanFormat parseBooleanFormat(String formatStr) {
    try {
      return BooleanFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid boolean format: {}, using BOOLEAN as default", formatStr);
      return BooleanFormat.BOOLEAN;
    }
  }

  /** 解析大小写风格 */
  private CaseStyle parseCaseStyle(String caseStyleStr) {
    try {
      return CaseStyle.valueOf(caseStyleStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid case style: {}, using LOWER as default", caseStyleStr);
      return CaseStyle.LOWER;
    }
  }

  /** 格式化布尔值 */
  private String formatBoolean(boolean value, BooleanFormat format) {
    switch (format) {
      case BOOLEAN:
        return value ? "true" : "false";
      case BINARY:
        return value ? "1" : "0";
      case YN:
        return value ? "Y" : "N";
      case YES_NO:
        return value ? "Yes" : "No";
      case ON_OFF:
        return value ? "On" : "Off";
      case ENABLED_DISABLED:
        return value ? "Enabled" : "Disabled";
      default:
        return value ? "true" : "false";
    }
  }

  /** 应用大小写风格 */
  private String applyCaseStyle(String text, CaseStyle caseStyle) {
    switch (caseStyle) {
      case LOWER:
        return text.toLowerCase();
      case UPPER:
        return text.toUpperCase();
      case TITLE:
        if (text.length() == 0) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
      default:
        return text.toLowerCase();
    }
  }
}
