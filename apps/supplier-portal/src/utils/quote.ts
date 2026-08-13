export type QuoteStatusView = { color: string; text: string };

export const money = (v?: number | string | null, currency = "CNY") =>
  new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: currency || "CNY",
  }).format(Number(v || 0));

export const fileSize = (v: number) =>
  v > 1048576 ? `${(v / 1048576).toFixed(1)} MB` : `${Math.ceil(v / 1024)} KB`;

export const formatDate = (v?: string) =>
  v ? new Intl.DateTimeFormat("zh-CN").format(new Date(v)) : "";

export const formatTime = (v?: string) =>
  v
    ? new Intl.DateTimeFormat("zh-CN", { hour12: false }).format(new Date(v))
    : "";

const startOfToday = () => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
};

/** 按自然日计算剩余天数：今天为 0，昨天为 -1。 */
export const daysLeft = (deadline?: string) => {
  if (!deadline) return 999;
  return Math.round(
    (new Date(`${deadline}T00:00:00`).getTime() - startOfToday().getTime()) /
      86400000,
  );
};

export const deadlineText = (deadline?: string) => {
  const d = daysLeft(deadline);
  return !deadline
    ? "未设截止"
    : d < 0
      ? "已截止"
      : d === 0
        ? "今天截止"
        : `${d} 天后截止`;
};

export const quoteStatus = (input: {
  awardStatus?: string;
  quote?: {
    source?: string;
    confirmed?: boolean;
    status?: string;
  } | null;
}): QuoteStatusView => {
  if (input.awardStatus === "AWARDED") return { color: "green", text: "已中标" };
  if (input.awardStatus === "NOT_AWARDED")
    return { color: "default", text: "未中标" };
  if (input.quote?.source === "INTERNAL_ENTRY" && !input.quote.confirmed)
    return { color: "orange", text: "采购代录 · 待确认" };
  if (input.quote?.status === "SUBMITTED")
    return { color: "blue", text: "已提交 · 待定标" };
  if (
    input.quote?.status === "DRAFT" ||
    input.quote?.status === "WITHDRAWN"
  )
    return { color: "orange", text: "草稿" };
  return { color: "default", text: "待报价" };
};

export const contractStatusText = (value: string) =>
  (
    {
      DRAFT: "随订单待生效",
      PENDING_APPROVAL: "审批中",
      ACTIVE: "已生效",
      REJECTED: "已驳回",
      SUPERSEDED: "已变更",
    } as Record<string, string>
  )[value] || value;

/** 返回有效期距今天数；无有效期返回 null。 */
export const docExpiryDays = (validTo?: string): number | null => {
  if (!validTo) return null;
  return Math.round(
    (new Date(`${validTo}T00:00:00`).getTime() - startOfToday().getTime()) /
      86400000,
  );
};

/** 生成资质到期提醒文案；未到期或距到期超过 90 天返回 null。 */
export const expiryMessage = (label: string, validTo?: string): string | null => {
  const days = docExpiryDays(validTo);
  if (days === null || days > 90) return null;
  if (days < 0) return `${label}已于 ${validTo} 到期，请尽快更新资质文件`;
  if (days === 0) return `${label}将于今天到期，请尽快更新`;
  return `${label}将于 ${validTo} 到期（剩 ${days} 天），请提前更新`;
};

export const UPLOAD_ALLOWED_EXTENSIONS = [
  ".jpg",
  ".jpeg",
  ".png",
  ".webp",
  ".pdf",
  ".doc",
  ".docx",
  ".xls",
  ".xlsx",
];
export const UPLOAD_MAX_BYTES = 20 * 1024 * 1024;

/** 上传前本地预校验：返回错误文案，通过返回 null。 */
export const validateUploadFile = (file: File): string | null => {
  if (file.size > UPLOAD_MAX_BYTES) {
    return "文件不能超过 20MB";
  }
  const ext = "." + (file.name.split(".").pop() || "").toLowerCase();
  if (!UPLOAD_ALLOWED_EXTENSIONS.includes(ext)) {
    return "仅支持图片、PDF、Word 和 Excel 文件";
  }
  return null;
};

export const shipmentStatusText = (value: string) =>
  (
    {
      PENDING: "待确认",
      CONFIRMED: "已确认到货",
      REJECTED: "已退回",
    } as Record<string, string>
  )[value] || value;

export const shipmentStatusColor = (value: string) =>
  (
    {
      PENDING: "orange",
      CONFIRMED: "green",
      REJECTED: "red",
    } as Record<string, string>
  )[value] || "default";
