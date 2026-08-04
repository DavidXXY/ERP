import { request } from "./http";

export type CurrentUser = {
  id: string;
  username: string;
  displayName: string;
  roleCodes?: string[];
  roles?: string[];
  permissions: string[];
};

export type LoginResponse = {
  token?: string;
  user?: CurrentUser;
  mfaRequired?: boolean;
};

export function loginApi(payload: {
  username: string;
  password: string;
  mfaCode?: string;
}) {
  return request<LoginResponse>({
    method: "POST",
    url: "/auth/login",
    data: payload,
  });
}

export function currentUserApi() {
  return request<CurrentUser>({
    method: "GET",
    url: "/auth/me",
  });
}
