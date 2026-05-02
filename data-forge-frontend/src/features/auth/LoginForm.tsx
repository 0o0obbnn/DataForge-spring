import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { z } from "zod";

import { login as loginApi } from "@/features/auth/authApi";
import { useAuthStore } from "@/features/auth/authStore";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";

const loginSchema = z.object({
  username: z.string().trim().min(1, "Username is required"),
  password: z.string().min(1, "Password is required"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

interface LoginFormProps {
  enableMockAuth: boolean;
}

export function LoginForm({ enableMockAuth }: LoginFormProps) {
  const navigate = useNavigate();
  const storeLogin = useAuthStore((state) => state.login);
  const [errorMessage, setErrorMessage] = useState<string>();
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

  const handleMockLogin = () => {
    storeLogin({
      accessToken: "mock-access-token",
      refreshToken: "mock-refresh-token",
      username: "mock-operator",
      expiresIn: 3600,
      mock: true,
    });
    navigate("/dashboard", { replace: true });
  };

  const onSubmit = async (values: LoginFormValues) => {
    setErrorMessage(undefined);
    try {
      const session = await loginApi(values);
      storeLogin({ ...session, mock: false });
      navigate("/dashboard", { replace: true });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Login failed");
    }
  };

  return (
    <div className="space-y-5">
      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="space-y-2">
          <label className="text-sm font-medium text-slate-200" htmlFor="username">
            Username
          </label>
          <Input
            id="username"
            autoComplete="username"
            className="border-slate-700/60 bg-slate-950/60 text-slate-50"
            {...register("username")}
          />
          {errors.username ? <p className="text-sm text-rose-300">{errors.username.message}</p> : null}
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-slate-200" htmlFor="password">
            Password
          </label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            className="border-slate-700/60 bg-slate-950/60 text-slate-50"
            {...register("password")}
          />
          {errors.password ? <p className="text-sm text-rose-300">{errors.password.message}</p> : null}
        </div>

        {errorMessage ? (
          <p className="rounded-2xl border border-rose-400/30 bg-rose-950/20 px-4 py-3 text-sm text-rose-200">
            {errorMessage}
          </p>
        ) : null}

        <Button className="w-full" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Signing in..." : "Sign in"}
        </Button>
      </form>

      {enableMockAuth ? (
        <Button className="w-full" type="button" variant="outline" onClick={handleMockLogin}>
          Continue with DEV MOCK
        </Button>
      ) : (
        <p className="rounded-2xl border border-slate-700/60 bg-slate-900/50 px-4 py-3 text-sm text-slate-400">
          Dev mock login is disabled for this environment.
        </p>
      )}
    </div>
  );
}
