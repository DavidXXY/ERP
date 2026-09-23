import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { isRetryableError, RequestError, request, upload } from "./http";

function stubUni() {
  const store = new Map<string, unknown>();
  vi.stubGlobal("uni", {
    getStorageSync: (key: string) => store.get(key) ?? "",
    setStorageSync: (key: string, value: unknown) => { store.set(key, value); },
    removeStorageSync: (key: string) => { store.delete(key); },
    request: vi.fn(),
    uploadFile: vi.fn(),
    reLaunch: vi.fn(),
  });
}

beforeEach(() => stubUni());
afterEach(() => vi.unstubAllGlobals());

describe("http retryable/offline classification", () => {
  it("classifies retryable vs non-retryable errors", () => {
    expect(isRetryableError(new RequestError("x", { retryable: true, offline: true }))).toBe(true);
    expect(isRetryableError(new RequestError("x", { retryable: false }))).toBe(false);
    expect(isRetryableError(new Error("unknown"))).toBe(true);
  });

  it("flags 5xx as retryable but not offline", async () => {
    const req = vi.mocked(uni.request);
    req.mockImplementation((opts) => opts.success!({ statusCode: 503, data: { success: false, message: "服务暂时不可用，请稍后重试" }, header: {}, cookies: [] }));

    await expect(request({ url: "/x" })).rejects.toMatchObject({ retryable: true, offline: false, statusCode: 503 });
  });

  it("flags 4xx as non-retryable", async () => {
    const req = vi.mocked(uni.request);
    req.mockImplementation((opts) => opts.success!({ statusCode: 400, data: { success: false, message: "提交内容不完整或格式有误" }, header: {}, cookies: [] }));

    await expect(request({ url: "/x" })).rejects.toMatchObject({ retryable: false, statusCode: 400 });
  });

  it("flags timeouts and network failures as offline and retryable", async () => {
    const req = vi.mocked(uni.request);
    req.mockImplementation((opts) => opts.fail!({ errMsg: "request:fail timeout" }));
    await expect(request({ url: "/x" })).rejects.toMatchObject({ retryable: true, offline: true, message: "请求超时，请稍后重试" });

    req.mockImplementation((opts) => opts.fail!({ errMsg: "request:fail" }));
    await expect(request({ url: "/x" })).rejects.toMatchObject({ retryable: true, offline: true, message: "网络不可用，请检查连接" });
  });

  it("classifies upload failures the same way", async () => {
    const up = vi.mocked(uni.uploadFile);
    up.mockImplementation((opts) => opts.success!({ statusCode: 500, data: JSON.stringify({ success: false, message: "服务暂时不可用，请稍后重试" }) }));
    await expect(upload("/x", "/tmp/a.png")).rejects.toMatchObject({ retryable: true, statusCode: 500 });

    up.mockImplementation((opts) => opts.fail!({ errMsg: "uploadFile:fail timeout" }));
    await expect(upload("/x", "/tmp/a.png")).rejects.toMatchObject({ retryable: true, offline: true });
  });
});
