import type { LeaveBalance, LeaveRecord } from "@/types/domain";
import { request } from "@/utils/http";

export const getSelfProfile = () => request<Record<string, unknown>>({ url: "/hr/self/profile" });
export const listLeaveBalances = () => request<LeaveBalance[]>({ url: "/hr/self/leave-balances" });
export const listSelfLeaves = () => request<LeaveRecord[]>({ url: "/hr/self/leaves" });
export const createSelfLeave = (data: Record<string, unknown>) => request<LeaveRecord>({ url: "/hr/self/leaves", method: "POST", data });
