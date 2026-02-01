# N2 & N4 修复执行计划

> **计划制定日期**: 2026-02-01
> **优先级**: P1 (高优先级)
> **预计工时**: 45分钟
> **影响范围**: 2个文件，1个新增常量类
> **编译验证**: 必须通过
> **测试验证**: 建议执行

---

## 📋 修复范围

基于 `CODE_REVIEW_VERIFICATION_REPORT.md` 的验证结果，本次修复以下2个确认问题：

| ID | 问题 | 文件 | 工时 | 风险等级 |
|----|------|------|------|----------|
| **N2** | ConfigLoader参数解析异常处理 | ConfigLoader.java | 30分钟 | 🟡 中 |
| **N4** | API请求缺少@Size限制 | GenerateRequest.java | 15分钟 | 🟢 低 |

---

## 🎯 修复目标

### N2: ConfigLoader 参数解析异常处理

**当前问题**:
```java
// ConfigLoader.java:51-66
public ForgeConfig mergeWithCliArgs(ForgeConfig config, String[] args) {
    for (int i = 0; i < args.length - 1; i++) {
        String arg = args[i];
        String value = args[i + 1];

        switch (arg) {
            case "-c", "--count" -> config.setCount(Integer.parseInt(value)); // ❌ 未处理异常
            case "-t", "--threads" -> config.setThreads(Integer.parseInt(value)); // ❌ 未处理异常
            case "--validate" -> config.setValidate(Boolean.parseBoolean(value));
            case "--seed" -> config.setSeed(Long.parseLong(value)); // ❌ 未处理异常
        }
    }
    return config;
}
```

**期望行为**:
1. 捕获 `NumberFormatException` 并提供友好的错误消息
2. 指明具体的参数名称和错误值
3. 抛出 `IllegalArgumentException` 而非让程序崩溃
4. 保持向后兼容性

**验证标准**:
```bash
# 测试用例1: 无效的count值
java -jar app.jar --count abc
# 期望: IllegalArgumentException with message "Invalid count value: 'abc'. Must be a valid integer."

# 测试用例2: 无效的threads值
java -jar app.jar --threads -1
# 期望: IllegalArgumentException with message "Invalid threads value: '-1'. Must be a positive integer."

# 测试用例3: 无效的seed值
java -jar app.jar --seed xyz
# 期望: IllegalArgumentException with message "Invalid seed value: 'xyz'. Must be a valid long."
```

---

### N4: API请求 @Size 限制

**当前问题**:
```java
// GenerateRequest.java:44-50
@NotEmpty(message = "Fields configuration cannot be empty")
@Valid
@Schema(
    description = "字段配置列表，定义每个字段的类型和参数。支持60+种数据生成器类型",
    requiredMode = Schema.RequiredMode.REQUIRED)
private List<FieldConfigWrapper> fields;
```

**期望行为**:
1. 添加 `@Size(max = 100)` 注解
2. 更新 Swagger 文档说明
3. 提供清晰的验证错误消息
4. 不破坏现有API使用方式

**验证标准**:
```json
// 测试用例: 发送超过100个字段的请求
POST /api/v1/dataforge/generate
{
  "count": 1,
  "fields": [
    // 101个字段配置
  ],
  "output": {...}
}
// 期望: HTTP 400 with message "Fields list cannot exceed 100 items"
```

---

## 🛠️ 详细修复方案

### 方案N2-1: ConfigLoader 参数解析改进

**步骤1**: 创建参数解析工具类

**新文件**: `data-forge-core/src/main/java/com/dataforge/config/CliArgumentParser.java`

```java
package com.dataforge.config;

import java.util.function.Function;

/**
 * CLI参数解析工具类
 *
 * <p>提供类型安全的参数解析和友好的错误消息
 *
 * @author DataForge Team
 * @since 1.0.0
 */
public final class CliArgumentParser {

  private CliArgumentParser() {
    // 工具类，禁止实例化
  }

  /**
   * 解析整数参数
   *
   * @param paramName 参数名称（用于错误消息）
   * @param value 参数值
   * @return 解析后的整数
   * @throws IllegalArgumentException 当value无法解析为整数时
   */
  public static int parseInt(String paramName, String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid %s value: '%s'. Must be a valid integer.",
              paramName, value),
          e);
    }
  }

  /**
   * 解析长整数参数
   *
   * @param paramName 参数名称（用于错误消息）
   * @param value 参数值
   * @return 解析后的长整数
   * @throws IllegalArgumentException 当value无法解析为长整数时
   */
  public static long parseLong(String paramName, String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid %s value: '%s'. Must be a valid long.",
              paramName, value),
          e);
    }
  }

  /**
   * 解析布尔参数
   *
   * @param paramName 参数名称（用于错误消息）
   * @param value 参数值
   * @return 解析后的布尔值
   */
  public static boolean parseBoolean(String paramName, String value) {
    try {
      return Boolean.parseBoolean(value);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid %s value: '%s'. Must be 'true' or 'false'.",
              paramName, value),
          e);
    }
  }

  /**
   * 解析带范围验证的整数
   *
   * @param paramName 参数名称
   * @param value 参数值
   * @param min 最小值（包含）
   * @param max 最大值（包含）
   * @return 解析并验证后的整数
   * @throws IllegalArgumentException 当值无效或超出范围时
   */
  public static int parseIntInRange(
      String paramName, String value, int min, int max) {
    int parsed = parseInt(paramName, value);
    if (parsed < min || parsed > max) {
      throw new IllegalArgumentException(
          String.format(
              "%s must be between %d and %d, got: %d",
              paramName, min, max, parsed));
    }
    return parsed;
  }
}
```

**步骤2**: 修改 ConfigLoader.java

**修改位置**: `ConfigLoader.java` 第51-66行

```java
/** 合并命令行参数到配置。 */
public ForgeConfig mergeWithCliArgs(ForgeConfig config, String[] args) {
  if (args == null || args.length == 0) {
    return config;
  }

  for (int i = 0; i < args.length - 1; i++) {
    String arg = args[i];
    String value = args[i + 1];

    try {
      switch (arg) {
        case "-c", "--count" -> {
          int count = CliArgumentParser.parseIntInRange("count", value, 1, 1_000_000_000);
          config.setCount(count);
        }
        case "-t", "--threads" -> {
          int threads = CliArgumentParser.parseIntInRange("threads", value, 1, 64);
          config.setThreads(threads);
        }
        case "--validate" ->
            config.setValidate(CliArgumentParser.parseBoolean("validate", value));
        case "--seed" -> config.setSeed(CliArgumentParser.parseLong("seed", value));
        default -> {
          // 忽略未知参数，记录警告
          logger.warn("Unknown CLI argument: {}, skipping", arg);
        }
      }
    } catch (IllegalArgumentException e) {
      logger.error("Failed to parse CLI argument: {} {}", arg, value, e);
      throw e; // 重新抛出以终止程序
    }
  }

  return config;
}
```

**优点**:
- ✅ 统一的错误处理逻辑
- ✅ 友好的错误消息
- ✅ 范围验证（count: 1-1B, threads: 1-64）
- ✅ 代码复用性高
- ✅ 易于扩展新参数类型

---

### 方案N4-1: GenerateRequest 字段列表限制

**修改位置**: `GenerateRequest.java` 第44-50行

```java
/** 字段配置列表 */
@NotEmpty(message = "Fields configuration cannot be empty")
@Size(max = 100, message = "Fields list cannot exceed 100 items")
@Valid
@Schema(
    description = "字段配置列表，定义每个字段的类型和参数。支持60+种数据生成器类型，最多支持100个字段",
    requiredMode = Schema.RequiredMode.REQUIRED)
private List<FieldConfigWrapper> fields;
```

**同时更新示例文档说明**:

**Swagger注解增强**:
```java
@Schema(
    description = """
        字段配置列表，定义每个字段的类型和参数。

        支持的数据类型：
        - 基础类型: uuid, string, integer, decimal, boolean
        - 个人信息: name, email, phone, idcard, address
        - 日期时间: date, time, datetime, timestamp
        - 业务数据: creditcard, bankcard, passport, license

        限制：
        - 最少1个字段
        - 最多100个字段
        """,
    requiredMode = Schema.RequiredMode.REQUIRED,
    example = "[{\"name\":\"id\",\"type\":\"uuid\"},{\"name\":\"name\",\"type\":\"name\"}]")
private List<FieldConfigWrapper> fields;
```

**优点**:
- ✅ 防止DoS攻击（超大列表）
- ✅ 内存保护
- ✅ 清晰的错误消息
- ✅ 文档自动更新

---

## 📊 测试验证计划

### N2 单元测试

**新测试文件**: `ConfigLoaderTest.java`

```java
package com.dataforge.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    @DisplayName("正常参数解析应成功")
    void testValidArguments() {
        ConfigLoader loader = new ConfigLoader();
        ForgeConfig config = new ForgeConfig();

        String[] args = {"--count", "1000", "--threads", "4"};
        ForgeConfig result = loader.mergeWithCliArgs(config, args);

        assertEquals(1000, result.getCount());
        assertEquals(4, result.getThreads());
    }

    @Test
    @DisplayName("无效count值应抛出IllegalArgumentException")
    void testInvalidCountValue() {
        ConfigLoader loader = new ConfigLoader();
        ForgeConfig config = new ForgeConfig();

        String[] args = {"--count", "abc"};

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args)
        );

        assertTrue(ex.getMessage().contains("Invalid count value"));
        assertTrue(ex.getMessage().contains("abc"));
    }

    @Test
    @DisplayName("count超出范围应抛出异常")
    void testCountOutOfRange() {
        ConfigLoader loader = new ConfigLoader();
        ForgeConfig config = new ForgeConfig();

        String[] args = {"--count", "0"};

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args)
        );

        assertTrue(ex.getMessage().contains("count must be between"));
    }

    @Test
    @DisplayName("无效seed值应抛出异常")
    void testInvalidSeedValue() {
        ConfigLoader loader = new ConfigLoader();
        ForgeConfig config = new ForgeConfig();

        String[] args = {"--seed", "xyz"};

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loader.mergeWithCliArgs(config, args)
        );

        assertTrue(ex.getMessage().contains("Invalid seed value"));
    }
}
```

### N4 集成测试

**修改现有测试**: `DataForgeControllerTest.java`

```java
@Test
@DisplayName("发送超过100个字段的请求应返回400错误")
void testGenerateDataWithTooManyFields() throws Exception {
    // 构建包含101个字段的请求
    GenerateRequest request = GenerateRequest.builder()
        .count(1)
        .output(createTestOutputConfig())
        .fields(createTooManyFields(101))
        .build();

    mockMvc.perform(post("/api/v1/dataforge/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            containsString("Fields list cannot exceed 100 items")));
}

private List<FieldConfigWrapper> createTooManyFields(int count) {
    List<FieldConfigWrapper> fields = new ArrayList<>();
    for (int i = 0; i < count; i++) {
        fields.add(FieldConfigWrapper.builder()
            .name("field" + i)
            .type("string")
            .build());
    }
    return fields;
}
```

---

## ✅ 验收标准

### 编译验证
```bash
mvn compile -pl data-forge-core,data-forge-web -am -DskipTests
```
**期望**: BUILD SUCCESS

### 单元测试验证
```bash
mvn test -Dtest=ConfigLoaderTest
```
**期望**: 所有测试通过

### 集成测试验证
```bash
mvn test -Dtest=DataForgeControllerTest
```
**期望**: 所有测试通过

### 手动验证N2
```bash
java -jar data-forge-cli/target/data-forge-cli-1.0.0-SNAPSHOT.jar \
  --count abc --threads 4
```
**期望**: 输出友好的错误消息，而非NumberFormatException堆栈

### 手动验证N4
```bash
curl -X POST http://localhost:8080/api/v1/dataforge/generate \
  -H "Content-Type: application/json" \
  -d '{"count":1,"fields":[...101个字段...],"output":{...}}'
```
**期望**: HTTP 400 + "Fields list cannot exceed 100 items"

---

## 🔄 回滚方案

如果修复导致问题，使用以下命令回滚：

```bash
# 查看最近提交
git log --oneline -5

# 回滚到修复前的提交
git revert HEAD

# 或者使用reset（仅本地）
git reset --hard <commit-before-fix>
```

---

## 📝 提交信息模板

```bash
git add -A
git commit -m "fix(N2,N4): 修复CLI参数解析和API验证

N2: ConfigLoader参数解析异常处理
- 新增CliArgumentParser工具类
- 添加NumberFormatException捕获
- 提供友好的错误消息
- 添加参数范围验证

N4: GenerateRequest字段列表大小限制
- 添加@Size(max=100)注解
- 更新Swagger文档说明
- 防止DoS攻击

测试验证:
- ConfigLoaderTest: 4个新测试用例
- DataForgeControllerTest: 1个新测试用例
- 编译: ✅ PASSED
- 所有单元测试: ✅ PASSED

参考: CODE_REVIEW_VERIFICATION_REPORT.md
"
```

---

## 📋 执行检查清单

### 修复前检查
- [ ] 确认当前代码编译通过
- [ ] 备份当前分支状态
- [ ] 创建feature分支: `fix/n2-n4-improvements`

### 修复执行
- [ ] 创建 `CliArgumentParser.java`
- [ ] 修改 `ConfigLoader.java`
- [ ] 修改 `GenerateRequest.java`
- [ ] 创建 `ConfigLoaderTest.java`
- [ ] 更新 `DataForgeControllerTest.java`

### 验证检查
- [ ] 编译成功 (`mvn compile`)
- [ ] 单元测试通过 (`mvn test -Dtest=ConfigLoaderTest`)
- [ ] 集成测试通过 (`mvn test -Dtest=DataForgeControllerTest`)
- [ ] 手动测试N2 (CLI错误参数)
- [ ] 手动测试N4 (API大列表)

### 代码质量检查
- [ ] Checkstyle通过
- [ ] PMD检查通过
- [ ] 代码格式化完成
- [ ] JavaDoc注释完整

### 提交前检查
- [ ] 所有修改已暂存
- [ ] 提交信息清晰
- [ ] 分支已推送
- [ ] Pull Request已创建

---

## 🚀 执行时间表

| 阶段 | 预计耗时 | 说明 |
|------|----------|------|
| 修复前准备 | 5分钟 | 编译验证、分支创建 |
| N2修复实施 | 20分钟 | 创建工具类 + 修改ConfigLoader |
| N4修复实施 | 10分钟 | 修改GenerateRequest |
| 测试编写 | 15分钟 | 单元测试 + 集成测试 |
| 验证执行 | 10分钟 | 编译、测试、手动验证 |
| 代码质量检查 | 10分钟 | Checkstyle、PMD、格式化 |
| **总计** | **70分钟** | 约1小时10分钟 |

---

**方案制定人**: Claude Code AI
**方案制定日期**: 2026-02-01
**状态**: ⏳ 待复核确认
