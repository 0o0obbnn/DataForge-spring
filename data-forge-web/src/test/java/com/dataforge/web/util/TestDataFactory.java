package com.dataforge.web.util;

import com.dataforge.config.FieldConfigWrapper;
import com.dataforge.config.OutputConfig;
import com.dataforge.config.SimpleFieldConfig;
import com.dataforge.web.model.GenerateRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 测试数据工厂类，提供测试数据生成的辅助方法。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public final class TestDataFactory {

  private TestDataFactory() {}

  /**
   * 创建简单的YAML配置。
   *
   * @param structure 结构类型
   * @param depth 深度
   * @param keyCount 键数量
   * @return 配置对象
   */
  public static SimpleFieldConfig createYamlConfig(String structure, int depth, int keyCount) {
    SimpleFieldConfig config = new SimpleFieldConfig();
    config.setType("yaml");
    config.setParam("structure", structure);
    config.setParam("depth", String.valueOf(depth));
    config.setParam("key_count", String.valueOf(keyCount));
    return config;
  }

  /**
   * 生成唯一值列表。
   *
   * @param count 数量
   * @return 唯一值列表
   */
  public static List<String> generateUniqueValues(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> UUID.randomUUID().toString())
        .collect(Collectors.toList());
  }

  /**
   * 创建字段配置包装器。
   *
   * @param name 字段名
   * @param type 字段类型
   * @return 字段配置包装器
   */
  public static FieldConfigWrapper createField(String name, String type) {
    return new FieldConfigWrapper(name, type);
  }

  /**
   * 创建字段配置包装器。
   *
   * @param name 字段名
   * @param type 字段类型
   * @param params 参数
   * @return 字段配置包装器
   */
  public static FieldConfigWrapper createField(
      String name, String type, Map<String, Object> params) {
    return new FieldConfigWrapper(name, type, params);
  }

  /**
   * 创建字段配置包装器。
   *
   * @param name 字段名
   * @param type 字段类型
   * @param description 描述
   * @return 字段配置包装器
   */
  public static FieldConfigWrapper createField(String name, String type, String description) {
    return FieldConfigWrapper.of(name, type).withDescription(description);
  }

  /**
   * 创建生成请求。
   *
   * @param count 生成数量
   * @param fields 字段列表
   * @return 生成请求
   */
  public static GenerateRequest createGenerateRequest(int count, List<FieldConfigWrapper> fields) {
    GenerateRequest request = new GenerateRequest();
    request.setCount(count);
    request.setFields(fields);
    request.setOutput(createOutputConfig());
    return request;
  }

  /**
   * 创建输出配置。
   *
   * @return 输出配置
   */
  public static OutputConfig createOutputConfig() {
    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.JSON);
    return config;
  }

  /**
   * 创建简单的生成请求。
   *
   * @param count 生成数量
   * @return 生成请求
   */
  public static GenerateRequest createSimpleGenerateRequest(int count) {
    List<FieldConfigWrapper> fields = new ArrayList<>();
    fields.add(createField("name", "name"));
    fields.add(createField("age", "age"));
    fields.add(createField("email", "email"));
    return createGenerateRequest(count, fields);
  }

  /**
   * 创建复杂的生成请求。
   *
   * @param count 生成数量
   * @return 生成请求
   */
  public static GenerateRequest createComplexGenerateRequest(int count) {
    List<FieldConfigWrapper> fields = new ArrayList<>();
    fields.add(createField("id", "uuid"));
    fields.add(createField("name", "name"));
    fields.add(createField("email", "email"));
    fields.add(createField("phone", "phone"));
    fields.add(createField("age", "age"));
    fields.add(createField("salary", "decimal"));
    fields.add(createField("birthday", "date"));
    fields.add(createField("address", "address"));
    fields.add(createField("company", "company"));
    fields.add(createField("createdAt", "timestamp"));
    return createGenerateRequest(count, fields);
  }

  /**
   * 生成随机字符串。
   *
   * @param length 长度
   * @return 随机字符串
   */
  public static String randomString(int length) {
    return UUID.randomUUID().toString().replace("-", "").substring(0, Math.min(length, 32));
  }

  /**
   * 生成随机邮箱。
   *
   * @return 随机邮箱
   */
  public static String randomEmail() {
    return "user" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
  }

  /**
   * 生成随机数字。
   *
   * @param min 最小值
   * @param max 最大值
   * @return 随机数字
   */
  public static int randomNumber(int min, int max) {
    return min + (int) (Math.random() * (max - min + 1));
  }

  /**
   * 创建批量字段配置。
   *
   * @param prefix 前缀
   * @param count 数量
   * @param type 类型
   * @return 字段配置列表
   */
  public static List<FieldConfigWrapper> createBatchFields(String prefix, int count, String type) {
    return IntStream.range(0, count)
        .mapToObj(i -> createField(prefix + i, type))
        .collect(Collectors.toList());
  }

  /**
   * 创建包含所有类型的字段配置。
   *
   * @return 字段配置列表
   */
  public static List<FieldConfigWrapper> createAllTypeFields() {
    List<FieldConfigWrapper> fields = new ArrayList<>();
    fields.add(createField("id", "uuid"));
    fields.add(createField("name", "name"));
    fields.add(createField("email", "email"));
    fields.add(createField("phone", "phone"));
    fields.add(createField("age", "age"));
    fields.add(createField("price", "decimal"));
    fields.add(createField("birthday", "date"));
    fields.add(createField("createdAt", "timestamp"));
    fields.add(createField("isActive", "boolean"));
    fields.add(createField("address", "address"));
    fields.add(createField("company", "company"));
    fields.add(createField("url", "url"));
    fields.add(createField("ip", "ip"));
    fields.add(createField("mac", "mac"));
    fields.add(createField("userAgent", "string"));  // Using string as a placeholder for userAgent
    return fields;
  }

  /**
   * 创建测试模板JSON。
   *
   * @param name 模板名称
   * @return JSON字符串
   */
  public static String createTemplateJson(String name) {
    return String.format(
        """
        {
            "name": "%s",
            "description": "Test template",
            "fields": [
                {"name": "id", "type": "uuid"},
                {"name": "name", "type": "string"},
                {"name": "email", "type": "email"}
            ],
            "isActive": true
        }
        """,
        name);
  }

  /**
   * 创建批量生成请求列表。
   *
   * @param count 请求数量
   * @param recordsPerRequest 每个请求的记录数
   * @return 请求列表
   */
  public static List<GenerateRequest> createBatchGenerateRequests(
      int count, int recordsPerRequest) {
    return IntStream.range(0, count)
        .mapToObj(i -> createSimpleGenerateRequest(recordsPerRequest))
        .collect(Collectors.toList());
  }

  /**
   * 创建带参数的生成请求。
   *
   * @param count 生成数量
   * @param fields 字段列表
   * @param async 是否异步
   * @return 生成请求
   */
  public static GenerateRequest createGenerateRequest(
      int count, List<FieldConfigWrapper> fields, boolean async) {
    GenerateRequest request = createGenerateRequest(count, fields);
    // async not exposed on GenerateRequest; callers may use threads field for concurrency
    return request;
  }

  /**
   * 创建模板ID生成请求。
   *
   * @param templateId 模板ID
   * @param count 生成数量
   * @return 生成请求
   */
  public static GenerateRequest createTemplateGenerateRequest(String templateId, int count) {
    GenerateRequest request = new GenerateRequest();
    request.setCount(count);
    request.setOutput(createOutputConfig());
    request.setFields(createSimpleGenerateRequest(1).getFields());
    return request;
  }

  /**
   * 生成随机日期时间。
   *
   * @param daysOffset 天数偏移
   * @return 日期时间
   */
  public static LocalDateTime randomDateTime(int daysOffset) {
    return LocalDateTime.now().plusDays(daysOffset);
  }

  /**
   * 创建性能测试配置。
   *
   * @param iterations 迭代次数
   * @param warmupIterations 预热迭代次数
   * @return 配置对象
   */
  public static SimpleFieldConfig createPerformanceConfig(int iterations, int warmupIterations) {
    SimpleFieldConfig config = new SimpleFieldConfig();
    config.setType("string");
    config.setParam("iterations", String.valueOf(iterations));
    config.setParam("warmup_iterations", String.valueOf(warmupIterations));
    return config;
  }

  /**
   * 创建带参数的字段配置。
   *
   * @param name 字段名
   * @param type 字段类型
   * @param paramKey 参数键
   * @param paramValue 参数值
   * @return 字段配置包装器
   */
  public static FieldConfigWrapper createFieldWithParam(
      String name, String type, String paramKey, Object paramValue) {
    Map<String, Object> params = new HashMap<>();
    params.put(paramKey, paramValue);
    return createField(name, type, params);
  }

  /**
   * 创建CSV输出配置。
   *
   * @param filePath 文件路径
   * @return 输出配置
   */
  public static OutputConfig createCsvOutputConfig(String filePath) {
    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.CSV);
    config.setFile(filePath);
    return config;
  }

  /**
   * 创建JSON输出配置。
   *
   * @param filePath 文件路径
   * @return 输出配置
   */
  public static OutputConfig createJsonOutputConfig(String filePath) {
    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.JSON);
    config.setFile(filePath);
    return config;
  }

  /**
   * 创建YAML输出配置。
   *
   * @param filePath 文件路径
   * @return 输出配置
   */
  public static OutputConfig createYamlOutputConfig(String filePath) {
    OutputConfig config = new OutputConfig();
    config.setFormat(OutputConfig.Format.JSON);
    config.setFile(filePath);
    return config;
  }
}
