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
        json: ok({
          content: [],
          totalElements: 0,
          totalPages: 0,
          number: 0,
          size: 20,
        }),
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

test("login reaches the requested work area and applies command permissions", async ({
  page,
}) => {
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

test("MFA challenge only creates a session after the second verification step", async ({
  page,
}) => {
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
  expect(
    await page.evaluate(() => sessionStorage.getItem("ops_erp_admin_token")),
  ).toBeNull();

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
  await page.addInitScript(() =>
    sessionStorage.setItem("ops_erp_admin_token", "e2e-token"),
  );
  await page.goto("/finance/overview");

  await expect(page).toHaveURL(/\/profile$/);
  await expect(
    page.getByText(user.displayName, { exact: true }).first(),
  ).toBeVisible();
});

test("finance analytics switch organization and salesperson contribution scopes", async ({
  page,
}) => {
  await page.unroute("**/api/**");
  const financeUser = {
    ...user,
    username: "finance",
    displayName: "财务用户",
    roleCodes: ["FINANCE"],
    permissions: ["finance:view"],
  };
  const analyticsRequests: string[] = [];
  const contributionRequests: string[] = [];
  await page.route("**/api/**", async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith("/auth/me")) {
      await route.fulfill({ json: ok(financeUser) });
      return;
    }
    if (url.pathname.endsWith("/office/notifications/count")) {
      await route.fulfill({ json: ok(0) });
      return;
    }
    if (url.pathname.endsWith("/finance/organizations")) {
      await route.fulfill({
        json: ok([
          {
            id: "10000000-0000-0000-0000-000000000001",
            name: "销售中心",
            type: "DEPARTMENT",
            fullPath: "总部 / 销售中心",
            children: [
              {
                id: "10000000-0000-0000-0000-000000000002",
                name: "华东销售部",
                type: "DEPARTMENT",
                fullPath: "总部 / 销售中心 / 华东销售部",
                children: [],
              },
            ],
          },
        ]),
      });
      return;
    }
    if (url.pathname.endsWith("/finance/contribution/salespeople")) {
      await route.fulfill({
        json: ok([
          {
            id: "20000000-0000-0000-0000-000000000001",
            displayName: "销售甲",
            organizationId: "10000000-0000-0000-0000-000000000002",
            organizationName: "华东销售部",
            organizationPath: "总部 / 销售中心 / 华东销售部",
            enabled: true,
          },
        ]),
      });
      return;
    }
    if (url.pathname.endsWith("/finance/contribution/analytics")) {
      contributionRequests.push(url.search);
      const userMode = url.searchParams.get("subjectType") === "USER";
      await route.fulfill({
        json: ok({
          asOf: "2026-08-05",
          fiscalYear: 2026,
          scope: {
            subjectType: userMode ? "USER" : "ORGANIZATION",
            subjectId: userMode ? "20000000-0000-0000-0000-000000000001" : null,
            subjectName: userMode ? "销售甲" : "全部销售归属",
            subjectPath: userMode
              ? "总部 / 销售中心 / 华东销售部 / 销售甲"
              : "按角色数据范围汇总",
            includeDescendants: !userMode,
            organizationCount: userMode ? 1 : 2,
            attributionBasis: "销售归属快照",
          },
          summary: {
            contractAmount: 1000000,
            actualCost: 620000,
            grossProfit: 380000,
            grossMarginRate: 38,
            receivedAmount: 700000,
            paidAmount: 400000,
            netCashFlow: 300000,
            receivableOutstanding: 300000,
            payableOutstanding: 100000,
            collectionRate: 70,
            projectCount: 1,
          },
          monthlyCashFlow: Array.from({ length: 12 }, (_, index) => ({
            month: index + 1,
            receipt: index === 7 ? 700000 : 0,
            payment: index === 7 ? 400000 : 0,
            netCash: index === 7 ? 300000 : 0,
          })),
          projects: [
            {
              projectId: "30000000-0000-0000-0000-000000000001",
              projectCode: "P-2026-001",
              projectName: "华东改造项目",
              customerName: "示例客户",
              stage: "CONSTRUCTION",
              salesOwnerName: "销售甲",
              contractAmount: 1000000,
              actualCost: 620000,
              grossProfit: 380000,
              grossMarginRate: 38,
              receivedAmount: 700000,
              paidAmount: 400000,
              netCashFlow: 300000,
              receivableOutstanding: 300000,
              payableOutstanding: 100000,
            },
          ],
          dataQuality: {
            unattributedProjectCount: 0,
            unattributedReceivableCount: 0,
            unlinkedReceivableCount: 0,
            note: "利润按项目实际成本，现金按实际收付款归集",
          },
        }),
      });
      return;
    }
    if (url.pathname.endsWith("/finance/analytics")) {
      analyticsRequests.push(url.search);
      const organizationId = url.searchParams.get("organizationId");
      const organizationName = organizationId ? "华东销售部" : "全部授权组织";
      await route.fulfill({
        json: ok({
          asOf: "2026-08-05",
          fiscalYear: 2026,
          scope: {
            organizationId,
            organizationName,
            organizationPath: organizationId
              ? "总部 / 销售中心 / 华东销售部"
              : "按角色数据范围汇总",
            includeDescendants:
              url.searchParams.get("includeDescendants") !== "false",
            organizationCount: organizationId ? 1 : 2,
            unrestricted: false,
            unallocatedExcluded: true,
          },
          monthlyCashFlow: [],
          forecast: [],
          aging: [],
          reconciliation: {
            ledger: [],
            bankLineCount: 0,
            matchedBankLines: 0,
            suggestedBankLines: 0,
            unmatchedBankLines: 0,
            unmatchedBankAmount: 0,
          },
          tax: {
            outputGross: 0,
            outputNet: 0,
            outputTax: 0,
            inputGross: 0,
            inputNet: 0,
            inputTax: 0,
            netTaxPayable: 0,
            pendingOutputInvoices: 0,
            inputInvoiceExceptions: 0,
            adjustedInvoices: 0,
          },
          cashPlan: {
            baseline: 0,
            committed: 0,
            actual: 0,
            forecast: 0,
            variance: 0,
            activePlans: 0,
          },
          risks: [],
        }),
      });
      return;
    }
    await route.fulfill({ json: ok([]) });
  });
  await page.addInitScript(() =>
    sessionStorage.setItem("ops_erp_admin_token", "finance-e2e-token"),
  );

  await page.goto("/finance/overview");
  await expect(
    page.getByRole("heading", { name: "财务控制台", level: 2 }),
  ).toBeVisible();
  await page.locator(".organization-select .ant-select-selector").click();
  await page.getByText("华东销售部", { exact: true }).last().click();

  await expect(
    page.getByText("总部 / 销售中心 / 华东销售部", { exact: true }),
  ).toBeVisible();
  await expect
    .poll(() => analyticsRequests.at(-1))
    .toContain("organizationId=10000000-0000-0000-0000-000000000002");

  await page.getByRole("switch").click();
  await expect
    .poll(() => analyticsRequests.at(-1))
    .toContain("includeDescendants=false");

  await page.goto("/finance/contribution");
  await expect(
    page.getByRole("heading", { name: "经营贡献分析", level: 2 }),
  ).toBeVisible();
  await expect(page.getByText("华东改造项目", { exact: true })).toBeVisible();
  await page.getByText("销售人员", { exact: true }).click();
  await page.locator(".salesperson-select .ant-select-selector").click();
  await page
    .getByText(/销售甲 · 华东销售部/)
    .last()
    .click();

  await expect(
    page.getByText("总部 / 销售中心 / 华东销售部 / 销售甲", {
      exact: true,
    }),
  ).toBeVisible();
  await expect
    .poll(() => contributionRequests.at(-1))
    .toContain("subjectId=20000000-0000-0000-0000-000000000001");
});

test("finance operations lock a reconciled tax period with an evidence reference", async ({
  page,
  isMobile,
}) => {
  await page.unroute("**/api/**");
  const financeUser = {
    ...user,
    username: "finance-manager",
    displayName: "财务主管",
    roleCodes: ["FINANCE_MANAGER"],
    permissions: ["finance:operations:view", "finance:operations:manage"],
  };
  let locked = false;
  let lockPayload: unknown;
  await page.route("**/api/**", async (route) => {
    const url = new URL(route.request().url());
    const method = route.request().method();
    if (url.pathname.endsWith("/auth/me")) {
      await route.fulfill({ json: ok(financeUser) });
      return;
    }
    if (url.pathname.endsWith("/office/notifications/count")) {
      await route.fulfill({ json: ok(0) });
      return;
    }
    if (url.pathname.endsWith("/finance/operations/overview")) {
      await route.fulfill({
        json: ok({
          pendingPeriodJobs: 0,
          failedVoucherRequests: 0,
          unreconciledPartners: 0,
          unlockedTaxPeriods: locked ? 0 : 1,
          draftConsolidations: 0,
          snapshots: locked ? 1 : 0,
          budgetVariance: 0,
          forecastLiquidity: 500000,
        }),
      });
      return;
    }
    if (url.pathname.endsWith("/finance/operations/tax-filings/2026/8/lock") && method === "POST") {
      lockPayload = route.request().postDataJSON();
      locked = true;
      await route.fulfill({ json: ok({ status: "LOCKED" }) });
      return;
    }
    if (url.pathname.endsWith("/finance/operations/tax-filings")) {
      await route.fulfill({
        json: ok([
          {
            id: "40000000-0000-0000-0000-000000000001",
            fiscalYear: 2026,
            periodNo: 8,
            outputTax: 13000,
            inputTax: 5000,
            taxPayable: 8000,
            ledgerTax: 8000,
            difference: 0,
            status: locked ? "LOCKED" : "RECONCILED",
            filingReference: locked ? "TAX-2026-08" : null,
          },
        ]),
      });
      return;
    }
    await route.fulfill({ json: ok([]) });
  });
  await page.addInitScript(() =>
    sessionStorage.setItem("ops_erp_admin_token", "finance-operations-token"),
  );

  await page.goto("/finance/operations");
  await expect(
    page.getByRole("heading", { name: "财务运营工作台", level: 2 }),
  ).toBeVisible();
  const taxTab = page.getByRole("tab", { name: "税务申报" });
  const taxControl = page.getByRole("button", { name: /未锁税务期间/ });
  if (isMobile) await taxControl.tap();
  else await taxControl.click();
  await expect(taxTab).toHaveAttribute("aria-selected", "true");
  await expect(page.getByText("2026-08", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "锁定申报" }).click();
  await page.getByPlaceholder("请输入税务申报回执编号").fill("TAX-2026-08");
  await page.locator(".ant-modal").getByRole("button", { name: "确 定" }).click();

  await expect(page.getByText("申报已锁定并固化快照")).toBeVisible();
  await expect.poll(() => lockPayload).toEqual({ filingReference: "TAX-2026-08" });
  await expect(page.getByRole("button", { name: "锁定申报" })).toHaveCount(0);
  await expect(page.getByText("TAX-2026-08", { exact: true })).toBeVisible();
});
