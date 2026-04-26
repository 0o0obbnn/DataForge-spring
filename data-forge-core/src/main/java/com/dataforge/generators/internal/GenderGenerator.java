package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 性别生成器
 *
 * <p>支持功能： 1. 基于权重的性别选择 2. 与身份证号的性别信息关联 3. 与姓名生成器关联 4. 支持多种性别表示格式
 *
 * <p>参数配置： - type: 性别类型 MALE|FEMALE|OTHER|ANY（默认ANY） - format: 输出格式
 * CHINESE|ENGLISH|NUMBER|SYMBOL（默认CHINESE） - male_ratio: 男性占比 0.0-1.0（默认0.5） - other_ratio: 其他性别占比
 * 0.0-1.0（默认0.01） - link_idcard: 是否关联身份证号（默认true）
 *
 * <p>关联字段： - idcard: 从身份证号中提取性别信息 - name: 向上下文输出性别信息供姓名生成器使用
 *
 * <p>输出格式： - CHINESE: 男/女/其他 - ENGLISH: Male/Female/Other - NUMBER: 1/0/2 (1=男性, 0=女性, 2=其他) -
 * SYMBOL: M/F/O
 *
 * @author DataForge
 * @since 1.0.0
 */
public class GenderGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger log = LoggerFactory.getLogger(GenderGenerator.class);

  private static final String TYPE = "gender";
  private static final String DEFAULT_TYPE = "ANY";
  private static final String DEFAULT_FORMAT = "CHINESE";
  private static final double DEFAULT_MALE_RATIO = 0.5;
  private static final double DEFAULT_OTHER_RATIO = 0.01;
  private static final boolean DEFAULT_LINK_IDCARD = true;

  // 性别枚举
  public enum Gender {
    MALE,
    FEMALE,
    OTHER
  }

  // 上下文键名
  private static final String CONTEXT_ID_CARD = "idcard";
  private static final String CONTEXT_GENDER = "gender";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    // 解析配置参数
    String genderType = getStringParam(config, "type", DEFAULT_TYPE);
    String format = getStringParam(config, "format", DEFAULT_FORMAT);
    double maleRatio = getDoubleParam(config, "male_ratio", DEFAULT_MALE_RATIO);
    double otherRatio = getDoubleParam(config, "other_ratio", DEFAULT_OTHER_RATIO);
    boolean linkIdCard = getBooleanParam(config, "link_idcard", DEFAULT_LINK_IDCARD);

    // 参数校验
    maleRatio = Math.max(0.0, Math.min(1.0, maleRatio));
    otherRatio = Math.max(0.0, Math.min(1.0, otherRatio));

    // 确定性别
    Gender gender = determineGender(genderType, maleRatio, otherRatio, linkIdCard, context);

    // 将性别信息存入上下文
    context.put(CONTEXT_GENDER, gender.name());

    // 格式化输出
    return formatGender(gender, format);
  }

  /** 确定性别 */
  private Gender determineGender(
      String genderType,
      double maleRatio,
      double otherRatio,
      boolean linkIdCard,
      DataForgeContext context) {

    // 1. 如果指定了具体性别类型
    if (!"ANY".equalsIgnoreCase(genderType)) {
      try {
        return Gender.valueOf(genderType.toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn("Invalid gender type: {}. Using random generation.", genderType);
      }
    }

    // 2. 尝试从身份证号中提取性别
    if (linkIdCard) {
      Gender genderFromIdCard = getGenderFromIdCard(context);
      if (genderFromIdCard != null) {
        log.debug("Using gender from ID card: {}", genderFromIdCard);
        return genderFromIdCard;
      }
    }

    // 3. 基于权重随机生成
    return generateRandomGender(maleRatio, otherRatio);
  }

  /** 从身份证号中提取性别 */
  private Gender getGenderFromIdCard(DataForgeContext context) {
    String idCard = context.get(CONTEXT_ID_CARD, String.class).orElse(null);
    if (idCard != null && idCard.length() >= 17) {
      try {
        // 身份证号第17位（倒数第2位）表示性别：奇数为男性，偶数为女性
        char genderChar = idCard.charAt(16);
        int genderDigit = Character.getNumericValue(genderChar);
        return (genderDigit % 2 == 1) ? Gender.MALE : Gender.FEMALE;
      } catch (Exception e) {
        log.debug("Failed to extract gender from ID card: {}", idCard, e);
      }
    }
    return null;
  }

  /** 基于权重随机生成性别 */
  private Gender generateRandomGender(double maleRatio, double otherRatio) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    double randomValue = random.nextDouble();

    if (randomValue < otherRatio) {
      return Gender.OTHER;
    } else if (randomValue < otherRatio + maleRatio * (1 - otherRatio)) {
      return Gender.MALE;
    } else {
      return Gender.FEMALE;
    }
  }

  /** 格式化性别输出 */
  private String formatGender(Gender gender, String format) {
    switch (format.toUpperCase()) {
      case "CHINESE":
      case "CN":
        switch (gender) {
          case MALE:
            return "男";
          case FEMALE:
            return "女";
          case OTHER:
            return "其他";
        }
        break;

      case "ENGLISH":
        switch (gender) {
          case MALE:
            return "Male";
          case FEMALE:
            return "Female";
          case OTHER:
            return "Other";
        }
        break;

      case "NUMBER":
        switch (gender) {
          case MALE:
            return "1";
          case FEMALE:
            return "0";
          case OTHER:
            return "2";
        }
        break;

      case "SYMBOL":
        switch (gender) {
          case MALE:
            return "M";
          case FEMALE:
            return "F";
          case OTHER:
            return "O";
        }
        break;

      default:
        log.warn("Unknown gender format: {}. Using CHINESE format.", format);
        return formatGender(gender, "CHINESE");
    }

    return gender.name(); // fallback
  }
}
