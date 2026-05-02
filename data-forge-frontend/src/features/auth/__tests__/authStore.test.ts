import { beforeEach, describe, expect, it } from "vitest";

import { useAuthStore } from "@/features/auth/authStore";

describe("authStore", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  it("stores real login tokens", () => {
    useAuthStore.getState().login({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      username: "admin",
      expiresIn: 3600,
      mock: false,
    });

    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().username).toBe("admin");
    expect(useAuthStore.getState().isMockMode).toBe(false);
  });

  it("stores mock mode explicitly", () => {
    useAuthStore.getState().login({
      accessToken: "mock-access-token",
      refreshToken: "mock-refresh-token",
      username: "mock-user",
      expiresIn: 3600,
      mock: true,
    });

    expect(useAuthStore.getState().isMockMode).toBe(true);
  });
});
