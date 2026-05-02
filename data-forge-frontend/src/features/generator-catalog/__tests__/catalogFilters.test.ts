import { describe, expect, it } from "vitest";

import { filterGenerators } from "@/features/generator-catalog/catalogFilters";
import { GeneratorDefinition } from "@/features/generator-catalog/generatorCatalogTypes";

const generators: GeneratorDefinition[] = [
  {
    id: "uuid",
    name: "UUID",
    category: "Identity",
    summary: "Unique identifiers",
    sample: "550e8400-e29b-41d4-a716-446655440000",
    params: [],
  },
  {
    id: "email",
    name: "Email",
    category: "Internet",
    summary: "Email addresses",
    sample: "user@example.com",
    params: [],
  },
];

describe("filterGenerators", () => {
  it("filters by text", () => {
    expect(filterGenerators(generators, { query: "email", category: "All" })).toHaveLength(1);
  });

  it("filters by category", () => {
    expect(filterGenerators(generators, { query: "", category: "Identity" })[0].id).toBe("uuid");
  });

  it("matches query against sample output", () => {
    expect(filterGenerators(generators, { query: "example.com", category: "All" })[0].id).toBe("email");
  });
});
