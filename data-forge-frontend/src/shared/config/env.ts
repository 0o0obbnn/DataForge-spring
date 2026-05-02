interface Env {
  apiBaseUrl: string;
  enableMockAuth: boolean;
}

function normalizeApiBaseUrl(value: string | undefined) {
  return value?.replace(/\/$/, "") ?? "";
}

export const env: Env = {
  apiBaseUrl: normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL),
  enableMockAuth: import.meta.env.VITE_ENABLE_MOCK_AUTH === "true",
};
