import { http, request } from "./http";

export type QualificationStatus =
  | "VALID"
  | "EXPIRING"
  | "EXPIRED"
  | "VOIDED"
  | "LOCKED"
  | "UNVERIFIED";
export type Attachment = {
  id?: string;
  name: string;
  type?: string;
  size?: number;
  dataUrl: string;
  uploadedAt?: string;
  uploadedBy?: string;
};
export type CompanyQualification = {
  id: string;
  subjectCompany: string;
  name: string;
  category: string;
  level?: string;
  certificateNo?: string;
  issuer?: string;
  issueDate?: string;
  validFrom?: string;
  validTo?: string;
  annualReviewDate?: string;
  renewalDate?: string;
  scope?: string;
  projectTypes: string[];
  holderBranch?: string;
  storageLocation?: string;
  availableForTender: boolean;
  manualStatus: string;
  locked: boolean;
  attachments: Attachment[];
  remark?: string;
  status: QualificationStatus;
  daysLeft?: number;
};
export type QualificationEmployee = {
  id: string;
  name: string;
  workNo?: string;
  organizationId?: string;
  organizationName?: string;
  organizationPath?: string;
  department?: string;
  position?: string;
  idCard?: string;
  phone?: string;
  entryDate?: string;
  employmentStatus: string;
  contractStart?: string;
  contractEnd?: string;
  socialSecurityUnit?: string;
  socialSecurityStart?: string;
  socialSecurityEnd?: string;
  remark?: string;
  systemUserId?: string;
  account?: EmployeeAccount;
  certificateCount: number;
  validCertificateCount: number;
};
export type EmployeeAccount = {
  id: string;
  username: string;
  displayName: string;
  phone?: string;
  email?: string;
  enabled: boolean;
  roles: string[];
};
export type EmployeeContract = {
  id: string;
  employeeId: string;
  contractNo: string;
  contractType: string;
  signDate?: string;
  startDate: string;
  endDate?: string;
  probationEndDate?: string;
  status: string;
  attachments: Attachment[];
  remark?: string;
  daysLeft?: number;
};
export type PersonnelCertificate = {
  id: string;
  employeeId: string;
  employeeName: string;
  name: string;
  type?: string;
  certificateNo?: string;
  specialty?: string;
  companyRegistered: boolean;
  issueDate?: string;
  validTo?: string;
  reviewDate?: string;
  availableForTender: boolean;
  manualStatus: string;
  locked: boolean;
  attachments: Attachment[];
  remark?: string;
  status: QualificationStatus;
  daysLeft?: number;
};
export type EmployeeDetail = {
  employee: QualificationEmployee;
  contracts: EmployeeContract[];
  certificates: PersonnelCertificate[];
} & {
  name?: string;
  workNo?: string;
  employmentStatus?: string;
  organizationName?: string;
  position?: string;
  phone?: string;
  entryDate?: string;
  idCard?: string;
  department?: string;
};
export type QualificationPerformance = {
  id: string;
  subjectCompany: string;
  name: string;
  clientName?: string;
  contractNo?: string;
  contractDate?: string;
  contractAmount?: string;
  projectType?: string;
  attachments: Attachment[];
  remark?: string;
};
export type QualificationWarning = {
  sourceType: string;
  sourceId: string;
  sourceName: string;
  warningType: string;
  title: string;
  dueDate: string;
  daysLeft: number;
  level: "INFO" | "WARNING" | "DANGER";
  status: "PENDING" | "OVERDUE";
};
export type QualificationDashboard = {
  companyQualificationCount: number;
  employeeCount: number;
  certificateCount: number;
  tenderAvailableCertificateCount: number;
  pendingWarningCount: number;
  expiredCount: number;
  companyCategoryDistribution: Array<{ name: string; count: number }>;
  certificateSpecialtyDistribution: Array<{ name: string; count: number }>;
  recentWarnings: QualificationWarning[];
};
export type QualificationReferences = {
  subjectCompanies: string[];
  qualificationCategories: string[];
  certificateTypes: string[];
  specialties: string[];
  projectTypes: string[];
  employees: Array<{ id: string; name: string; workNo?: string }>;
  organizations: Array<{
    id: string;
    name: string;
    fullPath: string;
    enabled: boolean;
  }>;
};
export type TenderEmployee = {
  employeeId: string;
  employeeName: string;
  workNo?: string;
  department?: string;
  position?: string;
  certificates: PersonnelCertificate[];
};

export type CompanyQualificationPayload = Omit<
  CompanyQualification,
  "id" | "status" | "daysLeft"
>;
export type EmployeePayload = Omit<
  QualificationEmployee,
  | "id"
  | "systemUserId"
  | "account"
  | "certificateCount"
  | "validCertificateCount"
> & { systemUserId?: string | null };
export type EmployeeContractPayload = Omit<
  EmployeeContract,
  "id" | "employeeId" | "daysLeft"
>;
export type CertificatePayload = Omit<
  PersonnelCertificate,
  "id" | "employeeName" | "status" | "daysLeft"
>;
export type PerformancePayload = Omit<QualificationPerformance, "id">;

export function getQualificationDashboard() {
  return request<QualificationDashboard>({
    method: "GET",
    url: "/qualifications/dashboard",
  });
}
export function getQualificationReferences() {
  return request<QualificationReferences>({
    method: "GET",
    url: "/qualifications/references",
  });
}
export function listCompanyQualifications(params?: Record<string, unknown>) {
  return request<CompanyQualification[]>({
    method: "GET",
    url: "/qualifications/companies",
    params,
  });
}
export function createCompanyQualification(data: CompanyQualificationPayload) {
  return request<CompanyQualification>({
    method: "POST",
    url: "/qualifications/companies",
    data,
  });
}
export function updateCompanyQualification(
  id: string,
  data: CompanyQualificationPayload,
) {
  return request<CompanyQualification>({
    method: "PUT",
    url: `/qualifications/companies/${id}`,
    data,
  });
}
export function deleteCompanyQualification(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/qualifications/companies/${id}`,
  });
}
export function listQualificationEmployees(params?: Record<string, unknown>) {
  return request<QualificationEmployee[]>({
    method: "GET",
    url: "/qualifications/employees",
    params,
  });
}
export function getQualificationEmployee(id: string) {
  return request<EmployeeDetail>({
    method: "GET",
    url: `/qualifications/employees/${id}`,
  });
}
export function createQualificationEmployee(data: EmployeePayload) {
  return request<QualificationEmployee>({
    method: "POST",
    url: "/qualifications/employees",
    data,
  });
}
export function updateQualificationEmployee(id: string, data: EmployeePayload) {
  return request<QualificationEmployee>({
    method: "PUT",
    url: `/qualifications/employees/${id}`,
    data,
  });
}
export function deleteQualificationEmployee(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/qualifications/employees/${id}`,
  });
}
export function listEmployeeContracts(employeeId: string) {
  return request<EmployeeContract[]>({
    method: "GET",
    url: `/qualifications/employees/${employeeId}/contracts`,
  });
}
export function createEmployeeContract(
  employeeId: string,
  data: EmployeeContractPayload,
) {
  return request<EmployeeContract>({
    method: "POST",
    url: `/qualifications/employees/${employeeId}/contracts`,
    data,
  });
}
export function updateEmployeeContract(
  id: string,
  data: EmployeeContractPayload,
) {
  return request<EmployeeContract>({
    method: "PUT",
    url: `/qualifications/employee-contracts/${id}`,
    data,
  });
}
export function deleteEmployeeContract(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/qualifications/employee-contracts/${id}`,
  });
}
export function listPersonnelCertificates(params?: Record<string, unknown>) {
  return request<PersonnelCertificate[]>({
    method: "GET",
    url: "/qualifications/certificates",
    params,
  });
}
export function createPersonnelCertificate(data: CertificatePayload) {
  return request<PersonnelCertificate>({
    method: "POST",
    url: "/qualifications/certificates",
    data,
  });
}
export function updatePersonnelCertificate(
  id: string,
  data: CertificatePayload,
) {
  return request<PersonnelCertificate>({
    method: "PUT",
    url: `/qualifications/certificates/${id}`,
    data,
  });
}
export function deletePersonnelCertificate(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/qualifications/certificates/${id}`,
  });
}
export function listQualificationPerformances(
  params?: Record<string, unknown>,
) {
  return request<QualificationPerformance[]>({
    method: "GET",
    url: "/qualifications/performances",
    params,
  });
}
export function createQualificationPerformance(data: PerformancePayload) {
  return request<QualificationPerformance>({
    method: "POST",
    url: "/qualifications/performances",
    data,
  });
}
export function updateQualificationPerformance(
  id: string,
  data: PerformancePayload,
) {
  return request<QualificationPerformance>({
    method: "PUT",
    url: `/qualifications/performances/${id}`,
    data,
  });
}
export function deleteQualificationPerformance(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/qualifications/performances/${id}`,
  });
}
export function searchTenderQualifications(params: {
  keyword?: string;
  specialties: string[];
  registeredOnly: boolean;
  availableOnly: boolean;
}) {
  return request<TenderEmployee[]>({
    method: "GET",
    url: "/qualifications/tender",
    params: { ...params, specialties: params.specialties.join(",") },
  });
}
export function listQualificationWarnings() {
  return request<QualificationWarning[]>({
    method: "GET",
    url: "/qualifications/warnings",
  });
}
export function uploadQualificationAttachment(
  file: File,
  operatorName: string,
) {
  const form = new FormData();
  form.append("file", file);
  form.append("operatorName", operatorName);
  return request<Attachment>({
    method: "POST",
    url: "/qualifications/attachments",
    data: form,
  });
}

export async function getQualificationAttachmentObjectUrl(dataUrl: string) {
  const url = dataUrl.startsWith("/qualification-files/")
    ? "/api" + dataUrl
    : dataUrl;
  const response = await http.get<Blob>(url, {
    baseURL: "",
    responseType: "blob",
  });
  return URL.createObjectURL(response.data);
}
