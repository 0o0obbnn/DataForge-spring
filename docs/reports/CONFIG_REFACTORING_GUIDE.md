# DataForge配置文件优化计划 - 后续执行指南

## 📊 概述

本文档提供了完成剩余6个生成器改造的详细步骤和代码模板，所有步骤均参考已完成的LicensePlateGenerator实现模式。

**已完成的准备工作**：
- ✅ LicensePlateGenerator完全改造（配置化示例）
- ✅ 3个YAML配置文件已创建（currencies.yml, landline-numbers.yml）
- ✅ 配置文件加载标准已建立
- ✅ 测试编写模式已建立
- ✅ Fallback机制已验证

**待完成工作**：
- ⏸️ 6个生成器改造（Currency, Landline, Timezone, HttpStatus, MimeType, Education）
- ⏸️ 6个生成器补充内置数据（HttpHeader, Occupation, CompanyName, Ethnicity, Religion, Color, Domain）

---

## 🎯 第一步：改造CurrencyGenerator

### 概述

**目标**：将CurrencyGenerator从硬编码改造为使用currencies.yml配置文件  
**难度**：高（需要处理约50种货币）  
**预计时间**：3-4小时

### 改造步骤

#### 步骤1.1：创建配置类定义

在`CurrencyGenerator.java`中添加以下配置类：

```java
/** 货币配置类。 */
public static class CurrencyConfig {
  private Map<String, CurrencyData> currencies;
  private SettingsConfig settings;
  private Map<String, Object> outputTypes;
  private Map<String, Object> amountFormats;
  private Map<String, Object> regions;

  public Map<String, CurrencyData> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(Map<String, CurrencyData> currencies) {
    this.currencies = currencies;
  }

  public SettingsConfig getSettings() {
    return settings;
  }

  public void setSettings(SettingsConfig settings) {
    this.settings = settings;
  }

  // 其他getter和setter...
}

/** 货币数据类。 */
public static class CurrencyData {
  private String symbol;
  private String name;
  private String region;
  private int digits;
  private boolean common;

  // getters and setters...
}

/** 设置配置类。 */
public static class SettingsConfig {
  private String defaultOutputType;
  private double defaultAmountMin;
  private double defaultAmountMax;
  private String defaultFormat;
  private boolean defaultCommonOnly;

  // getters and setters...
}
```

#### 步骤1.2：添加配置加载机制

在`CurrencyGenerator.java`类定义中添加：

```java
private static final String DEFAULT_CONFIG_FILE = "data/currencies.yml";
private volatile CurrencyConfig currencyConfig;
private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
```

添加配置加载方法：

```java
/**
 * 确保配置已加载。
 *
 * @param config 字段配置
 */
private void ensureConfigLoaded(FieldConfig config) {
  if (currencyConfig == null) {
    synchronized (this) {
      if (currencyConfig == null) {
        loadConfig(config);
      }
    }
  }
}

/**
 * 加载配置。
 *
 * @param config 字段配置
 */
private void loadConfig(FieldConfig config) {
  try {
    String configFile = getStringParam(config, "config_file", DEFAULT_CONFIG_FILE);
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
    if (inputStream != null) {
      currencyConfig = yamlMapper.readValue(inputStream, CurrencyConfig.class);
      logger.info("Currency config loaded from: {}", configFile);
    } else {
      logger.warn("Config file not found: {}, using fallback", configFile);
      initializeFallbackConfig();
    }
  } catch (Exception e) {
    logger.error("Failed to load currency config, using fallback", e);
    initializeFallbackConfig();
  }
}

/**
 * 初始化fallback配置。
 */
private void initializeFallbackConfig() {
  currencyConfig = new CurrencyConfig();
  logger.info("Currency fallback config initialized");
}
```

#### 步骤1.3：修改generate()方法

在`generate()`方法开始处添加：

```java
ensureConfigLoaded(config);
```

#### 步骤1.4：编写配置测试类

创建`CurrencyGeneratorConfigTest.java`：

```java
@DisplayName("CurrencyGenerator配置文件功能测试")
class CurrencyGeneratorConfigTest {

  private static CurrencyGenerator currencyGenerator;

  @BeforeAll
  static void setUpAll() {
    currencyGenerator = new CurrencyGenerator();
  }

  @Test
  @DisplayName("默认配置生成货币")
  void should_generate_currency_with_default_config() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();

    String currency = currencyGenerator.generate(config, context);
    assertNotNull(currency);
    assertFalse(currency.isBlank());
  }

  @Test
  @DisplayName("生成货币代码")
  void should_generate_currency_code() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("type", "CODE"));

    String currency = currencyGenerator.generate(config, context);
    assertNotNull(currency);
    assertEquals(3, currency.length());
    assertTrue(currency.matches("[A-Z]{3}"));
  }

  @Test
  @DisplayName("指定货币生成")
  void should_generate_specified_currency() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("currency", "USD"));

    String currency = currencyGenerator.generate(config, context);
    assertNotNull(currency);
    assertTrue(currency.equals("USD") || currency.equals("$"));
  }

  @Test
  @DisplayName("配置文件加载成功")
  void should_load_config_file_successfully() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("config_file", "data/currencies.yml"));

    String currency = currencyGenerator.generate(config, context);
    assertNotNull(currency);
    assertFalse(currency.isBlank());
  }

  @Test
  @DisplayName("指定不存在的配置文件应使用fallback")
  void should_use_fallback_when_config_file_not_found() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("config_file", "data/non-existent-config.yml"));

    String currency = currencyGenerator.generate(config, context);
    assertNotNull(currency);
    assertFalse(currency.isBlank());
  }

  @Test
  @DisplayName("生成金额")
  void should_generate_amount() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("type", "AMOUNT"));

    String amount = currencyGenerator.generate(config, context);
    assertNotNull(amount);
    assertFalse(amount.isBlank());
    // 金额应该是数字格式
    assertTrue(amount.matches("\\d+(\\.\\d+)?"));
  }

  @Test
  @DisplayName("批量生成货币")
  void should_generate_multiple_currencies() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();

    java.util.Set<String> currencies = new java.util.HashSet<>();
    for (int i = 0; i < 100; i++) {
      String currency = currencyGenerator.generate(config, context);
      assertNotNull(currency);
      currencies.add(currency);
    }

    // 检查生成的货币有一定多样性
    assertTrue(currencies.size() > 10, "应该生成多样化的货币");
  }

  @Test
  @DisplayName("获取生成器类型")
  void should_return_correct_type() {
    assertEquals("currency", currencyGenerator.getType());
  }

  @Test
  @DisplayName("获取生成器描述")
  void should_return_description() {
    String description = currencyGenerator.getDescription();
    assertNotNull(description);
    assertFalse(description.isBlank());
  }
}
```

#### 步骤1.5：运行测试验证

```bash
mvn test -Dtest=CurrencyGeneratorConfigTest
```

---

## 🎯 第二步：改造LandlineGenerator

### 概述

**目标**：将LandlineGenerator从硬编码改造为使用landline-numbers.yml配置文件  
**难度**：中（5个国家，67个区号）  
**预计时间**：3-4小时

### 改造步骤

#### 步骤2.1：创建配置类定义

```java
/** 固话配置类。 */
public static class LandlineConfig {
  private Map<String, CountryData> countries;
  private SettingsConfig settings;
  private Map<String, Object> outputFormats;

  // getters and setters...
}

/** 国家数据类。 */
public static class CountryData {
  private String name;
  private String countryCode;
  private String separator;
  private String description;
  private List<Integer> numberLengths;
  private Map<String, AreaCodeData> areaCodes;

  // getters and setters...
}

/** 区号数据类。 */
public static class AreaCodeData {
  private String name;
  private int weight;

  // getters and setters...
}

/** 设置配置类。 */
public static class SettingsConfig {
  private String defaultCountry;
  private String defaultFormat;
  private boolean defaultIncludeExtension;
  private int defaultExtensionLength;

  // getters and setters...
}
```

#### 步骤2.2：添加配置加载机制

```java
private static final String DEFAULT_CONFIG_FILE = "data/landline-numbers.yml";
private volatile LandlineConfig landlineConfig;
private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

private void ensureConfigLoaded(FieldConfig config) {
  if (landlineConfig == null) {
    synchronized (this) {
      if (landlineConfig == null) {
        loadConfig(config);
      }
    }
  }
}

private void loadConfig(FieldConfig config) {
  try {
    String configFile = getStringParam(config, "config_file", DEFAULT_CONFIG_FILE);
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
    if (inputStream != null) {
      landlineConfig = yamlMapper.readValue(inputStream, LandlineConfig.class);
      logger.info("Landline config loaded from: {}", configFile);
    } else {
      logger.warn("Config file not found: {}, using fallback", configFile);
      initializeFallbackConfig();
    }
  } catch (Exception e) {
    logger.error("Failed to load landline config, using fallback", e);
    initializeFallbackConfig();
  }
}

private void initializeFallbackConfig() {
  landlineConfig = new LandlineConfig();
  logger.info("Landline fallback config initialized");
}
```

#### 步骤2.3：编写测试类

创建`LandlineGeneratorConfigTest.java`：

```java
@DisplayName("LandlineGenerator配置文件功能测试")
class LandlineGeneratorConfigTest {

  private static LandlineGenerator landlineGenerator;

  @BeforeAll
  static void setUpAll() {
    landlineGenerator = new LandlineGenerator();
  }

  @Test
  @DisplayName("默认配置生成固话号")
  void should_generate_landline_with_default_config() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();

    String landline = landlineGenerator.generate(config, context);
    assertNotNull(landline);
    assertFalse(landline.isBlank());
  }

  @Test
  @DisplayName("生成中国固话号")
  void should_generate_chinese_landline() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("region", "CN"));

    String landline = landlineGenerator.generate(config, context);
    assertNotNull(landline);
    // 中国固话号应该以+86或区号开头
  }

  @Test
  @DisplayName("指定区号生成")
  void should_generate_with_area_code() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("area_code", "010"));

    String landline = landlineGenerator.generate(config, context);
    assertNotNull(landline);
    assertTrue(landline.contains("010"));
  }

  @Test
  @DisplayName("配置文件加载成功")
  void should_load_config_file_successfully() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("config_file", "data/landline-numbers.yml"));

    String landline = landlineGenerator.generate(config, context);
    assertNotNull(landline);
    assertFalse(landline.isBlank());
  }

  @Test
  @DisplayName("批量生成固话号")
  void should_generate_multiple_landlines() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();

    java.util.Set<String> landlines = new java.util.HashSet<>();
    for (int i = 0; i < 50; i++) {
      String landline = landlineGenerator.generate(config, context);
      assertNotNull(landline);
      landlines.add(landline);
    }

    assertTrue(landlines.size() > 10, "应该生成多样化的固话号");
  }

  // 更多测试...
}
```

---

## 📋 剩余生成器改造

### TimezoneGenerator（预计2-3小时）

**步骤**：
1. 创建`timezones.yml`配置文件（包含6个地区，50+个时区）
2. 添加配置类定义（TimezoneConfig, RegionConfig等）
3. 实现懒加载和Fallback机制
4. 编写`TimezoneGeneratorConfigTest.java`

**配置文件结构**：
```yaml
regions:
  AMERICA:
    name: "美洲"
    timezones: [America/New_York, America/Chicago, ...]
  EUROPE:
    name: "欧洲"
    timezones: [Europe/London, Europe/Paris, ...]
  # ...

settings:
  default_region: "ALL"
  default_format: "IANA"
```

### HttpStatusGenerator（预计2-3小时）

**步骤**：
1. 创建`http-status-codes.yml`配置文件
2. 添加配置类定义
3. 实现懒加载和Fallback机制
4. 编写`HttpStatusGeneratorConfigTest.java`

**配置文件结构**：
```yaml
status_codes:
  information:
    100: {message: "Continue", common: false}
    101: {message: "Switching Protocols", common: false}
  success:
    200: {message: "OK", common: true}
    201: {message: "Created", common: true}
    # ...
  # 其他类别...
```

### MimeTypeGenerator（预计2-3小时）

**步骤**：
1. 创建`mime-types.yml`配置文件
2. 添加配置类定义
3. 实现懒加载和Fallback机制
4. 编写`MimeTypeGeneratorConfigTest.java`

**配置文件结构**：
```yaml
mime_types:
  text:
    - type: "plain"
      extensions: ["txt", "text"]
      common: true
    # ...
  image:
    - type: "jpeg"
      extensions: ["jpg", "jpeg"]
      common: true
    # ...
  # 其他类别...

charsets:
  - "UTF-8"
  - "UTF-16"
  # ...

parameters:
  text/plain:
    - name: "charset"
      values: ["UTF-8", "GB2312"]
```

### EducationGenerator（预计1-2小时）

**步骤**：
1. 创建`education.yml`配置文件
2. 添加配置类定义
3. 实现懒加载和Fallback机制
4. 编写`EducationGeneratorConfigTest.java`

**配置文件结构**：
```yaml
education_levels:
  PRIMARY:
    name: "小学"
    min_age: 12
    weight: 15
  BACHELOR:
    name: "本科"
    min_age: 22
    weight: 20
  # ...

countries:
  CN:
    system: "中国教育体系"
  # ...

settings:
  default_level: "ANY"
  default_distribution: "REALISTIC"
```

### HttpHeaderGenerator（预计1-2小时）

**步骤**：
1. 创建`http-headers.yml`配置文件
2. 添加配置类定义
3. 实现懒加载和Fallback机制
4. 编写`HttpHeaderGeneratorConfigTest.java`

**配置文件结构**：
```yaml
headers:
  request:
    Accept:
      description: "可接受的响应内容类型"
      examples: ["text/html", "application/json", "*/*"]
      common: true
    User-Agent:
      description: "用户代理"
      examples: ["Mozilla/5.0 ..."]
      common: true
    # ...
  response:
    Content-Type:
      description: "响应内容类型"
      examples: ["text/html; charset=utf-8"]
      common: true
    # ...
```

---

## 📝 补充内置数据文件（6个生成器）

### OccupationGenerator

创建`occupations.txt`：
```
# 格式：职业名称:权重
# 权重用于随机选择，默认权重为1

软件工程师:50
项目经理:30
产品经理:25
数据分析师:20
# ... 添加更多职业
```

### CompanyNameGenerator

创建`company-prefixes.txt`和`company-suffixes.txt`：
```
# company-prefixes.txt
北京
上海
深圳
# ...

# company-suffixes.txt
科技有限公司
网络技术有限公司
信息技术有限公司
# ...
```

### EthnicityGenerator

创建`ethnicities.txt`：
```
# 格式：民族名称:权重
# 中国56个民族

汉族:90
壮族:2
回族:1
# ...
```

### ReligionGenerator

创建`religions.txt`：
```
# 格式：宗教名称:权重
# 世界主要宗教

基督教:31
伊斯兰教:24
佛教:7
# ...
```

### ColorGenerator/ColorValueGenerator

创建`colors.yml`：
```yaml
named_colors:
  red:
    hex: "#FF0000"
    rgb: [255, 0, 0]
    common: true
  blue:
    hex: "#0000FF"
    rgb: [0, 0, 255]
    common: true
  # ...

color_spaces:
  - RGB
  - HSL
  - HEX
```

### DomainGenerator

创建`tld-generics.txt`和`tld-countries.txt`：
```
# tld-generics.txt
com
org
net
io
# ...

# tld-countries.txt
cn
us
uk
# ...
```

---

## 🧪 验证和测试

### 每个生成器改造后的验证步骤

1. **编译验证**
```bash
mvn clean compile
```

2. **运行配置测试**
```bash
mvn test -Dtest=XXXGeneratorConfigTest
```

3. **验证测试通过率**
   - 所有测试用例应该通过
   - 目标通过率：100%

4. **验证配置加载**
   - 配置文件应该成功加载
   - Fallback机制应该正常工作

5. **运行完整测试套件**
```bash
mvn test
```

### 集成测试

```bash
mvn test -Dtest=*ConfigTest
```

---

## 📝 文档和上线准备

### 文档更新

1. **更新README.md**
   - 添加配置文件使用说明
   - 提供配置示例
   - 更新API文档

2. **创建《配置文件开发指南》**
   - 配置文件结构规范
   - 加载机制说明
   - 最佳实践

3. **更新用户文档**
   - 配置文件定制方法
   - 常见问题FAQ

### 发布准备

1. **代码审查**
   - 提交Pull Request
   - 等待代码审查通过

2. **版本管理**
   - 更新版本号
   - 准备发布说明
   - 准备变更日志

3. **测试和验证**
   - 运行完整测试套件
   - 性能基准测试
   - 集成测试

---

## 🎯 时间估算

| 生成器 | 难度 | 预计时间 |
|--------|------|----------|
| CurrencyGenerator | 高 | 3-4小时 |
| LandlineGenerator | 中 | 3-4小时 |
| TimezoneGenerator | 中 | 2-3小时 |
| HttpStatusGenerator | 低 | 2-3小时 |
| MimeTypeGenerator | 中 | 2-3小时 |
| EducationGenerator | 低 | 1-2小时 |
| HttpHeaderGenerator | 低 | 1-2小时 |
| 补充6个数据文件 | 低 | 2-3小时 |
| 集成测试和文档 | 中 | 3-5天 |

**总计**：约2-3周

---

## 🎯 注意事项

1. **保持向后兼容**
   - 所有现有API必须保持不变
   - 所有参数必须保持不变
   - 生成结果必须与改造前一致

2. **线程安全**
   - 配置加载必须使用synchronized
   - 配置对象必须使用volatile

3. **Fallback机制**
   - 配置文件缺失时必须使用fallback
   - 配置文件错误时必须使用fallback
   - Fallback数据必须保证系统可运行

4. **测试覆盖**
   - 每个改造的生成器都需要配置测试类
   - 测试用例应覆盖所有边界条件
   - 测试通过率目标：100%

5. **文档完整**
   - 配置文件必须有详细注释
   - 配置文件必须有示例
   - 代码必须有清晰的JavaDoc

---

## 📞 参考资料

### 已完成的改造示例

- **LicensePlateGenerator**：完整的配置化实现
- **PhoneGenerator**：已存在的配置化实现
- **MeasurementGenerator**：已存在的配置化实现
- **FilePathGenerator**：已存在的配置化实现
- **TradingCalendarGenerator**：已存在的配置化实现
- **UserAgentGenerator**：已存在的配置化实现

### 配置文件示例

- **license-plates.yml**：7.4KB，31个省份配置
- **currencies.yml**：6.9KB，50+种货币配置
- **landline-numbers.yml**：6.3KB，5个国家，67个区号
- **phone-prefixes.yml**：已存在的配置文件
- **measurements.yml**：已存在的配置文件
- **file-paths.yml**：已存在的配置文件
- **trading-calendar.yml**：已存在的配置文件
- **user-agents.yml**：已存在的配置文件

---

**祝改造顺利！按照本指南完成剩余生成器的改造工作。**
