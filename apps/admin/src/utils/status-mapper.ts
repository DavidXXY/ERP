/**
 * Type-safe status label/color mapper utility.
 * Replaces inline `{KEY:val}[value]||default` patterns that cause TS7053.
 *
 * Usage in templates:
 *   <a-tag :color="statusColor(record.status, { DRAFT:'default', ... })">
 *     {{ statusLabel(record.status, { DRAFT:'草稿', ... }) }}
 *   </a-tag>
 */

export function statusLabel(
  value: string | undefined | null,
  labels: Record<string, string>,
  defaultLabel?: string,
): string {
  if (value == null) return defaultLabel ?? "";
  return labels[value] ?? defaultLabel ?? value;
}

export function statusColor(
  value: string | undefined | null,
  colors: Record<string, string>,
  defaultColor?: string,
): string {
  if (value == null) return defaultColor ?? "default";
  return colors[value] ?? defaultColor ?? "default";
}

const BUSINESS_STATUS_LABELS: Record<string, string> = {
  MATCHED: "已匹配",
  MISMATCH: "存在差异",
  INVOICE_PENDING: "待收票",
  RECEIPT_PENDING: "待到货",
  PAYMENT_PENDING: "待付款",
  PAYABLE_PENDING: "待生成应付",
  PARTIAL: "部分完成",
  PENDING: "待处理",
  PROCESSING: "处理中",
  COMPLETED: "已完成",
  APPROVED: "已通过",
  REJECTED: "已驳回",
  CANCELLED: "已取消",
};

export function businessStatusLabel(value: string | undefined | null): string {
  return statusLabel(value, BUSINESS_STATUS_LABELS, value ? "状态待配置" : "-");
}
