import { request } from "./http";

export type UserResponse = {
  id: string;
  orgId?: string;
  username: string;
  displayName: string;
  phone?: string;
  email?: string;
  enabled: boolean;
  roles: { id: string; code: string; name: string }[];
  createdAt?: string;
  updatedAt?: string;
};

export type ApprovalConfigResponse = {
  id: string;
  flowCode: string;
  flowName: string;
  assigneeType: "USER" | "ROLE" | "DYNAMIC" | "AUTO";
  userId?: string;
  roleId?: string;
  assigneeName: string;
  versionNo: number;
  dynamicAssignee?: string;
  autoAction?: string;
  slaHours?: number;
  escalationRoleId?: string;
  escalationRoleName?: string;
  stepPolicy?: "ANY_APPROVE" | "ALL_APPROVE" | "MAJORITY_APPROVE";
  publishStatus?: "DRAFT" | "PUBLISHED";
  approvalMode: "PARALLEL" | "SEQUENTIAL";
  sequenceNo: number;
  conditionType: "ANY" | "AMOUNT" | "DEPARTMENT" | "AMOUNT_AND_DEPARTMENT" | "BUSINESS_TYPE" | "PROJECT" | "SUPPLIER_RISK" | "CUSTOMER_LEVEL" | "COMPOSITE";
  minAmount?: number;
  maxAmount?: number;
  departmentName?: string;
  businessType?: string;
  projectCode?: string;
  supplierRisk?: string;
  customerLevel?: string;
  priority: number;
  remark?: string;
  enabled: boolean;
};

export type ApprovalFlowPreview = {
  flowCode: string;
  flowName: string;
  approvalMode?: "PARALLEL" | "SEQUENTIAL";
  totalSteps: number;
  versionNo: number;
  ruleText: string;
  steps: Array<{ stepNo: number; assignees: string[]; conditions: string[]; slaHours?: number; escalationRoleName?: string; autoApproved: boolean }>;
};

export type ApprovalFlowDiagnostic = { flowCode: string; flowName: string; severity: "HIGH" | "MEDIUM" | "LOW"; message: string };

type ApprovalAssigneePayload = { flowCode: string; flowName: string; assigneeType: "USER" | "ROLE" | "DYNAMIC" | "AUTO"; userId?: string; roleId?: string; dynamicAssignee?: string; autoAction?: string; slaHours?: number; escalationRoleId?: string; stepPolicy?: ApprovalConfigResponse["stepPolicy"]; approvalMode: "PARALLEL" | "SEQUENTIAL"; sequenceNo: number; conditionType?: ApprovalConfigResponse["conditionType"]; minAmount?: number; maxAmount?: number; departmentName?: string; businessType?: string; projectCode?: string; supplierRisk?: string; customerLevel?: string; priority?: number; remark?: string };
export type ApprovalFlowVersion = { flowCode: string; flowName: string; versionNo: number; ruleCount: number; publishStatus: string };

export function listApprovalConfigs() {
  return request<ApprovalConfigResponse[]>({ method: "GET", url: "/system/approval-configs" });
}

export function previewApprovalFlow(data: { flowCode: string; amount?: number; departmentName?: string; businessType?: string; projectCode?: string; supplierRisk?: string; customerLevel?: string }) {
  return request<ApprovalFlowPreview>({ method: "POST", url: "/system/approval-configs/preview", data });
}

export function batchPreviewApprovalFlows(items: Array<{ flowCode: string; amount?: number; departmentName?: string; businessType?: string; projectCode?: string; supplierRisk?: string; customerLevel?: string }>) {
  return request<ApprovalFlowPreview[]>({ method: "POST", url: "/system/approval-configs/batch-preview", data: { items } });
}

export function listApprovalDiagnostics() {
  return request<ApprovalFlowDiagnostic[]>({ method: "GET", url: "/system/approval-configs/diagnostics" });
}

export function copyApprovalFlow(data: { sourceFlowCode: string; targetFlowCode: string; targetFlowName: string; overwrite: boolean }) {
  return request<ApprovalConfigResponse[]>({ method: "POST", url: "/system/approval-configs/copy", data });
}

export function listApprovalFlowVersions(flowCode: string) {
  return request<ApprovalFlowVersion[]>({ method: "GET", url: `/system/approval-configs/${flowCode}/versions` });
}

export function publishApprovalFlow(flowCode: string) {
  return request<ApprovalConfigResponse[]>({ method: "POST", url: `/system/approval-configs/${flowCode}/publish` });
}

export function rollbackApprovalFlow(flowCode: string, versionNo: number) {
  return request<ApprovalConfigResponse[]>({ method: "POST", url: `/system/approval-configs/${flowCode}/rollback/${versionNo}` });
}

export function createApprovalConfig(data: ApprovalAssigneePayload) {
  return request<ApprovalConfigResponse>({ method: "POST", url: "/system/approval-configs", data });
}

export function updateApprovalConfig(id: string, data: ApprovalAssigneePayload & { enabled?: boolean }) {
  return request<ApprovalConfigResponse>({ method: "PUT", url: `/system/approval-configs/${id}`, data });
}

export function deleteApprovalConfig(id: string) {
  return request<void>({ method: "DELETE", url: `/system/approval-configs/${id}` });
}

export type CreateUserRequest = {
  orgId?: string;
  username: string;
  displayName: string;
  password: string;
  phone?: string;
  email?: string;
  roleIds?: string[];
};

export type UpdateUserRequest = {
  orgId?: string;
  displayName?: string;
  phone?: string;
  email?: string;
  enabled?: boolean;
  roleIds?: string[];
};

export type RoleResponse = {
  id: string;
  code: string;
  name: string;
  dataScope: string;
  permissions: { id: string; code: string; name: string; module: string }[];
  dataOrganizations: { id: string; code: string; name: string }[];
  userCount: number;
  builtIn: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type CreateRoleRequest = {
  code: string;
  name: string;
  dataScope: string;
  permissionIds?: string[];
  dataOrganizationIds?: string[];
};

export type UpdateRoleRequest = {
  name?: string;
  dataScope?: string;
  permissionIds?: string[];
  dataOrganizationIds?: string[];
};

export type PermissionResponse = {
  id: string;
  code: string;
  name: string;
  module: string;
  roleCount: number;
  builtIn: boolean;
  createdAt?: string;
};

export type CreatePermissionRequest = {
  code: string;
  name: string;
  module: string;
};

export type UpdatePermissionRequest = {
  name?: string;
  module?: string;
};

export type OrganizationResponse = {
  id: string;
  code: string;
  name: string;
  type: string;
  sortOrder: number;
  parentId?: string;
  parentName?: string;
  fullPath: string;
  leaderName?: string;
  phone?: string;
  enabled: boolean;
  description?: string;
  directUserCount: number;
  totalUserCount: number;
  directEmployeeCount: number;
  totalEmployeeCount: number;
  childCount: number;
  children?: OrganizationResponse[];
  createdAt?: string;
  updatedAt?: string;
};

export type CreateOrganizationRequest = {
  code?: string;
  name: string;
  type?: string;
  sortOrder?: number;
  parentId?: string;
  leaderName?: string;
  phone?: string;
  enabled?: boolean;
  description?: string;
};

export type UpdateOrganizationRequest = {
  name?: string;
  type?: string;
  sortOrder?: number;
  parentId?: string;
  leaderName?: string;
  phone?: string;
  enabled?: boolean;
  description?: string;
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

// User APIs
export function listUsersApi(page: number, size: number = 20) {
  return request<PageResponse<UserResponse>>({
    method: "GET",
    url: `/users?page=${page}&size=${size}`,
  });
}

export function listUserOptionsApi() {
  return request<UserResponse[]>({
    method: "GET",
    url: "/users/options",
  });
}

export function getUserApi(id: string) {
  return request<UserResponse>({
    method: "GET",
    url: `/users/${id}`,
  });
}

export function createUserApi(data: CreateUserRequest) {
  return request<UserResponse>({
    method: "POST",
    url: "/users",
    data,
  });
}

export function updateUserApi(id: string, data: UpdateUserRequest) {
  return request<UserResponse>({
    method: "PUT",
    url: `/users/${id}`,
    data,
  });
}

export function deleteUserApi(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/users/${id}`,
  });
}

export function resetPasswordApi(id: string, newPassword: string) {
  return request<void>({
    method: "POST",
    url: `/users/${id}/reset-password`,
    data: { newPassword },
  });
}

// Role APIs
export function listRolesApi(page: number, size: number = 20) {
  return request<PageResponse<RoleResponse>>({
    method: "GET",
    url: `/roles?page=${page}&size=${size}`,
  });
}

export function getRoleApi(id: string) {
  return request<RoleResponse>({
    method: "GET",
    url: `/roles/${id}`,
  });
}

export function createRoleApi(data: CreateRoleRequest) {
  return request<RoleResponse>({
    method: "POST",
    url: "/roles",
    data,
  });
}

export function updateRoleApi(id: string, data: UpdateRoleRequest) {
  return request<RoleResponse>({
    method: "PUT",
    url: `/roles/${id}`,
    data,
  });
}

export function deleteRoleApi(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/roles/${id}`,
  });
}

// Permission APIs
export function listPermissionsApi() {
  return request<PermissionResponse[]>({
    method: "GET",
    url: "/permissions",
  });
}

export function getPermissionApi(id: string) {
  return request<PermissionResponse>({
    method: "GET",
    url: `/permissions/${id}`,
  });
}

export function createPermissionApi(data: CreatePermissionRequest) {
  return request<PermissionResponse>({
    method: "POST",
    url: "/permissions",
    data,
  });
}

export function updatePermissionApi(id: string, data: UpdatePermissionRequest) {
  return request<PermissionResponse>({
    method: "PUT",
    url: `/permissions/${id}`,
    data,
  });
}

export function deletePermissionApi(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/permissions/${id}`,
  });
}

// Organization APIs
export function listOrganizationsApi() {
  return request<OrganizationResponse[]>({
    method: "GET",
    url: "/organizations",
  });
}

export function listOrganizationsFlatApi() {
  return request<OrganizationResponse[]>({
    method: "GET",
    url: "/organizations/flat",
  });
}

export function getOrganizationApi(id: string) {
  return request<OrganizationResponse>({
    method: "GET",
    url: `/organizations/${id}`,
  });
}

export function createOrganizationApi(data: CreateOrganizationRequest) {
  return request<OrganizationResponse>({
    method: "POST",
    url: "/organizations",
    data,
  });
}

export function updateOrganizationApi(id: string, data: UpdateOrganizationRequest) {
  return request<OrganizationResponse>({
    method: "PUT",
    url: `/organizations/${id}`,
    data,
  });
}

export function deleteOrganizationApi(id: string) {
  return request<void>({
    method: "DELETE",
    url: `/organizations/${id}`,
  });
}

// --- System Health ---

export type SystemHealthResponse = {
  application?: {
    appName: string;
    version: string;
    buildTime: string;
    activeProfiles: string;
    storageType: string;
  };
  dependencies?: {
    databaseUrl: string;
    databaseDriver: string;
    redisEndpoint: string;
    storageType: string;
    localStoragePath: string;
    tempDir: string;
    workingDir: string;
  };
  operatingSystem: {
    name: string;
    version: string;
    architecture: string;
    availableProcessors: number;
    systemLoadAverage: number;
    processCpuLoad?: number;
    systemCpuLoad?: number;
  };
  cpu: {
    availableProcessors: number;
    systemLoadAverage: number;
    processCpuLoad: number;
    systemCpuLoad: number;
  };
  memory: {
    heap: { init: number; used: number; committed: number; max: number };
    nonHeap: { init: number; used: number; committed: number; max: number };
    totalPhysicalMemory?: number;
    freePhysicalMemory?: number;
  };
  jvm: {
    javaVersion: string;
    javaVendor: string;
    jvmName: string;
    jvmVersion: string;
    jvmVendor: string;
    uptime: number;
    inputArguments: string[];
    startTime: number;
  };
  disk: {
    path: string;
    totalSpace: number;
    freeSpace: number;
    usableSpace: number;
  }[];
};

export function getSystemHealthApi() {
  return request<SystemHealthResponse>({
    method: "GET",
    url: "/system/health",
  });
}

// ====== Global Search ======
export type SearchResult = {
  type: string; id: string; title: string; subtitle: string; url: string;
};
export function searchGlobal(q: string) {
  return request<SearchResult[]>({ method: "GET", url: "/search", params: { q } });
}

export function getSystemVersion() {
  return request<{ version: string; buildTime: string; appName: string }>({ method: "GET", url: "/system/version" });
}

export type DeletedRecord = {
  id: string;
  entityType: string;
  entityId: string;
  title: string;
  status: "PENDING" | "APPROVED";
  requestedBy?: string;
  requestedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
};

export function listDeletedRecords() {
  return request<DeletedRecord[]>({ method: "GET", url: "/system/deleted-records" });
}

export function approveDeletedRecord(id: string) {
  return request<void>({ method: "POST", url: `/system/deleted-records/${id}/approve` });
}

export function restoreDeletedRecord(id: string) {
  return request<void>({ method: "POST", url: `/system/deleted-records/${id}/restore` });
}
