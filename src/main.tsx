import React, { useMemo, useState, useRef, useEffect } from "react";
import { createRoot } from "react-dom/client";
import { login, getToken, fetchFinanceOverview, fetchReceivables, fetchPayables, registerInvoice, recordReceipt, type ReceivableResponse, type FinancePayableResponse, type FinanceOverviewResponse,
  type VoucherResponse, type LedgerOverviewResponse, type FinancialStatementsResponse,
  type PaymentApplicationResponse, type PaymentRecordResponse,
  fetchLedgerOverview, fetchLedgerVouchers, fetchFinancialStatements,
  fetchPaymentApplications, fetchPaymentRecords,
  createPaymentApplication, approvePaymentApplication, executePayment,
  fetchUsers, createUser, updateUser, deleteUser, resetUserPassword,
  fetchRoles, fetchOrganizationsFlat,
  type UserResponse, type RoleResponse, type OrganizationResponse,
  type CreateUserPayload, type UpdateUserPayload
} from "./api";
import {
  Activity,
  Archive,
  BarChart3,
  BellRing,
  BriefcaseBusiness,
  Building2,
  CalendarCheck,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  ClipboardCheck,
  ClipboardList,
  ClipboardSignature,
  Coins,
  Database,
  FileArchive,
  FileCheck2,
  FileWarning,
  Gauge,
  HardHat,
  Link2,
  ListChecks,
  LockKeyhole,
  KeyRound,
  MapPin,
  PackageCheck,
  PackageMinus,
  PanelRightOpen,
  Plus,
  RefreshCw,
  ReceiptText,
  Route,
  Search,
  Send,
  Save,
  ShieldCheck,
  TimerReset,
  TrendingUp,
  Truck,
  UserCog,
  UserRoundCheck,
  UsersRound,
  WalletCards,
  Wrench,
  X,
  type LucideIcon,
} from "lucide-react";
import "./styles.css";

type ModuleId =
  | "overview"
  | "crm"
  | "supplychain"
  | "projects"
  | "workorders"
  | "equipment"
  | "inventory"
  | "outsourcing"
  | "finance"
  | "hr"
  | "oa"
  | "documents"
  | "accounts"
  | "permissions"
  | "bi";

type CrmTabId =
  | "summary"
  | "customers"
  | "leads"
  | "quotes"
  | "contracts"
  | "followups"
  | "renewals"
  | "receivables"
  | "insights";

type Tone = "good" | "warn" | "danger" | "info" | "neutral";

type WorkOrderStatus =
  | "待派工"
  | "已派工"
  | "现场处理中"
  | "待客户验收"
  | "已闭环";

type Contract = {
  id: string;
  customer: string;
  project: string;
  type: string;
  amount: number;
  period: string;
  serviceCycle: string;
  nextBill: string;
  nextInspect: string;
  receivable: number;
  status: "履约中" | "待续约" | "逾期风险";
};

type WorkOrder = {
  id: string;
  source: string;
  customer: string;
  project: string;
  type: string;
  priority: "紧急" | "高" | "普通";
  equipment: string;
  engineer: string;
  certificate: string;
  sla: string;
  status: WorkOrderStatus;
  cost: number;
  partIds: string[];
  accepted: boolean;
};

type Part = {
  id: string;
  name: string;
  model: string;
  stock: number;
  safety: number;
  location: string;
  supplier: string;
  unitCost: number;
};

type Receivable = {
  id: string;
  source: string;
  customer: string;
  amount: number;
  due: string;
  status: "待开票" | "待回款" | "已核销" | "逾期";
};

type Payable = {
  id: string;
  source: string;
  supplier: string;
  amount: number;
  due: string;
  status: "待付款" | "逾期" | "已付款";
};

type Approval = {
  id: string;
  type: string;
  title: string;
  owner: string;
  amount: number;
  status: "审批中" | "已通过" | "待补充";
  relatedPartId?: string;
  effect?: "purchase" | "contract" | "expense" | "outsourcing";
};

type FlowEvent = {
  id: string;
  time: string;
  title: string;
  detail: string;
  tone: Tone;
};

type LinkageRow = {
  trigger: string;
  source: string;
  action: string;
  target: string;
  ledger: string;
  owner: string;
  tone: Tone;
};

type BusinessRule = {
  id: string;
  title: string;
  scene: string;
  rule: string;
  result: string;
  owner: string;
  tone: Tone;
};

type CustomerInsight = {
  customer: string;
  revenue: number;
  cost: number;
  orders: number;
  faults: number;
  renewal: number;
};

type CustomerProfile = {
  id: string;
  name: string;
  industry: string;
  level: "战略客户" | "重点客户" | "普通客户";
  owner: string;
  contacts: string;
  sites: number;
  addresses: string[];
  payment: string;
  lastVisit: string;
  risk: "正常" | "逾期" | "续约风险";
  riskNote: string;
  invoice: {
    title: string;
    taxNo: string;
    bank: string;
    account: string;
    address: string;
    phone: string;
  };
};

type CrmLead = {
  id: string;
  customer: string;
  source: string;
  need: string;
  stage: "初访" | "需求确认" | "现场勘查" | "方案报价" | "商务谈判" | "赢单";
  owner: string;
  value: number;
  probability: number;
  nextAction: string;
  nextDate: string;
};

type QuotePlan = {
  id: string;
  customer: string;
  type: string;
  amount: number;
  scope: string;
  cycle: string;
  paymentNodes: string;
  owner: string;
  status: "草稿" | "成本已核对，可以报价" | "审批中" | "已通过";
};

type FollowUpActivity = {
  id: string;
  customer: string;
  type: "拜访" | "电话" | "回访" | "投诉";
  owner: string;
  date: string;
  summary: string;
  next: string;
  tone: Tone;
};

type AccountFormState = {
  username: string;
  displayName: string;
  password: string;
  phone: string;
  email: string;
  orgId: string;
  roleIds: string[];
  enabled: boolean;
};

const emptyAccountForm: AccountFormState = {
  username: "",
  displayName: "",
  password: "Admin@123",
  phone: "",
  email: "",
  orgId: "",
  roleIds: [],
  enabled: true,
};

const modules: Array<{
  id: ModuleId;
  label: string;
  group: string;
  icon: LucideIcon;
}> = [
  { id: "overview", label: "运营总览", group: "经营驾驶舱", icon: Gauge },
  { id: "crm", label: "CRM", group: "核心业务", icon: BriefcaseBusiness },
  { id: "supplychain", label: "供应链采购", group: "核心业务", icon: Truck },
  { id: "projects", label: "项目管理", group: "核心业务", icon: HardHat },
  { id: "inventory", label: "库存管理", group: "核心业务", icon: PackageCheck },
          { id: "finance", label: "财务资金", group: "资金管控", icon: Coins },
  { id: "hr", label: "组织人事", group: "底层支撑", icon: UsersRound },
  { id: "oa", label: "OA审批", group: "底层支撑", icon: ClipboardCheck },
  { id: "documents", label: "电子档案", group: "底层支撑", icon: FileArchive },
  { id: "accounts", label: "账号管理", group: "底层支撑", icon: UserCog },
  { id: "permissions", label: "权限审计", group: "底层支撑", icon: ShieldCheck },
  { id: "bi", label: "BI看板", group: "经营驾驶舱", icon: BarChart3 },
];

const crmTabs: Array<{
  id: CrmTabId;
  label: string;
  icon: LucideIcon;
}> = [
  { id: "summary", label: "CRM总览", icon: Gauge },
  { id: "customers", label: "客户池", icon: Building2 },
  { id: "leads", label: "线索商机", icon: TrendingUp },
  { id: "quotes", label: "报价方案", icon: ReceiptText },
  { id: "contracts", label: "客户合同", icon: BriefcaseBusiness },
  { id: "followups", label: "跟进回访", icon: CalendarCheck },
  { id: "renewals", label: "续约管理", icon: TimerReset },
  { id: "receivables", label: "合同应收", icon: WalletCards },
  { id: "insights", label: "客户画像", icon: BarChart3 },
];

const contracts: Contract[] = [
  {
    id: "HT-2026-018",
    customer: "华东轨交能源中心",
    project: "年度高压配电服务",
    type: "年度服务",
    amount: 1260000,
    period: "2026.01-2026.12",
    serviceCycle: "季度巡检",
    nextBill: "2026-07-10",
    nextInspect: "2026-07-03",
    receivable: 315000,
    status: "履约中",
  },
  {
    id: "HT-2026-027",
    customer: "新澄水务集团",
    project: "泵站现场服务",
    type: "现场服务",
    amount: 880000,
    period: "2026.03-2027.02",
    serviceCycle: "每日巡查",
    nextBill: "2026-06-30",
    nextInspect: "2026-06-27",
    receivable: 146000,
    status: "逾期风险",
  },
  {
    id: "HT-2025-144",
    customer: "启明商业广场",
    project: "消防联动系统服务",
    type: "设备质保合同",
    amount: 420000,
    period: "2025.08-2026.08",
    serviceCycle: "月度检测",
    nextBill: "2026-08-05",
    nextInspect: "2026-07-01",
    receivable: 70000,
    status: "待续约",
  },
];

const initialOrders: WorkOrder[] = [
  {
    id: "GD-202606-1128",
    source: "合同周期",
    customer: "华东轨交能源中心",
    project: "年度高压配电服务",
    type: "季度巡检",
    priority: "普通",
    equipment: "10kV进线柜 A-01",
    engineer: "陈一鸣",
    certificate: "高压电工证",
    sla: "24小时",
    status: "现场处理中",
    cost: 1260,
    partIds: ["P-010"],
    accepted: false,
  },
  {
    id: "GD-202606-1136",
    source: "客户报修",
    customer: "新澄水务集团",
    project: "泵站现场服务",
    type: "故障抢修",
    priority: "紧急",
    equipment: "3号提升泵变频器",
    engineer: "待派工",
    certificate: "低压电工证",
    sla: "4小时",
    status: "待派工",
    cost: 0,
    partIds: ["P-022"],
    accepted: false,
  },
  {
    id: "GD-202606-1098",
    source: "项目改造",
    customer: "启明商业广场",
    project: "消防联动系统服务",
    type: "设备更换",
    priority: "高",
    equipment: "消防控制主机 F-02",
    engineer: "陆景",
    certificate: "消防设施操作员",
    sla: "8小时",
    status: "待客户验收",
    cost: 3840,
    partIds: ["P-030"],
    accepted: false,
  },
];

const initialParts: Part[] = [
  {
    id: "P-010",
    name: "温湿度传感器",
    model: "TH-310",
    stock: 18,
    safety: 8,
    location: "A-02-04",
    supplier: "苏州智控仪表",
    unitCost: 260,
  },
  {
    id: "P-022",
    name: "变频器控制板",
    model: "VFD-CB-22",
    stock: 5,
    safety: 6,
    location: "B-01-03",
    supplier: "杭州泵联机电",
    unitCost: 1480,
  },
  {
    id: "P-030",
    name: "消防联动模块",
    model: "XF-LD-8",
    stock: 9,
    safety: 4,
    location: "C-03-01",
    supplier: "南京安消科技",
    unitCost: 620,
  },
  {
    id: "P-041",
    name: "高压熔断器",
    model: "XRNP-12",
    stock: 7,
    safety: 6,
    location: "A-01-02",
    supplier: "常州电气成套",
    unitCost: 390,
  },
];

const initialReceivables: Receivable[] = [
  {
    id: "YS-202606-041",
    source: "HT-2026-027",
    customer: "新澄水务集团",
    amount: 146000,
    due: "2026-06-30",
    status: "待开票",
  },
  {
    id: "YS-202606-036",
    source: "HT-2026-018",
    customer: "华东轨交能源中心",
    amount: 315000,
    due: "2026-07-10",
    status: "待回款",
  },
  {
    id: "YS-202605-019",
    source: "GD-202606-1098",
    customer: "启明商业广场",
    amount: 8600,
    due: "2026-06-20",
    status: "逾期",
  },
];

const initialPayables: Payable[] = [
  { id: "YF-202606-051", source: "CG-202606-1128", supplier: "上海电气控制", amount: 11840, due: "2026-07-15", status: "待付款" },
  { id: "YF-202606-052", source: "WB-202606-033", supplier: "新澄劳务", amount: 56000, due: "2026-07-05", status: "逾期" },
  { id: "YF-202606-053", source: "SP-202606-276", supplier: "员工报销-周舟", amount: 2360, due: "2026-06-25", status: "已付款" },
];

const initialApprovals: Approval[] = [
  {
    id: "SP-202606-281",
    type: "采购申请",
    title: "变频器控制板补货 8 件",
    owner: "仓储主管",
    amount: 11840,
    status: "审批中",
    relatedPartId: "P-022",
    effect: "purchase",
  },
  {
    id: "SP-202606-276",
    type: "费用报销",
    title: "泵站现场服务交通住宿",
    owner: "周舟",
    amount: 2360,
    status: "已通过",
    effect: "expense",
  },
  {
    id: "SP-202606-269",
    type: "合同审批",
    title: "启明商业广场续约报价",
    owner: "客户经理",
    amount: 468000,
    status: "待补充",
    effect: "contract",
  },
];

const initialEvents: FlowEvent[] = [
  {
    id: "EV-01",
    time: "09:16",
    title: "客户报修生成工单",
    detail: "新澄水务 3号提升泵变频器故障，等待持证工程师派工",
    tone: "danger",
  },
  {
    id: "EV-02",
    time: "10:05",
    title: "物料库存触发采购",
    detail: "变频器控制板低于安全库存，已进入采购审批",
    tone: "warn",
  },
  {
    id: "EV-03",
    time: "11:30",
    title: "项目成本归集",
    detail: "消防联动模块领用、现场工时已计入启明商业广场项目",
    tone: "info",
  },
  {
    id: "EV-04",
    time: "13:42",
    title: "合同节点生成应收",
    detail: "华东轨交二季度服务款进入待回款台账",
    tone: "good",
  },
];

const equipment = [
  {
    id: "SB-A-01",
    name: "10kV进线柜 A-01",
    customer: "华东轨交能源中心",
    address: "能源中心配电室一层",
    warranty: "2027-03-31",
    cycle: "季度",
    lastService: "2026-04-02",
    nextService: "2026-07-03",
    faults: 2,
    needCert: "高压电工证",
  },
  {
    id: "SB-B-17",
    name: "3号提升泵变频器",
    customer: "新澄水务集团",
    address: "城北泵站 3号机房",
    warranty: "2026-12-20",
    cycle: "月度",
    lastService: "2026-05-25",
    nextService: "2026-06-27",
    faults: 5,
    needCert: "低压电工证",
  },
  {
    id: "SB-F-02",
    name: "消防控制主机 F-02",
    customer: "启明商业广场",
    address: "B1 消控中心",
    warranty: "2026-08-12",
    cycle: "月度",
    lastService: "2026-06-01",
    nextService: "2026-07-01",
    faults: 3,
    needCert: "消防设施操作员",
  },
];

const projects = [
  {
    id: "XM-2026-052",
    name: "华东轨交配电扩容改造",
    customer: "华东轨交能源中心",
    stage: "调试",
    budget: 1680000,
    cost: 1146000,
    progress: 76,
    receivable: 580000,
    warranty: "2027-06-30",
  },
  {
    id: "XM-2026-039",
    name: "启明商业广场消防联动升级",
    customer: "启明商业广场",
    stage: "终验",
    budget: 740000,
    cost: 612000,
    progress: 93,
    receivable: 222000,
    warranty: "2027-03-15",
  },
  {
    id: "XM-2026-061",
    name: "新澄泵站远程监控改造",
    customer: "新澄水务集团",
    stage: "进场",
    budget: 920000,
    cost: 184000,
    progress: 18,
    receivable: 276000,
    warranty: "2027-09-30",
  },
];

const people = [
  {
    name: "陈一鸣",
    role: "高压设备工程师",
    certs: "高压电工证 / 登高证",
    certExpire: "2026-12-08",
    location: "浦东新区",
    schedule: "轨交现场",
    status: "现场中",
    output: 6.8,
  },
  {
    name: "周舟",
    role: "机电维修工程师",
    certs: "低压电工证",
    certExpire: "2026-08-18",
    location: "城北泵站",
    schedule: "可派工",
    status: "可派工",
    output: 7.4,
  },
  {
    name: "陆景",
    role: "消防设备工程师",
    certs: "消防设施操作员",
    certExpire: "2027-01-22",
    location: "静安区",
    schedule: "项目工单",
    status: "验收中",
    output: 5.9,
  },
  {
    name: "沈南",
    role: "焊接外协负责人",
    certs: "焊工证 / 登高证",
    certExpire: "2026-07-16",
    location: "松江区",
    schedule: "待命",
    status: "可派工",
    output: 4.1,
  },
];

const suppliers = [
  {
    name: "苏州智控仪表",
    type: "物料供货商",
    scope: "传感器 / 仪表",
    accountPeriod: "月结30天",
    payable: 38400,
    rating: "A",
  },
  {
    name: "杭州泵联机电",
    type: "设备厂家",
    scope: "泵站变频器",
    accountPeriod: "预付30%",
    payable: 69200,
    rating: "A-",
  },
  {
    name: "上海安衡检测",
    type: "外包检测",
    scope: "年度检测 / 第三方报告",
    accountPeriod: "验收后15天",
    payable: 24800,
    rating: "B+",
  },
];

const docs = [
  ["合同档案", "客户合同 / 补充协议", "128份", "3份待归档"],
  ["设备图纸", "竣工图 / 接线图 / 点位表", "346份", "12份本月新增"],
  ["检测报告", "年度检测 / 第三方报告", "94份", "5份待客户确认"],
  ["人员证书", "电工 / 焊工 / 登高 / 消防", "41份", "2份临期"],
  ["验收单", "工单验收 / 项目终验", "219份", "7份待签章"],
  ["招投标", "标书 / 报价 / 澄清函", "56份", "4份审批中"],
];

const ledgerRows = [
  ["服务收入", "HT-2026-018", 315000, "应收账款 / 主营业务收入"],
  ["物料成本", "GD-202606-1098", -620, "主营业务成本 / 库存商品"],
  ["人工成本", "GD-202606-1128", -1000, "项目成本 / 应付职工薪酬"],
  ["差旅费用", "SP-202606-276", -2360, "项目成本 / 银行存款"],
];

const roles = [
  ["服务工程师", "本人负责工单 / 负责项目设备", "照片、检测记录、领料申请"],
  ["项目经理", "管辖项目 / 项目成本 / 工单进度", "派工、验收、项目预算"],
  ["财务", "全量应收应付 / 凭证 / 报表", "开票、核销、付款"],
  ["老板", "全域经营数据", "BI看板、利润分析"],
];

const linkageRows: LinkageRow[] = [
  {
    trigger: "合同生效",
    source: "CRM",
    action: "按巡检频次生成计划",
    target: "项目管理",
    ledger: "付款节点生成应收",
    owner: "客户经理",
    tone: "good",
  },
  {
    trigger: "客户报修",
    source: "客户档案",
    action: "校验合同与设备台账",
    target: "项目管理",
    ledger: "单次维修生成应收",
    owner: "调度主管",
    tone: "danger",
  },
  {
    trigger: "工单领料",
    source: "项目管理",
    action: "扣减库存并归集成本",
    target: "库存管理",
    ledger: "库存成本入项目",
    owner: "仓储主管",
    tone: "info",
  },
  {
    trigger: "安全库存不足",
    source: "库存管理",
    action: "自动生成采购申请",
    target: "供应链采购",
    ledger: "审批后入库生成应付",
    owner: "采购",
    tone: "warn",
  },
  {
    trigger: "项目验收",
    source: "项目管理",
    action: "归集工时/物料/外包/差旅",
    target: "财务资金",
    ledger: "结转收入与毛利",
    owner: "项目经理",
    tone: "good",
  },
  {
    trigger: "证书临期",
    source: "HR",
    action: "限制派工并提醒复审",
    target: "项目管理",
    ledger: "影响绩效与排班",
    owner: "人事",
    tone: "warn",
  },
];

const businessRules: BusinessRule[] = [
  {
    id: "RULE-01",
    title: "审批未通过不生效",
    scene: "采购、外包、报销、合同",
    rule: "状态为审批中或待补充时，只能暂存，不允许入库、付款或生成凭证。",
    result: "阻断库存与财务变动",
    owner: "OA",
    tone: "danger",
  },
  {
    id: "RULE-02",
    title: "派工必须匹配资质",
    scene: "高压、消防、登高、焊接",
    rule: "工单所需证书与人员证书不匹配时，调度动作被拦截。",
    result: "降低现场合规风险",
    owner: "HR",
    tone: "warn",
  },
  {
    id: "RULE-03",
    title: "领料自动归集成本",
    scene: "维修、巡检、改造项目",
    rule: "物料从工单领用后，库存减少，并写入工单与项目成本。",
    result: "实时计算服务毛利",
    owner: "仓储",
    tone: "info",
  },
  {
    id: "RULE-04",
    title: "客户验收触发应收",
    scene: "单次维修、合同节点、项目验收",
    rule: "客户线上签字后，系统生成应收单并推送财务开票。",
    result: "减少漏收与延迟开票",
    owner: "财务",
    tone: "good",
  },
  {
    id: "RULE-05",
    title: "质保期免费工单",
    scene: "工程项目质保期内",
    rule: "质保期内设备故障可生成免费工单，但人工与物料仍进入项目售后成本。",
    result: "看清真实项目利润",
    owner: "项目",
    tone: "info",
  },
];

const customerInsights: CustomerInsight[] = [
  {
    customer: "华东轨交能源中心",
    revenue: 1260000,
    cost: 486000,
    orders: 38,
    faults: 2,
    renewal: 92,
  },
  {
    customer: "新澄水务集团",
    revenue: 880000,
    cost: 412000,
    orders: 56,
    faults: 5,
    renewal: 72,
  },
  {
    customer: "启明商业广场",
    revenue: 420000,
    cost: 286000,
    orders: 31,
    faults: 3,
    renewal: 64,
  },
];

const crmCustomers: CustomerProfile[] = [
  {
    id: "KH-001",
    name: "华东轨交能源中心",
    industry: "轨道交通",
    level: "战略客户",
    owner: "客户经理A",
    contacts: "王主任 / 设备处",
    sites: 3,
    addresses: ["浦东能源中心配电室", "虹桥车辆基地", "北站区间泵房"],
    payment: "季度付款，信用良好",
    lastVisit: "2026-06-18",
    risk: "正常",
    riskNote: "付款节奏稳定，设备扩容改造有追加机会。",
    invoice: {
      title: "华东轨交能源中心有限公司",
      taxNo: "91310000MA1GD8A18X",
      bank: "中国建设银行上海张江支行",
      account: "31001588882050011234",
      address: "上海市浦东新区龙东大道1688号",
      phone: "021-5899 1026",
    },
  },
  {
    id: "KH-002",
    name: "新澄水务集团",
    industry: "市政水务",
    level: "重点客户",
    owner: "客户经理B",
    contacts: "刘工 / 设备部",
    sites: 8,
    addresses: ["城北泵站", "城南泵站", "东区调蓄池", "西区加压站"],
    payment: "月结，存在逾期",
    lastVisit: "2026-06-21",
    risk: "逾期",
    riskNote: "夜间响应要求提高，本月人员排班与回款需要同步跟进。",
    invoice: {
      title: "新澄水务集团有限公司",
      taxNo: "91320582MA25P6XQ9B",
      bank: "中国农业银行新澄支行",
      account: "10288101040021876",
      address: "江苏省新澄市水务路66号",
      phone: "0512-6688 9021",
    },
  },
  {
    id: "KH-003",
    name: "启明商业广场",
    industry: "商业综合体",
    level: "重点客户",
    owner: "客户经理C",
    contacts: "赵经理 / 物业工程",
    sites: 1,
    addresses: ["启明商业广场 B1 消控中心"],
    payment: "验收后付款",
    lastVisit: "2026-06-12",
    risk: "续约风险",
    riskNote: "合同8月到期，客户关注消防联动误报率与续约价格。",
    invoice: {
      title: "上海启明商业管理有限公司",
      taxNo: "91310106MA1FY2M33H",
      bank: "招商银行上海静安支行",
      account: "121908774610808",
      address: "上海市静安区启明路299号",
      phone: "021-6266 3188",
    },
  },
  {
    id: "KH-004",
    name: "东城数据中心",
    industry: "数据中心",
    level: "普通客户",
    owner: "客户经理A",
    contacts: "沈总 / 基建部",
    sites: 2,
    addresses: ["东城数据中心一期机房", "东城数据中心二期UPS间"],
    payment: "合同预付30%",
    lastVisit: "2026-06-24",
    risk: "正常",
    riskNote: "当前处于现场勘查阶段，重点推进UPS物料包报价。",
    invoice: {
      title: "东城数据中心有限公司",
      taxNo: "91310115MA1K4P7L8R",
      bank: "浦发银行上海自贸区支行",
      account: "66070154800001236",
      address: "上海市浦东新区云桥路588号",
      phone: "021-5088 7716",
    },
  },
];

const initialLeads: CrmLead[] = [
  {
    id: "XS-202606-071",
    customer: "东城数据中心",
    source: "老客户转介绍",
    need: "UPS与配电年度服务",
    stage: "现场勘查",
    owner: "客户经理A",
    value: 680000,
    probability: 68,
    nextAction: "提交现场勘查报告",
    nextDate: "2026-06-28",
  },
  {
    id: "XS-202606-066",
    customer: "临港生物医药园",
    source: "招标公告",
    need: "园区机电现场服务",
    stage: "需求确认",
    owner: "客户经理B",
    value: 1380000,
    probability: 52,
    nextAction: "确认现场人数与服务边界",
    nextDate: "2026-06-30",
  },
  {
    id: "XS-202606-058",
    customer: "南湖会展中心",
    source: "官网咨询",
    need: "消防系统年度检测",
    stage: "方案报价",
    owner: "客户经理C",
    value: 360000,
    probability: 74,
    nextAction: "报价审批后发送客户",
    nextDate: "2026-06-27",
  },
];

const initialQuotes: QuotePlan[] = [
  {
    id: "BJ-202606-038",
    customer: "南湖会展中心",
    type: "年度检测",
    amount: 360000,
    scope: "消防联动、喷淋、报警主机年度检测",
    cycle: "月度巡检 + 年度检测",
    paymentNodes: "合同签订30% / 检测完成50% / 报告归档20%",
    owner: "客户经理C",
    status: "成本已核对，可以报价",
  },
  {
    id: "BJ-202606-041",
    customer: "启明商业广场",
    type: "续约服务",
    amount: 468000,
    scope: "消防系统服务、故障抢修、年度联动测试",
    cycle: "月度巡检",
    paymentNodes: "半年付",
    owner: "客户经理C",
    status: "审批中",
  },
  {
    id: "BJ-202606-045",
    customer: "东城数据中心",
    type: "年度服务",
    amount: 680000,
    scope: "UPS、配电柜、温湿度监控服务",
    cycle: "季度巡检 + 7x24故障响应",
    paymentNodes: "季度付",
    owner: "客户经理A",
    status: "草稿",
  },
];

const crmFollowUps: FollowUpActivity[] = [
  {
    id: "GJ-202606-210",
    customer: "新澄水务集团",
    type: "回访",
    owner: "客服主管",
    date: "2026-06-25",
    summary: "泵站变频器故障回访，客户要求缩短夜间响应时间。",
    next: "调整人员排班并反馈项目经理",
    tone: "warn",
  },
  {
    id: "GJ-202606-205",
    customer: "东城数据中心",
    type: "拜访",
    owner: "客户经理A",
    date: "2026-06-24",
    summary: "现场确认UPS服务范围，客户关注物料响应和应急演练。",
    next: "补充物料包报价",
    tone: "info",
  },
  {
    id: "GJ-202606-198",
    customer: "华东轨交能源中心",
    type: "电话",
    owner: "客户经理A",
    date: "2026-06-22",
    summary: "确认二季度付款节点，财务本周安排回款。",
    next: "财务跟进核销",
    tone: "good",
  },
];

const formatMoney = (value: number) =>
  new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    maximumFractionDigits: 0,
  }).format(value);

const toneLabel: Record<Tone, string> = {
  good: "正常",
  warn: "预警",
  danger: "紧急",
  info: "流转",
  neutral: "记录",
};

const moduleGroups = Array.from(new Set(modules.map((item) => item.group)));

function App() {
  const [activeModule, setActiveModule] = useState<ModuleId>("overview");
  const [orders, setOrders] = useState<WorkOrder[]>(initialOrders);
  const [parts, setParts] = useState<Part[]>(initialParts);
  const [leads, setLeads] = useState<CrmLead[]>(initialLeads);
  const [quotes, setQuotes] = useState<QuotePlan[]>(initialQuotes);
  const [activeCrmTab, setActiveCrmTab] = useState<CrmTabId>("summary");
  const [crmMenuOpen, setCrmMenuOpen] = useState(true);
  const [selectedCustomerId, setSelectedCustomerId] = useState(crmCustomers[0].id);
  const [receivables, setReceivables] =
    useState<Receivable[]>(initialReceivables);
  const [payables, setPayables] = useState<Payable[]>(initialPayables);
  const [approvals, setApprovals] = useState<Approval[]>(initialApprovals);
  const [events, setEvents] = useState<FlowEvent[]>(initialEvents);
  const [vouchers, setVouchers] = useState<VoucherResponse[]>([]);
  const [ledgerOverview, setLedgerOverview] = useState<LedgerOverviewResponse | null>(null);
  const [financialStatements, setFinancialStatements] = useState<FinancialStatementsResponse | null>(null);
  const [paymentApps, setPaymentApps] = useState<PaymentApplicationResponse[]>([]);
  const [paymentRecords, setPaymentRecords] = useState<PaymentRecordResponse[]>([]);
  const [filterDateStart, setFilterDateStart] = useState("");
  const [filterDateEnd, setFilterDateEnd] = useState("");
  const [auditLog, setAuditLog] = useState<Array<{id:string;time:string;user:string;action:string;detail:string;entity:string}>>([]);
  const [systemUsers, setSystemUsers] = useState<UserResponse[]>([]);
  const [systemRoles, setSystemRoles] = useState<RoleResponse[]>([]);
  const [systemOrganizations, setSystemOrganizations] = useState<OrganizationResponse[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [accountForm, setAccountForm] = useState<AccountFormState>(emptyAccountForm);
  const [accountMode, setAccountMode] = useState<"create" | "edit">("create");
  const [accountSaving, setAccountSaving] = useState(false);
  const [isPeriodClosed, setPeriodClosed] = useState(false);
  const logAudit = (action: string, detail: string, entity: string) => {
    setAuditLog(prev => [{id:"LOG-"+Date.now(),time:new Date().toLocaleString("zh-CN"),user:"系统管理员",action,detail,entity}, ...prev].slice(0,200));
  };
  const [voucherDetailId, setVoucherDetail] = useState<string | null>(null);
  const [selectedContractId, setSelectedContractId] = useState(contracts[0].id);
  const [selectedEquipmentId, setSelectedEquipmentId] = useState(equipment[1].id);
  const [selectedOrderId, setSelectedOrderId] = useState(initialOrders[0].id);
  const [fault, setFault] = useState("变频器报警，现场无法复位");
  const [toast, setToast] = useState("企业一体化管理系统已加载");
  const [initializing, setInitializing] = useState(true);
  const receivableApiData = useRef<Map<string, ReceivableResponse>>(new Map());
  const payableApiData = useRef<Map<string, FinancePayableResponse>>(new Map());

  const refreshAccounts = async () => {
    const [usersPage, rolesPage, organizations] = await Promise.all([
      fetchUsers(0, 100),
      fetchRoles(0, 100),
      fetchOrganizationsFlat(),
    ]);
    setSystemUsers(usersPage.content);
    setSystemRoles(rolesPage.content);
    setSystemOrganizations(organizations);
    setSelectedUserId((current) => current ?? usersPage.content[0]?.id ?? null);
  };

  const loadAccountForm = (user: UserResponse) => {
    setAccountMode("edit");
    setSelectedUserId(user.id);
    setAccountForm({
      username: user.username,
      displayName: user.displayName,
      password: "",
      phone: user.phone ?? "",
      email: user.email ?? "",
      orgId: user.orgId ?? "",
      roleIds: user.roles.map((role) => role.id),
      enabled: user.enabled,
    });
  };

  const startCreateAccount = () => {
    setAccountMode("create");
    setSelectedUserId(null);
    setAccountForm(emptyAccountForm);
  };

  // Auto-login + fetch on mount
  useEffect(() => {
    const init = async () => {
      try {
        if (!getToken()) await login("admin", "Admin@123");
        setToast("已连接到服务器");
        const [rcList, pyList] = await Promise.all([
          fetchReceivables(),
          fetchPayables(),
        ]);

        // Map receivable status
        const mapRc = (s: string) =>
          s === "INVOICE_PENDING" ? "待开票" as const
          : s === "PAYMENT_PENDING" ? "待回款" as const
          : s === "SETTLED" ? "已核销" as const
          : "逾期" as const;

        receivableApiData.current.clear();
        const mappedRc: Receivable[] = rcList.map((r: ReceivableResponse) => {
          receivableApiData.current.set(r.code, r);
          return { id: r.code, source: r.sourceNo || r.contractCode, customer: r.customerName, amount: r.amount, due: r.dueDate, status: mapRc(r.status) };
        });
        setReceivables(mappedRc);

        payableApiData.current.clear();
        const mappedPy: Payable[] = pyList.map((p: FinancePayableResponse) => ({
          id: p.code, source: p.orderCode, supplier: p.supplierName,
          amount: p.outstandingAmount, due: p.dueDate,
          status: p.overdue || (p.status === "PENDING" && new Date(p.dueDate) < new Date()) ? "逾期" as const
                  : p.status === "PAID" ? "已付款" as const
                  : "待付款" as const,
        }));
        pyList.forEach((p: FinancePayableResponse) => payableApiData.current.set(p.code, p));
        setPayables(mappedPy);
        // Fetch ledger + payment data
        Promise.all([
          fetchLedgerOverview().catch(() => null),
          fetchLedgerVouchers().catch(() => []),
          fetchFinancialStatements().catch(() => null),
          fetchPaymentApplications().catch(() => []),
          fetchPaymentRecords().catch(() => []),
        ]).then(([lo, v, fs, pa, pr]) => {
          if (lo) setLedgerOverview(lo);
          if (v.length) setVouchers(v as VoucherResponse[]);
          if (fs) setFinancialStatements(fs);
          if (pa.length) setPaymentApps(pa as PaymentApplicationResponse[]);
          if (pr.length) setPaymentRecords(pr as PaymentRecordResponse[]);
        }).catch(() => {});

        refreshAccounts().catch(() => {
          setSystemUsers([]);
          setToast("账号管理数据未加载，请确认当前账号具备系统管理权限");
        });

        setToast("已加载后端数据");
      } catch (e) {
        console.warn("后端未连接，使用 mock 数据", e);
        setToast("离线模式 — 使用演示数据");
      } finally {
        setInitializing(false);
      }
    };
    init();
  }, []);


  function handleInvoice(code: string) {
    const apiItem = receivableApiData.current.get(code);
    if (apiItem) {
      const today = new Date().toISOString().slice(0, 10);
      registerInvoice(apiItem.id, "INV-" + Date.now(), today)
        .then(() => fetchReceivables())
        .then((rcList) => {
          const mapRc = (s: string) =>
            s === "INVOICE_PENDING" ? "待开票" as const
            : s === "PAYMENT_PENDING" ? "待回款" as const
            : s === "SETTLED" ? "已核销" as const
            : "逾期" as const;
          receivableApiData.current.clear();
          const mapped = rcList.map((r: ReceivableResponse) => {
            receivableApiData.current.set(r.code, r);
            return { id: r.code, source: r.sourceNo || r.contractCode, customer: r.customerName, amount: r.amount, due: r.dueDate, status: mapRc(r.status) } as Receivable;
          });
          setReceivables(mapped);
          pushEvent("应收开票", code + " 已开票", "good");
          logAudit("开票", code + " 已开票", "Receivable");
        })
        .catch((err) => pushEvent("开票失败", err.message, "danger"));
    } else {
      setReceivables((current) =>
        current.map((item) =>
          item.id === code && item.status === "待开票"
            ? { ...item, status: "待回款" as const }
            : item
        )
      );
      pushEvent("应收开票(离线)", code + " 已标记开票", "good");
    }
  }

  function handleCollect(code: string) {
    const apiItem = receivableApiData.current.get(code);
    if (apiItem) {
      const today = new Date().toISOString().slice(0, 10);
      recordReceipt(apiItem.id, apiItem.outstandingAmount || apiItem.amount, today, "RC-" + Date.now(), "系统管理员")
        .then(() => fetchReceivables())
        .then((rcList) => {
          const mapRc = (s: string) =>
            s === "INVOICE_PENDING" ? "待开票" as const
            : s === "PAYMENT_PENDING" ? "待回款" as const
            : s === "SETTLED" ? "已核销" as const
            : "逾期" as const;
          receivableApiData.current.clear();
          const mapped = rcList.map((r: ReceivableResponse) => {
            receivableApiData.current.set(r.code, r);
            return { id: r.code, source: r.sourceNo || r.contractCode, customer: r.customerName, amount: r.amount, due: r.dueDate, status: mapRc(r.status) } as Receivable;
          });
          setReceivables(mapped);
          pushEvent("回款登记", code + " 回款已登记", "good");
          logAudit("回款", code + " 已回款登记", "Receivable");
        })
        .catch((err) => pushEvent("回款失败", err.message, "danger"));
    } else {
      setReceivables((current) =>
        current.map((item) =>
          item.id === code && item.status === "待回款"
            ? { ...item, status: "已核销" as const }
            : item
        )
      );
      pushEvent("回款登记(离线)", code + " 已标记回款", "good");
    }
  }

  function handleWriteOff(code: string) {
    setReceivables((current) =>
      current.map((item) =>
        item.id === code
          ? { ...item, status: "已核销" as const }
          : item
      )
    );
    pushEvent("应收核销", code + " 已手动核销", "good");
    logAudit("核销", code + " 已核销", "Receivable");
  }


  const selectedModule =
    modules.find((item) => item.id === activeModule) ||
    ({ label: "项目管理" } as { label: string });
  const activeCrmTabConfig = crmTabs.find((tab) => tab.id === activeCrmTab)!;
  const selectedPageLabel =
    activeModule === "crm" ? activeCrmTabConfig.label : selectedModule.label;
  const selectedOrder =
    orders.find((item) => item.id === selectedOrderId) || orders[0];

  const metrics = useMemo(() => {
    const orderOpen = orders.filter((item) => item.status !== "已闭环").length;
    const urgent = orders.filter((item) => item.priority === "紧急").length;
    const lowStock = parts.filter((item) => item.stock <= item.safety).length;
    const overdue = receivables.filter((item) => item.status === "逾期").length;
    const pendingApprovals = approvals.filter(
      (item) => item.status !== "已通过",
    ).length;
    const receivableTotal = receivables
      .filter((item) => item.status !== "已核销")
      .reduce((sum, item) => sum + item.amount, 0);
    const cost = orders.reduce((sum, item) => sum + item.cost, 0);    const payableTotal = payables
      .filter((item) => item.status !== "已付款")
      .reduce((sum, item) => sum + item.amount, 0);
    const overduePayable = payables.filter((item) => item.status === "逾期").length;
    const revenue = contracts.reduce((sum, item) => sum + item.amount, 0);
    const gross = revenue - projects.reduce((sum, item) => sum + item.cost, 0);
    return {
      orderOpen,
      urgent,
      lowStock,
      overdue,
      pendingApprovals,
      receivableTotal,
      payableTotal,
      overduePayable,
      cost,
      revenue,
      gross,
    };
  }, [orders, parts, receivables, payables, approvals]);

  const pushEvent = (title: string, detail: string, tone: Tone) => {
    const event: FlowEvent = {
      id: `EV-${Date.now()}`,
      time: "刚刚",
      title,
      detail,
      tone,
    };
    setEvents((current) => [event, ...current].slice(0, 8));
    setToast(detail);
  };

  const createRepairOrder = () => {
    const contract = contracts.find((item) => item.id === selectedContractId)!;
    const asset = equipment.find((item) => item.id === selectedEquipmentId)!;
    const nextId = `GD-202606-${1138 + orders.length}`;
    const newOrder: WorkOrder = {
      id: nextId,
      source: "客户报修",
      customer: contract.customer,
      project: contract.project,
      type: "故障抢修",
      priority: "高",
      equipment: asset.name,
      engineer: "待派工",
      certificate: asset.needCert,
      sla: "8小时",
      status: "待派工",
      cost: 0,
      partIds: ["P-041"],
      accepted: false,
    };
    setOrders((current) => [newOrder, ...current]);
    setSelectedOrderId(nextId);
    setActiveModule("projects");
    pushEvent("客户报修生成工单", `${contract.customer}：${fault}`, "danger");
  };

  const createInspectionOrder = (contract: Contract) => {
    const asset =
      equipment.find((item) => item.customer === contract.customer) ||
      equipment[0];
    const nextId = `GD-202606-${1146 + orders.length}`;
    const newOrder: WorkOrder = {
      id: nextId,
      source: "合同周期",
      customer: contract.customer,
      project: contract.project,
      type: "周期巡检",
      priority: "普通",
      equipment: asset.name,
      engineer: "待派工",
      certificate: asset.needCert,
      sla: "24小时",
      status: "待派工",
      cost: 0,
      partIds: ["P-010"],
      accepted: false,
    };
    setOrders((current) => [newOrder, ...current]);
    setSelectedOrderId(nextId);
    setActiveModule("projects");
    pushEvent(
      "合同周期触发巡检",
      `${contract.id} 已生成 ${asset.name} 巡检工单`,
      "info",
    );
  };

  const nextOrderAction = (order: WorkOrder) => {
    setSelectedOrderId(order.id);
    const nextStatus: Record<WorkOrderStatus, WorkOrderStatus> = {
      待派工: "已派工",
      已派工: "现场处理中",
      现场处理中: "待客户验收",
      待客户验收: "已闭环",
      已闭环: "已闭环",
    };

    setOrders((current) =>
      current.map((item) => {
        if (item.id !== order.id) return item;
        const matchedPerson = people.find((person) =>
          person.certs.includes(item.certificate),
        );
        return {
          ...item,
          engineer:
            item.status === "待派工" && matchedPerson
              ? matchedPerson.name
              : item.engineer,
          status: nextStatus[item.status],
          accepted: item.status === "待客户验收" ? true : item.accepted,
        };
      }),
    );

    if (order.status === "待客户验收") {
      const receivable: Receivable = {
        id: `YS-202606-${44 + receivables.length}`,
        source: order.id,
        customer: order.customer,
        amount: order.type === "故障抢修" ? 6800 : 0,
        due: "2026-07-15",
        status: order.type === "故障抢修" ? "待开票" : "已核销",
      };
      setReceivables((current) => [receivable, ...current]);
      pushEvent(
        "工单客户验收闭环",
        `${order.id} 已生成应收与总账凭证线索`,
        "good",
      );
    } else {
      pushEvent(
        "工单状态更新",
        `${order.id} 从 ${order.status} 进入 ${nextStatus[order.status]}`,
        "info",
      );
    }
  };

  const consumeParts = (order: WorkOrder) => {
    setSelectedOrderId(order.id);
    if (order.status === "待派工") {
      pushEvent(
        "领料被规则拦截",
        `${order.id} 尚未派工，需先匹配持证工程师`,
        "warn",
      );
      return;
    }

    const consumed = order.partIds
      .map((partId) => parts.find((part) => part.id === partId))
      .filter(Boolean) as Part[];
    if (!consumed.length) return;

    setParts((current) =>
      current.map((part) =>
        order.partIds.includes(part.id)
          ? { ...part, stock: Math.max(0, part.stock - 1) }
          : part,
      ),
    );
    setOrders((current) =>
      current.map((item) =>
        item.id === order.id
          ? {
              ...item,
              cost:
                item.cost +
                consumed.reduce((sum, part) => sum + part.unitCost, 0),
            }
          : item,
      ),
    );

    const lowPart = consumed.find((part) => part.stock - 1 <= part.safety);
    if (lowPart) {
      const newApproval: Approval = {
        id: `SP-202606-${288 + approvals.length}`,
        type: "采购申请",
        title: `${lowPart.name} 低库存补货`,
        owner: "系统自动",
        amount: lowPart.unitCost * 8,
        status: "审批中",
        relatedPartId: lowPart.id,
        effect: "purchase",
      };
      setApprovals((current) => [newApproval, ...current]);
      pushEvent(
        "领料触发采购审批",
        `${lowPart.name} 已低于安全库存，采购申请进入OA`,
        "warn",
      );
    } else {
      pushEvent(
        "工单领料完成",
        `${order.id} 消耗 ${consumed.map((part) => part.name).join("、")}`,
        "info",
      );
    }
  };

  const createPurchaseApproval = (part: Part) => {
    const newApproval: Approval = {
      id: `SP-202606-${300 + approvals.length}`,
      type: "采购申请",
      title: `${part.name} 补货 ${Math.max(8, part.safety * 2)} 件`,
      owner: "仓储主管",
      amount: part.unitCost * Math.max(8, part.safety * 2),
      status: "审批中",
      relatedPartId: part.id,
      effect: "purchase",
    };
    setApprovals((current) => [newApproval, ...current]);
    setActiveModule("supplychain");
    pushEvent("物料补货申请", `${part.name} 已进入供应链采购审批`, "warn");
  };

  const advanceLead = (lead: CrmLead) => {
    if (lead.stage === "方案报价") {
      const quoteId = lead.id.replace(/^XS-/, "BJ-");
      setQuotes((current) =>
        current.some((item) => item.id === quoteId)
          ? current
          : [
              {
                id: quoteId,
                customer: lead.customer,
                type: lead.need,
                amount: lead.value,
                scope: lead.need,
                cycle: "待成本负责人核对",
                paymentNodes: "待确认",
                owner: lead.owner,
                status: "草稿",
              },
              ...current,
            ],
      );
      setActiveModule("crm");
      setActiveCrmTab("quotes");
      pushEvent("新增报价", `${lead.customer} 已生成报价草稿，并发起售前成本核算`, "info");
      return;
    }
    const stages: CrmLead["stage"][] = [
      "初访",
      "需求确认",
      "现场勘查",
      "方案报价",
      "商务谈判",
      "赢单",
    ];
    const currentIndex = stages.indexOf(lead.stage);
    const nextStage = stages[Math.min(currentIndex + 1, stages.length - 1)];
    setLeads((current) =>
      current.map((item) =>
        item.id === lead.id
          ? {
              ...item,
              stage: nextStage,
              probability: Math.min(95, item.probability + 10),
              nextAction:
                nextStage === "方案报价"
                  ? "生成报价方案"
                  : nextStage === "商务谈判"
                    ? "确认合同条款与付款节点"
                    : nextStage === "赢单"
                      ? "转入客户合同与项目计划"
                      : item.nextAction,
            }
          : item,
      ),
    );
    pushEvent(
      "商机阶段推进",
      `${lead.customer} 已进入 ${nextStage}`,
      nextStage === "赢单" ? "good" : "info",
    );
  };

  const submitQuoteApproval = (quote: QuotePlan) => {
    if (quote.status !== "成本已核对，可以报价") {
      pushEvent("报价无需重复提交", `${quote.id} 当前为${quote.status}`, "neutral");
      return;
    }
    setQuotes((current) =>
      current.map((item) =>
        item.id === quote.id ? { ...item, status: "审批中" } : item,
      ),
    );
    const approval: Approval = {
      id: `SP-202606-${320 + approvals.length}`,
      type: "报价审批",
      title: `${quote.customer} ${quote.type}`,
      owner: quote.owner,
      amount: quote.amount,
      status: "审批中",
      effect: "contract",
    };
    setApprovals((current) => [approval, ...current]);
    pushEvent(
      "报价提交审批",
      `${quote.customer} 报价 ${formatMoney(quote.amount)} 已进入OA审批`,
      "warn",
    );
  };

  const approveApproval = (approval: Approval) => {
    if (approval.status === "已通过") {
      pushEvent("审批无需重复处理", `${approval.id} 已通过`, "neutral");
      return;
    }

    setApprovals((current) =>
      current.map((item) =>
        item.id === approval.id ? { ...item, status: "已通过" } : item,
      ),
    );

    if (approval.effect === "purchase" && approval.relatedPartId) {
      const part = parts.find((item) => item.id === approval.relatedPartId);
      const restockQty = 8;
      setParts((current) =>
        current.map((item) =>
          item.id === approval.relatedPartId
            ? { ...item, stock: item.stock + restockQty }
            : item,
        ),
      );
      pushEvent(
        "采购审批通过并入库",
        `${part?.name || approval.title} 入库 ${restockQty} 件，同步生成应付账款`,
        "good",
      );
      return;
    }

    pushEvent(
      "OA审批通过",
      `${approval.title} 已生效，相关业务单据可继续流转`,
      "good",
    );
  };

  return (
    <div className="app-shell">
        {initializing && (
          <div style={{
            position: "fixed" as const, inset: 0, zIndex: 9999,
            display: "flex", flexDirection: "column",
            alignItems: "center", justifyContent: "center",
            background: "#f5f6f4", gap: 16,
            fontFamily: "Inter, system-ui, sans-serif",
          }}>
            <div style={{
              width: 40, height: 40,
              border: "3px solid #dfe5df",
              borderTopColor: "#1f6a5b",
              borderRadius: "50%",
              animation: "spin 0.8s linear infinite",
            }} />
            <p style={{ color: "#52605c", fontSize: 14, margin: 0 }}>连接服务器中...</p>
          </div>
        )}
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Route size={22} />
          </div>
          <div>
            <strong>企业管理系统</strong>
            <span>项目 · 工单 · 财务一体化</span>
          </div>
        </div>

        <nav className="module-nav" aria-label="系统模块">
          {moduleGroups.map((group) => (
            <div className="nav-group" key={group}>
              <p>{group}</p>
              {modules
                .filter((item) => item.group === group)
                .map((item) => {
                  const Icon = item.icon;
                  if (item.id === "crm") {
                    return (
                      <div className="nav-dropdown" key={item.id}>
                        <button
                          className={activeModule === item.id ? "active" : ""}
                          type="button"
                          onClick={() => {
                            setActiveModule("crm");
                            setCrmMenuOpen((current) => !current);
                          }}
                          title={item.label}
                        >
                          <Icon size={18} />
                          <span>{item.label}</span>
                          <ChevronRight
                            className={crmMenuOpen ? "open" : ""}
                            size={16}
                          />
                        </button>
                        {crmMenuOpen && (
                          <div className="nav-submenu">
                            {crmTabs.map((tab) => {
                              const TabIcon = tab.icon;
                              return (
                                <button
                                  className={
                                    activeModule === "crm" &&
                                    activeCrmTab === tab.id
                                      ? "active"
                                      : ""
                                  }
                                  key={tab.id}
                                  type="button"
                                  onClick={() => {
                                    setActiveModule("crm");
                                    setActiveCrmTab(tab.id);
                                    setCrmMenuOpen(true);
                                  }}
                                  title={tab.label}
                                >
                                  <TabIcon size={15} />
                                  <span>{tab.label}</span>
                                </button>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    );
                  }
                  return (
                    <button
                      className={activeModule === item.id ? "active" : ""}
                      key={item.id}
                      type="button"
                      onClick={() => setActiveModule(item.id)}
                      title={item.label}
                    >
                      <Icon size={18} />
                      <span>{item.label}</span>
                      {item.id === "finance" && paymentApps.filter(a => a.status === "PENDING").length > 0 && (
                        <span style={{position:"absolute",top:"50%",right:8,transform:"translateY(-50%)",background:"#bd3f2f",color:"#fff",fontSize:10,borderRadius:8,minWidth:16,height:16,display:"flex",alignItems:"center",justifyContent:"center",padding:"0 4px",lineHeight:1}}>{paymentApps.filter(a => a.status === "PENDING").length}</span>
                      )}
                      {activeModule === item.id && <ChevronRight size={16} />}
                    </button>
                  );
                })}
            </div>
          ))}
        </nav>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div>
            <p className="eyebrow">2026-06-26 · 上海运营中心</p>
            <h1>{selectedPageLabel}</h1>
          </div>
          <div className="top-actions">
            <label className="search-box">
              <Search size={18} />
              <input placeholder="客户、设备、工单、合同" />
            </label>
            <button className="icon-button" type="button" title="待办提醒">
              <BellRing size={19} />
              <span>
                {metrics.urgent +
                  metrics.lowStock +
                  metrics.overdue +
                  metrics.pendingApprovals}
              </span>
            </button>
          </div>
        </header>

        <section className="toast-strip" aria-live="polite">
          <Activity size={17} />
          <span>{toast}</span>
        </section>

        {renderModule()}
      </main>
    </div>
  );

  function renderModule() {
    switch (activeModule) {
      case "crm":
        return <CrmModule />;
      case "supplychain":
        return <SupplyChainModule />;
      case "projects":
        return <ProjectsModule />;
      case "workorders":
        return <WorkOrdersModule />;
      case "equipment":
        return <EquipmentModule />;
      case "inventory":
        return <InventoryModule />;
      case "outsourcing":
        return <OutsourcingModule />;
      case "finance":
        return <FinanceModule />;
      case "hr":
        return <HrModule />;
      case "oa":
        return <OaModule />;
      case "documents":
        return <DocumentsModule />;
      case "accounts":
        return <AccountsModule />;
      case "permissions":
        return <PermissionsModule />;
      case "bi":
        return <BiModule />;
      default:
        return <OverviewModule />;
    }
  }

  function OverviewModule() {
    return (
      <div className="page-stack">
        <section className="metric-grid">
          <MetricCard
            icon={Wrench}
            label="未闭环工单"
            value={`${metrics.orderOpen}单`}
            meta={`${metrics.urgent}单紧急`}
            tone="danger"
          />
          <MetricCard
            icon={WalletCards}
            label="待收款金额"
            value={formatMoney(metrics.receivableTotal)}
            meta={`${metrics.overdue}笔逾期`}
            tone="warn"
          />
          <MetricCard
            icon={PackageMinus}
            label="低库存物料"
            value={`${metrics.lowStock}类`}
            meta="自动触发采购"
            tone="info"
          />
          <MetricCard
            icon={Coins}
            label="工单已归集成本"
            value={formatMoney(metrics.cost)}
            meta="人工 + 物料"
            tone="good"
          />
          <MetricCard
            icon={ListChecks}
            label="待审流程"
            value={`${metrics.pendingApprovals}项`}
            meta="采购 / 合同 / 报销"
            tone="warn"
          />
          <MetricCard
            icon={TrendingUp}
            label="合同毛利池"
            value={formatMoney(metrics.gross)}
            meta="项目成本滚动扣减"
            tone="info"
          />
        </section>

        <section className="overview-layout">
          <div className="panel wide">
            <SectionTitle
              icon={Route}
              title="核心业务闭环"
              right="合同 → 工单 → 库存 → 应收 → 总账"
            />
            <div className="flow-steps">
              {[
                ["CRM", "线索 / 商机 / 合同 / 续约"],
                ["供应链采购", "采购申请 / 供应商 / 外包"],
                ["项目管理", "项目 / 工单 / 设备 / 成本"],
                ["库存管理", "入库 / 领用 / 安全库存"],
                ["财务资金", "应收 / 应付 / 凭证"],
              ].map(([title, detail]) => (
                <div className="flow-step" key={title}>
                  <strong>{title}</strong>
                  <span>{detail}</span>
                </div>
              ))}
            </div>
            <EventTimeline />
          </div>

          <QuickCreatePanel />
        </section>

        <LinkageMatrix />

        <section className="split-grid">
          <div className="panel">
            <SectionTitle icon={Wrench} title="今日工单" right="现场执行" />
            <WorkOrderTable compact />
          </div>
          <RiskPanel />
        </section>

        <ControlRulesPanel />
      </div>
    );
  }

  function CrmModule() {
    const activeTab = crmTabs.find((tab) => tab.id === activeCrmTab)!;
    const pipelineValue = leads.reduce((sum, item) => sum + item.value, 0);
    const activeQuotes = quotes.filter((item) => item.status !== "已通过");
    const renewalRisks = crmCustomers.filter(
      (item) => item.risk === "续约风险" || item.risk === "逾期",
    ).length;

    return (
      <div className="page-stack">
        <section className="crm-subpage">
          <div className="crm-subpage-head">
            <div>
              <p className="eyebrow">CRM / {activeTab.label}</p>
              <h2>{activeTab.label}</h2>
            </div>
          </div>
          {renderCrmTab()}
        </section>
      </div>
    );

    function renderCrmTab() {
      switch (activeCrmTab) {
        case "customers":
          return <CustomerPoolPanel />;
        case "leads":
          return <LeadPipelinePanel />;
        case "quotes":
          return <QuotePlanPanel />;
        case "contracts":
          return <ContractPanel />;
        case "followups":
          return <FollowUpPanel />;
        case "renewals":
          return <RenewalPanel />;
        case "receivables":
          return <ReceivablesPanel />;
        case "insights":
          return <CustomerInsightPanel />;
        default:
          return (
            <div className="page-stack">
              <section className="metric-grid">
                <MetricCard
                  icon={Building2}
                  label="客户池"
                  value={`${crmCustomers.length}家`}
                  meta="甲方 / 集团 / 项目地址"
                  tone="good"
                />
                <MetricCard
                  icon={TrendingUp}
                  label="商机金额"
                  value={formatMoney(pipelineValue)}
                  meta={`${leads.length}个在跟进`}
                  tone="info"
                />
                <MetricCard
                  icon={ReceiptText}
                  label="报价方案"
                  value={`${activeQuotes.length}份`}
                  meta="草稿 / 审批中"
                  tone="warn"
                />
                <MetricCard
                  icon={TimerReset}
                  label="续约/回款风险"
                  value={`${renewalRisks}家`}
                  meta="合同到期与逾期叠加"
                  tone="danger"
                />
              </section>
              <section className="crm-grid">
                <LeadPipelinePanel />
                <FollowUpPanel />
              </section>
            </div>
          );
      }
    }
  }

  function CustomerPoolPanel() {
    const selectedCustomer =
      crmCustomers.find((customer) => customer.id === selectedCustomerId) ||
      crmCustomers[0];
    const customerContracts = contracts.filter(
      (contract) => contract.customer === selectedCustomer.name,
    );
    const customerProjects = projects.filter(
      (project) => project.customer === selectedCustomer.name,
    );
    const customerLeads = leads.filter(
      (lead) => lead.customer === selectedCustomer.name,
    );
    const customerQuotes = quotes.filter(
      (quote) => quote.customer === selectedCustomer.name,
    );
    const customerReceivables = receivables.filter(
      (item) => item.customer === selectedCustomer.name,
    );
    const customerOrders = orders.filter(
      (order) => order.customer === selectedCustomer.name,
    );
    const customerFollowUps = crmFollowUps.filter(
      (item) => item.customer === selectedCustomer.name,
    );
    const customerInsight = customerInsights.find(
      (item) => item.customer === selectedCustomer.name,
    );
    const contractTotal = customerContracts.reduce(
      (sum, contract) => sum + contract.amount,
      0,
    );
    const projectTotal = customerProjects.reduce(
      (sum, project) => sum + project.budget,
      0,
    );
    const outstandingTotal = customerReceivables
      .filter((item) => item.status !== "已核销")
      .reduce((sum, item) => sum + item.amount, 0);
    const paidTotal = customerReceivables
      .filter((item) => item.status === "已核销")
      .reduce((sum, item) => sum + item.amount, 0);

    return (
      <section className="panel customer-pool-panel">
        <SectionTitle
          icon={Building2}
          title="客户池"
          right="一行一客户 / 点击查看全量档案"
        />

        <div className="table-wrap customer-table-wrap">
          <table className="customer-table">
            <thead>
              <tr>
                <th>客户</th>
                <th>等级 / 行业</th>
                <th>联系人</th>
                <th>项目地址</th>
                <th>付款习惯</th>
                <th>风险</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {crmCustomers.map((customer) => {
                const relatedContracts = contracts.filter(
                  (contract) => contract.customer === customer.name,
                );
                const relatedReceivables = receivables.filter(
                  (item) =>
                    item.customer === customer.name && item.status !== "已核销",
                );
                const pendingAmount = relatedReceivables.reduce(
                  (sum, item) => sum + item.amount,
                  0,
                );
                return (
                  <tr
                    className={
                      customer.id === selectedCustomer.id ? "selected-row" : ""
                    }
                    key={customer.id}
                    onClick={() => setSelectedCustomerId(customer.id)}
                  >
                    <td>
                      <strong>{customer.name}</strong>
                      <small>{customer.id} · {customer.owner}</small>
                    </td>
                    <td>
                      {customer.level}
                      <small>{customer.industry}</small>
                    </td>
                    <td>
                      {customer.contacts}
                      <small>最近拜访 {customer.lastVisit}</small>
                    </td>
                    <td>
                      {customer.sites}处
                      <small>{customer.addresses[0]}</small>
                    </td>
                    <td>
                      {customer.payment}
                      <small>{relatedContracts.length}份合同</small>
                    </td>
                    <td>
                      <StatusTag tone={customerRiskTone(customer.risk)}>
                        {customer.risk}
                      </StatusTag>
                      <small>{formatMoney(pendingAmount)} 待收</small>
                    </td>
                    <td>
                      <button
                        className="mini-action"
                        type="button"
                        title="查看客户详情"
                        onClick={(event) => {
                          event.stopPropagation();
                          setSelectedCustomerId(customer.id);
                        }}
                      >
                        <PanelRightOpen size={15} />
                        <span>查看</span>
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div className="customer-detail-panel">
          <div className="customer-detail-head">
            <div>
              <small>{selectedCustomer.id} · {selectedCustomer.industry}</small>
              <h3>{selectedCustomer.name}</h3>
              <p>{selectedCustomer.riskNote}</p>
            </div>
            <StatusTag tone={customerRiskTone(selectedCustomer.risk)}>
              {selectedCustomer.risk}
            </StatusTag>
          </div>

          <div className="customer-detail-metrics">
            <article>
              <span>已签合同</span>
              <strong>{customerContracts.length}份</strong>
              <small>{formatMoney(contractTotal)}</small>
            </article>
            <article>
              <span>跟踪项目</span>
              <strong>{customerProjects.length + customerLeads.length}个</strong>
              <small>预算 {formatMoney(projectTotal)}</small>
            </article>
            <article>
              <span>待收款项</span>
              <strong>{formatMoney(outstandingTotal)}</strong>
              <small>已核销 {formatMoney(paidTotal)}</small>
            </article>
            <article>
              <span>工单记录</span>
              <strong>{customerOrders.length}张</strong>
              <small>{customerInsight ? `${customerInsight.faults}次故障` : "暂无画像"}</small>
            </article>
          </div>

          <div className="customer-detail-grid">
            <article className="customer-detail-block">
              <h4>基础档案</h4>
              <dl className="detail-grid">
                <div>
                  <dt>客户等级</dt>
                  <dd>{selectedCustomer.level}</dd>
                </div>
                <div>
                  <dt>客户负责人</dt>
                  <dd>{selectedCustomer.owner}</dd>
                </div>
                <div>
                  <dt>联系人</dt>
                  <dd>{selectedCustomer.contacts}</dd>
                </div>
                <div>
                  <dt>付款习惯</dt>
                  <dd>{selectedCustomer.payment}</dd>
                </div>
              </dl>
              <div className="address-list">
                {selectedCustomer.addresses.map((address) => (
                  <span key={address}>
                    <MapPin size={14} />
                    {address}
                  </span>
                ))}
              </div>
            </article>

            <article className="customer-detail-block">
              <h4>开票资料</h4>
              <dl className="detail-grid">
                <div>
                  <dt>发票抬头</dt>
                  <dd>{selectedCustomer.invoice.title}</dd>
                </div>
                <div>
                  <dt>纳税人识别号</dt>
                  <dd>{selectedCustomer.invoice.taxNo}</dd>
                </div>
                <div>
                  <dt>开户银行</dt>
                  <dd>{selectedCustomer.invoice.bank}</dd>
                </div>
                <div>
                  <dt>银行账号</dt>
                  <dd>{selectedCustomer.invoice.account}</dd>
                </div>
                <div>
                  <dt>注册地址</dt>
                  <dd>{selectedCustomer.invoice.address}</dd>
                </div>
                <div>
                  <dt>注册电话</dt>
                  <dd>{selectedCustomer.invoice.phone}</dd>
                </div>
              </dl>
            </article>
          </div>

          <div className="customer-related-grid">
            <article className="customer-detail-block">
              <h4>已签约合同</h4>
              <div className="customer-related-list">
                {customerContracts.length ? (
                  customerContracts.map((contract) => (
                    <div className="customer-related-row" key={contract.id}>
                      <div>
                        <strong>{contract.project}</strong>
                        <span>{contract.id} · {contract.type} · {contract.period}</span>
                      </div>
                      <b>{formatMoney(contract.amount)}</b>
                      <StatusTag tone={contractTone(contract.status)}>
                        {contract.status}
                      </StatusTag>
                    </div>
                  ))
                ) : (
                  <p className="empty-state">暂无已签约合同</p>
                )}
              </div>
            </article>

            <article className="customer-detail-block">
              <h4>跟踪的项目</h4>
              <div className="customer-related-list">
                {customerProjects.map((project) => (
                  <div className="customer-related-row" key={project.id}>
                    <div>
                      <strong>{project.name}</strong>
                      <span>{project.id} · {project.stage} · 进度 {project.progress}%</span>
                    </div>
                    <b>{formatMoney(project.budget)}</b>
                    <StatusTag tone="info">项目</StatusTag>
                  </div>
                ))}
                {customerLeads.map((lead) => (
                  <div className="customer-related-row" key={lead.id}>
                    <div>
                      <strong>{lead.need}</strong>
                      <span>{lead.id} · {lead.stage} · 下次动作 {lead.nextAction}</span>
                    </div>
                    <b>{formatMoney(lead.value)}</b>
                    <StatusTag tone={lead.probability >= 70 ? "good" : "info"}>
                      {lead.probability}%
                    </StatusTag>
                  </div>
                ))}
                {customerQuotes.map((quote) => (
                  <div className="customer-related-row" key={quote.id}>
                    <div>
                      <strong>{quote.type}</strong>
                      <span>{quote.id} · {quote.scope}</span>
                    </div>
                    <b>{formatMoney(quote.amount)}</b>
                    <StatusTag tone={quoteTone(quote.status)}>
                      {quote.status}
                    </StatusTag>
                  </div>
                ))}
                {!customerProjects.length &&
                  !customerLeads.length &&
                  !customerQuotes.length && (
                    <p className="empty-state">暂无跟踪项目或商机</p>
                  )}
              </div>
            </article>

            <article className="customer-detail-block">
              <h4>合同款项支付情况</h4>
              <div className="customer-related-list">
                {customerReceivables.length ? (
                  customerReceivables.map((item) => (
                    <div className="customer-related-row" key={item.id}>
                      <div>
                        <strong>{item.id}</strong>
                        <span>{item.source} · 到期 {item.due}</span>
                      </div>
                      <b>{formatMoney(item.amount)}</b>
                      <StatusTag tone={receivableTone(item.status)}>
                        {item.status}
                      </StatusTag>
                    </div>
                  ))
                ) : (
                  <p className="empty-state">暂无合同应收记录</p>
                )}
              </div>
            </article>

            <article className="customer-detail-block">
              <h4>最近跟进与工单</h4>
              <div className="customer-related-list">
                {customerFollowUps.map((item) => (
                  <div className="customer-related-row" key={item.id}>
                    <div>
                      <strong>{item.type} · {item.date}</strong>
                      <span>{item.summary}</span>
                      <span>下次动作：{item.next}</span>
                    </div>
                    <StatusTag tone={item.tone}>{item.type}</StatusTag>
                  </div>
                ))}
                {customerOrders.slice(0, 3).map((order) => (
                  <div className="customer-related-row" key={order.id}>
                    <div>
                      <strong>{order.id}</strong>
                      <span>{order.type} · {order.equipment}</span>
                    </div>
                    <StatusTag tone={orderTone(order.status)}>
                      {order.status}
                    </StatusTag>
                  </div>
                ))}
                {!customerFollowUps.length && !customerOrders.length && (
                  <p className="empty-state">暂无跟进或工单记录</p>
                )}
              </div>
            </article>
          </div>
        </div>
      </section>
    );
  }

  function LeadPipelinePanel() {
    return (
      <section className="panel">
        <SectionTitle
          icon={TrendingUp}
          title="线索商机"
          right="初访 / 勘查 / 报价 / 谈判"
        />
        <div className="lead-board">
          {leads.map((lead) => (
            <article className="lead-card" key={lead.id}>
              <div className="lead-top">
                <div>
                  <small>{lead.id} · {lead.source}</small>
                  <strong>{lead.customer}</strong>
                </div>
                <StatusTag tone={lead.probability >= 70 ? "good" : "info"}>
                  {lead.probability}%
                </StatusTag>
              </div>
              <p>{lead.need}</p>
              <div className="lead-progress">
                <span style={{ width: `${lead.probability}%` }} />
              </div>
              <dl>
                <div>
                  <dt>阶段</dt>
                  <dd>{lead.stage}</dd>
                </div>
                <div>
                  <dt>预计金额</dt>
                  <dd>{formatMoney(lead.value)}</dd>
                </div>
                <div>
                  <dt>负责人</dt>
                  <dd>{lead.owner}</dd>
                </div>
                <div>
                  <dt>下次动作</dt>
                  <dd>{lead.nextDate}</dd>
                </div>
              </dl>
              <div className="lead-action-row">
                <span>{lead.nextAction}</span>
                <button
                  className="mini-action"
                  type="button"
                  onClick={() => advanceLead(lead)}
                  disabled={lead.stage === "赢单"}
                >
                  <RefreshCw size={15} />
                  <span>{lead.stage === "方案报价" ? "报价" : "推进"}</span>
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>
    );
  }

  function QuotePlanPanel() {
    return (
      <section className="panel">
        <SectionTitle
          icon={ReceiptText}
          title="报价方案"
          right="服务范围 / 巡检频次 / 付款节点 / 审批"
        />
        <div className="quote-list">
          {quotes.map((quote) => (
            <article className="quote-row" key={quote.id}>
              <div>
                <small>{quote.id} · {quote.type}</small>
                <strong>{quote.customer}</strong>
                <span>{quote.scope}</span>
              </div>
              <div>
                <small>巡检频次</small>
                <b>{quote.cycle}</b>
              </div>
              <div>
                <small>付款节点</small>
                <b>{quote.paymentNodes}</b>
              </div>
              <div>
                <small>报价金额</small>
                <b>{formatMoney(quote.amount)}</b>
              </div>
              <StatusTag tone={quoteTone(quote.status)}>{quote.status}</StatusTag>
              <button
                className="mini-action"
                type="button"
                onClick={() => submitQuoteApproval(quote)}
                disabled={quote.status !== "成本已核对，可以报价"}
              >
                <Send size={15} />
                <span>报价</span>
              </button>
            </article>
          ))}
        </div>
      </section>
    );
  }

  function ContractPanel() {
    return (
      <section className="panel">
        <SectionTitle
          icon={BriefcaseBusiness}
          title="客户合同"
          right="赢单后生成巡检计划 / 项目 / 应收"
        />
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>合同</th>
                <th>客户</th>
                <th>类型</th>
                <th>周期</th>
                <th>下次巡检</th>
                <th>下次应收</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {contracts.map((contract) => (
                <tr key={contract.id}>
                  <td>
                    <strong>{contract.id}</strong>
                    <small>{contract.project}</small>
                  </td>
                  <td>{contract.customer}</td>
                  <td>{contract.type}</td>
                  <td>{contract.serviceCycle}</td>
                  <td>{contract.nextInspect}</td>
                  <td>{formatMoney(contract.receivable)}</td>
                  <td>
                    <StatusTag tone={contractTone(contract.status)}>
                      {contract.status}
                    </StatusTag>
                  </td>
                  <td>
                    <button
                      className="mini-action"
                      type="button"
                      title="生成巡检工单"
                      onClick={() => createInspectionOrder(contract)}
                    >
                      <Plus size={15} />
                      <span>巡检</span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    );
  }

  function FollowUpPanel() {
    return (
      <section className="panel">
        <SectionTitle
          icon={CalendarCheck}
          title="跟进回访"
          right="拜访 / 电话 / 投诉 / 满意度"
        />
        <div className="followup-list">
          {crmFollowUps.map((item) => (
            <article className="followup-row" key={item.id}>
              <StatusTag tone={item.tone}>{item.type}</StatusTag>
              <div>
                <small>{item.id} · {item.date} · {item.owner}</small>
                <strong>{item.customer}</strong>
                <p>{item.summary}</p>
              </div>
              <span>{item.next}</span>
            </article>
          ))}
        </div>
      </section>
    );
  }

  function RenewalPanel() {
    return (
      <section className="panel">
        <SectionTitle
          icon={ClipboardSignature}
          title="续约管理"
          right="合同到期 / 质保到期 / 客户利润"
        />
        <div className="renewal-list">
          {contracts.map((contract) => (
            <div className="renewal-row" key={contract.id}>
              <span>{contract.customer}</span>
              <strong>{contract.period}</strong>
              <StatusTag tone={contractTone(contract.status)}>
                {contract.status}
              </StatusTag>
            </div>
          ))}
        </div>
      </section>
    );
  }

  function ReceivablesPanel() {
    return (
      <section className="panel">
        <SectionTitle
          icon={WalletCards}
          title="合同应收"
          right="分期收款 / 开票 / 回款"
        />
        <ReceivableList />
      </section>
    );
  }

  function CustomerInsightPanel() {
    return (
      <section className="panel">
        <SectionTitle
          icon={TrendingUp}
          title="客户经营画像"
          right="收入 / 成本 / 故障 / 续约"
        />
        <div className="customer-grid">
          {customerInsights.map((item) => {
            const margin = item.revenue - item.cost;
            const marginRate = Math.round((margin / item.revenue) * 100);
            return (
              <article className="customer-card" key={item.customer}>
                <div className="customer-head">
                  <strong>{item.customer}</strong>
                  <StatusTag tone={item.renewal >= 80 ? "good" : "warn"}>
                    续约 {item.renewal}%
                  </StatusTag>
                </div>
                <dl>
                  <div>
                    <dt>合同收入</dt>
                    <dd>{formatMoney(item.revenue)}</dd>
                  </div>
                  <div>
                    <dt>服务成本</dt>
                    <dd>{formatMoney(item.cost)}</dd>
                  </div>
                  <div>
                    <dt>毛利率</dt>
                    <dd>{marginRate}%</dd>
                  </div>
                  <div>
                    <dt>工单/故障</dt>
                    <dd>{item.orders} / {item.faults}</dd>
                  </div>
                </dl>
              </article>
            );
          })}
        </div>
      </section>
    );
  }

  function SupplyChainModule() {
    const lowStockParts = parts.filter((part) => part.stock <= part.safety);
    const purchaseApprovals = approvals.filter(
      (item) => item.effect === "purchase" || item.type === "采购申请",
    );

    return (
      <div className="page-stack">
        <section className="metric-grid">
          <MetricCard
            icon={PackageMinus}
            label="待采购物料"
            value={`${lowStockParts.length}类`}
            meta="低于安全库存"
            tone="warn"
          />
          <MetricCard
            icon={ClipboardCheck}
            label="采购审批"
            value={`${purchaseApprovals.filter((item) => item.status !== "已通过").length}项`}
            meta="通过后联动入库"
            tone="info"
          />
          <MetricCard
            icon={Truck}
            label="供应商/外包"
            value={`${suppliers.length}家`}
            meta="物料供货 / 外协检测"
            tone="good"
          />
          <MetricCard
            icon={Coins}
            label="采购应付"
            value={formatMoney(
              purchaseApprovals.reduce((sum, item) => sum + item.amount, 0),
            )}
            meta="审批后形成应付"
            tone="warn"
          />
        </section>

        <section className="split-grid">
          <div className="panel">
            <SectionTitle
              icon={ClipboardCheck}
              title="采购审批"
              right="审批通过 → 入库 → 应付"
            />
            <div className="approval-list">
              {purchaseApprovals.map((item) => (
                <article className="approval-row" key={item.id}>
                  <div>
                    <small>{item.id} · {item.type}</small>
                    <strong>{item.title}</strong>
                  </div>
                  <span>{item.owner}</span>
                  <b>{formatMoney(item.amount)}</b>
                  <StatusTag tone={approvalTone(item.status)}>
                    {item.status}
                  </StatusTag>
                  <button
                    className="mini-action"
                    type="button"
                    onClick={() => approveApproval(item)}
                    disabled={item.status === "已通过"}
                  >
                    <CheckCircle2 size={15} />
                    <span>通过</span>
                  </button>
                </article>
              ))}
            </div>
          </div>

          <div className="panel">
            <SectionTitle
              icon={PackageMinus}
              title="采购需求池"
              right="安全库存触发"
            />
            <div className="risk-list">
              {lowStockParts.map((part) => (
                <div className="risk-row warn" key={part.id}>
                  <strong>{part.name}</strong>
                  <span>
                    库存 {part.stock} / 安全库存 {part.safety} · {part.supplier}
                  </span>
                </div>
              ))}
              {!lowStockParts.length && (
                <div className="risk-row info">
                  <strong>库存正常</strong>
                  <span>暂无低于安全库存的物料</span>
                </div>
              )}
            </div>
          </div>
        </section>

        <section className="panel">
          <SectionTitle
            icon={Truck}
            title="供应商与外包服务商"
            right="物料供货 / 外协施工 / 检测结算"
          />
          <div className="supplier-grid">
            {suppliers.map((supplier) => (
              <article className="supplier-row" key={supplier.name}>
                <div>
                  <strong>{supplier.name}</strong>
                  <span>{supplier.type} · {supplier.scope}</span>
                </div>
                <div>
                  <small>账期</small>
                  <b>{supplier.accountPeriod}</b>
                </div>
                <div>
                  <small>应付</small>
                  <b>{formatMoney(supplier.payable)}</b>
                </div>
                <StatusTag tone={supplier.rating.startsWith("A") ? "good" : "warn"}>
                  {supplier.rating}
                </StatusTag>
              </article>
            ))}
          </div>
        </section>
      </div>
    );
  }

  function ProjectsModule() {
    return (
      <div className="page-stack">
        <section className="project-grid">
          {projects.map((project) => {
            const gross = project.budget - project.cost;
            return (
              <article className="project-card" key={project.id}>
                <div className="project-head">
                  <div>
                    <small>{project.id}</small>
                    <h2>{project.name}</h2>
                    <p>{project.customer}</p>
                  </div>
                  <StatusTag tone="info">{project.stage}</StatusTag>
                </div>
                <div className="progress-track">
                  <span style={{ width: `${project.progress}%` }} />
                </div>
                <dl className="project-stats">
                  <div>
                    <dt>预算</dt>
                    <dd>{formatMoney(project.budget)}</dd>
                  </div>
                  <div>
                    <dt>已归集</dt>
                    <dd>{formatMoney(project.cost)}</dd>
                  </div>
                  <div>
                    <dt>毛利</dt>
                    <dd>{formatMoney(gross)}</dd>
                  </div>
                  <div>
                    <dt>质保期</dt>
                    <dd>{project.warranty}</dd>
                  </div>
                </dl>
              </article>
            );
          })}
        </section>

        <section className="workbench">
          <QuickCreatePanel />
          <div className="panel wide">
            <SectionTitle
              icon={Wrench}
              title="项目工单调度"
              right="报修 / 派工 / 领料 / 验收"
            />
            <WorkOrderTable />
          </div>
        </section>

        <section className="split-grid">
          <OrderDetailPanel />
          <EquipmentModule />
        </section>

        <section className="panel">
          <SectionTitle icon={FileCheck2} title="项目成本归集" right="工时 / 领料 / 外包 / 差旅" />
          <LedgerTable />
        </section>
      </div>
    );
  }

  function WorkOrdersModule() {
    return (
      <div className="page-stack">
        <section className="workbench">
          <QuickCreatePanel />
          <div className="panel wide">
            <SectionTitle icon={Wrench} title="工单调度台" right="派工 / 领料 / 验收" />
            <WorkOrderTable />
          </div>
        </section>
        <section className="split-grid">
          <OrderDetailPanel />
          <div className="panel">
            <SectionTitle icon={MapPin} title="现场轨迹" right="签到定位 / SLA" />
            <div className="route-list compact-route">
              {people.map((person) => (
                <div className="route-row" key={person.name}>
                  <span>{person.name}</span>
                  <strong>{person.location}</strong>
                  <StatusTag tone={person.status === "可派工" ? "good" : "info"}>
                    {person.status}
                  </StatusTag>
                </div>
              ))}
            </div>
          </div>
        </section>
      </div>
    );
  }

  function EquipmentModule() {
    return (
      <section className="panel">
        <SectionTitle icon={Database} title="客户设备台账" right="故障历史 / 服务周期 / 质保提醒" />
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>设备</th>
                <th>客户</th>
                <th>安装地址</th>
                <th>服务周期</th>
                <th>下次服务</th>
                <th>故障</th>
                <th>资质</th>
              </tr>
            </thead>
            <tbody>
              {equipment.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.name}</strong>
                    <small>{item.id} · 质保至 {item.warranty}</small>
                  </td>
                  <td>{item.customer}</td>
                  <td>{item.address}</td>
                  <td>{item.cycle}</td>
                  <td>{item.nextService}</td>
                  <td>{item.faults}次</td>
                  <td>{item.needCert}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    );
  }

  function InventoryModule() {
    return (
      <div className="page-stack">
        <section className="panel">
          <SectionTitle icon={PackageCheck} title="物料库存" right="入库 / 领用 / 归还 / 报废 / 盘点" />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>物料</th>
                  <th>库存</th>
                  <th>安全库存</th>
                  <th>库位</th>
                  <th>供应商</th>
                  <th>成本</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {parts.map((part) => (
                  <tr key={part.id}>
                    <td>
                      <strong>{part.name}</strong>
                      <small>{part.model}</small>
                    </td>
                    <td>{part.stock}</td>
                    <td>{part.safety}</td>
                    <td>{part.location}</td>
                    <td>{part.supplier}</td>
                    <td>{formatMoney(part.unitCost)}</td>
                    <td>
                      <button
                        className="mini-action"
                        type="button"
                        title="生成采购申请"
                        onClick={() => createPurchaseApproval(part)}
                      >
                        <Send size={15} />
                        <span>补货</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
        <RiskPanel />
      </div>
    );
  }

  function OutsourcingModule() {
    return (
      <section className="panel">
        <SectionTitle icon={Truck} title="外包服务商" right="外协施工 / 检测 / 结算" />
        <div className="supplier-grid">
          {suppliers.map((supplier) => (
            <article className="supplier-row" key={supplier.name}>
              <div>
                <strong>{supplier.name}</strong>
                <span>{supplier.type} · {supplier.scope}</span>
              </div>
              <div>
                <small>账期</small>
                <b>{supplier.accountPeriod}</b>
              </div>
              <div>
                <small>应付</small>
                <b>{formatMoney(supplier.payable)}</b>
              </div>
              <StatusTag tone={supplier.rating.startsWith("A") ? "good" : "warn"}>
                {supplier.rating}
              </StatusTag>
            </article>
          ))}
        </div>
      </section>
    );
  }

  function FinanceModule() {    const [finTab, setFinTab] = useState<"vouchers"|"statements"|"payments">("vouchers");    const pendingPay = paymentApps.filter(a => a.status === "PENDING").length;    const totalBudget = projects.reduce((s, p) => s + p.budget, 0);    const totalCost = projects.reduce((s, p) => s + p.cost, 0);    const budgetMargin = totalBudget - totalCost;    const budgetPct = totalBudget > 0 ? Math.round(budgetMargin / totalBudget * 100) : 0;    return (      <div className="page-stack">        <section className="metric-grid">          <MetricCard icon={WalletCards} label="应收余额" value={formatMoney(metrics.receivableTotal)} meta="合同 + 单次维修" tone="warn" />          <MetricCard icon={Coins} label="应付余额" value={formatMoney(metrics.payableTotal)} meta="采购 + 外包 + 报销" tone="info" />          <MetricCard icon={CheckCircle2} label="本月核销" value={formatMoney(486000)} meta="回款率 82%" tone="good" />          <MetricCard icon={CircleAlert} label="应收逾期" value={`${metrics.overdue}笔`} meta="催收台账" tone="danger" />          <MetricCard icon={CircleAlert} label="应付逾期" value={`${metrics.overduePayable}笔`} meta="逾期付款" tone="danger" />          <MetricCard icon={TrendingUp} label="可用资金" value={formatMoney(metrics.receivableTotal - metrics.payableTotal + 486000)} meta="应收 - 应付 + 回款" tone="info" />          {ledgerOverview && (            <>              <MetricCard icon={ReceiptText} label="凭证" value={`${ledgerOverview.voucherCount}张`} meta="本月" tone="info" />              <MetricCard icon={TrendingUp} label="净利润" value={formatMoney(ledgerOverview.profit)} meta="收入-费用" tone={ledgerOverview.profit >= 0 ? "good" : "danger"} />              <MetricCard icon={Coins} label="现金余额" value={formatMoney(ledgerOverview.cashBalance)} meta="总账" tone="info" />            </>          )}          {totalBudget > 0 && (            <MetricCard icon={BarChart3} label="预算节余" value={formatMoney(budgetMargin)} meta={`${budgetPct}% 结余率`} tone={budgetMargin >= 0 ? "good" : "danger"} />          )}          {pendingPay > 0 && (            <MetricCard icon={ClipboardSignature} label="待批付款" value={`${pendingPay}笔`} meta="付款申请" tone="warn" />          )}        </section>        <section className="split-grid">          <div className="panel">            <div className="section-title">              <div><WalletCards size={19} /><h2>应收款</h2></div>              <div style={{display:"flex",gap:4,alignItems:"center"}}>                <input type="date" value={filterDateStart} onChange={e=>setFilterDateStart(e.target.value)} style={{padding:"3px 6px",border:"1px solid #d7dfd8",borderRadius:4,fontSize:11,width:110}} />                <span style={{fontSize:11,color:"#65716e"}}>~</span>                <input type="date" value={filterDateEnd} onChange={e=>setFilterDateEnd(e.target.value)} style={{padding:"3px 6px",border:"1px solid #d7dfd8",borderRadius:4,fontSize:11,width:110}} />                {(filterDateStart||filterDateEnd)&&<button className="mini-action" type="button" onClick={()=>{setFilterDateStart("");setFilterDateEnd("")}} style={{padding:"2px 6px",fontSize:11}}>×</button>}              </div>            </div>            <div style={{display:"flex",justifyContent:"flex-end",marginBottom:6}}>              <button className="mini-action" type="button" onClick={()=>{                const h="编号,客户,来源,金额,到期日,状态\n"; const r=receivables.map(x=>`${x.id},${x.customer},${x.source},${x.amount},${x.due},${x.status}`).join("\n");                const b=new Blob(["\ufeff"+h+r],{type:"text/csv;charset=utf-8"}); const a=document.createElement("a"); a.href=URL.createObjectURL(b); a.download="应收款.csv"; a.click(); URL.revokeObjectURL(a.href);                pushEvent("导出完成","应收款 CSV","good");              }}><FileArchive size={13} /><span>导出CSV</span></button>            </div>            <ReceivableList />          </div>          <div className="panel">            <div className="section-title">              <div><Coins size={19} /><h2>应付款</h2></div>              <div style={{display:"flex",gap:4,alignItems:"center"}}>                <input type="date" value={filterDateStart} onChange={e=>setFilterDateStart(e.target.value)} style={{padding:"3px 6px",border:"1px solid #d7dfd8",borderRadius:4,fontSize:11,width:110}} />                <span style={{fontSize:11,color:"#65716e"}}>~</span>                <input type="date" value={filterDateEnd} onChange={e=>setFilterDateEnd(e.target.value)} style={{padding:"3px 6px",border:"1px solid #d7dfd8",borderRadius:4,fontSize:11,width:110}} />                {(filterDateStart||filterDateEnd)&&<button className="mini-action" type="button" onClick={()=>{setFilterDateStart("");setFilterDateEnd("")}} style={{padding:"2px 6px",fontSize:11}}>×</button>}              </div>            </div>            <PayableList />          </div>        </section>        <section className="split-grid">          <div className="panel">            <SectionTitle icon={TrendingUp} title="应收账龄分布" right="到期分析" />            <AgingPanel />          </div>          <div className="panel">            <div className="section-title">              <div style={{display:"flex",gap:8,alignItems:"center"}}><ClipboardList size={19} /><h2>总账</h2></div>              <div style={{display:"flex",gap:4,flexWrap:"wrap"}}>                {(["vouchers","statements","payments"] as const).map((t) => (                  <button key={t} className={finTab===t?"mini-action active-tab":"mini-action"} type="button"                    onClick={()=>setFinTab(t)} style={finTab===t?{borderColor:"#1f6a5b",color:"#1f6a5b",background:"#edf4f0"}:{}}>                    {t==="vouchers"?"会计凭证":t==="statements"?"财务报表":"付款管理"}                  </button>                ))}              </div>            </div>            {finTab==="vouchers" && <VoucherPanel />}            {finTab==="statements" && <FinancialStatementsPanel />}            {finTab==="payments" && <PaymentApplicationsPanel />}          </div>        </section>      </div>    );  }
  function HrModule() {
    return (
      <section className="panel">
        <SectionTitle icon={UserRoundCheck} title="组织人事" right="资质 / 排班 / 补贴 / 绩效" />
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>人员</th>
                <th>资质</th>
                <th>证书到期</th>
                <th>位置</th>
                <th>排班</th>
                <th>状态</th>
                <th>人均产值</th>
              </tr>
            </thead>
            <tbody>
              {people.map((person) => (
                <tr key={person.name}>
                  <td>
                    <strong>{person.name}</strong>
                    <small>{person.role}</small>
                  </td>
                  <td>{person.certs}</td>
                  <td>{person.certExpire}</td>
                  <td>{person.location}</td>
                  <td>{person.schedule}</td>
                  <td>
                    <StatusTag tone={person.status === "可派工" ? "good" : "info"}>
                      {person.status}
                    </StatusTag>
                  </td>
                  <td>{person.output}万</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    );
  }

  function OaModule() {
    return (
      <section className="panel">
        <SectionTitle icon={ClipboardCheck} title="OA流程审批" right="报价 / 合同 / 采购 / 外包 / 报销 / 用章" />
        <div className="approval-list">
          {approvals.map((item) => (
            <article className="approval-row" key={item.id}>
              <div>
                <small>{item.id} · {item.type}</small>
                <strong>{item.title}</strong>
              </div>
              <span>{item.owner}</span>
              <b>{formatMoney(item.amount)}</b>
              <StatusTag tone={approvalTone(item.status)}>{item.status}</StatusTag>
              <button
                className="mini-action"
                type="button"
                onClick={() => approveApproval(item)}
                disabled={item.status === "已通过"}
              >
                <CheckCircle2 size={15} />
                <span>通过</span>
              </button>
            </article>
          ))}
        </div>
      </section>
    );
  }

  function DocumentsModule() {
    return (
      <section className="doc-grid">
        {docs.map(([title, scope, count, state]) => (
          <article className="doc-card" key={title}>
            <Archive size={22} />
            <h2>{title}</h2>
            <p>{scope}</p>
            <strong>{count}</strong>
            <span>{state}</span>
          </article>
        ))}
      </section>
    );
  }

  function AccountsModule() {
    const selectedUser = systemUsers.find((user) => user.id === selectedUserId) ?? null;
    const enabledCount = systemUsers.filter((user) => user.enabled).length;
    const selectedOrganization = selectedUser?.orgId
      ? systemOrganizations.find((org) => org.id === selectedUser.orgId)
      : null;

    const toggleRole = (roleId: string) => {
      setAccountForm((current) => ({
        ...current,
        roleIds: current.roleIds.includes(roleId)
          ? current.roleIds.filter((id) => id !== roleId)
          : [...current.roleIds, roleId],
      }));
    };

    const submitAccount = async () => {
      if (!accountForm.username.trim() || !accountForm.displayName.trim()) {
        pushEvent("账号保存失败", "用户名和姓名不能为空", "danger");
        return;
      }
      if (accountMode === "create" && accountForm.password.length < 6) {
        pushEvent("账号保存失败", "初始密码至少 6 位", "danger");
        return;
      }
      setAccountSaving(true);
      try {
        if (accountMode === "create") {
          const payload: CreateUserPayload = {
            orgId: accountForm.orgId || null,
            username: accountForm.username.trim(),
            displayName: accountForm.displayName.trim(),
            password: accountForm.password,
            phone: accountForm.phone.trim() || undefined,
            email: accountForm.email.trim() || undefined,
            roleIds: accountForm.roleIds,
          };
          const created = await createUser(payload);
          await refreshAccounts();
          setSelectedUserId(created.id);
          setAccountMode("edit");
          setAccountForm({ ...accountForm, password: "" });
          pushEvent("账号已创建", `${created.displayName} 已加入系统`, "good");
          logAudit("账号创建", created.username, "SystemUser");
        } else if (selectedUser) {
          const payload: UpdateUserPayload = {
            orgId: accountForm.orgId || null,
            displayName: accountForm.displayName.trim(),
            phone: accountForm.phone.trim() || undefined,
            email: accountForm.email.trim() || undefined,
            enabled: accountForm.enabled,
            roleIds: accountForm.roleIds,
          };
          const updated = await updateUser(selectedUser.id, payload);
          await refreshAccounts();
          setSelectedUserId(updated.id);
          pushEvent("账号已更新", `${updated.displayName} 的资料已保存`, "good");
          logAudit("账号更新", updated.username, "SystemUser");
        }
      } catch (error) {
        pushEvent("账号保存失败", error instanceof Error ? error.message : "请稍后重试", "danger");
      } finally {
        setAccountSaving(false);
      }
    };

    const toggleUserEnabled = async (user: UserResponse) => {
      try {
        await updateUser(user.id, {
          orgId: user.orgId,
          displayName: user.displayName,
          phone: user.phone ?? undefined,
          email: user.email ?? undefined,
          enabled: !user.enabled,
          roleIds: user.roles.map((role) => role.id),
        });
        await refreshAccounts();
        pushEvent(user.enabled ? "账号已停用" : "账号已启用", user.username, user.enabled ? "warn" : "good");
      } catch (error) {
        pushEvent("账号状态变更失败", error instanceof Error ? error.message : "请稍后重试", "danger");
      }
    };

    const resetPassword = async (user: UserResponse) => {
      try {
        await resetUserPassword(user.id, "Admin@123");
        pushEvent("密码已重置", `${user.username} 的密码已重置为 Admin@123`, "good");
        logAudit("密码重置", user.username, "SystemUser");
      } catch (error) {
        pushEvent("密码重置失败", error instanceof Error ? error.message : "请稍后重试", "danger");
      }
    };

    const removeUser = async (user: UserResponse) => {
      if (!window.confirm(`确认删除账号 ${user.username}？`)) return;
      try {
        await deleteUser(user.id);
        await refreshAccounts();
        startCreateAccount();
        pushEvent("账号已删除", user.username, "warn");
        logAudit("账号删除", user.username, "SystemUser");
      } catch (error) {
        pushEvent("账号删除失败", error instanceof Error ? error.message : "请稍后重试", "danger");
      }
    };

    return (
      <div className="page-stack">
        <section className="metric-grid">
          <MetricCard icon={UsersRound} label="系统账号" value={`${systemUsers.length}个`} meta={`${enabledCount}个启用`} tone="info" />
          <MetricCard icon={ShieldCheck} label="角色模板" value={`${systemRoles.length}类`} meta="按角色授予权限" tone="good" />
          <MetricCard icon={Building2} label="组织范围" value={`${systemOrganizations.length}个`} meta="账号绑定部门" tone="neutral" />
          <MetricCard icon={LockKeyhole} label="默认初始密码" value="Admin@123" meta="创建后可重置" tone="warn" />
        </section>

        <section className="split-grid account-layout">
          <div className="panel">
            <SectionTitle icon={UserCog} title="账号列表" right="用户 / 角色 / 状态" />
            <div className="account-toolbar">
              <button className="primary-action" type="button" onClick={startCreateAccount}>
                <Plus size={15} />
                <span>新建账号</span>
              </button>
              <button className="mini-action" type="button" onClick={() => refreshAccounts().then(() => pushEvent("账号已刷新", "已同步最新账号数据", "good")).catch((error) => pushEvent("账号刷新失败", error instanceof Error ? error.message : "请稍后重试", "danger"))}>
                <RefreshCw size={13} />
                <span>刷新</span>
              </button>
            </div>
            <div className="table-wrap">
              <table className="account-table">
                <thead>
                  <tr>
                    <th>账号</th>
                    <th>组织</th>
                    <th>角色</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {systemUsers.map((user) => {
                    const org = user.orgId ? systemOrganizations.find((item) => item.id === user.orgId) : null;
                    return (
                      <tr key={user.id} className={selectedUserId === user.id ? "selected-row" : ""} onClick={() => loadAccountForm(user)}>
                        <td>
                          <strong>{user.displayName}</strong>
                          <small>{user.username} · {user.phone || "未填手机号"}</small>
                        </td>
                        <td>{org?.name ?? "未绑定"}</td>
                        <td>{user.roles.map((role) => role.name).join("、") || "未分配"}</td>
                        <td><StatusTag tone={user.enabled ? "good" : "neutral"}>{user.enabled ? "启用" : "停用"}</StatusTag></td>
                        <td>
                          <div className="row-actions" onClick={(event) => event.stopPropagation()}>
                            <button className="mini-action" type="button" onClick={() => toggleUserEnabled(user)}>
                              <CheckCircle2 size={13} />
                              <span>{user.enabled ? "停用" : "启用"}</span>
                            </button>
                            <button className="mini-action" type="button" onClick={() => resetPassword(user)}>
                              <KeyRound size={13} />
                              <span>重置</span>
                            </button>
                            <button className="mini-action" type="button" onClick={() => removeUser(user)}>
                              <X size={13} />
                              <span>删除</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  {!systemUsers.length && (
                    <tr>
                      <td colSpan={5}>暂无账号数据，请确认后端服务和系统管理权限。</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="panel account-editor">
            <SectionTitle
              icon={accountMode === "create" ? Plus : UserRoundCheck}
              title={accountMode === "create" ? "新建账号" : "编辑账号"}
              right={selectedUser ? selectedOrganization?.name ?? "未绑定组织" : "账号资料"}
            />
            <div className="account-form">
              <label>
                <span>登录账号</span>
                <input
                  value={accountForm.username}
                  onChange={(event) => setAccountForm((current) => ({ ...current, username: event.target.value }))}
                  disabled={accountMode === "edit"}
                  placeholder="例如 zhangsan"
                />
              </label>
              <label>
                <span>姓名</span>
                <input
                  value={accountForm.displayName}
                  onChange={(event) => setAccountForm((current) => ({ ...current, displayName: event.target.value }))}
                  placeholder="员工姓名"
                />
              </label>
              {accountMode === "create" && (
                <label>
                  <span>初始密码</span>
                  <input
                    type="password"
                    value={accountForm.password}
                    onChange={(event) => setAccountForm((current) => ({ ...current, password: event.target.value }))}
                    placeholder="至少 6 位"
                  />
                </label>
              )}
              <label>
                <span>所属组织</span>
                <select
                  value={accountForm.orgId}
                  onChange={(event) => setAccountForm((current) => ({ ...current, orgId: event.target.value }))}
                >
                  <option value="">未绑定组织</option>
                  {systemOrganizations.map((org) => (
                    <option key={org.id} value={org.id}>{org.fullPath || org.name}</option>
                  ))}
                </select>
              </label>
              <label>
                <span>手机号</span>
                <input
                  value={accountForm.phone}
                  onChange={(event) => setAccountForm((current) => ({ ...current, phone: event.target.value }))}
                  placeholder="手机号"
                />
              </label>
              <label>
                <span>邮箱</span>
                <input
                  value={accountForm.email}
                  onChange={(event) => setAccountForm((current) => ({ ...current, email: event.target.value }))}
                  placeholder="name@company.com"
                />
              </label>
              <label className="account-switch">
                <input
                  type="checkbox"
                  checked={accountForm.enabled}
                  onChange={(event) => setAccountForm((current) => ({ ...current, enabled: event.target.checked }))}
                />
                <span>启用账号</span>
              </label>
            </div>

            <div className="role-picker">
              {systemRoles.map((role) => (
                <button
                  key={role.id}
                  type="button"
                  className={accountForm.roleIds.includes(role.id) ? "role-chip active" : "role-chip"}
                  onClick={() => toggleRole(role.id)}
                >
                  <ShieldCheck size={13} />
                  <span>{role.name}</span>
                </button>
              ))}
              {!systemRoles.length && <small>暂无角色数据，请先确认角色权限配置。</small>}
            </div>

            <div className="detail-actions">
              <button className="primary-action" type="button" onClick={submitAccount} disabled={accountSaving}>
                <Save size={15} />
                <span>{accountSaving ? "保存中" : accountMode === "create" ? "创建账号" : "保存账号"}</span>
              </button>
              <button className="mini-action" type="button" onClick={startCreateAccount}>
                <X size={13} />
                <span>清空</span>
              </button>
            </div>
          </div>
        </section>
      </div>
    );
  }

  function PermissionsModule() {
    return (
      <section className="panel">
        <SectionTitle icon={ShieldCheck} title="角色权限与操作审计" right="数据隔离 / 账号审批 / 日志" />
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>角色</th>
                <th>数据范围</th>
                <th>可操作单据</th>
              </tr>
            </thead>
            <tbody>
              {roles.map(([role, scope, actions]) => (
                <tr key={role}>
                  <td><strong>{role}</strong></td>
                  <td>{scope}</td>
                  <td>{actions}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    );
  }

  function BiModule() {
    const bars = [
      ["服务收入", 86, formatMoney(1260000), "good"],
      ["回款率", 82, "82%", "info"],
      ["项目毛利", 64, "64.8万", "warn"],
      ["工单完成率", 91, "91%", "good"],
      ["客户续约率", 76, "76%", "info"],
      ["物料消耗成本", 38, "18.6万", "danger"],
    ] as const;
    return (
      <div className="page-stack">
        <section className="metric-grid">
          <MetricCard icon={BarChart3} label="月度服务收入" value={formatMoney(1260000)} meta="同比 +18%" tone="good" />
          <MetricCard icon={ClipboardList} label="工单完成率" value="91%" meta="SLA达成 88%" tone="info" />
          <MetricCard icon={Building2} label="客户续约率" value="76%" meta="3家待跟进" tone="warn" />
          <MetricCard icon={PackageMinus} label="物料消耗" value={formatMoney(186000)} meta="高频设备 4类" tone="danger" />
        </section>
        <section className="panel">
          <SectionTitle icon={BarChart3} title="经营指标" right="收入 / 成本 / 客户 / 设备" />
          <div className="bar-list">
            {bars.map(([label, value, display, tone]) => (
              <div className="bar-row" key={label}>
                <span>{label}</span>
                <div className="bar-track">
                  <i className={`bar-fill ${tone}`} style={{ width: `${value}%` }} />
                </div>
                <strong>{display}</strong>
              </div>
            ))}
          </div>
        </section>
        <CustomerInsightPanel />
      </div>
    );
  }

  function QuickCreatePanel() {
    return (
      <aside className="panel quick-panel">
        <SectionTitle icon={Plus} title="报修登记" right="客户工单" />
        <label>
          <span>客户合同</span>
          <select
            value={selectedContractId}
            onChange={(event) => setSelectedContractId(event.target.value)}
          >
            {contracts.map((contract) => (
              <option value={contract.id} key={contract.id}>
                {contract.customer} · {contract.type}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>现场设备</span>
          <select
            value={selectedEquipmentId}
            onChange={(event) => setSelectedEquipmentId(event.target.value)}
          >
            {equipment.map((item) => (
              <option value={item.id} key={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>故障摘要</span>
          <textarea
            value={fault}
            onChange={(event) => setFault(event.target.value)}
            rows={4}
          />
        </label>
        <button className="primary-action" type="button" onClick={createRepairOrder}>
          <Send size={17} />
          <span>生成工单</span>
        </button>
      </aside>
    );
  }

  function OrderDetailPanel() {
    const asset = equipment.find((item) => item.name === selectedOrder.equipment);
    const plannedParts = selectedOrder.partIds
      .map((partId) => parts.find((part) => part.id === partId))
      .filter(Boolean) as Part[];
    const relatedReceivable = receivables.find(
      (item) => item.source === selectedOrder.id,
    );
    const matchedPeople = people.filter((person) =>
      person.certs.includes(selectedOrder.certificate),
    );
    const stageIndex = [
      "待派工",
      "已派工",
      "现场处理中",
      "待客户验收",
      "已闭环",
    ].indexOf(selectedOrder.status);

    return (
      <section className="panel order-detail">
        <SectionTitle
          icon={PanelRightOpen}
          title="工单详情"
          right="设备 / 物料 / 财务联动"
        />
        <div className="detail-head">
          <div>
            <small>{selectedOrder.source} · {selectedOrder.type}</small>
            <h2>{selectedOrder.id}</h2>
            <p>{selectedOrder.customer} · {selectedOrder.project}</p>
          </div>
          <StatusTag tone={orderTone(selectedOrder.status)}>
            {selectedOrder.status}
          </StatusTag>
        </div>

        <div className="stage-line">
          {["派工", "签到", "处理", "验收", "闭环"].map((stage, index) => (
            <span
              className={index <= stageIndex ? "done" : ""}
              key={stage}
            >
              {stage}
            </span>
          ))}
        </div>

        <dl className="detail-grid">
          <div>
            <dt>现场设备</dt>
            <dd>{selectedOrder.equipment}</dd>
          </div>
          <div>
            <dt>安装地址</dt>
            <dd>{asset?.address || "未绑定"}</dd>
          </div>
          <div>
            <dt>所需资质</dt>
            <dd>{selectedOrder.certificate}</dd>
          </div>
          <div>
            <dt>可派人员</dt>
            <dd>{matchedPeople.map((person) => person.name).join("、") || "无"}</dd>
          </div>
          <div>
            <dt>SLA</dt>
            <dd>{selectedOrder.sla}</dd>
          </div>
          <div>
            <dt>已归集成本</dt>
            <dd>{formatMoney(selectedOrder.cost)}</dd>
          </div>
        </dl>

        <div className="detail-section">
          <strong>计划物料</strong>
          <div className="part-chip-list">
            {plannedParts.map((part) => (
              <span
                className={part.stock <= part.safety ? "warn" : ""}
                key={part.id}
              >
                {part.name} · 库存 {part.stock}
              </span>
            ))}
          </div>
        </div>

        <div className="detail-section">
          <strong>财务状态</strong>
          <p>
            {relatedReceivable
              ? `${relatedReceivable.id} · ${relatedReceivable.status} · ${formatMoney(relatedReceivable.amount)}`
              : "客户验收后自动生成应收或免费质保成本记录"}
          </p>
        </div>

        <div className="detail-actions">
          <button
            className="mini-action"
            type="button"
            onClick={() => consumeParts(selectedOrder)}
          >
            <PackageMinus size={15} />
            <span>领料归集</span>
          </button>
          <button
            className="primary-action"
            type="button"
            onClick={() => nextOrderAction(selectedOrder)}
            disabled={selectedOrder.status === "已闭环"}
          >
            <RefreshCw size={16} />
            <span>推进流转</span>
          </button>
        </div>
      </section>
    );
  }

  function WorkOrderTable({ compact = false }: { compact?: boolean }) {
    return (
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>工单</th>
              <th>客户/项目</th>
              <th>类型</th>
              {!compact && <th>设备</th>}
              <th>工程师</th>
              <th>状态</th>
              {!compact && <th>成本</th>}
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr
                className={order.id === selectedOrder.id ? "selected-row" : ""}
                key={order.id}
              >
                <td>
                  <strong>{order.id}</strong>
                  <small>{order.source} · SLA {order.sla}</small>
                </td>
                <td>
                  {order.customer}
                  <small>{order.project}</small>
                </td>
                <td>
                  <PriorityTag priority={order.priority} />
                  <small>{order.type}</small>
                </td>
                {!compact && <td>{order.equipment}</td>}
                <td>
                  {order.engineer}
                  <small>{order.certificate}</small>
                </td>
                <td>
                  <StatusTag tone={orderTone(order.status)}>
                    {order.status}
                  </StatusTag>
                </td>
                {!compact && <td>{formatMoney(order.cost)}</td>}
                <td>
                  <div className="row-actions">
                    <button
                      className="mini-action"
                      type="button"
                      title="查看工单详情"
                      onClick={() => setSelectedOrderId(order.id)}
                    >
                      <PanelRightOpen size={15} />
                      <span>详情</span>
                    </button>
                    <button
                      className="mini-action"
                      type="button"
                      title="扣减库存并归集成本"
                      onClick={() => consumeParts(order)}
                    >
                      <PackageMinus size={15} />
                      <span>领料</span>
                    </button>
                    <button
                      className="mini-action"
                      type="button"
                      title="推进工单状态"
                      onClick={() => nextOrderAction(order)}
                      disabled={order.status === "已闭环"}
                    >
                      <RefreshCw size={15} />
                      <span>{order.status === "待客户验收" ? "闭环" : "流转"}</span>
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  function RiskPanel() {
    const lowParts = parts.filter((part) => part.stock <= part.safety);
    return (
      <div className="panel">
        <SectionTitle icon={CircleAlert} title="风险预警" right="自动触发" />
        <div className="risk-list">
          {receivables
            .filter((item) => item.status === "逾期")
            .map((item) => (
              <div className="risk-row danger" key={item.id}>
                <strong>{item.customer}</strong>
                <span>{item.id} 逾期未回款 {formatMoney(item.amount)}</span>
              </div>
            ))}
          {lowParts.map((part) => (
            <div className="risk-row warn" key={part.id}>
              <strong>{part.name}</strong>
              <span>库存 {part.stock}，安全库存 {part.safety}</span>
            </div>
          ))}
          {people
            .filter((person) => person.certExpire <= "2026-08-31")
            .map((person) => (
              <div className="risk-row info" key={person.name}>
                <strong>{person.name}</strong>
                <span>{person.certs} 将于 {person.certExpire} 到期</span>
              </div>
            ))}
        </div>
      </div>
    );
  }

  function LinkageMatrix() {
    return (
      <section className="panel">
        <SectionTitle
          icon={Link2}
          title="业务联动矩阵"
          right="触发源 / 下游单据 / 财务结果"
        />
        <div className="linkage-grid">
          {linkageRows.map((row) => (
            <article className="linkage-row" key={`${row.source}-${row.trigger}`}>
              <StatusTag tone={row.tone}>{row.trigger}</StatusTag>
              <div>
                <small>来源模块</small>
                <strong>{row.source}</strong>
              </div>
              <div>
                <small>业务动作</small>
                <span>{row.action}</span>
              </div>
              <div>
                <small>下游单据</small>
                <span>{row.target}</span>
              </div>
              <div>
                <small>财务影响</small>
                <span>{row.ledger}</span>
              </div>
              <div>
                <small>责任人</small>
                <strong>{row.owner}</strong>
              </div>
            </article>
          ))}
        </div>
      </section>
    );
  }

  function ControlRulesPanel() {
    return (
      <section className="panel">
        <SectionTitle
          icon={LockKeyhole}
          title="业务规则引擎"
          right="审批前置 / 资质校验 / 成本归集"
        />
        <div className="rule-grid">
          {businessRules.map((rule) => (
            <article className={`rule-card ${rule.tone}`} key={rule.id}>
              <div className="rule-head">
                <small>{rule.id} · {rule.owner}</small>
                <StatusTag tone={rule.tone}>{rule.result}</StatusTag>
              </div>
              <h3>{rule.title}</h3>
              <p>{rule.scene}</p>
              <span>{rule.rule}</span>
            </article>
          ))}
        </div>
      </section>
    );
  }

  function EventTimeline() {
    return (
      <div className="event-list">
        {events.map((event) => (
          <article className={`event-item ${event.tone}`} key={event.id}>
            <time>{event.time}</time>
            <div>
              <strong>{event.title}</strong>
              <p>{event.detail}</p>
            </div>
            <span>{toneLabel[event.tone]}</span>
          </article>
        ))}
      </div>
    );
  }

  function ReceivableList() {
    return (
      <div className="mini-list">
        {receivables.map((item) => (
          <div className="money-row" key={item.id}>
            <div>
              <strong>{item.customer}</strong>
              <span>{item.id} &middot; {item.source} &middot; {item.due}</span>
            </div>
            <b>{formatMoney(item.amount)}</b>
            <StatusTag tone={receivableTone(item.status)}>{item.status}</StatusTag>
            <div className="row-actions">
              {item.status === "待开票" && (
                <button className="mini-action" type="button" onClick={() => handleInvoice(item.id)}>
                  <ReceiptText size={13} /><span>开票</span>
                </button>
              )}
              {item.status === "待回款" && (
                <button className="mini-action" type="button" onClick={() => handleCollect(item.id)}>
                  <CheckCircle2 size={13} /><span>回款登记</span>
                </button>
              )}
              {(item.status === "待回款" || item.status === "逾期") && (
                <button className="mini-action" type="button" onClick={() => handleWriteOff(item.id)}>
                  <FileCheck2 size={13} /><span>核销</span>
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    );
  }

  function LedgerTable() {
    const genVoucher = (type: string, source: string, amount: number) => {
      const code = "PZ-" + Date.now().toString(36).toUpperCase();
      setVouchers((prev: any[]) => [{ id: "v-"+Date.now(), code, bizType: type, bizNo: source,
        voucherDate: new Date().toISOString().slice(0,10), description: type+" "+source,
        status: "DRAFT", totalDebit: amount, totalCredit: amount, entries: [],
      }, ...prev]);
      pushEvent("凭证已生成", code+" "+type, "good");
      logAudit("凭证生成", code+" "+type, "Voucher");
    };
    return (
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>业务</th>
              <th>来源</th>
              <th>金额</th>
              <th>凭证方向</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            {ledgerRows.map(([type, source, amount, entry]) => (
              <tr key={`${type}-${source}`}>
                <td><strong>{type}</strong></td>
                <td>{source}</td>
                <td>{formatMoney(Number(amount))}</td>
                <td>{entry}</td><td><button className="mini-action" type="button" onClick={()=>genVoucher(type as string,source as string,amount as number)}><FileCheck2 size={12} /><span>生成凭证</span></button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  function VoucherPanel() {
    if (vouchers.length === 0) return <p style={{color:"#65716e",fontSize:13,padding:"20px 0",textAlign:"center"}}>暂无会计凭证</p>;
    return (
      <div className="table-wrap"><table><thead><tr><th>凭证号</th><th>日期</th><th>摘要</th><th>借方</th><th>贷方</th><th>状态</th></tr></thead><tbody>
        {vouchers.map((v) => (
          <tr key={v.id} style={{cursor:"pointer"}} onClick={()=>setVoucherDetail(v.id===voucherDetailId?null:v.id)}>
            <td><strong>{v.code}</strong><small>{v.bizType}</small></td>
            <td>{v.voucherDate}</td>
            <td style={{maxWidth:180,overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap" as const}}>{v.description}</td>
            <td style={{textAlign:"right" as const}}>{formatMoney(v.totalDebit)}</td>
            <td style={{textAlign:"right" as const}}>{formatMoney(v.totalCredit)}</td>
            <td><StatusTag tone={v.status==="POSTED"?"good":v.status==="DRAFT"?"info":"warn"}>{v.status==="POSTED"?"已过账":v.status==="DRAFT"?"草稿":"已审核"}</StatusTag></td>
          </tr>
        ))}
      </tbody></table></div>
    );
  }

  function FinancialStatementsPanel() {
    if (!financialStatements) return <p style={{color:"#65716e",fontSize:13,padding:"20px 0",textAlign:"center"}}>暂无财务报表</p>;
    const s = financialStatements;
    return (
      <div className="mini-list">
        <div className="money-row"><div><strong>资产总计</strong><span>资产负债表</span></div><b>{formatMoney(s.totalAssets)}</b><StatusTag tone="good">资产</StatusTag></div>
        <div className="money-row"><div><strong>负债总计</strong></div><b>{formatMoney(s.totalLiabilities)}</b><StatusTag tone="warn">负债</StatusTag></div>
        <div className="money-row"><div><strong>总收入</strong><span>损益表</span></div><b>{formatMoney(s.totalRevenue)}</b><StatusTag tone="good">收入</StatusTag></div>
        <div className="money-row"><div><strong>总费用</strong></div><b>{formatMoney(s.totalExpense)}</b><StatusTag tone="danger">费用</StatusTag></div>
        <div className="money-row"><div><strong>净利润</strong><span>{s.profit>=0?"盈利":"亏损"}</span></div><b>{formatMoney(s.profit)}</b><StatusTag tone={s.profit>=0?"good":"danger"}>{s.profit>=0?"盈利":"亏损"}</StatusTag></div>
        <div className="money-row"><div><strong>净现金流</strong></div><b>{formatMoney(s.netCashFlow)}</b><StatusTag tone={s.netCashFlow>=0?"good":"danger"}>{s.netCashFlow>=0?"正向":"负向"}</StatusTag></div>
      </div>
    );
  }

  function PaymentApplicationsPanel() {
    const pending = paymentApps.filter(a => a.status === "PENDING");
    const approved = paymentApps.filter(a => a.status === "APPROVED");
    const paid = paymentApps.filter(a => a.status === "PAID");
    if (paymentApps.length === 0) return <p style={{color:"#65716e",fontSize:13,padding:"20px 0",textAlign:"center"}}>暂无付款申请</p>;
    return (
      <div className="mini-list">
        <p style={{fontSize:12,color:"#65716e",margin:"0 0 8px",padding:"0 4px"}}>{pending.length}笔待批 · {approved.length}笔已批 · {paid.length}笔已付</p>
        {paymentApps.map((app) => (
          <div className="money-row" key={app.id}>
            <div><strong>{app.supplierName}</strong><span>{app.code} · {app.purpose} · {app.requestedDate}</span></div>
            <b>{formatMoney(app.requestedAmount)}</b>
            <StatusTag tone={app.status==="PAID"?"good":app.status==="APPROVED"?"info":app.status==="REJECTED"?"danger":"warn"}>{app.status==="PENDING"?"待审批":app.status==="APPROVED"?"已批准":app.status==="REJECTED"?"已拒绝":"已付款"}</StatusTag>
            <div className="row-actions">
              {app.status==="PENDING"&&<><button className="mini-action" type="button" onClick={()=>{approvePaymentApplication(app.id,{decision:"APPROVED",comment:"自动",approverName:"系统管理员"}).then(()=>fetchPaymentApplications()).then(l=>{setPaymentApps(l);pushEvent("已批准",app.code,"good");}).catch(e=>pushEvent("审批失败",e.message,"danger"))}}><CheckCircle2 size={13}/><span>批准</span></button><button className="mini-action" type="button" onClick={()=>{approvePaymentApplication(app.id,{decision:"REJECTED",comment:"自动",approverName:"系统管理员"}).then(()=>fetchPaymentApplications()).then(l=>{setPaymentApps(l);pushEvent("已拒绝",app.code,"warn");}).catch(e=>pushEvent("拒绝失败",e.message,"danger"))}}><span style={{color:"#bd3f2f"}}>拒绝</span></button></>}
              logAudit("付款拒绝", app.code+" 已拒绝", "Payment");
              logAudit("付款批准", app.code+" 已批准", "Payment");
              {app.status==="APPROVED"&&<button className="mini-action" type="button" onClick={()=>{executePayment(app.id,{paidDate:new Date().toISOString().slice(0,10),paymentMethod:"BANK_TRANSFER",bankReference:"BK-"+Date.now(),payerName:"系统管理员"}).then(()=>Promise.all([fetchPaymentApplications(),fetchPaymentRecords(),fetchPayables()])).then(([al,rl,pl])=>{setPaymentApps(al);setPaymentRecords(rl);payableApiData.current.clear();setPayables(pl.map((p:any)=>{payableApiData.current.set(p.code,p);return{id:p.code,source:p.orderCode,supplier:p.supplierName,amount:p.outstandingAmount,due:p.dueDate,status:(p.overdue?"逾期":p.status==="PAID"?"已付款":"待付款") as "逾期"|"已付款"|"待付款"};}));pushEvent("付款已执行",app.code,"good");}).catch(e=>pushEvent("付款失败",e.message,"danger"))}}><CheckCircle2 size={13}/><span>执行付款</span></button>}
              logAudit("付款执行", app.code+" 已付款", "Payment");
            </div>
          </div>
        ))}
      </div>
    );
  }

  function PayableList() {
    const handlePay = (code: string) => {
      const api = payableApiData.current.get(code);
      if (api) {
        createPaymentApplication({
          payableId: api.id, requestedAmount: api.outstandingAmount || api.amount,
          requestedDate: new Date().toISOString().slice(0,10),
          applicantName: "系统管理员", purpose: "付款: " + api.supplierName,
        }).then((app) => {
          setPaymentApps((prev) => [app, ...prev]);
          pushEvent("付款申请已提交", app.code + " 待审批", "info");
          logAudit("付款申请", "付款申请已提交", "Payable");
          fetchPayables().then((pyList) => {
            payableApiData.current.clear();
            const mapped = pyList.map((p: any) => { payableApiData.current.set(p.code, p); return {
              id: p.code, source: p.orderCode, supplier: p.supplierName,
              amount: p.outstandingAmount, due: p.dueDate,
              status: (p.overdue ? "逾期" : p.status === "PAID" ? "已付款" : "待付款") as "逾期"|"已付款"|"待付款",
            };});
            setPayables(mapped);
          });
        }).catch((err) => pushEvent("申请失败", err.message, "danger"));
      } else {
        setPayables((c) => c.map((x) => x.id === code ? {...x, status: "已付款" as const} : x));
        pushEvent("付款(离线)", code + " 已标记付款", "good");
      }
    };
    return (
      <div className="mini-list">
        {payables.map((item) => (
          <div className="money-row" key={item.id}>
            <div>
              <strong>{item.supplier}</strong>
              <span>{item.id} &middot; {item.source} &middot; {item.due}</span>
            </div>
            <b>{formatMoney(item.amount)}</b>
            <StatusTag tone={item.status === "已付款" ? "good" : item.status === "逾期" ? "danger" : "info"}>{item.status}</StatusTag>
            <div className="row-actions">
              {(item.status === "待付款" || item.status === "逾期") && (
                <button className="mini-action" type="button" onClick={() => handlePay(item.id)}>
                  <CheckCircle2 size={13} /><ClipboardSignature size={13}/><span>申请付款</span>
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    );
  }

  function AgingPanel() {
    const now = new Date();
    const buckets: Record<string, number> = { "0-30天": 0, "31-60天": 0, "61-90天": 0, "90天以上": 0 };
    const total = receivables.filter(r => r.status !== "已核销").reduce((s, r) => s + r.amount, 0);
    receivables.filter(r => r.status !== "已核销").forEach(r => {
      const due = new Date(r.due);
      const days = Math.floor((now.getTime() - due.getTime()) / (1000 * 60 * 60 * 24));
      if (days <= 30) { buckets["0-30天"] += r.amount; }
      else if (days <= 60) { buckets["31-60天"] += r.amount; }
      else if (days <= 90) { buckets["61-90天"] += r.amount; }
      else { buckets["90天以上"] += r.amount; }
    });
    return (
      <div className="mini-list">
        {Object.entries(buckets).map(([label, amount]) => {
          const pct = total ? Math.round(amount / total * 100) : 0;
          const tone: Tone = label === "90天以上" ? "danger" : label === "61-90天" ? "warn" : "info";
          return (
            <div className="money-row" key={label}>
              <div>
                <strong>{label}</strong>
                <span>占应收 {pct}%</span>
              </div>
              <b>{formatMoney(amount)}</b>
              <StatusTag tone={tone}>{pct}%</StatusTag>
            </div>
          );
        })}
      </div>
    );
  }

  function ReceivableForecastPanel() {
    const now = new Date();
    const weeks: {label:string;amount:number;count:number}[] = [];
    for (let w = 0; w < 8; w++) {
      const start = new Date(now); start.setDate(start.getDate() + w * 7);
      const end = new Date(start); end.setDate(end.getDate() + 6);
      const label = `${start.getMonth()+1}/${start.getDate()}-${end.getMonth()+1}/${end.getDate()}`;
      const items = receivables.filter(r => {
        if (r.status === "已核销") return false;
        const d = new Date(r.due);
        return d >= start && d <= end;
      });
      const amount = items.reduce((s, r) => s + r.amount, 0);
      weeks.push({label, amount, count: items.length});
    }
    const maxAmt = Math.max(...weeks.map(w => w.amount), 1);
    const totalForecast = weeks.reduce((s, w) => s + w.amount, 0);
    return (
      <div className="mini-list">
        <p style={{fontSize:12,color:"#65716e",margin:"0 0 8px 4px"}}>预计未来 8 周可回款 {formatMoney(totalForecast)}（{receivables.filter(r=>r.status!=="已核销").length} 笔）</p>
        {weeks.map((w) => (
          <div className="money-row" key={w.label} style={{gridTemplateColumns:"120px 1fr 90px"}}>
            <div><strong>{w.label}</strong><span>{w.count} 笔到期</span></div>
            <div style={{background:"#e8efe9",borderRadius:4,height:20,overflow:"hidden"}}>
              <div style={{width:`${(w.amount / maxAmt) * 100}%`,height:"100%",background:"#1f6a5b",borderRadius:4,minWidth:w.amount>0?4:0}} />
            </div>
            <b style={{textAlign:"right",fontSize:13}}>{formatMoney(w.amount)}</b>
          </div>
        ))}
      </div>
    );
  }

  function AuditLogPanel() {
    if (auditLog.length === 0) return <p style={{color:"#65716e",fontSize:13,padding:"20px 0",textAlign:"center"}}>暂无操作记录</p>;
    return (
      <div className="mini-list">
        {auditLog.slice(0, 30).map((log) => (
          <div className="money-row" key={log.id} style={{gridTemplateColumns:"70px 1fr"}}>
            <div style={{display:"flex",flexDirection:"column",alignItems:"center"}}>
              <span style={{fontSize:10,color:"#65716e"}}>{log.time.slice(5,16)}</span>
              <StatusTag tone={log.action==="开票"||log.action==="回款"||log.action==="付款执行"||log.action==="付款批准"?"good":log.action==="付款拒绝"||log.action==="核銷"?"danger":"info"}>{log.action}</StatusTag>
            </div>
            <div>
              <strong style={{fontSize:13}}>{log.detail}</strong>
              <span style={{fontSize:11,color:"#65716e"}}>{log.user} · {log.entity}</span>
            </div>
          </div>
        ))}
        {auditLog.length > 30 && <p style={{fontSize:11,color:"#65716e",textAlign:"center",padding:8}}>仅显示最近 30 条</p>}
      </div>
    );
  }
}

function MetricCard({
  icon: Icon,
  label,
  value,
  meta,
  tone,
}: {
  icon: LucideIcon;
  label: string;
  value: string;
  meta: string;
  tone: Tone;
}) {
  return (
    <article className={`metric-card ${tone}`}>
      <Icon size={22} />
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{meta}</small>
    </article>
  );
}

function SectionTitle({
  icon: Icon,
  title,
  right,
}: {
  icon: LucideIcon;
  title: string;
  right: string;
}) {
  return (
    <div className="section-title">
      <div>
        <Icon size={19} />
        <h2>{title}</h2>
      </div>
      <span>{right}</span>
    </div>
  );
}

function StatusTag({
  children,
  tone,
}: {
  children: React.ReactNode;
  tone: Tone;
}) {
  return <span className={`status-tag ${tone}`}>{children}</span>;
}

function PriorityTag({ priority }: { priority: WorkOrder["priority"] }) {
  const tone: Tone =
    priority === "紧急" ? "danger" : priority === "高" ? "warn" : "neutral";
  return <span className={`priority-tag ${tone}`}>{priority}</span>;
}

function orderTone(status: WorkOrderStatus): Tone {
  if (status === "已闭环") return "good";
  if (status === "待派工") return "danger";
  if (status === "待客户验收") return "warn";
  return "info";
}

function contractTone(status: Contract["status"]): Tone {
  if (status === "履约中") return "good";
  if (status === "待续约") return "warn";
  return "danger";
}

function receivableTone(status: Receivable["status"]): Tone {
  if (status === "已核销") return "good";
  if (status === "逾期") return "danger";
  if (status === "待开票") return "warn";
  return "info";
}

function approvalTone(status: Approval["status"]): Tone {
  if (status === "已通过") return "good";
  if (status === "待补充") return "warn";
  return "info";
}

function customerRiskTone(status: CustomerProfile["risk"]): Tone {
  if (status === "正常") return "good";
  if (status === "续约风险") return "warn";
  return "danger";
}

function quoteTone(status: QuotePlan["status"]): Tone {
  if (status === "已通过") return "good";
  if (status === "成本已核对，可以报价") return "good";
  if (status === "审批中") return "info";
  return "warn";
}

createRoot(document.getElementById("root")!).render(<App />);
