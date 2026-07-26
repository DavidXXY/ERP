import type { RequestOptions } from "@/types/domain";
import { request, upload } from "./http";
import { OFFLINE_QUEUE_KEY, readStorage, writeStorage } from "./storage";

export type OfflineOperation = {
  id: string;
  label: string;
  kind: "REQUEST" | "UPLOAD";
  request?: RequestOptions;
  upload?: { url: string; filePath: string; name?: string; formData?: Record<string, string>; savedFile?: boolean };
  createdAt: string;
  retries: number;
  lastError?: string;
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

export async function flushQueue() {
  if (flushing || !getQueue().length) return;
  flushing = true;
  try {
    const queue = getQueue();
    const remaining: OfflineOperation[] = [];
    for (const item of queue) {
      try {
        if (item.kind === "UPLOAD" && item.upload) {
          await upload(item.upload.url, item.upload.filePath, item.upload.name, item.upload.formData);
          if (item.upload.savedFile) uni.removeSavedFile({ filePath: item.upload.filePath });
        } else if (item.request) {
          await request(item.request);
        }
      } catch (error) {
        remaining.push({ ...item, retries: item.retries + 1, lastError: (error as Error).message });
      }
    }
    writeStorage(OFFLINE_QUEUE_KEY, remaining);
  } finally {
    flushing = false;
  }
}

export function createOperationId(prefix = "mobile") {
  return `${prefix}-${operationId()}`;
}

export function persistOfflineFile(filePath: string) {
  return new Promise<string>((resolve) => uni.saveFile({
    tempFilePath: filePath,
    success: (result) => resolve(result.savedFilePath),
    fail: () => resolve(filePath),
  }));
}

function operationId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
