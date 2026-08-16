import { ref } from "vue";
import { message } from "ant-design-vue";
import {
  addSignApproval,
  createApproval,
  processApproval,
  resubmitApproval,
  returnApproval,
  transferApproval,
  withdrawApproval,
  type Approval,
  type ApprovalType,
} from "@/api/office";
import {
  approveContract,
  approveContractChange,
  processQuoteApproval,
  rejectContractChange,
} from "@/api/crm";
import { useAuthStore } from "@/stores/auth";
import type {
  MergedApprovalItem,
  MergedChangeItem,
  MergedContractItem,
  MergedQuoteItem,
} from "../approvalItemTypes";

export type ApprovalProcessPayload = {
  decision: "APPROVED" | "REJECTED";
  comment: string;
  approverName: string;
};

export type RuntimeActionPayload = {
  targetUserId: string;
  comment: string;
  operatorName: string;
};

export type ApprovalCreatePayload = {
  code: string;
  approvalType: ApprovalType;
  title: string;
  sourceNo: string;
  amount: number;
  applicantName: string;
  content: string;
  departmentName: string;
  businessType: string;
  projectCode: string;
  supplierRisk: string;
  customerLevel: string;
};

export interface ApprovalActionsOptions {
  loadData: () => Promise<void>;
  onChanged?: () => void;
  getDetailApproval: () => any;
  closeApprovalCreate: () => void;
  closeProcess: () => void;
  closeRuntime: () => void;
  closeCrmProcess: () => void;
  closeDetail: () => void;
}

export function useApprovalActions(opts: ApprovalActionsOptions) {
  const auth = useAuthStore();
  const saving = ref(false);

  async function submitCreateApproval(form: ApprovalCreatePayload) {
    saving.value = true;
    try {
      await createApproval({ ...form });
      opts.closeApprovalCreate();
      message.success("审批已发起");
      await opts.loadData();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "审批发起失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitOfficeProcess(
    approval: Approval,
    form: ApprovalProcessPayload,
  ) {
    saving.value = true;
    try {
      await processApproval(approval.id, { ...form });
      opts.closeProcess();
      message.success(
        form.decision === "APPROVED" ? "审批已通过" : "审批已驳回",
      );
      await opts.loadData();
      opts.onChanged?.();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "审批处理失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitDetailProcess(
    approval: Approval,
    form: ApprovalProcessPayload,
  ) {
    saving.value = true;
    try {
      await processApproval(approval.id, { ...form });
      opts.closeDetail();
      message.success(
        form.decision === "APPROVED" ? "审批已通过" : "审批已驳回",
      );
      await opts.loadData();
      opts.onChanged?.();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "审批处理失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitRuntime(
    approval: Approval,
    action: "transfer" | "addSign",
    form: RuntimeActionPayload,
  ) {
    saving.value = true;
    try {
      if (action === "transfer")
        await transferApproval(approval.id, { ...form });
      else await addSignApproval(approval.id, { ...form });
      opts.closeRuntime();
      message.success(action === "transfer" ? "审批已转交" : "审批已加签");
      await opts.loadData();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "操作失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitWithdraw(item: MergedApprovalItem) {
    saving.value = true;
    try {
      await withdrawApproval(item.id, {
        comment: "申请人撤回",
        operatorName: auth.user?.displayName || "",
      });
      message.success("审批已撤回");
      await opts.loadData();
      opts.onChanged?.();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "撤回失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitDetailWithdraw() {
    const item = opts.getDetailApproval();
    if (!item) return;
    await submitWithdraw(item);
    opts.closeDetail();
  }

  async function submitDetailResubmit() {
    const item = opts.getDetailApproval();
    if (!item) return;
    saving.value = true;
    try {
      await resubmitApproval(item.id, {
        applicantName: auth.user?.displayName || "",
        comment: "审批配置已更新，按最新规则重新提交",
      });
      message.success("审批已按最新规则重新提交");
      opts.closeDetail();
      await opts.loadData();
      opts.onChanged?.();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "重新提交失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitReturn(item: MergedApprovalItem) {
    saving.value = true;
    try {
      await returnApproval(item.id, {
        comment: "退回上一节点",
        operatorName: auth.user?.displayName || "",
      });
      message.success("审批已退回");
      await opts.loadData();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "退回失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitQuoteProcess(
    item: MergedQuoteItem,
    form: ApprovalProcessPayload,
  ) {
    saving.value = true;
    try {
      await processQuoteApproval(item._entityId, { ...form });
      opts.closeCrmProcess();
      message.success(
        form.decision === "APPROVED" ? "报价审批已通过" : "报价已驳回",
      );
      await opts.loadData();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "审批处理失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitContractApproval(item: MergedContractItem) {
    saving.value = true;
    try {
      await approveContract(item._contractId, {
        operatorName: auth.user?.displayName || "",
        comment: "合同审批通过",
      });
      message.success("合同审批已通过");
      await opts.loadData();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "合同审批失败");
    } finally {
      saving.value = false;
    }
  }

  async function submitCrmChange(item: MergedChangeItem, decision: string) {
    saving.value = true;
    try {
      if (decision === "APPROVED")
        await approveContractChange(item._entityId, {
          operatorName: auth.user?.displayName || "",
          comment: "",
        });
      else
        await rejectContractChange(item._entityId, {
          operatorName: auth.user?.displayName || "",
          comment: "",
        });
      message.success(decision === "APPROVED" ? "变更已通过" : "变更已驳回");
      await opts.loadData();
    } catch (error) {
      message.error(error instanceof Error ? error.message : "操作失败");
    } finally {
      saving.value = false;
    }
  }

  return {
    saving,
    submitCreateApproval,
    submitOfficeProcess,
    submitDetailProcess,
    submitRuntime,
    submitWithdraw,
    submitDetailWithdraw,
    submitDetailResubmit,
    submitReturn,
    submitQuoteProcess,
    submitContractApproval,
    submitCrmChange,
  };
}
