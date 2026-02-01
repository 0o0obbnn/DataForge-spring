package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 模板数据生成器。
 *
 * <p>根据模板生成内容，支持占位符替换。可以用于生成各种格式的文本内容，如HTML、XML、自定义文本等。 支持从上下文中获取数据来替换模板中的占位符，也可以生成随机数据填充模板。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class TemplateGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(TemplateGenerator.class);
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

  @Override
  public String getType() {
    return "template";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数配置
      String template = getStringParam(config, "template", "Hello, {{name}}!");
      String mode = getStringParam(config, "mode", "context"); // context or random

      if ("random".equalsIgnoreCase(mode)) {
        return generateWithRandomData(template);
      } else {
        return generateWithContextData(template, context);
      }

    } catch (Exception e) {
      logger.error("Failed to generate template content", e);
      return ""; // 返回空字符串作为fallback
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  /** 使用上下文数据生成模板内容 */
  private String generateWithContextData(String template, DataForgeContext context) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String placeholder = matcher.group(1);
      String replacement = "";

      // 从上下文中获取数据
      var contextValue = context.get(placeholder);
      if (contextValue.isPresent() && contextValue.get() != null) {
        replacement = contextValue.get().toString();
      } else {
        // 如果上下文中没有或者值为null，使用默认值或占位符名称
        replacement = getDefaultValue(placeholder);
      }

      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /** 使用随机数据生成模板内容 */
  private String generateWithRandomData(String template) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String placeholder = matcher.group(1);
      String replacement = generateRandomValue(placeholder);
      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /** 根据占位符名称获取默认值 */
  private String getDefaultValue(String placeholder) {
    return switch (placeholder.toLowerCase()) {
      case "name" -> "John Doe";
      case "email" -> "john.doe@example.com";
      case "id" -> "12345";
      case "date" -> "2025-01-01";
      default -> placeholder; // 直接返回占位符名称
    };
  }

  /** 根据占位符名称生成随机值 */
  private String generateRandomValue(String placeholder) {
    return switch (placeholder.toLowerCase()) {
      case "name" -> "User" + (int) (Math.random() * 10000);
      case "email" -> "user" + (int) (Math.random() * 10000) + "@example.com";
      case "id" -> String.valueOf((int) (Math.random() * 100000));
      case "date" -> "2025-"
          + String.format("%02d", (int) (Math.random() * 12) + 1)
          + "-"
          + String.format("%02d", (int) (Math.random() * 28) + 1);
      default -> placeholder + "_" + (int) (Math.random() * 1000);
    };
  }
}
