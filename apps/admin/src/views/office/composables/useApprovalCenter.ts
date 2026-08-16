import { computed, ref, type Ref } from "vue";
import { message } from "ant-design-vue";
import {
  getOfficeReferences,
  listApprovals,
  type Approval,
  type ApprovalRuntimeNode,
} from "@/api/office";
import {
  listContracts,
  listContractChanges,
  listQuotes,
  type QuotePlan,
  type ServiceContract,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import { approvalStatusColor, approvalStatusLabel } from "../approvalStatusMeta";
import { approvalTypeLabel, formatMoney } from "../approvalFormat";
import type { MergedApprovalItem } from "../approvalItemTypes";

export interface ApprovalCenterFilters {
  sourceFilter: Ref<string>;
  slaFilter: Ref<string | undefined>;
  riskFilter: Ref<string | undefined>;
}

export function useApprovalCenter(filters: ApprovalCenterFilters) {
  const auth = useAuthStore();
  const loading = ref(false);

  // Office approval data
  const officeApprovals = ref<Approval[]>([]);
  const users = ref<Array<{ id: string; displayName: string; enabled: boolean }>>(
    [],
  );
  // CRM approval data
  const pendingQuotes = ref<QuotePlan[]>([]);
  const pendingContracts = ref<ServiceContract[]>([]);
  const pendingChanges = ref<any[]>([]);

  const userOptions = computed(() =>
    users.value
      .filter((item) => item.enabled)
      .map((item) => ({ label: item.displayName, value: item.id })),
  );

  // Unified approval list
  const mergedList = computed(() => {
    const items: MergedApprovalItem[] = [];
    // Office approvals
    officeApprovals.value.forEach((a) => {
      items.push({
        _key: "office-" + a.id,
        _source: "office",
        _entityId: a.id,
        _type: "通用审批",
        _statusLabel: approvalStatusLabel(a.status),
        _statusColor: approvalStatusColor(a.status),
        id: a.id,
        code: a.code,
        title: a.title,
        amount: a.amount,
        content: a.content,
        approvalType: a.approvalType,
        sourceNo: a.sourceNo,
        applicantName: a.applicantName,
        status: a.status,
        date: a.createdAt,
        departmentName: a.departmentName,
        businessType: a.businessType,
        projectCode: a.projectCode,
        customerLevel: a.customerLevel,
        approverName: a.approverName,
        approvalComment: a.approvalComment,
        sourceDetail: a.sourceDetail,
        currentApproverName: a.currentApproverName,
        matchedRuleText: a.matchedRuleText,
        approvalConfigVersion: a.approvalConfigVersion,
        nodes: a.nodes || [],
        actions: a.actions || [],
        _slaLevel: slaLevel(a.createdAt, a.status === "PENDING"),
        _riskLevel: approvalRiskLevel(
          a.amount,
          a.createdAt,
          a.status === "PENDING",
        ),
      });
    });
    // CRM quote approvals
    pendingQuotes.value.forEach((q) => {
      items.push({
        _key: "quote-" + q.id,
        _source: "quote",
        _entityId: q.id,
        _type: "报价审批",
        _statusLabel: approvalStatusLabel(q.status),
        _statusColor: approvalStatusColor(q.status),
        id: q.id,
        code: q.code,
        title: q.customerName,
        desc: q.serviceScope,
        amount: q.amount,
        customerName: q.customerName,
        status: q.status,
        date: q.updatedAt,
        paymentNodes: q.paymentNodes,
        budgetAmount: q.budgetAmount,
        grossMarginRate: q.grossMarginRate,
        applicantName: (q as any).editorName,
        approverName: q.lastApproverName,
        approvalComment: q.lastApprovalComment,
        _slaLevel: slaLevel(q.updatedAt, true),
        _riskLevel: approvalRiskLevel(q.amount, q.updatedAt, true),
      });
    });
    // Contract approvals
    pendingContracts.value.forEach((c) => {
      items.push({
        _key: "contract-" + c.id,
        _source: "contract",
        _entityId: c.id,
        _contractId: c.id,
        _type: "合同审批",
        _statusLabel: approvalStatusLabel(c.status),
        _statusColor: approvalStatusColor(c.status),
        id: c.id,
        code: c.code || "-",
        title: c.projectName,
        desc: `${c.customerName || "-"} · ${c.contractType || "-"}`,
        amount: c.amount,
        customerName: c.customerName,
        status: c.status,
        date: c.startDate,
        contractType: c.contractType,
        startDate: c.startDate,
        endDate: c.endDate,
        serviceCycle: c.serviceCycle,
        _slaLevel: slaLevel(c.startDate, true),
        _riskLevel: approvalRiskLevel(Number(c.amount || 0), c.startDate, true),
      });
    });
    // Contract changes
    pendingChanges.value.forEach((c) => {
      items.push({
        _key: "change-" + c.id,
        _source: "change",
        _entityId: c.id,
        _contractId: c.contractId,
        _type: "合同变更",
        _statusLabel: approvalStatusLabel(c.status),
        _statusColor: approvalStatusColor(c.status),
        id: c.id,
        code: c.contractCode || "-",
        title: c.reason,
        desc: changeSummary(c),
        amount: changeAmount(c),
        changeData: c.changeData,
        date: c.requestedAt,
        applicantName: c.requestedBy,
        status: c.status,
        _slaLevel: slaLevel(c.requestedAt, true),
        _riskLevel: approvalRiskLevel(0, c.requestedAt, true),
      });
    });
    items.sort((a, b) => (a.date || "").localeCompare(b.date || "") * -1);
    return items;
  });

  const filteredList = computed(() => {
    return mergedList.value.filter((item) => {
      return (
        (filters.sourceFilter.value === "all" ||
          item._source === filters.sourceFilter.value) &&
        (!filters.slaFilter.value ||
          item._slaLevel === filters.slaFilter.value) &&
        (!filters.riskFilter.value ||
          item._riskLevel === filters.riskFilter.value)
      );
    });
  });
  const officeCount = computed(
    () => mergedList.value.filter((i) => i._source === "office").length,
  );
  const quoteCount = computed(
    () => mergedList.value.filter((i) => i._source === "quote").length,
  );
  const contractCount = computed(
    () => mergedList.value.filter((i) => i._source === "contract").length,
  );
  const changeCount = computed(
    () => mergedList.value.filter((i) => i._source === "change").length,
  );
  const pendingCount = computed(
    () => mergedList.value.filter((item) => isPendingApproval(item)).length,
  );
  const overdueCount = computed(
    () => mergedList.value.filter((item) => item._slaLevel === "OVERDUE").length,
  );
  const dueSoonCount = computed(
    () =>
      mergedList.value.filter((item) => item._slaLevel === "DUE_SOON").length,
  );
  const highRiskCount = computed(
    () => mergedList.value.filter((item) => item._riskLevel === "HIGH").length,
  );
  const largeAmount = computed(() =>
    mergedList.value
      .filter((item) => isPendingApproval(item))
      .reduce((sum, item) => sum + Number(item.amount || 0), 0),
  );
  const healthCards = computed(() => [
    {
      key: "pending",
      label: "待处理审批",
      value: `${pendingCount.value} 项`,
      hint: `待审金额（元，税价随来源单据）${formatMoney(largeAmount.value)}`,
      danger: pendingCount.value > 0,
      action: () => {
        filters.slaFilter.value = undefined;
        filters.riskFilter.value = undefined;
        filters.sourceFilter.value = "all";
      },
    },
    {
      key: "overdue",
      label: "SLA超时",
      value: `${overdueCount.value} 项`,
      hint: "超过48小时未处理",
      danger: overdueCount.value > 0,
      action: () => {
        filters.slaFilter.value = "OVERDUE";
        filters.riskFilter.value = undefined;
      },
    },
    {
      key: "dueSoon",
      label: "临近超时",
      value: `${dueSoonCount.value} 项`,
      hint: "24-48小时待处理",
      danger: dueSoonCount.value > 0,
      action: () => {
        filters.slaFilter.value = "DUE_SOON";
        filters.riskFilter.value = undefined;
      },
    },
    {
      key: "highRisk",
      label: "高风险审批",
      value: `${highRiskCount.value} 项`,
      hint: "大额或超时事项",
      danger: highRiskCount.value > 0,
      action: () => {
        filters.riskFilter.value = "HIGH";
        filters.slaFilter.value = undefined;
      },
    },
  ]);
  const slaOptions = [
    { label: "SLA超时", value: "OVERDUE" },
    { label: "临近超时", value: "DUE_SOON" },
    { label: "正常", value: "NORMAL" },
  ];
  const riskOptions = [
    { label: "高风险", value: "HIGH" },
    { label: "中风险", value: "MEDIUM" },
    { label: "正常", value: "NORMAL" },
  ];

  const mergedColumns = [
    { title: "来源", key: "source", width: 100 },
    { title: "编号 / 说明", key: "approval", width: 240 },
    { title: "审批信息", key: "detail", width: 360 },
    { title: "类型", key: "type", width: 100 },
    { title: "申请人", key: "applicant", width: 120 },
    { title: "金额（元，税价随来源单据）", key: "amount", width: 230 },
    { title: "状态", key: "status", width: 120 },
    { title: "规则来源", key: "rule", width: 260 },
    { title: "时间", key: "date", width: 120 },
    { title: "操作", key: "action", width: 220, fixed: "right" as const },
  ];

  async function loadData() {
    loading.value = true;
    try {
      const canLoadReferences =
        auth.can("office:view") ||
        auth.can("office:expense:create") ||
        auth.can("office:outsource:create") ||
        auth.can("office:travel:create") ||
        auth.can("office:seal:create");
      const [referenceData, approvalData, quoteData, contractData] =
        await Promise.all([
          canLoadReferences ? getOfficeReferences() : Promise.resolve(null),
          auth.can("office:approval:view")
            ? listApprovals()
            : Promise.resolve([]),
          auth.can("crm:quote:view") ? listQuotes() : Promise.resolve([]),
          auth.can("crm:contract:view") ? listContracts() : Promise.resolve([]),
        ]);
      users.value = referenceData?.users || [];
      officeApprovals.value = approvalData;
      const allQuotes = quoteData;
      const allContracts = contractData;
      pendingQuotes.value = allQuotes.filter(
        (q: QuotePlan) => q.status === "PENDING_APPROVAL",
      );
      pendingContracts.value = allContracts.filter(
        (c: ServiceContract) => c.status === "PENDING_APPROVAL",
      );
      // Fetch contract changes
      try {
        if (!auth.can("crm:contract:view")) {
          pendingChanges.value = [];
          return;
        }
        const contracts = allContracts;
        const allChanges: any[] = [];
        for (const c of contracts) {
          try {
            const changes = await listContractChanges(c.id);
            allChanges.push(
              ...changes
                .filter((ch: any) => ch.status === "PENDING")
                .map((ch: any) => ({
                  ...ch,
                  contractId: c.id,
                  contractCode: c.code,
                })),
            );
          } catch {
            /* skip */
          }
        }
        pendingChanges.value = allChanges;
      } catch {
        /* contract changes supplementary */
      }
    } catch (error) {
      message.error(error instanceof Error ? error.message : "数据加载失败");
    } finally {
      loading.value = false;
    }
  }

  function approvalDetailLines(record: MergedApprovalItem) {
    if (record._source === "office") {
      return [
        `类型：${approvalTypeLabel(record.approvalType) || record._type}`,
        `内容：${record.content || "未填写"}`,
        record.sourceNo ? `来源单号：${record.sourceNo}` : "",
        record.departmentName || record.businessType
          ? `组织/业务：${record.departmentName || "-"} / ${record.businessType || "-"}`
          : "",
      ].filter(Boolean);
    }
    if (record._source === "quote") {
      return [
        `客户：${record.customerName || "-"}`,
        `服务范围：${record.desc || "-"}`,
        `付款方式：${record.paymentNodes || "-"}`,
        `预算（含税，元）/毛利率：${record.budgetAmount != null ? formatMoney(record.budgetAmount) : "-"} / ${record.grossMarginRate != null ? `${Number(record.grossMarginRate).toFixed(1)}%` : "-"}`,
      ];
    }
    if (record._source === "contract") {
      return [
        `客户：${record.customerName || "-"}`,
        `合同类型：${record.contractType || "-"}`,
        `周期：${record.startDate || "-"} 至 ${record.endDate || "-"}`,
        `服务周期：${record.serviceCycle || "-"}`,
      ];
    }
    if (record._source === "change") {
      return [
        `合同：${record.code || "-"}`,
        `变更类型：${changeTypeLabel(record)}`,
        `变更内容：${changeSummary(record)}`,
        `申请原因：${record.title || "-"}`,
      ];
    }
    return ["-"];
  }

  function parseChangeData(record: any) {
    try {
      return record.changeData ? JSON.parse(record.changeData) : {};
    } catch {
      return {};
    }
  }

  function changeTypeLabel(record: any) {
    const data = parseChangeData(record);
    if (data.type === "SIGNED_DOC_APPROVAL") return "双方盖章件审批";
    if (data.type === "RECEIVABLE_UPDATE") return "应收计划变更";
    return "合同信息变更";
  }

  function changeSummary(record: any) {
    const data = parseChangeData(record);
    if (data.type === "SIGNED_DOC_APPROVAL")
      return "审批双方盖章件，通过后合同生效并自动创建项目";
    if (data.type === "RECEIVABLE_UPDATE") {
      return (
        [
          data.amount != null
            ? `应收金额（含税，元）改为 ${formatMoney(Number(data.amount))}`
            : "",
          data.dueDate ? `到期日改为 ${data.dueDate}` : "",
          data.sourceNo ? `来源单号改为 ${data.sourceNo}` : "",
        ]
          .filter(Boolean)
          .join("；") || "应收计划信息变更"
      );
    }
    const labels: Record<string, string> = {
      projectName: "项目名称",
      contractType: "合同类型",
      amount: "合同金额（含税，元）",
      taxRate: "税率",
      startDate: "开始日期",
      endDate: "结束日期",
      serviceCycle: "服务周期",
    };
    return (
      Object.entries(data)
        .filter(([key]) => key !== "type")
        .map(
          ([key, value]) =>
            `${labels[key] || key}：${key === "amount" ? formatMoney(Number(value)) : value}`,
        )
        .join("；") ||
      record.changeData ||
      "-"
    );
  }

  function changeAmount(record: any) {
    const data = parseChangeData(record);
    return data.amount != null ? Number(data.amount) : 0;
  }

  function isPendingApproval(item: MergedApprovalItem) {
    return item.status === "PENDING" || item.status === "PENDING_APPROVAL";
  }
  function approvalAgeHours(value?: string) {
    if (!value) return 0;
    return Math.max(
      0,
      Math.floor((Date.now() - new Date(value).getTime()) / 3600000),
    );
  }
  function slaLevel(value?: string, pending = true) {
    if (!pending) return "NORMAL";
    const hours = approvalAgeHours(value);
    if (hours >= 48) return "OVERDUE";
    if (hours >= 24) return "DUE_SOON";
    return "NORMAL";
  }
  function approvalRiskLevel(amount: number, value?: string, pending = true) {
    if (!pending) return "NORMAL";
    const hours = approvalAgeHours(value);
    if (Number(amount || 0) >= 100000 || hours >= 48) return "HIGH";
    if (Number(amount || 0) >= 30000 || hours >= 24) return "MEDIUM";
    return "NORMAL";
  }
  function approvalAgeLabel(record: MergedApprovalItem) {
    if (!isPendingApproval(record)) return "已处理";
    const hours = approvalAgeHours(record.date);
    if (hours < 1) return "刚提交";
    if (hours < 24) return `已等待 ${hours} 小时`;
    return `已等待 ${Math.floor(hours / 24)} 天`;
  }
  function runtimeNodeSummary(nodes: ApprovalRuntimeNode[]) {
    return nodes
      .map(
        (node) =>
          `第${node.stepNo}步 ${node.assigneeName || "-"} ${node.nodeStatus}${node.dueAt ? " 截止" + node.dueAt.slice(0, 16).replace("T", " ") : ""}`,
      )
      .join(" / ");
  }

  return {
    loading,
    officeApprovals,
    users,
    pendingQuotes,
    pendingContracts,
    pendingChanges,
    userOptions,
    mergedList,
    filteredList,
    officeCount,
    quoteCount,
    contractCount,
    changeCount,
    pendingCount,
    overdueCount,
    dueSoonCount,
    highRiskCount,
    largeAmount,
    healthCards,
    slaOptions,
    riskOptions,
    mergedColumns,
    loadData,
    approvalDetailLines,
    isPendingApproval,
    approvalAgeHours,
    slaLevel,
    approvalRiskLevel,
    approvalAgeLabel,
    runtimeNodeSummary,
    parseChangeData,
    changeTypeLabel,
    changeSummary,
    changeAmount,
  };
}
