import type { RequestOptions } from "@/types/domain";
import { isRetryableError, request, upload } from "./http";
import { OFFLINE_QUEUE_KEY, readStorage, writeStorage } from "./storage";

export const MAX_ATTEMPTS = 5;
const BASE_BACKOFF_MS = 30_000;
const MAX_BACKOFF_MS = 5 * 60_000;

export type OfflineOperation = {
  id: string;
  label: string;
  kind: "REQUEST" | "UPLOAD";
  request?: RequestOptions;
  upload?: { url: string; filePath: string; name?: string; formData?: Record<string, string>; savedFile?: boolean };
  createdAt: string;
  retries: number;
  lastError?: string;
  nextAttemptAt?: string;
  parked?: boolean;
};

let flushing = false;

export function queueOperation(input: Omit<OfflineOperation, "id" | "createdAt" | "retries">) {
  const queue = getQueue();
  queue.push({ ...input, id: operationId(), createdAt: new Date().toISOString(), retries: 0 });
  writeStorage(OFFLINE_QUEUE_KEY, queue);
  return queue[queue.length - 1];
}

export function getQueue() {
  return readStorage<OfflineOperation[]>(OFFLINE_QUEUE_KEY, []);
}

export function removeQueued(id: string) {
  const queue = getQueue();
  const target = queue.find((item) => item.id === id);
  if (target?.upload?.savedFile) uni.removeSavedFile({ filePath: target.upload.filePath });
  writeStorage(OFFLINE_QUEUE_KEY, queue.filter((item) => item.id !== id));
}

export function clearQueue() {
  getQueue().forEach((item) => {
    if (item.upload?.savedFile) uni.removeSavedFile({ filePath: item.upload.filePath });
  });
  writeStorage(OFFLINE_QUEUE_KEY, []);
}

// force = true is used by the offline page's manual retry: it ignores backoff and
// the parked flag so a user can always retry, even after the automatic cap.
export async function flushQueue(force = false) {
  if (flushing || !getQueue().length) return;
  flushing = true;
  try {
    const queue = getQueue();
    const now = Date.now();
    const remaining: OfflineOperation[] = [];
    for (const item of queue) {
      if (!force && (item.parked || (item.nextAttemptAt && now < Date.parse(item.nextAttemptAt)))) {
        remaining.push(item);
        continue;
      }
      try {
        if (item.kind === "UPLOAD" && item.upload) {
          await upload(item.upload.url, item.upload.filePath, item.upload.name, item.upload.formData);
          if (item.upload.savedFile) uni.removeSavedFile({ filePath: item.upload.filePath });
        } else if (item.request) {
          await request(item.request);
        }
      } catch (error) {
        const failed: OfflineOperation = { ...item, retries: item.retries + 1, lastError: (error as Error).message };
        // Non-retryable (4xx) failures park immediately; retryable ones back off
        // until MAX_ATTEMPTS, then park instead of looping forever.
        if (!isRetryableError(error) || failed.retries >= MAX_ATTEMPTS) {
          remaining.push({ ...failed, parked: true });
        } else {
          remaining.push({ ...failed, nextAttemptAt: new Date(now + backoffMs(failed.retries)).toISOString() });
        }
      }
    }
    writeStorage(OFFLINE_QUEUE_KEY, remaining);
  } finally {
    flushing = false;
  }
}

function backoffMs(attempts: number) {
  return Math.min(BASE_BACKOFF_MS * 2 ** (attempts - 1), MAX_BACKOFF_MS);
}

export function createOperationId(prefix = "mobile") {
  return `${prefix}-${operationId()}`;
}

export function createExpenseCode(now = new Date(), random = Math.random) {
  const datePart = now.toISOString().slice(0, 10).replace(/-/g, "");
  const suffix = Array.from({ length: 5 }, () =>
    Math.floor(random() * 36).toString(36).toUpperCase(),
  ).join("");
  return `BX-${datePart}-${suffix}`;
}

// uni.saveFile has no H5 implementation and blob:/temp URLs from chooseMedia are
// revoked on reload, so a queued upload would never succeed after a refresh. On
// H5 we resolve to "" as a "cannot persist" signal; callers then surface 需联网提交
// and skip queueing instead of enqueueing a path that can never work.
export function persistOfflineFile(filePath: string) {
  if (process.env.UNI_PLATFORM === "h5") return Promise.resolve("");
  return new Promise<string>((resolve) => uni.saveFile({
    tempFilePath: filePath,
    success: (result) => resolve(result.savedFilePath),
    fail: () => resolve(filePath),
  }));
}

// Persist + enqueue one attachment. Returns false when the platform cannot keep
// the file for a later offline upload (H5), so callers can warn the user.
export async function queueOfflineUpload(input: { label: string; url: string; filePath: string; formData?: Record<string, string> }) {
  const savedPath = await persistOfflineFile(input.filePath);
  if (!savedPath) return false;
  queueOperation({
    label: input.label,
    kind: "UPLOAD",
    upload: { url: input.url, filePath: savedPath, formData: input.formData, savedFile: savedPath !== input.filePath },
  });
  return true;
}

function operationId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
