import { describe, expect, it } from "vitest";
import { businessStatusLabel, statusColor, statusLabel } from "./status-mapper";

describe("status mapper", () => {
  it("maps known values and preserves unknown labels", () => {
    expect(statusLabel("DONE", { DONE: "完成" })).toBe("完成");
    expect(statusLabel("CUSTOM", {})).toBe("CUSTOM");
  });

  it("uses safe defaults for empty and unknown colors", () => {
    expect(statusColor(undefined, {})).toBe("default");
    expect(statusColor("UNKNOWN", {})).toBe("default");
  });

  it("localizes business statuses without leaking raw internal codes", () => {
    expect(businessStatusLabel("INVOICE_PENDING")).toBe("待收票");
    expect(businessStatusLabel("MISMATCH")).toBe("存在差异");
    expect(businessStatusLabel("NEW_BACKEND_STATUS")).toBe("状态待配置");
    expect(businessStatusLabel(undefined)).toBe("-");
  });
});
