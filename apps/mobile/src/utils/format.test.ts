import { describe, expect, it } from "vitest";
import { dateText, money, shortDate, statusClass } from "./format";

describe("mobile formatting", () => {
  it("formats server dates without timezone noise", () => {
    expect(dateText("2026-07-26T08:30:00+08:00")).toBe("2026-07-26 08:30");
    expect(shortDate("2026-07-26T08:30:00+08:00")).toBe("2026-07-26");
  });

  it("maps money and workflow states", () => {
    expect(money(1234.5)).toContain("1,234.50");
    expect(statusClass("APPROVED")).toBe("status-success");
    expect(statusClass("REJECTED")).toBe("status-danger");
    expect(statusClass("PENDING")).toBe("status-pending");
  });
});
