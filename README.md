# DataForge

高性能、灵活且高度可配置的测试数据生成工具。

## 特性

- 🚀 **高性能**: 支持每秒生成数万条记录，支持并发生成
- 🎯 **60+ 生成器**: 内置 60+ 种数据生成器，覆盖常见数据类型
- 🔌 **可扩展**: 基于 SPI 机制，轻松添加自定义生成器
- 📊 **多格式输出**: 支持 CSV、JSON、SQL、Console 等多种输出格式
- 🔒 **安全可靠**: 完善的输入验证、资源限制、API 限流
- 📈 **可观测性**: 集成 Micrometer、Prometheus，提供完整的监控指标
- 📚 **完整文档**: 提供 API 文档、架构文档、最佳实践指南

## 快速开始

### 前置要求

- Java 21+
- Maven 3.6+
- Redis (可选，用于分布式缓存和限流)

### 构建项目

```bash
# 完整构建（包含测试）
./build.sh

# 快速构建（Windows，跳过测试）
./build-quick.bat

# 仅运行测试
mvn test
```

### 启动 Web 服务

```bash
# Linux/macOS
./run-web.sh

# Windows
./run-web.bat

# 或手动启动
cd data-forge-web && mvn spring-boot:run
```

访问地址: http://localhost:8080

### 使用 CLI

```bash
# 生成数据
java -jar data-forge-cli/target/data-forge-cli-1.0.0-SNAPSHOT.jar \
  --count 10 \
  --format csv \
  --output data.csv \
  --fields "id:uuid,name:name,email:email"

# 使用配置文件
java -jar data-forge-cli/target/data-forge-cli-1.0.0-SNAPSHOT.jar \
  --config examples/basic-config.yml
```

## 项目结构

```
data-forge/
├── data-forge-core/      # 核心模块（生成引擎 + 60+ 生成器）
├── data-forge-cli/       # 命令行接口
├── data-forge-web/       # Web API 服务
├── data-forge-frontend/  # 前端控制台（React/Vite，开发中）
├── docs/                 # 文档
└── examples/             # 配置示例
```

## 前端控制台（React）

当前前端控制台实现位于主仓目录 `data-forge-frontend/`。

### 环境变量

参考示例文件：`data-forge-frontend/.env.example`

- `VITE_API_BASE_URL`: 后端 API 基地址（例如 `http://localhost:8080`；也可留空使用 Vite proxy `/api`）
- `VITE_ENABLE_MOCK_AUTH`: 是否启用 DEV MOCK 登录（`true/false`）

### 启动与验证

```bash
cd data-forge-frontend
npm install
npm run dev

# 验证
npm run lint
npm run test
npm run build
npm run e2e
```

## API 文档

启动服务后访问：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI 规范**: http://localhost:8080/v3/api-docs

详细 API 使用指南请参考 [API 使用指南](docs/API-USAGE-GUIDE.md)

## 文档

- [API 使用指南](docs/API-USAGE-GUIDE.md) - 完整的 API 使用说明和示例
- [架构文档](docs/ARCHITECTURE.md) - 系统架构和设计模式
- [最佳实践](docs/BEST-PRACTICES.md) - 使用最佳实践和故障排查
- [生成器使用指南](docs/DataGen-Usage-Guide.md) - 所有生成器的详细说明

## 支持的数据类型

### 基础类型
- UUID、字符串、整数、小数、布尔值
- 日期、时间、时间戳

### 个人信息
- 姓名（中英文）、邮箱、电话
- 身份证号、护照号

### 地址信息
- 地址、城市、省份、邮编

### 金融信息
- 银行卡号、信用卡号、账户号

### 网络信息
- IP 地址、MAC 地址、URL、域名

### 其他
- 车牌号、公司名、职位、车牌号

完整列表请参考 [生成器文档](docs/DataGen-Usage-Guide.md)

## 配置示例

### YAML 配置

```yaml
dataforge:
  count: 1000
  threads: 4
  validate: true
  executionMode: VIRTUAL  # 可选: PLATFORM（平台线程）或 VIRTUAL（虚拟线程），默认 PLATFORM
  
  output:
    format: csv
    file: "data.csv"
    encoding: "UTF-8"
    
  fields:
    - name: "id"
      type: "uuid"
    - name: "name"
      type: "name"
      params:
        type: "CN"
        gender: "ANY"
    - name: "email"
      type: "email"
    - name: "phone"
      type: "phone"
```

### 执行模式说明

DataForge 支持两种线程执行模式：

- **PLATFORM（默认）**: 使用传统平台线程池，兼容性最好，适合大多数场景
- **VIRTUAL**: 使用 Java 21 虚拟线程，高性能场景下可提升 2-3 倍吞吐量，减少 60% 内存占用

#### 性能对比

| 场景 | PLATFORM | VIRTUAL | 提升 |
|------|----------|---------|------|
| 10万条（16线程） | ~80秒 | ~40秒 | 2倍 |
| 100万条（16线程） | ~800秒 | ~400秒 | 2倍 |
| 内存占用（10万条） | ~500MB | ~200MB | 60%减少 |

#### 使用建议

- 使用 **VIRTUAL** 模式在大规模数据生成（>10万条）或高并发请求场景
- 使用 **PLATFORM** 模式在需要与传统数据库/框架集成或有特殊 JMX 监控需求的场景

### API 请求示例

```bash
curl -X POST http://localhost:8080/api/v1/dataforge/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "count": 100,
    "output": {
      "format": "CSV",
      "file": "output.csv"
    },
    "fields": [
      {"name": "id", "type": "uuid"},
      {"name": "name", "type": "name"},
      {"name": "email", "type": "email"}
    ]
  }'
```

## 开发

### 环境要求

- JDK 21+
- Maven 3.6+
- Node.js 16+ (前端开发)

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=GeneratorFactoryTest

	# 生成覆盖率报告（聚合：api + core + cli + web）
	# 推荐：只触发聚合模块，但会自动构建并运行其依赖模块的测试
	mvn -pl data-forge-coverage -am test
	# 报告位置（聚合）: target/jacoco-aggregate/index.html
	# 报告位置（单模块示例）: data-forge-core/target/jacoco-report/index.html

	# 如需执行覆盖率阈值校验（可能因阈值较高而失败）
	mvn verify
```

### 代码质量检查

```bash
# 运行所有质量检查
./quality-check.sh

# 仅生成报告
./quality-check.sh --report-only
```

### 代码格式化

```bash
# 检查格式
mvn spotless:check

# 自动格式化
mvn spotless:apply
```

## 监控

### 健康检查

```bash
curl http://localhost:8080/api/v1/health
```

### Prometheus 指标

```bash
curl http://localhost:8080/actuator/prometheus
```

### Actuator 端点

- `/actuator/health` - 健康检查
- `/actuator/metrics` - 指标列表
- `/actuator/prometheus` - Prometheus 格式指标

## 安全

### 依赖漏洞扫描

```bash
mvn dependency-check:check
```

### 安全配置

生产环境必须设置以下环境变量：

```bash
export JWT_SECRET=your-secret-key
export DATASOURCE_PASSWORD=your-db-password
export REDIS_PASSWORD=your-redis-password
```

## 性能

### 基准测试

```bash
# 运行性能基准测试
./run-performance-test.sh
```

### 性能指标

- **生成速度**: 10,000+ 记录/秒（单线程）
- **并发支持**: 支持多线程并发生成
- **内存使用**: 流式处理，内存占用稳定
- **响应时间**: P95 < 100ms

## 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

## 联系方式

- 项目主页: [GitHub Repository](https://github.com/0o0obbnn/DataForge-spring)
- 问题反馈: [Issues](https://github.com/0o0obbnn/DataForge-spring/issues)
- 文档: [Documentation](docs/)

## 更新日志

详见 [CHANGELOG.md](CHANGELOG.md)

---

**DataForge** - 让测试数据生成变得简单高效 🚀
