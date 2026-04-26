# DataForge Frontend Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the incomplete frontend remnants with a React + Vite + TypeScript DataForge control console MVP.

**Architecture:** Build a feature-oriented SPA that talks to the existing Spring Boot API through a typed HTTP client. Use TanStack Query for server state, Zustand for auth and builder draft state, shadcn/ui + Tailwind CSS for a dark futuristic console design, and route guards for JWT or dev mock authentication.

**Tech Stack:** React, Vite, TypeScript, Tailwind CSS, shadcn/ui, React Router, TanStack Query, Zustand, React Hook Form, Zod, Recharts, Vitest, React Testing Library, Playwright.

---

## Source Design

Read first:

- `docs/superpowers/specs/2026-04-25-dataforge-frontend-console-design.md`
- `README.md`
- `openapi-spec.yaml`
- `data-forge-web/src/main/java/com/dataforge/web/controller/AuthController.java`
- `data-forge-web/src/main/java/com/dataforge/web/controller/DataForgeController.java`
- `data-forge-web/src/main/java/com/dataforge/web/controller/TemplateController.java`
- `data-forge-web/src/main/java/com/dataforge/web/security/SecurityConfig.java`

## File Structure

Create or replace the frontend as:

```text
data-forge-frontend/
  package.json
  tsconfig.json
  tsconfig.node.json
  vite.config.ts
  vitest.config.ts
  playwright.config.ts
  components.json
  postcss.config.js
  index.html
  src/
    main.tsx
    app/
      App.tsx
      providers.tsx
      router.tsx
    pages/
      LoginPage.tsx
      DashboardPage.tsx
      GeneratorBuilderPage.tsx
      GeneratorCatalogPage.tsx
      TasksPage.tsx
      TemplatesPage.tsx
    features/
      auth/
        authApi.ts
        authStore.ts
        authTypes.ts
        LoginForm.tsx
        __tests__/authStore.test.ts
      dashboard/
        dashboardQueries.ts
        DashboardStats.tsx
      generator-builder/
        builderSchema.ts
        builderStore.ts
        BuilderForm.tsx
        RequestPreview.tsx
        __tests__/builderSchema.test.ts
      generator-catalog/
        generatorCatalogData.ts
        generatorCatalogTypes.ts
        catalogFilters.ts
        GeneratorCatalog.tsx
        GeneratorDetailDrawer.tsx
        __tests__/catalogFilters.test.ts
      tasks/
        taskApi.ts
        taskQueries.ts
        TaskTable.tsx
      templates/
        templateApi.ts
        templateQueries.ts
        TemplateForm.tsx
        TemplateTable.tsx
    shared/
      api/
        apiClient.ts
        apiErrors.ts
        apiTypes.ts
      components/
        AppShell.tsx
        EmptyState.tsx
        ErrorState.tsx
        LoadingState.tsx
        ProtectedRoute.tsx
        StatusBadge.tsx
        ui/
      config/
        env.ts
      lib/
        cn.ts
        format.ts
      types/
        dataforge.ts
    styles/
      globals.css
  tests/
    e2e/
      console-smoke.spec.ts
```

Remove obsolete Vue generated files:

- `data-forge-frontend/auto-imports.d.ts`
- `data-forge-frontend/components.d.ts`
- `data-forge-frontend/env.d.ts`

---

## Task 1: Scaffold React + Vite Foundation

**Files:**

- Create: `data-forge-frontend/package.json`
- Create: `data-forge-frontend/tsconfig.json`
- Create: `data-forge-frontend/tsconfig.node.json`
- Create: `data-forge-frontend/vite.config.ts`
- Create: `data-forge-frontend/vitest.config.ts`
- Create: `data-forge-frontend/src/main.tsx`
- Create: `data-forge-frontend/src/app/App.tsx`
- Create: `data-forge-frontend/src/app/providers.tsx`
- Modify: `data-forge-frontend/index.html`
- Delete: `data-forge-frontend/auto-imports.d.ts`
- Delete: `data-forge-frontend/components.d.ts`
- Delete: `data-forge-frontend/env.d.ts`

- [ ] **Step 1: Create `package.json` with scripts and dependencies**

Use package manager commands instead of pinning versions manually:

```bash
cd data-forge-frontend
npm init -y
npm install @vitejs/plugin-react vite typescript react react-dom react-router-dom @tanstack/react-query zustand react-hook-form zod @hookform/resolvers lucide-react recharts clsx tailwind-merge class-variance-authority
npm install -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom playwright eslint typescript-eslint
```

Then set scripts:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "test": "vitest run",
    "test:watch": "vitest",
    "e2e": "playwright test",
    "lint": "eslint ."
  }
}
```

- [ ] **Step 2: Configure Vite dev proxy**

`data-forge-frontend/vite.config.ts` must proxy `/api` to Spring Boot:

```ts
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import path from "node:path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 3: Create minimal React entry**

`data-forge-frontend/src/main.tsx`:

```tsx
import React from "react";
import ReactDOM from "react-dom/client";

import { App } from "@/app/App";
import { AppProviders } from "@/app/providers";
import "@/styles/globals.css";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </React.StrictMode>,
);
```

`data-forge-frontend/src/app/App.tsx`:

```tsx
export function App() {
  return <div>DataForge Console</div>;
}
```

- [ ] **Step 4: Update `index.html` root**

Use:

```html
<div id="root"></div>
<script type="module" src="/src/main.tsx"></script>
```

- [ ] **Step 5: Verify scaffold**

Run:

```bash
npm run build
```

Expected: TypeScript and Vite build complete without errors.

---

## Task 2: Install Tailwind CSS and shadcn/ui Dark Theme

**Files:**

- Create: `data-forge-frontend/postcss.config.js`
- Create: `data-forge-frontend/components.json`
- Create: `data-forge-frontend/src/styles/globals.css`
- Create: `data-forge-frontend/src/shared/lib/cn.ts`
- Create: `data-forge-frontend/src/shared/components/ui/button.tsx`
- Create: `data-forge-frontend/src/shared/components/ui/card.tsx`
- Create: `data-forge-frontend/src/shared/components/ui/input.tsx`
- Create: `data-forge-frontend/src/shared/components/ui/dialog.tsx`
- Create: `data-forge-frontend/src/shared/components/ui/table.tsx`
- Create: `data-forge-frontend/src/shared/components/ui/badge.tsx`

- [ ] **Step 1: Install Tailwind and initialize shadcn/ui**

Run:

```bash
cd data-forge-frontend
npm install tailwindcss @tailwindcss/vite
npx shadcn@latest init -t vite
npx shadcn@latest add button card input dialog table badge textarea select form dropdown-menu sheet skeleton sonner
```

When prompted, use:

```text
Style: default
Base color: slate
CSS variables: yes
Import alias: @/*
Components path: src/shared/components/ui
Utils path: src/shared/lib/utils
```

- [ ] **Step 2: Ensure `cn` helper exists**

If shadcn creates `utils.ts`, either keep it or expose `cn` from `src/shared/lib/cn.ts`:

```ts
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

- [ ] **Step 3: Add dark futuristic global styles**

`src/styles/globals.css` must include CSS variables from the design spec:

```css
@import "tailwindcss";

:root {
  color-scheme: dark;
  --background: 222 47% 3%;
  --foreground: 210 40% 98%;
  --card: 222 47% 7%;
  --card-foreground: 210 40% 98%;
  --primary: 188 86% 53%;
  --primary-foreground: 222 47% 4%;
  --border: 215 20% 28%;
  --ring: 188 86% 53%;
}

body {
  min-height: 100vh;
  margin: 0;
  background:
    radial-gradient(circle at top left, rgba(34, 211, 238, 0.16), transparent 32rem),
    radial-gradient(circle at top right, rgba(139, 92, 246, 0.14), transparent 28rem),
    #050816;
  color: #f8fafc;
}

* {
  box-sizing: border-box;
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
```

- [ ] **Step 4: Verify styling build**

Run:

```bash
npm run build
```

Expected: CSS and component imports compile.

---

## Task 3: Add App Providers, Routing, and Shell

**Files:**

- Create: `data-forge-frontend/src/app/router.tsx`
- Modify: `data-forge-frontend/src/app/App.tsx`
- Modify: `data-forge-frontend/src/app/providers.tsx`
- Create: `data-forge-frontend/src/shared/components/AppShell.tsx`
- Create: `data-forge-frontend/src/shared/components/ProtectedRoute.tsx`
- Create: `data-forge-frontend/src/shared/components/StatusBadge.tsx`
- Create: `data-forge-frontend/src/shared/components/EmptyState.tsx`
- Create: `data-forge-frontend/src/shared/components/ErrorState.tsx`
- Create: `data-forge-frontend/src/shared/components/LoadingState.tsx`
- Create: `data-forge-frontend/src/pages/LoginPage.tsx`
- Create: `data-forge-frontend/src/pages/DashboardPage.tsx`
- Create: `data-forge-frontend/src/pages/GeneratorBuilderPage.tsx`
- Create: `data-forge-frontend/src/pages/GeneratorCatalogPage.tsx`
- Create: `data-forge-frontend/src/pages/TasksPage.tsx`
- Create: `data-forge-frontend/src/pages/TemplatesPage.tsx`

- [ ] **Step 1: Add QueryClient provider**

`src/app/providers.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useState } from "react";

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
```

- [ ] **Step 2: Create protected route**

`src/shared/components/ProtectedRoute.tsx`:

```tsx
import { Navigate, Outlet } from "react-router-dom";

import { useAuthStore } from "@/features/auth/authStore";

export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}
```

- [ ] **Step 3: Create route config**

`src/app/router.tsx`:

```tsx
import { createBrowserRouter, Navigate } from "react-router-dom";

import { ProtectedRoute } from "@/shared/components/ProtectedRoute";
import { AppShell } from "@/shared/components/AppShell";
import { DashboardPage } from "@/pages/DashboardPage";
import { GeneratorBuilderPage } from "@/pages/GeneratorBuilderPage";
import { GeneratorCatalogPage } from "@/pages/GeneratorCatalogPage";
import { LoginPage } from "@/pages/LoginPage";
import { TasksPage } from "@/pages/TasksPage";
import { TemplatesPage } from "@/pages/TemplatesPage";

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: "/dashboard", element: <DashboardPage /> },
          { path: "/builder", element: <GeneratorBuilderPage /> },
          { path: "/catalog", element: <GeneratorCatalogPage /> },
          { path: "/tasks", element: <TasksPage /> },
          { path: "/templates", element: <TemplatesPage /> },
        ],
      },
    ],
  },
]);
```

- [ ] **Step 4: Wire router**

`src/app/App.tsx`:

```tsx
import { RouterProvider } from "react-router-dom";

import { router } from "@/app/router";

export function App() {
  return <RouterProvider router={router} />;
}
```

- [ ] **Step 5: Verify routing**

Run:

```bash
npm run build
```

Expected: route imports compile; unauthenticated users route to login.

---

## Task 4: Implement Environment and API Client

**Files:**

- Create: `data-forge-frontend/src/shared/config/env.ts`
- Create: `data-forge-frontend/src/shared/api/apiTypes.ts`
- Create: `data-forge-frontend/src/shared/api/apiErrors.ts`
- Create: `data-forge-frontend/src/shared/api/apiClient.ts`
- Create: `data-forge-frontend/src/shared/types/dataforge.ts`
- Create: `data-forge-frontend/src/shared/api/__tests__/apiErrors.test.ts`

- [ ] **Step 1: Write API error test**

`src/shared/api/__tests__/apiErrors.test.ts`:

```ts
import { describe, expect, it } from "vitest";

import { normalizeApiError } from "@/shared/api/apiErrors";

describe("normalizeApiError", () => {
  it("uses backend message when present", () => {
    const error = normalizeApiError({ code: 400, message: "Invalid config" });
    expect(error.message).toBe("Invalid config");
    expect(error.status).toBe(400);
  });

  it("falls back to a stable message", () => {
    const error = normalizeApiError(undefined);
    expect(error.message).toBe("Request failed");
  });
});
```

- [ ] **Step 2: Run failing test**

Run:

```bash
npm run test -- src/shared/api/__tests__/apiErrors.test.ts
```

Expected: fails because `normalizeApiError` does not exist.

- [ ] **Step 3: Implement API types and error normalization**

`src/shared/api/apiTypes.ts`:

```ts
export interface ApiResponse<T> {
  code: number;
  message: string;
  data?: T;
  timestamp?: string;
}
```

`src/shared/api/apiErrors.ts`:

```ts
export class ApiClientError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "ApiClientError";
  }
}

export function normalizeApiError(payload: unknown, status?: number): ApiClientError {
  if (payload && typeof payload === "object" && "message" in payload) {
    const message = String((payload as { message: unknown }).message);
    const code = "code" in payload ? Number((payload as { code: unknown }).code) : status;
    return new ApiClientError(message, code);
  }

  return new ApiClientError("Request failed", status);
}
```

- [ ] **Step 4: Implement HTTP client**

`src/shared/api/apiClient.ts`:

```ts
import { useAuthStore } from "@/features/auth/authStore";
import { env } from "@/shared/config/env";
import { ApiResponse } from "@/shared/api/apiTypes";
import { normalizeApiError } from "@/shared/api/apiErrors";

type RequestOptions = RequestInit & {
  skipAuth?: boolean;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = useAuthStore.getState().accessToken;
  const headers = new Headers(options.headers);

  headers.set("Content-Type", "application/json");

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

  const apiPayload = payload as ApiResponse<T>;
  return apiPayload.data as T;
}
```

- [ ] **Step 5: Verify**

Run:

```bash
npm run test -- src/shared/api/__tests__/apiErrors.test.ts
npm run build
```

Expected: tests and build pass.

---

## Task 5: Implement Auth Store, API, and Login Page

**Files:**

- Create: `data-forge-frontend/src/features/auth/authTypes.ts`
- Create: `data-forge-frontend/src/features/auth/authStore.ts`
- Create: `data-forge-frontend/src/features/auth/authApi.ts`
- Create: `data-forge-frontend/src/features/auth/LoginForm.tsx`
- Modify: `data-forge-frontend/src/pages/LoginPage.tsx`
- Create: `data-forge-frontend/src/features/auth/__tests__/authStore.test.ts`

- [ ] **Step 1: Write auth store test**

`src/features/auth/__tests__/authStore.test.ts`:

```ts
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
```

- [ ] **Step 2: Run failing test**

Run:

```bash
npm run test -- src/features/auth/__tests__/authStore.test.ts
```

Expected: fails because `authStore` does not exist.

- [ ] **Step 3: Implement auth types and store**

`src/features/auth/authTypes.ts`:

```ts
export interface LoginRequest {
  username: string;
  password: string;
}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  username: string;
  expiresIn: number;
}

export interface AuthSession extends JwtResponse {
  mock: boolean;
}
```

`src/features/auth/authStore.ts`:

```ts
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
```

- [ ] **Step 4: Implement login API and form**

`src/features/auth/authApi.ts`:

```ts
import { apiRequest } from "@/shared/api/apiClient";
import { JwtResponse, LoginRequest } from "@/features/auth/authTypes";

export function login(request: LoginRequest) {
  return apiRequest<JwtResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
    skipAuth: true,
  });
}
```

`LoginForm` should:

- Validate non-empty username and password with Zod.
- If dev mock is enabled and selected, call `useAuthStore.getState().login()` with mock tokens.
- Otherwise call `login()`, store returned tokens, and navigate to `/dashboard`.
- Render a visible `DEV MOCK` badge when mock mode is active.

- [ ] **Step 5: Verify**

Run:

```bash
npm run test -- src/features/auth/__tests__/authStore.test.ts
npm run build
```

Expected: auth store tests pass and login page compiles.

---

## Task 6: Build App Shell and Dark Console Navigation

**Files:**

- Modify: `data-forge-frontend/src/shared/components/AppShell.tsx`
- Modify: `data-forge-frontend/src/shared/components/StatusBadge.tsx`
- Modify: `data-forge-frontend/src/pages/DashboardPage.tsx`

- [ ] **Step 1: Implement `StatusBadge`**

Use status colors:

```tsx
type Status = "healthy" | "running" | "completed" | "failed" | "warning";

const statusClassName: Record<Status, string> = {
  healthy: "border-cyan-400/40 bg-cyan-400/10 text-cyan-200",
  running: "border-emerald-400/40 bg-emerald-400/10 text-emerald-200",
  completed: "border-green-400/40 bg-green-400/10 text-green-200",
  failed: "border-rose-400/40 bg-rose-400/10 text-rose-200",
  warning: "border-amber-400/40 bg-amber-400/10 text-amber-200",
};
```

- [ ] **Step 2: Implement `AppShell`**

Shell requirements:

- Left sidebar links to `/dashboard`, `/builder`, `/catalog`, `/tasks`, `/templates`.
- Header shows `DataForge Console`, current username, auth mode badge, and logout action.
- Main content renders `<Outlet />`.
- Use glass panel styling and visible focus states.

- [ ] **Step 3: Verify navigation**

Run:

```bash
npm run build
```

Expected: protected pages render inside shell after mock login.

---

## Task 7: Implement Generator Catalog Seed Data and Filtering

**Files:**

- Create: `data-forge-frontend/src/features/generator-catalog/generatorCatalogTypes.ts`
- Create: `data-forge-frontend/src/features/generator-catalog/generatorCatalogData.ts`
- Create: `data-forge-frontend/src/features/generator-catalog/catalogFilters.ts`
- Create: `data-forge-frontend/src/features/generator-catalog/__tests__/catalogFilters.test.ts`
- Create: `data-forge-frontend/src/features/generator-catalog/GeneratorCatalog.tsx`
- Create: `data-forge-frontend/src/features/generator-catalog/GeneratorDetailDrawer.tsx`
- Modify: `data-forge-frontend/src/pages/GeneratorCatalogPage.tsx`

- [ ] **Step 1: Write catalog filter tests**

`catalogFilters.test.ts`:

```ts
import { describe, expect, it } from "vitest";

import { filterGenerators } from "@/features/generator-catalog/catalogFilters";
import { GeneratorDefinition } from "@/features/generator-catalog/generatorCatalogTypes";

const generators: GeneratorDefinition[] = [
  { id: "uuid", name: "UUID", category: "Identity", summary: "Unique identifiers", sample: "550e8400-e29b-41d4-a716-446655440000", params: [] },
  { id: "email", name: "Email", category: "Internet", summary: "Email addresses", sample: "user@example.com", params: [] },
];

describe("filterGenerators", () => {
  it("filters by text", () => {
    expect(filterGenerators(generators, { query: "email", category: "All" })).toHaveLength(1);
  });

  it("filters by category", () => {
    expect(filterGenerators(generators, { query: "", category: "Identity" })[0].id).toBe("uuid");
  });
});
```

- [ ] **Step 2: Implement catalog types and filter**

```ts
export interface GeneratorParam {
  name: string;
  type: "string" | "number" | "boolean" | "select";
  required: boolean;
  description: string;
  options?: string[];
}

export interface GeneratorDefinition {
  id: string;
  name: string;
  category: string;
  summary: string;
  sample: string;
  params: GeneratorParam[];
}
```

`filterGenerators` must check query against id, name, category, summary, and sample.

- [ ] **Step 3: Add seed generators**

Seed at least these MVP definitions:

```text
uuid, string, integer, decimal, boolean, date, timestamp, name, email, phone, idcard, address, bankcard, ip, mac, url, domain, company, vehicle, yaml
```

Each generator must have a category, summary, sample, and param array.

- [ ] **Step 4: Build Catalog UI**

UI requirements:

- Search input at top.
- Category chips.
- Bento grid cards.
- Detail sheet with params and `Add to Builder`.
- `Add to Builder` writes selected generator into builder store.

- [ ] **Step 5: Verify**

Run:

```bash
npm run test -- src/features/generator-catalog/__tests__/catalogFilters.test.ts
npm run build
```

Expected: tests pass and Catalog compiles.

---

## Task 8: Implement Generator Builder Schema and Draft Store

**Files:**

- Create: `data-forge-frontend/src/features/generator-builder/builderSchema.ts`
- Create: `data-forge-frontend/src/features/generator-builder/builderStore.ts`
- Create: `data-forge-frontend/src/features/generator-builder/__tests__/builderSchema.test.ts`
- Create: `data-forge-frontend/src/features/generator-builder/BuilderForm.tsx`
- Create: `data-forge-frontend/src/features/generator-builder/RequestPreview.tsx`
- Modify: `data-forge-frontend/src/pages/GeneratorBuilderPage.tsx`

- [ ] **Step 1: Write builder validation tests**

Test cases:

- valid request with `count`, `output`, and one field passes.
- duplicate field names fail.
- `count < 1` fails.

Use Zod `safeParse` assertions.

- [ ] **Step 2: Implement schema**

Schema shape:

```ts
export const generateRequestSchema = z.object({
  count: z.number().int().min(1).max(1_000_000),
  threads: z.number().int().min(1).max(16).optional(),
  validate: z.boolean().default(true),
  seed: z.number().int().optional(),
  output: z.object({
    format: z.enum(["CSV", "JSON", "SQL", "CONSOLE"]),
    file: z.string().optional(),
    encoding: z.string().default("UTF-8"),
  }),
  fields: z.array(
    z.object({
      name: z.string().min(1),
      type: z.string().min(1),
      params: z.record(z.string(), z.unknown()).optional(),
    }),
  ).min(1),
}).superRefine((value, context) => {
  const names = new Set<string>();
  value.fields.forEach((field, index) => {
    if (names.has(field.name)) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Field names must be unique",
        path: ["fields", index, "name"],
      });
    }
    names.add(field.name);
  });
});
```

- [ ] **Step 3: Implement builder store**

Store actions:

- `addField(generatorType: string)`
- `removeField(index: number)`
- `duplicateField(index: number)`
- `updateField(index: number, patch)`
- `resetDraft()`

- [ ] **Step 4: Build form and preview**

Form must support:

- Field rows.
- Add field.
- JSON preview.
- Run Sync button.
- Run Async button.
- Save as Template button.

- [ ] **Step 5: Verify**

Run:

```bash
npm run test -- src/features/generator-builder/__tests__/builderSchema.test.ts
npm run build
```

Expected: validation tests pass and Builder compiles.

---

## Task 9: Implement Data Generation API Hooks

**Files:**

- Create: `data-forge-frontend/src/features/generator-builder/generationApi.ts`
- Modify: `data-forge-frontend/src/features/generator-builder/BuilderForm.tsx`
- Modify: `data-forge-frontend/src/shared/types/dataforge.ts`

- [ ] **Step 1: Define request and response types**

`shared/types/dataforge.ts` should include:

```ts
export interface GenerateField {
  name: string;
  type: string;
  params?: Record<string, unknown>;
}

export interface GenerateRequest {
  count: number;
  threads?: number;
  validate: boolean;
  seed?: number;
  output: {
    format: "CSV" | "JSON" | "SQL" | "CONSOLE";
    file?: string;
    encoding?: string;
  };
  fields: GenerateField[];
}

export interface SyncGenerationResult {
  message: string;
  outputPath?: string;
}
```

- [ ] **Step 2: Implement API calls**

```ts
export function generateSync(request: GenerateRequest) {
  return apiRequest<SyncGenerationResult>("/api/v1/dataforge/generate", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function generateAsync(request: GenerateRequest) {
  return apiRequest<number>("/api/v1/dataforge/generate/async", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
```

- [ ] **Step 3: Wire mutations**

Builder submit behavior:

- Sync success shows message and output path.
- Async success navigates to `/tasks` and highlights returned task id.
- API errors show `ErrorState` or inline toast.

- [ ] **Step 4: Verify against mock auth**

Run:

```bash
npm run build
```

Expected: Builder compiles and mutation types align.

---

## Task 10: Implement Tasks API, Polling, and Table

**Files:**

- Create: `data-forge-frontend/src/features/tasks/taskApi.ts`
- Create: `data-forge-frontend/src/features/tasks/taskQueries.ts`
- Create: `data-forge-frontend/src/features/tasks/TaskTable.tsx`
- Modify: `data-forge-frontend/src/pages/TasksPage.tsx`
- Modify: `data-forge-frontend/src/shared/types/dataforge.ts`

- [ ] **Step 1: Add task types**

```ts
export type GenerationTaskStatus = "IN_PROGRESS" | "COMPLETED" | "FAILED";

export interface GenerationTask {
  id: number;
  status: GenerationTaskStatus;
  recordCount: number;
  durationMs?: number;
  errorMessage?: string;
  createdAt?: string;
  completedAt?: string;
}
```

- [ ] **Step 2: Implement API calls**

```ts
export function listRecentTasks(page = 0, size = 10) {
  return apiRequest<GenerationTask[]>(`/api/v1/dataforge/tasks?page=${page}&size=${size}`);
}

export function getTask(taskId: number) {
  return apiRequest<GenerationTask>(`/api/v1/dataforge/tasks/${taskId}`);
}
```

- [ ] **Step 3: Implement polling query**

Use `refetchInterval` only while any returned task has `status === "IN_PROGRESS"`.

- [ ] **Step 4: Build table UI**

Table columns:

- id
- status badge
- record count
- duration
- created time
- completed time
- error message

- [ ] **Step 5: Verify**

Run:

```bash
npm run build
```

Expected: Tasks page compiles and has loading, empty, error, and data states.

---

## Task 11: Implement Templates API and Forms

**Files:**

- Create: `data-forge-frontend/src/features/templates/templateApi.ts`
- Create: `data-forge-frontend/src/features/templates/templateQueries.ts`
- Create: `data-forge-frontend/src/features/templates/TemplateForm.tsx`
- Create: `data-forge-frontend/src/features/templates/TemplateTable.tsx`
- Modify: `data-forge-frontend/src/pages/TemplatesPage.tsx`
- Modify: `data-forge-frontend/src/shared/types/dataforge.ts`

- [ ] **Step 1: Define template types**

```ts
export interface DataTemplate {
  id?: number;
  name: string;
  description?: string;
  config: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}
```

- [ ] **Step 2: Implement template API**

Functions:

- `listTemplates()`
- `createTemplate(template)`
- `updateTemplate(id, template)`
- `deleteTemplate(id)`
- `generateFromTemplate(templateId, request)`

Use endpoints under `/api/v1/templates` and `/api/v1/dataforge/generate/template/{templateId}`.

- [ ] **Step 3: Build template table and form**

Requirements:

- Show active/inactive.
- Create and edit template in a sheet/dialog.
- Delete with confirmation.
- Generate from template prompts for count and routes to Tasks on success.

- [ ] **Step 4: Verify**

Run:

```bash
npm run build
```

Expected: Templates page compiles and all mutations invalidate template queries.

---

## Task 12: Implement Dashboard

**Files:**

- Create: `data-forge-frontend/src/features/dashboard/dashboardQueries.ts`
- Create: `data-forge-frontend/src/features/dashboard/DashboardStats.tsx`
- Modify: `data-forge-frontend/src/pages/DashboardPage.tsx`

- [ ] **Step 1: Derive dashboard stats from tasks**

Stats:

- total recent tasks
- completed count
- failed count
- running count
- average duration for completed tasks

- [ ] **Step 2: Build KPI cards**

Cards:

- Recent Jobs
- Running
- Completed
- Failed
- Average Duration

- [ ] **Step 3: Add quick actions**

Buttons:

- New Generation -> `/builder`
- Browse Catalog -> `/catalog`
- Manage Templates -> `/templates`

- [ ] **Step 4: Verify**

Run:

```bash
npm run build
```

Expected: Dashboard compiles and uses task data query.

---

## Task 13: Add Formatting Helpers and Shared States

**Files:**

- Create: `data-forge-frontend/src/shared/lib/format.ts`
- Modify: `data-forge-frontend/src/shared/components/EmptyState.tsx`
- Modify: `data-forge-frontend/src/shared/components/ErrorState.tsx`
- Modify: `data-forge-frontend/src/shared/components/LoadingState.tsx`

- [ ] **Step 1: Implement format helpers**

Functions:

- `formatDuration(ms?: number): string`
- `formatDateTime(value?: string): string`
- `formatNumber(value?: number): string`

Expected behavior:

- Missing values render `-`.
- Durations under one second render milliseconds.
- Durations over one second render seconds with one decimal.

- [ ] **Step 2: Add unit tests for format helpers**

Test missing values, milliseconds, seconds, and number formatting.

- [ ] **Step 3: Build shared state components**

Components must accept explicit title/message props and render accessible text.

- [ ] **Step 4: Verify**

Run:

```bash
npm run test -- src/shared/lib/format.test.ts
npm run build
```

Expected: tests pass and shared state components compile.

---

## Task 14: Add E2E Smoke Test

**Files:**

- Create: `data-forge-frontend/playwright.config.ts`
- Create: `data-forge-frontend/tests/e2e/console-smoke.spec.ts`

- [ ] **Step 1: Configure Playwright**

Use Vite dev server:

```ts
import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1",
    url: "http://127.0.0.1:5173",
    reuseExistingServer: !process.env.CI,
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
```

- [ ] **Step 2: Write mock auth smoke test**

Test flow:

- Visit `/login`.
- Enable dev mock login.
- Submit login.
- Assert `/dashboard`.
- Navigate to Catalog.
- Search `email`.
- Add Email generator to Builder.
- Assert Builder contains an email field.

- [ ] **Step 3: Verify**

Run:

```bash
npm run e2e
```

Expected: smoke test passes in mock auth mode.

---

## Task 15: Final Verification and Documentation

**Files:**

- Modify: `README.md`
- Create: `data-forge-frontend/.env.example`

- [ ] **Step 1: Add frontend env example**

`data-forge-frontend/.env.example`:

```text
VITE_API_BASE_URL=
VITE_ENABLE_MOCK_AUTH=true
```

- [ ] **Step 2: Update README frontend section**

Add commands:

```bash
cd data-forge-frontend
npm install
npm run dev
npm run build
npm run test
npm run e2e
```

Mention backend expected at:

```text
http://localhost:8080
```

- [ ] **Step 3: Run full frontend verification**

Run:

```bash
cd data-forge-frontend
npm run lint
npm run test
npm run build
npm run e2e
```

Expected:

- lint exits 0
- unit tests pass
- production build succeeds
- Playwright smoke test passes

- [ ] **Step 4: Optional backend contract check**

If backend is running:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/v3/api-docs
```

Expected: health and OpenAPI endpoints respond successfully.

---

## Self-Review

Spec coverage:

- Control console MVP: Tasks 3, 6, 12.
- React + Vite + TypeScript scaffold: Task 1.
- Tailwind + shadcn dark futuristic UI: Tasks 2 and 6.
- JWT + dev mock auth: Task 5.
- API client and Spring Boot proxy: Tasks 1 and 4.
- Generator Catalog: Task 7.
- Generator Builder: Tasks 8 and 9.
- Tasks polling: Task 10.
- Templates CRUD: Task 11.
- Dashboard: Task 12.
- Testing: Tasks 4, 5, 7, 8, 13, 14, 15.

No unresolved placeholders remain. MVP decisions from the design spec are represented directly: frontend seed data for Catalog, Recharts for Dashboard, and `outputPath` display without direct download.
