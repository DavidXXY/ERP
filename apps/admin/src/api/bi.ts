import { request } from "./http";
export type MetricSummary = {
  contractRevenue: number;
  receivedAmount: number;
  receivableOutstanding: number;
  payableOutstanding: number;
  projectCost: number;
  workOrderCost: number;
  inventoryValue: number;
  netCashFlow: number;
  activeProjects: number;
  openWorkOrders: number;
  completedWorkOrders: number;
  lowStockParts: number;
  renewalRisks: number;
  complaints: number;
};
export type MonthlyTrend = {
  month: string;
  revenue: number;
  expense: number;
  cashIn: number;
  cashOut: number;
  profit: number;
};
export type CustomerProfit = {
  customerId: string;
  customerName: string;
  contractAmount: number;
  receivedAmount: number;
  workOrderRevenue: number;
  workOrderCost: number;
  grossProfit: number;
  margin: number;
  complaints: number;
};
export type EquipmentPerformance = {
  equipmentId: string;
  equipmentCode: string;
  equipmentName: string;
  customerName: string;
  workOrderCount: number;
  faultCount: number;
  maintenanceCost: number;
  averageCost: number;
};
export type WorkforcePerformance = {
  userId: string;
  engineerName: string;
  completedOrders: number;
  laborHours: number;
  generatedRevenue: number;
  laborCost: number;
  outputPerHour: number;
};
export type ExecutiveDashboard = {
  summary: MetricSummary;
  monthlyTrends: MonthlyTrend[];
  customerProfits: CustomerProfit[];
  equipmentPerformance: EquipmentPerformance[];
  workforcePerformance: WorkforcePerformance[];
};
export type CompanyKpiDashboard = {
  startDate: string;
  endDate: string;
  revenue: number;
  grossProfit: number;
  grossMarginRate: number;
  cashIn: number;
  cashOut: number;
  netCashFlow: number;
  projectBudget: number;
  projectActualCost: number;
  projectBudgetDeviationRate: number;
  procurementAmount: number;
  procurementRatio: number;
  inventoryValue: number;
  inventoryTurnover: number;
  workforceOutputPerHour: number;
  trends: MonthlyTrend[];
  topCustomers: CustomerProfit[];
};
export function getExecutiveDashboard() {
  return request<ExecutiveDashboard>({ method: "GET", url: "/bi/dashboard" });
}
export function getCompanyKpiDashboard(params?: {
  startDate?: string;
  endDate?: string;
}) {
  return request<CompanyKpiDashboard>({
    method: "GET",
    url: "/bi/company-dashboard",
    params,
  });
}
