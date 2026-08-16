// 审批中心共享的纯格式化 / 文案映射函数，供容器、详情抽屉与各弹窗复用。
import { approvalStatusLabel } from "./approvalStatusMeta";
import type {
  ApprovalStatus,
  ApprovalType,
  ExpenseStatus,
  ExpenseType,
} from "@/api/office";

export function formatMoney(v: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2,
  }).format(v || 0);
}

export function approvalTypeLabel(v: ApprovalType) {
  return (
    {
      QUOTE: "报价",
      CONTRACT: "合同",
      PURCHASE: "采购",
      OUTSOURCE: "外包",
      EXPENSE: "报销",
      PAYMENT: "付款",
      SEAL: "用章",
      LEAVE: "请假",
      TRAVEL: "出差",
      OTHER: "其他",
    } as Record<ApprovalType, string>
  )[v];
}

export function expenseTypeLabel(v: ExpenseType) {
  return (
    {
      TRAVEL: "差旅",
      TRANSPORT: "交通",
      ACCOMMODATION: "住宿",
      TOOL: "工具采购",
      OTHER: "其他",
    } as Record<ExpenseType, string>
  )[v];
}

export function expenseStatusLabel(v: ExpenseStatus) {
  return (
    ({ PAID: "已付款" } as Record<string, string>)[v] || approvalStatusLabel(v)
  );
}

export function approvalBusinessTypeLabel(value?: string) {
  if (!value) return "-";
  return (
    (
      {
        TRAVEL: "差旅",
        TRANSPORT: "交通",
        ACCOMMODATION: "住宿",
        TOOL: "工具采购",
        OTHER: "其他",
      } as Record<string, string>
    )[value] || value
  );
}

export function approvalActionColor(v: ApprovalStatus) {
  return v === "APPROVED" ? "green" : v === "REJECTED" ? "red" : "blue";
}

export function nodeStatusLabel(v: string) {
  return (
    (
      {
        PENDING: "待处理",
        SKIPPED: "已跳过",
      } as Record<string, string>
    )[v] || approvalStatusLabel(v)
  );
}

export function nodeColor(v: string) {
  return (
    (
      {
        PENDING: "blue",
        APPROVED: "green",
        REJECTED: "red",
        SKIPPED: "gray",
      } as Record<string, string>
    )[v] || "blue"
  );
}
