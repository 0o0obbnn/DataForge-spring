import { describe, expect, it } from "vitest";

import { deriveDashboardStats } from "@/features/dashboard/dashboardQueries";
import { GenerationTask } from "@/shared/types/dataforge";

describe("deriveDashboardStats", () => {
  it("summarizes recent task statuses and average completed duration", () => {
    const tasks: GenerationTask[] = [
      { id: 1, status: "COMPLETED", recordCount: 100, durationMs: 1000 },
      { id: 2, status: "COMPLETED", recordCount: 300, durationMs: 3000 },
      { id: 3, status: "FAILED", recordCount: 50 },
      { id: 4, status: "IN_PROGRESS", recordCount: 25 },
    ];

    expect(deriveDashboardStats(tasks)).toEqual({
      total: 4,
      completed: 2,
      failed: 1,
      running: 1,
      averageDurationMs: 2000,
      throughputRecords: 400,
    });
  });

  it("uses zero duration when no completed tasks have durations", () => {
    expect(deriveDashboardStats([{ id: 1, status: "IN_PROGRESS", recordCount: 10 }])).toMatchObject({
      total: 1,
      averageDurationMs: 0,
      throughputRecords: 0,
    });
  });
});
