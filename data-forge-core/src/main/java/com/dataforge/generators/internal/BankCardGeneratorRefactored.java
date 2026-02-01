package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.validation.LuhnValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 银行卡号生成器（重构版本）。
 *
 * <p>使用 {@link BaseDataLoadingGenerator} 基类，简化数据加载逻辑。 生成符合Luhn算法的银行卡号，支持大规模卡号生成。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class BankCardGeneratorRefactored extends BaseDataLoadingGenerator<String>
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(BankCardGeneratorRefactored.class);

  // LuhnValidator 保留用于后续扩展验证功能
  @SuppressWarnings("unused")
  private final LuhnValidator luhnValidator;

  public BankCardGeneratorRefactored() {
    this.luhnValidator = new LuhnValidator();
  }

  /** BIN码数据文件路径。 */
  private static final String BANK_BINS_PATH = "data/bank-bins.txt";

  /** 缓存的BIN码数据。 */
  private Map<String, BinInfo> bankBins;

  private Map<String, List<String>> binsByBank;
  private Map<String, List<String>> binsByOrganization;
  private Map<String, List<String>> binsByType;

  /** Fallback BIN码数据（当文件加载失败时使用）。 */
  private static final Map<String, BinInfo> FALLBACK_BANK_BINS = new HashMap<>();

  static {
    // UNIONPAY 银联卡
    FALLBACK_BANK_BINS.put(
        "ICBC_DEBIT", new BinInfo("622202", 19, "工商银行", "UNIONPAY", "DEBIT", 15));
    FALLBACK_BANK_BINS.put(
        "ICBC_CREDIT", new BinInfo("625330", 16, "工商银行", "UNIONPAY", "CREDIT", 12));
    FALLBACK_BANK_BINS.put("CCB_DEBIT", new BinInfo("621700", 19, "建设银行", "UNIONPAY", "DEBIT", 12));
    FALLBACK_BANK_BINS.put(
        "CCB_CREDIT", new BinInfo("625362", 16, "建设银行", "UNIONPAY", "CREDIT", 10));
    FALLBACK_BANK_BINS.put("ABC_DEBIT", new BinInfo("622848", 19, "农业银行", "UNIONPAY", "DEBIT", 12));

    // VISA 卡
    FALLBACK_BANK_BINS.put("VISA_DEBIT", new BinInfo("4111", 16, "VISA银行", "VISA", "DEBIT", 10));
    FALLBACK_BANK_BINS.put("VISA_CREDIT", new BinInfo("4222", 16, "VISA银行", "VISA", "CREDIT", 10));

    // MASTERCARD 万事达卡
    FALLBACK_BANK_BINS.put("MC_DEBIT", new BinInfo("5111", 16, "万事达银行", "MASTERCARD", "DEBIT", 10));
    FALLBACK_BANK_BINS.put(
        "MC_CREDIT", new BinInfo("5222", 16, "万事达银行", "MASTERCARD", "CREDIT", 10));

    // AMEX 美国运通
    FALLBACK_BANK_BINS.put("AMEX_CREDIT", new BinInfo("3411", 15, "美国运通", "AMEX", "CREDIT", 8));
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
      // 使用基类的延迟加载机制
      ensureDataLoaded();

      String cardType = getStringParam(config, "type", "BOTH");
      String bankCode = getStringParam(config, "bank", null);
      String issuer = getStringParam(config, "issuer", "ANY");
      boolean valid = getBooleanParam(config, "valid", true);
      String lengthStr = getStringParam(config, "length", null);
      Integer length = lengthStr != null ? Integer.parseInt(lengthStr) : null;
      boolean useWeight = getBooleanParam(config, "use_weight", true);

      if (!valid) {
        return generateInvalidBankCard();
      }

      String cardNumber = generateValidBankCard(cardType, bankCode, issuer, length, useWeight);

      context.put("bankcard", cardNumber);
      context.put("bankcard_masked", maskCardNumber(cardNumber));

      return cardNumber;

    } catch (Exception e) {
      logger.error("Failed to generate bank card number", e);
      return generateBasicValidCard();
    }
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  // ==================== BaseDataLoadingGenerator 抽象方法实现 ====================

  @Override
  protected String getDataFilePath() {
    return BANK_BINS_PATH;
  }

  @Override
  protected void parseData(List<String> lines) {
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

        int length = determineCardLength(prefix, organization);

        BinInfo info = new BinInfo(prefix, length, bankName, organization, cardType, weight);
        bankBins.put(prefix, info);

        binsByBank.computeIfAbsent(bankName, k -> new ArrayList<>()).add(prefix);
        binsByOrganization.computeIfAbsent(organization, k -> new ArrayList<>()).add(prefix);
        binsByType.computeIfAbsent(cardType, k -> new ArrayList<>()).add(prefix);
      }
    }

    logger.info(
        "Bank BIN data loaded - Total BINs: {}, Banks: {}, Organizations: {}",
        bankBins.size(),
        binsByBank.keySet().size(),
        binsByOrganization.keySet().size());
  }

  @Override
  protected void initializeFallbackData() {
    bankBins = new HashMap<>(FALLBACK_BANK_BINS);
    binsByBank = new HashMap<>();
    binsByOrganization = new HashMap<>();
    binsByType = new HashMap<>();

    for (BinInfo info : bankBins.values()) {
      binsByBank.computeIfAbsent(info.bankName, k -> new ArrayList<>()).add(info.prefix);
      binsByOrganization
          .computeIfAbsent(info.organization, k -> new ArrayList<>())
          .add(info.prefix);
      binsByType.computeIfAbsent(info.cardType, k -> new ArrayList<>()).add(info.prefix);
    }

    logger.info("Bank BIN fallback data initialized - Total BINs: {}", bankBins.size());
  }

  // ==================== 私有辅助方法 ====================

  private int parseWeight(String weightStr) {
    try {
      return Integer.parseInt(weightStr);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  private int determineCardLength(String prefix, String organization) {
    return switch (organization.toUpperCase()) {
      case "VISA" -> 16;
      case "MASTERCARD" -> 16;
      case "UNIONPAY" -> prefix.startsWith("62") ? 19 : 16;
      case "AMEX" -> 15;
      case "JCB" -> 16;
      default -> 16;
    };
  }

  private String generateValidBankCard(
      String cardType, String bankCode, String issuer, Integer length, boolean useWeight) {
    BinInfo binInfo = selectBinInfo(cardType, bankCode, issuer, useWeight);

    if (binInfo == null) {
      binInfo = getRandomBinInfo();
    }

    int cardLength = length != null ? length : binInfo.length;
    String prefix = binInfo.prefix;

    StringBuilder cardNumber = new StringBuilder(prefix);
    int remainingLength = cardLength - prefix.length() - 1;

    for (int i = 0; i < remainingLength; i++) {
      cardNumber.append(ThreadLocalRandom.current().nextInt(10));
    }

    String checkDigit = calculateLuhnCheckDigit(cardNumber.toString());
    cardNumber.append(checkDigit);

    return cardNumber.toString();
  }

  private String generateInvalidBankCard() {
    StringBuilder cardNumber = new StringBuilder("622202");
    for (int i = 0; i < 12; i++) {
      cardNumber.append(ThreadLocalRandom.current().nextInt(10));
    }
    cardNumber.append("0");
    return cardNumber.toString();
  }

  private String generateBasicValidCard() {
    return generateValidBankCard("DEBIT", null, "UNIONPAY", 19, false);
  }

  private BinInfo selectBinInfo(
      String cardType, String bankCode, String issuer, boolean useWeight) {
    List<String> candidates = new ArrayList<>();

    if (bankCode != null && binsByBank.containsKey(bankCode)) {
      candidates.addAll(binsByBank.get(bankCode));
    } else if (issuer != null
        && !"ANY".equalsIgnoreCase(issuer)
        && binsByOrganization.containsKey(issuer.toUpperCase())) {
      candidates.addAll(binsByOrganization.get(issuer.toUpperCase()));
    } else if (cardType != null
        && !"BOTH".equalsIgnoreCase(cardType)
        && binsByType.containsKey(cardType.toUpperCase())) {
      candidates.addAll(binsByType.get(cardType.toUpperCase()));
    }

    if (candidates.isEmpty()) {
      candidates.addAll(bankBins.keySet());
    }

    if (candidates.isEmpty()) {
      return null;
    }

    if (useWeight) {
      return selectByWeight(candidates);
    } else {
      String selectedPrefix =
          candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
      return bankBins.get(selectedPrefix);
    }
  }

  private BinInfo selectByWeight(List<String> candidates) {
    int totalWeight = candidates.stream().mapToInt(prefix -> bankBins.get(prefix).weight).sum();
    int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);

    int currentWeight = 0;
    for (String prefix : candidates) {
      currentWeight += bankBins.get(prefix).weight;
      if (randomWeight < currentWeight) {
        return bankBins.get(prefix);
      }
    }

    return bankBins.get(candidates.get(candidates.size() - 1));
  }

  private BinInfo getRandomBinInfo() {
    List<String> keys = new ArrayList<>(bankBins.keySet());
    String randomKey = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
    return bankBins.get(randomKey);
  }

  private String calculateLuhnCheckDigit(String prefix) {
    int sum = 0;
    boolean alternate = false;

    for (int i = prefix.length() - 1; i >= 0; i--) {
      int n = prefix.charAt(i) - '0';
      if (alternate) {
        n *= 2;
        if (n > 9) {
          n -= 9;
        }
      }
      sum += n;
      alternate = !alternate;
    }

    return String.valueOf((10 - (sum % 10)) % 10);
  }

  private String maskCardNumber(String cardNumber) {
    if (cardNumber.length() < 8) {
      return cardNumber;
    }
    return cardNumber.substring(0, 4)
        + " **** **** "
        + cardNumber.substring(cardNumber.length() - 4);
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
