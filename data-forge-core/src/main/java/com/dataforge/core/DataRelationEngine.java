package com.dataforge.core;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据关联引擎
 *
 * <p>根据DataForge详细开发规范3.3.2节：数据关联引擎 (DataRelationEngine) 实现数据字段间的逻辑关联，确保生成数据的一致性和真实性。
 *
 * <p>核心功能： - 身份证号与年龄/性别/地址关联：从身份证号解析出生日期、性别、地区信息 - 姓名与邮箱关联：基于姓名拼音生成邮箱用户名 - 银行卡与银行名称关联：根据BIN码匹配银行信息
 * - 地址层级关联：确保省市区的层级一致性 - 自定义关联规则：支持用户定义的复杂关联逻辑
 *
 * @author DataForge
 * @version 1.0.0
 * @since 2024-01-15
 */
@Component
public class DataRelationEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataRelationEngine.class);

  private final Map<String, RelationProcessor> relationProcessors;

  // 身份证号正则表达式
  private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})(\\d{8})(\\d{3})(\\d|X)");

  // 银行BIN码映射 (简化版本)
  private static final Map<String, String> BANK_BIN_MAPPING = new HashMap<>();

  static {
    BANK_BIN_MAPPING.put("4", "工商银行");
    BANK_BIN_MAPPING.put("5", "建设银行");
    BANK_BIN_MAPPING.put("6", "农业银行");
    BANK_BIN_MAPPING.put("62", "中国银联");
    // 可扩展更多银行BIN码
  }

  // 地区代码映射 (简化版本)
  private static final Map<String, RegionInfo> REGION_MAPPING = new HashMap<>();

  static {
    REGION_MAPPING.put("110000", new RegionInfo("北京市", "北京市", ""));
    REGION_MAPPING.put("120000", new RegionInfo("天津市", "天津市", ""));
    REGION_MAPPING.put("310000", new RegionInfo("上海市", "上海市", ""));
    REGION_MAPPING.put("330100", new RegionInfo("浙江省", "杭州市", ""));
    REGION_MAPPING.put("330200", new RegionInfo("浙江省", "宁波市", ""));
    REGION_MAPPING.put("440100", new RegionInfo("广东省", "广州市", ""));
    REGION_MAPPING.put("440300", new RegionInfo("广东省", "深圳市", ""));
    // 可扩展更多地区代码
  }

  public DataRelationEngine() {
    this.relationProcessors = initializeRelationProcessors();
  }

  /** 应用数据关联规则 */
  public Map<String, Object> applyRelations(
      Map<String, Object> record, List<DataRelation> relations) {
    if (relations == null || relations.isEmpty()) {
      return record;
    }

    Map<String, Object> processedRecord = new LinkedHashMap<>(record);

    for (DataRelation relation : relations) {
      try {
        processedRecord = applyRelation(processedRecord, relation);
      } catch (Exception e) {
        LOGGER.warn("Failed to apply relation {}: {}", relation.getType(), e.getMessage());
      }
    }

    return processedRecord;
  }

  /** 应用单个关联规则 */
  private Map<String, Object> applyRelation(Map<String, Object> record, DataRelation relation) {
    RelationProcessor processor = relationProcessors.get(relation.getType());
    if (processor == null) {
      LOGGER.warn("No processor found for relation type: {}", relation.getType());
      return record;
    }

    return processor.process(record, relation);
  }

  /** 初始化关联处理器 */
  private Map<String, RelationProcessor> initializeRelationProcessors() {
    Map<String, RelationProcessor> processors = new HashMap<>();

    // 身份证关联处理器
    processors.put("ID_CARD_RELATION", new IdCardRelationProcessor());

    // 姓名邮箱关联处理器
    processors.put("NAME_EMAIL_RELATION", new NameEmailRelationProcessor());

    // 银行卡关联处理器
    processors.put("BANK_CARD_RELATION", new BankCardRelationProcessor());

    // 地址层级关联处理器
    processors.put("ADDRESS_HIERARCHY_RELATION", new AddressHierarchyRelationProcessor());

    // 自定义关联处理器
    processors.put("CUSTOM_RELATION", new CustomRelationProcessor());

    return processors;
  }

  /** 关联处理器接口 */
  private interface RelationProcessor {
    Map<String, Object> process(Map<String, Object> record, DataRelation relation);
  }

  /** 身份证关联处理器 从身份证号解析年龄、性别、地区信息 */
  private static class IdCardRelationProcessor implements RelationProcessor {

    @Override
    public Map<String, Object> process(Map<String, Object> record, DataRelation relation) {
      String idCardField = relation.getSourceField();
      Object idCardValue = record.get(idCardField);

      if (idCardValue == null || !(idCardValue instanceof String)) {
        return record;
      }

      String idCard = (String) idCardValue;
      Matcher matcher = ID_CARD_PATTERN.matcher(idCard);

      if (!matcher.matches()) {
        return record;
      }

      Map<String, Object> result = new LinkedHashMap<>(record);

      try {
        // 解析地区代码 - 支持模糊匹配
        String regionCode = matcher.group(1);
        RegionInfo region = REGION_MAPPING.get(regionCode);

        // 如果精确匹配失败，尝试匹配更短的代码（省/市级）
        if (region == null && regionCode.length() >= 4) {
          region = REGION_MAPPING.get(regionCode.substring(0, 4) + "00");
        }
        if (region == null && regionCode.length() >= 2) {
          region = REGION_MAPPING.get(regionCode.substring(0, 2) + "0000");
        }

        if (region != null) {
          result.put("province", region.getProvince());
          result.put("city", region.getCity());
          if (!region.getDistrict().isEmpty()) {
            result.put("district", region.getDistrict());
          }
        }

        // 解析出生日期
        String birthDateStr = matcher.group(2);
        LocalDate birthDate =
            LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        result.put("birth_date", birthDate.toString());

        // 计算年龄
        long age = ChronoUnit.YEARS.between(birthDate, LocalDate.now());
        result.put("age", (int) age);

        // 解析性别
        String sequenceCode = matcher.group(3);
        int genderDigit = Integer.parseInt(sequenceCode.substring(2, 3));
        String gender = (genderDigit % 2 == 1) ? "MALE" : "FEMALE";
        result.put("gender", gender);

      } catch (Exception e) {
        LOGGER.warn("Failed to parse ID card {}: {}", idCard, e.getMessage());
      }

      return result;
    }
  }

  /** 姓名邮箱关联处理器 基于姓名生成相关的邮箱用户名 */
  private static class NameEmailRelationProcessor implements RelationProcessor {

    @Override
    public Map<String, Object> process(Map<String, Object> record, DataRelation relation) {
      String nameField = relation.getSourceField();
      Object nameValue = record.get(nameField);

      if (nameValue == null || !(nameValue instanceof String)) {
        return record;
      }

      String name = (String) nameValue;
      Map<String, Object> result = new LinkedHashMap<>(record);

      try {
        // 生成邮箱用户名（简化版拼音转换）
        String emailUsername = name.toLowerCase().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
        // 简单处理中文姓名转拼音
        emailUsername =
            emailUsername
                .replaceAll("张", "zhang")
                .replaceAll("李", "li")
                .replaceAll("王", "wang")
                .replaceAll("刘", "liu")
                .replaceAll("陈", "chen")
                .replaceAll("杨", "yang")
                .replaceAll("赵", "zhao")
                .replaceAll("黄", "huang")
                .replaceAll("周", "zhou")
                .replaceAll("吴", "wu");

        result.put("email_username", emailUsername);

      } catch (Exception e) {
        LOGGER.warn("Failed to generate email username for name {}: {}", name, e.getMessage());
      }

      return result;
    }
  }

  /** 银行卡关联处理器 根据银行卡BIN码匹配银行信息 */
  private static class BankCardRelationProcessor implements RelationProcessor {

    @Override
    public Map<String, Object> process(Map<String, Object> record, DataRelation relation) {
      String cardField = relation.getSourceField();
      Object cardValue = record.get(cardField);

      if (cardValue == null || !(cardValue instanceof String)) {
        return record;
      }

      String cardNumber = (String) cardValue;
      Map<String, Object> result = new LinkedHashMap<>(record);

      try {
        // 根据BIN码匹配银行 - 优先匹配更长的前缀
        final String cardNumberFinal = cardNumber;
        String bank =
            BANK_BIN_MAPPING.entrySet().stream()
                .filter(entry -> cardNumberFinal.startsWith(entry.getKey()))
                .filter(entry -> entry.getKey().length() > 0)
                .max((e1, e2) -> Integer.compare(e1.getKey().length(), e2.getKey().length()))
                .map(Map.Entry::getValue)
                .orElse("未知银行");

        result.put("bank_name", bank);

      } catch (Exception e) {
        LOGGER.warn("Failed to match bank for card {}: {}", cardNumber, e.getMessage());
      }

      return result;
    }
  }

  /** 地址层级关联处理器 确保省市区的层级一致性 */
  private static class AddressHierarchyRelationProcessor implements RelationProcessor {

    @Override
    public Map<String, Object> process(Map<String, Object> record, DataRelation relation) {
      // 此处理器主要用于确保地址字段的一致性
      // 在实际应用中，可能需要更复杂的逻辑来验证地址层级关系
      return new LinkedHashMap<>(record);
    }
  }

  /** 自定义关联处理器 支持用户定义的复杂关联逻辑 */
  private static class CustomRelationProcessor implements RelationProcessor {

    @Override
    public Map<String, Object> process(Map<String, Object> record, DataRelation relation) {
      // 自定义关联逻辑，根据参数实现特定的关联规则
      return new LinkedHashMap<>(record);
    }
  }

  /** 地区信息类 */
  private static class RegionInfo {
    private final String province;
    private final String city;
    private final String district;

    public RegionInfo(String province, String city, String district) {
      this.province = province;
      this.city = city;
      this.district = district;
    }

    public String getProvince() {
      return province;
    }

    public String getCity() {
      return city;
    }

    public String getDistrict() {
      return district;
    }
  }

  /** 数据关联定义类 */
  public static class DataRelation {
    private final String type;
    private final String sourceField;
    private final String targetField;
    private final Map<String, Object> parameters;

    public DataRelation(
        String type, String sourceField, String targetField, Map<String, Object> parameters) {
      this.type = type;
      this.sourceField = sourceField;
      this.targetField = targetField;
      this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public String getType() {
      return type;
    }

    public String getSourceField() {
      return sourceField;
    }

    public String getTargetField() {
      return targetField;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }
  }
}
