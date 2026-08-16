/**
 * 审批流预设模板。全部基于「动态审批人」（部门负责人/财务经理等），
 * 不绑定具体角色/人员 ID，套用到任意租户都能直接用；套用后再按需微调。
 */
export type ApprovalFlowTemplate = {
  key: string;
  name: string;
  description: string;
  mode: "PARALLEL" | "SEQUENTIAL";
  rules: Array<{
    sequenceNo: number;
    assigneeType: "DYNAMIC" | "AUTO";
    dynamicAssignee?: string;
    autoAction?: string;
    conditionType: "ANY" | "AMOUNT";
    minAmount?: number;
    maxAmount?: number;
    remark?: string;
  }>;
};

export const approvalTemplates: ApprovalFlowTemplate[] = [
  {
    key: "single",
    name: "单级审批（部门负责人）",
    description: "所有单据由部门负责人一级审批，最简单通用。",
    mode: "SEQUENTIAL",
    rules: [
      {
        sequenceNo: 1,
        assigneeType: "DYNAMIC",
        dynamicAssignee: "DEPARTMENT_LEADER",
        conditionType: "ANY",
      },
    ],
  },
  {
    key: "two-level",
    name: "两级审批（部门负责人 → 直属上级）",
    description: "部门负责人先审，再交直属上级终审。",
    mode: "SEQUENTIAL",
    rules: [
      {
        sequenceNo: 1,
        assigneeType: "DYNAMIC",
        dynamicAssignee: "DEPARTMENT_LEADER",
        conditionType: "ANY",
      },
      {
        sequenceNo: 2,
        assigneeType: "DYNAMIC",
        dynamicAssignee: "DIRECT_MANAGER",
        conditionType: "ANY",
      },
    ],
  },
  {
    key: "amount-escalate",
    name: "金额升级审批（超 5 万加财务经理）",
    description: "5 万以内部门负责人审批；超过 5 万追加财务经理终审。",
    mode: "SEQUENTIAL",
    rules: [
      {
        sequenceNo: 1,
        assigneeType: "DYNAMIC",
        dynamicAssignee: "DEPARTMENT_LEADER",
        conditionType: "ANY",
      },
      {
        sequenceNo: 2,
        assigneeType: "DYNAMIC",
        dynamicAssignee: "FINANCE_MANAGER",
        conditionType: "AMOUNT",
        minAmount: 50000,
        remark: "金额超过 5 万时追加",
      },
    ],
  },
  {
    key: "three-level",
    name: "三级审批（部门负责人 → 财务经理 → 直属上级）",
    description: "常规三级串行审批，适合采购、付款、合同等。",
    mode: "SEQUENTIAL",
    rules: [
      {
        sequenceNo: 1,
        assigneeType: "DYNAMIC",
        dynamicAssignee: "DEPARTMENT_LEADER",
        conditionType: "ANY",
      },
      {
        sequenceNo: 2,
        assigneeType: "DYNAMIC",
        dynamicAssignee: "FINANCE_MANAGER",
        conditionType: "ANY",
      },
      {
        sequenceNo: 3,
        assigneeType: "DYNAMIC",
        dynamicAssignee: "DIRECT_MANAGER",
        conditionType: "ANY",
      },
    ],
  },
];
