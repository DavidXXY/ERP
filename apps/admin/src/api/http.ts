import axios, { type AxiosRequestConfig } from "axios";

export const AUTH_TOKEN_KEY = "ops_erp_admin_token";

export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 12000,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401 || status === 403) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }
    let message = error.response?.data?.message || error.message || "接口请求失败";
    if (status === 502) {
      message = "后端 API 未启动或不可访问。请先启动 services/api 后端服务，再重新登录。";
    } else if (status === 504 || error.code === "ECONNABORTED") {
      message = "后端 API 响应超时，请检查后端服务和数据库连接。";
    } else if (!error.response) {
      message = "无法连接后端 API，请检查网络、后端服务或 VITE_API_BASE_URL 配置。";
    }
    return Promise.reject(new Error(message));
  },
);

export async function request<T>(config: AxiosRequestConfig) {
  const response = await http.request<ApiResponse<T>>(config);
  if (!response.data.success) {
    throw new Error(response.data.message || "接口处理失败");
  }
  return response.data.data;
}
