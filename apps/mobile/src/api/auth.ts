import type { CurrentUser, LoginResponse } from "@/types/domain";
import { request } from "@/utils/http";

export const loginApi = (username: string, password: string) => request<LoginResponse>({ url: "/auth/login", method: "POST", data: { username, password } });
export const currentUserApi = () => request<CurrentUser>({ url: "/auth/me" });
export const wechatLoginApi = (code: string) => request<LoginResponse>({ url: "/auth/wechat/login", method: "POST", data: { code } });
export const bindWechatApi = (code: string, username: string, password: string) => request<LoginResponse>({ url: "/auth/wechat/bind", method: "POST", data: { code, username, password } });
export const bindCurrentWechatApi = (code: string) => request<void>({ url: "/auth/wechat/bind-current", method: "POST", data: { code } });
