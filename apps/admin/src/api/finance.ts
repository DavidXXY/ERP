import { request } from "./http";
import type { ContractStatus, Receivable, ReceivableStatus } from "./crm";

export type FinanceOverview = {
  receivableAmount: number;
  receivedAmount: number;
  receivableOutstanding: number;
  receivableOverdue: number;
  payableAmount: number;
  paidAmount: number;
  payableOutstanding: number;
  payableOverdue: number;
  netCashInflow: number;
  pendingPaymentApplications: number;
};

export type FinancePayableStatus = "PENDING" | "PARTIAL_PAID" | "PAID" | "CANCELLED";

export type FinancePayable = {
  id: string;
  code?: string;
  supplierId: string;
  supplierName: string;
  orderId: string;
  orderCode: string;
  amount: number;
  paidAmount: number;
  outstandingAmount: number;
  reservedAmount: number;
  availableAmount: number;
  dueDate: string;
  status: FinancePayableStatus;
  overdue: boolean;
};

export type PaymentApplicationStatus = "PENDING_APPROVAL" | "APPROVED" | "REJECTED" | "PAID";
export type PaymentMethod = "BANK_TRANSFER" | "CHECK" | "CASH" | "OTHER";

export type PaymentApplication = {
  id: string;
  code?: string;
  payableId: string;
  payableCode: string;
  supplierId: string;
  supplierName: string;
  requestedAmount: number;
  requestedDate: string;
  applicantName: string;
  purpose: string;
  status: PaymentApplicationStatus;
  approvalComment?: string;
  approverName?: string;
  approvedAt?: string;
  paymentId?: string;
  paymentCode?: string;
};

export type PaymentRecord = {
  id: string;
  code?: string;
  applicationId: string;
  applicationCode: string;
  payableId: string;
  payableCode: string;
  supplierId: string;
  supplierName: string;
  amount: number;
  paidDate: string;
  paymentMethod: PaymentMethod;
  bankReference: string;
  payerName: string;
};

export type FinanceReceivableDetail = {
  receivable: Receivable;
  customerInvoice: {
    customerId: string;
    customerCode?: string;
    customerName?: string;
    ownerName?: string;
    invoiceTitle?: string;
    taxNo?: string;
    bankName?: string;
    bankAccount?: string;
    registeredAddress?: string;
    registeredPhone?: string;
    paymentHabit?: string;
  };
  contract?: {
    id: string;
    quoteId?: string;
    code?: string;
    projectName: string;
    contractType: string;
    amount: number;
    taxRate: number;
    netAmount: number;
    startDate: string;
    endDate: string;
    serviceCycle?: string;
    status: ContractStatus;
    receivableStatus: ReceivableStatus;
    createdAt?: string;
  };
};

export function getFinanceOverview() {
  return request<FinanceOverview>({ method: "GET", url: "/finance/overview" });
}

export function listFinanceReceivables() {
  return request<Receivable[]>({ method: "GET", url: "/finance/receivables" });
}

export function getFinanceReceivableDetail(id: string) {
  return request<FinanceReceivableDetail>({ method: "GET", url: `/finance/receivables/${id}` });
}

export function registerFinanceInvoice(id: string, payload: { invoiceNo: string; invoiceDate: string }) {
  return request<Receivable>({ method: "POST", url: `/finance/receivables/${id}/invoice`, data: payload });
}

export function recordFinanceReceipt(id: string, payload: { amount: number; receivedDate: string; referenceNo: string; recorderName: string }) {
  return request<Receivable>({ method: "POST", url: `/finance/receivables/${id}/receipts`, data: payload });
}

export function listFinancePayables() {
  return request<FinancePayable[]>({ method: "GET", url: "/finance/payables" });
}

export function listPaymentApplications() {
  return request<PaymentApplication[]>({ method: "GET", url: "/finance/payment-applications" });
}

export function createPaymentApplication(payload: { code: string; payableId: string; requestedAmount: number; requestedDate: string; applicantName: string; purpose: string }) {
  return request<PaymentApplication>({ method: "POST", url: "/finance/payment-applications", data: payload });
}

export function processPaymentApplication(id: string, payload: { decision: "APPROVED" | "REJECTED"; comment: string; approverName: string }) {
  return request<PaymentApplication>({ method: "POST", url: `/finance/payment-applications/${id}/approval`, data: payload });
}

export function executePayment(id: string, payload: { paymentCode: string; paidDate: string; paymentMethod: PaymentMethod; bankReference: string; payerName: string }) {
  return request<PaymentRecord>({ method: "POST", url: `/finance/payment-applications/${id}/payment`, data: payload });
}

export function listPaymentRecords() {
  return request<PaymentRecord[]>({ method: "GET", url: "/finance/payments" });
}
