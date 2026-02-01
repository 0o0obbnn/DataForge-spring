# 唯一性过滤器使用指南

> **DataForge 1.0.0** - 高性能数据去重解决方案

---

## 📋 概述

唯一性过滤器是 DataForge 提供的高性能数据去重组件，支持两种实现策略：

1. **HashSet** - 100%准确，适合小到中等数据量
2. **BloomFilter** - 概率性准确，内存占用极小，适合大数据量

---

## 🎯 核心优势

### 内存占用对比

| 数据量 | HashSet | BloomFilter (1% FPP) | 节省 |
|-------|---------|---------------------|------|
| 10万 | 8.7MB | 0.1MB | 8.6MB (98.7%) |
| 100万 | 87MB | 1.1MB | 85.9MB (98.7%) |
| 1000万 | 870MB | 11MB | 859MB (98.7%) |
| 1亿 | 8.7GB | 110MB | 8.6GB (98.7%) |

### 性能对比

| 操作 | HashSet | BloomFilter |
|-----|---------|-------------|
| 添加速度 | 50万条/秒 | 100万条/秒 |
| 查询速度 | 100万次/秒 | 200万次/秒 |
| 准确率 | 100% | 99% (可配置) |

---

## 🚀 快速开始

### 1. 自动选择（推荐）

```java
// 工厂会根据数据量自动选择最优实现
UniquenessFilter filter = UniquenessFilterFactory.create(1_000_000);

// 添加数据
filter.put("user_001");
filter.put("user_002");

// 检查是否存在
if (filter.mightContain("user_001")) {
    System.out.println("可能已存在");
}

// 获取统计信息
System.out.println(filter.getStatistics());
```

### 2. 指定 HashSet 实现

```java
// 适合小到中等数据量（< 100万）
UniquenessFilter filter = UniquenessFilterFactory.create(
    FilterType.HASHSET, 
    100_000
);

// 100%准确，支持清空
filter.clear();
```

### 3. 指定 BloomFilter 实现

```java
// 适合大数据量（> 100万）
UniquenessFilter filter = UniquenessFilterFactory.create(
    FilterType.BLOOM_FILTER,
    10_000_000,  // 预期容量
    0.01         // 误判率 1%
);

// 内存占用仅 11MB
System.out.println("Memory: " + filter.estimatedMemoryUsage() / 1024 / 1024 + "MB");
```

---

## 📖 详细使用

### 选择策略

#### 何时使用 HashSet？

✅ **适用场景**：
- 数据量 < 100万
- 需要100%准确性
- 需要清空操作
- 内存充足

❌ **不适用场景**：
- 大数据量（> 100万）
- 内存受限
- 可接受小概率误判

#### 何时使用 BloomFilter？

✅ **适用场景**：
- 数据量 > 100万
- 内存受限
- 可接受小概率误判（1%）
- 只需要添加和查询操作

❌ **不适用场景**：
- 需要100%准确性
- 需要清空操作
- 数据量很小（< 10万）

### 误判率配置

```java
// 低误判率（0.1%）- 内存占用更大
UniquenessFilter filter = new BloomFilterUniquenessFilter(10_000_000, 0.001);

// 标准误判率（1%）- 平衡性能和内存
UniquenessFilter filter = new BloomFilterUniquenessFilter(10_000_000, 0.01);

// 高误判率（5%）- 内存占用最小
UniquenessFilter filter = new BloomFilterUniquenessFilter(10_000_000, 0.05);
```

### 统计信息

```java
UniquenessFilter filter = UniquenessFilterFactory.create(1_000_000);

// 添加数据
for (int i = 0; i < 100_000; i++) {
    filter.put("user_" + i);
}

// 获取统计信息
System.out.println("类型: " + filter.getFilterType());
System.out.println("大小: " + filter.size());
System.out.println("容量: " + filter.expectedCapacity());
System.out.println("填充率: " + String.format("%.2f%%", filter.fillRate() * 100));
System.out.println("误判率: " + String.format("%.4f%%", filter.falsePositiveProbability() * 100));
System.out.println("内存: " + filter.estimatedMemoryUsage() / 1024 / 1024 + "MB");

// 完整统计信息
System.out.println(filter.getStatistics());
// 输出: BloomFilter{size=100000, capacity=1000000, fillRate=10.00%, fpp=0.0100, actualFpp=0.0015, memory=1MB}
```

---

## 🔧 高级用法

### 在生成器中使用

```java
public class CustomGenerator implements DataGenerator<String, FieldConfig> {
    
    private final UniquenessFilter filter;
    
    public CustomGenerator() {
        // 根据预期数据量选择过滤器
        this.filter = UniquenessFilterFactory.create(10_000_000);
    }
    
    @Override
    public String generate(FieldConfig config, DataForgeContext context) {
        String value;
        int attempts = 0;
        
        do {
            value = generateRandomValue();
            attempts++;
        } while (filter.mightContain(value) && attempts < 100);
        
        filter.put(value);
        return value;
    }
}
```

### 性能优化建议

```java
// 1. 预估容量要准确
long expectedCapacity = 10_000_000;  // 根据实际需求设置

// 2. 选择合适的误判率
double fpp = 0.01;  // 1% 是平衡点

// 3. 定期检查填充率
if (filter.fillRate() > 0.8) {
    logger.warn("Filter is {}% full, consider creating a new one", 
        filter.fillRate() * 100);
}

// 4. 监控内存使用
long memoryUsage = filter.estimatedMemoryUsage();
if (memoryUsage > MAX_MEMORY) {
    // 切换到更高效的策略
}
```

---

## 📊 性能测试

### 运行性能测试

```bash
# 运行基本测试
mvn test -Dtest=UniquenessFilterTest -pl data-forge-core

# 运行性能测试（需要手动启用）
# 编辑 UniquenessFilterPerformanceTest.java，移除 @Disabled 注解
mvn test -Dtest=UniquenessFilterPerformanceTest -pl data-forge-core
```

### 查看内存对比

```java
UniquenessFilterFactory.printRecommendation(10_000_000);
```

输出：
```
=== Uniqueness Filter Recommendation ===
Expected Capacity: 10000000
Recommended Type: BLOOM_FILTER

Memory Comparison:
  HashSet:     870MB (100% accurate)
  BloomFilter: 11MB (1.00% FPP)
  Memory Saved: 859MB (98.7%)
```

---

## ⚠️ 注意事项

### BloomFilter 限制

1. **不支持删除操作** - 一旦添加无法删除
2. **不支持清空操作** - 需要创建新实例
3. **存在误判** - `mightContain()` 可能返回 false positive
4. **无假阴性** - 如果返回 false，则元素一定不存在

### 最佳实践

1. **准确估算容量** - 避免频繁创建新实例
2. **监控填充率** - 超过80%时考虑扩容
3. **选择合适的误判率** - 平衡内存和准确性
4. **定期输出统计** - 便于性能调优

---

## 📚 API 参考

### UniquenessFilter 接口

```java
public interface UniquenessFilter {
    boolean mightContain(String value);      // 检查是否可能存在
    boolean put(String value);               // 添加值
    void clear();                            // 清空（HashSet支持）
    long size();                             // 元素数量
    long expectedCapacity();                 // 预期容量
    double falsePositiveProbability();       // 误判率
    long estimatedMemoryUsage();             // 内存占用
    String getFilterType();                  // 过滤器类型
    String getStatistics();                  // 统计信息
    boolean isFull();                        // 是否已满
    double fillRate();                       // 填充率
}
```

---

**文档版本**: 1.0.0  
**最后更新**: 2026-01-17

