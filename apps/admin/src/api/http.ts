import axios, { type AxiosRequestConfig } from "axios";

export const AUTH_TOKEN_KEY = "ops_erp_admin_token";

export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 12000,
});

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  const url = config.url || "";
  const isLoginRequest = url.includes("/auth/login");
  if (token && !isLoginRequest) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || "";
    const isCurrentUserRequest = url.includes("/auth/me");
    if (status === 401 || (status === 400 && isCurrentUserRequest)) {
      sessionStorage.removeItem(AUTH_TOKEN_KEY);
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }
    let message =
      error.response?.data?.message || error.message || "接口请求失败";
    if (status === 502) {
      message =
        "后端 API 未启动或不可访问。请先启动 services/api 后端服务，再重新登录。";
    } else if (status === 403) {
      message = "当前账号没有执行此操作的权限。";
    } else if (status === 504 || error.code === "ECONNABORTED") {
      message = "后端 API 响应超时，请检查后端服务和数据库连接。";
    } else if (!error.response) {
      message =
        "无法连接后端 API，请检查网络、后端服务或 VITE_API_BASE_URL 配置。";
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

/**
 * 顺序拉取所有分页数据（默认每页 200）。
 *
 * 与一次性 Promise.all 并发请求全部页不同，这里用有界并发（默认 4）逐页拉取，
 * 避免数据量大时对后端形成并发洪峰。对于确实需要全量数据的场景（下拉选项、导出、
 * 前端聚合）继续适用；纯列表展示应优先使用服务端分页而非本函数。
 *
 * 可选 options.maxPages 用于限制最大拉取页数（防止无界加载），
 * 不传则保持原有"拉全量"语义。
 */
export async function requestAllPages<T>(
  config: AxiosRequestConfig,
  size = 200,
  options?: { maxPages?: number; concurrency?: number },
): Promise<T[]> {
  const pageConfig = (page: number): AxiosRequestConfig => ({
    ...config,
    params: { ...config.params, page, size },
  });

  const first = await request<PageResponse<T>>(pageConfig(0));
  if (first.totalPages <= 1) return first.content;

  const totalPages = options?.maxPages
    ? Math.min(first.totalPages, options.maxPages)
    : first.totalPages;
  if (totalPages <= 1) return first.content;

  const concurrency = Math.max(1, options?.concurrency ?? 4);
  const pages: T[][] = new Array<T[]>(totalPages - 1);
  let nextPage = 1;

  const worker = async () => {
    while (nextPage < totalPages) {
      const page = nextPage++;
      pages[page - 1] = (await request<PageResponse<T>>(pageConfig(page))).content;
    }
  };

  await Promise.all(
    Array.from({ length: Math.min(concurrency, totalPages - 1) }, worker),
  );

  return [...first.content, ...pages.flat()];
}
