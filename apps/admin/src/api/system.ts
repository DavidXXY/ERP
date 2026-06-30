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
  childCount: number;
  children?: OrganizationResponse[];
  createdAt?: string;
  updatedAt?: string;
};

export type CreateOrganizationRequest = {
  code: string;
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
