export const TOKEN_KEY = "ops_erp_mobile_token";
export const USER_KEY = "ops_erp_mobile_user";
export const OFFLINE_QUEUE_KEY = "ops_erp_mobile_offline_queue";

export function readStorage<T>(key: string, fallback: T): T {
  try {
    const value = uni.getStorageSync(key);
    return value === "" || value == null ? fallback : (value as T);
  } catch {
    return fallback;
  }
}

export function writeStorage(key: string, value: unknown) {
  uni.setStorageSync(key, value);
}

export function removeStorage(key: string) {
  uni.removeStorageSync(key);
}
