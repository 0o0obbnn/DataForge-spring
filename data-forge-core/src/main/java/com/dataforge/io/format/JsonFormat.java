package com.dataforge.io.format;

import com.dataforge.config.OutputConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * JSON格式输出实现。
 *
 * <p>使用AtomicBoolean确保firstRecord状态的线程安全。
 */
@Component
public class JsonFormat implements OutputFormat {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AtomicBoolean firstRecord = new AtomicBoolean(true);

  @Override
  public OutputConfig.Format getFormat() {
    return OutputConfig.Format.JSON;
  }

  @Override
  public void start(List<String> fieldNames, Appendable out) throws IOException {
    out.append("[");
    firstRecord.set(true);
  }

  @Override
  public void writeRecord(Map<String, Object> record, List<String> fieldNames, Appendable out)
      throws IOException {
    if (!firstRecord.get()) {
      out.append(",");
    }

    // 保持字段顺序
    Map<String, Object> orderedRecord = new LinkedHashMap<>();
    for (String fieldName : fieldNames) {
      orderedRecord.put(fieldName, record.get(fieldName));
    }

    String json = objectMapper.writeValueAsString(orderedRecord);
    out.append(json);

    firstRecord.set(false);
  }

  @Override
  public void end(Appendable out) throws IOException {
    out.append("]");
  }
}
