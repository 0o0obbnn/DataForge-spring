import { useAuthStore } from "@/features/auth/authStore";
import { normalizeApiError } from "@/shared/api/apiErrors";
import { ApiResponse } from "@/shared/api/apiTypes";
import { env } from "@/shared/config/env";

type RequestOptions = RequestInit & {
  skipAuth?: boolean;
};

function hasRequestBody(options: RequestOptions) {
  return options.body !== undefined && options.body !== null;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = useAuthStore.getState().accessToken;
  const headers = new Headers(options.headers);

  if (hasRequestBody(options) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (!options.skipAuth && token) {
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
