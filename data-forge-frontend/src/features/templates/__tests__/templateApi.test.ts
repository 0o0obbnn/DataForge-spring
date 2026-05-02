import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  createTemplate,
  deleteTemplate,
  generateFromTemplate,
  listTemplates,
  updateTemplate,
} from "@/features/templates/templateApi";
import { DataTemplate, GenerateRequest } from "@/shared/types/dataforge";

const template: DataTemplate = {
  id: 1,
  name: "User records",
  description: "Basic user dataset",
  config: "count: 10",
  active: true,
};

const generateRequest: GenerateRequest = {
  count: 10,
  validate: true,
  output: { format: "JSON", encoding: "UTF-8" },
  fields: [{ name: "email", type: "email" }],
};

describe("templateApi", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("lists templates", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "ok", data: [template] }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await listTemplates();

    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/templates");
    expect(result[0].name).toBe("User records");
  });

  it("creates templates", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 201, message: "created", data: template }), {
        status: 201,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await createTemplate(template);
    const [, options] = fetchMock.mock.calls[0];

    expect(options.method).toBe("POST");
    expect(JSON.parse(options.body as string)).toEqual(template);
  });

  it("updates templates", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "updated", data: template }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await updateTemplate(1, template);

    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/templates/1");
    expect(fetchMock.mock.calls[0][1].method).toBe("PUT");
  });

  it("deletes templates", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "deleted" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await deleteTemplate(1);

    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/templates/1");
    expect(fetchMock.mock.calls[0][1].method).toBe("DELETE");
  });

  it("generates from template", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 200, message: "submitted", data: 42 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const taskId = await generateFromTemplate(1, generateRequest);

    expect(fetchMock.mock.calls[0][0]).toBe("/api/v1/dataforge/generate/template/1");
    expect(taskId).toBe(42);
  });
});
