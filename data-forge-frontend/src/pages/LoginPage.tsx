import { useTranslation } from "react-i18next";

import { LoginForm } from "@/features/auth/LoginForm";
import { env } from "@/shared/config/env";

export function LoginPage() {
  const { t } = useTranslation("pages");

  return (
    <main className="grid min-h-screen grid-cols-1 lg:grid-cols-[1fr_480px]">
      <section className="hidden flex-col justify-between bg-slate-900 p-12 lg:flex">
        <div>
          <p className="text-sm font-medium text-blue-400">DataForge</p>
          <h1 className="mt-6 max-w-lg text-4xl font-semibold leading-tight text-slate-100">
            {t("login.title")}
          </h1>
          <p className="mt-4 max-w-md text-base leading-relaxed text-slate-400">{t("login.subtitle")}</p>
        </div>
        <p className="text-xs text-slate-600">DataForge v1.0.0</p>
      </section>

      <section className="flex items-center justify-center bg-slate-950 p-8">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <p className="text-sm font-medium text-blue-400">DataForge</p>
            <h1 className="mt-2 text-2xl font-semibold text-slate-100">{t("login.title")}</h1>
          </div>

          <div className="mb-6">
            <h2 className="text-xl font-medium text-slate-100">{t("login.signInTitle")}</h2>
            <p className="mt-1 text-sm text-slate-500">
              {t("login.signInDesc")}
              {env.enableMockAuth ? ` ${t("login.signInDesc")}` : null}
            </p>
          </div>

          <LoginForm enableMockAuth={env.enableMockAuth} />
        </div>
      </section>
    </main>
  );
}
