# DataForge配置文件优化 - �即行动指南

## 📋 第一步：改造CurrencyGenerator

**目标**：将CurrencyGenerator从硬编码数据改造为使用currencies.yml配置文件  
**预计时间**：3-4小时  
**难度**：高（需要处理约50种货币数据）

### 准备工作

#### 1. 分析现有代码

```java
// 关键代码位置
// 1. 第143-206行：initializeCurrencies()方法（硬编码数据）
// 2. 第268-305行：selectCurrency()方法（选择逻辑）
// 3. 第308-324行：generateOutput()方法（生成输出）
```

#### 2. 创建配置类定义

参考`LicensePlateConfig`结构，在`CurrencyGenerator.java`类中添加：

```java
/** 货币配置类。 */
public static class CurrencyConfig {
  private Map<String, CurrencyData> currencies;
  private SettingsConfig settings;
  private Map<String, Object> outputTypes;
  private Map<String, Object> amountFormats;
  private Map<String, Object> regions;

  // getters and setters...
}

/** 货币数据类。 */
public static class CurrencyData {
  private String symbol;
  private String name;
  private String region;
  private int digits;
  private boolean common;

  public CurrencyData(
      String code,
      String symbol,
      String name,
      String region,
      int digits,
      boolean common) {
    this.code = code;
    this.symbol = symbol;
    this.name = name;
    this.region = region;
    this.digits = digits;
    this.common = common;
  }

  // getters...
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

#### 3. 添加配置加载机制

```java
private static final String DEFAULT_CONFIG_FILE = "data/currencies.yml";
private volatile CurrencyConfig currencyConfig;
private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

/**
 * 确保配置已加载。
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
}

/**
 * 加载配置。
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
    // 初始化内置fallback数据
    logger.info("Currency fallback config initialized");
}
```

#### 4. 修改generate()方法

在`generate()`方法开始处添加配置加载调用：

```java
@Override
public String generate(FieldConfig config, DataForgeContext context) {
    try {
        // 确保配置已加载
        ensureConfigLoaded(config);
        
        // ... 其他现有代码保持不变 ...
        
        // 存储到上下文（已有）
        context.put("currency_code", currency.getCode());
        context.put("currency_symbol", currency.getSymbol());
        context.put("currency_name", currency.getName());
        context.put("currency_region", currency.getRegion());
        
        return result;
        
    } catch (Exception e) {
        logger.error("Failed to generate currency", e);
        return "USD";
    }
}
```

#### 5. 注释掉或删除硬编码数据初始化

注释掉第143-206行的`initializeCurrencies()`静态代码块：

```java
/*
// static {
//     initializeCurrencies();
// }
*/
```

或者改为：

```java
// 在类加载时通过配置文件初始化
static {
    // 将会通过loadConfig()从配置文件加载
}
```

#### 6. 编写配置测试类

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
  @DisplayName("生成货币符号")
  void should_generate_currency_symbol() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("type", "SYMBOL"));

    String symbol = currencyGenerator.generate(config, context);
    assertNotNull(symbol);
    assertFalse(symbol.isBlank());
    assertTrue(symbol.matches("^[\\pP{ScY}£₹₹₿₽₮₹¥₮]+"));
  }

  @Test
  @DisplayName("指定货币生成")
  void should_generate_specified_currency() {
    DataForgeContext context = new DataForgeContext();
    FieldConfigWrapper config = new FieldConfigWrapper();
    config.setParams(Map.of("currency", "USD"));

    String currency = currencyGenerator.generate(config, context);
    assertNotNull(currency);
    // 应该返回货币符号（$）或货币代码（USD）
    assertTrue(currency.equals("$") || currency.equals("USD"));
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
    // 金额应该是数字格式
    assertTrue(amount.matches("\\d+(\\.\\d+)?"));
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

#### 7. 验证测试

```bash
# 编译
mvn clean compile

# 运行配置测试
mvn test -Dtest=CurrencyGeneratorConfigTest

# 运行完整测试套件
mvn test
```

---

## 🎯 第二步：改造LandlineGenerator

**目标**：将LandlineGenerator从硬编码数据改造为使用landline-numbers.yml配置文件  
**预计时间**：3-4小时  
**难度**：中（5个国家，67个区号数据）

### 准备工作

#### 1. 分析现有代码

```java
// 关键代码位置
// 1. 第58-197行：初始化方法（硬编码数据）
// 2. 第210-324行：区号选择方法（选择逻辑）
```

#### 2. 创建配置类定义

参考`LicensePlateConfig`结构，在`LandlineGenerator.java`类中添加：

```java
/** 国家信息类。 */
public static class CountryInfo {
  final String countryCode;
  private final Map<String, String> areaCodes;
  final int[] numberLengths;
  private final String separator;
  private final String description;

  // getters...
}

/** 区号信息类。 */
public static class AreaCodeData {
  private String name;
  private int weight;

  // getters and setters...
}

/** 固话配置类。 */
public static class LandlineConfig {
  private Map<String, CountryInfo> countries;
  private SettingsConfig settings;
  private Map<String, Object> outputFormats;

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

#### 3. 添加配置加载机制

```java
private static final String DEFAULT_CONFIG_FILE = "data/landline-numbers.yml";
private volatile LandlineConfig landlineConfig;
private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

// 确保配置已加载
private void ensureConfigLoaded(FieldConfig config) {
    if (landlineConfig == null) {
        synchronized (this) {
            if (landlineConfig == null) {
                loadConfig(config);
            }
        }
    }
  }

// 加载配置
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

// 初始化fallback配置
private void initializeFallbackConfig() {
    landlineConfig = new LandlineConfig();
    logger.info("Landline fallback config initialized");
}
```

#### 4. 编写配置测试类

创建`LandlineGeneratorConfigTest.java`，参考`LicensePlateGeneratorConfigTest.java`模式。

---

## 📋 阶段3：完成剩余6个生成器改造

### 预计估算

| 生成器 | 难度 | 预计时间 |
|--------|----------|----------|
| TimezoneGenerator | 中 | 2-3小时 | 约6个地区，50+个时区 |
| HttpStatusGenerator | 低 | 2-3小时 | 约40个状态码 |
| MimeTypeGenerator | 中 | 2-3小时 | 约15个类型，100+个扩展名 |
| EducationGenerator | 低 | 1-2小时 | 约7个学历层次 |
| HttpHeaderGenerator | 低 | 1-2小时 | 约15个HTTP头部 |
| OccupationGenerator | 低 | 0.5-1小时 | 50个职业 |
| CompanyNameGenerator | 低 | 0.5-1小时 | 2个文件 |
| EthnicityGenerator | 低 | 0.5-1小时 | 56个民族 |
| ReligionGenerator | 低 | 0.5-1小时 | 10个宗教 |
| ColorGenerator | 低 | 0.5-1小时 | 常个颜色 |
| DomainGenerator | 低 | 0.5-1小时 | 2个文件 |

**中期计划（3-4周）预计完成时间**：3-4周

---

## 📋 第四步：补充6个生成器的内置数据文件

### 生成器列表

| 生成器 | 数据文件 | 数据量 |
|--------|----------|----------|----------|
| OccupationGenerator | occupations.txt | ~50个职业 |
| CompanyNameGenerator | company-prefixes.txt, company-suffixes.txt | 2个文件 |
| EthnicityGenerator | ethnicities.txt | 56个民族 |
| ReligionGenerator | religions.txt | 10个宗教 |
| ColorGenerator | colors.yml | 常个颜色 |
| DomainGenerator | tld-generics.txt, tld-countries.txt | 2个文件 |

**数据文件创建优先级**：

1. **高优先级**（立即创建）：
   - `occupations.txt`（50个职业数据量大）
   - `ethnicities.txt`（56个民族数据很重要）
   - `religions.txt`（10个宗教数据有限）

2. **中优先级**：
   - `company-prefixes.txt`（公司前缀）
   - `company-suffixes.txt`（公司后缀）
   - `colors.yml`（颜色配置）

3. **低优先级**：
   - `tld-generics.txt`（顶级域名）
   - `tld-countries.txt`（国家代码）

**预计时间**：30分钟 - 1小时

---

## 🎋 第五步：集成测试和文档完善

### 集成测试套件验证

```bash
# 1. 运行完整测试套件
mvn test

# 2. 性能基准测试
mvn test -Dtest=*PerfTest
```

### 文档完善

1. **创建《配置文件开发指南》**
   - 配置文件格式规范
   - 配置文件结构说明
   - 加载机制说明
   - 最佳实践

2. **更新用户文档**
   - 配置文件使用说明
   - 自定义配置文件方法
   - 配置文件示例

3. **更新README.md**
   - 添加配置文件章节
   - 提供快速开始指南

---

## 🎯 执行建议

### 立即执行

**第一步**：
```bash
# 1. 备份现CurrencyGenerator.java
# 2. 创建配置类定义
# 3. 添加配置加载机制
# 4. 修改生成逻辑
# 5. 编写配置测试类
# 6. 运行测试验证
```

**第二步**：
```bash
# 1. 备份现LandlineGenerator.java
# 2. 创建配置类定义
# 3. 添加配置加载机制
# 4. 修改生成逻辑
# 5. 编写配置测试类
# 6. 运行测试验证
```

**继续后续步骤**：
- 按�照上述步骤完成第三、四、五、六步

---

## 🎯 注意事项

### 1. 代码兼容性

- ✅ 保持所有现有API不变
- ✅ 保持所有参数配置不变
- ✅ 生成结果与改造前一致

### 2. 配置文件格式

- ✅ YAML缩进使用2个空格
- ✅ 字符串使用双引号
- ✅ 布尔表项使用`-`
- ✅ Map键使用`:`:

### 3. 异常处理

- ✅ 配置文件缺失时使用fallback
- ✅ 配置文件错误时使用fallback
- ✅ 详细的日志记录
- ✅ 不影响现有功能

### 4. 测试覆盖

- ✅ 每个生成器都需要配置测试类
- ✅ 测试用例应覆盖所有边界条件
- ✅ 必须验证fallback机制
- ✅ 必须验证配置文件加载

### 5. 性能考虑

- ✅ 配置加载采用懒加载
- ✅ 配置数据使用缓存
- ✅ 避免重复I/O

---

## 🎯 成功标准

### ✅ 功能正确性
- 所有生成器的现有功能保持不变
- 生成结果与硬编码数据版本一致
- 随机性测试通过

### ✅ 性能指标
- 配置加载耗时 < 100ms
- 首次生成耗时 < 10ms
- 后续生成耗时 < 1ms

### ✅ 质量指标
- 测试通过率：100%
- 并发吞吐量 > 10000次/秒

### ✅ 兼容性指标
- 向后兼容：现有API完全兼容
- 参数配置：所有参数保持不变
- Spring版本兼容：兼容Spring Boot 2.x/3.x
- Java版本兼容：兼容Java 8/11/17

---

## 🎯 模板参考

### 配置文件示例

**已创建的配置文件**：
- license-plates.yml
- currencies.yml
- landline-numbers.yml

### 已配置化的生成器**：
- PhoneGenerator.java
- MeasurementGenerator.java
- FilePathGenerator.java
- TradingCalendarGenerator.java
- UserAgentGenerator.java
- LicensePlateGenerator.java

### 测试类示例**：
- PhoneGeneratorConfigTest.java
- MeasurementGeneratorConfigTest.java
- LicensePlateGeneratorConfigTest.java

---

## 🚀 下一步

按照本指南的步骤，逐步完成剩余生成器的改造工作。

**祝改造顺利！** 🎉✨