/** CSV export utility for CRM list views */

function escapeCsv(value: string | number | undefined | null): string {
  if (value == null) return "";
  const str = String(value);
  if (str.includes(",") || str.includes('"') || str.includes("\n")) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

export function downloadCsv(filename: string, headers: string[], rows: string[][]) {
  const bom = "\uFEFF";
  const csv = bom + [headers.join(","), ...rows.map((r) => r.join(","))].join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;bom" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

// Reusable column builder helpers
export function customerRowToCsv(r: {
  name: string; code?: string; industry: string; level: string;
  ownerName: string; contactCount: number; primaryContact?: string;
  paymentHabit?: string; riskStatus: string;
}): string[] {
  return [
    r.code || "", r.name, r.industry, levelLabel(r.level),
    r.ownerName, String(r.contactCount), r.primaryContact || "",
    r.paymentHabit || "", riskLabel(r.riskStatus),
  ];
}

export function receivableRowToCsv(r: {
  code?: string; customerName: string; sourceNo?: string;
  amount: number; outstandingAmount: number; dueDate?: string;
  status: string; invoiceNo?: string;
}): string[] {
  return [
    r.code || "", r.customerName, r.sourceNo || "",
    String(r.amount), String(r.outstandingAmount),
    r.dueDate || "", receivableStatusLabel(r.status), r.invoiceNo || "",
  ];
}

export function contractRowToCsv(r: {
  code?: string; customerName: string; projectName: string;
  contractType: string; amount: number; startDate?: string;
  endDate?: string; status: string;
}): string[] {
  return [
    r.code || "", r.customerName, r.projectName, r.contractType,
    String(r.amount), r.startDate || "", r.endDate || "",
    contractStatusLabel(r.status),
  ];
}

// Quick label helpers (inline to avoid circular imports)
function levelLabel(v: string) {
  return { STRATEGIC: "战略客户", KEY: "重点客户", NORMAL: "普通客户" }[v] || v;
}
function riskLabel(v: string) {
  return { NORMAL: "正常", OVERDUE: "逾期", RENEWAL_RISK: "续约风险" }[v] || v;
}
function receivableStatusLabel(v: string) {
  return { INVOICE_PENDING: "待开票", PAYMENT_PENDING: "待回款", SETTLED: "已核销", OVERDUE: "逾期" }[v] || v;
}
function contractStatusLabel(v: string) {
  return { ACTIVE: "履约中", RENEWAL_PENDING: "待续约", OVERDUE_RISK: "履约风险", CLOSED: "已关闭" }[v] || v;
}
