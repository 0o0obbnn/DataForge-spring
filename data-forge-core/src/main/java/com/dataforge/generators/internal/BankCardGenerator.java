package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import com.dataforge.validation.LuhnValidator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 银行卡号生成器。
 *
 * <p>生成符合Luhn算法的银行卡号，支持大规模卡号生成。 支持不同银行、卡组织的BIN码和权重选择。 通过配置文件管理BIN码数据，支持生成数十亿唯一卡号。 基于BIN码（Bank
 * Identification Number）生成真实有效的卡号。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class BankCardGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(BankCardGenerator.class);

  private final LuhnValidator luhnValidator;

  public BankCardGenerator() {
    this.luhnValidator = new LuhnValidator();
  }

  /** BIN码数据文件路径。 */
  private static final String BANK_BINS_PATH = "data/bank-bins.txt";

  /** 缓存的BIN码数据。 */
  private volatile Map<String, BinInfo> bankBins;

  private volatile Map<String, List<String>> binsByBank;
  private volatile Map<String, List<String>> binsByOrganization;
  private volatile Map<String, List<String>> binsByType;

  /** Fallback BIN码数据（当文件加载失败时使用）。 */
  private static final Map<String, BinInfo> FALLBACK_BANK_BINS = new HashMap<>();

  static {
    // 初始化fallback数据 - 添加多种卡组织支持
    // UNIONPAY 银联卡
    FALLBACK_BANK_BINS.put(
        "ICBC_DEBIT", new BinInfo("622202", 19, "工商银行", "UNIONPAY", "DEBIT", 15));
    FALLBACK_BANK_BINS.put(
        "ICBC_CREDIT", new BinInfo("625330", 16, "工商银行", "UNIONPAY", "CREDIT", 12));
    FALLBACK_BANK_BINS.put("CCB_DEBIT", new BinInfo("621700", 19, "建设银行", "UNIONPAY", "DEBIT", 12));
    FALLBACK_BANK_BINS.put(
        "CCB_CREDIT", new BinInfo("625362", 16, "建设银行", "UNIONPAY", "CREDIT", 10));
    FALLBACK_BANK_BINS.put("ABC_DEBIT", new BinInfo("622848", 19, "农业银行", "UNIONPAY", "DEBIT", 12));
    FALLBACK_BANK_BINS.put(
        "ABC_CREDIT", new BinInfo("625996", 16, "农业银行", "UNIONPAY", "CREDIT", 10));

    // VISA 卡
    FALLBACK_BANK_BINS.put("VISA_DEBIT", new BinInfo("4111", 16, "VISA银行", "VISA", "DEBIT", 10));
    FALLBACK_BANK_BINS.put("VISA_CREDIT", new BinInfo("4222", 16, "VISA银行", "VISA", "CREDIT", 10));

    // MASTERCARD 万事达卡
    FALLBACK_BANK_BINS.put("MC_DEBIT", new BinInfo("5111", 16, "万事达银行", "MASTERCARD", "DEBIT", 10));
    FALLBACK_BANK_BINS.put(
        "MC_CREDIT", new BinInfo("5222", 16, "万事达银行", "MASTERCARD", "CREDIT", 10));

    // AMEX 美国运通
    FALLBACK_BANK_BINS.put("AMEX_CREDIT", new BinInfo("3411", 15, "美国运通", "AMEX", "CREDIT", 8));
    FALLBACK_BANK_BINS.put("AMEX_CORPORATE", new BinInfo("3711", 15, "美国运通", "AMEX", "CREDIT", 5));

    // JCB 日本信用卡
    FALLBACK_BANK_BINS.put("JCB_CREDIT", new BinInfo("3511", 16, "JCB银行", "JCB", "CREDIT", 5));
  }

  /** 卡组织前缀。 */
  private static final Map<String, List<String>> CARD_ORGANIZATION_PREFIXES = new HashMap<>();

  static {
    CARD_ORGANIZATION_PREFIXES.put("VISA", Arrays.asList("4"));
    CARD_ORGANIZATION_PREFIXES.put("MASTERCARD", Arrays.asList("5"));
    CARD_ORGANIZATION_PREFIXES.put("UNIONPAY", Arrays.asList("62"));
    CARD_ORGANIZATION_PREFIXES.put("AMEX", Arrays.asList("34", "37"));
    CARD_ORGANIZATION_PREFIXES.put("JCB", Arrays.asList("35"));
  }

  @Override
  public String getType() {
    return "bankcard";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 延迟加载数据
      ensureDataLoaded(config);

      // 从参数中获取卡类型
      String cardType = getStringParam(config, "type", "BOTH");

      // 从参数中获取银行代码
      String bankCode = getStringParam(config, "bank", null);

      // 从参数中获取卡组织
      String issuer = getStringParam(config, "issuer", "ANY");

      // 从参数中获取是否生成有效卡号
      boolean valid = getBooleanParam(config, "valid", true);

      // 从参数中获取无效类型（当 valid=false 时）
      String invalidType = getStringParam(config, "invalid_type", null);

      // 从参数中获取卡号长度
      String lengthStr = getStringParam(config, "length", null);
      Integer length = lengthStr != null ? Integer.parseInt(lengthStr) : null;

      // 从参数中获取是否使用权重选择
      boolean useWeight = getBooleanParam(config, "use_weight", true);

      if (!valid) {
        return generateInvalidBankCard(invalidType);
      }

      String cardNumber = generateValidBankCard(cardType, bankCode, issuer, length, useWeight);

      // 将生成的银行卡信息放入上下文
      context.put("bankcard", cardNumber);
      context.put("bankcard_masked", maskCardNumber(cardNumber));

      return cardNumber;

    } catch (Exception e) {
      logger.error("Failed to generate bank card number", e);
      // 使用fallback数据生成随机银行卡号
      try {
        initializeFallbackData();
        return generateValidBankCard("BOTH", null, "ANY", null, false);
      } catch (Exception fallbackError) {
        logger.error("Fallback generation also failed", fallbackError);
        // 最后的fallback - 生成基本的有效卡号
        return generateBasicValidCard();
      }
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  /**
   * 确保数据已加载。
   *
   * @param config 配置
   */
  private void ensureDataLoaded(FieldConfig config) {
    if (bankBins == null) {
      synchronized (this) {
        if (bankBins == null) {
          loadData(config);
        }
      }
    }
  }

  /**
   * 加载BIN码数据。
   *
   * @param config 配置
   */
  private void loadData(FieldConfig config) {
    try {
      // 检查是否有自定义数据文件路径
      String customBinsPath = getStringParam(config, "bank_bins_file", null);

      List<String> lines;
      if (customBinsPath != null) {
        lines = DataLoader.loadDataFromFile(customBinsPath);
      } else {
        lines = DataLoader.loadDataFromResource(BANK_BINS_PATH);
      }

      bankBins = new HashMap<>();
      binsByBank = new HashMap<>();
      binsByOrganization = new HashMap<>();
      binsByType = new HashMap<>();

      for (String line : lines) {
        String[] parts = line.split(":");
        if (parts.length >= 4) {
          String prefix = parts[0].trim();
          String bankName = parts[1].trim();
          String organization = parts[2].trim();
          String cardType = parts[3].trim();
          int weight = parts.length > 4 ? parseWeight(parts[4].trim()) : 1;

          // 根据前缀确定卡号长度
          int length = determineCardLength(prefix, organization);

          BinInfo info = new BinInfo(prefix, length, bankName, organization, cardType, weight);
          bankBins.put(prefix, info);

          binsByBank.computeIfAbsent(bankName, k -> new java.util.ArrayList<>()).add(prefix);
          binsByOrganization
              .computeIfAbsent(organization, k -> new java.util.ArrayList<>())
              .add(prefix);
          binsByType.computeIfAbsent(cardType, k -> new java.util.ArrayList<>()).add(prefix);
        }
      }

      // 如果加载失败，使用fallback数据
      if (bankBins.isEmpty()) {
        initializeFallbackData();
      }

      logger.info(
          "Bank BIN data loaded - Total BINs: {}, Banks: {}, Organizations: {}",
          bankBins.size(),
          binsByBank.keySet().size(),
          binsByOrganization.keySet().size());

    } catch (Exception e) {
      logger.error("Failed to load bank BIN data, using fallback", e);
      initializeFallbackData();
    }
  }

  /**
   * 解析权重值。
   *
   * @param weightStr 权重字符串
   * @return 权重值
   */
  private int parseWeight(String weightStr) {
    try {
      return Integer.parseInt(weightStr);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  /**
   * 根据前缀和卡组织确定卡号长度。
   *
   * @param prefix 前缀
   * @param organization 卡组织
   * @return 卡号长度
   */
  private int determineCardLength(String prefix, String organization) {
    // 根据卡组织和前缀确定标准长度
    if (organization == null || organization.trim().isEmpty()) {
      return 16; // 默认长度
    }
    return switch (organization.toUpperCase()) {
      case "VISA" -> 16;
      case "MASTERCARD" -> 16;
      case "UNIONPAY" -> prefix.startsWith("62") ? 19 : 16;
      case "AMEX" -> 15;
      case "JCB" -> 16;
      default -> 16;
    };
  }

  /** 初始化fallback数据。 */
  private void initializeFallbackData() {
    bankBins = new HashMap<>();
    binsByBank = new HashMap<>();
    binsByOrganization = new HashMap<>();
    binsByType = new HashMap<>();

    // 添加fallback数据
    for (Map.Entry<String, BinInfo> entry : FALLBACK_BANK_BINS.entrySet()) {
      BinInfo info = entry.getValue();
      bankBins.put(info.prefix, info);

      binsByBank.computeIfAbsent(info.bankName, k -> new java.util.ArrayList<>()).add(info.prefix);
      binsByOrganization
          .computeIfAbsent(info.organization, k -> new java.util.ArrayList<>())
          .add(info.prefix);
      binsByType.computeIfAbsent(info.cardType, k -> new java.util.ArrayList<>()).add(info.prefix);
    }
  }

  /**
   * 生成有效的银行卡号。
   *
   * @param cardType 卡类型
   * @param bankCode 银行代码
   * @param issuer 卡组织
   * @param length 卡号长度
   * @param useWeight 是否使用权重选择
   * @return 有效的银行卡号
   */
  private String generateValidBankCard(
      String cardType, String bankCode, String issuer, Integer length, boolean useWeight) {
    BinInfo binInfo = selectBinInfo(cardType, bankCode, issuer, useWeight);

    if (binInfo == null) {
      logger.warn("No matching BIN found for issuer: {}, using fallback generation", issuer);
      // 使用基于卡组织的fallback逻辑
      return generateFallbackCardForIssuer(issuer, length);
    }

    // 使用指定长度或BIN默认长度
    int cardLength = length != null ? length : binInfo.length;

    // 确保长度在合理范围内
    if (cardLength < 13 || cardLength > 19) {
      logger.warn("Invalid card length: {}, using default: {}", cardLength, binInfo.length);
      cardLength = binInfo.length;
    }

    // 生成卡号前缀
    String prefix = binInfo.prefix;

    // 生成中间随机数字
    StringBuilder cardNumber = new StringBuilder(prefix);
    int remainingDigits = cardLength - prefix.length() - 1; // 减去1位校验位

    for (int i = 0; i < remainingDigits; i++) {
      cardNumber.append(ThreadLocalRandom.current().nextInt(10));
    }

    // 使用Luhn算法生成校验位
    String partialNumber = cardNumber.toString();
    int checkDigit = luhnValidator.generateCheckDigit(partialNumber);
    cardNumber.append(checkDigit);

    String result = cardNumber.toString();

    // 验证生成的卡号
    if (!luhnValidator.isValid(result)) {
      logger.error("Generated invalid card number: {}", maskCardNumber(result));
      // 重新生成
      return generateValidBankCard(cardType, bankCode, issuer, length, useWeight);
    }

    logger.debug(
        "Generated valid bank card: {} ({} {})",
        maskCardNumber(result),
        binInfo.bankName,
        binInfo.cardType);
    return result;
  }

  /**
   * 生成无效的银行卡号。
   *
   * @param invalidType 无效类型（NON_NUMERIC / WRONG_LENGTH / WRONG_CHECKSUM / OTHER），为 null 时随机
   * @return 无效的银行卡号
   */
  private String generateInvalidBankCard(String invalidType) {
    if (invalidType != null && !invalidType.trim().isEmpty()) {
      switch (invalidType.trim().toUpperCase()) {
        case "NON_NUMERIC":
          return generateNonNumericCard();
        case "WRONG_LENGTH":
          return generateWrongLengthCard();
        case "WRONG_CHECKSUM":
          return generateWrongChecksumCard();
        case "OTHER":
          return generateOtherInvalidCard();
        default:
          break;
      }
    }
    int type = ThreadLocalRandom.current().nextInt(4);

    return switch (type) {
      case 0 -> generateWrongLengthCard();
      case 1 -> generateWrongChecksumCard();
      case 2 -> generateNonNumericCard();
      default -> generateOtherInvalidCard();
    };
  }

  /**
   * 生成长度错误的银行卡号。
   *
   * @return 长度错误的银行卡号
   */
  private String generateWrongLengthCard() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    int length =
        random.nextBoolean()
            ? random.nextInt(5) + 8
            : // 8-12位
            random.nextInt(5) + 20; // 20-24位

    StringBuilder card = new StringBuilder();
    for (int i = 0; i < length; i++) {
      card.append(random.nextInt(10));
    }

    return ensureNumericCardFailsLuhn(card.toString());
  }

  /**
   * 生成校验位错误的银行卡号。
   *
   * @return 校验位错误的银行卡号
   */
  private String generateWrongChecksumCard() {
    // 先生成一个有效的卡号
    String validCard = generateValidBankCard("DEBIT", null, "ANY", 16, false);

    // 修改中间的一位数字（倒数第3位），确保不会意外产生有效的校验和
    // 因为改变中间数字会改变整个Luhn校验和
    StringBuilder invalidCard = new StringBuilder(validCard);
    int positionToChange = validCard.length() - 3; // 改变倒数第3位数字
    int currentDigit = Character.getNumericValue(validCard.charAt(positionToChange));
    int wrongDigit = (currentDigit + 1) % 10;
    invalidCard.setCharAt(positionToChange, (char) ('0' + wrongDigit));

    // 验证确实无效
    if (!luhnValidator.isValid(invalidCard.toString())) {
      return invalidCard.toString();
    }

    // 如果不幸还是有效，修改最后一位
    int lastDigit = Character.getNumericValue(validCard.charAt(validCard.length() - 1));
    int wrongLastDigit = (lastDigit + 1) % 10;
    invalidCard.setCharAt(validCard.length() - 1, (char) ('0' + wrongLastDigit));

    return invalidCard.toString();
  }

  /**
   * 生成包含非数字字符的银行卡号。
   *
   * @return 包含非数字字符的银行卡号
   */
  private String generateNonNumericCard() {
    StringBuilder card = new StringBuilder();
    ThreadLocalRandom random = ThreadLocalRandom.current();

    // 生成16位，其中随机位置包含字母
    for (int i = 0; i < 16; i++) {
      if (random.nextInt(5) == 0) { // 20%概率插入字母
        card.append((char) ('A' + random.nextInt(26)));
      } else {
        card.append(random.nextInt(10));
      }
    }

    return card.toString();
  }

  /**
   * 生成其他类型的无效银行卡号。
   *
   * @return 其他无效银行卡号
   */
  private String generateOtherInvalidCard() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    // 生成全0或全相同数字的卡号
    if (random.nextBoolean()) {
      return ensureNumericCardFailsLuhn("0000000000000000");
    } else {
      int digit = random.nextInt(10);
      return ensureNumericCardFailsLuhn(String.valueOf(digit).repeat(16));
    }
  }

  /** 确保数字卡号不会偶然通过Luhn校验。 */
  private String ensureNumericCardFailsLuhn(String cardNumber) {
    if (!luhnValidator.isValid(cardNumber)) {
      return cardNumber;
    }

    StringBuilder invalidCard = new StringBuilder(cardNumber);
    int lastIndex = invalidCard.length() - 1;
    int lastDigit = Character.getNumericValue(invalidCard.charAt(lastIndex));
    invalidCard.setCharAt(lastIndex, (char) ('0' + ((lastDigit + 1) % 10)));
    return invalidCard.toString();
  }

  /**
   * 选择BIN信息。
   *
   * @param cardType 卡类型
   * @param bankCode 银行代码
   * @param issuer 卡组织
   * @param useWeight 是否使用权重选择
   * @return BIN信息
   */
  private BinInfo selectBinInfo(
      String cardType, String bankCode, String issuer, boolean useWeight) {
    List<BinInfo> candidates =
        bankBins.values().stream()
            .filter(bin -> matchesCardType(bin, cardType))
            .filter(bin -> matchesBankCode(bin, bankCode))
            .filter(bin -> matchesIssuer(bin, issuer))
            .collect(Collectors.toList());

    if (candidates.isEmpty()) {
      return null;
    }

    if (!useWeight || candidates.size() == 1) {
      return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    // 使用权重选择
    Map<String, Integer> weightMap =
        candidates.stream().collect(Collectors.toMap(bin -> bin.prefix, bin -> bin.weight));

    String selectedPrefix = DataLoader.selectByWeight(weightMap, ThreadLocalRandom.current());
    return bankBins.get(selectedPrefix);
  }

  /**
   * 检查是否匹配卡类型。
   *
   * @param binInfo BIN信息
   * @param cardType 卡类型
   * @return 是否匹配
   */
  private boolean matchesCardType(BinInfo binInfo, String cardType) {
    if (cardType == null || "BOTH".equalsIgnoreCase(cardType) || "ANY".equalsIgnoreCase(cardType)) {
      return true;
    }

    return cardType.equalsIgnoreCase(binInfo.cardType);
  }

  /**
   * 检查是否匹配银行代码。
   *
   * @param binInfo BIN信息
   * @param bankCode 银行代码
   * @return 是否匹配
   */
  private boolean matchesBankCode(BinInfo binInfo, String bankCode) {
    if (bankCode == null || "ANY".equalsIgnoreCase(bankCode)) {
      return true;
    }

    return bankCode.equalsIgnoreCase(binInfo.bankName);
  }

  /**
   * 检查是否匹配卡组织。
   *
   * @param binInfo BIN信息
   * @param issuer 卡组织
   * @return 是否匹配
   */
  private boolean matchesIssuer(BinInfo binInfo, String issuer) {
    if (issuer == null || "ANY".equalsIgnoreCase(issuer)) {
      return true;
    }

    return issuer.equalsIgnoreCase(binInfo.organization);
  }

  /**
   * 掩码银行卡号用于日志记录。
   *
   * @param cardNumber 原始卡号
   * @return 掩码后的卡号
   */
  private String maskCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 8) {
      return "****";
    }

    // 显示前4位和后4位，中间用*代替
    String prefix = cardNumber.substring(0, 4);
    String suffix = cardNumber.substring(cardNumber.length() - 4);
    int maskLength = cardNumber.length() - 8;
    String mask = "*".repeat(Math.max(0, maskLength));

    return prefix + mask + suffix;
  }

  /**
   * 获取BIN码库统计信息。
   *
   * @return 统计信息
   */
  public String getBinStats() {
    ensureDataLoaded(null);

    StringBuilder stats = new StringBuilder();
    stats.append("Total BINs: ").append(bankBins.size()).append("\n");

    for (Map.Entry<String, List<String>> entry : binsByBank.entrySet()) {
      stats.append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" BINs\n");
    }

    stats.append("\nOrganizations:\n");
    for (Map.Entry<String, List<String>> entry : binsByOrganization.entrySet()) {
      stats.append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" BINs\n");
    }

    // 计算理论组合数（每个BIN可生成数十亿个卡号）
    long totalCombinations = 0;
    for (BinInfo bin : bankBins.values()) {
      // 每个BIN根据其长度可生成的卡号数量
      int remainingDigits = bin.length - bin.prefix.length() - 1; // 减去前缀和校验位
      long combinations = (long) Math.pow(10, remainingDigits);
      totalCombinations += combinations;
    }

    stats.append("\nTotal possible combinations: ").append(String.format("%,d", totalCombinations));

    return stats.toString();
  }

  /**
   * 生成基本的有效银行卡号作为最后的fallback。 使用简单的VISA前缀确保能够生成有效卡号。
   *
   * @return 基本有效银行卡号
   */
  private String generateBasicValidCard() {
    // 使用最通用的VISA前缀
    String prefix = "4";
    int cardLength = 16;

    StringBuilder cardNumber = new StringBuilder(prefix);
    int remainingDigits = cardLength - prefix.length() - 1; // 减去1位校验位

    // 生成随机中间数字
    for (int i = 0; i < remainingDigits; i++) {
      cardNumber.append(ThreadLocalRandom.current().nextInt(10));
    }

    // 计算Luhn校验位
    String partialNumber = cardNumber.toString();
    int checkDigit = calculateLuhnCheckDigit(partialNumber);
    cardNumber.append(checkDigit);

    return cardNumber.toString();
  }

  /** 计算Luhn校验位（简化版本，用于fallback）。 */
  private int calculateLuhnCheckDigit(String partialNumber) {
    int sum = 0;
    boolean alternate = true;

    for (int i = partialNumber.length() - 1; i >= 0; i--) {
      int digit = Character.getNumericValue(partialNumber.charAt(i));

      if (alternate) {
        digit *= 2;
        if (digit > 9) {
          digit = (digit % 10) + 1;
        }
      }

      sum += digit;
      alternate = !alternate;
    }

    return (10 - (sum % 10)) % 10;
  }

  /**
   * 为指定卡组织生成fallback银行卡号。
   *
   * @param issuer 卡组织
   * @param length 卡号长度
   * @return 生成的银行卡号
   */
  private String generateFallbackCardForIssuer(String issuer, Integer length) {
    String prefix;
    int defaultLength;

    // 根据卡组织选择合适的前缀和长度
    if (issuer != null) {
      switch (issuer.toUpperCase()) {
        case "MASTERCARD":
          prefix = "5";
          defaultLength = 16;
          break;
        case "AMEX":
          prefix = "34";
          defaultLength = 15;
          break;
        case "UNIONPAY":
          prefix = "62";
          defaultLength = 19;
          break;
        case "JCB":
          prefix = "35";
          defaultLength = 16;
          break;
        case "VISA":
        default:
          prefix = "4";
          defaultLength = 16;
          break;
      }
    } else {
      prefix = "4"; // 默认VISA
      defaultLength = 16;
    }

    int cardLength = length != null ? length : defaultLength;

    // 确保长度在合理范围内
    if (cardLength < 13 || cardLength > 19) {
      cardLength = defaultLength;
    }

    StringBuilder cardNumber = new StringBuilder(prefix);
    int remainingDigits = cardLength - prefix.length() - 1; // 减去1位校验位

    // 生成随机中间数字
    for (int i = 0; i < remainingDigits; i++) {
      cardNumber.append(ThreadLocalRandom.current().nextInt(10));
    }

    // 计算Luhn校验位
    String partialNumber = cardNumber.toString();
    int checkDigit = calculateLuhnCheckDigit(partialNumber);
    cardNumber.append(checkDigit);

    return cardNumber.toString();
  }

  @Override
  public String getDescription() {
    return "Bank card number generator - generates valid bank card numbers with Luhn algorithm and"
        + " comprehensive BIN support";
  }

  /** BIN信息类。 */
  private static class BinInfo {
    final String prefix;
    final int length;
    final String bankName;
    final String organization;
    final String cardType;
    final int weight;

    BinInfo(
        String prefix,
        int length,
        String bankName,
        String organization,
        String cardType,
        int weight) {
      this.prefix = prefix;
      this.length = length;
      this.bankName = bankName;
      this.organization = organization;
      this.cardType = cardType;
      this.weight = weight;
    }
  }
}
