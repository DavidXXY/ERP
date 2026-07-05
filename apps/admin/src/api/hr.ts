import { request, http } from "./http";
import type { EmployeeDetail, PersonnelCertificate } from "./qualification";

// ====== Types ======

export interface EducationRecord {
  id: string;
  employeeId: string;
  schoolName: string;
  degree: string;
  major: string;
  startDate?: string;
  endDate?: string;
  highest: boolean;
  remark?: string;
}

export interface EducationPayload {
  schoolName: string;
  degree?: string;
  major?: string;
  startDate?: string;
  endDate?: string;
  highest?: boolean;
  remark?: string;
}

export interface WorkExperienceRecord {
  id: string;
  employeeId: string;
  companyName: string;
  position?: string;
  startDate?: string;
  endDate?: string;
  current: boolean;
  description?: string;
  remark?: string;
}

export interface WorkExperiencePayload {
  companyName: string;
  position?: string;
  startDate?: string;
  endDate?: string;
  current?: boolean;
  description?: string;
  remark?: string;
}

export interface EmergencyContactRecord {
  id: string;
  employeeId: string;
  name: string;
  relationship?: string;
  phone?: string;
  address?: string;
  primary: boolean;
  remark?: string;
}

export interface EmergencyContactPayload {
  name: string;
  relationship?: string;
  phone?: string;
  address?: string;
  primary?: boolean;
  remark?: string;
}

export interface LifecycleRecord {
  id: string;
  employeeId: string;
  employeeName: string;
  lifecycleType: string;
  effectiveDate: string;
  fromOrganizationId?: string;
  fromOrganizationName?: string;
  fromPosition?: string;
  toOrganizationId?: string;
  toOrganizationName?: string;
  toPosition?: string;
  reason?: string;
  status: string;
  approvedBy?: string;
  approvedAt?: string;
  remark?: string;
}

export interface LifecyclePayload {
  lifecycleType: string;
  effectiveDate: string;
  fromOrganizationId?: string;
  fromOrganizationName?: string;
  fromPosition?: string;
  toOrganizationId?: string;
  toOrganizationName?: string;
  toPosition?: string;
  reason?: string;
  remark?: string;
}

export interface LeaveRecord {
  id: string;
  employeeId: string;
  employeeName: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason?: string;
  status: string;
  approvedBy?: string;
  approvedAt?: string;
  approvalRemark?: string;
}

export interface LeavePayload {
  leaveType: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason?: string;
}

export interface ApprovePayload {
  approved: boolean;
  remark?: string;
  operatorName: string;
}

export interface HrAnalytics {
  totalEmployees: number;
  activeEmployees: number;
  leftEmployees: number;
  newThisMonth: number;
  leavePendingCount: number;
  educationDistribution: Array<{ name: string; count: number }>;
  statusDistribution: Array<{ name: string; count: number }>;
  organizationDistribution: Array<{ name: string; count: number }>;
  recentLifecycles: Array<{ date: string; employeeName: string; type: string; detail: string }>;
}

// ====== Employee Detail Extras ======
export interface EmployeeDetailExtras {
  educations: EducationRecord[];
  workExperiences: WorkExperienceRecord[];
  emergencyContacts: EmergencyContactRecord[];
}

// ====== Education ======
export function listEducations(employeeId: string) {
  return request<EducationRecord[]>({ method: "GET", url: `/hr/employees/${employeeId}/educations` });
}
export function createEducation(employeeId: string, data: EducationPayload) {
  return request<EducationRecord>({ method: "POST", url: `/hr/employees/${employeeId}/educations`, data });
}
export function updateEducation(id: string, data: EducationPayload) {
  return request<EducationRecord>({ method: "PUT", url: `/hr/educations/${id}`, data });
}
export function deleteEducation(id: string) {
  return request<void>({ method: "DELETE", url: `/hr/educations/${id}` });
}

// ====== Work Experience ======
export function listWorkExperiences(employeeId: string) {
  return request<WorkExperienceRecord[]>({ method: "GET", url: `/hr/employees/${employeeId}/work-experiences` });
}
export function createWorkExperience(employeeId: string, data: WorkExperiencePayload) {
  return request<WorkExperienceRecord>({ method: "POST", url: `/hr/employees/${employeeId}/work-experiences`, data });
}
export function updateWorkExperience(id: string, data: WorkExperiencePayload) {
  return request<WorkExperienceRecord>({ method: "PUT", url: `/hr/work-experiences/${id}`, data });
}
export function deleteWorkExperience(id: string) {
  return request<void>({ method: "DELETE", url: `/hr/work-experiences/${id}` });
}

// ====== Emergency Contact ======
export function listEmergencyContacts(employeeId: string) {
  return request<EmergencyContactRecord[]>({ method: "GET", url: `/hr/employees/${employeeId}/emergency-contacts` });
}
export function createEmergencyContact(employeeId: string, data: EmergencyContactPayload) {
  return request<EmergencyContactRecord>({ method: "POST", url: `/hr/employees/${employeeId}/emergency-contacts`, data });
}
export function updateEmergencyContact(id: string, data: EmergencyContactPayload) {
  return request<EmergencyContactRecord>({ method: "PUT", url: `/hr/emergency-contacts/${id}`, data });
}
export function deleteEmergencyContact(id: string) {
  return request<void>({ method: "DELETE", url: `/hr/emergency-contacts/${id}` });
}

// ====== Lifecycle ======
export function listEmployeeLifecycles(employeeId: string) {
  return request<LifecycleRecord[]>({ method: "GET", url: `/hr/employees/${employeeId}/lifecycles` });
}
export function listAllLifecycles() {
  return request<LifecycleRecord[]>({ method: "GET", url: "/hr/lifecycles" });
}
export function createLifecycle(employeeId: string, data: LifecyclePayload) {
  return request<LifecycleRecord>({ method: "POST", url: `/hr/employees/${employeeId}/lifecycles`, data });
}
export function approveLifecycle(id: string, data: ApprovePayload) {
  return request<LifecycleRecord>({ method: "POST", url: `/hr/lifecycles/${id}/approve`, data });
}

// ====== Leave ======
export function listEmployeeLeaves(employeeId: string) {
  return request<LeaveRecord[]>({ method: "GET", url: `/hr/employees/${employeeId}/leaves` });
}
export function listAllLeaves() {
  return request<LeaveRecord[]>({ method: "GET", url: "/hr/leaves" });
}
export function createLeave(employeeId: string, data: LeavePayload) {
  return request<LeaveRecord>({ method: "POST", url: `/hr/employees/${employeeId}/leaves`, data });
}
export function approveLeave(id: string, data: ApprovePayload) {
  return request<LeaveRecord>({ method: "POST", url: `/hr/leaves/${id}/approve`, data });
}


// ====== Leave Balance ======
export interface LeaveBalanceRecord {
  id: string;
  employeeId: string;
  employeeName: string;
  leaveType: string;
  year: number;
  totalDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface LeaveBalancePayload {
  leaveType: string;
  year: number;
  totalDays: number;
  usedDays: number;
}

// ====== Analytics ======
export function getHrAnalytics() {
  return request<HrAnalytics>({ method: "GET", url: "/hr/analytics" });
}

// ====== Leave Balance ======
export function listEmployeeLeaveBalances(employeeId: string) {
  return request<LeaveBalanceRecord[]>({ method: "GET", url: `/hr/employees/${employeeId}/leave-balances` });
}
export function listAllLeaveBalances() {
  return request<LeaveBalanceRecord[]>({ method: "GET", url: "/hr/leave-balances" });
}
export function setLeaveBalance(employeeId: string, data: LeaveBalancePayload) {
  return request<LeaveBalanceRecord>({ method: "POST", url: `/hr/employees/${employeeId}/leave-balances`, data });
}
export function initLeaveBalances(employeeId: string) {
  return request<void>({ method: "POST", url: `/hr/employees/${employeeId}/init-leave-balances` });
}

// ====== Excel Import/Export ======
export async function exportEmployeesExcel() {
  const response = await http.get<Blob>("/hr/export/employees", { responseType: "blob" });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url; anchor.download = "hr-employees.xlsx";
  anchor.click(); URL.revokeObjectURL(url);
}
export async function downloadImportTemplate() {
  const response = await http.get<Blob>("/hr/export/template", { responseType: "blob" });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = url; anchor.download = "hr-import-template.xlsx";
  anchor.click(); URL.revokeObjectURL(url);
}
export function importEmployeesExcel(file: File, operatorName?: string) {
  const form = new FormData();
  form.append("file", file);
  if (operatorName) form.append("operatorName", operatorName);
  return request<{ success: number; fail: number; errors: string[] }>({ method: "POST", url: "/hr/import/employees", data: form });
}


// ====== Self-Service ======
export function getSelfProfile() {
  return request<EmployeeDetail>({ method: "GET", url: "/hr/self/profile" });
}
export function getSelfLeaveBalances() {
  return request<LeaveBalanceRecord[]>({ method: "GET", url: "/hr/self/leave-balances" });
}
export function getSelfLeaves() {
  return request<LeaveRecord[]>({ method: "GET", url: "/hr/self/leaves" });
}
export function createSelfLeave(data: LeavePayload) {
  return request<LeaveRecord>({ method: "POST", url: "/hr/self/leaves", data });
}

export function getSelfEducations() {
  return request<EducationRecord[]>({ method: "GET", url: "/hr/self/educations" });
}
export function getSelfWorkExperiences() {
  return request<WorkExperienceRecord[]>({ method: "GET", url: "/hr/self/work-experiences" });
}
export function getSelfEmergencyContacts() {
  return request<EmergencyContactRecord[]>({ method: "GET", url: "/hr/self/emergency-contacts" });
}
export function getSelfCertificates() {
  return request<PersonnelCertificate[]>({ method: "GET", url: "/hr/self/certificates" });
}
export function getSelfContracts() {
  return request<EmployeeContract[]>({ method: "GET", url: "/hr/self/contracts" });
}
