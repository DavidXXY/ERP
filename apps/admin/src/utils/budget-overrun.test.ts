import { describe, expect, it } from "vitest";
import {
  budgetChangeRoute,
  getErrorMessage,
  isBudgetOverrunMessage,
} from "./budget-overrun";

describe("budget overrun guidance", () => {
  it.each([
    "项目预算不足：剩余可申请 100，本次申请 200",
    "登记后将超出项目预算",
    "成本超过项目预算，已被预算控制拦截",
    "Cost exceeds budget by 10%",
  ])("recognizes a budget overrun message: %s", (value) => {
    expect(isBudgetOverrunMessage(value)).toBe(true);
  });

  it("does not intercept unrelated errors", () => {
    expect(isBudgetOverrunMessage("关联项目不存在")).toBe(false);
  });

  it("builds a budget request deep link with the project selected", () => {
    expect(budgetChangeRoute("project-1")).toEqual({
      path: "/collaboration",
      query: {
        tab: "budget",
        action: "request-budget",
        projectId: "project-1",
      },
    });
  });

  it("normalizes thrown values", () => {
    expect(getErrorMessage(new Error("预算错误"))).toBe("预算错误");
    expect(getErrorMessage("接口错误")).toBe("接口错误");
    expect(getErrorMessage(null, "提交失败")).toBe("提交失败");
  });
});
