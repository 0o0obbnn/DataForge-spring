package com.dataforge.io;

import com.dataforge.config.OutputConfig;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CSV输出策略实现
 *
 * <p>支持功能： 1. 流式写入CSV文件 2. 自定义分隔符 3. 可选的标题行 4. 字符编码支持 5. 追加模式 6. CSV注入防护
 *
 * @author DataForge
 * @since 1.0.0
 */
@Component
public class CsvOutputStrategy implements OutputStrategy {

  private static final Logger log = LoggerFactory.getLogger(CsvOutputStrategy.class);

  // 定义可能导致CSV注入的危险字符前缀
  private static final Set<Character> DANGEROUS_PREFIXES = new HashSet<>();

  static {
    DANGEROUS_PREFIXES.add('=');
    DANGEROUS_PREFIXES.add('+');
    DANGEROUS_PREFIXES.add('-');
    DANGEROUS_PREFIXES.add('@');
    DANGEROUS_PREFIXES.add('\t');
    DANGEROUS_PREFIXES.add('\r');
    DANGEROUS_PREFIXES.add('\'');
    DANGEROUS_PREFIXES.add('"');
    DANGEROUS_PREFIXES.add('`');
    DANGEROUS_PREFIXES.add('!');
    DANGEROUS_PREFIXES.add('^');
    DANGEROUS_PREFIXES.add('*');
    DANGEROUS_PREFIXES.add('(');
    DANGEROUS_PREFIXES.add(')');
    DANGEROUS_PREFIXES.add('{');
    DANGEROUS_PREFIXES.add('}');
    DANGEROUS_PREFIXES.add('[');
    DANGEROUS_PREFIXES.add(']');
    DANGEROUS_PREFIXES.add('|');
    DANGEROUS_PREFIXES.add('\\');
    DANGEROUS_PREFIXES.add('/');
    DANGEROUS_PREFIXES.add('?');
    DANGEROUS_PREFIXES.add('<');
    DANGEROUS_PREFIXES.add('>');
    DANGEROUS_PREFIXES.add('~');
  }

  private BufferedWriter writer;
  private String delimiter;
  private boolean includeHeader;
  private boolean headerWritten = false;
  private int recordCount = 0;
  private List<String> fieldNames; // 保存字段名顺序
  private boolean safeMode = false; // CSV安全模式

  @Override
  public OutputConfig.Format getSupportedFormat() {
    return OutputConfig.Format.CSV;
  }

  @Override
  public boolean supports(OutputConfig config) {
    if (config == null) {
      return false;
    }
    boolean hasFile = config.getFile() != null && !config.getFile().trim().isEmpty();
    // CSV策略仅在未配置文件（非文件场景，如控制台或其他上层流式）时由本策略处理
    return !hasFile && config.getFormat() == OutputConfig.Format.CSV;
  }

  @Override
  public void initialize(OutputConfig config, List<String> fieldNames) throws OutputException {
    try {
      // 保存字段名顺序
      this.fieldNames = fieldNames;

      // 解析配置
      this.delimiter = config.getCsvDelimiter() != null ? config.getCsvDelimiter() : ",";
      this.includeHeader = config.isCsvIncludeHeader();
      this.safeMode = config.isCsvSafeMode();

      // 创建文件写入器
      String encoding = config.getEncoding() != null ? config.getEncoding() : "UTF-8";
      Charset charset = Charset.forName(encoding);

      if (config.getFile() != null && !config.getFile().trim().isEmpty()) {
        this.writer =
            new BufferedWriter(new FileWriter(config.getFile(), charset, config.isAppend()));
        log.info(
            "CSV output initialized - file: {}, delimiter: '{}', header: {}, encoding: {}",
            config.getFile(),
            delimiter,
            includeHeader,
            encoding);
      } else {
        // 未配置文件时，使用空输出（丢弃写入），以便在非文件场景下不报错
        this.writer =
            new BufferedWriter(
                new OutputStreamWriter(java.io.OutputStream.nullOutputStream(), charset));
        log.debug(
            "CSV output initialized with null sink (no file configured). delimiter='{}', header={},"
                + " encoding={}",
            delimiter,
            includeHeader,
            encoding);
      }

    } catch (IOException e) {
      throw new OutputException("Failed to initialize CSV output: " + e.getMessage(), e);
    }
  }

  @Override
  public void writeRecord(Map<String, Object> record) throws OutputException {
    try {
      // 写入标题行（仅第一次）
      if (includeHeader && !headerWritten) {
        writeHeader();
        headerWritten = true;
      }

      // 写入数据行
      writeDataRow(record);
      recordCount++;

    } catch (IOException e) {
      throw new OutputException("Failed to write CSV record: " + e.getMessage(), e);
    }
  }

  @Override
  public void writeRecords(List<Map<String, Object>> records) throws OutputException {
    for (Map<String, Object> record : records) {
      writeRecord(record);
    }
  }

  @Override
  public void finish() throws OutputException {
    try {
      if (writer != null) {
        writer.flush();
        writer.close();
        log.info("CSV output completed. Total records: {}", recordCount);
      }
    } catch (IOException e) {
      throw new OutputException("Failed to close CSV output: " + e.getMessage(), e);
    }
  }

  /** 写入标题行（使用固定顺序） */
  private void writeHeader() throws IOException {
    boolean first = true;
    for (String fieldName : fieldNames) {
      if (!first) {
        writer.write(delimiter);
      }
      writer.write(escapeCsvField(fieldName));
      first = false;
    }
    writer.newLine();
  }

  /** 写入数据行（使用固定顺序） */
  private void writeDataRow(Map<String, Object> record) throws IOException {
    boolean first = true;
    for (String fieldName : fieldNames) {
      if (!first) {
        writer.write(delimiter);
      }

      Object value = record.get(fieldName);
      String stringValue = value != null ? value.toString() : "";
      writer.write(escapeCsvField(stringValue));
      first = false;
    }
    writer.newLine();
  }

  /**
   * 转义CSV字段
   *
   * <p>规则： 1. 如果字段包含分隔符、双引号或换行符，则用双引号包围 2. 字段内的双引号需要转义为两个双引号 3. 安全模式下，防止公式注入（=, +, -, @开头的值）
   */
  private String escapeCsvField(String field) {
    if (field == null) {
      return "";
    }

    // CSV安全模式：防止公式注入
    if (safeMode && isFormulaRisk(field)) {
      field = "'" + field; // 前置单引号
    }

    // 检查是否需要转义
    boolean needsEscaping =
        field.contains(delimiter)
            || field.contains("\"")
            || field.contains("\n")
            || field.contains("\r");

    if (needsEscaping) {
      // 转义双引号
      String escaped = field.replace("\"", "\"\"");
      // 用双引号包围
      return "\"" + escaped + "\"";
    }

    return field;
  }

  /** 检查是否存在公式注入风险 */
  private boolean isFormulaRisk(String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }

    char firstChar = value.charAt(0);
    return DANGEROUS_PREFIXES.contains(firstChar);
  }
}
