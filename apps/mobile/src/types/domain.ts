export type CurrentUser = {
  id: string;
  username: string;
  displayName: string;
  roles: string[];
  roleCodes?: string[];
  permissions: string[];
};

export type LoginResponse = { token: string; user: CurrentUser };

export type MobileWorkbench = {
  generatedAt: string;
  pendingApprovals: number;
  unreadNotifications: number;
  activeWorkOrders: number;
  urgentWorkOrders: number;
  offlineOperations?: number;
  todos: MobileTodo[];
  workOrders: WorkOrder[];
};

export type MobileTodo = {
  id: string;
  type: string;
  title: string;
  subtitle?: string;
  priority?: string;
  amount?: number;
  createdAt?: string;
  route?: string;
};

export type Approval = {
  id: string;
  code: string;
  approvalType: string;
  title: string;
  applicantName: string;
  departmentName?: string;
  amount?: number;
  status: string;
  currentApprover?: string;
  currentApproverName?: string;
  currentNodeName?: string;
  reason?: string;
  content?: string;
  createdAt: string;
  updatedAt?: string;
  canApprove?: boolean;
  nodes?: Array<{
    id: string;
    nodeName?: string;
    assigneeName?: string;
    nodeStatus?: string;
    completedAt?: string;
  }>;
  actions?: Array<{
    id: string;
    operatorName: string;
    decision: string;
    comment?: string;
    createdAt: string;
  }>;
};

export type NotificationRecord = {
  id: string;
  type: string;
  title: string;
  content: string;
  relatedType?: string;
  relatedId?: string;
  read: boolean;
  createdAt: string;
};

export type WorkOrderStatus =
  | "CREATED"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "ACCEPTED"
  | "CANCELLED";

export type WorkOrderAttachment = {
  id: string;
  category: "SITE_PHOTO" | "RESULT_PHOTO" | "CUSTOMER_SIGNATURE" | "OTHER";
  fileName: string;
  contentType?: string;
  fileSize: number;
  uploadedBy?: string;
  createdAt: string;
  previewUrl?: string;
};

export type WorkOrderMaterial = {
  id?: string;
  partId?: string;
  partName: string;
  quantity: number;
  unitCost?: number;
  amount?: number;
};

export type WorkOrder = {
  id: string;
  code: string;
  title: string;
  description?: string;
  customerId?: string;
  customerName?: string;
  equipmentId?: string;
  equipmentCode?: string;
  equipmentName?: string;
  workType: string;
  priority: string;
  source: string;
  status: WorkOrderStatus;
  assigneeId?: string;
  assigneeName?: string;
  assignmentAcceptedAt?: string;
  siteAddress?: string;
  plannedDate?: string;
  checkInAt?: string;
  checkInLocation?: string;
  checkInLatitude?: number;
  checkInLongitude?: number;
  serviceResult?: string;
  customerSigner?: string;
  laborHours?: number;
  materialCost?: number;
  travelCost?: number;
  startedAt?: string;
  completedAt?: string;
  acceptedAt?: string;
  createdAt: string;
  updatedAt: string;
  remarks?: string;
  attachments?: WorkOrderAttachment[];
  materials?: WorkOrderMaterial[];
  statusLogs?: Array<{
    id: string;
    fromStatus?: string;
    toStatus: string;
    operatorName: string;
    comment?: string;
    createdAt: string;
  }>;
};

export type PersonalProfile = {
  id: string;
  username: string;
  displayName: string;
  phone?: string;
  email?: string;
  organizationName?: string;
  roleNames?: string[];
};

export type PersonalOverview = {
  account: PersonalProfile;
  employee?: { id: string; name: string; workNo?: string; organizationName?: string; organizationPath?: string; position?: string; entryDate?: string; employmentStatus?: string };
  certificates?: Array<Record<string, unknown>>;
  contracts?: Array<Record<string, unknown>>;
};

export type LeaveBalance = { leaveType: string; year: number; totalDays: number; usedDays: number; remainingDays: number };
export type LeaveRecord = { id: string; leaveType: string; startDate: string; endDate: string; totalDays: number; reason?: string; status: string; approvedAt?: string };

export type RequestOptions = {
  url: string;
  method?: UniApp.RequestOptions["method"];
  data?: unknown;
  header?: Record<string, string>;
  timeout?: number;
  silent?: boolean;
};
