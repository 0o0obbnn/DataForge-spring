import { create } from "zustand";
import { persist } from "zustand/middleware";

import { AuthSession } from "@/features/auth/authTypes";

interface AuthState {
  accessToken?: string;
  refreshToken?: string;
  username?: string;
  expiresIn?: number;
  isAuthenticated: boolean;
  isMockMode: boolean;
  login: (session: AuthSession) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      isAuthenticated: false,
      isMockMode: false,
      login: (session) =>
        set({
          accessToken: session.accessToken,
          refreshToken: session.refreshToken,
          username: session.username,
          expiresIn: session.expiresIn,
          isAuthenticated: true,
          isMockMode: session.mock,
        }),
      logout: () =>
        set({
          accessToken: undefined,
          refreshToken: undefined,
          username: undefined,
          expiresIn: undefined,
          isAuthenticated: false,
          isMockMode: false,
        }),
    }),
    { name: "dataforge-auth" },
  ),
);
