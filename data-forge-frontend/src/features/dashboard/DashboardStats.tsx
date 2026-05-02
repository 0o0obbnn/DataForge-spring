import { Activity, CheckCircle2, Gauge, ListChecks, Timer, XCircle } from "lucide-react";

import { DashboardStats as DashboardStatsValue } from "@/features/dashboard/dashboardQueries";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { formatDuration, formatNumber } from "@/shared/lib/format";

interface DashboardStatsProps {
  stats: DashboardStatsValue;
}

export function DashboardStats({ stats }: DashboardStatsProps) {
  const cards = [
    { label: "Recent Jobs", value: formatNumber(stats.total), icon: ListChecks, tone: "text-cyan-200" },
    { label: "Running", value: formatNumber(stats.running), icon: Activity, tone: "text-emerald-200" },
    { label: "Completed", value: formatNumber(stats.completed), icon: CheckCircle2, tone: "text-green-200" },
    { label: "Failed", value: formatNumber(stats.failed), icon: XCircle, tone: "text-rose-200" },
    { label: "Avg Duration", value: formatDuration(stats.averageDurationMs), icon: Timer, tone: "text-violet-200" },
    { label: "Throughput", value: formatNumber(stats.throughputRecords), icon: Gauge, tone: "text-amber-200" },
  ];

  return (
    <section className="grid gap-4 md:grid-cols-3 xl:grid-cols-6">
      {cards.map((card) => (
        <Card key={card.label} className="border-slate-700/50 bg-slate-900/50 text-slate-50">
          <CardHeader className="flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-slate-400">{card.label}</CardTitle>
            <card.icon className={`size-4 ${card.tone}`} aria-hidden="true" />
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-semibold">{card.value}</p>
          </CardContent>
        </Card>
      ))}
    </section>
  );
}
