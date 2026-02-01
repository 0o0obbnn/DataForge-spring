package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 星座生成器
 *
 * <p>支持的参数： - sign: 指定星座
 * (ARIES|TAURUS|GEMINI|CANCER|LEO|VIRGO|LIBRA|SCORPIO|SAGITTARIUS|CAPRICORN|AQUARIUS|PISCES|ANY) -
 * birth_date_related: 是否与出生日期关联 (true|false) - format: 输出格式 (CHINESE|ENGLISH|SYMBOL|CODE)
 *
 * @author DataForge
 */
public class ZodiacGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(ZodiacGenerator.class);
  private static final Random random = new Random();

  // 星座枚举
  private enum ZodiacSign {
    ARIES, // 白羊座
    TAURUS, // 金牛座
    GEMINI, // 双子座
    CANCER, // 巨蟹座
    LEO, // 狮子座
    VIRGO, // 处女座
    LIBRA, // 天秤座
    SCORPIO, // 天蝎座
    SAGITTARIUS, // 射手座
    CAPRICORN, // 摩羯座
    AQUARIUS, // 水瓶座
    PISCES // 双鱼座
  }

  // 星座信息类
  private static class ZodiacInfo {
    final ZodiacSign sign;
    final String chineseName;
    final String englishName;
    final String symbol;
    final String code;
    final int startMonth;
    final int startDay;
    final int endMonth;
    final int endDay;

    ZodiacInfo(
        ZodiacSign sign,
        String chineseName,
        String englishName,
        String symbol,
        String code,
        int startMonth,
        int startDay,
        int endMonth,
        int endDay) {
      this.sign = sign;
      this.chineseName = chineseName;
      this.englishName = englishName;
      this.symbol = symbol;
      this.code = code;
      this.startMonth = startMonth;
      this.startDay = startDay;
      this.endMonth = endMonth;
      this.endDay = endDay;
    }
  }

  // 星座信息映射
  private static final Map<ZodiacSign, ZodiacInfo> ZODIAC_INFO = new HashMap<>();

  static {
    initializeZodiacInfo();
  }

  private static void initializeZodiacInfo() {
    ZODIAC_INFO.put(
        ZodiacSign.ARIES,
        new ZodiacInfo(ZodiacSign.ARIES, "白羊座", "Aries", "♈", "ARI", 3, 21, 4, 19));
    ZODIAC_INFO.put(
        ZodiacSign.TAURUS,
        new ZodiacInfo(ZodiacSign.TAURUS, "金牛座", "Taurus", "♉", "TAU", 4, 20, 5, 20));
    ZODIAC_INFO.put(
        ZodiacSign.GEMINI,
        new ZodiacInfo(ZodiacSign.GEMINI, "双子座", "Gemini", "♊", "GEM", 5, 21, 6, 21));
    ZODIAC_INFO.put(
        ZodiacSign.CANCER,
        new ZodiacInfo(ZodiacSign.CANCER, "巨蟹座", "Cancer", "♋", "CAN", 6, 22, 7, 22));
    ZODIAC_INFO.put(
        ZodiacSign.LEO, new ZodiacInfo(ZodiacSign.LEO, "狮子座", "Leo", "♌", "LEO", 7, 23, 8, 22));
    ZODIAC_INFO.put(
        ZodiacSign.VIRGO,
        new ZodiacInfo(ZodiacSign.VIRGO, "处女座", "Virgo", "♍", "VIR", 8, 23, 9, 22));
    ZODIAC_INFO.put(
        ZodiacSign.LIBRA,
        new ZodiacInfo(ZodiacSign.LIBRA, "天秤座", "Libra", "♎", "LIB", 9, 23, 10, 23));
    ZODIAC_INFO.put(
        ZodiacSign.SCORPIO,
        new ZodiacInfo(ZodiacSign.SCORPIO, "天蝎座", "Scorpio", "♏", "SCO", 10, 24, 11, 22));
    ZODIAC_INFO.put(
        ZodiacSign.SAGITTARIUS,
        new ZodiacInfo(ZodiacSign.SAGITTARIUS, "射手座", "Sagittarius", "♐", "SAG", 11, 23, 12, 21));
    ZODIAC_INFO.put(
        ZodiacSign.CAPRICORN,
        new ZodiacInfo(ZodiacSign.CAPRICORN, "摩羯座", "Capricorn", "♑", "CAP", 12, 22, 1, 19));
    ZODIAC_INFO.put(
        ZodiacSign.AQUARIUS,
        new ZodiacInfo(ZodiacSign.AQUARIUS, "水瓶座", "Aquarius", "♒", "AQU", 1, 20, 2, 18));
    ZODIAC_INFO.put(
        ZodiacSign.PISCES,
        new ZodiacInfo(ZodiacSign.PISCES, "双鱼座", "Pisces", "♓", "PIS", 2, 19, 3, 20));
  }

  @Override
  public String getType() {
    return "zodiac";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String sign = config.getParam("sign", String.class, "ANY");
      boolean birthDateRelated =
          Boolean.parseBoolean(config.getParam("birth_date_related", String.class, "true"));
      String format = config.getParam("format", String.class, "CHINESE");

      ZodiacSign zodiacSign;

      // 如果启用出生日期关联，尝试从上下文获取出生日期
      if (birthDateRelated) {
        zodiacSign = getZodiacFromBirthDate(context, sign);
      } else {
        zodiacSign = getSpecifiedZodiac(sign);
      }

      // 将星座信息存入上下文
      context.put("zodiac_sign", zodiacSign.name());

      // 格式化输出
      String result = formatZodiac(zodiacSign, format);

      logger.debug("Generated zodiac sign: {}", result);
      return result;

    } catch (Exception e) {
      logger.error("Error generating zodiac sign", e);
      return "白羊座";
    }
  }

  private ZodiacSign getZodiacFromBirthDate(DataForgeContext context, String sign) {
    // 尝试从上下文获取出生日期
    String birthDateStr = context.get("birth_date", String.class).orElse(null);

    if (birthDateStr != null) {
      try {
        LocalDate birthDate =
            LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        ZodiacSign calculatedSign = calculateZodiacSign(birthDate);

        // 如果指定了特定星座，检查是否匹配
        if (!"ANY".equals(sign)) {
          try {
            ZodiacSign specifiedSign = ZodiacSign.valueOf(sign);
            if (calculatedSign == specifiedSign) {
              return calculatedSign;
            } else {
              logger.debug(
                  "Birth date {} corresponds to {}, but {} was specified. Using calculated sign.",
                  birthDateStr,
                  calculatedSign,
                  specifiedSign);
              return calculatedSign;
            }
          } catch (IllegalArgumentException e) {
            logger.warn("Unknown zodiac sign: {}. Using calculated sign from birth date.", sign);
            return calculatedSign;
          }
        }

        return calculatedSign;

      } catch (DateTimeParseException e) {
        logger.warn("Failed to parse birth date: {}. Using random zodiac sign.", birthDateStr);
      }
    }

    // 如果没有出生日期或解析失败，使用指定的星座或随机选择
    return getSpecifiedZodiac(sign);
  }

  private ZodiacSign getSpecifiedZodiac(String sign) {
    if ("ANY".equals(sign)) {
      ZodiacSign[] signs = ZodiacSign.values();
      return signs[random.nextInt(signs.length)];
    } else {
      try {
        return ZodiacSign.valueOf(sign);
      } catch (IllegalArgumentException e) {
        logger.warn("Unknown zodiac sign: {}. Using random sign.", sign);
        ZodiacSign[] signs = ZodiacSign.values();
        return signs[random.nextInt(signs.length)];
      }
    }
  }

  private ZodiacSign calculateZodiacSign(LocalDate birthDate) {
    int month = birthDate.getMonthValue();
    int day = birthDate.getDayOfMonth();

    for (ZodiacInfo info : ZODIAC_INFO.values()) {
      if (isDateInZodiacRange(month, day, info)) {
        return info.sign;
      }
    }

    // 默认返回白羊座（理论上不应该到达这里）
    return ZodiacSign.ARIES;
  }

  private boolean isDateInZodiacRange(int month, int day, ZodiacInfo info) {
    // 处理跨年的星座（摩羯座、水瓶座、双鱼座）
    if (info.startMonth > info.endMonth) {
      // 跨年星座
      return (month == info.startMonth && day >= info.startDay)
          || (month == info.endMonth && day <= info.endDay)
          || (month > info.startMonth || month < info.endMonth);
    } else {
      // 同年星座
      return (month == info.startMonth && day >= info.startDay)
          || (month == info.endMonth && day <= info.endDay)
          || (month > info.startMonth && month < info.endMonth);
    }
  }

  private String formatZodiac(ZodiacSign sign, String format) {
    ZodiacInfo info = ZODIAC_INFO.get(sign);

    switch (format.toUpperCase()) {
      case "CHINESE":
      case "CN":
        return info.chineseName;

      case "ENGLISH":
      case "EN":
        return info.englishName;

      case "SYMBOL":
        return info.symbol;

      case "CODE":
        return info.code;

      default:
        logger.warn("Unknown zodiac format: {}. Using CHINESE format.", format);
        return info.chineseName;
    }
  }
}
