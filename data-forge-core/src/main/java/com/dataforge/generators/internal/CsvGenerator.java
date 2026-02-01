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
 * CSV内容生成器
 *
 * <p>用于生成各种格式的CSV内容，适用于数据导入测试、报表生成等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>rows: 行数 (1-1000) 默认: 10
 *   <li>columns: 列数 (1-50) 默认: 5
 *   <li>header: 是否包含表头 默认: true
 *   <li>delimiter: 分隔符 (COMMA|SEMICOLON|TAB|PIPE) 默认: COMMA
 *   <li>quote: 引号字符 (NONE|SINGLE|DOUBLE) 默认: DOUBLE
 *   <li>escape: 转义字符 (NONE|BACKSLASH|DOUBLE) 默认: DOUBLE
 *   <li>data_types: 数据类型 (STRING|NUMBER|DATE|BOOLEAN|ALL) 默认: ALL
 *   <li>include_empty: 是否包含空值 默认: false
 *   <li>invalid_csv: 是否生成无效CSV用于测试 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class CsvGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(CsvGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 分隔符类型
  public enum DelimiterType {
    COMMA(","),
    SEMICOLON(";"),
    TAB("\t"),
    PIPE("|");

    private final String delimiter;

    DelimiterType(String delimiter) {
      this.delimiter = delimiter;
    }

    public String getDelimiter() {
      return delimiter;
    }
  }

  // 引号类型
  public enum QuoteType {
    NONE(""),
    SINGLE("'"),
    DOUBLE("\"");

    private final String quote;

    QuoteType(String quote) {
      this.quote = quote;
    }

    public String getQuote() {
      return quote;
    }
  }

  // 转义类型
  public enum EscapeType {
    NONE(""),
    BACKSLASH("\\"),
    DOUBLE("\"");

    private final String escape;

    EscapeType(String escape) {
      this.escape = escape;
    }

    public String getEscape() {
      return escape;
    }
  }

  // 数据类型
  public enum DataType {
    STRING,
    NUMBER,
    DATE,
    BOOLEAN
  }

  @Override
  public String getType() {
    return "csv";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      int rows = getIntParam(config, "rows", 10);
      int columns = getIntParam(config, "columns", 5);
      boolean header = getBooleanParam(config, "header", true);
      String delimiterParam = getStringParam(config, "delimiter", "COMMA");
      String quoteParam = getStringParam(config, "quote", "DOUBLE");
      String escapeParam = getStringParam(config, "escape", "DOUBLE");
      String dataTypesParam = getStringParam(config, "data_types", "ALL");
      boolean includeEmpty = getBooleanParam(config, "include_empty", false);
      boolean invalidCsv = getBooleanParam(config, "invalid_csv", false);

      // 限制参数范围
      rows = Math.max(1, Math.min(1000, rows));
      columns = Math.max(1, Math.min(50, columns));

      // 解析参数
      DelimiterType delimiter = parseDelimiter(delimiterParam);
      QuoteType quote = parseQuote(quoteParam);
      EscapeType escape = parseEscape(escapeParam);
      Set<DataType> dataTypes = parseDataTypes(dataTypesParam);

      StringBuilder csv = new StringBuilder();

      // 生成表头
      if (header) {
        generateHeader(csv, columns, delimiter.getDelimiter(), quote.getQuote());
        if (invalidCsv) {
          // 生成无效CSV - 表头行缺少一个字段
          csv.append("\n"); // 添加空行而不是完整的表头
        }
      }

      // 生成数据行
      for (int i = 0; i < rows; i++) {
        if (i > 0 || header) {
          csv.append("\n");
        }
        generateRow(
            csv,
            columns,
            delimiter.getDelimiter(),
            quote.getQuote(),
            escape.getEscape(),
            dataTypes,
            includeEmpty,
            invalidCsv && i == rows - 1);
      }

      return csv.toString();
    } catch (Exception e) {
      logger.error("Error generating CSV content", e);
      return "Error generating CSV content";
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String getDescription() {
    return "CSV内容生成器，用于生成各种格式的CSV数据";
  }

  /** 解析分隔符参数 */
  private DelimiterType parseDelimiter(String delimiterParam) {
    try {
      return DelimiterType.valueOf(delimiterParam.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid delimiter type: {}, using COMMA", delimiterParam);
      return DelimiterType.COMMA;
    }
  }

  /** 解析引号参数 */
  private QuoteType parseQuote(String quoteParam) {
    try {
      return QuoteType.valueOf(quoteParam.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid quote type: {}, using DOUBLE", quoteParam);
      return QuoteType.DOUBLE;
    }
  }

  /** 解析转义参数 */
  private EscapeType parseEscape(String escapeParam) {
    try {
      return EscapeType.valueOf(escapeParam.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid escape type: {}, using DOUBLE", escapeParam);
      return EscapeType.DOUBLE;
    }
  }

  /** 解析数据类型参数 */
  private Set<DataType> parseDataTypes(String dataTypesParam) {
    Set<DataType> dataTypes = new HashSet<>();

    if ("ALL".equalsIgnoreCase(dataTypesParam)) {
      dataTypes.addAll(Arrays.asList(DataType.values()));
    } else {
      String[] types = dataTypesParam.split(",");
      for (String type : types) {
        try {
          dataTypes.add(DataType.valueOf(type.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
          logger.warn("Invalid data type: {}, skipping", type);
        }
      }
    }

    // 如果没有有效的类型，默认使用所有类型
    if (dataTypes.isEmpty()) {
      dataTypes.addAll(Arrays.asList(DataType.values()));
    }

    return dataTypes;
  }

  /** 生成表头 */
  private void generateHeader(StringBuilder csv, int columns, String delimiter, String quote) {
    for (int i = 0; i < columns; i++) {
      if (i > 0) {
        csv.append(delimiter);
      }
      csv.append(quote).append("Column").append(i + 1).append(quote);
    }
  }

  /** 生成数据行 */
  private void generateRow(
      StringBuilder csv,
      int columns,
      String delimiter,
      String quote,
      String escape,
      Set<DataType> dataTypes,
      boolean includeEmpty,
      boolean invalid) {
    DataType[] types = dataTypes.toArray(new DataType[0]);

    for (int i = 0; i < columns; i++) {
      if (i > 0) {
        csv.append(delimiter);
      }

      // 可能生成空值
      if (includeEmpty && random.nextDouble() < 0.1) { // 10%概率为空
        csv.append("");
        continue;
      }

      // 生成不同类型的数据
      DataType type = types[random.nextInt(types.length)];
      String value = generateValue(type);

      // 处理特殊字符和引号
      if (invalid && i == columns - 1) {
        // 生成无效CSV - 缺少闭合引号
        csv.append(quote).append(value);
      } else if (needsQuoting(value, delimiter, quote)) {
        csv.append(quote).append(escapeValue(value, quote, escape)).append(quote);
      } else {
        csv.append(value);
      }
    }
  }

  /** 生成值 */
  private String generateValue(DataType type) {
    switch (type) {
      case STRING:
        return generateRandomString();
      case NUMBER:
        return String.valueOf(random.nextInt(10000));
      case DATE:
        return "2023-"
            + String.format("%02d", random.nextInt(12) + 1)
            + "-"
            + String.format("%02d", random.nextInt(28) + 1);
      case BOOLEAN:
        return random.nextBoolean() ? "true" : "false";
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

  /** 检查是否需要引号 */
  private boolean needsQuoting(String value, String delimiter, String quote) {
    return value.contains(delimiter)
        || value.contains(quote)
        || value.contains("\n")
        || value.contains("\r");
  }

  /** 转义值 */
  private String escapeValue(String value, String quote, String escape) {
    if (escape.isEmpty()) {
      return value;
    }
    return value.replace(quote, escape + quote);
  }
}
