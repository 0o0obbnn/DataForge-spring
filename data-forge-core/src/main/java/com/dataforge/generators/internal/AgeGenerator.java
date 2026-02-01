package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 年龄生成器
 *
 * <p>支持功能： 1. 指定年龄范围生成 2. 与身份证号的出生日期关联 3. 支持精确年龄计算 4. 支持年龄分布控制
 *
 * <p>参数配置： - min: 最小年龄（默认18） - max: 最大年龄（默认60） - precision: 年龄精度，1表示整数年龄，0.5表示可以有半岁（默认1） -
 * distribution: 年龄分布类型 UNIFORM|NORMAL（默认UNIFORM） - link_birth_date: 是否关联出生日期（默认true）
 *
 * <p>关联字段： - birth_date: 从上下文中获取出生日期，精确计算年龄 - idcard: 从身份证号中提取出生日期
 *
 * @author DataForge
 * @since 1.0.0
 */
public class AgeGenerator extends BaseGenerator implements DataGenerator<Integer, FieldConfig> {

  private static final Logger log = LoggerFactory.getLogger(AgeGenerator.class);

  private static final String TYPE = "age";
  private static final int DEFAULT_MIN_AGE = 18;
  private static final int DEFAULT_MAX_AGE = 60;
  private static final double DEFAULT_PRECISION = 1.0;
  private static final String DEFAULT_DISTRIBUTION = "UNIFORM";
  private static final boolean DEFAULT_LINK_BIRTH_DATE = true;

  // 上下文键名
  private static final String CONTEXT_BIRTH_DATE = "birth_date";
  private static final String CONTEXT_ID_CARD = "idcard";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public Integer generate(FieldConfig config, DataForgeContext context) {
    Map<String, Object> params = config.getParams();

    // 解析配置参数
    int minAge = getIntParam(params, "min", DEFAULT_MIN_AGE);
    int maxAge = getIntParam(params, "max", DEFAULT_MAX_AGE);
    double precision = getDoubleParam(params, "precision", DEFAULT_PRECISION);
    String distribution = getStringParam(params, "distribution", DEFAULT_DISTRIBUTION);
    boolean linkBirthDate = getBooleanParam(params, "link_birth_date", DEFAULT_LINK_BIRTH_DATE);

    // 参数校验
    if (minAge < 0 || maxAge < 0 || minAge > maxAge) {
      log.warn("Invalid age range: min={}, max={}. Using defaults.", minAge, maxAge);
      minAge = DEFAULT_MIN_AGE;
      maxAge = DEFAULT_MAX_AGE;
    }

    // 尝试从上下文获取出生日期
    if (linkBirthDate) {
      Integer ageFromContext = getAgeFromContext(context);
      if (ageFromContext != null) {
        log.debug("Using age from context: {}", ageFromContext);
        return ageFromContext;
      }
    }

    // 生成随机年龄
    return generateRandomAge(minAge, maxAge, precision, distribution);
  }

  /** 从上下文中获取年龄 */
  private Integer getAgeFromContext(DataForgeContext context) {
    // 1. 直接从上下文获取出生日期
    LocalDate birthDate = context.get(CONTEXT_BIRTH_DATE, LocalDate.class).orElse(null);
    if (birthDate != null) {
      return calculateAge(birthDate);
    }

    // 2. 从身份证号中提取出生日期
    String idCard = context.get(CONTEXT_ID_CARD, String.class).orElse(null);
    if (idCard != null && idCard.length() >= 14) {
      try {
        String birthDateStr = idCard.substring(6, 14);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        birthDate = LocalDate.parse(birthDateStr, formatter);
        return calculateAge(birthDate);
      } catch (Exception e) {
        log.debug("Failed to extract birth date from ID card: {}", idCard, e);
      }
    }

    return null;
  }

  /** 计算年龄 */
  private Integer calculateAge(LocalDate birthDate) {
    if (birthDate == null) {
      return null;
    }

    LocalDate now = LocalDate.now();
    if (birthDate.isAfter(now)) {
      log.warn("Birth date is in the future: {}", birthDate);
      return null;
    }

    Period period = Period.between(birthDate, now);
    return period.getYears();
  }

  /** 生成随机年龄 */
  private Integer generateRandomAge(int minAge, int maxAge, double precision, String distribution) {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    if ("NORMAL".equalsIgnoreCase(distribution)) {
      // 正态分布：均值为中位数，标准差为范围的1/6
      double mean = (minAge + maxAge) / 2.0;
      double stdDev = (maxAge - minAge) / 6.0;

      double age;
      do {
        age = random.nextGaussian() * stdDev + mean;
      } while (age < minAge || age > maxAge);

      return (int) Math.round(age / precision) * (int) precision;
    } else {
      // 均匀分布
      if (precision == 1.0) {
        return random.nextInt(minAge, maxAge + 1);
      } else {
        // 支持小数精度
        double range = maxAge - minAge;
        double randomValue = random.nextDouble() * range + minAge;
        return (int) Math.round(randomValue / precision) * (int) precision;
      }
    }
  }

  // 工具方法（重载以支持Map参数，向后兼容）
  private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
    Object value = params.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        log.warn("Invalid integer parameter '{}': {}", key, value);
      }
    }
    return defaultValue;
  }

  private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
    Object value = params.get(key);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    if (value instanceof String) {
      try {
        return Double.parseDouble((String) value);
      } catch (NumberFormatException e) {
        log.warn("Invalid double parameter '{}': {}", key, value);
      }
    }
    return defaultValue;
  }

  private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
    Object value = params.get(key);
    return value != null ? value.toString() : defaultValue;
  }

  private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
    Object value = params.get(key);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    }
    return defaultValue;
  }
}
