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

export type Session = {
  token: string;
  account: PortalAccount;
  supplier: SupplierProfile;
};

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
  historicalPrice?: number;
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
  id: string;
  quoteId: string;
  attachmentType: string;
  fileName: string;
  contentType?: string;
  sizeBytes: number;
  sha256: string;
  createdAt: string;
};

export type Clarification = {
  id: string;
  inquiryId: string;
  supplierId: string;
  supplierName?: string;
  question: string;
  askedAt: string;
  answer?: string;
  answeredByName?: string;
  answeredAt?: string;
  status: string;
};

export type PortalInquiry = {
  id: string;
  code: string;
  title: string;
  deadline?: string;
  status: "OPEN" | "AWARDED";
  awardStatus: "PENDING" | "AWARDED" | "NOT_AWARDED";
  awardedAt?: string;
  invitationStatus: string;
  invitedAt: string;
  lines: QuoteLine[];
  quote?: PortalQuote;
  contract?: {
    id: string;
    contractNo: string;
    name: string;
    amount: number;
    currency: string;
    status: "DRAFT" | "PENDING_APPROVAL" | "ACTIVE" | "REJECTED" | "SUPERSEDED";
    approvalStatus: "PENDING" | "APPROVED" | "REJECTED";
    startDate?: string;
    endDate?: string;
    orderId?: string;
    acknowledged?: boolean;
    acknowledgedAt?: string;
    acknowledgedByName?: string;
    documents?: ContractDocument[];
  };
  declineReason?: string;
  declinedAt?: string;
  attachments?: QuoteAttachment[];
  clarifications?: Clarification[];
};

export type ContractDocument = {
  id: string;
  fileName: string;
  contentType?: string;
  sizeBytes: number;
  uploadedBy?: string;
  uploadedAt?: string;
};

export type PortalNotification = {
  id: string;
  type: string;
  title: string;
  content: string;
  relatedType?: string;
  relatedId?: string;
  read: boolean;
  readAt?: string;
  createdAt: string;
};

export type QuoteRevision = {
  id: string;
  versionNo: number;
  submissionSource: string;
  submittedByName?: string;
  submittedAt?: string;
  snapshot: {
    totalAmount?: number;
    materialAmount?: number;
    freightAmount?: number;
    otherCostAmount?: number;
    paymentTerms?: string;
    remark?: string;
    validUntil?: string;
    versionNo?: number;
    lines?: Array<{
      requestId?: string;
      requestCode?: string;
      partName?: string;
      quantity?: number;
      unitPrice?: number;
      taxRate?: number;
      deliveryDate?: string;
    }>;
  };
};

export type PortalChangeRequest = {
  id: string;
  changeType: string;
  proposedName?: string;
  proposedCreditCode?: string;
  proposedBankName?: string;
  proposedBankAccount?: string;
  proposedSettlementTerms?: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  requestedByName?: string;
  reviewedByName?: string;
  reviewComment?: string;
  reviewedAt?: string;
  createdAt: string;
};

export type PerformanceReview = {
  id: string;
  reviewPeriod: string;
  onTimeRate: number;
  qualityRate: number;
  invoiceMatchRate: number;
  responseScore: number;
  totalScore: number;
  grade: string;
  reviewerName?: string;
  improvementAction?: string;
  status: string;
  createdAt: string;
};

export type ProcurementShipment = {
  id: string;
  orderId: string;
  orderCode?: string;
  supplierId: string;
  deliveryNo?: string;
  carrier?: string;
  expectedArrival?: string;
  remark?: string;
  status: string;
  createdAt: string;
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
    return Promise.reject(
      new Error(error.response?.data?.message || error.message || "请求失败"),
    );
  },
);

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<{
    success: boolean;
    message: string;
    data: T;
  }>(config);
  if (!response.data.success)
    throw new Error(response.data.message || "请求失败");
  return response.data.data;
}

export const login = (data: { email: string; password: string }) =>
  request<Session>({ url: "/auth/login", method: "POST", data });
export const register = (data: Record<string, unknown>) =>
  request<Session>({ url: "/auth/register", method: "POST", data });
export const getSession = () => request<Session>({ url: "/me" });
export const updateProfile = (data: Record<string, unknown>) =>
  request<SupplierProfile>({ url: "/profile", method: "PUT", data });
export const listDocuments = () =>
  request<PortalDocument[]>({ url: "/documents" });
export const uploadDocument = (data: FormData) =>
  request<PortalDocument>({ url: "/documents", method: "POST", data });
export const deleteDocument = (id: string) =>
  request<void>({ url: `/documents/${id}`, method: "DELETE" });
const withToken = (url: string) => {
  const token = localStorage.getItem(SUPPLIER_TOKEN_KEY);
  return token ? `${url}?token=${encodeURIComponent(token)}` : url;
};
export const documentDownloadUrl = (id: string) =>
  withToken(`${http.defaults.baseURL}/documents/${id}/download`);
export const contractDocumentDownloadUrl = (id: string) =>
  withToken(`${http.defaults.baseURL}/contract-documents/${id}/download`);
export const listNotifications = () =>
  request<PortalNotification[]>({ url: "/notifications" });
export const unreadNotificationCount = () =>
  request<number>({ url: "/notifications/unread-count" });
export const markNotificationRead = (id: string) =>
  request<void>({ url: `/notifications/${id}/read`, method: "POST" });
export const markAllNotificationsRead = () =>
  request<void>({ url: "/notifications/read-all", method: "POST" });
export const listMyShipments = () =>
  request<ProcurementShipment[]>({ url: "/shipments" });
export const createShipment = (
  orderId: string,
  data: {
    deliveryNo?: string;
    carrier?: string;
    expectedArrival?: string;
    remark?: string;
  },
) =>
  request<ProcurementShipment>({
    url: `/orders/${orderId}/shipments`,
    method: "POST",
    data,
  });
export const acknowledgeContract = (id: string) =>
  request<Record<string, unknown>>({
    url: `/contracts/${id}/acknowledge`,
    method: "POST",
  });
export const listInquiries = () =>
  request<PortalInquiry[]>({ url: "/inquiries" });
export const saveQuote = (
  id: string,
  data: Record<string, unknown>,
  submit = false,
) =>
  request<PortalQuote>({
    url: `/inquiries/${id}/quote${submit ? "/submit" : ""}`,
    method: submit ? "POST" : "PUT",
    data,
  });
export const withdrawQuote = (id: string) =>
  request<PortalQuote>({
    url: `/inquiries/${id}/quote/withdraw`,
    method: "POST",
  });
export const confirmQuote = (id: string) =>
  request<PortalQuote>({
    url: `/inquiries/${id}/quote/confirm`,
    method: "POST",
  });
export const declineInquiry = (id: string, reason: string) =>
  request<PortalInquiry>({
    url: `/inquiries/${id}/decline`,
    method: "POST",
    data: { reason },
  });
export const changePassword = (data: {
  currentPassword: string;
  newPassword: string;
}) =>
  request<Session>({ url: "/account/change-password", method: "POST", data });
export const uploadQuoteAttachment = (id: string, data: FormData) =>
  request<QuoteAttachment>({
    url: `/inquiries/${id}/attachments`,
    method: "POST",
    data,
  });
export const deleteQuoteAttachment = (inquiryId: string, id: string) =>
  request<void>({
    url: `/inquiries/${inquiryId}/attachments/${id}`,
    method: "DELETE",
  });
export const quoteAttachmentDownloadUrl = (inquiryId: string, id: string) =>
  withToken(
    `${http.defaults.baseURL}/inquiries/${inquiryId}/attachments/${id}/download`,
  );
export const askClarification = (id: string, question: string) =>
  request<Clarification>({
    url: `/inquiries/${id}/clarifications`,
    method: "POST",
    data: { question },
  });
export const listQuoteRevisions = (inquiryId: string) =>
  request<QuoteRevision[]>({ url: `/inquiries/${inquiryId}/quote/revisions` });
export const listChangeRequests = () =>
  request<PortalChangeRequest[]>({ url: "/change-requests" });
export const createChangeRequest = (data: Record<string, unknown>) =>
  request<PortalChangeRequest>({
    url: "/change-requests",
    method: "POST",
    data,
  });
export const listPerformanceReviews = () =>
  request<PerformanceReview[]>({ url: "/performance" });
