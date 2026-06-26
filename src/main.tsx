import React, { useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
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
  ShieldCheck,
  TimerReset,
  TrendingUp,
  Truck,
  UserRoundCheck,
  UsersRound,
  WalletCards,
  Wrench,
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
  status: "草稿" | "审批中" | "已通过";
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
  { id: "inventory", label: "备件仓储", group: "核心业务", icon: PackageCheck },
  { id: "finance", label: "财务资金", group: "资金管控", icon: Coins },
  { id: "hr", label: "组织外勤", group: "底层支撑", icon: UsersRound },
  { id: "oa", label: "OA审批", group: "底层支撑", icon: ClipboardCheck },
  { id: "documents", label: "电子档案", group: "底层支撑", icon: FileArchive },
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
  { id: "contracts", label: "维保合同", icon: BriefcaseBusiness },
  { id: "followups", label: "跟进回访", icon: CalendarCheck },
  { id: "renewals", label: "续约管理", icon: TimerReset },
  { id: "receivables", label: "合同应收", icon: WalletCards },
  { id: "insights", label: "客户画像", icon: BarChart3 },
];

const contracts: Contract[] = [
  {
    id: "HT-2026-018",
    customer: "华东轨交能源中心",
    project: "年度高压配电维保",
    type: "年度包年维保",
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
    project: "泵站驻场运维",
    type: "驻场运维",
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
    project: "消防联动系统维保",
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
    project: "年度高压配电维保",
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
    project: "泵站驻场运维",
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
    project: "消防联动系统维保",
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
    title: "泵站驻场交通住宿",
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
    title: "备件库存触发采购",
    detail: "变频器控制板低于安全库存，已进入采购审批",
    tone: "warn",
  },
  {
    id: "EV-03",
    time: "11:30",
    title: "项目成本归集",
    detail: "消防联动模块领用、外勤工时已计入启明商业广场项目",
    tone: "info",
  },
  {
    id: "EV-04",
    time: "13:42",
    title: "合同节点生成应收",
    detail: "华东轨交二季度维保款进入待回款台账",
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
    role: "高压运维工程师",
    certs: "高压电工证 / 登高证",
    certExpire: "2026-12-08",
    location: "浦东新区",
    schedule: "轨交驻场",
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
    role: "消防维保工程师",
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
    type: "备件供货商",
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
  ["合同档案", "维保合同 / 补充协议", "128份", "3份待归档"],
  ["设备图纸", "竣工图 / 接线图 / 点位表", "346份", "12份本月新增"],
  ["检测报告", "年度检测 / 第三方报告", "94份", "5份待客户确认"],
  ["人员证书", "电工 / 焊工 / 登高 / 消防", "41份", "2份临期"],
  ["验收单", "工单验收 / 项目终验", "219份", "7份待签章"],
  ["招投标", "标书 / 报价 / 澄清函", "56份", "4份审批中"],
];

const ledgerRows = [
  ["维保收入", "HT-2026-018", 315000, "应收账款 / 主营业务收入"],
  ["备件成本", "GD-202606-1098", -620, "主营业务成本 / 库存商品"],
  ["人工成本", "GD-202606-1128", -1000, "项目成本 / 应付职工薪酬"],
  ["差旅费用", "SP-202606-276", -2360, "项目成本 / 银行存款"],
];

const roles = [
  ["运维工程师", "本人负责工单 / 负责项目设备", "照片、检测记录、领料申请"],
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
    target: "备件仓储",
    ledger: "库存成本入项目",
    owner: "仓储主管",
    tone: "info",
  },
  {
    trigger: "安全库存不足",
    source: "备件仓储",
    action: "自动生成采购申请",
    target: "供应链采购",
    ledger: "审批后入库生成应付",
    owner: "采购",
    tone: "warn",
  },
  {
    trigger: "项目验收",
    source: "项目管理",
    action: "归集工时/备件/外包/差旅",
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
    rule: "备件从工单领用后，库存减少，并写入工单与项目成本。",
    result: "实时计算维保毛利",
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
    rule: "质保期内设备故障可生成免费工单，但人工与备件仍进入项目售后成本。",
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
    contacts: "刘工 / 运维部",
    sites: 8,
    addresses: ["城北泵站", "城南泵站", "东区调蓄池", "西区加压站"],
    payment: "月结，存在逾期",
    lastVisit: "2026-06-21",
    risk: "逾期",
    riskNote: "夜间响应要求提高，本月驻场排班与回款需要同步跟进。",
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
    riskNote: "当前处于现场勘查阶段，重点推进UPS备件包报价。",
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
    need: "UPS与配电年度维保",
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
    need: "园区机电驻场运维",
    stage: "需求确认",
    owner: "客户经理B",
    value: 1380000,
    probability: 52,
    nextAction: "确认驻场人数与服务边界",
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
    status: "草稿",
  },
  {
    id: "BJ-202606-041",
    customer: "启明商业广场",
    type: "续约维保",
    amount: 468000,
    scope: "消防系统维保、故障抢修、年度联动测试",
    cycle: "月度巡检",
    paymentNodes: "半年付",
    owner: "客户经理C",
    status: "审批中",
  },
  {
    id: "BJ-202606-045",
    customer: "东城数据中心",
    type: "年度包年维保",
    amount: 680000,
    scope: "UPS、配电柜、温湿度监控维保",
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
    next: "调整驻场排班并反馈项目经理",
    tone: "warn",
  },
  {
    id: "GJ-202606-205",
    customer: "东城数据中心",
    type: "拜访",
    owner: "客户经理A",
    date: "2026-06-24",
    summary: "现场确认UPS维保范围，客户关注备件响应和应急演练。",
    next: "补充备件包报价",
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
  const [approvals, setApprovals] = useState<Approval[]>(initialApprovals);
  const [events, setEvents] = useState<FlowEvent[]>(initialEvents);
  const [selectedContractId, setSelectedContractId] = useState(contracts[0].id);
  const [selectedEquipmentId, setSelectedEquipmentId] = useState(equipment[1].id);
  const [selectedOrderId, setSelectedOrderId] = useState(initialOrders[0].id);
  const [fault, setFault] = useState("变频器报警，现场无法复位");
  const [toast, setToast] = useState("工程运维一体化系统已加载");

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
    const cost = orders.reduce((sum, item) => sum + item.cost, 0);
    const revenue = contracts.reduce((sum, item) => sum + item.amount, 0);
    const gross = revenue - projects.reduce((sum, item) => sum + item.cost, 0);
    return {
      orderOpen,
      urgent,
      lowStock,
      overdue,
      pendingApprovals,
      receivableTotal,
      cost,
      revenue,
      gross,
    };
  }, [orders, parts, receivables, approvals]);

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
    pushEvent("备件补货申请", `${part.name} 已进入供应链采购审批`, "warn");
  };

  const advanceLead = (lead: CrmLead) => {
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
                      ? "转入维保合同与项目计划"
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
    if (quote.status !== "草稿") {
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
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Route size={22} />
          </div>
          <div>
            <strong>工程运维ERP</strong>
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
            label="低库存备件"
            value={`${metrics.lowStock}类`}
            meta="自动触发采购"
            tone="info"
          />
          <MetricCard
            icon={Coins}
            label="工单已归集成本"
            value={formatMoney(metrics.cost)}
            meta="人工 + 备件"
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
                ["备件仓储", "入库 / 领用 / 安全库存"],
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
                  <span>推进</span>
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
                disabled={quote.status !== "草稿"}
              >
                <Send size={15} />
                <span>提交</span>
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
          title="维保合同"
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
            label="待采购备件"
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
            meta="备件供货 / 外协检测"
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
                  <span>暂无低于安全库存的备件</span>
                </div>
              )}
            </div>
          </div>
        </section>

        <section className="panel">
          <SectionTitle
            icon={Truck}
            title="供应商与外包服务商"
            right="备件供货 / 外协施工 / 检测结算"
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
            <SectionTitle icon={MapPin} title="外勤轨迹" right="签到定位 / SLA" />
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
        <SectionTitle icon={Database} title="甲方设备台账" right="故障历史 / 维保周期 / 质保提醒" />
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>设备</th>
                <th>客户</th>
                <th>安装地址</th>
                <th>维保周期</th>
                <th>下次维保</th>
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
          <SectionTitle icon={PackageCheck} title="备品备件库存" right="入库 / 领用 / 归还 / 报废 / 盘点" />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>备件</th>
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

  function FinanceModule() {
    return (
      <div className="page-stack">
        <section className="metric-grid">
          <MetricCard icon={WalletCards} label="应收余额" value={formatMoney(metrics.receivableTotal)} meta="合同 + 单次维修" tone="warn" />
          <MetricCard icon={Coins} label="应付余额" value={formatMoney(132400)} meta="采购 + 外包 + 报销" tone="info" />
          <MetricCard icon={CheckCircle2} label="本月核销" value={formatMoney(486000)} meta="回款率 82%" tone="good" />
          <MetricCard icon={CircleAlert} label="逾期笔数" value={`${metrics.overdue}笔`} meta="催收台账" tone="danger" />
        </section>
        <section className="split-grid">
          <div className="panel">
            <SectionTitle icon={WalletCards} title="应收款" right="开票 / 回款 / 核销" />
            <ReceivableList />
          </div>
          <div className="panel">
            <SectionTitle icon={Coins} title="业务凭证线索" right="总账同步" />
            <LedgerTable />
          </div>
        </section>
      </div>
    );
  }

  function HrModule() {
    return (
      <section className="panel">
        <SectionTitle icon={UserRoundCheck} title="组织人事与外勤" right="资质 / 排班 / 补贴 / 绩效" />
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
      ["维保收入", 86, formatMoney(1260000), "good"],
      ["回款率", 82, "82%", "info"],
      ["项目毛利", 64, "64.8万", "warn"],
      ["工单完成率", 91, "91%", "good"],
      ["客户续约率", 76, "76%", "info"],
      ["备件消耗成本", 38, "18.6万", "danger"],
    ] as const;
    return (
      <div className="page-stack">
        <section className="metric-grid">
          <MetricCard icon={BarChart3} label="月度维保收入" value={formatMoney(1260000)} meta="同比 +18%" tone="good" />
          <MetricCard icon={ClipboardList} label="工单完成率" value="91%" meta="SLA达成 88%" tone="info" />
          <MetricCard icon={Building2} label="客户续约率" value="76%" meta="3家待跟进" tone="warn" />
          <MetricCard icon={PackageMinus} label="备件消耗" value={formatMoney(186000)} meta="高频设备 4类" tone="danger" />
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
          <span>维保合同</span>
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
          right="设备 / 备件 / 财务联动"
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
          <strong>计划备件</strong>
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
              <span>{item.id} · {item.source} · {item.due}</span>
            </div>
            <b>{formatMoney(item.amount)}</b>
            <StatusTag tone={receivableTone(item.status)}>{item.status}</StatusTag>
          </div>
        ))}
      </div>
    );
  }

  function LedgerTable() {
    return (
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>业务</th>
              <th>来源</th>
              <th>金额</th>
              <th>凭证方向</th>
            </tr>
          </thead>
          <tbody>
            {ledgerRows.map(([type, source, amount, entry]) => (
              <tr key={`${type}-${source}`}>
                <td><strong>{type}</strong></td>
                <td>{source}</td>
                <td>{formatMoney(Number(amount))}</td>
                <td>{entry}</td>
              </tr>
            ))}
          </tbody>
        </table>
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
  if (status === "审批中") return "info";
  return "warn";
}

createRoot(document.getElementById("root")!).render(<App />);
