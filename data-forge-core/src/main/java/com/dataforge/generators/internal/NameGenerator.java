package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 优化版姓名生成器。
 *
 * <p>生成中文或英文姓名，支持大规模唯一姓名生成。 支持性别关联、权重选择和自定义姓名库。 通过配置文件管理姓名数据，支持生成1亿+唯一姓名。
 *
 * @author DataForge Team
 * @since 1.0.0
 */
@Component
public class NameGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(NameGenerator.class);

  /** 数据文件路径常量。 */
  private static final String CHINESE_SURNAMES_PATH = "data/chinese-surnames.txt";

  private static final String CHINESE_MALE_NAMES_PATH = "data/chinese-male-names.txt";
  private static final String CHINESE_FEMALE_NAMES_PATH = "data/chinese-female-names.txt";
  private static final String ENGLISH_FIRST_NAMES_PATH = "data/english-first-names.txt";
  private static final String ENGLISH_LAST_NAMES_PATH = "data/english-last-names.txt";

  /** 缓存的数据列表。 */
  private volatile List<String> chineseSurnames;

  private volatile List<String> chineseMaleNames;
  private volatile List<String> chineseFemaleNames;
  private volatile List<String> englishFirstNames;
  private volatile List<String> englishLastNames;

  /** 缓存的权重数据。 */
  private volatile Map<String, Integer> chineseSurnameWeights;

  @Override
  public String getType() {
    return "name";
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 延迟加载数据
      ensureDataLoaded(config);

      // 从参数中获取名字类型，默认为中文
      String nameType = getStringParam(config, "type", "CN");

      // 从参数中获取性别，默认为随机
      String gender = getStringParam(config, "gender", "ANY");

      // 如果上下文中有性别信息，优先使用
      String contextGender = context.get("gender", String.class).orElse(null);
      if (contextGender != null) {
        gender = contextGender;
      }

      // 从参数中获取是否使用权重选择
      boolean useWeight = getBooleanParam(config, "use_weight", true);

      String generatedName =
          switch (nameType.toUpperCase()) {
            case "CN" -> generateChineseName(gender, useWeight, config);
            case "EN" -> generateEnglishName(gender, config);
            case "BOTH" -> ThreadLocalRandom.current().nextBoolean()
                ? generateChineseName(gender, useWeight, config)
                : generateEnglishName(gender, config);
            default -> {
              logger.warn("Unknown name type: {}, using CN", nameType);
              yield generateChineseName(gender, useWeight, config);
            }
          };

      // 将生成的姓名信息放入上下文
      context.put("name", generatedName);
      context.put("name_type", nameType);

      return generatedName;

    } catch (Exception e) {
      logger.error("Failed to generate name", e);
      // 返回一个默认名字作为fallback
      return "张三";
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
    if (chineseSurnames == null) {
      synchronized (this) {
        if (chineseSurnames == null) {
          loadData(config);
        }
      }
    }
  }

  /**
   * 加载姓名数据。
   *
   * @param config 配置
   */
  private void loadData(FieldConfig config) {
    try {
      // 检查是否有自定义数据文件路径
      String customSurnamesPath = getStringParam(config, "cn_surname_file", null);
      String customMaleNamesPath = getStringParam(config, "cn_male_names_file", null);
      String customFemaleNamesPath = getStringParam(config, "cn_female_names_file", null);
      String customEnglishFirstPath = getStringParam(config, "en_first_names_file", null);
      String customEnglishLastPath = getStringParam(config, "en_last_names_file", null);

      // 加载中文姓氏（支持权重）
      if (customSurnamesPath != null) {
        chineseSurnames = DataLoader.loadDataFromFile(customSurnamesPath);
        chineseSurnameWeights = loadWeightedDataFromFile(customSurnamesPath);
      } else {
        chineseSurnames = DataLoader.loadDataFromResource(CHINESE_SURNAMES_PATH);
        chineseSurnameWeights = DataLoader.loadWeightedDataFromResource(CHINESE_SURNAMES_PATH);
      }

      // 加载中文男性名字
      if (customMaleNamesPath != null) {
        chineseMaleNames = DataLoader.loadDataFromFile(customMaleNamesPath);
      } else {
        chineseMaleNames = DataLoader.loadDataFromResource(CHINESE_MALE_NAMES_PATH);
      }

      // 加载中文女性名字
      if (customFemaleNamesPath != null) {
        chineseFemaleNames = DataLoader.loadDataFromFile(customFemaleNamesPath);
      } else {
        chineseFemaleNames = DataLoader.loadDataFromResource(CHINESE_FEMALE_NAMES_PATH);
      }

      // 加载英文名字
      if (customEnglishFirstPath != null) {
        englishFirstNames = DataLoader.loadDataFromFile(customEnglishFirstPath);
      } else {
        englishFirstNames = DataLoader.loadDataFromResource(ENGLISH_FIRST_NAMES_PATH);
      }

      // 加载英文姓氏
      if (customEnglishLastPath != null) {
        englishLastNames = DataLoader.loadDataFromFile(customEnglishLastPath);
      } else {
        englishLastNames = DataLoader.loadDataFromResource(ENGLISH_LAST_NAMES_PATH);
      }

      logger.info(
          "Name data loaded - Chinese surnames: {}, Male names: {}, Female names: {}, English"
              + " first: {}, English last: {}",
          chineseSurnames.size(),
          chineseMaleNames.size(),
          chineseFemaleNames.size(),
          englishFirstNames.size(),
          englishLastNames.size());

    } catch (Exception e) {
      logger.error("Failed to load name data, using fallback", e);
      initializeFallbackData();
    }
  }

  /**
   * 从文件加载权重数据。
   *
   * @param filePath 文件路径
   * @return 权重映射
   */
  private Map<String, Integer> loadWeightedDataFromFile(String filePath) {
    try {
      return DataLoader.loadWeightedDataFromResource(filePath);
    } catch (Exception e) {
      logger.warn("Failed to load weighted data from: {}", filePath, e);
      return new java.util.HashMap<>();
    }
  }

  /** 初始化fallback数据。 */
  private void initializeFallbackData() {
    chineseSurnames = java.util.Arrays.asList("王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴");
    chineseMaleNames = java.util.Arrays.asList("伟", "强", "磊", "军", "勇", "杰", "涛", "明", "超", "辉");
    chineseFemaleNames = java.util.Arrays.asList("丽", "娟", "敏", "静", "洁", "华", "秀", "兰", "红", "霞");
    englishFirstNames =
        java.util.Arrays.asList(
            "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda");
    englishLastNames =
        java.util.Arrays.asList(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis");
    chineseSurnameWeights = new java.util.HashMap<>();

    logger.info(
        "Initialized fallback name data - Chinese surnames: {}, Male names: {}, Female names: {}",
        chineseSurnames.size(),
        chineseMaleNames.size(),
        chineseFemaleNames.size());
  }

  /**
   * 生成中文姓名。
   *
   * @param gender 性别
   * @param useWeight 是否使用权重选择
   * @param config 配置
   * @return 中文姓名
   */
  private String generateChineseName(String gender, boolean useWeight, FieldConfig config) {
    // 选择姓氏
    String surname = selectChineseSurname(useWeight);

    // 选择名字
    String givenName = selectChineseGivenName(gender, config);

    return surname + givenName;
  }

  /**
   * 选择中文姓氏。
   *
   * @param useWeight 是否使用权重选择
   * @return 姓氏
   */
  private String selectChineseSurname(boolean useWeight) {
    if (useWeight && chineseSurnameWeights != null && !chineseSurnameWeights.isEmpty()) {
      return DataLoader.selectByWeight(chineseSurnameWeights, ThreadLocalRandom.current());
    }

    if (chineseSurnames == null || chineseSurnames.isEmpty()) {
      return "王";
    }

    return chineseSurnames.get(ThreadLocalRandom.current().nextInt(chineseSurnames.size()));
  }

  /**
   * 选择中文名字。
   *
   * @param gender 性别
   * @param config 配置
   * @return 名字
   */
  private String selectChineseGivenName(String gender, FieldConfig config) {
    List<String> namePool;

    if ("MALE".equalsIgnoreCase(gender) || "M".equalsIgnoreCase(gender)) {
      namePool = chineseMaleNames;
    } else if ("FEMALE".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender)) {
      namePool = chineseFemaleNames;
    } else {
      // 随机选择性别
      namePool = ThreadLocalRandom.current().nextBoolean() ? chineseMaleNames : chineseFemaleNames;
    }

    if (namePool == null || namePool.isEmpty()) {
      return "三";
    }

    // 支持组合名字以增加唯一性
    boolean allowCombination = getBooleanParam(config, "allow_combination", true);
    if (allowCombination && ThreadLocalRandom.current().nextDouble() < 0.3) { // 30%概率组合
      return generateCombinedChineseName(namePool);
    }

    return namePool.get(ThreadLocalRandom.current().nextInt(namePool.size()));
  }

  /**
   * 生成组合中文名字。
   *
   * @param namePool 名字池
   * @return 组合名字
   */
  private String generateCombinedChineseName(List<String> namePool) {
    // 从单字名中选择两个字组合
    List<String> singleCharNames =
        namePool.stream()
            .filter(name -> name.length() == 1)
            .collect(java.util.stream.Collectors.toList());

    if (singleCharNames.size() >= 2) {
      String first =
          singleCharNames.get(ThreadLocalRandom.current().nextInt(singleCharNames.size()));
      String second;
      do {
        second = singleCharNames.get(ThreadLocalRandom.current().nextInt(singleCharNames.size()));
      } while (first.equals(second) && singleCharNames.size() > 1);

      return first + second;
    }

    // 如果单字名不够，返回随机名字
    return namePool.get(ThreadLocalRandom.current().nextInt(namePool.size()));
  }

  /**
   * 生成英文姓名。
   *
   * @param gender 性别
   * @param config 配置
   * @return 英文姓名
   */
  private String generateEnglishName(String gender, FieldConfig config) {
    if (englishFirstNames == null
        || englishFirstNames.isEmpty()
        || englishLastNames == null
        || englishLastNames.isEmpty()) {
      return "John Smith";
    }

    String firstName =
        englishFirstNames.get(ThreadLocalRandom.current().nextInt(englishFirstNames.size()));
    String lastName =
        englishLastNames.get(ThreadLocalRandom.current().nextInt(englishLastNames.size()));

    // 支持中间名以增加唯一性
    boolean includeMiddleName = getBooleanParam(config, "include_middle_name", false);
    if (includeMiddleName && ThreadLocalRandom.current().nextDouble() < 0.2) { // 20%概率添加中间名
      String middleName =
          englishFirstNames.get(ThreadLocalRandom.current().nextInt(englishFirstNames.size()));
      return firstName + " " + middleName + " " + lastName;
    }

    return firstName + " " + lastName;
  }

  /**
   * 获取姓名库统计信息。
   *
   * @return 统计信息
   */
  public String getNameLibraryStats() {
    ensureDataLoaded(null);

    long chineseCombinations = calculateChineseCombinations();
    long englishCombinations = calculateEnglishCombinations();

    return String.format(
        "Chinese combinations: %,d, English combinations: %,d, Total: %,d",
        chineseCombinations, englishCombinations, chineseCombinations + englishCombinations);
  }

  /**
   * 计算中文姓名组合数。
   *
   * @return 组合数
   */
  private long calculateChineseCombinations() {
    if (chineseSurnames == null || chineseMaleNames == null || chineseFemaleNames == null) {
      return 0;
    }

    // 基础组合：姓氏 × (男性名字 + 女性名字)
    long basicCombinations =
        (long) chineseSurnames.size() * (chineseMaleNames.size() + chineseFemaleNames.size());

    // 单字组合：姓氏 × 单字名 × 单字名
    long singleCharMale =
        chineseMaleNames.stream().mapToLong(name -> name.length() == 1 ? 1 : 0).sum();
    long singleCharFemale =
        chineseFemaleNames.stream().mapToLong(name -> name.length() == 1 ? 1 : 0).sum();
    long combinationNames = singleCharMale * singleCharMale + singleCharFemale * singleCharFemale;
    long combinedCombinations = (long) chineseSurnames.size() * combinationNames;

    return basicCombinations + combinedCombinations;
  }

  /**
   * 计算英文姓名组合数。
   *
   * @return 组合数
   */
  private long calculateEnglishCombinations() {
    if (englishFirstNames == null || englishLastNames == null) {
      return 0;
    }

    // 基础组合：名 × 姓
    long basicCombinations = (long) englishFirstNames.size() * englishLastNames.size();

    // 中间名组合：名 × 中间名 × 姓
    long middleNameCombinations =
        (long) englishFirstNames.size() * englishFirstNames.size() * englishLastNames.size();

    return basicCombinations + middleNameCombinations;
  }

  @Override
  public String getDescription() {
    return "name generator - generates Chinese/English names with massive scale support (100M+"
        + " unique names)";
  }
}
