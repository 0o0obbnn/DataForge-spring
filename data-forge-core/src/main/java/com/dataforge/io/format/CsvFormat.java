package com.dataforge.io.format;

import com.dataforge.config.OutputConfig;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** CSV格式输出实现 */
@Component
public class CsvFormat implements OutputFormat {

  private boolean safeMode = false;
  private String delimiter = ",";

  @Override
  public OutputConfig.Format getFormat() {
    return OutputConfig.Format.CSV;
  }

  @Override
  public void start(List<String> fieldNames, Appendable out) throws IOException {
    // 写入CSV头部
    for (int i = 0; i < fieldNames.size(); i++) {
      if (i > 0) {
        out.append(delimiter);
      }
      out.append(escapeCsvField(fieldNames.get(i)));
    }
    out.append("\n");
  }

  @Override
  public void writeRecord(Map<String, Object> record, List<String> fieldNames, Appendable out)
      throws IOException {
    for (int i = 0; i < fieldNames.size(); i++) {
      if (i > 0) {
        out.append(delimiter);
      }
      Object value = record.get(fieldNames.get(i));
      String stringValue = value != null ? value.toString() : "";
      out.append(escapeCsvField(stringValue));
    }
    out.append("\n");
  }

  @Override
  public void end(Appendable out) throws IOException {
    // CSV无需特殊结尾
  }

  /** 转义CSV字段 */
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
    return firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@';
  }

  public void setSafeMode(boolean safeMode) {
    this.safeMode = safeMode;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }
}
