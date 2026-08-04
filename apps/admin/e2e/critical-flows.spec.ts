import { expect, test, type Page, type Route } from "@playwright/test";

const user = {
  id: "00000000-0000-0000-0000-000000000001",
  username: "auditor",
  displayName: "审计用户",
  roleCodes: ["AUDITOR"],
  permissions: ["system:user:view", "system:role:view"],
};

function ok(data: unknown) {
  return { success: true, message: "ok", data };
}

async function mockApi(page: Page) {
  await page.route("**/api/**", async (route: Route) => {
    const url = new URL(route.request().url());
    if (!url.pathname.startsWith("/api/")) {
      await route.continue();
      return;
    }
    if (url.pathname.endsWith("/auth/login")) {
      await route.fulfill({ json: ok({ token: "e2e-token", user }) });
      return;
    }
    if (url.pathname.endsWith("/auth/me")) {
      await route.fulfill({ json: ok(user) });
      return;
    }
    if (url.pathname.endsWith("/users") || url.pathname.endsWith("/roles")) {
      await route.fulfill({
        json: ok({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }),
      });
      return;
    }
    if (url.pathname.endsWith("/office/notifications/count")) {
      await route.fulfill({ json: ok(0) });
      return;
    }
    if (url.pathname.endsWith("/personal")) {
      await route.fulfill({
        json: ok({
          account: {
            id: user.id,
            username: user.username,
            displayName: user.displayName,
            enabled: true,
          },
          certificates: [],
          contracts: [],
        }),
      });
      return;
    }
    await route.fulfill({ json: ok([]) });
  });
}

test.beforeEach(async ({ page }) => {
  await mockApi(page);
});

test("login reaches the requested work area and applies command permissions", async ({ page }) => {
  await page.goto("/login?redirect=/system/users");
  await page.getByLabel("账号").fill("auditor");
  await page.getByLabel("密码").fill("correct-password");
  await page.getByRole("button", { name: "登录系统" }).click();

  await expect(page).toHaveURL(/\/system\/users$/);
  await expect(
    page.getByRole("heading", { name: "账号管理", level: 2 }),
  ).toBeVisible();
  await expect(page.getByRole("button", { name: /新增账号/ })).toHaveCount(0);
  await expect(page.getByText("财务中心", { exact: true })).toHaveCount(0);
});

test("MFA challenge only creates a session after the second verification step", async ({ page }) => {
  const loginPayloads: Array<Record<string, unknown>> = [];
  await page.route("**/api/auth/login", async (route) => {
    const payload = route.request().postDataJSON() as Record<string, unknown>;
    loginPayloads.push(payload);
    if (!payload.mfaCode) {
      await route.fulfill({ json: ok({ mfaRequired: true }) });
      return;
    }
    await route.fulfill({ json: ok({ token: "mfa-e2e-token", user }) });
  });

  await page.goto("/login?redirect=/system/users");
  await page.getByLabel("账号").fill("auditor");
  await page.getByLabel("密码").fill("correct-password");
  await page.getByRole("button", { name: "登录系统" }).click();

  await expect(page.getByLabel("动态验证码或恢复码")).toBeVisible();
  await expect(page.getByRole("button", { name: "验证并登录" })).toBeVisible();
  expect(await page.evaluate(() => sessionStorage.getItem("ops_erp_admin_token"))).toBeNull();

  await page.getByLabel("动态验证码或恢复码").fill("123456");
  await page.getByRole("button", { name: "验证并登录" }).click();

  await expect(page).toHaveURL(/\/system\/users$/);
  expect(loginPayloads).toEqual([
    { username: "auditor", password: "correct-password" },
    { username: "auditor", password: "correct-password", mfaCode: "123456" },
  ]);
  expect(
    await page.evaluate(() => sessionStorage.getItem("ops_erp_admin_token")),
  ).toBe("mfa-e2e-token");
});

test("route guard redirects an unauthorized deep link", async ({ page }) => {
  await page.addInitScript(() => sessionStorage.setItem("ops_erp_admin_token", "e2e-token"));
  await page.goto("/finance/overview");

  await expect(page).toHaveURL(/\/profile$/);
  await expect(page.getByText(user.displayName, { exact: true }).first()).toBeVisible();
});
