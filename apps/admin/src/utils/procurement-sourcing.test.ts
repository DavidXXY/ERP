import { describe, expect, it } from "vitest";
import {
  sourcingMethodLabel,
  sourcingMethodOptions,
} from "./procurement-sourcing";

describe("procurement sourcing method display", () => {
  it("maps every selectable sourcing method to Chinese", () => {
    expect(
      sourcingMethodOptions.map(({ value }) => sourcingMethodLabel(value)),
    ).toEqual(["竞争性询价", "单一来源", "框架协议"]);
  });

  it("keeps unknown values visible and handles empty values", () => {
    expect(sourcingMethodLabel("CUSTOM_METHOD")).toBe("CUSTOM_METHOD");
    expect(sourcingMethodLabel()).toBe("-");
    expect(sourcingMethodLabel(null)).toBe("-");
  });
});
