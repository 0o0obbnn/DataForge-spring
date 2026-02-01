package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 富文本生成器 支持HTML和Markdown格式的富文本生成
 *
 * <p>支持的参数： - format: 富文本格式 (HTML|MARKDOWN，默认HTML) - minLength: 最小长度 (默认50) - maxLength: 最大长度
 * (默认200) - includeTags: 包含的HTML标签 (p,b,i,a,img等，默认p,b,i,a) - includeElements: 包含的Markdown元素
 * (heading,list,link,image等，默认heading,list,link) - xssPayload: 是否包含XSS攻击脚本 (默认false) - valid:
 * 是否生成合法格式 (默认true) - contentLanguage: 内容语言 (CN|EN|BOTH，默认BOTH)
 */
public class RichTextGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Random RANDOM = new Random();

  // 示例文本内容
  private static final List<String> SAMPLE_TEXTS_CN =
      Arrays.asList("这是一个示例文本", "重要内容", "强调文字", "链接文本", "图片描述", "列表项目", "代码示例", "引用内容");

  private static final List<String> SAMPLE_TEXTS_EN =
      Arrays.asList(
          "This is sample text",
          "Important content",
          "Emphasized text",
          "Link text",
          "Image description",
          "List item",
          "Code example",
          "Quote content");

  // XSS攻击载荷
  private static final List<String> XSS_PAYLOADS =
      Arrays.asList(
          "<script>alert('XSS')</script>",
          "<img src=x onerror=alert('XSS')>",
          "<svg onload=alert('XSS')>",
          "<iframe src=javascript:alert('XSS')>",
          "<body onload=alert('XSS')>",
          "<input onfocus=alert('XSS') autofocus>",
          "<select onfocus=alert('XSS') autofocus>",
          "<textarea onfocus=alert('XSS') autofocus>",
          "<keygen onfocus=alert('XSS') autofocus>",
          "<video><source onerror=alert('XSS')>");

  @Override
  public String getType() {
    return "richtext";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    String format = getStringParam(config, "format", "HTML");
    int minLength = getIntParam(config, "minLength", 50);
    int maxLength = getIntParam(config, "maxLength", 200);
    String includeTagsStr = getStringParam(config, "includeTags", "p,b,i,a");
    String includeElementsStr = getStringParam(config, "includeElements", "heading,list,link");
    boolean xssPayload = getBooleanParam(config, "xssPayload", false);
    boolean valid = getBooleanParam(config, "valid", true);
    String contentLanguage = getStringParam(config, "contentLanguage", "BOTH");

    List<String> includeTags = Arrays.asList(includeTagsStr.split(","));
    List<String> includeElements = Arrays.asList(includeElementsStr.split(","));

    // 如果要求XSS载荷，直接返回
    if (xssPayload) {
      return XSS_PAYLOADS.get(RANDOM.nextInt(XSS_PAYLOADS.size()));
    }

    StringBuilder result = new StringBuilder();
    int targetLength = RANDOM.nextInt(maxLength - minLength + 1) + minLength;

    switch (format.toUpperCase()) {
      case "HTML":
        result.append(generateHtml(includeTags, targetLength, valid, contentLanguage));
        break;
      case "MARKDOWN":
        result.append(generateMarkdown(includeElements, targetLength, valid, contentLanguage));
        break;
      default:
        result.append(generateHtml(includeTags, targetLength, valid, contentLanguage));
        break;
    }

    return result.toString();
  }

  /** 生成HTML内容 */
  private String generateHtml(
      List<String> includeTags, int targetLength, boolean valid, String contentLanguage) {
    StringBuilder html = new StringBuilder();

    while (html.length() < targetLength) {
      String tag = includeTags.get(RANDOM.nextInt(includeTags.size())).trim();
      String content = generateHtmlElement(tag, valid, contentLanguage);

      if (html.length() + content.length() <= targetLength) {
        html.append(content);
        if (html.length() < targetLength - 10) {
          html.append(" ");
        }
      } else {
        break;
      }
    }

    return html.toString();
  }

  /** 生成HTML元素 */
  private String generateHtmlElement(String tag, boolean valid, String contentLanguage) {
    String text = getSampleText(contentLanguage);

    switch (tag.toLowerCase()) {
      case "p":
        return valid ? "<p>" + text + "</p>" : "<p>" + text; // 无效：缺少闭合标签
      case "b":
      case "strong":
        return valid ? "<" + tag + ">" + text + "</" + tag + ">" : "<" + tag + ">" + text;
      case "i":
      case "em":
        return valid ? "<" + tag + ">" + text + "</" + tag + ">" : "<" + tag + ">" + text;
      case "a":
        String href = "https://example.com/" + RANDOM.nextInt(100);
        return valid
            ? "<a href=\"" + href + "\">" + text + "</a>"
            : "<a href=" + href + ">" + text + "</a>"; // 无效：缺少引号
      case "img":
        String src = "https://example.com/image" + RANDOM.nextInt(100) + ".jpg";
        return valid
            ? "<img src=\"" + src + "\" alt=\"" + text + "\">"
            : "<img src=\"" + src + "\" alt=" + text + ">"; // 无效：缺少引号
      case "div":
        return valid ? "<div>" + text + "</div>" : "<div>" + text;
      case "span":
        return valid ? "<span>" + text + "</span>" : "<span>" + text;
      case "h1":
      case "h2":
      case "h3":
        return valid ? "<" + tag + ">" + text + "</" + tag + ">" : "<" + tag + ">" + text;
      case "ul":
        return generateList("ul", valid, contentLanguage);
      case "ol":
        return generateList("ol", valid, contentLanguage);
      case "li":
        return valid ? "<li>" + text + "</li>" : "<li>" + text;
      case "br":
        return "<br>";
      case "hr":
        return "<hr>";
      default:
        return valid ? "<" + tag + ">" + text + "</" + tag + ">" : "<" + tag + ">" + text;
    }
  }

  /** 生成HTML列表 */
  private String generateList(String listType, boolean valid, String contentLanguage) {
    StringBuilder list = new StringBuilder();
    list.append("<").append(listType).append(">");

    int itemCount = RANDOM.nextInt(3) + 2; // 2-4个列表项
    for (int i = 0; i < itemCount; i++) {
      String text = getSampleText(contentLanguage);
      if (valid) {
        list.append("<li>").append(text).append("</li>");
      } else {
        list.append("<li>").append(text); // 无效：缺少闭合标签
      }
    }

    if (valid) {
      list.append("</").append(listType).append(">");
    }

    return list.toString();
  }

  /** 生成Markdown内容 */
  private String generateMarkdown(
      List<String> includeElements, int targetLength, boolean valid, String contentLanguage) {
    StringBuilder markdown = new StringBuilder();

    while (markdown.length() < targetLength) {
      String element = includeElements.get(RANDOM.nextInt(includeElements.size())).trim();
      String content = generateMarkdownElement(element, valid, contentLanguage);

      if (markdown.length() + content.length() <= targetLength) {
        markdown.append(content);
        if (markdown.length() < targetLength - 10) {
          markdown.append("\n\n");
        }
      } else {
        break;
      }
    }

    return markdown.toString();
  }

  /** 生成Markdown元素 */
  private String generateMarkdownElement(String element, boolean valid, String contentLanguage) {
    String text = getSampleText(contentLanguage);

    switch (element.toLowerCase()) {
      case "heading":
        int level = RANDOM.nextInt(3) + 1; // H1-H3
        String hashes = "#".repeat(level);
        return valid ? hashes + " " + text : hashes + text; // 无效：缺少空格
      case "bold":
        return valid ? "**" + text + "**" : "*" + text + "*"; // 无效：使用单星号
      case "italic":
        return valid ? "*" + text + "*" : "**" + text + "**"; // 无效：使用双星号
      case "link":
        String url = "https://example.com/" + RANDOM.nextInt(100);
        return valid ? "[" + text + "](" + url + ")" : "[" + text + "]" + url; // 无效：缺少括号
      case "image":
        String imgUrl = "https://example.com/image" + RANDOM.nextInt(100) + ".jpg";
        return valid
            ? "![" + text + "](" + imgUrl + ")"
            : "!" + text + "(" + imgUrl + ")"; // 无效：缺少方括号
      case "list":
        return generateMarkdownList(valid, contentLanguage);
      case "code":
        return valid ? "`" + text + "`" : text; // 无效：缺少反引号
      case "quote":
        return valid ? "> " + text : ">" + text; // 无效：缺少空格
      case "table":
        return generateMarkdownTable(valid, contentLanguage);
      default:
        return text;
    }
  }

  /** 生成Markdown列表 */
  private String generateMarkdownList(boolean valid, String contentLanguage) {
    StringBuilder list = new StringBuilder();
    int itemCount = RANDOM.nextInt(3) + 2; // 2-4个列表项

    for (int i = 0; i < itemCount; i++) {
      String text = getSampleText(contentLanguage);
      if (valid) {
        list.append("- ").append(text);
      } else {
        list.append("-").append(text); // 无效：缺少空格
      }
      if (i < itemCount - 1) {
        list.append("\n");
      }
    }

    return list.toString();
  }

  /** 生成Markdown表格 */
  private String generateMarkdownTable(boolean valid, String contentLanguage) {
    StringBuilder table = new StringBuilder();

    // 表头
    String header1 = getSampleText(contentLanguage);
    String header2 = getSampleText(contentLanguage);

    if (valid) {
      table.append("| ").append(header1).append(" | ").append(header2).append(" |\n");
      table.append("|---|---|\n");

      // 表格行
      for (int i = 0; i < 2; i++) {
        String cell1 = getSampleText(contentLanguage);
        String cell2 = getSampleText(contentLanguage);
        table.append("| ").append(cell1).append(" | ").append(cell2).append(" |\n");
      }
    } else {
      // 无效：缺少分隔符行
      table.append("| ").append(header1).append(" | ").append(header2).append(" |\n");
      String cell1 = getSampleText(contentLanguage);
      String cell2 = getSampleText(contentLanguage);
      table.append("| ").append(cell1).append(" | ").append(cell2).append(" |");
    }

    return table.toString();
  }

  /** 获取示例文本 */
  private String getSampleText(String contentLanguage) {
    switch (contentLanguage.toUpperCase()) {
      case "CN":
        return SAMPLE_TEXTS_CN.get(RANDOM.nextInt(SAMPLE_TEXTS_CN.size()));
      case "EN":
        return SAMPLE_TEXTS_EN.get(RANDOM.nextInt(SAMPLE_TEXTS_EN.size()));
      case "BOTH":
      default:
        if (RANDOM.nextBoolean()) {
          return SAMPLE_TEXTS_CN.get(RANDOM.nextInt(SAMPLE_TEXTS_CN.size()));
        } else {
          return SAMPLE_TEXTS_EN.get(RANDOM.nextInt(SAMPLE_TEXTS_EN.size()));
        }
    }
  }
}
