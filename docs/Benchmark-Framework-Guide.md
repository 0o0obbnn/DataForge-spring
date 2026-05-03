# DataForge 性能基准测试框架使用指南

> **版本**: 1.0.0  
> **更新日期**: 2026-01-17

---

## 📋 目录

1. [概述](#概述)
2. [快速开始](#快速开始)
3. [核心组件](#核心组件)
4. [使用示例](#使用示例)
5. [性能回归检测](#性能回归检测)
6. [最佳实践](#最佳实践)
7. [API参考](#api参考)

---

## 概述

DataForge 性能基准测试框架提供了一套完整的工具，用于：

- ✅ **自动化性能测试** - 预热、迭代、统计分析
- ✅ **多维度指标收集** - 时间、内存、吞吐量
- ✅ **统计分析** - 平均值、中位数、P95、P99、标准差
- ✅ **多格式报告** - 控制台、JSON、HTML
- ✅ **性能回归检测** - 自动对比基线，检测性能退化
- ✅ **灵活配置** - 支持快速测试、精确测试等多种模式

---

## 快速开始

### 1. 创建简单的基准测试

```java
import com.dataforge.benchmark.*;

public class MyBenchmarkTest {
    
    @Test
    @Disabled("Benchmark test - run manually")
    void myFirstBenchmark() throws Exception {
        // 创建运行器
        BenchmarkRunner runner = BenchmarkRunner.withQuickConfig();
        
        // 运行测试
        BenchmarkResult result = runner.run(
            "My First Benchmark",
            "Test scenario",
            () -> {
                // 你的测试代码
                for (int i = 0; i < 10000; i++) {
                    // 执行操作
                }
                return null;
            }
        );
        
        // 输出报告
        BenchmarkReporter reporter = new BenchmarkReporter();
        reporter.addResult(result);
        reporter.printConsoleReport();
    }
}
```

### 2. 运行基准测试

```bash
# 运行特定的基准测试
mvn test -Dtest=GeneratorBenchmarkSuite#benchmarkUuidGenerator -pl data-forge-core

# 运行完整的基准测试套件
mvn test -Dtest=GeneratorBenchmarkSuite#runFullBenchmarkSuite -pl data-forge-core
```

---

## 核心组件

### 1. BenchmarkConfig - 测试配置

控制基准测试的执行参数：

```java
BenchmarkConfig config = BenchmarkConfig.builder()
    .warmupIterations(3)           // 预热次数
    .measurementIterations(5)      // 测试迭代次数
    .timeoutMillis(60_000)         // 超时时间
    .collectMemoryStats(true)      // 收集内存统计
    .forceGcBeforeTest(true)       // 测试前执行GC
    .verbose(false)                // 详细日志
    .build();
```

**预定义配置**：

- `BenchmarkConfig.defaultConfig()` - 默认配置（3次预热，5次迭代）
- `BenchmarkConfig.quickConfig()` - 快速测试（1次预热，3次迭代）
- `BenchmarkConfig.preciseConfig()` - 精确测试（5次预热，10次迭代）

### 2. BenchmarkRunner - 测试执行器

负责执行基准测试：

```java
BenchmarkRunner runner = new BenchmarkRunner(config);

BenchmarkResult result = runner.run(
    "Benchmark Name",
    "Scenario Description",
    () -> {
        // 测试代码
        return null;
    }
);
```

### 3. BenchmarkResult - 测试结果

包含所有性能指标：

```java
result.getAverageTime();        // 平均时间
result.getMedianTime();         // 中位数时间
result.getP95Time();            // P95时间
result.getP99Time();            // P99时间
result.getMinTime();            // 最小时间
result.getMaxTime();            // 最大时间
result.getStandardDeviation();  // 标准差
result.getAverageMemory();      // 平均内存使用
```

### 4. BenchmarkReporter - 报告生成器

生成多种格式的报告：

```java
BenchmarkReporter reporter = new BenchmarkReporter();
reporter.addResult(result1);
reporter.addResult(result2);

// 控制台报告
reporter.printConsoleReport();

// JSON报告
reporter.generateJsonReport("target/benchmark-reports/report.json");
```

### 5. BenchmarkRegression - 性能回归检测

检测性能退化：

```java
BenchmarkRegression regression = new BenchmarkRegression(0.10); // 10%阈值

// 保存基线
regression.saveBaseline(results, "baseline.json");

// 检测回归
RegressionReport report = regression.compare(currentResults, "baseline.json");

if (report.hasRegressions()) {
    System.out.println("Performance regressions detected!");
    for (RegressionItem item : report.getRegressions()) {
        System.out.printf("%s: %.2f%% slower%n", 
            item.benchmarkName, item.changePercent);
    }
}
```

---

## 使用示例

### 示例1: 测试单个生成器性能

```java
@Test
@Disabled("Benchmark test - run manually")
void benchmarkUuidGenerator() throws Exception {
    BenchmarkRunner runner = BenchmarkRunner.withQuickConfig();
    int iterations = 100_000;
    
    BenchmarkResult result = runner.run(
        "UUID Generator",
        iterations + " iterations",
        () -> {
            DataGenerator<String, FieldConfig> generator = 
                GeneratorFactory.getGenerator("uuid");
            DataForgeContext context = new DataForgeContext();
            FieldConfig config = new FieldConfig("id", "uuid", new HashMap<>());
            
            for (int i = 0; i < iterations; i++) {
                generator.generate(config, context);
            }
            
            context.close();
            return null;
        }
    );
    
    BenchmarkReporter reporter = new BenchmarkReporter();
    reporter.addResult(result);
    reporter.printConsoleReport();
}
```

### 示例2: 多线程性能对比

```java
@Test
@Disabled("Benchmark test - run manually")
void benchmarkThreadScaling() throws Exception {
    BenchmarkRunner runner = BenchmarkRunner.withQuickConfig();
    BenchmarkReporter reporter = new BenchmarkReporter();
    
    int recordCount = 100_000;
    int[] threadCounts = {1, 2, 4, 8, 16};
    
    for (int threads : threadCounts) {
        BenchmarkResult result = runner.run(
            "Thread Scaling",
            recordCount + " records, " + threads + " threads",
            () -> {
                DataForgeService service = new DataForgeService();
                ForgeConfig config = createConfig(recordCount);
                config.setThreads(threads);
                service.generateData(config);
                return null;
            }
        );
        
        reporter.addResult(result);
    }
    
    reporter.printConsoleReport();
    reporter.generateJsonReport("target/benchmark-reports/thread-scaling.json");
}
```

### 示例3: 输出格式性能对比

```java
@Test
@Disabled("Benchmark test - run manually")
void benchmarkOutputFormats() throws Exception {
    BenchmarkRunner runner = BenchmarkRunner.withQuickConfig();
    BenchmarkReporter reporter = new BenchmarkReporter();
    
    String[] formats = {"CSV", "JSON", "SQL"};
    int recordCount = 50_000;
    
    for (String format : formats) {
        BenchmarkResult result = runner.run(
            "Output Format: " + format,
            recordCount + " records",
            () -> {
                DataForgeService service = new DataForgeService();
                ForgeConfig config = createConfig(recordCount);
                config.getOutput().setFormat(
                    OutputConfig.OutputFormat.valueOf(format)
                );
                service.generateData(config);
                return null;
            }
        );
        
        reporter.addResult(result);
    }
    
    reporter.printConsoleReport();
}
```

---

## 性能回归检测

### 1. 建立性能基线

首次运行基准测试后，保存结果作为基线：

```java
@Test
void establishBaseline() throws Exception {
    BenchmarkRunner runner = BenchmarkRunner.withPreciseConfig();
    BenchmarkReporter reporter = new BenchmarkReporter();
    
    // 运行所有基准测试
    // ... 添加测试结果到reporter
    
    // 保存基线
    BenchmarkRegression regression = new BenchmarkRegression();
    regression.saveBaseline(
        reporter.getResults(),
        "target/benchmark-baseline.json"
    );
}
```

### 2. 检测性能回归

后续运行时，对比当前结果与基线：

```java
@Test
void detectRegression() throws Exception {
    BenchmarkRunner runner = BenchmarkRunner.withPreciseConfig();
    BenchmarkReporter reporter = new BenchmarkReporter();
    
    // 运行所有基准测试
    // ... 添加测试结果到reporter
    
    // 检测回归
    BenchmarkRegression regression = new BenchmarkRegression(0.10); // 10%阈值
    RegressionReport report = regression.compare(
        reporter.getResults(),
        "target/benchmark-baseline.json"
    );
    
    // 输出回归报告
    if (report.hasRegressions()) {
        System.out.println("\n⚠️  Performance Regressions Detected:");
        for (RegressionItem item : report.getRegressions()) {
            System.out.printf("  - %s: %.2f%% slower (%.2fms -> %.2fms)%n",
                item.benchmarkName,
                item.changePercent,
                item.baselineAvgTime,
                item.currentAvgTime);
        }
        
        // 可以选择让测试失败
        // fail("Performance regression detected!");
    }
    
    if (!report.getImprovements().isEmpty()) {
        System.out.println("\n✅ Performance Improvements:");
        for (RegressionItem item : report.getImprovements()) {
            System.out.printf("  - %s: %.2f%% faster%n",
                item.benchmarkName,
                -item.changePercent);
        }
    }
}
```

### 3. CI/CD集成

在CI/CD流程中自动检测性能回归：

```bash
# 运行基准测试并检测回归
mvn test -Dtest=BenchmarkRegressionTest -pl data-forge-core

# 如果检测到回归，构建失败
if [ $? -ne 0 ]; then
    echo "Performance regression detected!"
    exit 1
fi
```

---

## 最佳实践

### 1. 测试隔离

- ✅ 使用 `@Disabled` 标记基准测试，避免常规测试运行
- ✅ 每个测试独立运行，避免相互影响
- ✅ 测试前执行GC，确保内存状态一致

### 2. 预热

- ✅ 至少3次预热迭代，让JIT编译器优化代码
- ✅ 预热迭代数应该与测试迭代数相当

### 3. 迭代次数

- ✅ 快速测试：3-5次迭代
- ✅ 精确测试：10-20次迭代
- ✅ 根据测试时长调整迭代次数

### 4. 环境一致性

- ✅ 在相同的硬件环境运行基准测试
- ✅ 关闭不必要的后台程序
- ✅ 使用相同的JVM参数

### 5. 结果分析

- ✅ 关注中位数和P95，而不仅仅是平均值
- ✅ 检查标准差，确保结果稳定
- ✅ 对比多次运行结果，排除偶然因素

---

## API参考

### BenchmarkConfig

| 方法 | 说明 | 默认值 |
|-----|------|--------|
| `warmupIterations(int)` | 预热迭代次数 | 3 |
| `measurementIterations(int)` | 测试迭代次数 | 5 |
| `timeoutMillis(long)` | 超时时间（毫秒） | 60000 |
| `collectMemoryStats(boolean)` | 收集内存统计 | true |
| `forceGcBeforeTest(boolean)` | 测试前执行GC | true |
| `verbose(boolean)` | 详细日志 | false |

### BenchmarkResult

| 方法 | 说明 |
|-----|------|
| `getAverageTime()` | 平均执行时间（毫秒） |
| `getMedianTime()` | 中位数执行时间（毫秒） |
| `getP95Time()` | P95执行时间（毫秒） |
| `getP99Time()` | P99执行时间（毫秒） |
| `getMinTime()` | 最小执行时间（毫秒） |
| `getMaxTime()` | 最大执行时间（毫秒） |
| `getStandardDeviation()` | 标准差 |
| `getAverageMemory()` | 平均内存使用（字节） |
| `getTotalTestTime()` | 总测试时间（毫秒） |

### BenchmarkReporter

| 方法 | 说明 |
|-----|------|
| `addResult(BenchmarkResult)` | 添加测试结果 |
| `printConsoleReport()` | 输出控制台报告 |
| `generateJsonReport(String)` | 生成JSON报告 |
| `clear()` | 清空所有结果 |
| `getResults()` | 获取所有结果 |

### BenchmarkRegression

| 方法 | 说明 |
|-----|------|
| `saveBaseline(List, String)` | 保存基线 |
| `compare(List, String)` | 对比基线，检测回归 |

---

## 总结

DataForge 性能基准测试框架提供了完整的性能测试解决方案：

- ✅ **自动化** - 预热、迭代、统计分析全自动
- ✅ **准确** - 多次迭代、统计分析、排除干扰
- ✅ **灵活** - 支持多种配置和测试场景
- ✅ **实用** - 性能回归检测、多格式报告
- ✅ **易用** - 简单的API，丰富的示例

**开始使用基准测试框架，确保DataForge的性能始终保持最佳状态！** 🚀

---

**文档版本**: 1.0.0  
**最后更新**: 2026-01-17  
**作者**: DataForge Team

