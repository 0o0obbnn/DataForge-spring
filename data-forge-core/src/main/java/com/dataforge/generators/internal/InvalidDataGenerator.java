package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 无效数据生成器
 *
 * <p>根据DataForge设计文档要求，生成各种类型的无效数据用于测试系统的输入验证和错误处理能力。 支持格式错误、超出范围、非法字符、类型不匹配等多种无效数据生成。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class InvalidDataGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(InvalidDataGenerator.class);

  // SQL注入测试数据
  private static final List<String> SQL_INJECTION_PAYLOADS =
      Arrays.asList(
          "' OR '1'='1",
          "'; DROP TABLE users; --",
          "' UNION SELECT username, password FROM users; --",
          "1; SELECT * FROM information_schema.tables; --",
          "' OR 1=1--",
          "' OR '1'='1' ({",
          "' OR '1'='1' /*");

  // XSS攻击测试数据
  private static final List<String> XSS_PAYLOADS =
      Arrays.asList(
          "<script>alert('XSS')</script>",
          "<img src=x onerror=alert('XSS')>",
          "javascript:alert('XSS')",
          "<svg/onload=alert('XSS')>",
          "<iframe src=javascript:alert('XSS')>",
          "<body onload=alert('XSS')>",
          "<input onfocus=alert('XSS') autofocus>");

  // 格式错误的邮箱地址
  private static final List<String> INVALID_EMAILS =
      Arrays.asList(
          "invalid.email",
          "@invalid.com",
          "invalid@",
          "invalid@.com",
          "invalid@com",
          "invalid..email@example.com",
          "invalid@email..com");

  // 格式错误的手机号
  private static final List<String> INVALID_PHONES =
      Arrays.asList("123", "123456789012345", "abc123def", "+123-45-678-9012", "123 456 78901");

  // 格式错误的URL
  private static final List<String> INVALID_URLS =
      Arrays.asList(
          "http://",
          "https://",
          "://example.com",
          "http:// example.com",
          "http://example.com:999999",
          "http://[::1",
          "http://%x%y%z%");

  // 超长字符串
  private static final List<String> OVERLENGTH_STRINGS =
      Arrays.asList("A".repeat(1000), "B".repeat(2000), "C".repeat(5000), "D".repeat(10000));

  // 特殊字符和控制字符
  private static final List<String> SPECIAL_CHARACTERS =
      Arrays.asList(
          "\u0000", // Null字符
          "\u0001", // SOH字符
          "\u0002", // STX字符
          "\u007F", // DEL字符
          "\u202E", // RTL覆盖字符
          "\u200B", // 零宽空格
          "\u202A", // LRE字符
          "\u202B", // RLE字符
          "\u202C", // PDF字符
          "\u2066", // LRI字符
          "\u2067" // RLI字符
          );

  @Override
  public String getType() {
    return "INVALID_DATA";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String category = config.getParam("category", String.class, "ALL");
      boolean includeNullBytes =
          Boolean.parseBoolean(config.getParam("includeNullBytes", String.class, "false"));

      return generateInvalidData(category, includeNullBytes);

    } catch (Exception e) {
      logger.warn("Error generating invalid data: {}", e.getMessage());
      // 生成默认无效数据
      return generateDefaultInvalidData();
    }
  }

  /** 根据类别生成无效数据 */
  private String generateInvalidData(String category, boolean includeNullBytes) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    // 如果包含null字节，有一定概率返回null字节
    if (includeNullBytes && random.nextDouble() < 0.2) {
      return SPECIAL_CHARACTERS.get(0); // Null字符
    }

    switch (category.toUpperCase()) {
      case "SQL_INJECTION":
        return SQL_INJECTION_PAYLOADS.get(random.nextInt(SQL_INJECTION_PAYLOADS.size()));

      case "XSS":
        return XSS_PAYLOADS.get(random.nextInt(XSS_PAYLOADS.size()));

      case "INVALID_EMAIL":
        return INVALID_EMAILS.get(random.nextInt(INVALID_EMAILS.size()));

      case "INVALID_PHONE":
        return INVALID_PHONES.get(random.nextInt(INVALID_PHONES.size()));

      case "INVALID_URL":
        return INVALID_URLS.get(random.nextInt(INVALID_URLS.size()));

      case "OVERLENGTH":
        return OVERLENGTH_STRINGS.get(random.nextInt(OVERLENGTH_STRINGS.size()));

      case "SPECIAL_CHARS":
        return SPECIAL_CHARACTERS.get(random.nextInt(SPECIAL_CHARACTERS.size()));

      case "ALL":
      default:
        // 随机选择一种类型的无效数据
        int type = random.nextInt(7);
        switch (type) {
          case 0:
            return SQL_INJECTION_PAYLOADS.get(random.nextInt(SQL_INJECTION_PAYLOADS.size()));
          case 1:
            return XSS_PAYLOADS.get(random.nextInt(XSS_PAYLOADS.size()));
          case 2:
            return INVALID_EMAILS.get(random.nextInt(INVALID_EMAILS.size()));
          case 3:
            return INVALID_PHONES.get(random.nextInt(INVALID_PHONES.size()));
          case 4:
            return INVALID_URLS.get(random.nextInt(INVALID_URLS.size()));
          case 5:
            return OVERLENGTH_STRINGS.get(random.nextInt(OVERLENGTH_STRINGS.size()));
          case 6:
            return SPECIAL_CHARACTERS.get(random.nextInt(SPECIAL_CHARACTERS.size()));
          default:
            return generateDefaultInvalidData();
        }
    }
  }

  /** 生成默认无效数据 */
  private String generateDefaultInvalidData() {
    return SQL_INJECTION_PAYLOADS.get(
        ThreadLocalRandom.current().nextInt(SQL_INJECTION_PAYLOADS.size()));
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String category = config.getParam("category", String.class, "ALL");

    // 验证类别
    String[] validCategories = {
      "SQL_INJECTION",
      "XSS",
      "INVALID_EMAIL",
      "INVALID_PHONE",
      "INVALID_URL",
      "OVERLENGTH",
      "SPECIAL_CHARS",
      "ALL"
    };
    for (String validCat : validCategories) {
      if (validCat.equalsIgnoreCase(category)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String getDescription() {
    return "生成无效数据，支持SQL注入、XSS攻击、格式错误的邮箱/手机号/URL、超长字符串、特殊字符等多种类型，" + "适用于测试系统的输入验证和安全防护能力";
  }
}
