import type { MobileWorkbench } from "@/types/domain";
import { request } from "@/utils/http";

export const getMobileWorkbench = () => request<MobileWorkbench>({ url: "/mobile/workbench" });
