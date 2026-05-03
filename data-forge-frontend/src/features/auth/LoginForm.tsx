import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { z } from "zod";

import { login as loginApi } from "@/features/auth/authApi";
import { useAuthStore } from "@/features/auth/authStore";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";

interface LoginFormProps {
  enableMockAuth: boolean;
}

export function LoginForm({ enableMockAuth }: LoginFormProps) {
  const navigate = useNavigate();
  const storeLogin = useAuthStore((state) => state.login);
  const { t } = useTranslation("common");
  const [errorMessage, setErrorMessage] = useState<string>();

  const loginSchema = z.object({
    username: z.string().trim().min(1, t("auth.usernameRequired")),
    password: z.string().min(1, t("auth.passwordRequired")),
  });

  type LoginFormValues = z.infer<typeof loginSchema>;

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  const handleMockLogin = async () => {
    try {
      const session = await loginApi({ username: "admin", password: "admin123456*" });
      storeLogin({ ...session, mock: true });
      navigate("/dashboard", { replace: true });
    } catch {
      setErrorMessage(t("auth.loginFailed"));
    }
  };

  const onSubmit = async (values: LoginFormValues) => {
    setErrorMessage(undefined);
    try {
      const session = await loginApi(values);
      storeLogin({ ...session, mock: false });
      navigate("/dashboard", { replace: true });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : t("auth.loginFailed"));
    }
  };

  return (
    <div className="space-y-4">
      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="space-y-1.5">
          <label className="text-sm font-medium text-slate-300" htmlFor="username">
            {t("auth.username")}
          </label>
          <Input
            id="username"
            autoComplete="username"
            className="border-slate-700 bg-slate-900 text-slate-100"
            {...register("username")}
          />
          {errors.username ? <p className="text-sm text-red-400">{errors.username.message}</p> : null}
        </div>

        <div className="space-y-1.5">
          <label className="text-sm font-medium text-slate-300" htmlFor="password">
            {t("auth.password")}
          </label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            className="border-slate-700 bg-slate-900 text-slate-100"
            {...register("password")}
          />
          {errors.password ? <p className="text-sm text-red-400">{errors.password.message}</p> : null}
        </div>

        {errorMessage ? (
          <p className="rounded-lg border border-red-800 bg-red-950/50 px-3 py-2 text-sm text-red-300">
            {errorMessage}
          </p>
        ) : null}

        <Button className="w-full" type="submit" disabled={isSubmitting}>
          {isSubmitting ? t("auth.signingIn") : t("auth.signIn")}
        </Button>
      </form>

      {enableMockAuth ? (
        <Button className="w-full" type="button" variant="outline" onClick={handleMockLogin}>
          {t("auth.mockLogin")}
        </Button>
      ) : (
        <p className="rounded-lg border border-slate-800 bg-slate-900/50 px-3 py-2 text-sm text-slate-500">
          {t("auth.mockDisabled")}
        </p>
      )}
    </div>
  );
}
