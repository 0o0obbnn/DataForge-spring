export interface GenerateField {
  name: string;
  type: string;
  params?: Record<string, unknown>;
}

export interface GenerateRequest {
  count: number;
  threads?: number;
  validate: boolean;
  seed?: number;
  output: {
    format: "CSV" | "JSON" | "SQL" | "CONSOLE";
    file?: string;
    encoding?: string;
  };
  fields: GenerateField[];
}

export interface SyncGenerationResult {
  message: string;
  outputPath?: string;
}

export type GenerationTaskStatus = "IN_PROGRESS" | "COMPLETED" | "FAILED";

export interface GenerationTask {
  id: number;
  status: GenerationTaskStatus;
  recordCount: number;
  durationMs?: number;
  errorMessage?: string;
  createdAt?: string;
  completedAt?: string;
}

export interface DataTemplate {
  id?: number;
  name: string;
  description?: string;
  config: string;
  active?: boolean;
  version?: number;
  createdAt?: string;
  updatedAt?: string;
}
