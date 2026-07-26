import type { WorkOrder, WorkOrderAttachment, WorkOrderMaterial } from "@/types/domain";
import { request, upload } from "@/utils/http";
import { API_BASE_URL } from "@/utils/http";
import { TOKEN_KEY, readStorage } from "@/utils/storage";

export const listMyWorkOrders = () => request<WorkOrder[]>({ url: "/maintenance/mobile/work-orders" });
export const listAssignees = () => request<Array<{ id:string; displayName:string }>>({ url: "/maintenance/mobile/assignees" });
export const getWorkOrder = (id: string) => request<WorkOrder>({ url: `/maintenance/mobile/work-orders/${id}` });
export const acceptWorkOrder = (id: string, operationId: string) => request<WorkOrder>({ url: `/maintenance/mobile/work-orders/${id}/accept-assignment`, method: "PUT", data: { operationId } });
export const reassignWorkOrder = (id:string, assignee:{id:string;displayName:string}) => request<WorkOrder>({ url:`/maintenance/work-orders/${id}/assign`, method:"PUT", data:{assigneeId:assignee.id,assigneeName:assignee.displayName} });
export const checkInWorkOrder = (id: string, data: Record<string, unknown>) => request<WorkOrder>({ url: `/maintenance/mobile/work-orders/${id}/check-in`, method: "PUT", data });
export const completeWorkOrder = (id: string, data: Record<string, unknown>) => request<WorkOrder>({ url: `/maintenance/mobile/work-orders/${id}/complete`, method: "PUT", data });
export const uploadWorkOrderAttachment = (id: string, category: string, filePath: string) =>
  upload<WorkOrderAttachment>(`/maintenance/mobile/work-orders/${id}/attachments`, filePath, "file", { category });
export const listWorkOrderMaterials = (id: string) => request<WorkOrderMaterial[]>({ url: `/maintenance/mobile/work-orders/${id}/materials` });

export function downloadWorkOrderAttachment(attachmentId: string) {
  const token = readStorage(TOKEN_KEY, "");
  return new Promise<string>((resolve, reject) => uni.downloadFile({
    url: `${API_BASE_URL}/maintenance/mobile/attachments/${attachmentId}/content`,
    header: token ? { Authorization: `Bearer ${token}` } : {},
    success: (result) => result.statusCode === 200 ? resolve(result.tempFilePath) : reject(new Error("现场图片加载失败")),
    fail: () => reject(new Error("现场图片加载失败")),
  }));
}
