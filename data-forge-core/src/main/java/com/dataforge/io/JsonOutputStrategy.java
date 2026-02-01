package com.dataforge.io;

import com.dataforge.config.OutputConfig;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** JSON输出策略（非文件场景） 使用 Jackson Streaming API 实现数组流式输出 */
@Component
public class JsonOutputStrategy implements OutputStrategy {

  private static final Logger log = LoggerFactory.getLogger(JsonOutputStrategy.class);

  private List<String> fieldNames;
  private JsonFactory jsonFactory;
  private JsonGenerator generator;
  private Writer writer;
  private long recordCount = 0;

  @Override
  public OutputConfig.Format getSupportedFormat() {
    return OutputConfig.Format.JSON;
  }

  @Override
  public boolean supports(OutputConfig config) {
    if (config == null) return false;
    boolean hasFile = config.getFile() != null && !config.getFile().trim().isEmpty();
    return !hasFile && config.getFormat() == OutputConfig.Format.JSON;
  }

  @Override
  public void initialize(OutputConfig config, List<String> fieldNames) throws OutputException {
    try {
      this.fieldNames = fieldNames;
      this.jsonFactory = new JsonFactory();
      String encoding = config.getEncoding() != null ? config.getEncoding() : "UTF-8";
      Charset charset = Charset.forName(encoding);
      // 非文件场景：写入到一个丢弃输出（与 CSV 非文件策略一致）
      this.writer = new OutputStreamWriter(java.io.OutputStream.nullOutputStream(), charset);
      this.generator = jsonFactory.createGenerator(writer);
      this.generator.writeStartArray();
    } catch (IOException e) {
      throw new OutputException("Failed to initialize JSON output", e);
    }
  }

  @Override
  public void writeRecord(Map<String, Object> record) throws OutputException {
    try {
      generator.writeStartObject();
      for (String fieldName : fieldNames) {
        Object value = record.get(fieldName);
        if (value == null) {
          generator.writeNullField(fieldName);
        } else if (value instanceof Number n) {
          // 避免精度问题，不强制 double；使用 writeNumber(primitive) 更安全
          if (n instanceof Integer i) generator.writeNumberField(fieldName, i);
          else if (n instanceof Long l) generator.writeNumberField(fieldName, l);
          else if (n instanceof Float f) generator.writeNumberField(fieldName, f);
          else if (n instanceof Double d) generator.writeNumberField(fieldName, d);
          else generator.writeStringField(fieldName, n.toString());
        } else if (value instanceof Boolean b) {
          generator.writeBooleanField(fieldName, b);
        } else {
          generator.writeStringField(fieldName, String.valueOf(value));
        }
      }
      generator.writeEndObject();
      recordCount++;
    } catch (IOException e) {
      throw new OutputException("Failed to write JSON record", e);
    }
  }

  @Override
  public void finish() throws OutputException {
    try {
      if (generator != null) {
        generator.writeEndArray();
        generator.flush();
        generator.close();
      }
      if (writer != null) {
        writer.flush();
        writer.close();
      }
      log.info("JSON output completed. Total records: {}", recordCount);
    } catch (IOException e) {
      throw new OutputException("Failed to finish JSON output", e);
    }
  }

  @Override
  public void flush() throws OutputException {
    try {
      if (generator != null) generator.flush();
      if (writer != null) writer.flush();
    } catch (IOException e) {
      throw new OutputException("Failed to flush JSON output", e);
    }
  }

  @Override
  public long getWrittenRecordCount() {
    return recordCount;
  }

  @Override
  public String getDescription() {
    return "JSON output strategy (non-file) - streams records as a JSON array";
  }
}
