import { beforeEach, describe, expect, it, vi } from "vitest";

import { generateAsync, generateSync } from "@/features/generator-builder/generationApi";
import { GenerateRequest } from "@/shared/types/dataforge";

const request: GenerateRequest = {
  count: 10,
  validate: true,
  output: {
    format: "JSON",
    encoding: "UTF-8",
  },
  fields: [{ name: "email", type: "email" }],
};

describe("generationApi", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("posts sync generation requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "ok", data: { message: "done", outputPath: "/tmp/out.json" } }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await generateSync(request);
    const [path, options] = fetchMock.mock.calls[0];

    expect(path).toBe("/api/v1/dataforge/generate");
    expect(options.method).toBe("POST");
    expect(JSON.parse(options.body as string)).toEqual(request);
    expect(result.outputPath).toBe("/tmp/out.json");
  });

  it("posts async generation requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "ok", data: 42 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const taskId = await generateAsync(request);
    const [path] = fetchMock.mock.calls[0];

    expect(path).toBe("/api/v1/dataforge/generate/async");
    expect(taskId).toBe(42);
  });
});
