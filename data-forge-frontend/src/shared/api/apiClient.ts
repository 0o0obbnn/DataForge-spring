import { useAuthStore } from "@/features/auth/authStore";
import { normalizeApiError } from "@/shared/api/apiErrors";
import { ApiResponse } from "@/shared/api/apiTypes";
import { env } from "@/shared/config/env";
import { getMockData } from "@/shared/api/mockData";

type RequestOptions = RequestInit & {
  skipAuth?: boolean;
};

function hasRequestBody(options: RequestOptions) {
  return options.body !== undefined && options.body !== null;
}

function isValidJwt(token: string | undefined): token is string {
  if (!token || token === "null" || token === "undefined") return false;
  const parts = token.split(".");
  return parts.length === 3 && parts.every((p) => p.length > 0);
}

function isMockMode(token: string | undefined): boolean {
  if (!token) return false;
  return token.startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtb2NrLW9wZXJhdG9y");
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = useAuthStore.getState().accessToken;
  const isMock = isMockMode(token);

  // Mock mode: return mock data
  if (isMock && !options.skipAuth) {
    return getMockData<T>(path, options);
  }

  const headers = new Headers(options.headers);

  if (hasRequestBody(options) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (!options.skipAuth && isValidJwt(token)) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    ...options,
    headers,
  });

  const payload = await response.json().catch(() => undefined);

  if (!response.ok) {
    throw normalizeApiError(payload, response.status);
  }

  const apiPayload = payload as ApiResponse<T> | undefined;
  return apiPayload?.data as T;
}
