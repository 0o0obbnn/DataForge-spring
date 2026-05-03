import { describe, expect, it } from "vitest";

import { shouldPollTasks } from "@/features/tasks/taskQueries";
import { GenerationTask } from "@/shared/types/dataforge";

describe("shouldPollTasks", () => {
  it("polls while any task is in progress", () => {
    const tasks: GenerationTask[] = [{ id: 1, status: "IN_PROGRESS", recordCount: 100 }];

    expect(shouldPollTasks(tasks)).toBe(true);
  });

  it("does not poll when all tasks are finished", () => {
    const tasks: GenerationTask[] = [
      { id: 1, status: "COMPLETED", recordCount: 100 },
      { id: 2, status: "FAILED", recordCount: 20 },
    ];

    expect(shouldPollTasks(tasks)).toBe(false);
  });

  it("does not poll without task data", () => {
    expect(shouldPollTasks(undefined)).toBe(false);
  });
});
