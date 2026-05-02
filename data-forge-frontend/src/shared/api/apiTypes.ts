export interface ApiResponse<T> {
  code: number;
  message: string;
  data?: T;
  timestamp?: string;
  context?: Record<string, unknown>;
}
