import { request } from "./http";

export type WorkOrderStatus =
  | "CREATED"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "ACCEPTED"
  | "CANCELLED";

export interface Dashboard {
  open: number;
  closed: number;
  urgent: number;
  equipmentCount: number;
}
export interface ReferenceOption {
  id: string;
  name: string;
  code?: string;
}
export interface References {
  customers: ReferenceOption[];
  equipment: ReferenceOption[];
  contracts: ReferenceOption[];
}
export interface Assignee {
  id: string;
  displayName: string;
}
export interface WorkOrder {
  id: string;
  code: string;
  title: string;
  description?: string;
  customerId?: string;
  customerName?: string;
  equipmentId?: string;
  equipmentName?: string;
  workType: string;
  priority: string;
  source: string;
  status: WorkOrderStatus;
  assigneeId?: string;
  assigneeName?: string;
  plannedDate?: string;
  siteAddress?: string;
  checkInAt?: string;
  checkInLocation?: string;
  completedAt?: string;
  billableAmount?: number;
  costAmount?: number;
}
export interface Equipment {
  id: string;
  code: string;
  name: string;
  customerId: string;
  customerName?: string;
  contractId?: string;
  requiredCertificate?: string;
  category: string;
  model?: string;
  serialNo?: string;
  siteAddress: string;
  installedDate?: string;
  warrantyEndDate?: string;
  maintenanceCycleDays: number;
  lastMaintenanceDate?: string;
  nextMaintenanceDate?: string;
  status: string;
  orderCount: number;
}
export interface EquipmentPayload {
  customerId: string;
  contractId?: string;
  code?: string;
  name: string;
  category: string;
  model?: string;
  serialNo?: string;
  siteAddress: string;
  installedDate?: string;
  warrantyEndDate?: string;
  maintenanceCycleDays: number;
  nextMaintenanceDate?: string;
  requiredCertificate?: string;
  notes?: string;
}
export interface Plan {
  id: string;
  code: string;
  assetId: string;
  assetName: string;
  name: string;
  description?: string;
  workType: string;
  priority: string;
  cycleDays: number;
  autoGenerate: boolean;
  nextRunDate: string;
  enabled: boolean;
}
export interface PlanPayload {
  assetId: string;
  name: string;
  description?: string;
  workType: string;
  priority: string;
  cycleDays: number;
  autoGenerate: boolean;
  nextRunDate: string;
}
export interface Certificate {
  id: string;
  userId: string;
  employeeName: string;
  certificateType: string;
  certificateNo: string;
  issueDate?: string;
  expiryDate: string;
  issuingAuthority?: string;
  remark?: string;
  daysUntilExpiry: number;
}
export interface Schedule {
  id: string;
  orderId: string;
  orderCode: string;
  title: string;
  engineerName?: string;
  scheduledAt?: string;
  checkInAt?: string;
  checkInLocation?: string;
  startedAt?: string;
  completedAt?: string;
  status: WorkOrderStatus;
}
export interface Attendance {
  id: string;
  orderId: string;
  orderCode: string;
  engineerId?: string;
  engineerName?: string;
  checkInAt: string;
  checkInLocation?: string;
  checkOutAt?: string;
}

export const getMaintenanceDashboard = () =>
  request<Dashboard>({ method: "GET", url: "/maintenance/dashboard" });
export const getMaintenanceReferences = () =>
  request<References>({ method: "GET", url: "/maintenance/references" });
export const listAssignees = () =>
  request<Assignee[]>({ method: "GET", url: "/maintenance/mobile/assignees" });
export const listWorkOrders = () =>
  request<WorkOrder[]>({ method: "GET", url: "/maintenance/work-orders" });
export const createWorkOrder = (data: Record<string, unknown>) =>
  request<WorkOrder>({ method: "POST", url: "/maintenance/work-orders", data });
export const assignWorkOrder = (
  id: string,
  data: { assigneeId: string; assigneeName: string },
) =>
  request<WorkOrder>({
    method: "PUT",
    url: `/maintenance/work-orders/${id}/assign`,
    data,
  });
export const acceptWorkOrder = (
  id: string,
  data: { actualCost?: number; remarks?: string },
) =>
  request<WorkOrder>({
    method: "PUT",
    url: `/maintenance/work-orders/${id}/accept`,
    data,
  });
export const listEquipment = () =>
  request<Equipment[]>({ method: "GET", url: "/maintenance/equipment" });
export const createEquipment = (data: EquipmentPayload) =>
  request<Equipment>({ method: "POST", url: "/maintenance/equipment", data });
export const updateEquipment = (id: string, data: EquipmentPayload) =>
  request<Equipment>({
    method: "PUT",
    url: `/maintenance/equipment/${id}`,
    data,
  });
export const listPlans = () =>
  request<Plan[]>({ method: "GET", url: "/maintenance/plans" });
export const createPlan = (data: PlanPayload) =>
  request<Plan>({ method: "POST", url: "/maintenance/plans", data });
export const updatePlan = (id: string, data: PlanPayload) =>
  request<Plan>({ method: "PUT", url: `/maintenance/plans/${id}`, data });
export const setPlanEnabled = (id: string, enabled: boolean) =>
  request<Plan>({
    method: "PUT",
    url: `/maintenance/plans/${id}/enabled`,
    params: { enabled },
  });
export const generatePlans = (planId?: string) =>
  request<{ generated: number }>({
    method: "POST",
    url: "/maintenance/plans/generate",
    data: planId ? { planId } : undefined,
  });
export const listCertificates = () =>
  request<Certificate[]>({ method: "GET", url: "/maintenance/certificates" });
export const createCertificate = (data: Record<string, unknown>) =>
  request<Certificate>({
    method: "POST",
    url: "/maintenance/certificates",
    data,
  });
export const deleteCertificate = (id: string) =>
  request<void>({ method: "DELETE", url: `/maintenance/certificates/${id}` });
export const listSchedules = () =>
  request<Schedule[]>({ method: "GET", url: "/maintenance/schedules" });
export const createSchedule = (data: {
  orderId: string;
  engineerId: string;
  scheduledAt: string;
}) =>
  request<Schedule>({ method: "POST", url: "/maintenance/schedules", data });
export const listAttendance = () =>
  request<Attendance[]>({ method: "GET", url: "/maintenance/attendance" });
