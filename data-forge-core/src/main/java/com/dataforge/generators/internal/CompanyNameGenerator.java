package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.dataforge.util.DataLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 企业名称生成器
 *
 * <p>支持的参数： - industry: 行业类型 (IT|FINANCE|RETAIL|MANUFACTURING|EDUCATION|HEALTHCARE|ANY) - type:
 * 公司类型 (CO_LTD|GROUP|INSTITUTE|CORP|ANY) - prefix_region: 是否添加地区前缀 (true|false) - keywords_file:
 * 自定义行业关键词文件路径 - suffix_file: 自定义公司类型后缀文件路径 - length_range: 公司名称长度范围 (如 "6,20")
 *
 * @author DataForge
 */
public class CompanyNameGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(CompanyNameGenerator.class);
  private static final Random random = new Random();

  // 行业关键词
  private static final Map<String, List<String>> INDUSTRY_KEYWORDS = new HashMap<>();

  // 公司类型后缀
  private static final Map<String, List<String>> COMPANY_TYPES = new HashMap<>();

  // 地区前缀
  private static final List<String> REGION_PREFIXES =
      Arrays.asList(
          "北京", "上海", "广州", "深圳", "杭州", "南京", "苏州", "成都", "武汉", "西安", "天津", "重庆", "青岛", "大连", "宁波",
          "厦门", "长沙", "郑州", "济南", "合肥", "福州", "昆明", "南昌", "贵阳", "太原", "石家庄", "哈尔滨", "长春", "沈阳",
          "兰州");

  // 通用词汇
  private static final List<String> COMMON_WORDS =
      Arrays.asList(
          "创新", "智能", "数字", "云端", "未来", "新兴", "优质", "精品", "卓越", "领先", "专业", "高端", "品质", "服务", "发展",
          "建设", "管理", "咨询", "投资", "贸易", "实业", "集团", "控股", "产业", "科技", "信息", "网络", "系统", "工程", "设计");

  static {
    initializeIndustryKeywords();
    initializeCompanyTypes();
  }

  private static void initializeIndustryKeywords() {
    INDUSTRY_KEYWORDS.put(
        "IT",
        Arrays.asList(
            "科技", "信息", "网络", "软件", "数据", "云计算", "人工智能", "大数据", "物联网", "区块链", "互联网", "电子", "通信",
            "计算机", "系统", "技术", "创新", "数字", "智能", "在线"));

    INDUSTRY_KEYWORDS.put(
        "FINANCE",
        Arrays.asList(
            "金融", "投资", "资本", "基金", "证券", "银行", "保险", "信托", "财富", "资产", "理财", "融资", "风控", "支付",
            "金服", "普惠", "小贷", "担保", "租赁", "期货"));

    INDUSTRY_KEYWORDS.put(
        "RETAIL",
        Arrays.asList(
            "商贸", "零售", "批发", "电商", "购物", "商城", "超市", "便利", "连锁", "品牌", "时尚", "服装", "家居", "美妆",
            "食品", "母婴", "数码", "家电", "汽车", "珠宝"));

    INDUSTRY_KEYWORDS.put(
        "MANUFACTURING",
        Arrays.asList(
            "制造", "工业", "机械", "设备", "生产", "加工", "材料", "化工", "钢铁", "有色", "纺织", "服装", "食品", "医药",
            "汽车", "船舶", "航空", "电子", "仪器", "模具"));

    INDUSTRY_KEYWORDS.put(
        "EDUCATION",
        Arrays.asList(
            "教育", "培训", "学校", "学院", "大学", "幼儿", "职业", "技能", "语言", "艺术", "体育", "音乐", "美术", "舞蹈",
            "书法", "围棋", "编程", "机器人", "科学", "实验"));

    INDUSTRY_KEYWORDS.put(
        "HEALTHCARE",
        Arrays.asList(
            "医疗", "健康", "医院", "诊所", "药业", "生物", "康复", "养老", "护理", "美容", "口腔", "眼科", "妇产", "儿科",
            "中医", "针灸", "推拿", "理疗", "体检", "疫苗"));
  }

  private static void initializeCompanyTypes() {
    COMPANY_TYPES.put("CO_LTD", Arrays.asList("有限公司", "有限责任公司"));

    COMPANY_TYPES.put("GROUP", Arrays.asList("集团有限公司", "控股集团有限公司", "产业集团有限公司"));

    COMPANY_TYPES.put("INSTITUTE", Arrays.asList("研究院", "技术研究院", "科学研究院", "工程技术研究院"));

    COMPANY_TYPES.put("CORP", Arrays.asList("股份有限公司", "实业有限公司", "投资有限公司", "贸易有限公司"));
  }

  @Override
  public String getType() {
    return "company";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String industry = config.getParam("industry", String.class, "ANY");
      String type = config.getParam("type", String.class, "ANY");
      boolean prefixRegion =
          Boolean.parseBoolean(config.getParam("prefix_region", String.class, "true"));
      String lengthRange = config.getParam("length_range", String.class, "6,20");

      // 解析长度范围
      String[] lengthParts = lengthRange.split(",");
      int minLength = Integer.parseInt(lengthParts[0].trim());
      int maxLength = lengthParts.length > 1 ? Integer.parseInt(lengthParts[1].trim()) : minLength;

      // 加载自定义数据
      List<String> keywords = loadKeywords(config, industry);
      List<String> suffixes = loadSuffixes(config, type);

      // 生成公司名称
      StringBuilder companyName = new StringBuilder();

      // 添加地区前缀
      if (prefixRegion && random.nextBoolean()) {
        String region = REGION_PREFIXES.get(random.nextInt(REGION_PREFIXES.size()));
        companyName.append(region);
      }

      // 添加主体名称
      String mainName = generateMainName(keywords, minLength, maxLength, companyName.length());
      companyName.append(mainName);

      // 添加公司类型后缀
      String suffix = suffixes.get(random.nextInt(suffixes.size()));
      companyName.append(suffix);

      String result = companyName.toString();

      // 长度控制
      if (result.length() > maxLength) {
        // 如果太长，尝试去掉地区前缀
        if (prefixRegion && companyName.toString().startsWith(REGION_PREFIXES.get(0))) {
          result = mainName + suffix;
          if (result.length() > maxLength) {
            // 如果还是太长，截断主体名称
            int maxMainLength = maxLength - suffix.length();
            if (maxMainLength > 2) {
              mainName = mainName.substring(0, Math.min(mainName.length(), maxMainLength));
              result = mainName + suffix;
            }
          }
        }
      }

      logger.debug("Generated company name: {}", result);
      return result;

    } catch (Exception e) {
      logger.error("Error generating company name", e);
      return "示例科技有限公司";
    }
  }

  private List<String> loadKeywords(FieldConfig config, String industry) {
    String keywordsFile = config.getParam("keywords_file", String.class, null);
    if (keywordsFile != null) {
      try {
        return DataLoader.loadDataFromFile(keywordsFile);
      } catch (Exception e) {
        logger.warn("Failed to load custom keywords file: {}", keywordsFile, e);
      }
    }

    // 使用内置关键词
    List<String> keywords = new ArrayList<>();
    if ("ANY".equals(industry)) {
      // 混合所有行业关键词
      INDUSTRY_KEYWORDS.values().forEach(keywords::addAll);
      keywords.addAll(COMMON_WORDS);
    } else {
      keywords.addAll(INDUSTRY_KEYWORDS.getOrDefault(industry, COMMON_WORDS));
    }

    return keywords;
  }

  private List<String> loadSuffixes(FieldConfig config, String type) {
    String suffixFile = config.getParam("suffix_file", String.class, null);
    if (suffixFile != null) {
      try {
        return DataLoader.loadDataFromFile(suffixFile);
      } catch (Exception e) {
        logger.warn("Failed to load custom suffix file: {}", suffixFile, e);
      }
    }

    // 使用内置后缀
    List<String> suffixes = new ArrayList<>();
    if ("ANY".equals(type)) {
      COMPANY_TYPES.values().forEach(suffixes::addAll);
    } else {
      suffixes.addAll(COMPANY_TYPES.getOrDefault(type, COMPANY_TYPES.get("CO_LTD")));
    }

    return suffixes;
  }

  private String generateMainName(
      List<String> keywords, int minLength, int maxLength, int prefixLength) {
    StringBuilder mainName = new StringBuilder();
    int targetLength =
        random.nextInt(Math.max(1, maxLength - prefixLength - 5))
            + Math.max(2, minLength - prefixLength);

    // 随机选择1-3个关键词组合
    int wordCount = random.nextInt(3) + 1;
    Set<String> usedWords = new HashSet<>();

    for (int i = 0; i < wordCount && mainName.length() < targetLength; i++) {
      String word = keywords.get(random.nextInt(keywords.size()));
      if (!usedWords.contains(word)) {
        usedWords.add(word);
        mainName.append(word);
      }
    }

    // 如果名称太短，添加通用词汇
    if (mainName.length() < 2) {
      String commonWord = COMMON_WORDS.get(random.nextInt(COMMON_WORDS.size()));
      mainName.append(commonWord);
    }

    return mainName.toString();
  }
}
