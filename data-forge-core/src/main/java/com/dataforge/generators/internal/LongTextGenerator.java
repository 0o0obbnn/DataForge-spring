package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 长文本生成器 支持中文、英文或混合文本生成
 *
 * <p>支持的参数： - language: 文本语言 (CN|EN|BOTH，默认BOTH) - minLength: 最小长度 (默认100) - maxLength: 最大长度
 * (默认500) - includePunctuation: 是否包含标点符号 (默认true) - includeNewlines: 是否包含换行符 (默认true) -
 * paragraphCount: 段落数量 (默认1-3) - contentType: 内容类型
 * (RANDOM|NEWS|ARTICLE|REVIEW|DESCRIPTION，默认RANDOM)
 */
public class LongTextGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Random RANDOM = new Random();

  // 中文常用字符
  private static final String CHINESE_CHARS =
      "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委素技备半办青省列习响约支般史感劳便团往酸历市克何除消构府称太准精值号率族维划选标写存候毛亲快效斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严";

  // 英文常用单词
  private static final List<String> ENGLISH_WORDS =
      Arrays.asList(
          "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on",
          "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "she",
          "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up",
          "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like",
          "time", "no", "just", "him", "know", "take", "people", "into", "year", "your", "good",
          "some", "could", "them", "see", "other", "than", "then", "now", "look", "only", "come",
          "its", "over", "think", "also", "back", "after", "use", "two", "how", "our", "work",
          "first", "well", "way", "even", "new", "want", "because", "any", "these", "give", "day",
          "most", "us", "is", "was", "are", "been", "has", "had", "were", "said", "each", "which",
          "their", "time", "will", "about", "if", "up", "out", "many", "then", "them", "these",
          "so", "some", "her", "would", "make", "like", "into", "him", "has", "two", "more", "very",
          "what", "know", "just", "first", "get", "over", "think", "where", "much", "go", "well",
          "were", "been", "have", "had", "has", "said", "each", "which", "she", "do", "how",
          "their", "if", "will", "up", "other", "about");

  // 新闻类文本模板
  private static final List<String> NEWS_TEMPLATES =
      Arrays.asList("据报道，", "记者了解到，", "最新消息显示，", "相关部门表示，", "专家指出，", "调查发现，", "统计数据表明，");

  // 评论类文本模板
  private static final List<String> REVIEW_TEMPLATES =
      Arrays.asList("总的来说，", "我认为，", "从个人体验来看，", "值得一提的是，", "不得不说，", "坦率地讲，", "客观地评价，");

  @Override
  public String getType() {
    return "longtext";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    String language = getStringParam(config, "language", "BOTH");
    int minLength = getIntParam(config, "minLength", 100);
    int maxLength = getIntParam(config, "maxLength", 500);
    boolean includePunctuation = getBooleanParam(config, "includePunctuation", true);
    boolean includeNewlines = getBooleanParam(config, "includeNewlines", true);
    String paragraphCountStr = getStringParam(config, "paragraphCount", "1-3");
    String contentType = getStringParam(config, "contentType", "RANDOM");

    // 解析段落数量范围
    int[] paragraphRange = parseRange(paragraphCountStr, 1, 3);
    int paragraphCount =
        RANDOM.nextInt(paragraphRange[1] - paragraphRange[0] + 1) + paragraphRange[0];

    // 计算目标长度
    int targetLength = RANDOM.nextInt(maxLength - minLength + 1) + minLength;

    StringBuilder result = new StringBuilder();
    int currentLength = 0;

    for (int p = 0; p < paragraphCount && currentLength < targetLength; p++) {
      if (p > 0 && includeNewlines) {
        result.append("\n");
        currentLength++;
      }

      // 添加段落开头（根据内容类型）
      String paragraphStart = getParagraphStart(contentType, language);
      if (!paragraphStart.isEmpty()) {
        result.append(paragraphStart);
        currentLength += paragraphStart.length();
      }

      // 生成段落内容
      int paragraphTargetLength = (targetLength - currentLength) / (paragraphCount - p);
      String paragraphContent =
          generateParagraph(language, paragraphTargetLength, includePunctuation);
      result.append(paragraphContent);
      currentLength += paragraphContent.length();
    }

    // 确保不超过最大长度
    if (result.length() > maxLength) {
      return result.substring(0, maxLength);
    }

    return result.toString();
  }

  /** 生成段落内容 */
  private String generateParagraph(String language, int targetLength, boolean includePunctuation) {
    StringBuilder paragraph = new StringBuilder();

    while (paragraph.length() < targetLength) {
      String sentence = generateSentence(language, includePunctuation);
      if (paragraph.length() + sentence.length() <= targetLength) {
        paragraph.append(sentence);
        if (includePunctuation && paragraph.length() < targetLength - 1) {
          paragraph.append(" ");
        }
      } else {
        break;
      }
    }

    return paragraph.toString();
  }

  /** 生成句子 */
  private String generateSentence(String language, boolean includePunctuation) {
    StringBuilder sentence = new StringBuilder();
    int sentenceLength = RANDOM.nextInt(20) + 10; // 10-30个字符/单词

    switch (language.toUpperCase()) {
      case "CN":
        sentence.append(generateChineseSentence(sentenceLength));
        break;
      case "EN":
        sentence.append(generateEnglishSentence(sentenceLength));
        break;
      case "BOTH":
      default:
        if (RANDOM.nextBoolean()) {
          sentence.append(generateChineseSentence(sentenceLength));
        } else {
          sentence.append(generateEnglishSentence(sentenceLength));
        }
        break;
    }

    // 添加句末标点
    if (includePunctuation) {
      if (sentence.toString().matches(".*[\\u4e00-\\u9fa5].*")) {
        // 中文句子
        String[] punctuations = {"。", "！", "？", "；"};
        sentence.append(punctuations[RANDOM.nextInt(punctuations.length)]);
      } else {
        // 英文句子
        String[] punctuations = {".", "!", "?", ";"};
        sentence.append(punctuations[RANDOM.nextInt(punctuations.length)]);
      }
    }

    return sentence.toString();
  }

  /** 生成中文句子 */
  private String generateChineseSentence(int length) {
    StringBuilder sentence = new StringBuilder();

    for (int i = 0; i < length; i++) {
      char ch = CHINESE_CHARS.charAt(RANDOM.nextInt(CHINESE_CHARS.length()));
      sentence.append(ch);
    }

    return sentence.toString();
  }

  /** 生成英文句子 */
  private String generateEnglishSentence(int wordCount) {
    StringBuilder sentence = new StringBuilder();

    for (int i = 0; i < wordCount; i++) {
      if (i > 0) {
        sentence.append(" ");
      }
      String word = ENGLISH_WORDS.get(RANDOM.nextInt(ENGLISH_WORDS.size()));
      if (i == 0) {
        // 首字母大写
        word = word.substring(0, 1).toUpperCase() + word.substring(1);
      }
      sentence.append(word);
    }

    return sentence.toString();
  }

  /** 获取段落开头 */
  private String getParagraphStart(String contentType, String language) {
    switch (contentType.toUpperCase()) {
      case "NEWS":
        if (language.equals("CN") || (language.equals("BOTH") && RANDOM.nextBoolean())) {
          return NEWS_TEMPLATES.get(RANDOM.nextInt(NEWS_TEMPLATES.size()));
        }
        return "According to reports, ";
      case "REVIEW":
        if (language.equals("CN") || (language.equals("BOTH") && RANDOM.nextBoolean())) {
          return REVIEW_TEMPLATES.get(RANDOM.nextInt(REVIEW_TEMPLATES.size()));
        }
        return "In my opinion, ";
      case "ARTICLE":
        if (language.equals("CN") || (language.equals("BOTH") && RANDOM.nextBoolean())) {
          return "首先，";
        }
        return "First of all, ";
      case "DESCRIPTION":
        if (language.equals("CN") || (language.equals("BOTH") && RANDOM.nextBoolean())) {
          return "这是一个";
        }
        return "This is a ";
      default:
        return "";
    }
  }

  /** 解析范围字符串 */
  private int[] parseRange(String rangeStr, int defaultMin, int defaultMax) {
    try {
      if (rangeStr.contains("-")) {
        String[] parts = rangeStr.split("-");
        int min = Integer.parseInt(parts[0].trim());
        int max = Integer.parseInt(parts[1].trim());
        return new int[] {min, max};
      } else {
        int value = Integer.parseInt(rangeStr.trim());
        return new int[] {value, value};
      }
    } catch (Exception e) {
      return new int[] {defaultMin, defaultMax};
    }
  }
}
