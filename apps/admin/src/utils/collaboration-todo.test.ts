import { describe, expect, it } from "vitest";
import { mapCanonicalTodo } from "./collaboration-todo";

describe("mapCanonicalTodo", () => {
  it("uses the canonical overdue state and assignee", () => {
    const result = mapCanonicalTodo({
      type: "PURCHASE_REQUEST",
      id: "request-1",
      title: "采购申请待审批",
      detail: "泵组",
      createdAt: "2026-07-20T09:00:00+08:00",
      dueAt: "2026-07-22T18:00:00+08:00",
      status: "PENDING",
      priority: "HIGH",
      link: "/procurement/requests",
      assigneeName: "采购经理",
      overdueDays: 2,
    });

    expect(result).toMatchObject({
      key: "PURCHASE_REQUEST-request-1",
      module: "PROCUREMENT",
      status: "OVERDUE",
      priority: "HIGH",
      owner: "采购经理",
    });
  });

  it("maps completed approvals to a detail-capable todo", () => {
    const result = mapCanonicalTodo({
      type: "APPROVAL",
      id: "approval-1",
      title: "审批事项",
      detail: "申请人",
      createdAt: "2026-07-20T09:00:00+08:00",
      status: "DONE",
      priority: "NORMAL",
      link: "/office/approvals",
      overdueDays: 0,
    });

    expect(result.status).toBe("DONE");
    expect(result.entityId).toBe("approval-1");
    expect(result.owner).toBe("待认领");
  });
});
