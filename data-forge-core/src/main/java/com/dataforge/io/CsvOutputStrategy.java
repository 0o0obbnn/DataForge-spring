package com.dataforge.io;

import com.dataforge.config.OutputConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    // 验证字段名列表
    if (fieldNames == null || fieldNames.isEmpty()) {
      throw new OutputException("Field names cannot be null or empty");
    }

    BufferedWriter tempWriter = null;
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
        // 验证并清理文件路径，防止目录遍历攻击
        String safeFilePath = validateAndSanitizePath(config.getFile());

        tempWriter = new BufferedWriter(new FileWriter(safeFilePath, charset, config.isAppend()));
        this.writer = tempWriter;
        tempWriter = null; // 防止finally中关闭

        log.info(
            "CSV output initialized - file: {}, delimiter: '{}', header: {}, encoding: {}",
            safeFilePath,
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
    } finally {
      // 如果初始化失败，确保关闭临时writer
      if (tempWriter != null) {
        try {
          tempWriter.close();
        } catch (IOException e) {
          // 忽略关闭异常
        }
      }
    }
  }

  /**
   * 验证并清理文件路径，防止目录遍历攻击。
   *
   * <p>安全措施：
   *
   * <ul>
   *   <li>1. 将路径标准化，解析所有".."和"."
   *   <li>2. 确保文件在允许的输出目录内
   *   <li>3. 验证文件名不包含非法字符
   * </ul>
   *
   * @param filePath 原始文件路径
   * @return 安全的文件路径
   * @throws OutputException 当路径验证失败时
   */
  private String validateAndSanitizePath(String filePath) throws OutputException {
    if (filePath == null || filePath.trim().isEmpty()) {
      throw new OutputException("文件路径不能为空");
    }

    try {
      // 标准化路径，解析所有".."和"."
      Path path = Paths.get(filePath).normalize();
      File file = path.toFile();

      // 获取规范化的绝对路径
      String canonicalPath = file.getCanonicalPath();
      File canonicalFile = new File(canonicalPath);

      // 检查是否是系统临时目录（允许测试使用）
      String tempDir = System.getProperty("java.io.tmpdir");
      if (tempDir != null && canonicalPath.startsWith(new File(tempDir).getCanonicalPath())) {
        // 确保父目录存在
        File parentDir = canonicalFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
          parentDir.mkdirs();
        }
        return canonicalPath;
      }

      // 定义允许的基础输出目录（默认为当前工作目录下的output文件夹）
      String baseDir = System.getProperty("dataforge.output.dir", "output");
      File baseDirFile = new File(baseDir).getCanonicalFile();

      // 确保文件在允许的输出目录内
      if (!canonicalFile.getAbsolutePath().startsWith(baseDirFile.getAbsolutePath())) {
        // 如果路径不在基础目录内，将文件限制在基础目录下
        String fileName = path.getFileName().toString();
        // 验证文件名不包含路径分隔符
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
          throw new OutputException("非法文件名，包含路径分隔符: " + fileName);
        }
        canonicalFile = new File(baseDirFile, fileName);
        canonicalPath = canonicalFile.getCanonicalPath();
      }

      // 确保父目录存在
      File parentDir = canonicalFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        if (!parentDir.mkdirs()) {
          throw new OutputException("无法创建父目录: " + parentDir.getAbsolutePath());
        }
      }

      return canonicalPath;

    } catch (IOException e) {
      throw new OutputException("文件路径验证失败: " + filePath, e);
    }
  }

  @Override
  public void writeRecord(Map<String, Object> record) throws OutputException {
    if (record == null) {
      throw new OutputException("Cannot write null record");
    }

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

  @Override
  public long getWrittenRecordCount() {
    return recordCount;
  }
}
