import type { RequestOptions } from "@/types/domain";
import { TOKEN_KEY, USER_KEY, readStorage, removeStorage } from "./storage";

export type ApiEnvelope<T> = { success: boolean; message: string; data: T };

const configuredBase = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "");
export const API_BASE_URL = configuredBase || "/api";

let redirecting = false;
let onUnauthorized: (() => void) | null = null;

export class RequestError extends Error {
  retryable: boolean;
  offline: boolean;
  statusCode?: number;
  constructor(message: string, opts: { retryable?: boolean; offline?: boolean; statusCode?: number } = {}) {
    super(message);
    this.name = "RequestError";
    this.retryable = opts.retryable ?? false;
    this.offline = opts.offline ?? false;
    this.statusCode = opts.statusCode;
  }
}

// Network failures, timeouts and 5xx are safe to retry (a 4xx business error is
// not). Unknown errors default to retryable so the offline queue still caps them.
export function isRetryableError(error: unknown): boolean {
  return !(error instanceof RequestError) || error.retryable;
}

// Registered by the auth store so a 401 can clear Pinia state without http.ts
// importing the store (which would create a circular import through api/auth).
export function setOnUnauthorized(handler: () => void) {
  onUnauthorized = handler;
}

export function request<T>(options: RequestOptions): Promise<T> {
  const token = readStorage(TOKEN_KEY, "");
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || "GET",
      data: options.data as UniApp.RequestOptions["data"],
      timeout: options.timeout || 15000,
      header: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.header || {}),
      },
      success(response) {
        const status = response.statusCode;
        const payload = response.data as ApiEnvelope<T> | undefined;
        if (status >= 200 && status < 300 && payload?.success) {
          resolve(payload.data);
          return;
        }
        if (status === 401) {
          reject(unauthorized(payload?.message || statusMessage(status)));
          return;
        }
        reject(new RequestError(payload?.message || statusMessage(status), {
          retryable: status >= 500,
          offline: false,
          statusCode: status,
        }));
      },
      fail(error) {
        const timedOut = error.errMsg?.includes("timeout") ?? false;
        reject(new RequestError(
          timedOut ? "请求超时，请稍后重试" : "网络不可用，请检查连接",
          { retryable: true, offline: true },
        ));
      },
    });
  });
}

export async function requestAllPages<T>(url: string, size = 200): Promise<T[]> {
  const separator = url.includes("?") ? "&" : "?";
  const first = await request<{ content: T[]; totalPages: number }>({
    url: `${url}${separator}page=0&size=${size}`,
  });
  if (first.totalPages <= 1) return first.content;
  const rest = await Promise.all(
    Array.from({ length: first.totalPages - 1 }, (_, index) =>
      request<{ content: T[] }>({
        url: `${url}${separator}page=${index + 1}&size=${size}`,
      }),
    ),
  );
  return [...first.content, ...rest.flatMap((page) => page.content)];
}

export function upload<T>(url: string, filePath: string, name = "file", formData: Record<string, string> = {}): Promise<T> {
  const token = readStorage(TOKEN_KEY, "");
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}${url}`,
      filePath,
      name,
      formData,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success(response) {
        let payload: ApiEnvelope<T> | undefined;
        try { payload = JSON.parse(response.data) as ApiEnvelope<T>; } catch { /* handled below */ }
        if (response.statusCode >= 200 && response.statusCode < 300 && payload?.success) resolve(payload.data);
        else if (response.statusCode === 401) reject(unauthorized(payload?.message || statusMessage(response.statusCode)));
        else reject(new RequestError(payload?.message || statusMessage(response.statusCode), {
          retryable: response.statusCode >= 500,
          offline: false,
          statusCode: response.statusCode,
        }));
      },
      fail(error) {
        const timedOut = error.errMsg?.includes("timeout") ?? false;
        reject(new RequestError(
          timedOut ? "请求超时，请稍后重试" : "网络不可用，请检查连接",
          { retryable: true, offline: true },
        ));
      },
    });
  });
}

function unauthorized(message: string) {
  removeStorage(TOKEN_KEY);
  removeStorage(USER_KEY);
  onUnauthorized?.();
  if (!redirecting) {
    redirecting = true;
    uni.reLaunch({ url: "/pages/login/index", complete: () => { redirecting = false; } });
  }
  return new RequestError(message, { statusCode: 401 });
}

function statusMessage(status: number) {
  if (status === 400) return "提交内容不完整或格式有误";
  if (status === 403) return "当前账号没有此操作权限";
  if (status === 404) return "请求的数据不存在";
  if (status >= 500) return "服务暂时不可用，请稍后重试";
  return "请求失败";
}
