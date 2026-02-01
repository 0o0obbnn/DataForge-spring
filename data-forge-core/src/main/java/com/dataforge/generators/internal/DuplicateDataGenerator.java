package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 重复数据生成器
 *
 * <p>根据DataForge设计文档要求，生成各种类型的重复数据用于测试系统处理重复数据的能力。
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class DuplicateDataGenerator extends BaseGenerator
    implements DataGenerator<Object, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(DuplicateDataGenerator.class);

  private final Random random;

  // 重复数据模式
  private static final List<String> DUPLICATE_PATTERNS =
      Arrays.asList(
          // 文本重复模式
          "duplicate_text",
          "重复文本",
          "A".repeat(100),

          // 数值重复模式
          "123456789",
          "987654321",
          "000000000",

          // 特殊字符重复模式
          "!@#$%^&*()",
          "__________",
          "----------",

          // 日期时间重复模式
          "2023-01-01",
          "00:00:00",
          "9999-12-31");

  // 重复次数配置
  private static final List<Integer> REPEAT_COUNTS = Arrays.asList(2, 3, 5, 10, 50, 100);

  public DuplicateDataGenerator() {
    this.random = new Random();
  }

  @Override
  public String getType() {
    return "DUPLICATE_DATA";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public Object generate(FieldConfig config, DataForgeContext context) {
    try {
      String duplicateType = config.getParam("duplicateType", String.class, "ALL");
      String targetField = config.getParam("targetField", String.class, "ANY");
      int repeatCount = config.getParam("repeatCount", Integer.class, getRandomRepeatCount());
      String separator = config.getParam("separator", String.class, "");
      double frequency = Double.parseDouble(config.getParam("frequency", String.class, "0.1"));

      // 根据频率决定是否生成重复数据
      if (random.nextDouble() > frequency) {
        return generateNormalData(targetField);
      }

      return generateDuplicateData(duplicateType, targetField, repeatCount, separator);

    } catch (Exception e) {
      logger.warn("Error generating duplicate data: {}", e.getMessage());
      return "duplicate_data_error";
    }
  }

  /** 生成重复数据 */
  private Object generateDuplicateData(
      String duplicateType, String targetField, int repeatCount, String separator) {
    switch (duplicateType.toUpperCase()) {
      case "TEXT":
        return generateTextDuplicate(targetField, repeatCount, separator);
      case "NUMBER":
        return generateDuplicateContent(DUPLICATE_PATTERNS.get(3), repeatCount, ",");
      case "SPECIAL_CHARS":
        return generateDuplicateContent(
            DUPLICATE_PATTERNS.get(7), repeatCount, separator); // "__________"
      case "DATE":
        return generateDuplicateContent(
            DUPLICATE_PATTERNS.get(9), repeatCount, separator); // "2023-01-01"
      case "ALL":
      default:
        return generateRandomDuplicate(targetField, repeatCount, separator);
    }
  }

  /** 生成文本重复数据 */
  private Object generateTextDuplicate(String targetField, int repeatCount, String separator) {
    String baseText;

    switch (targetField.toUpperCase()) {
      case "NAME":
        baseText = "张三";
        break;
      case "EMAIL":
        baseText = "user@example.com";
        break;
      case "PHONE":
        baseText = "13800138000";
        break;
      case "ADDRESS":
        baseText = "北京市朝阳区某某街道";
        break;
      default:
        baseText = DUPLICATE_PATTERNS.get(random.nextInt(3));
        break;
    }

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < repeatCount; i++) {
      if (i > 0) result.append(separator);
      result.append(baseText);
    }
    return result.toString();
  }

  /** 生成重复内容 */
  private Object generateDuplicateContent(String baseContent, int repeatCount, String separator) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < repeatCount; i++) {
      if (i > 0) result.append(separator);
      result.append(baseContent);
    }
    return result.toString();
  }

  /** 生成随机重复数据 */
  private Object generateRandomDuplicate(String targetField, int repeatCount, String separator) {
    String[] duplicateTypes = {"TEXT", "NUMBER", "SPECIAL_CHARS", "DATE"};
    String randomType = duplicateTypes[random.nextInt(duplicateTypes.length)];
    return generateDuplicateData(randomType, targetField, repeatCount, separator);
  }

  /** 生成正常数据（非重复数据） */
  private Object generateNormalData(String targetField) {
    switch (targetField.toUpperCase()) {
      case "NAME":
        return "李四";
      case "EMAIL":
        return "normal@example.com";
      case "PHONE":
        return "13900139000";
      case "ADDRESS":
        return "上海市浦东新区某某路";
      default:
        return "normal_data";
    }
  }

  /** 获取随机重复次数 */
  private int getRandomRepeatCount() {
    return REPEAT_COUNTS.get(random.nextInt(REPEAT_COUNTS.size()));
  }

  @Override
  public boolean isValidConfig(FieldConfig config) {
    if (config == null) {
      return false;
    }

    String duplicateType = config.getParam("duplicateType", String.class, "ALL");
    int repeatCount = config.getParam("repeatCount", Integer.class, 1);
    double frequency = Double.parseDouble(config.getParam("frequency", String.class, "0.1"));

    // 验证重复数据类型
    String[] validDuplicateTypes = {"TEXT", "NUMBER", "SPECIAL_CHARS", "DATE", "ALL"};
    if (!Arrays.asList(validDuplicateTypes).contains(duplicateType.toUpperCase())) {
      return false;
    }

    // 验证重复次数
    if (repeatCount < 1 || repeatCount > 1000) {
      return false;
    }

    // 验证频率
    return frequency >= 0.0 && frequency <= 1.0;
  }

  @Override
  public String getDescription() {
    return "生成各种类型的重复数据，包括文本重复、数值重复、特殊字符重复、日期重复等，" + "用于测试系统处理重复数据的能力";
  }
}
