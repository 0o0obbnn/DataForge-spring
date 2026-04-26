package com.dataforge.generators.internal;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User-Agent生成器
 *
 * <p>支持生成各种浏览器和操作系统的User-Agent字符串，用于Web应用测试、 爬虫开发、浏览器兼容性测试等场景。
 *
 * <p>支持的参数：
 *
 * <ul>
 *   <li>browser: 浏览器类型 (CHROME|FIREFOX|SAFARI|EDGE|IE|OPERA|RANDOM) 默认: RANDOM
 *   <li>os: 操作系统 (WINDOWS|MACOS|LINUX|ANDROID|IOS|RANDOM) 默认: RANDOM
 *   <li>device: 设备类型 (DESKTOP|MOBILE|TABLET|RANDOM) 默认: RANDOM
 *   <li>version: 是否包含版本号 默认: true
 *   <li>realistic: 是否生成真实的User-Agent 默认: true
 * </ul>
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public class UserAgentGenerator extends BaseGenerator
    implements DataGenerator<String, FieldConfig> {

  private static final Logger logger = LoggerFactory.getLogger(UserAgentGenerator.class);
  private static final SecureRandom random = new SecureRandom();

  private static final String DEFAULT_CONFIG_FILE = "data/user-agents.yml";

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  /** 配置数据（从文件加载） */
  private volatile UserAgentConfig userAgentConfig;

  // 浏览器类型枚举
  public enum BrowserType {
    CHROME,
    FIREFOX,
    SAFARI,
    EDGE,
    IE,
    OPERA,
    RANDOM
  }

  // 操作系统类型枚举
  public enum OSType {
    WINDOWS,
    MACOS,
    LINUX,
    ANDROID,
    IOS,
    RANDOM
  }

  // 设备类型枚举
  public enum DeviceType {
    DESKTOP,
    MOBILE,
    TABLET,
    RANDOM
  }

  // Chrome User-Agent模板
  private static final List<String> CHROME_TEMPLATES =
      Arrays.asList(
          "Mozilla/5.0 ({os}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{version}"
              + " Safari/537.36",
          "Mozilla/5.0 ({os}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{version} Mobile"
              + " Safari/537.36");

  // Firefox User-Agent模板
  private static final List<String> FIREFOX_TEMPLATES =
      Arrays.asList(
          "Mozilla/5.0 ({os}; rv:{version}) Gecko/20100101 Firefox/{version}",
          "Mozilla/5.0 (Mobile; rv:{version}) Gecko/{version} Firefox/{version}");

  // Safari User-Agent模板
  private static final List<String> SAFARI_TEMPLATES =
      Arrays.asList(
          "Mozilla/5.0 ({os}) AppleWebKit/{webkit_version} (KHTML, like Gecko) Version/{version}"
              + " Safari/{webkit_version}",
          "Mozilla/5.0 ({os}) AppleWebKit/{webkit_version} (KHTML, like Gecko) Version/{version}"
              + " Mobile/15E148 Safari/{webkit_version}");

  // Edge User-Agent模板
  private static final List<String> EDGE_TEMPLATES =
      Arrays.asList(
          "Mozilla/5.0 ({os}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{chrome_version}"
              + " Safari/537.36 Edg/{version}",
          "Mozilla/5.0 ({os}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{chrome_version}"
              + " Safari/537.36 EdgA/{version}");

  // 操作系统字符串映射
  private static final Map<OSType, List<String>> OS_STRINGS = new HashMap<>();

  static {
    OS_STRINGS.put(
        OSType.WINDOWS,
        Arrays.asList(
            "Windows NT 10.0; Win64; x64",
            "Windows NT 10.0; WOW64",
            "Windows NT 6.3; Win64; x64",
            "Windows NT 6.1; Win64; x64",
            "Windows NT 6.1; WOW64"));

    OS_STRINGS.put(
        OSType.MACOS,
        Arrays.asList(
            "Macintosh; Intel Mac OS X 10_15_7",
            "Macintosh; Intel Mac OS X 10_14_6",
            "Macintosh; Intel Mac OS X 10_13_6",
            "Macintosh; Intel Mac OS X 11_2_3",
            "Macintosh; Intel Mac OS X 12_1"));

    OS_STRINGS.put(
        OSType.LINUX,
        Arrays.asList(
            "X11; Linux x86_64",
            "X11; Ubuntu; Linux x86_64",
            "X11; Linux i686",
            "X11; CrOS x86_64"));

    OS_STRINGS.put(
        OSType.ANDROID,
        Arrays.asList(
            "Linux; Android 11; SM-G991B",
            "Linux; Android 10; SM-A505F",
            "Linux; Android 9; SM-G960F",
            "Linux; Android 12; Pixel 6",
            "Linux; Android 11; OnePlus 9"));

    OS_STRINGS.put(
        OSType.IOS,
        Arrays.asList(
            "iPhone; CPU iPhone OS 15_0 like Mac OS X",
            "iPhone; CPU iPhone OS 14_7_1 like Mac OS X",
            "iPad; CPU OS 15_0 like Mac OS X",
            "iPhone; CPU iPhone OS 13_7 like Mac OS X"));
  }

  @Override
  public String getType() {
    return "user_agent";
  }

  @Override
  public Class<FieldConfig> getConfigClass() {
    return FieldConfig.class;
  }

  @Override
  public String generate(FieldConfig config, DataForgeContext context) {
    try {
      ensureConfigLoaded(config);

      String browserStr = getStringParam(config, "browser", "RANDOM");
      BrowserType browser = parseBrowserType(browserStr);

      String osStr = getStringParam(config, "os", "RANDOM");
      OSType os = parseOSType(osStr);

      String deviceStr = getStringParam(config, "device", "RANDOM");
      DeviceType device = parseDeviceType(deviceStr);

      boolean includeVersion = getBooleanParam(config, "version", true);
      boolean realistic = getBooleanParam(config, "realistic", true);

      if (browser == BrowserType.RANDOM) {
        BrowserType[] browsers = {
          BrowserType.CHROME, BrowserType.FIREFOX, BrowserType.SAFARI, BrowserType.EDGE
        };
        browser = browsers[random.nextInt(browsers.length)];
      }

      if (os == OSType.RANDOM) {
        OSType[] oses = {OSType.WINDOWS, OSType.MACOS, OSType.LINUX, OSType.ANDROID, OSType.IOS};
        os = oses[random.nextInt(oses.length)];
      }

      if (device == DeviceType.RANDOM) {
        DeviceType[] devices = {DeviceType.DESKTOP, DeviceType.MOBILE, DeviceType.TABLET};
        device = devices[random.nextInt(devices.length)];
      }

      adjustOSAndDevice(os, device);

      return generateUserAgent(browser, os, device, includeVersion, realistic);
    } catch (Exception e) {
      logger.error("Failed to generate user agent", e);
      return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
          + " Chrome/91.0.4472.124 Safari/537.36";
    }
  }

  /** 解析浏览器类型 */
  private BrowserType parseBrowserType(String browserStr) {
    try {
      return BrowserType.valueOf(browserStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid browser type: {}, using RANDOM as default", browserStr);
      return BrowserType.RANDOM;
    }
  }

  /** 解析操作系统类型 */
  private OSType parseOSType(String osStr) {
    try {
      return OSType.valueOf(osStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid OS type: {}, using RANDOM as default", osStr);
      return OSType.RANDOM;
    }
  }

  /** 解析设备类型 */
  private DeviceType parseDeviceType(String deviceStr) {
    try {
      return DeviceType.valueOf(deviceStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid device type: {}, using RANDOM as default", deviceStr);
      return DeviceType.RANDOM;
    }
  }

  /** 调整操作系统和设备类型的兼容性 */
  private void adjustOSAndDevice(OSType os, DeviceType device) {
    // 确保移动操作系统与移动设备匹配
    if ((os == OSType.ANDROID || os == OSType.IOS) && device == DeviceType.DESKTOP) {
      // 移动操作系统不能是桌面设备，调整为移动设备
      device = DeviceType.MOBILE;
    }

    if ((os == OSType.WINDOWS || os == OSType.MACOS || os == OSType.LINUX)
        && (device == DeviceType.MOBILE || device == DeviceType.TABLET)) {
      // 桌面操作系统通常是桌面设备
      device = DeviceType.DESKTOP;
    }
  }

  /** 生成User-Agent字符串 */
  private String generateUserAgent(
      BrowserType browser,
      OSType os,
      DeviceType device,
      boolean includeVersion,
      boolean realistic) {

    String osString = getRandomOSString(os);
    String template = getRandomTemplate(browser, device);

    Map<String, String> replacements = new HashMap<>();
    replacements.put("{os}", osString);

    if (includeVersion) {
      replacements.put("{version}", generateVersion(browser, realistic));
      replacements.put("{webkit_version}", generateWebKitVersion(realistic));
      replacements.put("{chrome_version}", generateChromeVersion(realistic));
    } else {
      replacements.put("{version}", "1.0");
      replacements.put("{webkit_version}", "537.36");
      replacements.put("{chrome_version}", "91.0.4472.124");
    }

    String userAgent = template;
    for (Map.Entry<String, String> entry : replacements.entrySet()) {
      userAgent = userAgent.replace(entry.getKey(), entry.getValue());
    }

    return userAgent;
  }

  /**
   * 确保配置已加载。
   *
   * @param config 配置
   */
  private void ensureConfigLoaded(FieldConfig config) {
    if (userAgentConfig == null) {
      synchronized (this) {
        if (userAgentConfig == null) {
          loadConfig(config);
        }
      }
    }
  }

  /**
   * 加载配置。
   *
   * @param config 配置
   */
  private void loadConfig(FieldConfig config) {
    try {
      String configFile = getStringParam(config, "ua_config_file", DEFAULT_CONFIG_FILE);

      InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
      if (inputStream != null) {
        userAgentConfig = yamlMapper.readValue(inputStream, UserAgentConfig.class);
        logger.info("User-Agent config loaded from: {}", configFile);
      } else {
        logger.warn("Config file not found: {}, using fallback data", configFile);
        initializeFallbackConfig();
      }
    } catch (Exception e) {
      logger.error("Failed to load user-agent config, using fallback data", e);
      initializeFallbackConfig();
    }
  }

  /** 初始化fallback配置。 */
  private void initializeFallbackConfig() {
    userAgentConfig = new UserAgentConfig();
    userAgentConfig.setBrowsers(new HashMap<>());
    userAgentConfig.setOperatingSystems(new HashMap<>());
    userAgentConfig.setDevices(new HashMap<>());
  }

  /**
   * 获取随机操作系统字符串（从配置或fallback）。
   *
   * @param os 操作系统类型
   * @return 操作系统字符串
   */
  private String getRandomOSString(OSType os) {
    if (userAgentConfig != null && userAgentConfig.getOperatingSystems() != null) {
      Map<String, OSConfig> osConfigs = userAgentConfig.getOperatingSystems();
      OSConfig osConfig = osConfigs.get(os.name().toLowerCase());
      if (osConfig != null && osConfig.getStrings() != null && !osConfig.getStrings().isEmpty()) {
        return osConfig.getStrings().get(random.nextInt(osConfig.getStrings().size()));
      }
    }

    List<String> osStrings = OS_STRINGS.get(os);
    if (osStrings == null || osStrings.isEmpty()) {
      return "Windows NT 10.0; Win64; x64";
    }
    return osStrings.get(random.nextInt(osStrings.size()));
  }

  /**
   * 获取随机模板（从配置或fallback）。
   *
   * @param browser 浏览器类型
   * @param device 设备类型
   * @return 模板字符串
   */
  private String getRandomTemplate(BrowserType browser, DeviceType device) {
    if (userAgentConfig != null && userAgentConfig.getBrowsers() != null) {
      Map<String, BrowserConfig> browserConfigs = userAgentConfig.getBrowsers();
      BrowserConfig browserConfig = browserConfigs.get(browser.name().toLowerCase());
      if (browserConfig != null
          && browserConfig.getTemplates() != null
          && !browserConfig.getTemplates().isEmpty()) {
        List<String> templates = browserConfig.getTemplates();
        if (device == DeviceType.MOBILE && templates.size() > 1) {
          return templates.get(1);
        }
        return templates.get(0);
      }
    }

    List<String> templates;
    switch (browser) {
      case CHROME:
        templates = CHROME_TEMPLATES;
        break;
      case FIREFOX:
        templates = FIREFOX_TEMPLATES;
        break;
      case SAFARI:
        templates = SAFARI_TEMPLATES;
        break;
      case EDGE:
        templates = EDGE_TEMPLATES;
        break;
      default:
        templates = CHROME_TEMPLATES;
        break;
    }

    if (device == DeviceType.MOBILE && templates.size() > 1) {
      return templates.get(1);
    }

    return templates.get(0);
  }

  /**
   * 生成浏览器版本号（从配置或fallback）。
   *
   * @param browser 浏览器类型
   * @param realistic 是否真实
   * @return 版本号
   */
  private String generateVersion(BrowserType browser, boolean realistic) {
    if (!realistic) {
      return "1.0.0";
    }

    if (userAgentConfig != null && userAgentConfig.getBrowsers() != null) {
      Map<String, BrowserConfig> browserConfigs = userAgentConfig.getBrowsers();
      BrowserConfig browserConfig = browserConfigs.get(browser.name().toLowerCase());
      if (browserConfig != null && browserConfig.getVersions() != null) {
        VersionConfig versionConfig = browserConfig.getVersions();
        if (versionConfig != null) {
          int min = versionConfig.getMin() != null ? versionConfig.getMin() : 90;
          int max = versionConfig.getMax() != null ? versionConfig.getMax() : 120;
          int step = versionConfig.getStep() != null ? versionConfig.getStep() : 1;
          int version = min + random.nextInt((max - min) / step) * step;
          return String.valueOf(version);
        }
      }
    }

    switch (browser) {
      case CHROME:
        return String.format(
            "%d.0.%d.%d",
            90 + random.nextInt(20), 4000 + random.nextInt(1000), 100 + random.nextInt(200));
      case FIREFOX:
        return String.format("%d.0", 80 + random.nextInt(20));
      case SAFARI:
        return String.format(
            "%d.%d.%d", 14 + random.nextInt(3), random.nextInt(10), random.nextInt(10));
      case EDGE:
        return String.format(
            "%d.0.%d.%d",
            90 + random.nextInt(20), 1000 + random.nextInt(1000), random.nextInt(100));
      default:
        return "1.0.0";
    }
  }

  /**
   * 生成WebKit版本号（从配置或fallback）。
   *
   * @param realistic 是否真实
   * @return WebKit版本号
   */
  private String generateWebKitVersion(boolean realistic) {
    if (!realistic) {
      return "537.36";
    }

    if (userAgentConfig != null && userAgentConfig.getWebkitVersions() != null) {
      WebkitVersionConfig webkitConfig = userAgentConfig.getWebkitVersions();
      if (webkitConfig != null) {
        int min = webkitConfig.getMin() != null ? webkitConfig.getMin() : 605;
        int max = webkitConfig.getMax() != null ? webkitConfig.getMax() : 606;
        int step = webkitConfig.getStep() != null ? webkitConfig.getStep() : 1;
        int version = min + random.nextInt((max - min) / step) * step;
        return String.format("537.%d", version);
      }
    }

    return String.format("537.%d", 30 + random.nextInt(10));
  }

  /**
   * 生成Chrome版本号（用于Edge等基于Chromium的浏览器）（从配置或fallback）。
   *
   * @param realistic 是否真实
   * @return Chrome版本号
   */
  private String generateChromeVersion(boolean realistic) {
    if (!realistic) {
      return "91.0.4472.124";
    }

    if (userAgentConfig != null && userAgentConfig.getChromeVersions() != null) {
      ChromeVersionConfig chromeConfig = userAgentConfig.getChromeVersions();
      if (chromeConfig != null) {
        int min = chromeConfig.getMin() != null ? chromeConfig.getMin() : 90;
        int max = chromeConfig.getMax() != null ? chromeConfig.getMax() : 120;
        int step = chromeConfig.getStep() != null ? chromeConfig.getStep() : 1;
        int major = min + random.nextInt((max - min) / step) * step;
        int build = 4000 + random.nextInt(1000);
        int patch = 100 + random.nextInt(200);
        return String.format("%d.0.%d.%d", major, build, patch);
      }
    }

    return String.format(
        "%d.0.%d.%d",
        90 + random.nextInt(20), 4000 + random.nextInt(1000), 100 + random.nextInt(200));
  }

  /** User-Agent配置类。 */
  @SuppressWarnings("unused")
  private static class UserAgentConfig {
    private Map<String, BrowserConfig> browsers;
    private Map<String, OSConfig> operatingSystems;
    private Map<String, DeviceConfig> devices;
    private WebkitVersionConfig webkitVersions;
    private ChromeVersionConfig chromeVersions;

    public Map<String, BrowserConfig> getBrowsers() {
      return browsers;
    }

    public void setBrowsers(Map<String, BrowserConfig> browsers) {
      this.browsers = browsers;
    }

    public Map<String, OSConfig> getOperatingSystems() {
      return operatingSystems;
    }

    public void setOperatingSystems(Map<String, OSConfig> operatingSystems) {
      this.operatingSystems = operatingSystems;
    }

    public Map<String, DeviceConfig> getDevices() {
      return devices;
    }

    public void setDevices(Map<String, DeviceConfig> devices) {
      this.devices = devices;
    }

    public WebkitVersionConfig getWebkitVersions() {
      return webkitVersions;
    }

    public void setWebkitVersions(WebkitVersionConfig webkitVersions) {
      this.webkitVersions = webkitVersions;
    }

    public ChromeVersionConfig getChromeVersions() {
      return chromeVersions;
    }

    public void setChromeVersions(ChromeVersionConfig chromeVersions) {
      this.chromeVersions = chromeVersions;
    }
  }

  /** 浏览器配置类。 */
  @SuppressWarnings("unused")
  private static class BrowserConfig {
    private String name;
    private List<String> templates;
    private VersionConfig versions;
    private int weight;
    private String description;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getTemplates() {
      return templates;
    }

    public void setTemplates(List<String> templates) {
      this.templates = templates;
    }

    public VersionConfig getVersions() {
      return versions;
    }

    public void setVersions(VersionConfig versions) {
      this.versions = versions;
    }

    public int getWeight() {
      return weight;
    }

    public void setWeight(int weight) {
      this.weight = weight;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  /** 操作系统配置类。 */
  @SuppressWarnings("unused")
  private static class OSConfig {
    private String name;
    private List<String> strings;
    private int weight;
    private String description;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getStrings() {
      return strings;
    }

    public void setStrings(List<String> strings) {
      this.strings = strings;
    }

    public int getWeight() {
      return weight;
    }

    public void setWeight(int weight) {
      this.weight = weight;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  /** 设备配置类。 */
  @SuppressWarnings("unused")
  private static class DeviceConfig {
    private String name;
    private int weight;
    private String description;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getWeight() {
      return weight;
    }

    public void setWeight(int weight) {
      this.weight = weight;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  /** 版本配置类。 */
  @SuppressWarnings("unused")
  private static class VersionConfig {
    private Integer min;
    private Integer max;
    private Integer step;

    public Integer getMin() {
      return min;
    }

    public void setMin(Integer min) {
      this.min = min;
    }

    public Integer getMax() {
      return max;
    }

    public void setMax(Integer max) {
      this.max = max;
    }

    public Integer getStep() {
      return step;
    }

    public void setStep(Integer step) {
      this.step = step;
    }
  }

  /** WebKit版本配置类。 */
  @SuppressWarnings("unused")
  private static class WebkitVersionConfig {
    private Integer min;
    private Integer max;
    private Integer step;
    private String description;

    public Integer getMin() {
      return min;
    }

    public void setMin(Integer min) {
      this.min = min;
    }

    public Integer getMax() {
      return max;
    }

    public void setMax(Integer max) {
      this.max = max;
    }

    public Integer getStep() {
      return step;
    }

    public void setStep(Integer step) {
      this.step = step;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  /** Chrome版本配置类。 */
  @SuppressWarnings("unused")
  private static class ChromeVersionConfig {
    private Integer min;
    private Integer max;
    private Integer step;
    private String description;

    public Integer getMin() {
      return min;
    }

    public void setMin(Integer min) {
      this.min = min;
    }

    public Integer getMax() {
      return max;
    }

    public void setMax(Integer max) {
      this.max = max;
    }

    public Integer getStep() {
      return step;
    }

    public void setStep(Integer step) {
      this.step = step;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}
