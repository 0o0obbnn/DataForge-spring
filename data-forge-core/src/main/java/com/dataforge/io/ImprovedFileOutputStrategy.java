package com.dataforge.io;

import com.dataforge.config.OutputConfig;
import com.dataforge.io.format.OutputFormat;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 改进的文件输出策略 使用Format+Sink模式，职责清晰 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ImprovedFileOutputStrategy implements OutputStrategy {

  private BufferedWriter writer;
  private OutputFormat format;
  private List<String> fieldNames;
  private boolean initialized = false;

  @Override
  public boolean supports(OutputConfig config) {
    if (config == null) return false;
    boolean hasFile = config.getFile() != null && !config.getFile().trim().isEmpty();
    if (!hasFile) return false;
    // 仅在文件输出且格式为CSV或JSON时生效，避免误选 CONSOLE 等
    return config.getFormat() == OutputConfig.Format.CSV
        || config.getFormat() == OutputConfig.Format.JSON;
  }

  @Override
  public void initialize(OutputConfig config, List<String> fieldNames) throws OutputException {
    try {
      this.fieldNames = fieldNames;

      // 创建文件写入器，支持编码和追加模式
      String encoding = config.getEncoding() != null ? config.getEncoding() : "UTF-8";
      Charset charset = Charset.forName(encoding);

      this.writer =
          new BufferedWriter(new FileWriter(config.getFile(), charset, config.isAppend()));

      // 根据格式选择对应的Format实现
      this.format = selectFormat(config.getFormat());

      // 写入格式头部
      format.start(fieldNames, writer);

      this.initialized = true;

    } catch (IOException e) {
      throw new OutputException("无法创建输出文件: " + config.getFile(), e);
    }
  }

  @Override
  public void writeRecord(Map<String, Object> record) throws OutputException {
    if (!initialized) {
      throw new OutputException("输出策略未初始化");
    }

    try {
      format.writeRecord(record, fieldNames, writer);
    } catch (IOException e) {
      throw new OutputException("写入记录失败", e);
    }
  }

  @Override
  public void writeRecords(List<Map<String, Object>> records) throws OutputException {
    if (!initialized) {
      throw new OutputException("输出策略未初始化");
    }

    try {
      format.writeRecords(records, fieldNames, writer);
    } catch (IOException e) {
      throw new OutputException("批量写入记录失败", e);
    }
  }

  @Override
  public void flush() throws OutputException {
    if (writer != null) {
      try {
        writer.flush();
      } catch (IOException e) {
        throw new OutputException("刷新输出失败", e);
      }
    }
  }

  @Override
  public void finish() throws OutputException {
    try {
      if (writer != null && format != null) {
        format.end(writer);
        writer.close();
      }
    } catch (IOException e) {
      throw new OutputException("完成输出失败", e);
    }
  }

  @Override
  public OutputConfig.Format getSupportedFormat() {
    return format != null ? format.getFormat() : OutputConfig.Format.CSV;
  }

  /** 根据配置选择格式实现 */
  private OutputFormat selectFormat(OutputConfig.Format formatType) throws OutputException {
    // 这里可以通过Spring容器注入或工厂模式获取Format实现
    // 简化实现，直接创建
    switch (formatType) {
      case CSV:
        return new com.dataforge.io.format.CsvFormat();
      case JSON:
        return new com.dataforge.io.format.JsonFormat();
      default:
        throw new OutputException("不支持的输出格式: " + formatType);
    }
  }
}
