package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * YAML内容生成器
 *
 * <p>用于生成各种格式的YAML内容，适用于配置文件测试、数据导入测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>structure: YAML结构类型 (SIMPLE|NESTED|LIST|COMPLEX) 默认: SIMPLE
 *   <li>depth: 嵌套深度 (1-10) 默认: 3
 *   <li>key_count: 键值对数量 (1-100) 默认: 5
 *   <li>value_types: 值类型 (STRING|NUMBER|BOOLEAN|NULL|ALL) 默认: ALL
 *   <li>include_comments: 是否包含注释 默认: false
 *   <li>invalid_yaml: 是否生成无效YAML用于测试 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class YamlGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(YamlGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // YAML结构类型
  public enum StructureType {
    SIMPLE("简单结构"),
    NESTED("嵌套结构"),
    LIST("列表结构"),
    COMPLEX("复杂结构");

    private final String description;

    StructureType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 值类型
  public enum ValueType {
    STRING,
    NUMBER,
    BOOLEAN,
    NULL
  }

  @Override
  public String getType() {
    return "yaml";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String structure = getStringParam(config, "structure", "SIMPLE");
      int depth = getIntParam(config, "depth", 3);
      int keyCount = getIntParam(config, "key_count", 5);
      String valueTypesParam = getStringParam(config, "value_types", "ALL");
      boolean includeComments = getBooleanParam(config, "include_comments", false);
      boolean invalidYaml = getBooleanParam(config, "invalid_yaml", false);

      // 限制参数范围
      depth = Math.max(1, Math.min(10, depth));
      keyCount = Math.max(1, Math.min(100, keyCount));

      // 解析值类型
      Set<ValueType> valueTypes = parseValueTypes(valueTypesParam);

      StructureType structureType = StructureType.SIMPLE;
      try {
        structureType = StructureType.valueOf(structure.toUpperCase());
      } catch (IllegalArgumentException e) {
        logger.warn("Invalid structure type: {}, using SIMPLE", structure);
      }

      StringBuilder yaml = new StringBuilder();

      if (includeComments) {
        yaml.append("# Auto-generated YAML content\n");
        yaml.append("# Structure type: ").append(structureType.name()).append("\n\n");
      }

      switch (structureType) {
        case SIMPLE:
          generateSimpleYaml(yaml, keyCount, valueTypes, invalidYaml);
          break;
        case NESTED:
          generateNestedYaml(yaml, depth, keyCount, valueTypes, invalidYaml);
          break;
        case LIST:
          generateListYaml(yaml, keyCount, valueTypes, invalidYaml);
          break;
        case COMPLEX:
          generateComplexYaml(yaml, depth, keyCount, valueTypes, invalidYaml);
          break;
      }

      return yaml.toString();
    } catch (Exception e) {
      logger.error("Error generating YAML content", e);
      return "# Error generating YAML content\n";
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String getDescription() {
    return "YAML内容生成器，用于生成各种格式的YAML数据";
  }

  /** 解析值类型参数 */
  private Set<ValueType> parseValueTypes(String valueTypesParam) {
    Set<ValueType> valueTypes = new HashSet<>();

    if ("ALL".equalsIgnoreCase(valueTypesParam)) {
      valueTypes.addAll(Arrays.asList(ValueType.values()));
    } else {
      String[] types = valueTypesParam.split(",");
      for (String type : types) {
        try {
          valueTypes.add(ValueType.valueOf(type.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
          logger.warn("Invalid value type: {}, skipping", type);
        }
      }
    }

    // 如果没有有效的类型，默认使用所有类型
    if (valueTypes.isEmpty()) {
      valueTypes.addAll(Arrays.asList(ValueType.values()));
    }

    return valueTypes;
  }

  /** 生成简单YAML结构 */
  private void generateSimpleYaml(
      StringBuilder yaml, int keyCount, Set<ValueType> valueTypes, boolean invalid) {
    for (int i = 0; i < keyCount; i++) {
      String key = generateKey(i);
      Object value = generateValue(valueTypes);

      if (invalid && i == keyCount - 1) {
        // 生成无效YAML - 缺少冒号
        yaml.append(key).append(" ").append(formatValue(value)).append("\n");
      } else {
        yaml.append(key).append(": ").append(formatValue(value)).append("\n");
      }
    }
  }

  /** 生成嵌套YAML结构 */
  private void generateNestedYaml(
      StringBuilder yaml, int depth, int keyCount, Set<ValueType> valueTypes, boolean invalid) {
    generateNestedStructure(yaml, "", depth, keyCount, valueTypes, invalid, 0);
  }

  /** 递归生成嵌套结构 */
  private void generateNestedStructure(
      StringBuilder yaml,
      String prefix,
      int maxDepth,
      int keyCount,
      Set<ValueType> valueTypes,
      boolean invalid,
      int currentDepth) {
    if (currentDepth >= maxDepth) {
      // 到达最大深度，生成叶子节点
      String key = generateKey(0);
      Object value = generateValue(valueTypes);
      yaml.append(prefix).append(key).append(": ").append(formatValue(value)).append("\n");
      return;
    }

    for (int i = 0; i < keyCount; i++) {
      String key = generateKey(i);
      if (currentDepth == 0 && invalid) {
        // 生成无效YAML - 缺少缩进
        yaml.append(key).append(":\n");
        generateNestedStructure(
            yaml, "  ", maxDepth, keyCount, valueTypes, false, currentDepth + 1);
      } else {
        yaml.append(prefix).append(key).append(":\n");
        generateNestedStructure(
            yaml, prefix + "  ", maxDepth, keyCount, valueTypes, false, currentDepth + 1);
      }
    }
  }

  /** 生成列表YAML结构 */
  private void generateListYaml(
      StringBuilder yaml, int keyCount, Set<ValueType> valueTypes, boolean invalid) {
    yaml.append("items:\n");
    for (int i = 0; i < keyCount; i++) {
      Object value = generateValue(valueTypes);
      if (invalid && i == 0) {
        // 生成无效YAML - 列表项缺少破折号
        yaml.append("  ").append(formatValue(value)).append("\n");
      } else {
        yaml.append("  - ").append(formatValue(value)).append("\n");
      }
    }
  }

  /** 生成复杂YAML结构 */
  private void generateComplexYaml(
      StringBuilder yaml, int depth, int keyCount, Set<ValueType> valueTypes, boolean invalid) {
    // 混合嵌套结构和列表
    yaml.append("config:\n");
    yaml.append("  database:\n");
    yaml.append("    host: ").append(formatValue("localhost")).append("\n");
    yaml.append("    port: ").append(formatValue(5432)).append("\n");
    yaml.append("    credentials:\n");
    yaml.append("      username: ").append(formatValue("admin")).append("\n");
    yaml.append("      password: ").append(formatValue("secret123")).append("\n");

    yaml.append("  features:\n");
    yaml.append("    - name: ").append(formatValue("authentication")).append("\n");
    yaml.append("      enabled: ").append(formatValue(true)).append("\n");
    yaml.append("    - name: ").append(formatValue("logging")).append("\n");
    yaml.append("      enabled: ").append(formatValue(false)).append("\n");

    if (invalid) {
      // 生成无效YAML - 错误的缩进
      yaml.append("  nested:\n");
      yaml.append("  key: ").append(formatValue("value_with_wrong_indentation")).append("\n");
    }
  }

  /** 生成键名 */
  private String generateKey(int index) {
    String[] prefixes = {
      "name", "title", "description", "value", "config", "setting", "property", "attribute"
    };
    String[] suffixes = {"id", "type", "status", "count", "size", "length", "width", "height"};

    if (random.nextBoolean()) {
      return prefixes[random.nextInt(prefixes.length)] + "_" + index;
    } else {
      return "key_" + index + "_" + suffixes[random.nextInt(suffixes.length)];
    }
  }

  /** 生成值 */
  private Object generateValue(Set<ValueType> valueTypes) {
    ValueType[] types = valueTypes.toArray(new ValueType[0]);
    ValueType type = types[random.nextInt(types.length)];

    switch (type) {
      case STRING:
        return generateRandomString();
      case NUMBER:
        return random.nextInt(1000);
      case BOOLEAN:
        return random.nextBoolean();
      case NULL:
        return null;
      default:
        return generateRandomString();
    }
  }

  /** 生成随机字符串 */
  private String generateRandomString() {
    String[] words = {
      "apple", "banana", "cherry", "date", "elderberry", "fig", "grape", "honeydew"
    };
    return words[random.nextInt(words.length)] + "_" + random.nextInt(100);
  }

  /** 格式化值为YAML字符串 */
  private String formatValue(Object value) {
    if (value == null) {
      return "null";
    } else if (value instanceof String) {
      return "\"" + value + "\"";
    } else if (value instanceof Boolean) {
      return value.toString();
    } else {
      return value.toString();
    }
  }
}
