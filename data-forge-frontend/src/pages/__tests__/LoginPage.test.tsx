import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

describe("LoginPage", () => {
  afterEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
  });

  it("hides dev mock login unless explicitly enabled", async () => {
    vi.stubEnv("VITE_ENABLE_MOCK_AUTH", "false");
    const { LoginPage } = await import("@/pages/LoginPage");

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    expect(screen.queryByRole("button", { name: /continue with dev mock/i })).toBeNull();
  });

  it("shows dev mock login when explicitly enabled", async () => {
    vi.stubEnv("VITE_ENABLE_MOCK_AUTH", "true");
    const { LoginPage } = await import("@/pages/LoginPage");

    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole("button", { name: /continue with dev mock/i })).toBeTruthy();
  });
});
