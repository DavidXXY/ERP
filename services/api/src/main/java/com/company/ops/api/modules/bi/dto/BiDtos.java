package com.company.ops.api.modules.bi.dto;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.YearMonth; import java.util.List; import java.util.UUID;
public final class BiDtos {private BiDtos(){}
  public record MetricSummary(BigDecimal contractRevenue,BigDecimal receivedAmount,BigDecimal receivableOutstanding,BigDecimal payableOutstanding,BigDecimal projectCost,BigDecimal workOrderCost,BigDecimal inventoryValue,BigDecimal netCashFlow,long activeProjects,long openWorkOrders,long completedWorkOrders,long lowStockParts,long renewalRisks,long complaints){}
  public record MonthlyTrend(String month,BigDecimal revenue,BigDecimal expense,BigDecimal cashIn,BigDecimal cashOut,BigDecimal profit){}
  public record CustomerProfit(UUID customerId,String customerName,BigDecimal contractAmount,BigDecimal receivedAmount,BigDecimal workOrderRevenue,BigDecimal workOrderCost,BigDecimal grossProfit,BigDecimal margin,long complaints){}
  public record EquipmentPerformance(UUID equipmentId,String equipmentCode,String equipmentName,String customerName,long workOrderCount,long faultCount,BigDecimal maintenanceCost,BigDecimal averageCost){}
  public record WorkforcePerformance(UUID userId,String engineerName,long completedOrders,BigDecimal laborHours,BigDecimal generatedRevenue,BigDecimal laborCost,BigDecimal outputPerHour){}
  public record ExecutiveDashboard(MetricSummary summary,List<MonthlyTrend> monthlyTrends,List<CustomerProfit> customerProfits,List<EquipmentPerformance> equipmentPerformance,List<WorkforcePerformance> workforcePerformance){}
  public record CompanyKpiDashboard(LocalDate startDate,LocalDate endDate,BigDecimal revenue,BigDecimal grossProfit,BigDecimal grossMarginRate,BigDecimal cashIn,BigDecimal cashOut,BigDecimal netCashFlow,BigDecimal projectBudget,BigDecimal projectActualCost,BigDecimal projectBudgetDeviationRate,BigDecimal procurementAmount,BigDecimal procurementRatio,BigDecimal inventoryValue,BigDecimal inventoryTurnover,BigDecimal workforceOutputPerHour,List<MonthlyTrend> trends,List<CustomerProfit> topCustomers){}
}
