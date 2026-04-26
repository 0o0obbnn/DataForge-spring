package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 货币生成器
 *
 * <p>支持生成各种货币代码、符号、名称和金额， 用于国际化测试、电商系统、金融系统等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>type: 输出类型 (CODE|SYMBOL|NAME|AMOUNT|FULL) 默认: CODE
 *   <li>currency: 指定货币 (USD|EUR|CNY|JPY|GBP|...|ANY) 默认: ANY
 *   <li>region: 地区过滤 (AMERICAS|EUROPE|ASIA|AFRICA|OCEANIA|ANY) 默认: ANY
 *   <li>common_only: 是否只生成常用货币 默认: true
 *   <li>amount_min: 金额最小值 默认: 0.01
 *   <li>amount_max: 金额最大值 默认: 10000.00
 *   <li>scale: 小数位数 默认: 2
 *   <li>format: 金额格式 (PLAIN|FORMATTED|SYMBOL) 默认: FORMATTED
 *   <li>locale: 本地化设置 默认: en_US
 *   <li>include_minor: 是否包含辅币单位 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class CurrencyGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(CurrencyGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 输出类型枚举
  public enum OutputType {
    CODE("货币代码"),
    SYMBOL("货币符号"),
    NAME("货币名称"),
    AMOUNT("金额"),
    FULL("完整信息");

    private final String description;

    OutputType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 金额格式枚举
  public enum AmountFormat {
    PLAIN("纯数字"),
    FORMATTED("格式化"),
    SYMBOL("带符号");

    private final String description;

    AmountFormat(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  // 货币信息类
  public static class CurrencyInfo {
    private final String code;
    private final String symbol;
    private final String name;
    private final String region;
    private final int defaultFractionDigits;
    private final boolean common;

    public CurrencyInfo(
        String code,
        String symbol,
        String name,
        String region,
        int defaultFractionDigits,
        boolean common) {
      this.code = code;
      this.symbol = symbol;
      this.name = name;
      this.region = region;
      this.defaultFractionDigits = defaultFractionDigits;
      this.common = common;
    }

    // Getters
    public String getCode() {
      return code;
    }

    public String getSymbol() {
      return symbol;
    }

    public String getName() {
      return name;
    }

    public String getRegion() {
      return region;
    }

    public int getDefaultFractionDigits() {
      return defaultFractionDigits;
    }

    public boolean isCommon() {
      return common;
    }
  }

  // 货币数据库
  private static final Map<String, CurrencyInfo> CURRENCIES = new HashMap<>();
  private static final Map<String, List<CurrencyInfo>> CURRENCIES_BY_REGION = new HashMap<>();
  private static final List<CurrencyInfo> COMMON_CURRENCIES = new ArrayList<>();

  static {
    initializeCurrencies();
  }

  private static void initializeCurrencies() {
    // 主要货币
    addCurrency("USD", "$", "US Dollar", "AMERICAS", 2, true);
    addCurrency("EUR", "€", "Euro", "EUROPE", 2, true);
    addCurrency("CNY", "¥", "Chinese Yuan", "ASIA", 2, true);
    addCurrency("JPY", "¥", "Japanese Yen", "ASIA", 0, true);
    addCurrency("GBP", "£", "British Pound", "EUROPE", 2, true);
    addCurrency("AUD", "A$", "Australian Dollar", "OCEANIA", 2, true);
    addCurrency("CAD", "C$", "Canadian Dollar", "AMERICAS", 2, true);
    addCurrency("CHF", "Fr", "Swiss Franc", "EUROPE", 2, true);
    addCurrency("HKD", "HK$", "Hong Kong Dollar", "ASIA", 2, true);
    addCurrency("SGD", "S$", "Singapore Dollar", "ASIA", 2, true);

    // 其他常用货币
    addCurrency("KRW", "₩", "South Korean Won", "ASIA", 0, true);
    addCurrency("INR", "₹", "Indian Rupee", "ASIA", 2, true);
    addCurrency("RUB", "₽", "Russian Ruble", "EUROPE", 2, true);
    addCurrency("BRL", "R$", "Brazilian Real", "AMERICAS", 2, true);
    addCurrency("MXN", "$", "Mexican Peso", "AMERICAS", 2, true);
    addCurrency("ZAR", "R", "South African Rand", "AFRICA", 2, true);
    addCurrency("SEK", "kr", "Swedish Krona", "EUROPE", 2, true);
    addCurrency("NOK", "kr", "Norwegian Krone", "EUROPE", 2, true);
    addCurrency("DKK", "kr", "Danish Krone", "EUROPE", 2, true);
    addCurrency("PLN", "zł", "Polish Zloty", "EUROPE", 2, true);

    // 不太常用的货币
    addCurrency("TRY", "₺", "Turkish Lira", "EUROPE", 2, false);
    addCurrency("THB", "฿", "Thai Baht", "ASIA", 2, false);
    addCurrency("MYR", "RM", "Malaysian Ringgit", "ASIA", 2, false);
    addCurrency("IDR", "Rp", "Indonesian Rupiah", "ASIA", 2, false);
    addCurrency("PHP", "₱", "Philippine Peso", "ASIA", 2, false);
    addCurrency("VND", "₫", "Vietnamese Dong", "ASIA", 0, false);
    addCurrency("EGP", "£", "Egyptian Pound", "AFRICA", 2, false);
    addCurrency("NGN", "₦", "Nigerian Naira", "AFRICA", 2, false);

    addCurrency("KES", "KSh", "Kenyan Shilling", "AFRICA", 2, false);
    addCurrency("MAD", "د.م.", "Moroccan Dirham", "AFRICA", 2, false);

    // 中东货币
    addCurrency("AED", "د.إ", "UAE Dirham", "ASIA", 2, false);
    addCurrency("SAR", "﷼", "Saudi Riyal", "ASIA", 2, false);
    addCurrency("QAR", "﷼", "Qatari Riyal", "ASIA", 2, false);
    addCurrency("KWD", "د.ك", "Kuwaiti Dinar", "ASIA", 3, false);
    addCurrency("BHD", ".د.ب", "Bahraini Dinar", "ASIA", 3, false);

    // 拉丁美洲货币
    addCurrency("ARS", "$", "Argentine Peso", "AMERICAS", 2, false);
    addCurrency("CLP", "$", "Chilean Peso", "AMERICAS", 0, false);
    addCurrency("COP", "$", "Colombian Peso", "AMERICAS", 2, false);
    addCurrency("PEN", "S/", "Peruvian Sol", "AMERICAS", 2, false);
    addCurrency("UYU", "$U", "Uruguayan Peso", "AMERICAS", 2, false);

    // 欧洲其他货币
    addCurrency("CZK", "Kč", "Czech Koruna", "EUROPE", 2, false);
    addCurrency("HUF", "Ft", "Hungarian Forint", "EUROPE", 2, false);
    addCurrency("RON", "lei", "Romanian Leu", "EUROPE", 2, false);
    addCurrency("BGN", "лв", "Bulgarian Lev", "EUROPE", 2, false);
    addCurrency("HRK", "kn", "Croatian Kuna", "EUROPE", 2, false);

    // 加密货币（虚拟）
    addCurrency("BTC", "₿", "Bitcoin", "DIGITAL", 8, false);
    addCurrency("ETH", "Ξ", "Ethereum", "DIGITAL", 18, false);
    addCurrency("USDT", "₮", "Tether", "DIGITAL", 6, false);
  }

  private static void addCurrency(
      String code, String symbol, String name, String region, int fractionDigits, boolean common) {
    CurrencyInfo currency = new CurrencyInfo(code, symbol, name, region, fractionDigits, common);
    CURRENCIES.put(code, currency);

    CURRENCIES_BY_REGION.computeIfAbsent(region, k -> new ArrayList<>()).add(currency);

    if (common) {
      COMMON_CURRENCIES.add(currency);
    }
  }

  @Override
  public String getType() {
    return "currency";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取输出类型
      String typeStr = getStringParam(config, "type", "CODE");
      OutputType outputType = parseOutputType(typeStr);

      // 选择货币
      CurrencyInfo currency = selectCurrency(config);

      // 生成输出
      String result = generateOutput(currency, outputType, config, context);

      // 存储到上下文
      context.put("currency_code", currency.getCode());
      context.put("currency_symbol", currency.getSymbol());
      context.put("currency_name", currency.getName());
      context.put("currency_region", currency.getRegion());

      return result;

    } catch (Exception e) {
      logger.error("Failed to generate currency", e);
      return "USD";
    }
  }

  /** 解析输出类型 */
  private OutputType parseOutputType(String typeStr) {
    try {
      return OutputType.valueOf(typeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid output type: {}, using CODE as default", typeStr);
      return OutputType.CODE;
    }
  }

  /** 选择货币 */
  private CurrencyInfo selectCurrency(FieldConfig config) {
    String currency = getStringParam(config, "currency", "ANY");
    String region = getStringParam(config, "region", "ANY");
    boolean commonOnly = getBooleanParam(config, "common_only", true);

    // 如果指定了具体货币
    if (!"ANY".equalsIgnoreCase(currency)) {
      CurrencyInfo info = CURRENCIES.get(currency.toUpperCase());
      if (info != null) {
        return info;
      }
    }

    // 构建候选列表
    List<CurrencyInfo> candidates = new ArrayList<>();

    if (commonOnly) {
      candidates.addAll(COMMON_CURRENCIES);
    } else {
      candidates.addAll(CURRENCIES.values());
    }

    // 按地区过滤
    if (!"ANY".equalsIgnoreCase(region)) {
      List<CurrencyInfo> regionCurrencies = CURRENCIES_BY_REGION.get(region.toUpperCase());
      if (regionCurrencies != null) {
        candidates.retainAll(regionCurrencies);
      }
    }

    // 如果没有候选货币，使用默认
    if (candidates.isEmpty()) {
      return CURRENCIES.get("USD");
    }

    // 随机选择
    return candidates.get(random.nextInt(candidates.size()));
  }

  /** 生成输出 */
  private String generateOutput(
      CurrencyInfo currency, OutputType outputType, FieldConfig config, DataForgeContext context) {
    switch (outputType) {
      case CODE:
        return currency.getCode();
      case SYMBOL:
        return currency.getSymbol();
      case NAME:
        return currency.getName();
      case AMOUNT:
        return generateAmount(currency, config);
      case FULL:
        return generateFullInfo(currency, config);
      default:
        return currency.getCode();
    }
  }

  /** 生成金额 */
  private String generateAmount(CurrencyInfo currency, FieldConfig config) {
    // 获取金额范围
    double min = getDoubleParam(config, "amount_min", 0.01);
    double max = getDoubleParam(config, "amount_max", 10000.0);

    // 生成随机金额
    double amount = min + random.nextDouble() * (max - min);

    // 获取小数位数
    int scale = getIntParam(config, "scale", currency.getDefaultFractionDigits());

    // 应用精度
    BigDecimal decimal = BigDecimal.valueOf(amount).setScale(scale, RoundingMode.HALF_UP);

    // 获取格式
    String formatStr = getStringParam(config, "format", "FORMATTED");
    AmountFormat format = parseAmountFormat(formatStr);

    return formatAmount(decimal, currency, format, config);
  }

  /** 解析金额格式 */
  private AmountFormat parseAmountFormat(String formatStr) {
    try {
      return AmountFormat.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid amount format: {}, using FORMATTED as default", formatStr);
      return AmountFormat.FORMATTED;
    }
  }

  /** 格式化金额 */
  private String formatAmount(
      BigDecimal amount, CurrencyInfo currency, AmountFormat format, FieldConfig config) {
    switch (format) {
      case PLAIN:
        return amount.toPlainString();
      case FORMATTED:
        return formatAmountWithLocale(amount, currency, config);
      case SYMBOL:
        return currency.getSymbol() + amount.toPlainString();
      default:
        return amount.toPlainString();
    }
  }

  /** 使用本地化格式化金额 */
  private String formatAmountWithLocale(
      BigDecimal amount, CurrencyInfo currency, FieldConfig config) {
    String localeStr = getStringParam(config, "locale", "en_US");
    Locale locale = parseLocale(localeStr);

    try {
      NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);
      Currency javaCurrency = Currency.getInstance(currency.getCode());
      currencyFormat.setCurrency(javaCurrency);
      return currencyFormat.format(amount);
    } catch (Exception e) {
      // 如果货币不被Java支持，使用符号格式
      logger.debug("Currency {} not supported by Java, using symbol format", currency.getCode());
      return currency.getSymbol() + amount.toPlainString();
    }
  }

  /** 生成完整信息 */
  private String generateFullInfo(CurrencyInfo currency, FieldConfig config) {
    StringBuilder sb = new StringBuilder();
    sb.append(currency.getCode());
    sb.append(" (").append(currency.getSymbol()).append(")");
    sb.append(" - ").append(currency.getName());

    // 如果需要包含金额
    if (getBooleanParam(config, "include_amount", true)) {
      String amount = generateAmount(currency, config);
      sb.append(" - ").append(amount);
    }

    return sb.toString();
  }

  /** 解析本地化设置 */
  private Locale parseLocale(String localeStr) {
    try {
      if (localeStr.contains("_")) {
        String[] parts = localeStr.split("_");
        if (parts.length >= 2) {
          // 修复弃用的Locale构造函数，使用Locale.Builder替代
          return new Locale.Builder().setLanguage(parts[0]).setRegion(parts[1]).build();
        }
      }
      return Locale.forLanguageTag(localeStr.replace("_", "-"));
    } catch (Exception e) {
      logger.warn("Invalid locale: {}, using English as default", localeStr);
      return Locale.ENGLISH;
    }
  }

  /** 获取所有支持的货币代码 */
  public static Set<String> getSupportedCurrencies() {
    return CURRENCIES.keySet();
  }

  /** 获取指定地区的货币 */
  public static List<CurrencyInfo> getCurrenciesByRegion(String region) {
    return CURRENCIES_BY_REGION.getOrDefault(region.toUpperCase(), new ArrayList<>());
  }

  /** 获取常用货币 */
  public static List<CurrencyInfo> getCommonCurrencies() {
    return new ArrayList<>(COMMON_CURRENCIES);
  }

  /** 验证货币代码 */
  public static boolean isValidCurrency(String code) {
    return CURRENCIES.containsKey(code.toUpperCase());
  }

  /** 获取货币信息 */
  public static CurrencyInfo getCurrencyInfo(String code) {
    return CURRENCIES.get(code.toUpperCase());
  }
}
