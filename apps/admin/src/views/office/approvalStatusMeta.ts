// 审批状态文案 / 颜色集中映射，供 office 审批中心与审批详情复用。
// office 审批状态：PENDING / APPROVED / REJECTED / WITHDRAWN
// CRM 审批状态：PENDING_APPROVAL / APPROVED / REJECTED

export type ApprovalStatusMetaKey =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "WITHDRAWN"
  | "PENDING_APPROVAL";

const APPROVAL_STATUS_LABEL: Record<ApprovalStatusMetaKey, string> = {
  PENDING: "待审批",
  PENDING_APPROVAL: "待审批",
  APPROVED: "已通过",
  REJECTED: "已驳回",
  WITHDRAWN: "已撤回",
};

const APPROVAL_STATUS_COLOR: Record<ApprovalStatusMetaKey, string> = {
  PENDING: "orange",
  PENDING_APPROVAL: "orange",
  APPROVED: "green",
  REJECTED: "red",
  WITHDRAWN: "gray",
};

export function approvalStatusLabel(status: string): string {
  return APPROVAL_STATUS_LABEL[status as ApprovalStatusMetaKey] ?? status;
}

export function approvalStatusColor(status: string): string {
  return APPROVAL_STATUS_COLOR[status as ApprovalStatusMetaKey] ?? "blue";
}
