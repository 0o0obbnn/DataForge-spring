import { expect, test } from "@playwright/test";

test("console smoke (mock auth): login → dashboard → catalog → builder", async ({ page }) => {
  const consoleErrors: string[] = [];

  page.on("console", (message) => {
    if (message.type() === "error") {
      const location = message.location();
      consoleErrors.push(`${message.text()} @ ${location.url}:${location.lineNumber}:${location.columnNumber}`);
    }
  });

  page.on("pageerror", (error) => {
    throw error;
  });

  await page.route(/\/api\/v1\//, async (route) => {
    const url = route.request().url();

    if (url.includes("/api/v1/auth/login")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          code: 0,
          message: "ok",
          data: {
            accessToken: "e2e-access-token",
            refreshToken: "e2e-refresh-token",
            tokenType: "Bearer",
            username: "e2e-operator",
            expiresIn: 3600,
          },
        }),
      });
      return;
    }

    if (url.includes("/api/v1/dataforge/tasks")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ code: 0, message: "ok", data: [] }),
      });
      return;
    }

    if (url.includes("/api/v1/templates")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ code: 0, message: "ok", data: [] }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ code: 0, message: "ok", data: null }),
    });
  });

  const response = await page.goto("/");
  const status = response?.status();
  const ok = response?.ok();

  if (!ok) {
    const bodySnippet = (await response?.text())?.slice(0, 400);
    throw new Error(`Failed to load app. status=${status ?? "unknown"} body=${bodySnippet ?? ""}`);
  }

  try {
    await expect(page.locator("#username")).toBeVisible({ timeout: 15_000 });
  } catch {
    const htmlSnippet = (await page.content()).slice(0, 800);
    throw new Error(`Login inputs did not render. consoleErrors=${consoleErrors.join(" | ")} html=${htmlSnippet}`);
  }

  await page.locator("#username").fill("admin");
  await page.locator("#password").fill("password");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page
    .getByRole("navigation", { name: "Primary navigation" })
    .getByRole("link", { name: "Catalog" })
    .click();
  await expect(page).toHaveURL(/\/catalog$/);

  await page.getByLabel("Search generators").fill("email");
  await page.getByRole("button", { name: "View Details" }).first().click();

  await page.getByRole("button", { name: "Add to Builder" }).click();
  await expect(page).toHaveURL(/\/builder$/);
  await expect(page.getByText("Generator Builder")).toBeVisible();

  const firstFieldNameInput = page.locator("label", { hasText: "Field Name" }).locator("input").first();
  await expect(firstFieldNameInput).toHaveValue("email");
});

