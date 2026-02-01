package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON Schema数据生成器
 *
 * <p>基于简化的JSON Schema规范生成结构化数据，用于API测试、 数据模拟、接口验证等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>schema_type: Schema类型 (USER|PRODUCT|ORDER|EVENT|CUSTOM) 默认: USER
 *   <li>format: 输出格式 (JSON|COMPACT|PRETTY) 默认: JSON
 *   <li>include_optional: 是否包含可选字段 默认: true
 *   <li>array_size: 数组大小 默认: 3
 *   <li>nested_level: 嵌套层级 默认: 2
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class JsonSchemaGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(JsonSchemaGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 示例数据
  private static final List<String> SAMPLE_NAMES =
      Arrays.asList("张三", "李四", "王五", "赵六", "John", "Jane", "Mike", "Sarah");

  private static final List<String> SAMPLE_PRODUCTS =
      Arrays.asList(
          "iPhone 15", "MacBook Pro", "iPad Air", "AirPods", "Samsung Galaxy", "Dell Laptop");

  private static final List<String> SAMPLE_CATEGORIES =
      Arrays.asList("电子产品", "服装鞋帽", "家居用品", "美妆护肤", "食品饮料", "图书音像");

  private static final List<String> SAMPLE_EVENTS =
      Arrays.asList("user_login", "page_view", "button_click", "form_submit", "purchase", "logout");

  @Override
  public String getType() {
    return "json_schema";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String schemaType = getStringParam(config, "schema_type", "USER").toUpperCase();
      String format = getStringParam(config, "format", "JSON").toUpperCase();
      boolean includeOptional = getBooleanParam(config, "include_optional", true);
      int arraySize = getIntParam(config, "array_size", 3);
      int nestedLevel = getIntParam(config, "nested_level", 2);

      String jsonData =
          generateJsonData(schemaType, includeOptional, arraySize, nestedLevel, context);

      // 存储到上下文
      context.put("json_schema_type", schemaType);
      context.put("json_data", jsonData);

      return formatJson(jsonData, format);

    } catch (Exception e) {
      logger.error("Failed to generate JSON schema data", e);
      return "{\"error\":\"Failed to generate data\"}";
    }
  }

  private String generateJsonData(
      String schemaType,
      boolean includeOptional,
      int arraySize,
      int nestedLevel,
      DataForgeContext context) {
    switch (schemaType) {
      case "USER":
        return generateUserSchema(includeOptional, context);
      case "PRODUCT":
        return generateProductSchema(includeOptional, arraySize);
      case "ORDER":
        return generateOrderSchema(includeOptional, arraySize);
      case "EVENT":
        return generateEventSchema(includeOptional);
      case "CUSTOM":
        return generateCustomSchema(nestedLevel, arraySize);
      default:
        return generateUserSchema(includeOptional, context);
    }
  }

  private String generateUserSchema(boolean includeOptional, DataForgeContext context) {
    StringBuilder json = new StringBuilder("{");

    // 必需字段
    json.append("\"id\":").append(1000 + random.nextInt(9000)).append(",");
    json.append("\"name\":\"")
        .append(SAMPLE_NAMES.get(random.nextInt(SAMPLE_NAMES.size())))
        .append("\",");
    json.append("\"email\":\"").append(generateEmail()).append("\",");
    json.append("\"age\":").append(18 + random.nextInt(50));

    // 可选字段
    if (includeOptional) {
      json.append(",\"phone\":\"").append(generatePhone()).append("\",");
      json.append("\"address\":{");
      json.append("\"street\":\"").append("第" + (1 + random.nextInt(999)) + "号").append("\",");
      json.append("\"city\":\"").append("北京市").append("\",");
      json.append("\"zipcode\":\"")
          .append(String.format("%06d", random.nextInt(1000000)))
          .append("\"");
      json.append("},");
      json.append("\"preferences\":[");
      for (int i = 0; i < 2; i++) {
        if (i > 0) json.append(",");
        json.append("\"")
            .append(SAMPLE_CATEGORIES.get(random.nextInt(SAMPLE_CATEGORIES.size())))
            .append("\"");
      }
      json.append("]");
    }

    json.append("}");
    return json.toString();
  }

  private String generateProductSchema(boolean includeOptional, int arraySize) {
    StringBuilder json = new StringBuilder("{");

    json.append("\"id\":\"prod_").append(10000 + random.nextInt(90000)).append("\",");
    json.append("\"name\":\"")
        .append(SAMPLE_PRODUCTS.get(random.nextInt(SAMPLE_PRODUCTS.size())))
        .append("\",");
    json.append("\"price\":")
        .append(String.format("%.2f", 10.0 + random.nextDouble() * 990.0))
        .append(",");
    json.append("\"category\":\"")
        .append(SAMPLE_CATEGORIES.get(random.nextInt(SAMPLE_CATEGORIES.size())))
        .append("\"");

    if (includeOptional) {
      json.append(",\"description\":\"").append("高质量产品，值得信赖").append("\",");
      json.append("\"inStock\":").append(random.nextBoolean()).append(",");
      json.append("\"tags\":[");
      for (int i = 0; i < Math.min(arraySize, 3); i++) {
        if (i > 0) json.append(",");
        json.append("\"tag").append(i + 1).append("\"");
      }
      json.append("],");
      json.append("\"specifications\":{");
      json.append("\"weight\":\"")
          .append(String.format("%.1f", 0.1 + random.nextDouble() * 5.0))
          .append("kg\",");
      json.append("\"dimensions\":\"")
          .append((10 + random.nextInt(50)) + "x" + (10 + random.nextInt(50)) + "cm")
          .append("\"");
      json.append("}");
    }

    json.append("}");
    return json.toString();
  }

  private String generateOrderSchema(boolean includeOptional, int arraySize) {
    StringBuilder json = new StringBuilder("{");

    json.append("\"orderId\":\"ORD")
        .append(String.format("%08d", random.nextInt(100000000)))
        .append("\",");
    json.append("\"userId\":").append(1000 + random.nextInt(9000)).append(",");
    json.append("\"status\":\"").append(getRandomStatus()).append("\",");
    json.append("\"total\":").append(String.format("%.2f", 50.0 + random.nextDouble() * 950.0));

    if (includeOptional) {
      json.append(",\"items\":[");
      for (int i = 0; i < Math.min(arraySize, 5); i++) {
        if (i > 0) json.append(",");
        json.append("{");
        json.append("\"productId\":\"prod_").append(10000 + random.nextInt(90000)).append("\",");
        json.append("\"quantity\":").append(1 + random.nextInt(5)).append(",");
        json.append("\"price\":").append(String.format("%.2f", 10.0 + random.nextDouble() * 90.0));
        json.append("}");
      }
      json.append("],");
      json.append("\"shipping\":{");
      json.append("\"method\":\"").append("标准快递").append("\",");
      json.append("\"cost\":").append(String.format("%.2f", 5.0 + random.nextDouble() * 15.0));
      json.append("}");
    }

    json.append("}");
    return json.toString();
  }

  private String generateEventSchema(boolean includeOptional) {
    StringBuilder json = new StringBuilder("{");

    json.append("\"eventId\":\"").append(java.util.UUID.randomUUID().toString()).append("\",");
    json.append("\"type\":\"")
        .append(SAMPLE_EVENTS.get(random.nextInt(SAMPLE_EVENTS.size())))
        .append("\",");
    json.append("\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\",");
    json.append("\"userId\":").append(1000 + random.nextInt(9000));

    if (includeOptional) {
      json.append(",\"properties\":{");
      json.append("\"page\":\"").append("/page" + random.nextInt(10)).append("\",");
      json.append("\"userAgent\":\"").append("Mozilla/5.0 (compatible)").append("\",");
      json.append("\"ip\":\"").append(generateIpAddress()).append("\"");
      json.append("},");
      json.append("\"metadata\":{");
      json.append("\"source\":\"").append("web").append("\",");
      json.append("\"version\":\"").append("1.0.").append(random.nextInt(10)).append("\"");
      json.append("}");
    }

    json.append("}");
    return json.toString();
  }

  private String generateCustomSchema(int nestedLevel, int arraySize) {
    StringBuilder json = new StringBuilder("{");

    json.append("\"id\":").append(random.nextInt(10000)).append(",");
    json.append("\"name\":\"").append("CustomObject").append("\",");
    json.append("\"active\":").append(random.nextBoolean());

    if (nestedLevel > 0) {
      json.append(",\"nested\":{");
      json.append("\"level\":").append(nestedLevel).append(",");
      json.append("\"data\":\"").append("nested_data_" + random.nextInt(100)).append("\"");
      if (nestedLevel > 1) {
        json.append(",\"deep\":").append(generateCustomSchema(nestedLevel - 1, arraySize));
      }
      json.append("}");
    }

    json.append(",\"items\":[");
    for (int i = 0; i < Math.min(arraySize, 3); i++) {
      if (i > 0) json.append(",");
      json.append("\"item").append(i + 1).append("\"");
    }
    json.append("]");

    json.append("}");
    return json.toString();
  }

  private String generateEmail() {
    String[] domains = {"gmail.com", "163.com", "qq.com", "hotmail.com"};
    return "user" + random.nextInt(1000) + "@" + domains[random.nextInt(domains.length)];
  }

  private String generatePhone() {
    return "1" + (30 + random.nextInt(70)) + String.format("%08d", random.nextInt(100000000));
  }

  private String generateIpAddress() {
    return (1 + random.nextInt(254))
        + "."
        + random.nextInt(256)
        + "."
        + random.nextInt(256)
        + "."
        + (1 + random.nextInt(254));
  }

  private String getRandomStatus() {
    String[] statuses = {"pending", "processing", "shipped", "delivered", "cancelled"};
    return statuses[random.nextInt(statuses.length)];
  }

  private String formatJson(String jsonData, String format) {
    switch (format) {
      case "COMPACT":
        return jsonData.replaceAll("\\s+", "");
      case "PRETTY":
        return prettyFormatJson(jsonData);
      case "JSON":
      default:
        return jsonData;
    }
  }

  private String prettyFormatJson(String jsonData) {
    // 简单的JSON格式化
    StringBuilder pretty = new StringBuilder();
    int indent = 0;
    boolean inString = false;

    for (int i = 0; i < jsonData.length(); i++) {
      char c = jsonData.charAt(i);

      if (c == '"' && (i == 0 || jsonData.charAt(i - 1) != '\\')) {
        inString = !inString;
      }

      if (!inString) {
        if (c == '{' || c == '[') {
          pretty.append(c).append('\n');
          indent++;
          pretty.append("  ".repeat(indent));
        } else if (c == '}' || c == ']') {
          pretty.append('\n');
          indent--;
          pretty.append("  ".repeat(indent)).append(c);
        } else if (c == ',') {
          pretty.append(c).append('\n');
          pretty.append("  ".repeat(indent));
        } else {
          pretty.append(c);
        }
      } else {
        pretty.append(c);
      }
    }

    return pretty.toString();
  }
}
