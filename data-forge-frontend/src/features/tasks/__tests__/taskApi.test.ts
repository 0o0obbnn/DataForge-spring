import { beforeEach, describe, expect, it, vi } from "vitest";

import { getTask, listRecentTasks } from "@/features/tasks/taskApi";

describe("taskApi", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("lists recent tasks with pagination", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "ok", data: [] }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await listRecentTasks(1, 20);

    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/dataforge/tasks?page=1&size=20");
  });

  it("gets task detail by id", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "ok", data: { id: 42, status: "COMPLETED", recordCount: 10 } }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const task = await getTask(42);

    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/dataforge/tasks/42");
    expect(task.id).toBe(42);
  });
});
