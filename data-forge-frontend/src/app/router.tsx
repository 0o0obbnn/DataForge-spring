import { lazy, Suspense, type ReactElement } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";

import { AppShell } from "@/shared/components/AppShell";
import { LoadingState } from "@/shared/components/LoadingState";
import { ProtectedRoute } from "@/shared/components/ProtectedRoute";

const DashboardPage = lazy(() => import("@/pages/DashboardPage").then((module) => ({ default: module.DashboardPage })));
const GeneratorBuilderPage = lazy(() =>
  import("@/pages/GeneratorBuilderPage").then((module) => ({ default: module.GeneratorBuilderPage })),
);
const GeneratorCatalogPage = lazy(() =>
  import("@/pages/GeneratorCatalogPage").then((module) => ({ default: module.GeneratorCatalogPage })),
);
const LoginPage = lazy(() => import("@/pages/LoginPage").then((module) => ({ default: module.LoginPage })));
const TasksPage = lazy(() => import("@/pages/TasksPage").then((module) => ({ default: module.TasksPage })));
const TemplatesPage = lazy(() => import("@/pages/TemplatesPage").then((module) => ({ default: module.TemplatesPage })));

function page(element: ReactElement) {
  return <Suspense fallback={<LoadingState label="Loading page" />}>{element}</Suspense>;
}

export const router = createBrowserRouter([
  { path: "/login", element: page(<LoginPage />) },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: "/dashboard", element: page(<DashboardPage />) },
          { path: "/builder", element: page(<GeneratorBuilderPage />) },
          { path: "/catalog", element: page(<GeneratorCatalogPage />) },
          { path: "/tasks", element: page(<TasksPage />) },
          { path: "/templates", element: page(<TemplatesPage />) },
        ],
      },
    ],
  },
  { path: "*", element: <Navigate to="/dashboard" replace /> },
]);
