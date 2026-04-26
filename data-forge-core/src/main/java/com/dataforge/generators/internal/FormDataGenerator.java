package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 表单数据生成器
 *
 * <p>用于生成各种格式的表单数据，适用于Web表单测试、API测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>field_count: 字段数量 (1-50) 默认: 5
 *   <li>field_types: 字段类型 (TEXT|NUMBER|EMAIL|PASSWORD|CHECKBOX|RADIO|SELECT|TEXTAREA|ALL) 默认: ALL
 *   <li>form_encoding: 表单编码 (URL_ENCODED|MULTIPART|PLAIN) 默认: URL_ENCODED
 *   <li>include_empty: 是否包含空值 默认: false
 *   <li>invalid_data: 是否生成无效数据用于测试 默认: false
 *   <li>nesting_level: 嵌套级别 (0-5) 默认: 0
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class FormDataGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(FormDataGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 字段类型
  public enum FieldType {
    TEXT,
    NUMBER,
    EMAIL,
    PASSWORD,
    CHECKBOX,
    RADIO,
    SELECT,
    TEXTAREA
  }

  // 表单编码类型
  public enum FormEncoding {
    URL_ENCODED,
    MULTIPART,
    PLAIN
  }

  @Override
  public String getType() {
    return "form-data";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      int fieldCount = getIntParam(config, "field_count", 5);
      String fieldTypesParam = getStringParam(config, "field_types", "ALL");
      String formEncodingParam = getStringParam(config, "form_encoding", "URL_ENCODED");
      boolean includeEmpty = getBooleanParam(config, "include_empty", false);
      boolean invalidData = getBooleanParam(config, "invalid_data", false);
      int nestingLevel = getIntParam(config, "nesting_level", 0);

      // 限制参数范围
      fieldCount = Math.max(1, Math.min(50, fieldCount));
      nestingLevel = Math.max(0, Math.min(5, nestingLevel));

      // 解析参数
      Set<FieldType> fieldTypes = parseFieldTypes(fieldTypesParam);
      FormEncoding formEncoding = parseFormEncoding(formEncodingParam);

      Map<String, Object> formData = new LinkedHashMap<>();

      // 生成表单数据
      generateFormData(
          formData, fieldCount, fieldTypes, includeEmpty, invalidData, nestingLevel, 0);

      // 格式化为指定编码
      return formatFormData(formData, formEncoding);
    } catch (Exception e) {
      logger.error("Error generating form data", e);
      return "Error generating form data";
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String getDescription() {
    return "表单数据生成器，用于生成各种格式的Web表单数据";
  }

  /** 解析字段类型参数 */
  private Set<FieldType> parseFieldTypes(String fieldTypesParam) {
    Set<FieldType> fieldTypes = new HashSet<>();

    if ("ALL".equalsIgnoreCase(fieldTypesParam)) {
      fieldTypes.addAll(Arrays.asList(FieldType.values()));
    } else {
      String[] types = fieldTypesParam.split(",");
      for (String type : types) {
        try {
          fieldTypes.add(FieldType.valueOf(type.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
          logger.warn("Invalid field type: {}, skipping", type);
        }
      }
    }

    // 如果没有有效的类型，默认使用所有类型
    if (fieldTypes.isEmpty()) {
      fieldTypes.addAll(Arrays.asList(FieldType.values()));
    }

    return fieldTypes;
  }

  /** 解析表单编码参数 */
  private FormEncoding parseFormEncoding(String formEncodingParam) {
    try {
      return FormEncoding.valueOf(formEncodingParam.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid form encoding: {}, using URL_ENCODED", formEncodingParam);
      return FormEncoding.URL_ENCODED;
    }
  }

  /** 递归生成表单数据 */
  private void generateFormData(
      Map<String, Object> formData,
      int fieldCount,
      Set<FieldType> fieldTypes,
      boolean includeEmpty,
      boolean invalidData,
      int maxNestingLevel,
      int currentLevel) {
    FieldType[] types = fieldTypes.toArray(new FieldType[0]);

    for (int i = 0; i < fieldCount; i++) {
      String fieldName = generateFieldName(i, currentLevel);
      FieldType fieldType = types[random.nextInt(types.length)];

      // 可能生成空值
      if (includeEmpty && random.nextDouble() < 0.1) { // 10%概率为空
        formData.put(fieldName, "");
        continue;
      }

      // 生成字段值
      Object fieldValue = generateFieldValue(fieldType, invalidData);

      // 处理嵌套结构
      if (currentLevel < maxNestingLevel && random.nextDouble() < 0.3) { // 30%概率嵌套
        Map<String, Object> nestedData = new LinkedHashMap<>();
        int nestedFieldCount = 1 + random.nextInt(3); // 1-3个嵌套字段
        generateFormData(
            nestedData,
            nestedFieldCount,
            fieldTypes,
            includeEmpty,
            invalidData,
            maxNestingLevel,
            currentLevel + 1);
        formData.put(fieldName, nestedData);
      } else {
        formData.put(fieldName, fieldValue);
      }
    }
  }

  /** 生成字段名 */
  private String generateFieldName(int index, int level) {
    String[] prefixes = {
      "user", "product", "order", "config", "setting", "data", "info", "profile"
    };
    String[] suffixes = {"name", "id", "type", "status", "value", "count", "date", "email"};

    StringBuilder fieldName = new StringBuilder();
    for (int i = 0; i < level; i++) {
      fieldName.append("nested_");
    }

    if (random.nextBoolean()) {
      fieldName.append(prefixes[random.nextInt(prefixes.length)]).append("_");
    }

    fieldName.append(suffixes[random.nextInt(suffixes.length)]).append("_").append(index);
    return fieldName.toString();
  }

  /** 生成字段值 */
  private Object generateFieldValue(FieldType fieldType, boolean invalid) {
    switch (fieldType) {
      case TEXT:
        return generateRandomText(invalid);
      case NUMBER:
        return invalid ? "not_a_number" : String.valueOf(random.nextInt(10000));
      case EMAIL:
        return invalid ? "invalid_email" : generateRandomEmail();
      case PASSWORD:
        return generateRandomPassword();
      case CHECKBOX:
        return random.nextBoolean();
      case RADIO:
        return "option" + (1 + random.nextInt(5));
      case SELECT:
        return "choice" + (1 + random.nextInt(10));
      case TEXTAREA:
        return generateRandomText(invalid) + "\n" + generateRandomText(invalid);
      default:
        return generateRandomText(invalid);
    }
  }

  /** 生成随机文本 */
  private String generateRandomText(boolean invalid) {
    if (invalid) {
      // 生成包含特殊字符的无效文本
      return "text with <script>alert('xss')</script> and \"quotes\"";
    }

    String[] words = {
      "apple", "banana", "cherry", "date", "elderberry", "fig", "grape", "honeydew"
    };
    int wordCount = 2 + random.nextInt(5); // 2-6个单词
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < wordCount; i++) {
      if (i > 0) text.append(" ");
      text.append(words[random.nextInt(words.length)]);
    }
    return text.toString();
  }

  /** 生成随机邮箱 */
  private String generateRandomEmail() {
    String[] domains = {"example.com", "test.org", "demo.net", "sample.io"};
    String[] names = {"user", "admin", "test", "demo", "sample"};
    return names[random.nextInt(names.length)]
        + random.nextInt(1000)
        + "@"
        + domains[random.nextInt(domains.length)];
  }

  /** 生成随机密码 */
  private String generateRandomPassword() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    StringBuilder password = new StringBuilder();
    int length = 8 + random.nextInt(8); // 8-15个字符
    for (int i = 0; i < length; i++) {
      password.append(chars.charAt(random.nextInt(chars.length())));
    }
    return password.toString();
  }

  /** 格式化表单数据 */
  private String formatFormData(Map<String, Object> formData, FormEncoding formEncoding) {
    switch (formEncoding) {
      case URL_ENCODED:
        return formatAsUrlEncoded(formData);
      case MULTIPART:
        return formatAsMultipart(formData);
      case PLAIN:
        return formatAsPlain(formData);
      default:
        return formatAsUrlEncoded(formData);
    }
  }

  /** 格式化为URL编码 */
  private String formatAsUrlEncoded(Map<String, Object> formData) {
    StringBuilder result = new StringBuilder();
    boolean first = true;

    for (Map.Entry<String, Object> entry : formData.entrySet()) {
      if (!first) {
        result.append("&");
      }
      first = false;

      result.append(urlEncode(entry.getKey())).append("=");
      result.append(urlEncode(entry.getValue().toString()));
    }

    return result.toString();
  }

  /** 格式化为Multipart */
  private String formatAsMultipart(Map<String, Object> formData) {
    StringBuilder result = new StringBuilder();
    String boundary = "----FormDataBoundary" + System.currentTimeMillis();

    result.append("--").append(boundary).append("\n");

    for (Map.Entry<String, Object> entry : formData.entrySet()) {
      result
          .append("Content-Disposition: form-data; name=\"")
          .append(entry.getKey())
          .append("\"\n\n");
      result.append(entry.getValue().toString()).append("\n");
      result.append("--").append(boundary).append("\n");
    }

    return result.toString();
  }

  /** 格式化为纯文本 */
  private String formatAsPlain(Map<String, Object> formData) {
    StringBuilder result = new StringBuilder();

    for (Map.Entry<String, Object> entry : formData.entrySet()) {
      result.append(entry.getKey()).append(": ").append(entry.getValue().toString()).append("\n");
    }

    return result.toString();
  }

  /** URL编码 */
  private String urlEncode(String value) {
    try {
      return java.net.URLEncoder.encode(value, "UTF-8");
    } catch (Exception e) {
      return value;
    }
  }
}
