import { describe, expect, it } from "vitest";

import { formatDateTime, formatDuration, formatNumber } from "@/shared/lib/format";

describe("format helpers", () => {
  it("formats missing values as dash", () => {
    expect(formatNumber()).toBe("-");
    expect(formatDuration()).toBe("-");
    expect(formatDateTime()).toBe("-");
  });

  it("formats milliseconds below one second", () => {
    expect(formatDuration(250)).toBe("250 ms");
  });

  it("formats seconds with one decimal", () => {
    expect(formatDuration(2500)).toBe("2.5 s");
  });

  it("formats numbers with locale separators", () => {
    expect(formatNumber(1200)).toBe("1,200");
  });
});
