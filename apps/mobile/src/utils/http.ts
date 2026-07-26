import type { RequestOptions } from "@/types/domain";
import { TOKEN_KEY, readStorage, removeStorage } from "./storage";

export type ApiEnvelope<T> = { success: boolean; message: string; data: T };

const configuredBase = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "");
export const API_BASE_URL = configuredBase || "/api";

let redirecting = false;

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
          removeStorage(TOKEN_KEY);
          if (!redirecting) {
            redirecting = true;
            uni.reLaunch({ url: "/pages/login/index", complete: () => { redirecting = false; } });
          }
        }
        reject(new Error(payload?.message || statusMessage(status)));
      },
      fail(error) {
        reject(new Error(error.errMsg?.includes("timeout") ? "请求超时，请稍后重试" : "网络不可用，请检查连接"));
      },
    });
  });
}

export async function requestAllPages<T>(url: string, size = 200): Promise<T[]> {
  const separator = url.includes("?") ? "&" : "?";
  const first = await request<{ content: T[]; totalPages: number }>({
    url: `${url}${separator}page=0&size=${size}`,
  });
  const items = [...first.content];
  for (let page = 1; page < first.totalPages; page += 1) {
    const next = await request<{ content: T[] }>({
      url: `${url}${separator}page=${page}&size=${size}`,
    });
    items.push(...next.content);
  }
  return items;
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
        else reject(new Error(payload?.message || statusMessage(response.statusCode)));
      },
      fail(error) { reject(new Error(error.errMsg || "文件上传失败")); },
    });
  });
}

function statusMessage(status: number) {
  if (status === 400) return "提交内容不完整或格式有误";
  if (status === 403) return "当前账号没有此操作权限";
  if (status === 404) return "请求的数据不存在";
  if (status >= 500) return "服务暂时不可用，请稍后重试";
  return "请求失败";
}
