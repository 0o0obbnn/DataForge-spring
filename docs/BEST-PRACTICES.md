# DataForge 最佳实践

## 配置最佳实践

### 1. 环境配置外部化

**❌ 错误做法**:
```yaml
jwt:
  secret: hardcoded-secret-key-12345
```

**✅ 正确做法**:
```yaml
jwt:
  secret: ${JWT_SECRET:default-secret-for-dev-only}
```

使用环境变量管理敏感配置，生产环境必须设置。

### 2. 资源限制配置

根据实际需求配置合理的资源限制：

```yaml
dataforge:
  security:
    max-record-count: 1000000      # 单次生成最大记录数
    max-thread-count: 32           # 最大并发线程数
    max-field-count: 500           # 最大字段数
    max-config-size-mb: 50         # 最大配置大小
```

### 3. 缓存配置

合理配置缓存大小和过期时间：

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterAccess=300s
```

## 代码最佳实践

### 1. 使用模板而非重复配置

**❌ 错误做法**:
每次生成都传递完整配置

**✅ 正确做法**:
```java
// 创建模板
DataTemplate template = new DataTemplate();
template.setName("user-data");
template.setConfig(configJson);
templateService.createTemplate(template);

// 使用模板
generateDataByTemplateIdAsync(templateId, recordCount);
```

### 2. 大批量数据使用异步接口

**❌ 错误做法**:
```java
// 同步生成100万条数据
generateData(request); // 可能超时
```

**✅ 正确做法**:
```java
// 异步生成
Long taskId = generateDataAsync(request);
// 轮询任务状态
while (true) {
    TaskStatus status = getTaskStatus(taskId);
    if (status.isCompleted()) break;
    Thread.sleep(1000);
}
```

### 3. 合理设置并发线程数

根据 CPU 核心数和 I/O 特性设置：

```java
// CPU 密集型：线程数 = CPU核心数
// I/O 密集型：线程数 = CPU核心数 * 2
int threads = Runtime.getRuntime().availableProcessors();
request.setThreads(threads);
```

### 4. 使用种子实现可重现生成

```java
GenerateRequest request = new GenerateRequest();
request.setSeed(12345L); // 设置种子
// 相同种子会生成相同的数据序列
```

## 性能优化实践

### 1. 字段配置优化

**避免不必要的验证**:
```java
// 如果不需要验证，关闭验证以提高性能
request.setValidate(false);
```

**合理使用缓存**:
```java
// 对于重复生成相同类型的数据，生成器会被缓存
// 无需额外配置
```

### 2. 输出格式选择

- **CSV**: 适合大数据量，文件小
- **JSON**: 适合结构化数据，便于解析
- **SQL**: 适合直接导入数据库
- **Console**: 仅用于调试

### 3. 分批处理

对于超大数据量，建议分批生成：

```java
int totalCount = 1000000;
int batchSize = 100000;

for (int i = 0; i < totalCount; i += batchSize) {
    int currentBatch = Math.min(batchSize, totalCount - i);
    generateDataBatch(currentBatch, i);
}
```

## 安全最佳实践

### 1. 认证令牌管理

**❌ 错误做法**:
```java
String token = "hardcoded-token"; // 不要硬编码
```

**✅ 正确做法**:
```java
// 从环境变量或配置中心获取
String token = System.getenv("API_TOKEN");
// 或使用配置类
@Value("${api.token}")
private String token;
```

### 2. 输入验证

始终验证用户输入：

```java
@Valid @RequestBody GenerateRequest request
// Spring 会自动验证 @Valid 注解的字段
```

### 3. 错误处理

不要暴露敏感信息：

**❌ 错误做法**:
```java
catch (Exception e) {
    return ResponseEntity.ok(e.getMessage()); // 可能包含敏感信息
}
```

**✅ 正确做法**:
```java
catch (Exception e) {
    log.error("Generation failed", e);
    return ResponseEntity.ok("Generation failed. Please contact support.");
}
```

### 4. API 限流

为关键接口添加限流：

```java
@RateLimit(value = 10, seconds = 60) // 每分钟10次
public ResponseEntity<?> generateData(...) {
    // ...
}
```

## 测试最佳实践

### 1. 单元测试

为每个生成器编写单元测试：

```java
@Test
void testUuidGenerator() {
    UuidGenerator generator = new UuidGenerator();
    String uuid = generator.generate(context, config);
    assertNotNull(uuid);
    assertTrue(uuid.matches(UUID_PATTERN));
}
```

### 2. 集成测试

测试完整的数据生成流程：

```java
@Test
void testEndToEndGeneration() {
    ForgeConfig config = createTestConfig();
    dataForgeService.generateData(config);
    // 验证输出文件
}
```

### 3. 性能测试

定期运行性能基准测试：

```java
@Benchmark
public void benchmarkGeneration() {
    // 测试生成性能
}
```

## 监控最佳实践

### 1. 关键指标监控

监控以下关键指标：

- **生成成功率**: `dataforge.generation.success / dataforge.generation.count`
- **平均生成时间**: `dataforge.generation.duration`
- **错误率**: `dataforge.generation.failure / dataforge.generation.count`
- **系统资源**: CPU、内存、线程数

### 2. 告警配置

设置合理的告警阈值：

- 错误率 > 5%
- 平均响应时间 > 1秒
- 内存使用 > 80%
- 生成器数量 = 0

### 3. 日志级别

生产环境使用 INFO 级别：

```yaml
logging:
  level:
    root: INFO
    com.dataforge: INFO
```

开发环境可以使用 DEBUG：

```yaml
logging:
  level:
    com.dataforge: DEBUG
```

## 部署最佳实践

### 1. 容器化

使用多阶段构建优化镜像大小：

```dockerfile
FROM maven:3.8-openjdk-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-jre-slim
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. 健康检查

配置健康检查端点：

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

### 3. 资源限制

设置合理的资源限制：

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "2000m"
```

## 故障排查

### 1. 查看日志

使用请求ID追踪日志：

```bash
grep "requestId=abc-123" application.log
```

### 2. 检查健康状态

```bash
curl http://localhost:8080/api/v1/health
```

### 3. 查看指标

```bash
curl http://localhost:8080/actuator/metrics/dataforge.generation.count
```

## 常见问题

### Q: 生成速度慢怎么办？

A: 
1. 增加线程数（但不要超过 CPU 核心数太多）
2. 关闭数据验证（如果不需要）
3. 使用异步接口
4. 检查网络和磁盘 I/O

### Q: 内存溢出怎么办？

A:
1. 减少单次生成的记录数
2. 使用分批处理
3. 使用流式输出
4. 增加 JVM 堆内存

### Q: 如何提高生成器性能？

A:
1. 使用缓存（自动启用）
2. 避免在生成器中执行耗时操作
3. 使用线程安全的实现
4. 考虑使用预生成数据池

## 总结

遵循这些最佳实践可以：

- ✅ 提高系统性能和稳定性
- ✅ 增强安全性
- ✅ 简化维护和故障排查
- ✅ 改善用户体验
- ✅ 降低运营成本

更多信息请参考：
- [API 使用指南](./API-USAGE-GUIDE.md)
- [架构文档](./ARCHITECTURE.md)
- [生成器文档](./DataGen-Usage-Guide.md)
