import { describe, expect, it } from "vitest";
import { statusColor, statusLabel } from "./status-mapper";

describe("status mapper", () => {
  it("maps known values and preserves unknown labels", () => {
    expect(statusLabel("DONE", { DONE: "完成" })).toBe("完成");
    expect(statusLabel("CUSTOM", {})).toBe("CUSTOM");
  });

  it("uses safe defaults for empty and unknown colors", () => {
    expect(statusColor(undefined, {})).toBe("default");
    expect(statusColor("UNKNOWN", {})).toBe("default");
  });
});
