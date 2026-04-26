package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URL生成器
 *
 * <p>支持的参数： - scheme: 协议方案 (HTTP|HTTPS|FTP|FILE|ANY) - domain: 域名 (指定域名或使用随机域名) - path_depth: 路径深度
 * (0-10) - include_query: 是否包含查询参数 (true|false) - include_fragment: 是否包含片段标识符 (true|false) - port:
 * 端口号 (指定端口或随机端口)
 *
 * @author DataForge
 */
public class UrlGenerator extends BaseGenerator implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(UrlGenerator.class);
  private static final Random random = new Random();

  // 常见域名后缀
  private static final List<String> COMMON_TLDS =
      Arrays.asList(
          "com", "org", "net", "edu", "gov", "mil", "int", "cn", "uk", "de", "fr", "jp", "au", "ca",
          "ru", "info", "biz", "name", "pro", "museum", "travel");

  // 常见子域名
  private static final List<String> COMMON_SUBDOMAINS =
      Arrays.asList(
          "www", "api", "app", "admin", "blog", "shop", "mail", "ftp", "cdn", "static", "img",
          "media", "dev", "test", "staging");

  // 常见路径词汇
  private static final List<String> PATH_WORDS =
      Arrays.asList(
          "home",
          "about",
          "contact",
          "products",
          "services",
          "blog",
          "news",
          "user",
          "admin",
          "api",
          "v1",
          "v2",
          "public",
          "private",
          "secure",
          "data",
          "files",
          "images",
          "docs",
          "help",
          "support",
          "login",
          "register",
          "profile",
          "settings",
          "dashboard",
          "reports",
          "analytics");

  // 常见查询参数
  private static final List<String> QUERY_PARAMS =
      Arrays.asList(
          "id",
          "name",
          "type",
          "category",
          "page",
          "size",
          "limit",
          "offset",
          "sort",
          "order",
          "filter",
          "search",
          "q",
          "lang",
          "locale",
          "format",
          "version",
          "timestamp",
          "token",
          "key",
          "session",
          "user",
          "ref");

  @Override
  public String getType() {
    return "url";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      // 获取参数
      String scheme = config.getParam("scheme", String.class, "HTTPS");
      String domain = config.getParam("domain", String.class, null);
      int pathDepth = Integer.parseInt(config.getParam("path_depth", String.class, "2"));
      boolean includeQuery =
          Boolean.parseBoolean(config.getParam("include_query", String.class, "true"));
      boolean includeFragment =
          Boolean.parseBoolean(config.getParam("include_fragment", String.class, "false"));
      String port = config.getParam("port", String.class, null);

      // 生成URL
      String url = generateUrl(scheme, domain, pathDepth, includeQuery, includeFragment, port);

      // 将URL信息存入上下文
      context.put("url", url);
      context.put("url_scheme", extractScheme(url));
      context.put("url_domain", extractDomain(url));

      logger.debug("Generated URL: {}", url);
      return url;

    } catch (Exception e) {
      logger.error("Error generating URL", e);
      return "https://www.example.com/api/v1/users";
    }
  }

  private String generateUrl(
      String scheme,
      String domain,
      int pathDepth,
      boolean includeQuery,
      boolean includeFragment,
      String port) {

    StringBuilder url = new StringBuilder();

    // 1. 生成协议方案
    String urlScheme = selectScheme(scheme);
    url.append(urlScheme).append("://");

    // 2. 生成域名
    String hostname = generateHostname(domain);
    url.append(hostname);

    // 3. 生成端口号
    String portNumber = generatePort(port, urlScheme);
    if (portNumber != null) {
      url.append(":").append(portNumber);
    }

    // 4. 生成路径
    String path = generatePath(pathDepth);
    if (!path.isEmpty()) {
      url.append(path);
    }

    // 5. 生成查询参数
    if (includeQuery && random.nextDouble() < 0.7) {
      String query = generateQuery();
      if (!query.isEmpty()) {
        url.append("?").append(query);
      }
    }

    // 6. 生成片段标识符
    if (includeFragment && random.nextDouble() < 0.3) {
      String fragment = generateFragment();
      if (!fragment.isEmpty()) {
        url.append("#").append(fragment);
      }
    }

    return url.toString();
  }

  private String selectScheme(String scheme) {
    switch (scheme.toUpperCase()) {
      case "HTTP":
        return "http";
      case "HTTPS":
        return "https";
      case "FTP":
        return "ftp";
      case "FILE":
        return "file";
      case "ANY":
      default:
        // 70%概率选择HTTPS
        return random.nextDouble() < 0.7 ? "https" : "http";
    }
  }

  private String generateHostname(String domain) {
    if (domain != null && !domain.isEmpty()) {
      return domain;
    }

    StringBuilder hostname = new StringBuilder();

    // 添加子域名（30%概率）
    if (random.nextDouble() < 0.3) {
      String subdomain = COMMON_SUBDOMAINS.get(random.nextInt(COMMON_SUBDOMAINS.size()));
      hostname.append(subdomain).append(".");
    }

    // 生成主域名
    String mainDomain = generateRandomDomainName();
    hostname.append(mainDomain);

    // 添加顶级域名
    String tld = COMMON_TLDS.get(random.nextInt(COMMON_TLDS.size()));
    hostname.append(".").append(tld);

    return hostname.toString();
  }

  private String generateRandomDomainName() {
    // 生成随机域名
    String[] prefixes = {"tech", "data", "cloud", "web", "app", "digital", "smart", "global"};
    String[] suffixes = {"corp", "inc", "ltd", "group", "systems", "solutions", "services", "labs"};

    if (random.nextBoolean()) {
      return prefixes[random.nextInt(prefixes.length)] + suffixes[random.nextInt(suffixes.length)];
    } else {
      // 生成随机字符串
      StringBuilder name = new StringBuilder();
      int length = 5 + random.nextInt(8);
      for (int i = 0; i < length; i++) {
        name.append((char) ('a' + random.nextInt(26)));
      }
      return name.toString();
    }
  }

  private String generatePort(String port, String scheme) {
    if (port != null && !port.isEmpty()) {
      if ("random".equalsIgnoreCase(port)) {
        return String.valueOf(1024 + random.nextInt(64512)); // 1024-65535
      } else {
        return port;
      }
    }

    // 10%概率添加非默认端口
    if (random.nextDouble() < 0.1) {
      int[] commonPorts = {8080, 8443, 3000, 8000, 9000, 8888, 8090, 9090};
      return String.valueOf(commonPorts[random.nextInt(commonPorts.length)]);
    }

    return null; // 使用默认端口
  }

  private String generatePath(int maxDepth) {
    if (maxDepth <= 0) {
      return "";
    }

    StringBuilder path = new StringBuilder();
    int depth = random.nextInt(maxDepth) + 1;

    for (int i = 0; i < depth; i++) {
      path.append("/");

      if (i == depth - 1 && random.nextDouble() < 0.3) {
        // 最后一级可能是文件
        path.append(generateFileName());
      } else {
        // 路径段
        String segment = PATH_WORDS.get(random.nextInt(PATH_WORDS.size()));

        // 30%概率添加数字或ID
        if (random.nextDouble() < 0.3) {
          segment += random.nextInt(1000);
        }

        path.append(segment);
      }
    }

    return path.toString();
  }

  private String generateFileName() {
    String[] names = {"index", "home", "about", "contact", "data", "config", "settings"};
    String[] extensions = {"html", "php", "jsp", "asp", "json", "xml", "txt", "pdf"};

    String name = names[random.nextInt(names.length)];
    String ext = extensions[random.nextInt(extensions.length)];

    return name + "." + ext;
  }

  private String generateQuery() {
    StringBuilder query = new StringBuilder();
    int paramCount = 1 + random.nextInt(4); // 1-4个参数

    Set<String> usedParams = new HashSet<>();

    for (int i = 0; i < paramCount; i++) {
      if (i > 0) {
        query.append("&");
      }

      String param;
      do {
        param = QUERY_PARAMS.get(random.nextInt(QUERY_PARAMS.size()));
      } while (usedParams.contains(param));

      usedParams.add(param);

      String value = generateQueryValue(param);
      query.append(param).append("=").append(value);
    }

    return query.toString();
  }

  private String generateQueryValue(String param) {
    switch (param) {
      case "id":
        return String.valueOf(random.nextInt(10000));
      case "page":
        return String.valueOf(1 + random.nextInt(100));
      case "size":
      case "limit":
        return String.valueOf(10 + random.nextInt(90));
      case "sort":
        return random.nextBoolean() ? "asc" : "desc";
      case "format":
        return random.nextBoolean() ? "json" : "xml";
      case "lang":
        String[] langs = {"en", "zh", "fr", "de", "ja", "ko"};
        return langs[random.nextInt(langs.length)];
      default:
        // 生成随机值
        if (random.nextBoolean()) {
          return String.valueOf(random.nextInt(1000));
        } else {
          return "value" + random.nextInt(100);
        }
    }
  }

  private String generateFragment() {
    String[] fragments = {
      "top",
      "bottom",
      "header",
      "footer",
      "content",
      "sidebar",
      "nav",
      "menu",
      "section1",
      "section2",
      "chapter1",
      "chapter2",
      "page1",
      "page2"
    };

    return fragments[random.nextInt(fragments.length)];
  }

  private String extractScheme(String url) {
    int index = url.indexOf("://");
    if (index > 0) {
      return url.substring(0, index);
    }
    return "unknown";
  }

  private String extractDomain(String url) {
    try {
      int start = url.indexOf("://") + 3;
      int end = url.indexOf("/", start);
      if (end == -1) {
        end = url.indexOf("?", start);
      }
      if (end == -1) {
        end = url.indexOf("#", start);
      }
      if (end == -1) {
        end = url.length();
      }

      String hostPort = url.substring(start, end);
      int portIndex = hostPort.indexOf(":");
      if (portIndex > 0) {
        return hostPort.substring(0, portIndex);
      }
      return hostPort;
    } catch (Exception e) {
      return "unknown";
    }
  }
}
