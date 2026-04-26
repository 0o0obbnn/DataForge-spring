package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 时区标识生成器
 *
 * <p>支持生成各种时区标识符，用于国际化应用测试、时间处理功能验证、 多时区系统开发等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>format: 输出格式 (IANA|OFFSET|ABBREVIATION|DISPLAY_NAME) 默认: IANA
 *   <li>region: 地区过滤 (AMERICA|EUROPE|ASIA|AFRICA|AUSTRALIA|PACIFIC|ALL) 默认: ALL
 *   <li>offset_min: 最小UTC偏移（小时）默认: -12
 *   <li>offset_max: 最大UTC偏移（小时）默认: 14
 *   <li>include_dst: 是否包含夏令时时区 默认: true
 *   <li>locale: 显示名称的语言环境 默认: en
 *   <li>style: 显示名称样式 (FULL|SHORT) 默认: FULL
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class TimezoneGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(TimezoneGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 输出格式枚举
  public enum OutputFormat {
    IANA("IANA时区标识符"),
    OFFSET("UTC偏移量"),
    ABBREVIATION("时区缩写"),
    DISPLAY_NAME("显示名称");

    private final String description;

    OutputFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 地区枚举
  public enum Region {
    AMERICA("美洲"),
    EUROPE("欧洲"),
    ASIA("亚洲"),
    AFRICA("非洲"),
    AUSTRALIA("澳洲"),
    PACIFIC("太平洋"),
    ALL("全部");

    private final String description;

    Region(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 常用时区映射
  private static final Map<String, List<String>> REGION_TIMEZONES = new HashMap<>();

  static {
    REGION_TIMEZONES.put(
        "AMERICA",
        Arrays.asList(
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "America/Toronto",
            "America/Vancouver",
            "America/Mexico_City",
            "America/Sao_Paulo",
            "America/Buenos_Aires",
            "America/Lima",
            "America/Bogota",
            "America/Caracas"));

    REGION_TIMEZONES.put(
        "EUROPE",
        Arrays.asList(
            "Europe/London",
            "Europe/Paris",
            "Europe/Berlin",
            "Europe/Rome",
            "Europe/Madrid",
            "Europe/Amsterdam",
            "Europe/Brussels",
            "Europe/Vienna",
            "Europe/Prague",
            "Europe/Warsaw",
            "Europe/Moscow",
            "Europe/Istanbul"));

    REGION_TIMEZONES.put(
        "ASIA",
        Arrays.asList(
            "Asia/Shanghai",
            "Asia/Tokyo",
            "Asia/Seoul",
            "Asia/Hong_Kong",
            "Asia/Singapore",
            "Asia/Bangkok",
            "Asia/Jakarta",
            "Asia/Manila",
            "Asia/Kolkata",
            "Asia/Dubai",
            "Asia/Riyadh",
            "Asia/Tehran"));

    REGION_TIMEZONES.put(
        "AFRICA",
        Arrays.asList(
            "Africa/Cairo",
            "Africa/Lagos",
            "Africa/Johannesburg",
            "Africa/Nairobi",
            "Africa/Casablanca",
            "Africa/Tunis",
            "Africa/Algiers",
            "Africa/Addis_Ababa"));

    REGION_TIMEZONES.put(
        "AUSTRALIA",
        Arrays.asList(
            "Australia/Sydney",
            "Australia/Melbourne",
            "Australia/Brisbane",
            "Australia/Perth",
            "Australia/Adelaide",
            "Australia/Darwin",
            "Australia/Hobart"));

    REGION_TIMEZONES.put(
        "PACIFIC",
        Arrays.asList(
            "Pacific/Auckland",
            "Pacific/Fiji",
            "Pacific/Honolulu",
            "Pacific/Guam",
            "Pacific/Tahiti",
            "Pacific/Samoa",
            "Pacific/Tonga"));
  }

  @Override
  public String getType() {
    return "timezone";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取输出格式
      String formatStr = getStringParam(config, "format", "IANA");
      OutputFormat format = parseOutputFormat(formatStr);

      // 获取地区过滤
      String regionStr = getStringParam(config, "region", "ALL");
      Region region = parseRegion(regionStr);

      // 获取其他参数
      boolean includeDst = getBooleanParam(config, "include_dst", true);
      String localeStr = getStringParam(config, "locale", "en");
      String styleStr = getStringParam(config, "style", "FULL");

      // 生成时区
      ZoneId zoneId = generateTimezone(region, includeDst, config);

      // 格式化输出
      return formatTimezone(zoneId, format, localeStr, styleStr);

    } catch (Exception e) {
      logger.error("Failed to generate timezone", e);
      // 返回一个默认时区作为fallback
      return "UTC";
    }
  }

  /** 解析输出格式 */
  private OutputFormat parseOutputFormat(String formatStr) {
    try {
      return OutputFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output format: {}, using IANA as default", formatStr);
      return OutputFormat.IANA;
    }
  }

  /** 解析地区 */
  private Region parseRegion(String regionStr) {
    try {
      return Region.valueOf(regionStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid region: {}, using ALL as default", regionStr);
      return Region.ALL;
    }
  }

  /** 生成时区 */
  private ZoneId generateTimezone(Region region, boolean includeDst, FieldConfig config) {
    List<String> candidateTimezones = new ArrayList<>();

    if (region == Region.ALL) {
      // 获取所有时区
      for (List<String> timezones : REGION_TIMEZONES.values()) {
        candidateTimezones.addAll(timezones);
      }
    } else {
      // 获取指定地区的时区
      List<String> regionTimezones = REGION_TIMEZONES.get(region.name());
      if (regionTimezones != null) {
        candidateTimezones.addAll(regionTimezones);
      }
    }

    // 如果没有候选时区，使用所有可用时区
    if (candidateTimezones.isEmpty()) {
      candidateTimezones = ZoneId.getAvailableZoneIds().stream().collect(Collectors.toList());
    }

    // 过滤出系统中实际可用的时区
    List<String> validTimezones =
        candidateTimezones.stream()
            .filter(
                zoneId -> {
                  try {
                    ZoneId.of(zoneId);
                    return true;
                  } catch (Exception e) {
                    logger.debug("Invalid timezone ID: {}", zoneId);
                    return false;
                  }
                })
            .collect(Collectors.toList());

    if (validTimezones.isEmpty()) {
      return ZoneId.of("UTC");
    }

    // 根据UTC偏移量过滤
    int offsetMin = getIntParam(config, "offset_min", -12);
    int offsetMax = getIntParam(config, "offset_max", 14);

    List<ZoneId> filteredZones =
        validTimezones.stream()
            .map(ZoneId::of)
            .filter(
                zone -> {
                  ZoneOffset offset = zone.getRules().getOffset(java.time.Instant.now());
                  int offsetHours = offset.getTotalSeconds() / 3600;
                  return offsetHours >= offsetMin && offsetHours <= offsetMax;
                })
            .collect(Collectors.toList());

    // 如果不包含夏令时，过滤掉有夏令时的时区
    if (!includeDst) {
      filteredZones =
          filteredZones.stream()
              .filter(zone -> !zone.getRules().isDaylightSavings(java.time.Instant.now()))
              .collect(Collectors.toList());
    }

    // 如果过滤后没有时区，返回UTC
    if (filteredZones.isEmpty()) {
      return ZoneId.of("UTC");
    }

    // 随机选择一个时区
    return filteredZones.get(random.nextInt(filteredZones.size()));
  }

  /** 格式化时区 */
  private String formatTimezone(
      ZoneId zoneId, OutputFormat format, String localeStr, String styleStr) {
    switch (format) {
      case IANA:
        return zoneId.getId();
      case OFFSET:
        return formatOffset(zoneId);
      case ABBREVIATION:
        return formatAbbreviation(zoneId, localeStr);
      case DISPLAY_NAME:
        return formatDisplayName(zoneId, localeStr, styleStr);
      default:
        return zoneId.getId();
    }
  }

  /** 格式化为UTC偏移量 */
  private String formatOffset(ZoneId zoneId) {
    ZoneOffset offset = zoneId.getRules().getOffset(java.time.Instant.now());
    return offset.getId();
  }

  /** 格式化为时区缩写 */
  private String formatAbbreviation(ZoneId zoneId, String localeStr) {
    Locale locale = parseLocale(localeStr);
    return zoneId.getDisplayName(TextStyle.SHORT, locale);
  }

  /** 格式化为显示名称 */
  private String formatDisplayName(ZoneId zoneId, String localeStr, String styleStr) {
    Locale locale = parseLocale(localeStr);
    TextStyle style = parseTextStyle(styleStr);
    return zoneId.getDisplayName(style, locale);
  }

  /** 解析语言环境 */
  private Locale parseLocale(String localeStr) {
    try {
      return Locale.forLanguageTag(localeStr.replace("_", "-"));
    } catch (Exception e) {
      logger.warn("Invalid locale: {}, using English as default", localeStr);
      return Locale.ENGLISH;
    }
  }

  /** 解析文本样式 */
  private TextStyle parseTextStyle(String styleStr) {
    try {
      return TextStyle.valueOf(styleStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid text style: {}, using FULL as default", styleStr);
      return TextStyle.FULL;
    }
  }
}
