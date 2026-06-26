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

// Fallback: read token directly from localStorage if Zustand store not hydrated yet
function getAccessToken(): string | undefined {
  const storeToken = useAuthStore.getState().accessToken;
  if (storeToken) return storeToken;

  // Fallback to localStorage for early requests before store hydration
  const persisted = localStorage.getItem("dataforge-auth");
  if (persisted) {
    try {
      const parsed = JSON.parse(persisted);
      return parsed.state?.accessToken;
    } catch {
      return undefined;
    }
  }
  return undefined;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = getAccessToken();
  const isMock = isMockMode(token);

  // Mock mode: return mock data (extract .data field to match real API behavior)
  if (isMock && !options.skipAuth) {
    const mockPayload = await getMockData<ApiResponse<T>>(path, options);
    return mockPayload.data as T;
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
