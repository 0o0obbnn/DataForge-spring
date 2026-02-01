package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户行为数据生成器
 *
 * <p>生成用户行为数据，包括点击流、搜索关键词、购物车内容等， 用于用户行为分析、推荐系统测试、数据分析等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>behavior_type: 行为类型 (CLICK|SEARCH|PURCHASE|VIEW|CART|LOGIN|LOGOUT) 默认: CLICK
 *   <li>format: 输出格式 (JSON|LOG|CSV|SIMPLE) 默认: JSON
 *   <li>include_timestamp: 是否包含时间戳 默认: true
 *   <li>include_user_agent: 是否包含用户代理 默认: false
 *   <li>session_duration: 会话持续时间（分钟）默认: 30
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class UserBehaviorGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(UserBehaviorGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  // 搜索关键词
  private static final List<String> SEARCH_KEYWORDS =
      Arrays.asList(
          "手机",
          "电脑",
          "衣服",
          "鞋子",
          "包包",
          "化妆品",
          "食品",
          "书籍",
          "phone",
          "laptop",
          "clothes",
          "shoes",
          "bag",
          "cosmetics",
          "food",
          "book",
          "iPhone",
          "MacBook",
          "Nike",
          "Adidas",
          "Chanel",
          "Samsung",
          "Dell",
          "HP");

  // 页面路径
  private static final List<String> PAGE_PATHS =
      Arrays.asList(
          "/",
          "/home",
          "/products",
          "/category",
          "/search",
          "/cart",
          "/checkout",
          "/profile",
          "/orders",
          "/help",
          "/about",
          "/contact",
          "/login",
          "/register");

  // 用户代理字符串
  private static final List<String> USER_AGENTS =
      Arrays.asList(
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
          "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15",
          "Mozilla/5.0 (Android 11; Mobile; rv:68.0) Gecko/68.0 Firefox/88.0");

  @Override
  public String getType() {
    return "user_behavior";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      String behaviorType = getStringParam(config, "behavior_type", "CLICK").toUpperCase();
      String format = getStringParam(config, "format", "JSON").toUpperCase();
      boolean includeTimestamp = getBooleanParam(config, "include_timestamp", true);
      boolean includeUserAgent = getBooleanParam(config, "include_user_agent", false);
      int sessionDuration = getIntParam(config, "session_duration", 30);

      BehaviorData behavior =
          generateBehaviorData(
              behaviorType, includeTimestamp, includeUserAgent, sessionDuration, context);

      String result = formatBehaviorData(behavior, format);

      // 存储到上下文
      context.put("user_behavior_type", behaviorType);
      context.put("user_behavior_timestamp", behavior.timestamp);
      context.put("user_session_id", behavior.sessionId);

      return result;

    } catch (Exception e) {
      logger.error("Failed to generate user behavior data", e);
      return "{\"action\":\"click\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
    }
  }

  private BehaviorData generateBehaviorData(
      String behaviorType,
      boolean includeTimestamp,
      boolean includeUserAgent,
      int sessionDuration,
      DataForgeContext context) {
    BehaviorData behavior = new BehaviorData();

    // 基础信息
    behavior.action = behaviorType.toLowerCase();
    behavior.sessionId = generateSessionId();
    behavior.userId =
        context
            .get("generated_user_id", String.class)
            .orElse("user_" + (1000 + random.nextInt(9000)));

    // 时间戳
    if (includeTimestamp) {
      behavior.timestamp =
          LocalDateTime.now()
              .minusMinutes(random.nextInt(sessionDuration))
              .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // 用户代理
    if (includeUserAgent) {
      behavior.userAgent = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
    }

    // 根据行为类型生成特定数据
    switch (behaviorType) {
      case "CLICK":
        behavior.url = generateClickUrl();
        break;
      case "SEARCH":
        behavior.query = SEARCH_KEYWORDS.get(random.nextInt(SEARCH_KEYWORDS.size()));
        behavior.results = 10 + random.nextInt(990);
        break;
      case "PURCHASE":
        behavior.productId = "prod_" + (10000 + random.nextInt(90000));
        behavior.amount = 10.0 + random.nextDouble() * 990.0;
        break;
      case "VIEW":
        behavior.url = generateViewUrl();
        behavior.duration = 5 + random.nextInt(295); // 5-300秒
        break;
      case "CART":
        behavior.productId = "prod_" + (10000 + random.nextInt(90000));
        behavior.quantity = 1 + random.nextInt(5);
        behavior.action = random.nextBoolean() ? "add_to_cart" : "remove_from_cart";
        break;
      case "LOGIN":
      case "LOGOUT":
        behavior.ip = generateIpAddress();
        break;
    }

    return behavior;
  }

  private String generateSessionId() {
    return "sess_" + System.currentTimeMillis() + "_" + random.nextInt(10000);
  }

  private String generateClickUrl() {
    String path = PAGE_PATHS.get(random.nextInt(PAGE_PATHS.size()));
    if (random.nextBoolean()) {
      path += "?id=" + random.nextInt(1000);
    }
    return "https://example.com" + path;
  }

  private String generateViewUrl() {
    String[] pages = {"/product/", "/article/", "/category/", "/brand/"};
    String page = pages[random.nextInt(pages.length)];
    return "https://example.com" + page + (1000 + random.nextInt(9000));
  }

  private String generateIpAddress() {
    return (1 + random.nextInt(254))
        + "."
        + random.nextInt(256)
        + "."
        + random.nextInt(256)
        + "."
        + (1 + random.nextInt(254));
  }

  private String formatBehaviorData(BehaviorData behavior, String format) {
    switch (format) {
      case "JSON":
        return formatAsJson(behavior);
      case "LOG":
        return formatAsLog(behavior);
      case "CSV":
        return formatAsCsv(behavior);
      case "SIMPLE":
      default:
        return formatAsSimple(behavior);
    }
  }

  private String formatAsJson(BehaviorData behavior) {
    StringBuilder json = new StringBuilder("{");
    json.append("\"action\":\"").append(behavior.action).append("\",");
    json.append("\"sessionId\":\"").append(behavior.sessionId).append("\",");
    json.append("\"userId\":\"").append(behavior.userId).append("\"");

    if (behavior.timestamp != null) {
      json.append(",\"timestamp\":\"").append(behavior.timestamp).append("\"");
    }
    if (behavior.url != null) {
      json.append(",\"url\":\"").append(behavior.url).append("\"");
    }
    if (behavior.query != null) {
      json.append(",\"query\":\"").append(behavior.query).append("\"");
    }
    if (behavior.productId != null) {
      json.append(",\"productId\":\"").append(behavior.productId).append("\"");
    }
    if (behavior.amount > 0) {
      json.append(",\"amount\":").append(String.format("%.2f", behavior.amount));
    }
    if (behavior.duration > 0) {
      json.append(",\"duration\":").append(behavior.duration);
    }
    if (behavior.results > 0) {
      json.append(",\"results\":").append(behavior.results);
    }
    if (behavior.quantity > 0) {
      json.append(",\"quantity\":").append(behavior.quantity);
    }
    if (behavior.ip != null) {
      json.append(",\"ip\":\"").append(behavior.ip).append("\"");
    }
    if (behavior.userAgent != null) {
      json.append(",\"userAgent\":\"").append(behavior.userAgent).append("\"");
    }

    json.append("}");
    return json.toString();
  }

  private String formatAsLog(BehaviorData behavior) {
    return String.format(
        "[%s] %s %s %s %s",
        behavior.timestamp != null ? behavior.timestamp : "NOW",
        behavior.userId,
        behavior.action.toUpperCase(),
        behavior.url != null ? behavior.url : "",
        behavior.query != null ? behavior.query : "");
  }

  private String formatAsCsv(BehaviorData behavior) {
    return String.format(
        "%s,%s,%s,%s,%s,%.2f,%d",
        behavior.userId,
        behavior.action,
        behavior.timestamp != null ? behavior.timestamp : "",
        behavior.url != null ? behavior.url : "",
        behavior.query != null ? behavior.query : "",
        behavior.amount,
        behavior.duration);
  }

  private String formatAsSimple(BehaviorData behavior) {
    return behavior.userId
        + " "
        + behavior.action
        + (behavior.url != null ? " " + behavior.url : "")
        + (behavior.query != null ? " \"" + behavior.query + "\"" : "");
  }

  // 行为数据类
  private static class BehaviorData {
    String action;
    String sessionId;
    String userId;
    String timestamp;
    String url;
    String query;
    String productId;
    String ip;
    String userAgent;
    double amount;
    int duration;
    int results;
    int quantity;
  }
}
