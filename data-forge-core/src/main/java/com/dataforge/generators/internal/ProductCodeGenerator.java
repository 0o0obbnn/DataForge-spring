package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 产品编码生成器
 *
 * <p>支持生成各种产品编码，如SKU、GTIN、ISBN、ISSN等， 用于商品管理、库存系统、电商平台等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 编码类型 (SKU|EAN13|UPCA|ISBN13|ISBN10|ISSN|CUSTOM) 默认: SKU
 *   <li>length: 编码长度（仅对SKU/CUSTOM有效）默认: 12
 *   <li>prefix: 自定义前缀 默认: ""
 *   <li>suffix: 自定义后缀 默认: ""
 *   <li>chars: 字符集 (ALPHANUMERIC|NUMERIC|ALPHA) 默认: ALPHANUMERIC
 *   <li>valid: 是否生成有效编码（包含校验位）默认: true
 *   <li>separator: 分隔符 默认: ""
 *   <li>category: 产品类别代码（用于SKU生成）
 *   <li>brand: 品牌代码（用于SKU生成）
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class ProductCodeGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(ProductCodeGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 编码类型枚举
  public enum CodeType {
    SKU("库存单位编码"),
    EAN13("欧洲商品编码13位"),
    UPCA("美国通用产品代码"),
    ISBN13("国际标准书号13位"),
    ISBN10("国际标准书号10位"),
    ISSN("国际标准期刊号"),
    CUSTOM("自定义编码");

    private final String description;

    CodeType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 字符集枚举
  public enum CharSet {
    ALPHANUMERIC("字母数字"),
    NUMERIC("纯数字"),
    ALPHA("纯字母");

    private final String description;

    CharSet(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @Override
  public String getType() {
    return "product_code";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取编码类型
      String typeStr = getStringParam(config, "type", "SKU");
      CodeType codeType = parseCodeType(typeStr);

      // 根据类型生成编码
      switch (codeType) {
        case SKU:
          return generateSKU(config);
        case EAN13:
          return generateEAN13(config);
        case UPCA:
          return generateUPCA(config);
        case ISBN13:
          return generateISBN13(config);
        case ISBN10:
          return generateISBN10(config);
        case ISSN:
          return generateISSN(config);
        case CUSTOM:
          return generateCustom(config);
        default:
          return generateSKU(config);
      }

    } catch (Exception e) {
      logger.error("Failed to generate product code", e);
      // 返回一个默认的产品编码作为fallback
      return "SKU" + System.currentTimeMillis();
    }
  }

  /** 解析编码类型 */
  private CodeType parseCodeType(String typeStr) {
    try {
      return CodeType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid code type: {}, using SKU as default", typeStr);
      return CodeType.SKU;
    }
  }

  /** 生成SKU编码 */
  private String generateSKU(FieldConfig config) {
    StringBuilder sku = new StringBuilder();

    // 品牌代码
    String brand = getStringParam(config, "brand", null);
    if (brand != null && !brand.isEmpty()) {
      sku.append(brand.toUpperCase());
    }

    // 类别代码
    String category = getStringParam(config, "category", null);
    if (category != null && !category.isEmpty()) {
      if (sku.length() > 0) {
        sku.append("-");
      }
      sku.append(category.toUpperCase());
    }

    // 随机部分
    int length = getIntParam(config, "length", 8);
    String randomPart = generateRandomString(length, CharSet.ALPHANUMERIC);

    if (sku.length() > 0) {
      sku.append("-");
    }
    sku.append(randomPart);

    return sku.toString();
  }

  /** 生成EAN-13编码 */
  private String generateEAN13(FieldConfig config) {
    boolean valid = getBooleanParam(config, "valid", true);

    // 生成前12位
    StringBuilder code = new StringBuilder();

    // 国家代码（前3位）
    String countryCode = String.format("%03d", random.nextInt(1000));
    code.append(countryCode);

    // 厂商代码（4-7位）
    String manufacturerCode = String.format("%04d", random.nextInt(10000));
    code.append(manufacturerCode);

    // 产品代码（8-12位）
    String productCode = String.format("%05d", random.nextInt(100000));
    code.append(productCode);

    if (valid) {
      // 计算校验位
      int checkDigit = calculateEANCheckDigit(code.toString());
      code.append(checkDigit);
    } else {
      // 生成错误的校验位
      code.append(random.nextInt(10));
    }

    return code.toString();
  }

  /** 生成UPC-A编码 */
  private String generateUPCA(FieldConfig config) {
    boolean valid = getBooleanParam(config, "valid", true);

    // 生成前11位
    StringBuilder code = new StringBuilder();

    // 系统位（第1位）
    code.append(random.nextInt(10));

    // 厂商代码（2-6位）
    String manufacturerCode = String.format("%05d", random.nextInt(100000));
    code.append(manufacturerCode);

    // 产品代码（7-11位）
    String productCode = String.format("%05d", random.nextInt(100000));
    code.append(productCode);

    if (valid) {
      // 计算校验位
      int checkDigit = calculateUPCCheckDigit(code.toString());
      code.append(checkDigit);
    } else {
      // 生成错误的校验位
      code.append(random.nextInt(10));
    }

    return code.toString();
  }

  /** 生成ISBN-13编码 */
  private String generateISBN13(FieldConfig config) {
    boolean valid = getBooleanParam(config, "valid", true);

    StringBuilder isbn = new StringBuilder();

    // 前缀（978或979）
    isbn.append(random.nextBoolean() ? "978" : "979");

    // 国家/语言代码（1位）
    isbn.append(random.nextInt(10));

    // 出版社代码（4位）
    String publisherCode = String.format("%04d", random.nextInt(10000));
    isbn.append(publisherCode);

    // 书籍代码（4位）
    String bookCode = String.format("%04d", random.nextInt(10000));
    isbn.append(bookCode);

    if (valid) {
      // 计算校验位
      int checkDigit = calculateISBN13CheckDigit(isbn.toString());
      isbn.append(checkDigit);
    } else {
      // 生成错误的校验位
      isbn.append(random.nextInt(10));
    }

    return isbn.toString();
  }

  /** 生成ISBN-10编码 */
  private String generateISBN10(FieldConfig config) {
    boolean valid = getBooleanParam(config, "valid", true);

    StringBuilder isbn = new StringBuilder();

    // 国家/语言代码（1位）
    isbn.append(random.nextInt(10));

    // 出版社代码（4位）
    String publisherCode = String.format("%04d", random.nextInt(10000));
    isbn.append(publisherCode);

    // 书籍代码（4位）
    String bookCode = String.format("%04d", random.nextInt(10000));
    isbn.append(bookCode);

    if (valid) {
      // 计算校验位
      String checkDigit = calculateISBN10CheckDigit(isbn.toString());
      isbn.append(checkDigit);
    } else {
      // 生成错误的校验位
      isbn.append(random.nextInt(10));
    }

    return isbn.toString();
  }

  /** 生成ISSN编码 */
  private String generateISSN(FieldConfig config) {
    boolean valid = getBooleanParam(config, "valid", true);

    StringBuilder issn = new StringBuilder();

    // 生成前7位数字
    for (int i = 0; i < 7; i++) {
      issn.append(random.nextInt(10));
    }

    if (valid) {
      // 计算校验位
      String checkDigit = calculateISSNCheckDigit(issn.toString());
      issn.append(checkDigit);
    } else {
      // 生成错误的校验位
      issn.append(random.nextInt(10));
    }

    // 添加连字符格式
    return issn.substring(0, 4) + "-" + issn.substring(4);
  }

  /** 生成自定义编码 */
  private String generateCustom(FieldConfig config) {
    int length = getIntParam(config, "length", 12);
    String charsStr = getStringParam(config, "chars", "ALPHANUMERIC");
    CharSet charSet = parseCharSet(charsStr);

    String prefix = getStringParam(config, "prefix", "");
    String suffix = getStringParam(config, "suffix", "");
    String separator = getStringParam(config, "separator", "");

    StringBuilder code = new StringBuilder();

    if (!prefix.isEmpty()) {
      code.append(prefix);
    }

    String randomPart = generateRandomString(length, charSet);
    if (code.length() > 0 && !separator.isEmpty()) {
      code.append(separator);
    }
    code.append(randomPart);

    if (!suffix.isEmpty()) {
      if (!separator.isEmpty()) {
        code.append(separator);
      }
      code.append(suffix);
    }

    return code.toString();
  }

  /** 解析字符集 */
  private CharSet parseCharSet(String charsStr) {
    try {
      return CharSet.valueOf(charsStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      return CharSet.ALPHANUMERIC;
    }
  }

  /** 生成随机字符串 */
  private String generateRandomString(int length, CharSet charSet) {
    String chars;
    switch (charSet) {
      case NUMERIC:
        chars = "0123456789";
        break;
      case ALPHA:
        chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        break;
      case ALPHANUMERIC:
      default:
        chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        break;
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
  }

  /** 计算EAN校验位 */
  private int calculateEANCheckDigit(String code) {
    int sum = 0;
    for (int i = 0; i < code.length(); i++) {
      int digit = Character.getNumericValue(code.charAt(i));
      sum += (i % 2 == 0) ? digit : digit * 3;
    }
    return (10 - (sum % 10)) % 10;
  }

  /** 计算UPC校验位 */
  private int calculateUPCCheckDigit(String code) {
    int sum = 0;
    for (int i = 0; i < code.length(); i++) {
      int digit = Character.getNumericValue(code.charAt(i));
      sum += (i % 2 == 0) ? digit * 3 : digit;
    }
    return (10 - (sum % 10)) % 10;
  }

  /** 计算ISBN-13校验位 */
  private int calculateISBN13CheckDigit(String code) {
    int sum = 0;
    for (int i = 0; i < code.length(); i++) {
      int digit = Character.getNumericValue(code.charAt(i));
      sum += (i % 2 == 0) ? digit : digit * 3;
    }
    return (10 - (sum % 10)) % 10;
  }

  /** 计算ISBN-10校验位 */
  private String calculateISBN10CheckDigit(String code) {
    int sum = 0;
    for (int i = 0; i < code.length(); i++) {
      int digit = Character.getNumericValue(code.charAt(i));
      sum += digit * (10 - i);
    }
    int remainder = sum % 11;
    return (remainder == 0) ? "0" : (remainder == 1) ? "X" : String.valueOf(11 - remainder);
  }

  /** 计算ISSN校验位 */
  private String calculateISSNCheckDigit(String code) {
    int sum = 0;
    for (int i = 0; i < code.length(); i++) {
      int digit = Character.getNumericValue(code.charAt(i));
      sum += digit * (8 - i);
    }
    int remainder = sum % 11;
    return (remainder == 0) ? "0" : (remainder == 1) ? "X" : String.valueOf(11 - remainder);
  }
}
