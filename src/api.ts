// API Client — connects the React prototype to the Spring Boot backend

const TOKEN_KEY = "ops_erp_token";

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface LoginResponse {
  token: string;
  user: { id: string; username: string; displayName: string; roles: string[]; permissions: string[] };
}

export type ReceivableStatus = "INVOICE_PENDING" | "PAYMENT_PENDING" | "SETTLED" | "OVERDUE";
export type FinancePayableStatus = "PENDING" | "PARTIAL_PAID" | "PAID" | "CANCELLED";

export interface ReceivableResponse {
  id: string; customerId: string; customerName: string; contractId: string;
  contractCode: string; code: string; sourceNo: string;
  amount: number; dueDate: string; invoiceNo: string | null;
  invoiceDate: string | null; settledAmount: number;
  outstandingAmount: number; status: ReceivableStatus;
}

export interface FinancePayableResponse {
  id: string; code: string; supplierId: string; supplierName: string;
  orderId: string; orderCode: string; amount: number; paidAmount: number;
  outstandingAmount: number; reservedAmount: number; availableAmount: number;
  dueDate: string; status: FinancePayableStatus; overdue: boolean;
}

export interface FinanceOverviewResponse {
  receivableAmount: number; receivedAmount: number;
  receivableOutstanding: number; receivableOverdue: number;
  payableAmount: number; paidAmount: number;
  payableOutstanding: number; payableOverdue: number;
  netCashInflow: number; pendingPaymentApplications: number;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}
function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const res = await fetch("/api/auth/login", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) throw new Error(`Login failed: ${res.status}`);
  const body: ApiResponse<LoginResponse> = await res.json();
  if (body.code !== 200) throw new Error(body.message || "Login failed");
  setToken(body.data.token);
  return body.data;
}

async function authFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = { ...(init?.headers as Record<string, string>) };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  if (!headers["Content-Type"]) headers["Content-Type"] = "application/json";
  const res = await fetch(url, { ...init, headers });
  if (res.status === 401) { clearToken(); throw new Error("unauthorized"); }
  if (!res.ok) throw new Error(`API error ${res.status}`);
  const body: ApiResponse<T> = await res.json();
  if (body.code !== 200) throw new Error(body.message || "API error");
  return body.data;
}

export function fetchFinanceOverview(): Promise<FinanceOverviewResponse> {
  return authFetch<FinanceOverviewResponse>("/api/finance/overview");
}
export function fetchReceivables(): Promise<ReceivableResponse[]> {
  return authFetch<ReceivableResponse[]>("/api/finance/receivables");
}
export function registerInvoice(id: string, invoiceNo: string, invoiceDate: string): Promise<ReceivableResponse> {
  return authFetch<ReceivableResponse>(`/api/finance/receivables/${id}/invoice`, {
    method: "POST", body: JSON.stringify({ invoiceNo, invoiceDate }),
  });
}
export function recordReceipt(id: string, amount: number, receivedDate: string, referenceNo: string, recorderName: string): Promise<ReceivableResponse> {
  return authFetch<ReceivableResponse>(`/api/finance/receivables/${id}/receipts`, {
    method: "POST", body: JSON.stringify({ amount, receivedDate, referenceNo, recorderName }),
  });
}
export function fetchPayables(): Promise<FinancePayableResponse[]> {
  return authFetch<FinancePayableResponse[]>("/api/finance/payables");
}
