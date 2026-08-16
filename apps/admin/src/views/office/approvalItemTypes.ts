// 审批中心 mergedList 的判别联合类型，按 _source 区分来源。
import type { Approval, ApprovalRuntimeNode, ApprovalType } from "@/api/office";

interface MergedApprovalItemBase {
  _key: string;
  _source: "office" | "quote" | "contract" | "change";
  _entityId: string;
  _type: string;
  _statusLabel: string;
  _statusColor: string;
  _slaLevel: string;
  _riskLevel: string;
  id: string;
  code?: string;
  amount?: number;
  status: string;
  date: string;
}

export interface MergedOfficeItem extends MergedApprovalItemBase {
  _source: "office";
  title: string;
  content: string;
  approvalType: ApprovalType;
  sourceNo?: string;
  applicantName: string;
  departmentName?: string;
  businessType?: string;
  projectCode?: string;
  customerLevel?: string;
  approverName?: string;
  approvalComment?: string;
  sourceDetail?: Approval["sourceDetail"];
  currentApproverName?: string;
  matchedRuleText?: string;
  approvalConfigVersion?: number;
  nodes: ApprovalRuntimeNode[];
  actions: Approval["actions"];
}

export interface MergedQuoteItem extends MergedApprovalItemBase {
  _source: "quote";
  title: string;
  desc?: string;
  customerName: string;
  paymentNodes?: string;
  budgetAmount?: number;
  grossMarginRate?: number;
  applicantName?: string;
  approverName?: string;
  approvalComment?: string;
}

export interface MergedContractItem extends MergedApprovalItemBase {
  _source: "contract";
  _contractId: string;
  title: string;
  desc?: string;
  customerName: string;
  contractType?: string;
  startDate?: string;
  endDate?: string;
  serviceCycle?: string;
}

export interface MergedChangeItem extends MergedApprovalItemBase {
  _source: "change";
  _contractId: string;
  title: string;
  desc?: string;
  changeData?: string;
  applicantName?: string;
}

export type MergedApprovalItem =
  | MergedOfficeItem
  | MergedQuoteItem
  | MergedContractItem
  | MergedChangeItem;
