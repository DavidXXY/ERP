import { describe, expect, it } from "vitest";
import { projectStageColor, projectStageLabel } from "./project-stage";

describe("project stage display", () => {
  it("maps every project stage to Chinese", () => {
    expect(projectStageLabel("INITIATED")).toBe("立项");
    expect(projectStageLabel("CONSTRUCTION")).toBe("施工");
    expect(projectStageLabel("FINAL_ACCEPTANCE")).toBe("终验");
    expect(projectStageLabel("CLOSED")).toBe("结项");
  });

  it("keeps unknown values visible and handles empty values", () => {
    expect(projectStageLabel("CUSTOM_STAGE")).toBe("CUSTOM_STAGE");
    expect(projectStageLabel()).toBe("-");
    expect(projectStageColor("WARRANTY")).toBe("green");
  });
});
