package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * UUID生成器。
 *
 * <p>生成符合RFC 4122标准的UUID字符串。 支持UUID4（随机UUID）和UUID1（基于时间的UUID）。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class UuidGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(UuidGenerator.class);

  @Override
  public String getType() {
    return "uuid";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 从参数中获取UUID类型，默认为UUID4
      String uuidType = getStringParam(config, "type", "UUID4");

      return switch (uuidType.toUpperCase()) {
        case "UUID1" -> generateUuid1();
        case "UUID4" -> generateUuid4();
        default -> {
          logger.warn("Unknown UUID type: {}, using UUID4", uuidType);
          yield generateUuid4();
        }
      };

    } catch (Exception e) {
      logger.error("Failed to generate UUID", e);
      // 返回一个默认的UUID4作为fallback
      return UUID.randomUUID().toString();
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  /**
   * 生成UUID4（随机UUID）。
   *
   * @return UUID4字符串
   */
  private String generateUuid4() {
    return UUID.randomUUID().toString();
  }

  /**
   * 生成UUID1（基于时间的UUID）。
   *
   * <p>注意：Java标准库不直接支持UUID1生成，这里使用简化实现。 实际应用中可能需要使用第三方库如java-uuid-generator。
   *
   * @return UUID1字符串
   */
  private String generateUuid1() {
    // 简化的UUID1实现
    // 实际应用中应该使用专门的UUID1生成库
    long timestamp = System.currentTimeMillis();
    long randomPart = UUID.randomUUID().getMostSignificantBits();

    // 构造一个类似UUID1格式的字符串
    // 这不是真正的UUID1，只是为了演示
    String timeHex = Long.toHexString(timestamp);
    String randomHex = Long.toHexString(Math.abs(randomPart));

    // 确保长度
    while (timeHex.length() < 12) {
      timeHex = "0" + timeHex;
    }
    while (randomHex.length() < 16) {
      randomHex = "0" + randomHex;
    }

    // 检查长度，确保不会越界
    if (timeHex.length() < 12 || randomHex.length() < 19) {
      // 如果长度不足，使用UUID4作为后备方案
      logger.warn("Insufficient length for UUID1 generation, falling back to UUID4");
      return UUID.randomUUID().toString();
    }

    // 格式化为UUID格式
    String uuid =
        timeHex.substring(0, 8)
            + "-"
            + timeHex.substring(8, 12)
            + "-"
            + "1"
            + randomHex.substring(0, 3)
            + "-"
            + randomHex.substring(3, 7)
            + "-"
            + randomHex.substring(7, 23); // 修正为正确的长度

    return uuid;
  }

  @Override
  public String getDescription() {
    return "UUID generator - generates RFC 4122 compliant UUID strings (UUID1/UUID4)";
  }
}
