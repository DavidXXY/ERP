export function reportTypeLabel(type: string) {
  return { DAILY: "日报", WEEKLY: "周报", MONTHLY: "月报" }[type] || type;
}

export function dateText(v: string) {
  if (!v) return "-";
  return new Date(v).toLocaleString("zh-CN", { hour12: false });
}
