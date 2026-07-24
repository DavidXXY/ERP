import type { CollaborationTodo } from "@/api/collaboration";

export type CanonicalBusinessTodo = {
  key: string;
  module: string;
  moduleName: string;
  title: string;
  subject: string;
  priority: "HIGH" | "MEDIUM" | "LOW";
  status: "OPEN" | "PROCESSING" | "OVERDUE" | "DONE";
  owner: string;
  dueDate: string;
  route: string;
  action: string;
  entityId?: string;
};

const presentationByType: Record<
  string,
  { module: string; moduleName: string; action: string }
> = {
  APPROVAL: {
    module: "OFFICE",
    moduleName: "OA协同",
    action: "处理审批或协同事项",
  },
  PURCHASE_REQUEST: {
    module: "PROCUREMENT",
    moduleName: "供应链采购",
    action: "审批采购申请并确认预算归集对象",
  },
  PURCHASE_ORDER: {
    module: "PROCUREMENT",
    moduleName: "供应链采购",
    action: "审批采购订单",
  },
  HANDOVER: {
    module: "PROJECT",
    moduleName: "项目管理",
    action: "补齐交接材料并完成接收",
  },
  INVOICE_MATCH: {
    module: "PROCUREMENT",
    moduleName: "供应链采购",
    action: "核对订单、收货、发票和应付差异",
  },
  RECEIVABLE: {
    module: "FINANCE",
    moduleName: "财务资金",
    action: "登记催收并推动回款",
  },
};

export function mapCanonicalTodo(
  item: CollaborationTodo,
): CanonicalBusinessTodo {
  const presentation = presentationByType[item.type] || {
    module: "COLLABORATION",
    moduleName: "跨部门协同",
    action: "进入业务页面处理",
  };
  return {
    key: `${item.type}-${item.id}`,
    module: presentation.module,
    moduleName: presentation.moduleName,
    title: item.title,
    subject: item.detail,
    priority:
      item.priority === "URGENT" || item.priority === "HIGH"
        ? "HIGH"
        : "MEDIUM",
    status:
      item.status === "DONE"
        ? "DONE"
        : item.overdueDays > 0
          ? "OVERDUE"
          : "OPEN",
    owner: item.assigneeName || "待认领",
    dueDate: item.dueAt || item.createdAt,
    route: item.link,
    action: presentation.action,
    entityId: item.type === "APPROVAL" ? item.id : undefined,
  };
}
