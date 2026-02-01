# Phase 2 重构组件使用示例

本文档展示如何使用 Phase 2 重构后的组件。

---

## 一、DataLoadingService 使用示例

### 1.1 基本使用

```java
import com.dataforge.service.DataLoadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoaderExample {

    @Autowired
    private DataLoadingService dataLoadingService;

    public void loadDataExample() {
        // 加载文本文件
        List<String> lines = dataLoadingService.loadLines("data/my-data.txt");
        lines.forEach(System.out::println);

        // 加载并解析数据
        List<Integer> numbers = dataLoadingService.loadData(
            "data/numbers.txt",
            Integer::parseInt
        );

        // 加载带 fallback
        List<String> data = dataLoadingService.loadLinesWithFallback(
            "data/optional.txt",
            () -> List.of("default1", "default2")
        );
    }
}
```

### 1.2 自定义解析器

```java
public class CustomParserExample {

    @Autowired
    private DataLoadingService dataLoadingService;

    public void customParsing() {
        // 解析 CSV 格式数据
        List<String[]> csvData = dataLoadingService.loadData(
            "data/data.csv",
            line -> line.split(",")
        );

        // 解析 JSON 格式数据
        List<MyObject> objects = dataLoadingService.loadData(
            "data/objects.json",
            line -> parseJson(line)
        );
    }

    private MyObject parseJson(String json) {
        // JSON 解析逻辑
        return new MyObject();
    }
}
```

---

## 二、BaseDataLoadingGenerator 使用示例

### 2.1 创建自定义生成器

```java
import com.dataforge.generators.internal.BaseDataLoadingGenerator;
import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomDataGenerator extends BaseDataLoadingGenerator<String>
        implements DataGenerator<String, FieldConfig> {

    private List<String> dataItems;

    @Override
    public String getType() {
        return "custom";
    }

    @Override
    public String generate(FieldConfig config, DataForgeContext context) {
        // 使用基类的延迟加载机制
        ensureDataLoaded();

        // 生成逻辑
        if (dataItems.isEmpty()) {
            return "default";
        }

        int index = ThreadLocalRandom.current().nextInt(dataItems.size());
        return dataItems.get(index);
    }

    @Override
    public Class<FieldConfig> getConfigClass() {
        return FieldConfig.class;
    }

    // ==================== 基类抽象方法实现 ====================

    @Override
    protected String getDataFilePath() {
        return "data/custom-data.txt";
    }

    @Override
    protected void parseData(List<String> lines) {
        dataItems = lines.stream()
            .filter(line -> !line.trim().isEmpty())
            .collect(Collectors.toList());
    }

    @Override
    protected void initializeFallbackData() {
        dataItems = List.of("fallback1", "fallback2", "fallback3");
    }
}
```

### 2.2 使用自定义解析器

```java
@Component
public class AdvancedDataGenerator extends BaseDataLoadingGenerator<MyData> {

    private Map<String, MyData> dataMap;

    @Override
    protected String getDataFilePath() {
        return "data/advanced-data.csv";
    }

    @Override
    protected void parseData(List<String> lines) {
        dataMap = new HashMap<>();

        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                MyData data = new MyData(parts[0], parts[1]);
                dataMap.put(parts[0], data);
            }
        }
    }

    @Override
    protected void initializeFallbackData() {
        dataMap = new HashMap<>();
        dataMap.put("key1", new MyData("key1", "value1"));
        dataMap.put("key2", new MyData("key2", "value2"));
    }

    // 使用自定义解析器加载数据
    public List<MyData> loadWithCustomParser() {
        return loadDataWithParser(line -> {
            String[] parts = line.split(":");
            return new MyData(parts[0], parts[1]);
        });
    }
}
```

---

## 三、IdCardRegionService 使用示例

### 3.1 基本使用

```java
import com.dataforge.generators.internal.idcard.IdCardRegionService;
import com.dataforge.generators.internal.idcard.IdCardRegionService.RegionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegionServiceExample {

    @Autowired
    private IdCardRegionService regionService;

    public void regionExamples() {
        // 获取随机地区代码
        String randomRegion = regionService.getRandomRegionCode();
        System.out.println("随机地区: " + randomRegion);

        // 根据地区名称选择代码
        String beijingCode = regionService.selectRegionCode("北京市");
        String shanghaiCode = regionService.selectRegionCode("上海市");

        // 获取地区详细信息
        RegionInfo info = regionService.getRegionInfo("110101");
        if (info != null) {
            System.out.println("省份: " + info.province);
            System.out.println("城市: " + info.city);
            System.out.println("区县: " + info.district);
        }

        // 获取所有地区代码
        List<String> allRegions = regionService.getAllRegionCodes();
        System.out.println("总地区数: " + allRegions.size());

        // 检查地区是否存在
        boolean exists = regionService.containsRegion("110101");
    }
}
```

### 3.2 在生成器中使用

```java
@Component
public class CustomIdCardGenerator extends BaseGenerator
        implements DataGenerator<String, FieldConfig> {

    @Autowired
    private IdCardRegionService regionService;

    @Override
    public String generate(FieldConfig config, DataForgeContext context) {
        // 根据配置选择地区
        String region = getStringParam(config, "region", null);
        String regionCode = regionService.selectRegionCode(region);

        // 获取地区信息放入上下文
        RegionInfo info = regionService.getRegionInfo(regionCode);
        if (info != null) {
            context.put("province", info.province);
            context.put("city", info.city);
        }

        // 生成身份证号码...
        return generateIdCard(regionCode);
    }

    private String generateIdCard(String regionCode) {
        // 生成逻辑
        return regionCode + "19900101123X";
    }
}
```

---

## 四、IdCardValidationHelper 使用示例

### 4.1 基本校验

```java
import com.dataforge.generators.internal.idcard.IdCardValidationHelper;

public class ValidationExample {

    public void validationExamples() {
        // 验证身份证号码
        String idCard = "110101199001011237";
        boolean isValid = IdCardValidationHelper.isValidIdCard(idCard);
        System.out.println("是否有效: " + isValid);

        // 计算校验码
        String prefix = "11010119900101123";
        String checkDigit = IdCardValidationHelper.calculateCheckDigit(prefix);
        System.out.println("校验码: " + checkDigit);

        // 提取信息
        String birthDate = IdCardValidationHelper.extractBirthDate(idCard);
        String gender = IdCardValidationHelper.extractGender(idCard);
        String regionCode = IdCardValidationHelper.extractRegionCode(idCard);

        System.out.println("出生日期: " + birthDate);
        System.out.println("性别: " + gender);
        System.out.println("地区代码: " + regionCode);

        // 计算年龄
        int age = IdCardValidationHelper.calculateAge(birthDate);
        System.out.println("年龄: " + age);

        // 掩码处理
        String masked = IdCardValidationHelper.maskIdCard(idCard);
        System.out.println("掩码后: " + masked); // 110101********1237
    }
}
```

### 4.2 生成身份证号码

```java
@Component
public class IdCardGenerationExample {

    public String generateIdCardExample() {
        // 地区代码
        String regionCode = "110101";

        // 生成出生日期
        String birthDate = IdCardValidationHelper.generateBirthDate("1980-01-01,2000-12-31");

        // 生成顺序码（男性）
        String sequenceCode = IdCardValidationHelper.generateSequenceCode("MALE");

        // 计算校验码
        String prefix = regionCode + birthDate + sequenceCode;
        String checkDigit = IdCardValidationHelper.calculateCheckDigit(prefix);

        // 完整身份证号码
        return prefix + checkDigit;
    }

    public void generateBatchExample(int count) {
        for (int i = 0; i < count; i++) {
            String idCard = generateIdCardExample();
            System.out.println("Generated: " + idCard);

            // 验证生成的号码
            boolean valid = IdCardValidationHelper.isValidIdCard(idCard);
            assert valid : "Generated invalid ID card!";
        }
    }
}
```

---

## 五、完整示例：自定义生成器

```java
package com.dataforge.examples;

import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.internal.BaseDataLoadingGenerator;
import com.dataforge.generators.internal.idcard.IdCardRegionService;
import com.dataforge.generators.internal.idcard.IdCardValidationHelper;
import com.dataforge.generators.spi.DataGenerator;
import com.dataforge.model.FieldConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 完整的自定义生成器示例。
 */
@Component
public class CompleteExampleGenerator extends BaseDataLoadingGenerator<String>
        implements DataGenerator<String, FieldConfig> {

    @Autowired
    private IdCardRegionService regionService;

    private Map<String, String> nameMapping;

    @Override
    public String getType() {
        return "complete_example";
    }

    @Override
    public String generate(FieldConfig config, DataForgeContext context) {
        // 1. 使用基类加载数据
        ensureDataLoaded();

        // 2. 使用 RegionService 获取地区
        String region = getStringParam(config, "region", null);
        String regionCode = regionService.selectRegionCode(region);

        // 3. 使用 ValidationHelper 生成日期
        String birthDate = IdCardValidationHelper.generateBirthDate("1990-01-01,2010-12-31");

        // 4. 生成自定义数据
        String customData = generateCustomData(config);

        // 5. 构建结果
        String result = String.format("%s-%s-%s", regionCode, birthDate, customData);

        // 6. 放入上下文
        context.put("result", result);
        context.put("regionCode", regionCode);

        return result;
    }

    @Override
    public Class<FieldConfig> getConfigClass() {
        return FieldConfig.class;
    }

    @Override
    protected String getDataFilePath() {
        return "data/name-mapping.txt";
    }

    @Override
    protected void parseData(List<String> lines) {
        nameMapping = lines.stream()
            .filter(line -> line.contains("="))
            .map(line -> line.split("=", 2))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> parts[0].trim(),
                parts -> parts[1].trim()
            ));
    }

    @Override
    protected void initializeFallbackData() {
        nameMapping = Map.of(
            "A", "Alpha",
            "B", "Beta",
            "C", "Gamma"
        );
    }

    private String generateCustomData(FieldConfig config) {
        if (nameMapping.isEmpty()) {
            return "DEFAULT";
        }

        List<String> keys = List.copyOf(nameMapping.keySet());
        String randomKey = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        return nameMapping.get(randomKey);
    }
}
```

---

## 六、测试示例

```java
import com.dataforge.generators.internal.idcard.IdCardValidationHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExampleTest {

    @Test
    void testIdCardGeneration() {
        // 生成身份证号码
        String regionCode = "110101";
        String birthDate = IdCardValidationHelper.generateBirthDate("1990-01-01,2000-12-31");
        String sequenceCode = IdCardValidationHelper.generateSequenceCode("ANY");

        String prefix = regionCode + birthDate + sequenceCode;
        String checkDigit = IdCardValidationHelper.calculateCheckDigit(prefix);
        String idCard = prefix + checkDigit;

        // 验证
        assertEquals(18, idCard.length());
        assertTrue(IdCardValidationHelper.isValidIdCard(idCard));

        // 验证提取的信息
        assertEquals(regionCode, IdCardValidationHelper.extractRegionCode(idCard));
        assertEquals(birthDate, IdCardValidationHelper.extractBirthDate(idCard));
    }

    @Test
    void testRegionService() {
        // 测试地区服务
        IdCardRegionService service = new IdCardRegionService();

        String regionCode = service.getRandomRegionCode();
        assertNotNull(regionCode);
        assertEquals(6, regionCode.length());

        IdCardRegionService.RegionInfo info = service.getRegionInfo(regionCode);
        assertNotNull(info);
    }
}
```

---

## 七、最佳实践

### 7.1 数据加载

1. **使用延迟加载**: 通过 `ensureDataLoaded()` 实现线程安全的延迟加载
2. **提供 Fallback**: 始终提供 fallback 数据，确保服务可用性
3. **合理缓存**: 数据加载后缓存，避免重复加载

### 7.2 代码组织

1. **职责分离**: 将数据加载、业务逻辑、校验逻辑分离到不同类
2. **使用服务**: 通过 Spring 依赖注入使用服务
3. **保持简单**: 每个类只负责一个明确的职责

### 7.3 测试

1. **单元测试**: 为每个组件编写单元测试
2. **集成测试**: 测试组件之间的协作
3. **边界测试**: 测试异常情况和边界条件

---

**文档版本**: 1.0.0  
**更新日期**: 2026-01-30  
**作者**: DataForge Team
