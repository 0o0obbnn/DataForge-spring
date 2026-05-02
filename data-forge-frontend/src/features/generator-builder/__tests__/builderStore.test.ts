import { beforeEach, describe, expect, it } from "vitest";

import { useBuilderStore } from "@/features/generator-builder/builderStore";

describe("builderStore", () => {
  beforeEach(() => {
    useBuilderStore.getState().resetDraft();
  });

  it("adds a generator field with a unique default name", () => {
    useBuilderStore.getState().addField("email");
    useBuilderStore.getState().addField("email");

    expect(useBuilderStore.getState().draft.fields).toMatchObject([
      { name: "email", type: "email" },
      { name: "email_2", type: "email" },
    ]);
  });
});
