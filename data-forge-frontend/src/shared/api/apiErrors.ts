export class ApiClientError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly context?: Record<string, unknown>,
  ) {
    super(message);
    this.name = "ApiClientError";
  }
}

export function normalizeApiError(payload: unknown, status?: number): ApiClientError {
  if (payload && typeof payload === "object") {
    const errorPayload = payload as { code?: unknown; message?: unknown; context?: unknown };
    const message = typeof errorPayload.message === "string" ? errorPayload.message : "Request failed";
    const responseStatus = typeof errorPayload.code === "number" ? errorPayload.code : status;
    const context =
      errorPayload.context && typeof errorPayload.context === "object"
        ? (errorPayload.context as Record<string, unknown>)
        : undefined;

    return new ApiClientError(message, responseStatus, context);
  }

  return new ApiClientError("Request failed", status);
}
