import { request } from "./http";

export type CustomerLevel = "STRATEGIC" | "KEY" | "NORMAL";
export type RiskStatus = "NORMAL" | "OVERDUE" | "RENEWAL_RISK";
export type ContractStatus = "ACTIVE" | "RENEWAL_PENDING" | "OVERDUE_RISK" | "CLOSED";
export type ReceivableStatus = "INVOICE_PENDING" | "PAYMENT_PENDING" | "SETTLED" | "OVERDUE";

export type CustomerSummary = {
  id: string;
  code: string;
  name: string;
  industry: string;
  level: CustomerLevel;
  ownerName: string;
  paymentHabit?: string;
  riskStatus: RiskStatus;
  contactCount: number;
  siteCount: number;
  primaryContact?: string;
};

export type CustomerDetail = CustomerSummary & {
  riskNote?: string;
  invoice: {
    title?: string;
    taxNo?: string;
    bankName?: string;
    bankAccount?: string;
    registeredAddress?: string;
    registeredPhone?: string;
  };
  contacts: Array<{
    id: string;
    name: string;
    title?: string;
    phone?: string;
    email?: string;
    primaryContact: boolean;
  }>;
  sites: Array<{
    id: string;
    name: string;
    address: string;
  }>;
  contracts: Array<{
    id: string;
    code: string;
    projectName: string;
    contractType: string;
    amount: number;
    startDate: string;
    endDate: string;
    serviceCycle?: string;
    status: ContractStatus;
  }>;
  receivables: Array<{
    id: string;
    code: string;
    sourceNo: string;
    amount: number;
    dueDate: string;
    status: ReceivableStatus;
  }>;
  metrics: {
    contractCount: number;
    contractAmount: number;
    outstandingAmount: number;
    settledAmount: number;
  };
};

export type CreateCustomerPayload = {
  code: string;
  name: string;
  industry: string;
  level: CustomerLevel;
  ownerName: string;
  paymentHabit?: string;
  riskStatus?: RiskStatus;
  riskNote?: string;
  invoice?: {
    title?: string;
    taxNo?: string;
    bankName?: string;
    bankAccount?: string;
    registeredAddress?: string;
    registeredPhone?: string;
  };
  contacts?: Array<{
    name: string;
    title?: string;
    phone?: string;
    email?: string;
    primaryContact: boolean;
  }>;
  sites?: Array<{
    name: string;
    address: string;
  }>;
};

export function listCustomers() {
  return request<CustomerSummary[]>({
    method: "GET",
    url: "/crm/customers",
  });
}

export function getCustomer(id: string) {
  return request<CustomerDetail>({
    method: "GET",
    url: `/crm/customers/${id}`,
  });
}

export function createCustomer(payload: CreateCustomerPayload) {
  return request<CustomerDetail>({
    method: "POST",
    url: "/crm/customers",
    data: payload,
  });
}

