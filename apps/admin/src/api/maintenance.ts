import { request } from "./http";

export type EquipmentStatus = "ACTIVE" | "MAINTENANCE_DUE" | "OUT_OF_SERVICE" | "RETIRED";
export type WorkOrderSource = "MAINTENANCE_PLAN" | "CUSTOMER_REPAIR" | "PROJECT" | "EQUIPMENT_INSPECTION" | "MANUAL";
export type WorkOrderType = "INSPECTION" | "REPAIR" | "REPLACEMENT" | "ANNUAL_TEST" | "MODIFICATION" | "ON_SITE_SERVICE";
export type WorkOrderPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";
export type WorkOrderStatus = "CREATED" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "ACCEPTED" | "CANCELLED";
export type ScheduleStatus = "AVAILABLE" | "ON_DUTY" | "LEAVE";

export type MaintenanceDashboard = {
  equipmentCount: number; dueEquipmentCount: number; openWorkOrders: number; overdueWorkOrders: number;
  expiringCertificates: number; monthCost: number; monthBillable: number;
};
export type MaintenanceReferences = {
  customers: Array<{ id: string; code: string; name: string }>;
  contracts: Array<{ id: string; customerId: string; code: string; projectName: string }>;
  parts: Array<{ id: string; code: string; name: string; model?: string; stockQty: number; unitCost: number }>;
  users: Array<{ id: string; displayName: string; enabled: boolean }>;
};
export type Equipment = {
  id: string; customerId: string; customerName: string; contractId?: string; contractCode?: string;
  code: string; name: string; category: string; model?: string; serialNo?: string; siteAddress: string;
  installedDate?: string; warrantyEndDate?: string; maintenanceCycleDays: number; lastMaintenanceDate?: string;
  nextMaintenanceDate?: string; status: EquipmentStatus; requiredCertificate?: string; notes?: string;
  workOrderCount: number; faultCount: number;
};
export type MaintenancePlan = {
  id: string; code: string; assetId: string; assetCode: string; assetName: string; contractId?: string;
  contractCode?: string; planName: string; cycleDays: number; nextDueDate: string; lastGeneratedDate?: string;
  active: boolean; due: boolean;
};
export type WorkOrderMaterial = { id: string; partId: string; partName: string; quantity: number; unitCost: number; amount: number };
export type WorkOrderStatusLog = { id: string; fromStatus?: WorkOrderStatus; toStatus: WorkOrderStatus; operatorName: string; remark?: string; createdAt: string };
export type WorkOrder = {
  id: string; code: string; source: WorkOrderSource; workType: WorkOrderType; priority: WorkOrderPriority;
  status: WorkOrderStatus; title: string; customerId: string; customerName: string; contractId?: string;
  contractCode?: string; projectId?: string; equipmentId?: string; equipmentCode?: string; equipmentName?: string;
  plannedDate: string; siteAddress: string; problemDescription: string; assigneeId?: string; engineerName?: string;
  requiredCertificate?: string; checkInAt?: string; checkInLocation?: string; startedAt?: string; completedAt?: string;
  acceptedAt?: string; customerSigner?: string; serviceResult?: string; acceptanceNote?: string;
  laborHours: number; laborCost: number; materialCost: number; travelCost: number; outsourcingCost: number;
  costAmount: number; billableAmount: number; freeWarranty: boolean; materials: WorkOrderMaterial[]; statusLogs: WorkOrderStatusLog[];
};
export type EmployeeOption = { id: string; displayName: string; validCertificates: string[]; availableOnPlannedDate: boolean };
export type EmployeeCertificate = { id: string; userId: string; employeeName: string; certificateType: string; certificateNo: string; issueDate?: string; expiryDate: string; issuingAuthority?: string; expired: boolean; expiringSoon: boolean };
export type FieldSchedule = { id: string; userId: string; employeeName: string; workDate: string; shiftName: string; siteName?: string; status: ScheduleStatus };
export type FieldAttendance = { id: string; userId: string; employeeName: string; workOrderId: string; workOrderCode: string; checkInAt: string; checkOutAt?: string; checkInLocation: string; checkOutLocation?: string };

export function getMaintenanceDashboard() { return request<MaintenanceDashboard>({ method: "GET", url: "/maintenance/dashboard" }); }
export function getMaintenanceReferences() { return request<MaintenanceReferences>({ method: "GET", url: "/maintenance/references" }); }
export function listEquipment() { return request<Equipment[]>({ method: "GET", url: "/maintenance/equipment" }); }
export function createEquipment(data: Omit<Equipment, "id" | "customerName" | "contractCode" | "status" | "lastMaintenanceDate" | "workOrderCount" | "faultCount"> & { createPlan: boolean }) { return request<Equipment>({ method: "POST", url: "/maintenance/equipment", data }); }
export function listMaintenancePlans() { return request<MaintenancePlan[]>({ method: "GET", url: "/maintenance/plans" }); }
export function createMaintenancePlan(data: { code: string; assetId: string; planName: string; cycleDays: number; nextDueDate: string }) { return request<MaintenancePlan>({ method: "POST", url: "/maintenance/plans", data }); }
export function generateMaintenanceWorkOrders(data: { throughDate: string; operatorName: string }) { return request<{ generatedCount: number; workOrderCodes: string[] }>({ method: "POST", url: "/maintenance/plans/generate", data }); }
export function listWorkOrders() { return request<WorkOrder[]>({ method: "GET", url: "/maintenance/work-orders" }); }
export function getWorkOrder(id: string) { return request<WorkOrder>({ method: "GET", url: `/maintenance/work-orders/${id}` }); }
export function createWorkOrder(data: { code: string; source: WorkOrderSource; workType: WorkOrderType; priority: WorkOrderPriority; title: string; customerId: string; contractId?: string; projectId?: string; equipmentId?: string; plannedDate: string; siteAddress: string; problemDescription: string; billableAmount: number; freeWarranty: boolean; operatorName: string }) { return request<WorkOrder>({ method: "POST", url: "/maintenance/work-orders", data }); }
export function listEligibleEmployees(id: string) { return request<EmployeeOption[]>({ method: "GET", url: `/maintenance/work-orders/${id}/eligible-employees` }); }
export function assignWorkOrder(id: string, data: { assigneeId: string; operatorName: string }) { return request<WorkOrder>({ method: "POST", url: `/maintenance/work-orders/${id}/assign`, data }); }
export function checkInWorkOrder(id: string, data: { location: string; operatorName: string }) { return request<WorkOrder>({ method: "POST", url: `/maintenance/work-orders/${id}/check-in`, data }); }
export function completeWorkOrder(id: string, data: { serviceResult: string; laborHours: number; laborCost: number; travelCost: number; outsourcingCost: number; materials: Array<{ partId: string; quantity: number }>; operatorName: string }) { return request<WorkOrder>({ method: "POST", url: `/maintenance/work-orders/${id}/complete`, data }); }
export function acceptWorkOrder(id: string, data: { customerSigner: string; acceptanceNote: string; operatorName: string }) { return request<WorkOrder>({ method: "POST", url: `/maintenance/work-orders/${id}/accept`, data }); }
export function listCertificates() { return request<EmployeeCertificate[]>({ method: "GET", url: "/maintenance/certificates" }); }
export function createCertificate(data: { userId: string; certificateType: string; certificateNo: string; issueDate?: string; expiryDate: string; issuingAuthority?: string }) { return request<EmployeeCertificate>({ method: "POST", url: "/maintenance/certificates", data }); }
export function listSchedules() { return request<FieldSchedule[]>({ method: "GET", url: "/maintenance/schedules" }); }
export function createSchedule(data: { userId: string; workDate: string; shiftName: string; siteName?: string; status: ScheduleStatus }) { return request<FieldSchedule>({ method: "POST", url: "/maintenance/schedules", data }); }
export function listAttendance() { return request<FieldAttendance[]>({ method: "GET", url: "/maintenance/attendance" }); }
