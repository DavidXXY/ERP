import type {
  ContractStatus,
  CustomerLevel,
  FollowUpType,
  OpportunityStage,
  QuoteStatus,
  ReceivableStatus,
  RiskStatus,
} from "@/api/crm";

export const opportunityStageOptions = [
  { label: "线索", value: "LEAD" },
  { label: "已确认需求", value: "QUALIFIED" },
  { label: "方案沟通", value: "SOLUTION" },
  { label: "报价中", value: "QUOTATION" },
  { label: "商务谈判", value: "NEGOTIATION" },
  { label: "赢单", value: "WON" },
  { label: "丢单", value: "LOST" },
] as const;

export const followUpTypeOptions = [
  { label: "拜访", value: "VISIT" },
  { label: "电话", value: "PHONE" },
  { label: "回访", value: "CALLBACK" },
  { label: "投诉", value: "COMPLAINT" },
] as const;

export function opportunityStageLabel(value: OpportunityStage) {
  return opportunityStageOptions.find((item) => item.value === value)?.label || value;
}

export function opportunityStageColor(value: OpportunityStage) {
  return {
    LEAD: "default",
    QUALIFIED: "cyan",
    SOLUTION: "blue",
    QUOTATION: "purple",
    NEGOTIATION: "orange",
    WON: "green",
    LOST: "red",
  }[value];
}

export function quoteStatusLabel(value: QuoteStatus) {
  return {
    DRAFT: "草稿",
    PENDING_APPROVAL: "审批中",
    APPROVED: "已通过",
    REJECTED: "已驳回",
  }[value];
}

export function quoteStatusColor(value: QuoteStatus) {
  return {
    DRAFT: "default",
    PENDING_APPROVAL: "orange",
    APPROVED: "green",
    REJECTED: "red",
  }[value];
}

export function contractStatusLabel(value: ContractStatus) {
  return {
    ACTIVE: "履约中",
    RENEWAL_PENDING: "待续约",
    OVERDUE_RISK: "履约风险",
    CLOSED: "已关闭",
  }[value];
}

export function contractStatusColor(value: ContractStatus) {
  return {
    ACTIVE: "green",
    RENEWAL_PENDING: "orange",
    OVERDUE_RISK: "red",
    CLOSED: "default",
  }[value];
}

export function receivableStatusLabel(value: ReceivableStatus) {
  return {
    INVOICE_PENDING: "待开票",
    PAYMENT_PENDING: "待回款",
    SETTLED: "已核销",
    OVERDUE: "逾期",
  }[value];
}

export function receivableStatusColor(value: ReceivableStatus) {
  return {
    INVOICE_PENDING: "orange",
    PAYMENT_PENDING: "blue",
    SETTLED: "green",
    OVERDUE: "red",
  }[value];
}

export function followUpTypeLabel(value: FollowUpType) {
  return followUpTypeOptions.find((item) => item.value === value)?.label || value;
}

export function followUpTypeColor(value: FollowUpType) {
  return value === "COMPLAINT" ? "red" : value === "VISIT" ? "blue" : "default";
}

export function levelLabel(value: CustomerLevel) {
  return { STRATEGIC: "战略客户", KEY: "重点客户", NORMAL: "普通客户" }[value];
}

export function levelColor(value: CustomerLevel) {
  return { STRATEGIC: "green", KEY: "blue", NORMAL: "default" }[value];
}

export function riskLabel(value: RiskStatus) {
  return { NORMAL: "正常", OVERDUE: "逾期", RENEWAL_RISK: "续约风险" }[value];
}

export function riskColor(value: RiskStatus) {
  return { NORMAL: "green", OVERDUE: "red", RENEWAL_RISK: "orange" }[value];
}

export function formatMoney(value?: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value || 0);
}
