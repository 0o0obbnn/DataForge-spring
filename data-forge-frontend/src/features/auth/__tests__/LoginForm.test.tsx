import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

import { login as loginApi } from "@/features/auth/authApi";
import { LoginForm } from "@/features/auth/LoginForm";
import { useAuthStore } from "@/features/auth/authStore";

vi.mock("@/features/auth/authApi", () => ({
  login: vi.fn(),
}));

describe("LoginForm", () => {
  afterEach(() => {
    useAuthStore.getState().logout();
    vi.clearAllMocks();
  });

  it("validates required username and password", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <LoginForm enableMockAuth={false} />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole("button", { name: /sign in/i }));

    expect(await screen.findByText("Username is required")).toBeTruthy();
    expect(screen.getByText("Password is required")).toBeTruthy();
    expect(loginApi).not.toHaveBeenCalled();
  });

  it("stores real login tokens after successful API login", async () => {
    vi.mocked(loginApi).mockResolvedValue({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      username: "admin",
      expiresIn: 3600,
    });
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <LoginForm enableMockAuth={false} />
      </MemoryRouter>,
    );

    await user.type(screen.getByLabelText(/username/i), "admin");
    await user.type(screen.getByLabelText(/password/i), "password123");
    await user.click(screen.getByRole("button", { name: /sign in/i }));

    await waitFor(() => expect(useAuthStore.getState().isAuthenticated).toBe(true));
    expect(useAuthStore.getState().isMockMode).toBe(false);
    expect(useAuthStore.getState().accessToken).toBe("access-token");
  });

  it("stores mock session when dev mock auth is enabled", async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <LoginForm enableMockAuth />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole("button", { name: /continue with dev mock/i }));

    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().isMockMode).toBe(true);
    expect(loginApi).not.toHaveBeenCalled();
  });
});
