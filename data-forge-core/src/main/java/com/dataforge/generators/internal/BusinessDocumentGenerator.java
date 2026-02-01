package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业务单据号生成器
 *
 * <p>支持生成各种业务单据号，如订单号、发票号、合同号等， 用于业务系统测试、订单管理、财务系统等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 单据类型 (ORDER|INVOICE|CONTRACT|RECEIPT|CUSTOM) 默认: ORDER
 *   <li>prefix: 自定义前缀 默认: 根据type自动生成
 *   <li>date_format: 日期格式 (yyyyMMdd|yyMM|yyyy|NONE) 默认: yyyyMMdd
 *   <li>sequence_length: 序列号长度 默认: 6
 *   <li>sequence_start: 序列号起始值 默认: 1
 *   <li>sequence_mode: 序列号模式 (INCREMENTAL|RANDOM) 默认: INCREMENTAL
 *   <li>separator: 分隔符 默认: ""
 *   <li>checksum: 是否添加校验位 默认: false
 *   <li>template: 自定义模板 格式: {PREFIX}-{DATE}-{SEQ}
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class BusinessDocumentGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(BusinessDocumentGenerator.class);
  private static final SecureRandom random = new SecureRandom();
  private static final AtomicLong sequenceCounter = new AtomicLong(1);

  // 单据类型枚举
  public enum DocumentType {
    ORDER("ORD", "订单号"),
    INVOICE("INV", "发票号"),
    CONTRACT("CON", "合同号"),
    RECEIPT("REC", "收据号"),
    PAYMENT("PAY", "支付单号"),
    REFUND("REF", "退款单号"),
    SHIPMENT("SHP", "发货单号"),
    RETURN("RET", "退货单号"),
    CUSTOM("", "自定义");

    private final String defaultPrefix;
    private final String description;

    DocumentType(String defaultPrefix, String description) {
      this.defaultPrefix = defaultPrefix;
      this.description = description;
    }

    public String getDefaultPrefix() {
      return defaultPrefix;
    }

    public String getDescription() {
      return description;
    }
  }

  // 序列号模式枚举
  public enum SequenceMode {
    INCREMENTAL("递增序列"),
    RANDOM("随机序列");

    private final String description;

    SequenceMode(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @Override
  public String getType() {
    return "business_document";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取单据类型
      String typeStr = getStringParam(config, "type", "ORDER");
      DocumentType docType = parseDocumentType(typeStr);

      // 获取模板
      String template = getStringParam(config, "template", null);
      if (template != null) {
        return generateFromTemplate(template, config, docType);
      }

      // 标准格式生成
      return generateStandardFormat(config, docType);

    } catch (Exception e) {
      logger.error("Failed to generate business document number", e);
      // 返回一个默认的单据号作为fallback
      return "ORD" + System.currentTimeMillis();
    }
  }

  /** 解析单据类型 */
  private DocumentType parseDocumentType(String typeStr) {
    try {
      return DocumentType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid document type: {}, using ORDER as default", typeStr);
      return DocumentType.ORDER;
    }
  }

  /** 从模板生成单据号 */
  private String generateFromTemplate(String template, FieldConfig config, DocumentType docType) {
    String result = template;

    // 替换前缀
    String prefix = getPrefix(config, docType);
    result = result.replace("{PREFIX}", prefix);

    // 替换日期
    String dateStr = getDateString(config);
    result = result.replace("{DATE}", dateStr);

    // 替换序列号
    String sequence = getSequenceString(config);
    result = result.replace("{SEQ}", sequence);

    // 替换随机数
    result = result.replace("{RAND:4}", String.format("%04d", random.nextInt(10000)));
    result = result.replace("{RAND:6}", String.format("%06d", random.nextInt(1000000)));

    return result;
  }

  /** 生成标准格式单据号 */
  private String generateStandardFormat(FieldConfig config, DocumentType docType) {
    StringBuilder sb = new StringBuilder();

    // 前缀
    String prefix = getPrefix(config, docType);
    if (!prefix.isEmpty()) {
      sb.append(prefix);
    }

    // 分隔符
    String separator = getStringParam(config, "separator", "");

    // 日期
    String dateStr = getDateString(config);
    if (!dateStr.isEmpty()) {
      if (sb.length() > 0 && !separator.isEmpty()) {
        sb.append(separator);
      }
      sb.append(dateStr);
    }

    // 序列号
    String sequence = getSequenceString(config);
    if (!sequence.isEmpty()) {
      if (sb.length() > 0 && !separator.isEmpty()) {
        sb.append(separator);
      }
      sb.append(sequence);
    }

    // 校验位
    boolean addChecksum = getBooleanParam(config, "checksum", false);
    if (addChecksum) {
      String checksum = calculateChecksum(sb.toString());
      if (!separator.isEmpty()) {
        sb.append(separator);
      }
      sb.append(checksum);
    }

    return sb.toString();
  }

  /** 获取前缀 */
  private String getPrefix(FieldConfig config, DocumentType docType) {
    String customPrefix = getStringParam(config, "prefix", null);
    if (customPrefix != null) {
      return customPrefix;
    }
    return docType.getDefaultPrefix();
  }

  /** 获取日期字符串 */
  private String getDateString(FieldConfig config) {
    String dateFormat = getStringParam(config, "date_format", "yyyyMMdd");

    if ("NONE".equalsIgnoreCase(dateFormat)) {
      return "";
    }

    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
      return LocalDate.now().format(formatter);
    } catch (Exception e) {
      logger.warn("Invalid date format: {}, using yyyyMMdd as default", dateFormat);
      return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
  }

  /** 获取序列号字符串 */
  private String getSequenceString(FieldConfig config) {
    int sequenceLength = getIntParam(config, "sequence_length", 6);
    long sequenceStart = getLongParam(config, "sequence_start", 1L);
    String sequenceModeStr = getStringParam(config, "sequence_mode", "INCREMENTAL");

    SequenceMode mode;
    try {
      mode = SequenceMode.valueOf(sequenceModeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      mode = SequenceMode.INCREMENTAL;
    }

    long sequenceValue;
    switch (mode) {
      case INCREMENTAL:
        sequenceValue = sequenceCounter.getAndIncrement() + sequenceStart - 1;
        break;
      case RANDOM:
        long maxValue = (long) Math.pow(10, sequenceLength) - 1;
        sequenceValue = sequenceStart + random.nextLong(maxValue - sequenceStart + 1);
        break;
      default:
        sequenceValue = sequenceStart;
        break;
    }

    // 格式化为指定长度
    String format = "%0" + sequenceLength + "d";
    return String.format(format, sequenceValue);
  }

  /** 计算校验位（简单的模10算法） */
  private String calculateChecksum(String input) {
    int sum = 0;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (Character.isDigit(c)) {
        sum += Character.getNumericValue(c);
      } else {
        // 对于非数字字符，使用其ASCII值
        sum += (int) c;
      }
    }
    return String.valueOf(sum % 10);
  }
}
