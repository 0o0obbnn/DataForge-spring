# DataForge API 使用指南

## 概述

DataForge 提供了完整的 REST API 用于测试数据生成。本文档介绍如何使用这些 API。

## 基础信息

- **Base URL**: `http://localhost:8080/api/v1`
- **API 版本**: v1
- **认证方式**: JWT Bearer Token
- **内容类型**: `application/json`

## 快速开始

### 1. 认证

首先需要获取访问令牌：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your-username",
    "password": "your-password"
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "username": "your-username"
  },
  "timestamp": "2024-01-01T12:00:00"
}
```

### 2. 生成数据

使用获取的令牌生成测试数据：

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
      {
        "name": "id",
        "type": "uuid"
      },
      {
        "name": "name",
        "type": "name",
        "params": {
          "type": "CN",
          "gender": "ANY"
        }
      },
      {
        "name": "email",
        "type": "email"
      }
    ],
    "validate": true,
    "threads": 4
  }'
```

## API 端点

### 认证 API

#### POST /api/v1/auth/login

用户登录并获取 JWT 令牌。

**请求体**:
```json
{
  "username": "string",
  "password": "string"
}
```

**响应**:
- `200 OK`: 登录成功
- `401 Unauthorized`: 用户名或密码错误

### 数据生成 API

#### POST /api/v1/dataforge/generate

同步生成测试数据。

**请求体**:
```json
{
  "count": 100,
  "output": {
    "format": "CSV|JSON|SQL|CONSOLE",
    "file": "output.csv",
    "encoding": "UTF-8"
  },
  "fields": [
    {
      "name": "fieldName",
      "type": "generatorType",
      "params": {}
    }
  ],
  "validate": true,
  "threads": 1,
  "seed": 12345
}
```

**响应**:
- `200 OK`: 生成成功
- `400 Bad Request`: 参数验证失败
- `500 Internal Server Error`: 服务器错误

#### POST /api/v1/dataforge/generate/async

异步生成测试数据，立即返回任务ID。

**响应**:
```json
{
  "code": 200,
  "message": "Task submitted successfully",
  "data": 123,
  "timestamp": "2024-01-01T12:00:00"
}
```

#### GET /api/v1/dataforge/tasks/{taskId}

查询异步任务状态。

**响应**:
```json
{
  "code": 200,
  "message": "Task status retrieved successfully",
  "data": {
    "id": 123,
    "status": "COMPLETED|IN_PROGRESS|FAILED",
    "recordCount": 1000,
    "durationMs": 5000,
    "completedAt": "2024-01-01T12:05:00",
    "errorMessage": null
  }
}
```

### 模板管理 API

#### POST /api/v1/templates

创建数据模板。

#### GET /api/v1/templates

获取所有模板列表。

#### GET /api/v1/templates/{id}

根据ID获取模板。

#### PUT /api/v1/templates/{id}

更新模板。

#### DELETE /api/v1/templates/{id}

删除模板。

### 健康检查 API

#### GET /api/v1/health

系统健康检查，返回系统状态、生成器数量、内存使用等信息。

## 支持的数据生成器类型

DataForge 支持 60+ 种数据生成器，包括：

- **基础类型**: `uuid`, `string`, `integer`, `decimal`, `boolean`, `date`, `datetime`
- **个人信息**: `name`, `email`, `phone`, `idCard`, `passport`
- **地址信息**: `address`, `city`, `province`, `postcode`
- **金融信息**: `bankCard`, `creditCard`, `accountNumber`
- **网络信息**: `ip`, `mac`, `url`, `domain`
- **其他**: `licensePlate`, `companyName`, `jobTitle`

详细列表请参考 [生成器文档](../docs/DataGen-Usage-Guide.md)。

## 错误处理

所有 API 错误都遵循统一的响应格式：

```json
{
  "code": 400,
  "message": "Error message",
  "data": null,
  "timestamp": "2024-01-01T12:00:00",
  "context": {
    "errorType": "ValidationException",
    "fieldErrors": {
      "count": "Count must be at least 1"
    }
  }
}
```

### 常见错误码

- `400`: 请求参数错误
- `401`: 未授权
- `403`: 禁止访问
- `404`: 资源不存在
- `429`: 请求过于频繁（限流）
- `500`: 服务器内部错误

## 限流

API 支持限流保护，默认限制：
- 每个IP每分钟最多 60 次请求
- 数据生成接口每分钟最多 10 次请求

超过限制将返回 `429 Too Many Requests`。

## 最佳实践

1. **使用模板**: 对于重复使用的配置，建议创建模板
2. **异步生成**: 大批量数据（>10万条）使用异步接口
3. **错误处理**: 始终检查响应码和错误信息
4. **令牌管理**: 定期刷新令牌，不要硬编码
5. **参数验证**: 在客户端进行基础验证，减少无效请求

## 示例代码

### Java (Spring RestTemplate)

```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.setBearerAuth(token);

GenerateRequest request = new GenerateRequest();
request.setCount(100);
// ... 设置其他参数

HttpEntity<GenerateRequest> entity = new HttpEntity<>(request, headers);
ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
    "http://localhost:8080/api/v1/dataforge/generate",
    entity,
    ApiResponse.class
);
```

### Python

```python
import requests

headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {token}"
}

data = {
    "count": 100,
    "output": {"format": "CSV", "file": "output.csv"},
    "fields": [
        {"name": "id", "type": "uuid"},
        {"name": "name", "type": "name"}
    ]
}

response = requests.post(
    "http://localhost:8080/api/v1/dataforge/generate",
    json=data,
    headers=headers
)
```

### JavaScript/TypeScript

```typescript
const response = await fetch('http://localhost:8080/api/v1/dataforge/generate', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    count: 100,
    output: { format: 'CSV', file: 'output.csv' },
    fields: [
      { name: 'id', type: 'uuid' },
      { name: 'name', type: 'name' }
    ]
  })
});

const result = await response.json();
```

## 更多资源

- [Swagger UI](http://localhost:8080/swagger-ui.html) - 交互式 API 文档
- [OpenAPI 规范](http://localhost:8080/v3/api-docs) - OpenAPI 3.0 规范
- [架构文档](./ARCHITECTURE.md) - 系统架构说明
- [最佳实践](./BEST-PRACTICES.md) - 使用最佳实践
