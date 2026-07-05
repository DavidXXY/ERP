import { request } from "./http";

export type CurrentUser = {
  id: string;
  username: string;
  displayName: string;
  roleCodes: string[];
  permissions: string[];
};

export type LoginResponse = {
  token: string;
  user: CurrentUser;
};

export function loginApi(payload: { username: string; password: string }) {
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

