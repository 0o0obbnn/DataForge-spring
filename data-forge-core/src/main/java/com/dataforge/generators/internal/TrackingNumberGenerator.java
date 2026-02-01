package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 物流单号生成器
 *
 * <p>支持生成各种物流公司的运单号，用于物流系统、电商平台等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>carrier: 物流公司 (SF|JD|YTO|ZTO|STO|EMS|CUSTOM) 默认: CUSTOM
 *   <li>prefix: 自定义前缀
 *   <li>length: 单号长度 默认: 12
 *   <li>chars: 字符集 (ALPHANUMERIC|NUMERIC) 默认: NUMERIC
 *   <li>valid: 是否生成有效格式 默认: true
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class TrackingNumberGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(TrackingNumberGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 长度限制常量
  private static final int MIN_LENGTH = 6;
  private static final int MAX_LENGTH = 20;

  // 物流公司配置
  private static final Map<String, CarrierConfig> CARRIERS = new HashMap<>();

  static {
    CARRIERS.put("SF", new CarrierConfig("SF", 12, true));
    CARRIERS.put("JD", new CarrierConfig("JD", 15, false));
    CARRIERS.put("YTO", new CarrierConfig("YT", 10, false));
    CARRIERS.put("ZTO", new CarrierConfig("", 12, false));
    CARRIERS.put("STO", new CarrierConfig("", 12, false));
    CARRIERS.put("EMS", new CarrierConfig("E", 13, true));
    CARRIERS.put("CUSTOM", new CarrierConfig("", 12, false));
  }

  // 物流公司配置类
  private static class CarrierConfig {
    final String prefix;
    final int defaultLength;
    final boolean needsCheckDigit;

    CarrierConfig(String prefix, int defaultLength, boolean needsCheckDigit) {
      this.prefix = prefix;
      this.defaultLength = defaultLength;
      this.needsCheckDigit = needsCheckDigit;
    }
  }

  @Override
  public String getType() {
    return "tracking_number";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 参数验证
      String carrier = getStringParam(config, "carrier", "CUSTOM").toUpperCase();
      CarrierConfig carrierConfig = CARRIERS.get(carrier);
      if (carrierConfig == null) {
        logger.warn("Unknown carrier: {}, using CUSTOM", carrier);
        carrierConfig = CARRIERS.get("CUSTOM");
      }

      StringBuilder trackingNumber = new StringBuilder();

      // 前缀处理
      String prefix = getPrefix(config, carrierConfig);
      if (!prefix.isEmpty()) {
        trackingNumber.append(prefix);
      }

      // 生成主体部分
      int length = getIntParam(config, "length", carrierConfig.defaultLength);
      length = validateLength(length, prefix.length());

      String chars = getStringParam(config, "chars", "NUMERIC");
      String mainPart = generateMainPart(length, chars);
      trackingNumber.append(mainPart);

      // 校验位处理
      boolean valid = getBooleanParam(config, "valid", true);
      if (valid && carrierConfig.needsCheckDigit) {
        String checkDigit = calculateCheckDigit(trackingNumber.toString());
        trackingNumber.append(checkDigit);
      }

      return trackingNumber.toString();

    } catch (Exception e) {
      logger.error("Failed to generate tracking number", e);
      return "TN" + String.format("%010d", System.currentTimeMillis() % 10000000000L);
    }
  }

  private String getPrefix(FieldConfig config, CarrierConfig carrierConfig) {
    String customPrefix = getStringParam(config, "prefix", null);
    if (customPrefix != null) {
      return customPrefix;
    }
    return carrierConfig.prefix;
  }

  private int validateLength(int length, int prefixLength) {
    if (length < MIN_LENGTH || length > MAX_LENGTH) {
      logger.warn("Invalid length: {}, using default range", length);
      length = Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, length));
    }
    return Math.max(MIN_LENGTH - prefixLength, length - prefixLength);
  }

  private String generateMainPart(int length, String charsType) {
    if (length <= 0) return "";

    String chars;
    switch (charsType.toUpperCase()) {
      case "ALPHANUMERIC":
        chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        break;
      case "NUMERIC":
      default:
        chars = "0123456789";
        break;
    }

    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }

    return sb.toString();
  }

  private String calculateCheckDigit(String trackingNumber) {
    // 改进的校验位算法（加权模10）
    int sum = 0;
    int weight = 1;

    for (int i = trackingNumber.length() - 1; i >= 0; i--) {
      char c = trackingNumber.charAt(i);
      if (Character.isDigit(c)) {
        int digit = Character.getNumericValue(c) * weight;
        sum += digit > 9 ? digit - 9 : digit;
        weight = weight == 1 ? 2 : 1;
      }
    }

    return String.valueOf((10 - (sum % 10)) % 10);
  }
}
