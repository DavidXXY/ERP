import { describe, expect, it } from "vitest";
import { createExpenseCode } from "./offline";

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
