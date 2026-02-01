package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 金融交易日历生成器
 *
 * <p>支持功能： - 多个金融市场（A股、美股、港股、欧股等） - 交易日/非交易日生成 - 节假日数据支持 - 交易时间段生成 - 市场开盘/收盘时间 - 与日期时间字段关联
 *
 * <p>配置参数： - market: 金融市场（A_SHARE、US_STOCK、HK_STOCK、EU_STOCK、CUSTOM） - type:
 * 日期类型（TRADING_DAY、NON_TRADING_DAY、HOLIDAY、ANY） - startDate: 开始日期 - endDate: 结束日期 - format:
 * 输出格式（DATE、DATETIME、TIMESTAMP、VERBOSE） - includeTime: 是否包含交易时间 - timeSession:
 * 交易时段（MORNING、AFTERNOON、FULL、CUSTOM） - holidayFile: 自定义节假日文件
 *
 * @author DataForge
 * @version 1.0
 */
public class TradingCalendarGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(TradingCalendarGenerator.class);

  private static final String DEFAULT_CONFIG_FILE = "data/trading-calendar.yml";

  /** 配置数据（从文件加载） */
  private volatile TradingCalendarConfig calendarConfig;

  // A股交易时间（fallback）
  private static final String[] A_SHARE_MORNING = {"09:30", "11:30"};
  private static final String[] A_SHARE_AFTERNOON = {"13:00", "15:00"};

  // 美股交易时间（EST）（fallback）
  private static final String[] US_STOCK_REGULAR = {"09:30", "16:00"};
  private static final String[] US_STOCK_PREMARKET = {"04:00", "09:30"};
  private static final String[] US_STOCK_AFTERHOURS = {"16:00", "20:00"};

  // 港股交易时间（fallback）
  private static final String[] HK_STOCK_MORNING = {"09:30", "12:00"};
  private static final String[] HK_STOCK_AFTERNOON = {"13:00", "16:00"};

  // 中国A股节假日（fallback）
  private static final Set<String> A_SHARE_HOLIDAYS_2024 =
      new HashSet<>(
          Arrays.asList(
              "2024-01-01",
              "2024-02-10",
              "2024-02-11",
              "2024-02-12",
              "2024-02-13",
              "2024-02-14",
              "2024-02-15",
              "2024-02-16",
              "2024-02-17",
              "2024-04-04",
              "2024-04-05",
              "2024-04-06",
              "2024-05-01",
              "2024-05-02",
              "2024-05-03",
              "2024-06-10",
              "2024-09-15",
              "2024-09-16",
              "2024-09-17",
              "2024-10-01",
              "2024-10-02",
              "2024-10-03",
              "2024-10-04",
              "2024-10-05",
              "2024-10-06",
              "2024-10-07"));

  // 美股节假日（fallback）
  private static final Set<String> US_STOCK_HOLIDAYS_2024 =
      new HashSet<>(
          Arrays.asList(
              "2024-01-01",
              "2024-01-15",
              "2024-02-19",
              "2024-03-29",
              "2024-05-27",
              "2024-06-19",
              "2024-07-04",
              "2024-09-02",
              "2024-11-28",
              "2024-12-25"));

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  public String getType() {
    return "tradingcalendar";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      ensureConfigLoaded(config);

      String market = getStringParam(config, "market", "A_SHARE");
      String type = getStringParam(config, "type", "TRADING_DAY");
      String startDateStr = getStringParam(config, "startDate", "2024-01-01");
      String endDateStr = getStringParam(config, "endDate", "2024-12-31");
      String format = getStringParam(config, "format", "DATE");
      boolean includeTime = getBooleanParam(config, "includeTime", false);
      String timeSession = getStringParam(config, "timeSession", "FULL");

      try {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        LocalDate contextDate = getDateFromContext(context);
        if (contextDate != null && isTradingDate(contextDate, market, type)) {
          return formatTradingDate(contextDate, market, format, includeTime, timeSession);
        }

        LocalDate randomDate = generateRandomTradingDate(startDate, endDate, market, type);
        storeTradingDateInContext(context, randomDate, market);
        return formatTradingDate(randomDate, market, format, includeTime, timeSession);
      } catch (Exception e) {
        LocalDate defaultDate = getNextTradingDay(LocalDate.now(), market);
        return formatTradingDate(defaultDate, market, format, includeTime, timeSession);
      }
    } catch (Exception e) {
      String market = getStringParam(config, "market", "A_SHARE");
      String format = getStringParam(config, "format", "DATE");
      boolean includeTime = getBooleanParam(config, "includeTime", false);
      String timeSession = getStringParam(config, "timeSession", "FULL");
      LocalDate defaultDate = getNextTradingDay(LocalDate.now(), market);
      return formatTradingDate(defaultDate, market, format, includeTime, timeSession);
    }
  }

  /** 从上下文中获取相关日期信息 */
  private LocalDate getDateFromContext(DataForgeContext context) {
    return context.get("generated_date", LocalDate.class).orElse(null);
  }

  /** 生成随机交易日期 */
  private LocalDate generateRandomTradingDate(
      LocalDate startDate, LocalDate endDate, String market, String type) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    LocalDate randomDate;
    int maxAttempts = 100;
    int attempts = 0;

    do {
      long daysBetween = startDate.until(endDate).getDays();
      long randomDays = random.nextLong(daysBetween + 1);
      randomDate = startDate.plusDays(randomDays);
      attempts++;
    } while (!isTradingDate(randomDate, market, type) && attempts < maxAttempts);

    return randomDate;
  }

  /** 检查是否为指定类型的交易日期 */
  private boolean isTradingDate(LocalDate date, String market, String type) {
    switch (type.toUpperCase()) {
      case "TRADING_DAY":
        return isTradingDay(date, market);
      case "NON_TRADING_DAY":
        return !isTradingDay(date, market);
      case "HOLIDAY":
        return isHoliday(date, market);
      case "ANY":
      default:
        return true;
    }
  }

  /** 检查是否为交易日 */
  private boolean isTradingDay(LocalDate date, String market) {
    // 首先检查是否为周末
    if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
      return false;
    }

    // 然后检查是否为节假日
    return !isHoliday(date, market);
  }

  /** 检查是否为节假日（从配置或fallback） */
  private boolean isHoliday(LocalDate date, String market) {
    String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    if (calendarConfig != null && calendarConfig.getMarkets() != null) {
      Map<String, MarketConfig> marketConfigs = calendarConfig.getMarkets();
      MarketConfig marketConfig = marketConfigs.get(market.toUpperCase());
      if (marketConfig != null && marketConfig.getHolidays() != null) {
        return marketConfig.getHolidays().contains(dateStr);
      }
    }

    switch (market.toUpperCase()) {
      case "A_SHARE":
        return A_SHARE_HOLIDAYS_2024.contains(dateStr);
      case "US_STOCK":
        return US_STOCK_HOLIDAYS_2024.contains(dateStr);
      case "HK_STOCK":
        return A_SHARE_HOLIDAYS_2024.contains(dateStr);
      case "EU_STOCK":
        return US_STOCK_HOLIDAYS_2024.contains(dateStr);
      default:
        return false;
    }
  }

  /** 获取下一个交易日 */
  private LocalDate getNextTradingDay(LocalDate date, String market) {
    LocalDate nextDay = date.plusDays(1);
    while (!isTradingDay(nextDay, market)) {
      nextDay = nextDay.plusDays(1);
    }
    return nextDay;
  }

  /** 将交易日期信息存储到上下文中 */
  private void storeTradingDateInContext(DataForgeContext context, LocalDate date, String market) {
    context.put("generated_date", date);
    context.put("generated_trading_date", date);
    context.put("generated_market", market);
    context.put("generated_is_trading_day", isTradingDay(date, market));
    context.put("generated_is_holiday", isHoliday(date, market));
    context.put("generated_day_of_week", date.getDayOfWeek().toString());

    // 存储交易时间信息
    String[] tradingHours = getTradingHours(market, "FULL");
    if (tradingHours.length >= 2) {
      context.put("generated_market_open", tradingHours[0]);
      context.put("generated_market_close", tradingHours[1]);
    }
  }

  /** 格式化交易日期 */
  private String formatTradingDate(
      LocalDate date, String market, String format, boolean includeTime, String timeSession) {
    switch (format.toUpperCase()) {
      case "DATE":
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

      case "DATETIME":
        if (includeTime) {
          String[] tradingHours = getTradingHours(market, timeSession);
          String time = tradingHours.length > 0 ? tradingHours[0] : "09:30";
          return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + time + ":00";
        } else {
          return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }

      case "TIMESTAMP":
        if (includeTime) {
          String[] tradingHours = getTradingHours(market, timeSession);
          String time = tradingHours.length > 0 ? tradingHours[0] : "09:30";
          return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T" + time + ":00";
        } else {
          return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T00:00:00";
        }

      case "VERBOSE":
        return formatVerboseTradingDate(date, market, includeTime, timeSession);

      case "CHINESE":
        return formatChineseTradingDate(date, market, includeTime);

      default:
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
  }

  /** 格式化详细交易日期 */
  private String formatVerboseTradingDate(
      LocalDate date, String market, boolean includeTime, String timeSession) {
    StringBuilder sb = new StringBuilder();

    sb.append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    sb.append(" (").append(date.getDayOfWeek().toString()).append(")");

    if (isTradingDay(date, market)) {
      sb.append(" - Trading Day");
    } else if (isHoliday(date, market)) {
      sb.append(" - Holiday");
    } else {
      sb.append(" - Weekend");
    }

    sb.append(" [").append(market).append("]");

    if (includeTime && isTradingDay(date, market)) {
      String[] tradingHours = getTradingHours(market, timeSession);
      if (tradingHours.length >= 2) {
        sb.append(" Trading Hours: ").append(tradingHours[0]).append("-").append(tradingHours[1]);
      }
    }

    return sb.toString();
  }

  /** 格式化中文交易日期 */
  private String formatChineseTradingDate(LocalDate date, String market, boolean includeTime) {
    StringBuilder sb = new StringBuilder();

    sb.append(date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));

    String[] dayNames = {"一", "二", "三", "四", "五", "六", "日"};
    sb.append("（星期").append(dayNames[date.getDayOfWeek().getValue() - 1]).append("）");

    if (isTradingDay(date, market)) {
      sb.append(" 交易日");
    } else if (isHoliday(date, market)) {
      sb.append(" 节假日");
    } else {
      sb.append(" 周末");
    }

    String marketName = getMarketChineseName(market);
    sb.append(" [").append(marketName).append("]");

    return sb.toString();
  }

  /** 获取市场中文名称 */
  private String getMarketChineseName(String market) {
    switch (market.toUpperCase()) {
      case "A_SHARE":
        return "A股";
      case "US_STOCK":
        return "美股";
      case "HK_STOCK":
        return "港股";
      case "EU_STOCK":
        return "欧股";
      default:
        return market;
    }
  }

  @Override
  public String getDescription() {
    return "Generator for financial trading calendar dates with market-specific holidays and"
        + " trading hours";
  }

  /**
   * 确保配置已加载。
   *
   * @param config 配置
   */
  private void ensureConfigLoaded(FieldConfig config) {
    if (calendarConfig == null) {
      synchronized (this) {
        if (calendarConfig == null) {
          loadConfig(config);
        }
      }
    }
  }

  /**
   * 加载配置。
   *
   * @param config 配置
   */
  private void loadConfig(FieldConfig config) {
    try {
      String configFile = getStringParam(config, "calendar_file", DEFAULT_CONFIG_FILE);

      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
      if (inputStream != null) {
        calendarConfig = yamlMapper.readValue(inputStream, TradingCalendarConfig.class);
        logger.info("Trading calendar config loaded from: {}", configFile);
      } else {
        logger.warn("Config file not found: {}, using fallback data", configFile);
        initializeFallbackConfig();
      }
    } catch (Exception e) {
      logger.error("Failed to load trading calendar config, using fallback data", e);
      initializeFallbackConfig();
    }
  }

  /** 初始化fallback配置。 */
  private void initializeFallbackConfig() {
    calendarConfig = new TradingCalendarConfig();
  }

  /**
   * 获取交易时间（从配置或fallback）。
   *
   * @param market 市场
   * @param timeSession 交易时段
   * @return 交易时间数组
   */
  private String[] getTradingHours(String market, String timeSession) {
    if (calendarConfig != null && calendarConfig.getMarkets() != null) {
      Map<String, MarketConfig> marketConfigs = calendarConfig.getMarkets();
      MarketConfig marketConfig = marketConfigs.get(market.toUpperCase());
      if (marketConfig != null && marketConfig.getSessions() != null) {
        Map<String, String[]> sessions = marketConfig.getSessions();
        switch (timeSession.toUpperCase()) {
          case "MORNING":
            return sessions.get("morning");
          case "AFTERNOON":
            return sessions.get("afternoon");
          case "FULL":
            return new String[] {sessions.get("morning")[0], sessions.get("afternoon")[1]};
          case "PREMARKET":
            return sessions.get("premarket");
          case "AFTERHOURS":
            return sessions.get("afterhours");
          case "REGULAR":
            return sessions.get("regular");
          default:
            return new String[] {"09:30", "16:00"};
        }
      }
    }

    switch (market.toUpperCase()) {
      case "A_SHARE":
        switch (timeSession.toUpperCase()) {
          case "MORNING":
            return A_SHARE_MORNING;
          case "AFTERNOON":
            return A_SHARE_AFTERNOON;
          case "FULL":
            return new String[] {A_SHARE_MORNING[0], A_SHARE_AFTERNOON[1]};
          default:
            return new String[] {"09:30", "16:00"};
        }
      case "US_STOCK":
        switch (timeSession.toUpperCase()) {
          case "PREMARKET":
            return US_STOCK_PREMARKET;
          case "AFTERHOURS":
            return US_STOCK_AFTERHOURS;
          case "REGULAR":
            return US_STOCK_REGULAR;
          case "FULL":
            return new String[] {US_STOCK_PREMARKET[0], US_STOCK_AFTERHOURS[1]};
          default:
            return US_STOCK_REGULAR;
        }
      case "HK_STOCK":
        switch (timeSession.toUpperCase()) {
          case "MORNING":
            return HK_STOCK_MORNING;
          case "AFTERNOON":
            return HK_STOCK_AFTERNOON;
          case "FULL":
            return new String[] {HK_STOCK_MORNING[0], HK_STOCK_AFTERNOON[1]};
          default:
            return new String[] {"09:30", "16:00"};
        }
      default:
        return new String[] {"09:30", "16:00"};
    }
  }

  /** 交易日历配置类。 */
  @SuppressWarnings("unused")
  private static class TradingCalendarConfig {
    private Map<String, MarketConfig> markets;

    public Map<String, MarketConfig> getMarkets() {
      return markets;
    }

    public void setMarkets(Map<String, MarketConfig> markets) {
      this.markets = markets;
    }
  }

  /** 市场配置类。 */
  @SuppressWarnings("unused")
  private static class MarketConfig {
    private String name;
    private String currency;
    private String timezone;
    private Map<String, String[]> sessions;
    private Set<String> holidays;
    private Set<String> tradingDays;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getCurrency() {
      return currency;
    }

    public void setCurrency(String currency) {
      this.currency = currency;
    }

    public String getTimezone() {
      return timezone;
    }

    public void setTimezone(String timezone) {
      this.timezone = timezone;
    }

    public Map<String, String[]> getSessions() {
      return sessions;
    }

    public void setSessions(Map<String, String[]> sessions) {
      this.sessions = sessions;
    }

    public Set<String> getHolidays() {
      return holidays;
    }

    public void setHolidays(Set<String> holidays) {
      this.holidays = holidays;
    }

    public Set<String> getTradingDays() {
      return tradingDays;
    }

    public void setTradingDays(Set<String> tradingDays) {
      this.tradingDays = tradingDays;
    }
  }
}
