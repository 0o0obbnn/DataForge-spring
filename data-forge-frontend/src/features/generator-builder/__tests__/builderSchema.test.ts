import { describe, expect, it } from "vitest";

import { generateRequestSchema } from "@/features/generator-builder/builderSchema";

describe("generateRequestSchema", () => {
  it("accepts a valid generation request", () => {
    const result = generateRequestSchema.safeParse({
      count: 100,
      threads: 2,
      validate: true,
      output: {
        format: "JSON",
        encoding: "UTF-8",
      },
      fields: [{ name: "email", type: "email", params: { domain: "example.com" } }],
    });

    expect(result.success).toBe(true);
  });

  it("rejects duplicate field names", () => {
    const result = generateRequestSchema.safeParse({
      count: 100,
      validate: true,
      output: {
        format: "CSV",
        encoding: "UTF-8",
      },
      fields: [
        { name: "email", type: "email" },
        { name: "email", type: "string" },
      ],
    });

    expect(result.success).toBe(false);
    expect(result.error?.issues[0]?.message).toBe("Field names must be unique");
  });

  it("rejects count lower than one", () => {
    const result = generateRequestSchema.safeParse({
      count: 0,
      validate: true,
      output: {
        format: "CONSOLE",
        encoding: "UTF-8",
      },
      fields: [{ name: "id", type: "uuid" }],
    });

    expect(result.success).toBe(false);
  });
});
