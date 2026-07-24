package com.company.ops.api.modules.bi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.company.ops.api.modules.bi.dto.BiDtos.*;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.*;
import com.company.ops.api.modules.crm.repository.*;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.ledger.domain.*;
import com.company.ops.api.modules.ledger.repository.*;
import com.company.ops.api.modules.maintenance.domain.*;
import com.company.ops.api.modules.maintenance.repository.*;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.project.domain.*;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.company.ops.api.common.util.MoneyUtils.amount;

@Service
public class BiService {
  private static final Logger log = LoggerFactory.getLogger(BiService.class);
  private final CustomerRepository customers;
  private final ServiceContractRepository contracts;
  private final ReceivableRepository receivables;
  private final FollowUpRepository followUps;
  private final ProjectRepository projects;
  private final WorkOrderRepository orders;
  private final EquipmentAssetRepository equipment;
  private final InventoryPartRepository parts;
  private final ProcurementPayableRepository payables;
  private final AccountingVoucherRepository vouchers;
  private final AccountingEntryRepository entries;

  public BiService(
      CustomerRepository customers, ServiceContractRepository contracts,
      ReceivableRepository receivables, FollowUpRepository followUps,
      ProjectRepository projects, WorkOrderRepository orders,
      EquipmentAssetRepository equipment, InventoryPartRepository parts,
      ProcurementPayableRepository payables,
      AccountingVoucherRepository vouchers, AccountingEntryRepository entries) {
    this.customers = customers;
    this.contracts = contracts;
    this.receivables = receivables;
    this.followUps = followUps;
    this.projects = projects;
    this.orders = orders;
    this.equipment = equipment;
    this.parts = parts;
    this.payables = payables;
    this.vouchers = vouchers;
    this.entries = entries;
  }

  @Transactional(readOnly = true)
  public ExecutiveDashboard dashboard() {
    try {
    BigDecimal contractRevenue = amount(contracts.sumContractAmount());
    BigDecimal received = amount(receivables.sumSettledAmount());
    BigDecimal receiveOutstanding = amount(receivables.sumOutstandingAmount());
    BigDecimal payableOutstanding = amount(payables.sumOutstandingAmount());
    BigDecimal projectCost = amount(projects.sumActualCost());
    BigDecimal orderCost = amount(orders.sumCostAmount());
    BigDecimal inventoryValue = amount(parts.sumInventoryValue());

    FinancialCash cash = financialCash();
    long activeProjects = projects.countByStageNot(ProjectStage.CLOSED);
    long openOrders = orders.countByStatusNotIn(List.of(WorkOrderStatus.ACCEPTED, WorkOrderStatus.CANCELLED));
    long completed = orders.countByStatus(WorkOrderStatus.ACCEPTED);
    long lowStock = parts.countLowStock();
    
    // 使用 Repository 数据库查询（避免 Stream 过滤中的日期比较 bug）
    long renewals = contracts.countRenewalRisks(LocalDate.now().plusDays(90));
    long complaints = followUps.countByType(FollowUpType.COMPLAINT);

    MetricSummary summary = new MetricSummary(contractRevenue, received, receiveOutstanding,
        payableOutstanding, projectCost, orderCost, inventoryValue,
        cash.in.subtract(cash.out), activeProjects, openOrders, completed,
        lowStock, renewals, complaints);

    return new ExecutiveDashboard(summary, monthlyTrends(),
        customerProfits(), equipmentPerformance(), workforcePerformance());
    } catch (Exception e) {
      log.error("Dashboard query failed", e);
      throw new RuntimeException(e);
    }
  }

  @Transactional(readOnly = true)
  public CompanyKpiDashboard companyDashboard(LocalDate startDate, LocalDate endDate) {
    LocalDate end = endDate == null ? LocalDate.now() : endDate;
    LocalDate start = startDate == null ? end.minusMonths(6).withDayOfMonth(1) : startDate;
    if(start.isAfter(end))throw new BusinessException("开始日期不能晚于结束日期");
    List<ServiceContract> contractRows = contracts.findByBusinessDateBetween(start,end);
    List<Receivable> receivableRows = receivables.findByDueDateBetweenOrderByDueDateAsc(start,end);
    List<Project> projectRows = projects.findByPlannedStartDateBetween(start,end);
    ZoneOffset businessOffset=ZoneOffset.ofHours(8);
    List<WorkOrder> orderRows = orders.findByCreatedAtBetweenOrderByCreatedAtDesc(
        start.atStartOfDay().atOffset(businessOffset),end.atTime(LocalTime.MAX).atOffset(businessOffset));
    List<ProcurementPayable> payableRows = payables.findByDueDateBetweenOrderByDueDateAsc(start,end);
    BigDecimal revenue = sum(contractRows.stream().map(ServiceContract::getAmount).toList())
        .add(sum(orderRows.stream().map(WorkOrder::getBillableAmount).toList()));
    BigDecimal projectActualCost = sum(projectRows.stream().map(Project::getActualCost).toList());
    BigDecimal projectBudget = sum(projectRows.stream().map(Project::getBudgetAmount).toList());
    BigDecimal workOrderCost = sum(orderRows.stream().map(WorkOrder::getCostAmount).toList());
    BigDecimal procurementAmount = sum(payableRows.stream().map(ProcurementPayable::getAmount).toList());
    BigDecimal inventoryValue = sum(parts.findAllByOrderByCreatedAtDesc().stream()
        .map(part -> amount(part.getStockQty()).multiply(amount(part.getUnitCost()))).toList());
    BigDecimal cashIn = sum(receivableRows.stream().map(Receivable::getSettledAmount).toList());
    BigDecimal cashOut = sum(payableRows.stream().map(ProcurementPayable::getPaidAmount).toList());
    BigDecimal grossProfit = revenue.subtract(projectActualCost).subtract(workOrderCost);
    BigDecimal hours = sum(orderRows.stream().map(WorkOrder::getLaborHours).toList());
    BigDecimal outboundCost = workOrderCost.add(projectActualCost);
    return new CompanyKpiDashboard(start, end, revenue, grossProfit, percent(grossProfit, revenue),
        cashIn, cashOut, cashIn.subtract(cashOut), projectBudget, projectActualCost,
        percent(projectActualCost.subtract(projectBudget), projectBudget), procurementAmount,
        percent(procurementAmount, revenue), inventoryValue,
        inventoryValue.compareTo(BigDecimal.ZERO) > 0 ? outboundCost.divide(inventoryValue, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
        hours.compareTo(BigDecimal.ZERO) > 0 ? revenue.divide(hours, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
        monthlyTrends(YearMonth.from(end)), customerProfits(customers.findAllByOrderByCreatedAtDesc(), contractRows, receivableRows, orderRows));
  }

  private List<MonthlyTrend> monthlyTrends() {
    return monthlyTrends(YearMonth.now());
  }

  private List<MonthlyTrend> monthlyTrends(YearMonth endMonth) {
    List<MonthlyTrend> result = new ArrayList<>();
    try {
      List<Object[]> rows = entries.aggregateMonthlyTrends();
      java.util.Map<String, Object[]> trendMap = new java.util.LinkedHashMap<>();
      for (Object[] row : rows) {
        int year = ((Number) row[0]).intValue();
        int month = ((Number) row[1]).intValue();
        String key = year + "-" + String.format("%02d", month);
        trendMap.put(key, row);
      }
      for (int i = 5; i >= 0; i--) {
        YearMonth month = endMonth.minusMonths(i);
        String key = month.toString();
        Object[] row = trendMap.get(key);
        if (row != null) {
          BigDecimal revenue = (BigDecimal) row[2];
          BigDecimal expense = (BigDecimal) row[3];
          BigDecimal cashIn = (BigDecimal) row[4];
          BigDecimal cashOut = (BigDecimal) row[5];
          result.add(new MonthlyTrend(key, revenue, expense, cashIn, cashOut, revenue.subtract(expense)));
        } else {
          result.add(new MonthlyTrend(key, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
      }
      return result;
    } catch (Exception e) {
      log.warn("Database monthly trends failed, falling back to in-memory", e);
      return inMemoryMonthlyTrends(endMonth);
    }
  }

  private List<MonthlyTrend> inMemoryMonthlyTrends(YearMonth endMonth) {
    List<AccountingVoucher> voucherRows = vouchers.findAllByOrderByVoucherDateDescCreatedAtDesc();
    List<MonthlyTrend> result = new ArrayList<>();
    for (int i = 5; i >= 0; i--) {
      YearMonth month = endMonth.minusMonths(i);
      BigDecimal revenue = BigDecimal.ZERO, expense = BigDecimal.ZERO,
          cashIn = BigDecimal.ZERO, cashOut = BigDecimal.ZERO;
      for (AccountingVoucher voucher : voucherRows) {
        if (voucher.getVoucherDate() == null || !YearMonth.from(voucher.getVoucherDate()).equals(month))
          continue;
        for (AccountingEntry entry : entries.findByVoucherIdOrderByCreatedAtAsc(voucher.getId())) {
          if ("1002".equals(entry.getAccountCode())) {
            cashIn = cashIn.add(amount(entry.getDebit()));
            cashOut = cashOut.add(amount(entry.getCredit()));
          }
          if (entry.getAccountCode() != null && entry.getAccountCode().startsWith("6")
              && entry.getAccountCode().compareTo("6400") < 0)
            revenue = revenue.add(amount(entry.getCredit()).subtract(amount(entry.getDebit())));
          if (entry.getAccountCode() != null && entry.getAccountCode().startsWith("6")
              && entry.getAccountCode().compareTo("6400") >= 0)
            expense = expense.add(amount(entry.getDebit()).subtract(amount(entry.getCredit())));
        }
      }
      result.add(new MonthlyTrend(month.toString(), revenue, expense, cashIn, cashOut,
          revenue.subtract(expense)));
    }
    return result;
  }

  private List<CustomerProfit> customerProfits(List<Customer> customerRows,
      List<ServiceContract> contractRows, List<Receivable> receivableRows,
      List<WorkOrder> orderRows) {
    Map<UUID, Long> complaints = followUps.countByTypeGroupByCustomer(FollowUpType.COMPLAINT).stream()
        .collect(Collectors.toMap(row->(UUID)row[0],row->((Number)row[1]).longValue()));
    Map<UUID,BigDecimal> contractsByCustomer=contractRows.stream().collect(Collectors.groupingBy(
        ServiceContract::getCustomerId,Collectors.reducing(BigDecimal.ZERO,c->amount(c.getAmount()),BigDecimal::add)));
    Map<UUID,BigDecimal> receivedByCustomer=receivableRows.stream().collect(Collectors.groupingBy(
        Receivable::getCustomerId,Collectors.reducing(BigDecimal.ZERO,r->amount(r.getSettledAmount()),BigDecimal::add)));
    Map<UUID,List<WorkOrder>> ordersByCustomer=orderRows.stream().filter(o->o.getCustomerId()!=null)
        .collect(Collectors.groupingBy(WorkOrder::getCustomerId));
    return customerRows.stream().map(c -> {
      BigDecimal contractAmount = contractsByCustomer.getOrDefault(c.getId(),BigDecimal.ZERO);
      BigDecimal received = receivedByCustomer.getOrDefault(c.getId(),BigDecimal.ZERO);
      List<WorkOrder> customerOrders = ordersByCustomer.getOrDefault(c.getId(),List.of());
      BigDecimal workRevenue = sum(customerOrders.stream().map(WorkOrder::getBillableAmount).toList());
      BigDecimal cost = sum(customerOrders.stream().map(WorkOrder::getCostAmount).toList());
      BigDecimal profit = contractAmount.add(workRevenue).subtract(cost);
      BigDecimal base = contractAmount.add(workRevenue);
      return new CustomerProfit(c.getId(), c.getName(), contractAmount, received,
          workRevenue, cost, profit,
          base.compareTo(BigDecimal.ZERO) > 0
              ? profit.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO,
          complaints.getOrDefault(c.getId(), 0L));
    }).sorted(Comparator.comparing(CustomerProfit::grossProfit).reversed()).limit(20).toList();
  }

  private List<CustomerProfit> customerProfits() {
    Map<UUID,BigDecimal> contractsByCustomer=contracts.aggregateAmountByCustomer().stream().collect(
        Collectors.toMap(row->(UUID)row[0],row->amount((BigDecimal)row[1])));
    Map<UUID,BigDecimal> receivedByCustomer=receivables.aggregateSettledByCustomer().stream().collect(
        Collectors.toMap(row->(UUID)row[0],row->amount((BigDecimal)row[1])));
    Map<UUID,Object[]> ordersByCustomer=orders.aggregateByCustomer().stream().collect(
        Collectors.toMap(row->(UUID)row[0],Function.identity()));
    Map<UUID,Long> complaints=followUps.countByTypeGroupByCustomer(FollowUpType.COMPLAINT).stream().collect(
        Collectors.toMap(row->(UUID)row[0],row->((Number)row[1]).longValue()));
    Set<UUID> customerIds=new HashSet<>();
    customerIds.addAll(contractsByCustomer.keySet());customerIds.addAll(receivedByCustomer.keySet());
    customerIds.addAll(ordersByCustomer.keySet());customerIds.addAll(complaints.keySet());
    return customers.findAllById(customerIds).stream().map(c->{
      BigDecimal contractAmount=contractsByCustomer.getOrDefault(c.getId(),BigDecimal.ZERO);
      BigDecimal received=receivedByCustomer.getOrDefault(c.getId(),BigDecimal.ZERO);
      Object[] orderRow=ordersByCustomer.get(c.getId());
      BigDecimal workRevenue=orderRow==null?BigDecimal.ZERO:amount((BigDecimal)orderRow[1]);
      BigDecimal cost=orderRow==null?BigDecimal.ZERO:amount((BigDecimal)orderRow[2]);
      BigDecimal profit=contractAmount.add(workRevenue).subtract(cost);
      BigDecimal base=contractAmount.add(workRevenue);
      return new CustomerProfit(c.getId(),c.getName(),contractAmount,received,workRevenue,cost,profit,
          base.signum()>0?profit.multiply(BigDecimal.valueOf(100)).divide(base,2,RoundingMode.HALF_UP):BigDecimal.ZERO,
          complaints.getOrDefault(c.getId(),0L));
    }).sorted(Comparator.comparing(CustomerProfit::grossProfit).reversed()).limit(20).toList();
  }

  private List<EquipmentPerformance> equipmentPerformance() {
    List<EquipmentAsset> assets=equipment.findAllByOrderByNextMaintenanceDateAsc();
    Set<UUID> customerIds=assets.stream().map(EquipmentAsset::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
    Map<UUID, Customer> customerMap = customers.findAllById(customerIds).stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
    Map<UUID,Object[]> performance=orders.aggregateByEquipment().stream().collect(Collectors.toMap(row->(UUID)row[0],Function.identity()));
    return assets.stream().map(asset -> {
      Object[] row=performance.get(asset.getId());
      long total=row==null?0:((Number)row[1]).longValue();
      long faults=row==null?0:((Number)row[2]).longValue();
      BigDecimal cost=row==null?BigDecimal.ZERO:amount((BigDecimal)row[3]);
      return new EquipmentPerformance(asset.getId(), asset.getCode(), asset.getName(),
          customerMap.get(asset.getCustomerId()) == null ? null
              : customerMap.get(asset.getCustomerId()).getName(),
          total, faults, cost,
          total==0 ? BigDecimal.ZERO : cost.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
    }).sorted(Comparator.comparing(EquipmentPerformance::faultCount).reversed()).limit(20).toList();
  }

  private List<WorkforcePerformance> workforcePerformance() {
    try {
      return orders.aggregateByAssignee().stream().map(row -> {
        UUID assigneeId = (UUID) row[0];
        String name = (String) row[1];
        long totalOrders = (long) row[2];
        long completed = (long) row[3];
        BigDecimal hours = (BigDecimal) row[4];
        BigDecimal revenue = (BigDecimal) row[5];
        BigDecimal laborCost = (BigDecimal) row[6];
        return new WorkforcePerformance(assigneeId, name, completed, hours, revenue, laborCost,
            hours.compareTo(BigDecimal.ZERO) > 0
                ? revenue.divide(hours, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
      }).sorted(Comparator.comparing(WorkforcePerformance::generatedRevenue).reversed()).toList();
    } catch (Exception e) {
      log.warn("Database workforce aggregation failed, falling back to in-memory", e);
      return inMemoryWorkforcePerformance(orders.findAllByOrderByCreatedAtDesc());
    }
  }

  private List<WorkforcePerformance> inMemoryWorkforcePerformance(List<WorkOrder> orderRows) {
    return orderRows.stream().filter(o -> o.getAssigneeId() != null)
        .collect(Collectors.groupingBy(WorkOrder::getAssigneeId)).entrySet().stream().map(e -> {
          List<WorkOrder> rows = e.getValue();
          BigDecimal hours = sum(rows.stream().map(WorkOrder::getLaborHours).toList());
          BigDecimal revenue = sum(rows.stream().map(WorkOrder::getBillableAmount).toList());
          BigDecimal laborCost = sum(rows.stream().map(WorkOrder::getLaborCost).toList());
          return new WorkforcePerformance(e.getKey(),
              rows.get(0).getEngineerName(),
              rows.stream().filter(o -> o.getStatus() == WorkOrderStatus.ACCEPTED).count(),
              hours, revenue, laborCost,
              hours.compareTo(BigDecimal.ZERO) > 0
                  ? revenue.divide(hours, 2, RoundingMode.HALF_UP)
                  : BigDecimal.ZERO);
        }).sorted(Comparator.comparing(WorkforcePerformance::generatedRevenue).reversed()).toList();
  }

  private FinancialCash financialCash() {
    try {
      BigDecimal in = entries.sumDebitByAccountCode("1002");
      BigDecimal out = entries.sumCreditByAccountCode("1002");
      return new FinancialCash(in == null ? BigDecimal.ZERO : in, out == null ? BigDecimal.ZERO : out);
    } catch (Exception e) {
      log.warn("Database cash aggregation failed, falling back to in-memory", e);
      BigDecimal in = BigDecimal.ZERO, out = BigDecimal.ZERO;
      for (AccountingEntry entry : entries.findAllByOrderByAccountCodeAsc())
        if (entry.getAccountCode().equals("1002")) {
          in = in.add(amount(entry.getDebit()));
          out = out.add(amount(entry.getCredit()));
        }
      return new FinancialCash(in, out);
    }
  }

  private record FinancialCash(BigDecimal in, BigDecimal out) {
  }

  private BigDecimal sum(List<BigDecimal> values) {
    return values.stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal amount(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
    return amount(denominator).compareTo(BigDecimal.ZERO) > 0
        ? amount(numerator).multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;
  }
}
