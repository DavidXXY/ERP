import axios, { type AxiosRequestConfig } from "axios";

export const SUPPLIER_TOKEN_KEY = "ops_erp_supplier_portal_token";

export type PortalAccount = {
  id: string;
  supplierId: string;
  supplierCode?: string;
  supplierName?: string;
  supplierAdmissionStatus?: string;
  email: string;
  phone?: string;
  contactName: string;
  status: "PENDING_REVIEW" | "ACTIVE" | "REJECTED" | "SUSPENDED";
  mustChangePassword?: boolean;
  reviewComment?: string;
};

export type SupplierProfile = {
  id: string;
  code?: string;
  name: string;
  category?: string;
  contactName?: string;
  phone?: string;
  legalRepresentative?: string;
  unifiedSocialCreditCode: string;
  registeredCapital?: string;
  registeredAddress?: string;
  businessScope?: string;
  licenseValidTo?: string;
  qualificationValidTo?: string;
  taxpayerType?: string;
  bankName?: string;
  maskedBankAccount?: string;
  settlementTerms?: string;
  admissionStatus: "PENDING" | "APPROVED" | "REJECTED";
  admissionReviewComment?: string;
  riskStatus?: string;
};

export type Session = { token: string; account: PortalAccount; supplier: SupplierProfile };

export type PortalDocument = {
  id: string;
  supplierId: string;
  documentType: string;
  documentName: string;
  contentType?: string;
  sizeBytes: number;
  validTo?: string;
  reviewStatus: "PENDING" | "APPROVED" | "REJECTED";
  reviewComment?: string;
  createdAt: string;
};

export type QuoteLine = {
  requestId: string;
  requestCode?: string;
  partName?: string;
  quantity: number;
  expectedDate?: string;
  unitPrice?: number;
  taxRate?: number;
  deliveryDate?: string;
  remark?: string;
};

export type PortalQuote = {
  id: string;
  source: "SUPPLIER_PORTAL" | "INTERNAL_ENTRY";
  status: "DRAFT" | "SUBMITTED" | "WITHDRAWN";
  versionNo: number;
  currency: string;
  paymentTerms?: string;
  remark?: string;
  freightAmount: number;
  otherCostAmount: number;
  validUntil?: string;
  submittedByName?: string;
  submittedAt?: string;
  confirmed: boolean;
  confirmedAt?: string;
  materialAmount: number;
  totalAmount: number;
  lines: QuoteLine[];
  declinedAt?: string;
  declineReason?: string;
};

export type QuoteAttachment = {
  id: string; quoteId: string; attachmentType: string; fileName: string;
  contentType?: string; sizeBytes: number; sha256: string; createdAt: string;
};

export type Clarification = {
  id: string; inquiryId: string; supplierId: string; supplierName?: string;
  question: string; askedAt: string; answer?: string; answeredByName?: string;
  answeredAt?: string; status: string;
};

export type PortalInquiry = {
  id: string;
  code: string;
  title: string;
  deadline?: string;
  status: "OPEN" | "AWARDED";
  invitationStatus: string;
  invitedAt: string;
  lines: QuoteLine[];
  quote?: PortalQuote;
  declineReason?: string;
  declinedAt?: string;
  attachments?: QuoteAttachment[];
  clarifications?: Clarification[];
};

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api/supplier-portal",
  timeout: 15000,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(SUPPLIER_TOKEN_KEY);
  if (token && !config.url?.startsWith("/auth/")) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(SUPPLIER_TOKEN_KEY);
      if (location.pathname !== "/login") location.href = "/login";
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || "请求失败"));
  },
);

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<{ success: boolean; message: string; data: T }>(config);
  if (!response.data.success) throw new Error(response.data.message || "请求失败");
  return response.data.data;
}

export const login = (data: { email: string; password: string }) =>
  request<Session>({ url: "/auth/login", method: "POST", data });
export const register = (data: Record<string, unknown>) =>
  request<Session>({ url: "/auth/register", method: "POST", data });
export const getSession = () => request<Session>({ url: "/me" });
export const updateProfile = (data: Record<string, unknown>) =>
  request<SupplierProfile>({ url: "/profile", method: "PUT", data });
export const listDocuments = () => request<PortalDocument[]>({ url: "/documents" });
export const uploadDocument = (data: FormData) =>
  request<PortalDocument>({ url: "/documents", method: "POST", data });
export const deleteDocument = (id: string) =>
  request<void>({ url: `/documents/${id}`, method: "DELETE" });
export const documentDownloadUrl = (id: string) => `${http.defaults.baseURL}/documents/${id}/download`;
export const listInquiries = () => request<PortalInquiry[]>({ url: "/inquiries" });
export const saveQuote = (id: string, data: Record<string, unknown>, submit = false) =>
  request<PortalQuote>({
    url: `/inquiries/${id}/quote${submit ? "/submit" : ""}`,
    method: submit ? "POST" : "PUT",
    data,
  });
export const withdrawQuote = (id: string) =>
  request<PortalQuote>({ url: `/inquiries/${id}/quote/withdraw`, method: "POST" });
export const confirmQuote = (id: string) =>
  request<PortalQuote>({ url: `/inquiries/${id}/quote/confirm`, method: "POST" });
export const declineInquiry = (id: string, reason: string) =>
  request<PortalInquiry>({ url: `/inquiries/${id}/decline`, method: "POST", data: { reason } });
export const changePassword = (data: { currentPassword: string; newPassword: string }) =>
  request<Session>({ url: "/account/change-password", method: "POST", data });
export const uploadQuoteAttachment = (id: string, data: FormData) =>
  request<QuoteAttachment>({ url: `/inquiries/${id}/attachments`, method: "POST", data });
export const deleteQuoteAttachment = (inquiryId: string, id: string) =>
  request<void>({ url: `/inquiries/${inquiryId}/attachments/${id}`, method: "DELETE" });
export const quoteAttachmentDownloadUrl = (inquiryId: string, id: string) =>
  `${http.defaults.baseURL}/inquiries/${inquiryId}/attachments/${id}/download`;
export const askClarification = (id: string, question: string) =>
  request<Clarification>({ url: `/inquiries/${id}/clarifications`, method: "POST", data: { question } });
