import { request } from "@/utils/http";

export type InventoryPart = { id:string; code:string; name:string; model?:string; stockQty:number; safetyQty:number; location?:string; unitCost:number; lowStock:boolean };
export type ProjectOption = { id:string; code:string; name:string };
export type IssueLine = { id:string; partId:string; partName:string; quantity:number; returnedQty:number; returnableQty:number; unitCost:number; amount:number };
export type MaterialIssue = { id:string; code:string; projectId:string; projectCode:string; projectName:string; issueDate:string; receiverName:string; purpose:string; totalAmount:number; status:string; lines:IssueLine[] };

export const listParts=()=>request<InventoryPart[]>({url:"/inventory/parts"});
export const listEligibleProjects=()=>request<ProjectOption[]>({url:"/inventory/eligible-projects"});
export const listIssues=()=>request<MaterialIssue[]>({url:"/inventory/issues"});
export const createIssue=(data:Record<string,unknown>)=>request<MaterialIssue>({url:"/inventory/issues",method:"POST",data});
export const createReturn=(issueId:string,data:Record<string,unknown>)=>request({url:`/inventory/issues/${issueId}/returns`,method:"POST",data});
export const scrapPart=(partId:string,data:Record<string,unknown>)=>request<InventoryPart>({url:`/inventory/parts/${partId}/movements`,method:"POST",data});
