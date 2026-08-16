import { describe, expect, it } from "vitest";
import {
  approvalActionColor,
  approvalBusinessTypeLabel,
  approvalTypeLabel,
  expenseStatusLabel,
  expenseTypeLabel,
  formatMoney,
  nodeColor,
  nodeStatusLabel,
} from "./approvalFormat";
import { approvalStatusColor, approvalStatusLabel } from "./approvalStatusMeta";

describe("approvalFormat", () => {
  it("formats money in CNY", () => {
    expect(formatMoney(1234.5)).toContain("1,234.50");
    expect(formatMoney(0)).toContain("0.00");
  });

  it("maps approval types to labels", () => {
    expect(approvalTypeLabel("QUOTE")).toBe("报价");
    expect(approvalTypeLabel("CONTRACT")).toBe("合同");
    expect(approvalTypeLabel("PURCHASE")).toBe("采购");
    expect(approvalTypeLabel("OUTSOURCE")).toBe("外包");
    expect(approvalTypeLabel("EXPENSE")).toBe("报销");
    expect(approvalTypeLabel("PAYMENT")).toBe("付款");
    expect(approvalTypeLabel("SEAL")).toBe("用章");
    expect(approvalTypeLabel("LEAVE")).toBe("请假");
    expect(approvalTypeLabel("TRAVEL")).toBe("出差");
    expect(approvalTypeLabel("OTHER")).toBe("其他");
  });

  it("maps expense types to labels", () => {
    expect(expenseTypeLabel("TRAVEL")).toBe("差旅");
    expect(expenseTypeLabel("TRANSPORT")).toBe("交通");
    expect(expenseTypeLabel("ACCOMMODATION")).toBe("住宿");
    expect(expenseTypeLabel("TOOL")).toBe("工具采购");
    expect(expenseTypeLabel("OTHER")).toBe("其他");
  });

  it("maps expense status with paid fallback", () => {
    expect(expenseStatusLabel("PAID")).toBe("已付款");
    expect(expenseStatusLabel("PENDING_APPROVAL")).toBe("待审批");
  });

  it("maps business types with fallback", () => {
    expect(approvalBusinessTypeLabel("TRAVEL")).toBe("差旅");
    expect(approvalBusinessTypeLabel(undefined)).toBe("-");
    expect(approvalBusinessTypeLabel("UNKNOWN")).toBe("UNKNOWN");
  });

  it("maps action colors", () => {
    expect(approvalActionColor("APPROVED")).toBe("green");
    expect(approvalActionColor("REJECTED")).toBe("red");
    expect(approvalActionColor("PENDING")).toBe("blue");
  });

  it("maps node status labels", () => {
    expect(nodeStatusLabel("PENDING")).toBe("待处理");
    expect(nodeStatusLabel("SKIPPED")).toBe("已跳过");
    expect(nodeStatusLabel("APPROVED")).toBe("已通过");
  });

  it("maps node colors", () => {
    expect(nodeColor("PENDING")).toBe("blue");
    expect(nodeColor("APPROVED")).toBe("green");
    expect(nodeColor("REJECTED")).toBe("red");
    expect(nodeColor("SKIPPED")).toBe("gray");
    expect(nodeColor("UNKNOWN")).toBe("blue");
  });
});

describe("approvalStatusMeta", () => {
  it("maps status labels", () => {
    expect(approvalStatusLabel("PENDING")).toBe("待审批");
    expect(approvalStatusLabel("PENDING_APPROVAL")).toBe("待审批");
    expect(approvalStatusLabel("APPROVED")).toBe("已通过");
    expect(approvalStatusLabel("REJECTED")).toBe("已驳回");
    expect(approvalStatusLabel("WITHDRAWN")).toBe("已撤回");
    expect(approvalStatusLabel("UNKNOWN")).toBe("UNKNOWN");
  });

  it("maps status colors", () => {
    expect(approvalStatusColor("PENDING")).toBe("orange");
    expect(approvalStatusColor("PENDING_APPROVAL")).toBe("orange");
    expect(approvalStatusColor("APPROVED")).toBe("green");
    expect(approvalStatusColor("REJECTED")).toBe("red");
    expect(approvalStatusColor("WITHDRAWN")).toBe("gray");
    expect(approvalStatusColor("UNKNOWN")).toBe("blue");
  });
});
