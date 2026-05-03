import { GenerationTask, DataTemplate } from "@/shared/types/dataforge";

// Mock data for API endpoints (returns same format as real API)
export async function getMockData<T>(path: string, options: RequestInit): Promise<T> {
  // Simulate network delay
  await new Promise((resolve) => setTimeout(resolve, 300));

  // Parse path to determine which mock data to return
  if (path.includes("/tasks") && options.method === "GET") {
    return { data: mockTasks, code: 200, message: "Success" } as T;
  }

  if (path.includes("/templates") && options.method === "GET") {
    return { data: mockTemplates, code: 200, message: "Success" } as T;
  }

  if (path.includes("/generators") && options.method === "GET") {
    return { data: mockGenerators, code: 200, message: "Success" } as T;
  }

  if (path.includes("/sync") || path.includes("/async")) {
    return { data: mockGenerationResult, code: 200, message: "Generation started" } as T;
  }

  // Default empty response
  return { data: {}, code: 200, message: "Success" } as T;
}

// Mock tasks data
const mockTasks: GenerationTask[] = [
  {
    id: 1,
    status: "COMPLETED",
    recordCount: 1000,
    durationMs: 1234,
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    completedAt: new Date(Date.now() - 3500000).toISOString(),
  },
  {
    id: 2,
    status: "COMPLETED",
    recordCount: 5000,
    durationMs: 5678,
    createdAt: new Date(Date.now() - 7200000).toISOString(),
    completedAt: new Date(Date.now() - 7100000).toISOString(),
  },
  {
    id: 3,
    status: "FAILED",
    recordCount: 0,
    errorMessage: "Invalid configuration",
    createdAt: new Date(Date.now() - 10800000).toISOString(),
  },
  {
    id: 4,
    status: "IN_PROGRESS",
    recordCount: 500,
    createdAt: new Date(Date.now() - 300000).toISOString(),
  },
];

// Mock templates data
const mockTemplates: DataTemplate[] = [
  {
    id: 1,
    name: "用户数据模板",
    description: "生成用户基础信息",
    active: true,
    config: 'count: 1000\nfields:\n  - name: id\n    type: uuid\n  - name: name\n    type: name\n  - name: email\n    type: email',
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 3600000).toISOString(),
  },
  {
    id: 2,
    name: "订单数据模板",
    description: "生成模拟订单数据",
    active: true,
    config: 'count: 5000\nfields:\n  - name: orderId\n    type: uuid\n  - name: amount\n    type: decimal\n    params:\n      min: 1\n      max: 1000\n      scale: 2',
    createdAt: new Date(Date.now() - 172800000).toISOString(),
    updatedAt: new Date(Date.now() - 7200000).toISOString(),
  },
];

// Mock generators data (simplified)
const mockGenerators = [
  { id: "uuid", name: "UUID", category: "Core", outputType: "string" },
  { id: "name", name: "Name", category: "Identity", outputType: "string" },
  { id: "email", name: "Email", category: "Internet", outputType: "string" },
  { id: "phone", name: "Phone", category: "Internet", outputType: "string" },
  { id: "integer", name: "Integer", category: "Core", outputType: "number" },
  { id: "decimal", name: "Decimal", category: "Core", outputType: "number" },
  { id: "date", name: "Date", category: "Date/Time", outputType: "string" },
  { id: "boolean", name: "Boolean", category: "Core", outputType: "boolean" },
];

// Mock generation result
const mockGenerationResult = {
  taskId: 999,
  status: "COMPLETED",
  recordCount: 1000,
  durationMs: 500,
  outputFile: "/tmp/data-forge-output-999.csv",
};
