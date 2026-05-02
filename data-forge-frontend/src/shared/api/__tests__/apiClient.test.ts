import { beforeEach, describe, expect, it, vi } from "vitest";

import { useAuthStore } from "@/features/auth/authStore";
import { ApiClientError } from "@/shared/api/apiErrors";
import { apiRequest } from "@/shared/api/apiClient";

describe("apiRequest", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
    vi.restoreAllMocks();
  });

  it("attaches bearer token for authenticated requests", async () => {
    useAuthStore.getState().login({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      username: "admin",
      expiresIn: 3600,
      mock: false,
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "ok", data: { ready: true } }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await apiRequest<{ ready: boolean }>("/api/v1/dataforge/tasks");
    const headers = fetchMock.mock.calls[0][1]?.headers as Headers;

    expect(result.ready).toBe(true);
    expect(headers.get("Authorization")).toBe("Bearer access-token");
  });

  it("skips bearer token when requested", async () => {
    useAuthStore.getState().login({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      username: "admin",
      expiresIn: 3600,
      mock: false,
    });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "ok", data: true }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await apiRequest<boolean>("/api/v1/auth/login", { skipAuth: true });
    const headers = fetchMock.mock.calls[0][1]?.headers as Headers;

    expect(headers.has("Authorization")).toBe(false);
  });

  it("throws normalized backend errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ code: 401, message: "Invalid username or password" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );

    await expect(apiRequest("/api/v1/auth/login", { skipAuth: true })).rejects.toMatchObject({
      message: "Invalid username or password",
      status: 401,
    });
  });
});
