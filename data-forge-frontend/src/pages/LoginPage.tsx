import { LoginForm } from "@/features/auth/LoginForm";
import { env } from "@/shared/config/env";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/components/ui/card";

export function LoginPage() {
  return (
    <main className="grid min-h-screen grid-cols-[1.1fr_0.9fr] overflow-hidden bg-[#050816] text-slate-50">
      <section className="relative flex flex-col justify-between border-r border-cyan-300/10 p-12">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(34,211,238,0.18),transparent_32rem),radial-gradient(circle_at_80%_10%,rgba(139,92,246,0.18),transparent_28rem)]" />
        <div className="relative">
          <p className="text-sm uppercase tracking-[0.36em] text-cyan-200">DataForge Console</p>
          <h1 className="mt-8 max-w-3xl text-6xl font-semibold tracking-tight">
            Generate structured test data from a cockpit built for operators.
          </h1>
        </div>
        <p className="relative max-w-xl text-lg leading-8 text-slate-300">
          Authenticate, configure generators, launch jobs, monitor async tasks, and reuse templates from one
          high-density developer console.
        </p>
      </section>

      <section className="flex items-center justify-center p-10">
        <Card className="w-full max-w-md border-slate-700/60 bg-slate-950/70 text-slate-50 shadow-2xl shadow-cyan-950/20 backdrop-blur-xl">
          <CardHeader>
            <CardTitle className="text-2xl">Sign in</CardTitle>
            <CardDescription className="text-slate-400">
              Real JWT login arrives with the auth API wiring.
              {env.enableMockAuth ? " Use dev mock mode to enter the console now." : null}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <LoginForm enableMockAuth={env.enableMockAuth} />
          </CardContent>
        </Card>
      </section>
    </main>
  );
}
