# DataForge 测试环境配置指南

## 概述

本文档描述了运行 DataForge 项目测试所需的环境配置。

## 模块测试环境需求

### 1. data-forge-api 模块

**测试类型**: 纯单元测试  
**环境需求**: 无特殊需求  
**运行方式**:
```bash
mvn test -pl data-forge-api
```

### 2. data-forge-core 模块

**测试类型**: 单元测试  
**环境需求**:
- Java 21+
- 无外部服务依赖

**运行方式**:
```bash
mvn test -pl data-forge-core
```

**生成器测试说明**:
- 生成器测试使用 `TestFieldConfig` 和内存中的 `DataForgeContext`
- 无需数据库或外部服务
- 测试覆盖主要生成器：ColorGenerator、CookieGenerator、CurrencyGenerator、DecimalGenerator 等

### 3. data-forge-cli 模块

**测试类型**: 单元测试  
**环境需求**:
- Java 21+
- 无外部服务依赖

**运行方式**:
```bash
mvn test -pl data-forge-cli
```

### 4. data-forge-web 模块

**测试类型**: 集成测试（需要 Spring Boot 上下文）  
**环境需求**:
- Java 21+
- H2 内存数据库（已配置，无需额外安装）
- Redis（可选，测试配置已处理）

**配置文件**: `data-forge-web/src/test/resources/application.yml`

**关键配置说明**:

```yaml
spring:
  # 数据库配置 - 使用 H2 内存数据库
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
    driver-class-name: org.h2.Driver

  # JPA 配置 - 自动创建和删除表结构
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true

  # 禁用 Flyway 迁移
  flyway:
    enabled: false

  # Redis 配置 - 测试中使用简单缓存
  data:
    redis:
      host: localhost
      port: 6379

  # 使用简单缓存替代 Redis
  cache:
    type: simple

# JWT 配置（对应 JwtProperties 配置类，prefix = "app.jwt"）
app:
  jwt:
    secret: test-secret-key-for-testing-only-not-for-production
    expiration: 3600000
    refresh-expiration: 604800000

# DataForge 配置
dataforge:
  cache:
    enabled: false
  performance:
    enabled: true
    queue-capacity: 100
    thread-pool-size: 2
  security:
    max-record-count: 10000
    max-thread-count: 4
    max-field-count: 100
    max-config-size-mb: 10
    enable-resource-monitoring: false
```

**可选依赖处理**:

1. **Redis**: 
   - `RedisConfig` 类添加了 `@ConditionalOnProperty` 注解
   - `MultiLevelCacheManager` 中的 `RedisTemplate` 设为可选依赖
   - 测试时 Redis 功能自动降级为本地缓存

2. **Metrics**:
   - `MetricsConfig` 类添加了 `@ConditionalOnBean(MeterRegistry.class)`
   - 测试环境中无 Micrometer 指标收集

**运行方式**:
```bash
mvn test -pl data-forge-web
```

## 完整测试运行

### 编译测试（推荐）
```bash
mvn clean compile -Ddependency-check.skip=true
```

### 运行所有测试
```bash
mvn clean test -Ddependency-check.skip=true
```

### 跳过测试编译
```bash
mvn clean compile -DskipTests -Ddependency-check.skip=true
```

## 常见问题

### 1. Redis 连接失败

**现象**: `No qualifying bean of type 'RedisConnectionFactory'`

**解决方案**:
- 已在 `MultiLevelCacheManager` 中将 `RedisTemplate` 设为可选依赖
- 已在 `RedisConfig` 中添加条件注解
- 测试配置使用 `cache.type=simple`

### 2. 数据库表不存在

**现象**: `Schema-validation: missing table [xxx]`

**解决方案**:
- 确保测试配置中 `jpa.hibernate.ddl-auto=create-drop`
- 确保 `flyway.enabled=false`

### 3. Metrics 依赖缺失

**现象**: `No qualifying bean of type 'MeterRegistry'`

**解决方案**:
- 已在 `MetricsConfig` 中添加 `@ConditionalOnBean(MeterRegistry.class)`

### 4. YAML 格式错误

**现象**: YAML 文件解析错误

**解决方案**:
- 确保键名使用下划线（如 `format_sql`）而不是中括号
- 确保正确的缩进（2个空格）

## 测试数据

### 测试用生成器

以下生成器测试文件已创建：

| 生成器 | 测试文件 | 测试覆盖 |
|--------|----------|----------|
| ColorGenerator | ColorGeneratorTest.java | HEX、RGB、Alpha、Name格式 |
| CookieGenerator | CookieGeneratorTest.java | 默认、命名、HttpOnly、Secure |
| CurrencyGenerator | CurrencyGeneratorTest.java | 默认、人民币、美元、格式化 |
| DecimalGenerator | DecimalGeneratorTest.java | 默认、精度、范围、正数 |
| IdCardValidationHelper | IdCardValidationHelperTest.java | 校验码、验证、提取信息 |

### 添加新测试

参考现有测试文件结构：

```java
package com.dataforge.generators.internal;

import static org.assertj.core.api.Assertions.assertThat;
import com.dataforge.core.DataForgeContext;
import com.dataforge.generators.TestFieldConfig;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("XxxGenerator - 描述")
class XxxGeneratorTest {

    private XxxGenerator generator;
    private DataForgeContext context;

    @BeforeEach
    void setUp() {
        generator = new XxxGenerator();
        context = new DataForgeContext();
    }

    @Test
    @DisplayName("测试场景描述")
    void shouldDoSomething() {
        TestFieldConfig config = new TestFieldConfig("field", "type", new HashMap<>());
        config.set("param", "value");

        String result = generator.generate(config, context);

        assertThat(result).isNotNull();
        // 更多断言...
    }
}
```

## 持续集成建议

### GitHub Actions 示例

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          
      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          
      - name: Run tests
        run: mvn clean test -Ddependency-check.skip=true
```

## 总结

- **data-forge-api/core/cli**: 纯单元测试，无外部依赖
- **data-forge-web**: 集成测试，需要 H2 数据库，Redis 可选
- 所有外部依赖已通过条件注解和可选依赖处理
- 测试可在无外部服务的情况下运行
- YAML 配置使用下划线命名（如 `format_sql`）
