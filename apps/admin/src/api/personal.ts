import { request } from "./http";
import type { Attachment } from "./qualification";

export type PersonalAccount = {
  id: string;
  username: string;
  displayName: string;
  phone?: string;
  email?: string;
  organizationName?: string;
  enabled: boolean;
};

export type PersonalEmployee = {
  id: string;
  name: string;
  workNo?: string;
  organizationName?: string;
  organizationPath?: string;
  position?: string;
  entryDate?: string;
  employmentStatus: string;
};

export type MyCertificate = {
  id: string;
  name: string;
  type?: string;
  certificateNo?: string;
  specialty?: string;
  issueDate?: string;
  validTo?: string;
  reviewDate?: string;
  companyRegistered: boolean;
  availableForTender: boolean;
  locked: boolean;
  status: string;
  daysLeft?: number;
  attachments: Attachment[];
  remark?: string;
};

export type MyContract = {
  id: string;
  contractNo: string;
  contractType: string;
  signDate?: string;
  startDate: string;
  endDate?: string;
  probationEndDate?: string;
  status: string;
  attachments: Attachment[];
  remark?: string;
};

export type PersonalOverview = {
  account: PersonalAccount;
  employee?: PersonalEmployee;
  certificates: MyCertificate[];
  contracts: MyContract[];
};

export type MyCertificatePayload = {
  name: string;
  type?: string;
  certificateNo?: string;
  specialty?: string;
  issueDate?: string;
  validTo?: string;
  reviewDate?: string;
  attachments: Attachment[];
  remark?: string;
};

export function getPersonalOverviewApi() {
  return request<PersonalOverview>({ method: "GET", url: "/personal" });
}

export function updateMyProfileApi(data: { displayName: string; phone?: string; email?: string }) {
  return request<PersonalAccount>({ method: "PUT", url: "/personal/profile", data });
}

export function changeMyPasswordApi(data: { currentPassword: string; newPassword: string }) {
  return request<void>({ method: "PUT", url: "/personal/password", data });
}

export function createMyCertificateApi(data: MyCertificatePayload) {
  return request<MyCertificate>({ method: "POST", url: "/personal/certificates", data });
}

export function updateMyCertificateApi(id: string, data: MyCertificatePayload) {
  return request<MyCertificate>({ method: "PUT", url: `/personal/certificates/${id}`, data });
}

export function deleteMyCertificateApi(id: string) {
  return request<void>({ method: "DELETE", url: `/personal/certificates/${id}` });
}

export function uploadMyAttachmentApi(file: File) {
  const form = new FormData();
  form.append("file", file);
  return request<Attachment>({ method: "POST", url: "/personal/attachments", data: form });
}
