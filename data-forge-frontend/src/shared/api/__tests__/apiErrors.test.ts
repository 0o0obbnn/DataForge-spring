import { describe, expect, it } from "vitest";

import { ApiClientError, normalizeApiError } from "@/shared/api/apiErrors";

describe("normalizeApiError", () => {
  it("uses backend message and code when present", () => {
    const error = normalizeApiError({ code: 400, message: "Invalid config" }, 422);

    expect(error).toBeInstanceOf(ApiClientError);
    expect(error.message).toBe("Invalid config");
    expect(error.status).toBe(400);
  });

  it("uses HTTP status when backend code is missing", () => {
    const error = normalizeApiError({ message: "Rate limited" }, 429);

    expect(error.message).toBe("Rate limited");
    expect(error.status).toBe(429);
  });

  it("falls back to a stable message", () => {
    const error = normalizeApiError(undefined, 500);

    expect(error.message).toBe("Request failed");
    expect(error.status).toBe(500);
  });
});
