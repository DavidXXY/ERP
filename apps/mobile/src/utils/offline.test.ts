import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { RequestError } from "./http";
import { MAX_ATTEMPTS, createExpenseCode, flushQueue, getQueue, queueOperation } from "./offline";

vi.mock("./http", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./http")>();
  return { ...actual, request: vi.fn(), upload: vi.fn() };
});

import { request, upload } from "./http";

function stubUni() {
  const store = new Map<string, unknown>();
  vi.stubGlobal("uni", {
    getStorageSync: (key: string) => store.get(key) ?? "",
    setStorageSync: (key: string, value: unknown) => { store.set(key, value); },
    removeStorageSync: (key: string) => { store.delete(key); },
    removeSavedFile: vi.fn(),
  });
}

beforeEach(() => {
  stubUni();
  vi.mocked(request).mockReset();
  vi.mocked(upload).mockReset();
});
afterEach(() => vi.unstubAllGlobals());

describe("mobile offline ids", () => {
  it("creates compact expense codes for approvals", () => {
    let index = 0;
    const values = [0, 1 / 36, 10 / 36, 35 / 36, 2 / 36];
    const random = () => values[index++] ?? 0;

    const code = createExpenseCode(new Date("2026-07-28T09:30:00+08:00"), random);

    expect(code).toBe("BX-20260728-01AZ2");
    expect(code).toHaveLength(17);
    expect(`SP-${code}`).toHaveLength(20);
  });

  it("keeps expense codes in the expected short format", () => {
    expect(createExpenseCode()).toMatch(/^BX-\d{8}-[0-9A-Z]{5}$/);
  });
});

describe("mobile offline flushQueue", () => {
  const networkError = () => new RequestError("网络不可用，请检查连接", { retryable: true, offline: true });

  function queueUpload() {
    return queueOperation({ label: "现场照片", kind: "UPLOAD", upload: { url: "/attachments", filePath: "/tmp/a.png" } });
  }

  it("parks a retryable item after the attempt cap instead of retrying forever", async () => {
    vi.mocked(upload).mockRejectedValue(networkError());
    queueUpload();

    for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) await flushQueue(true);

    const [item] = getQueue();
    expect(item.retries).toBe(MAX_ATTEMPTS);
    expect(item.parked).toBe(true);
  });

  it("skips items that are not yet due but lets a manual retry force them", async () => {
    vi.mocked(upload)
      .mockRejectedValueOnce(networkError())
      .mockResolvedValueOnce(undefined);
    queueUpload();

    await flushQueue();
    const [first] = getQueue();
    expect(first.retries).toBe(1);
    expect(Date.parse(first.nextAttemptAt!)).toBeGreaterThan(Date.now());

    const callsAfterFirst = vi.mocked(upload).mock.calls.length;
    await flushQueue();
    expect(vi.mocked(upload).mock.calls.length).toBe(callsAfterFirst);
    expect(getQueue()[0].retries).toBe(1);

    await flushQueue(true);
    expect(getQueue()).toHaveLength(0);
  });

  it("parks a non-retryable (4xx) failure immediately", async () => {
    vi.mocked(upload).mockRejectedValue(new RequestError("提交内容不完整或格式有误", { statusCode: 400 }));
    queueUpload();

    await flushQueue(true);

    const [item] = getQueue();
    expect(item.retries).toBe(1);
    expect(item.parked).toBe(true);
  });

  it("removes an item once the operation succeeds", async () => {
    vi.mocked(request).mockResolvedValue({ ok: true });
    queueOperation({ label: "完工记录", kind: "REQUEST", request: { url: "/complete", method: "PUT", data: {} } });

    await flushQueue();

    expect(getQueue()).toHaveLength(0);
    expect(vi.mocked(request)).toHaveBeenCalledTimes(1);
  });
});
