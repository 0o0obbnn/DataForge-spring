package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 优惠券码生成器
 *
 * <p>支持生成各种格式的优惠券码和促销码，用于电商系统、营销活动等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>length: 优惠券码长度 默认: 8
 *   <li>chars: 字符集 (ALPHANUMERIC|ALPHA|NUMERIC|CUSTOM) 默认: ALPHANUMERIC
 *   <li>custom_chars: 自定义字符集
 *   <li>prefix: 前缀 默认: ""
 *   <li>suffix: 后缀 默认: ""
 *   <li>separator: 分隔符 默认: ""
 *   <li>exclude_chars: 排除字符 (如0,O,1,I) 默认: "0O1I"
 *   <li>include_date: 是否包含日期 默认: false
 *   <li>date_format: 日期格式 默认: "yyMM"
 *   <li>batch_id: 批次ID
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class CouponCodeGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(CouponCodeGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 字符集定义
  private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String NUMERIC = "0123456789";

  @Override
  public String getType() {
    return "coupon_code";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      StringBuilder coupon = new StringBuilder();

      // 前缀
      String prefix = getStringParam(config, "prefix", "");
      if (!prefix.isEmpty()) {
        coupon.append(prefix);
      }

      // 批次ID
      String batchId = getStringParam(config, "batch_id", null);
      if (batchId != null && !batchId.isEmpty()) {
        if (coupon.length() > 0) {
          coupon.append(getSeparator(config));
        }
        coupon.append(batchId);
      }

      // 日期
      boolean includeDate = getBooleanParam(config, "include_date", false);
      if (includeDate) {
        if (coupon.length() > 0) {
          coupon.append(getSeparator(config));
        }
        coupon.append(generateDatePart(config));
      }

      // 主要随机部分
      if (coupon.length() > 0) {
        coupon.append(getSeparator(config));
      }
      coupon.append(generateRandomPart(config));

      // 后缀
      String suffix = getStringParam(config, "suffix", "");
      if (!suffix.isEmpty()) {
        coupon.append(getSeparator(config));
        coupon.append(suffix);
      }

      return coupon.toString();

    } catch (Exception e) {
      logger.error("Failed to generate coupon code", e);
      return "COUPON" + System.currentTimeMillis() % 100000;
    }
  }

  private String getSeparator(FieldConfig config) {
    return getStringParam(config, "separator", "");
  }

  private String generateDatePart(FieldConfig config) {
    String dateFormat = getStringParam(config, "date_format", "yyMM");
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
      return LocalDate.now().format(formatter);
    } catch (Exception e) {
      logger.warn("Invalid date format: {}, using yyMM as default", dateFormat);
      return LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
    }
  }

  private String generateRandomPart(FieldConfig config) {
    int length = getIntParam(config, "length", 8);
    String chars = getCharacterSet(config);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }

    return sb.toString();
  }

  private String getCharacterSet(FieldConfig config) {
    String charsType = getStringParam(config, "chars", "ALPHANUMERIC");
    String excludeChars = getStringParam(config, "exclude_chars", "0O1I");

    String baseChars;
    switch (charsType.toUpperCase()) {
      case "ALPHA":
        baseChars = ALPHA;
        break;
      case "NUMERIC":
        baseChars = NUMERIC;
        break;
      case "CUSTOM":
        baseChars = getStringParam(config, "custom_chars", ALPHANUMERIC);
        break;
      case "ALPHANUMERIC":
      default:
        baseChars = ALPHANUMERIC;
        break;
    }

    // 排除指定字符
    if (excludeChars != null && !excludeChars.isEmpty()) {
      StringBuilder filtered = new StringBuilder();
      for (char c : baseChars.toCharArray()) {
        if (excludeChars.indexOf(c) == -1) {
          filtered.append(c);
        }
      }
      return filtered.toString();
    }

    return baseChars;
  }
}
