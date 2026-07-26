export const statusLabels: Record<string, string> = {
  CREATED: "待派单", ASSIGNED: "待接单", IN_PROGRESS: "进行中", COMPLETED: "待验收", ACCEPTED: "已完成", CANCELLED: "已取消",
  PENDING: "待审批", APPROVED: "已通过", REJECTED: "已驳回", DRAFT: "草稿",
  PENDING_APPROVAL: "待审批", PAID: "已支付",
};

export const approvalTypeLabels: Record<string, string> = {
  QUOTE: "报价", CONTRACT: "合同", PURCHASE: "采购", OUTSOURCE: "外包", EXPENSE: "费用",
  PAYMENT: "付款", SEAL: "用印", LEAVE: "请假", TRAVEL: "出差", OTHER: "通用",
};

export function dateText(value?: string) {
  if (!value) return "-";
  return value.replace("T", " ").replace(/\+.*$/, "").slice(0, 16);
}

export function shortDate(value?: string) {
  return value ? value.slice(0, 10) : "未安排";
}

export function money(value?: number) {
  return value == null ? "-" : `¥${Number(value).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function statusClass(status: string) {
  if (["APPROVED", "ACCEPTED", "COMPLETED"].includes(status)) return "status-success";
  if (["REJECTED", "CANCELLED"].includes(status)) return "status-danger";
  return "status-pending";
}
