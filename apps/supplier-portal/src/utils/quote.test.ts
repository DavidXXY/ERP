import { describe, expect, it } from "vitest";
import {
  contractStatusText,
  daysLeft,
  deadlineText,
  docExpiryDays,
  expiryMessage,
  fileSize,
  money,
  quoteStatus,
} from "./quote";

const dateString = (daysFromNow: number) => {
  const d = new Date();
  d.setDate(d.getDate() + daysFromNow);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

describe("money", () => {
  it("formats CNY with thousands separators", () => {
    expect(money(1234567.5)).toBe("¥1,234,567.50");
  });
  it("treats null/undefined as zero", () => {
    expect(money()).toBe("¥0.00");
    expect(money(null)).toBe("¥0.00");
  });
});

describe("fileSize", () => {
  it("shows KB below 1MB and MB above", () => {
    expect(fileSize(512)).toBe("1 KB");
    expect(fileSize(2048 * 1024)).toBe("2.0 MB");
  });
});

describe("deadline", () => {
  it("returns days left", () => {
    expect(daysLeft(dateString(3))).toBe(3);
    expect(daysLeft(undefined)).toBe(999);
  });
  it("labels deadline states", () => {
    expect(deadlineText(undefined)).toBe("未设截止");
    expect(deadlineText(dateString(0))).toBe("今天截止");
    expect(deadlineText(dateString(-1))).toBe("已截止");
    expect(deadlineText(dateString(2))).toBe("2 天后截止");
  });
});

describe("quoteStatus", () => {
  it("maps award and quote states", () => {
    expect(quoteStatus({ awardStatus: "AWARDED" }).text).toBe("已中标");
    expect(quoteStatus({ awardStatus: "NOT_AWARDED" }).text).toBe("未中标");
    expect(
      quoteStatus({ quote: { source: "INTERNAL_ENTRY", confirmed: false } }).text,
    ).toBe("采购代录 · 待确认");
    expect(
      quoteStatus({ quote: { source: "SUPPLIER_PORTAL", status: "SUBMITTED" } }).text,
    ).toBe("已提交 · 待定标");
    expect(
      quoteStatus({ quote: { source: "SUPPLIER_PORTAL", status: "DRAFT" } }).text,
    ).toBe("草稿");
    expect(quoteStatus({}).text).toBe("待报价");
  });
});

describe("contractStatusText", () => {
  it("translates known statuses and passes through others", () => {
    expect(contractStatusText("ACTIVE")).toBe("已生效");
    expect(contractStatusText("PENDING_APPROVAL")).toBe("审批中");
    expect(contractStatusText("UNKNOWN")).toBe("UNKNOWN");
  });
});

describe("expiry", () => {
  it("computes remaining days", () => {
    expect(docExpiryDays(dateString(30))).toBe(30);
    expect(docExpiryDays(undefined)).toBeNull();
  });
  it("warns within 90 days and returns null beyond", () => {
    expect(expiryMessage("营业执照", dateString(30))).toContain("剩 30 天");
    expect(expiryMessage("营业执照", dateString(200))).toBeNull();
    expect(expiryMessage("资质", dateString(-5))).toContain("已于");
  });
});
