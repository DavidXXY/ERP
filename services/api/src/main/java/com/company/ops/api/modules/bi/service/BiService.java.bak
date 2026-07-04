package com.company.ops.api.modules.bi.service;

import com.company.ops.api.modules.bi.dto.BiDtos.*;
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
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BiService {
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
    List<Customer> customerRows = customers.findAllByOrderByCreatedAtDesc();
    List<ServiceContract> contractRows = contracts.findAllByOrderByEndDateAsc();
    List<Receivable> receivableRows = receivables.findAllByOrderByDueDateAsc();
    List<Project> projectRows = projects.findAllByOrderByCreatedAtDesc();
    List<WorkOrder> orderRows = orders.findAllByOrderByCreatedAtDesc();
    List<InventoryPart> partRows = parts.findAllByOrderByCreatedAtDesc();
    List<ProcurementPayable> payableRows = payables.findAllByOrderByDueDateAsc();

    BigDecimal contractRevenue = sum(contractRows.stream().map(ServiceContract::getAmount).toList());
    BigDecimal received = sum(receivableRows.stream().map(Receivable::getSettledAmount).toList());
    BigDecimal receiveOutstanding = sum(receivableRows.stream()
        .map(r -> amount(r.getAmount()).subtract(amount(r.getSettledAmount()))).toList());
    BigDecimal payableOutstanding = sum(payableRows.stream()
        .map(p -> amount(p.getAmount()).subtract(amount(p.getPaidAmount()))).toList());
    BigDecimal projectCost = sum(projectRows.stream().map(Project::getActualCost).toList());
    BigDecimal orderCost = sum(orderRows.stream().map(WorkOrder::getCostAmount).toList());
    BigDecimal inventoryValue = sum(partRows.stream()
        .map(p -> amount(p.getStockQty()).multiply(amount(p.getUnitCost()))).toList());

    FinancialCash cash = financialCash();
    long activeProjects = projectRows.stream()
        .filter(p -> p.getStage() != ProjectStage.CLOSED).count();
    long openOrders = orderRows.stream()
        .filter(o -> o.getStatus() != WorkOrderStatus.ACCEPTED
            && o.getStatus() != WorkOrderStatus.CANCELLED).count();
    long completed = orderRows.stream()
        .filter(o -> o.getStatus() == WorkOrderStatus.ACCEPTED).count();
    long lowStock = partRows.stream().filter(InventoryPart::isLowStock).count();
    
    // 使用 Repository 数据库查询（避免 Stream 过滤中的日期比较 bug）
    long renewals = contracts.countRenewalRisks(LocalDate.now().plusDays(90));
    long complaints = followUps.countByType(FollowUpType.COMPLAINT);

    MetricSummary summary = new MetricSummary(contractRevenue, received, receiveOutstanding,
        payableOutstanding, projectCost, orderCost, inventoryValue,
        cash.in.subtract(cash.out), activeProjects, openOrders, completed,
        lowStock, renewals, complaints);

    return new ExecutiveDashboard(summary, monthlyTrends(),
        customerProfits(customerRows, contractRows, receivableRows, orderRows),
        equipmentPerformance(customerRows, orderRows),
        workforcePerformance(orderRows));
  }

  private List<MonthlyTrend> monthlyTrends() {
    List<AccountingVoucher> voucherRows = vouchers.findAllByOrderByVoucherDateDescCreatedAtDesc();
    List<MonthlyTrend> result = new ArrayList<>();
    YearMonth now = YearMonth.now();
    for (int i = 5; i >= 0; i--) {
      YearMonth month = now.minusMonths(i);
      BigDecimal revenue = BigDecimal.ZERO, expense = BigDecimal.ZERO,
          cashIn = BigDecimal.ZERO, cashOut = BigDecimal.ZERO;
      for (AccountingVoucher voucher : voucherRows) {
        if (!YearMonth.from(voucher.getVoucherDate()).equals(month))
          continue;
        for (AccountingEntry entry : entries.findByVoucherIdOrderByCreatedAtAsc(voucher.getId())) {
          if (entry.getAccountCode().equals("1002")) {
            cashIn = cashIn.add(amount(entry.getDebit()));
            cashOut = cashOut.add(amount(entry.getCredit()));
          }
          if (entry.getAccountCode().startsWith("6")
              && entry.getAccountCode().compareTo("6400") < 0)
            revenue = revenue.add(amount(entry.getCredit()).subtract(amount(entry.getDebit())));
          if (entry.getAccountCode().startsWith("6")
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
    Map<UUID, Long> complaints = followUps.findAllByOrderByFollowedAtDesc().stream()
        .filter(f -> f.getType() == FollowUpType.COMPLAINT)
        .collect(Collectors.groupingBy(FollowUp::getCustomerId, Collectors.counting()));
    return customerRows.stream().map(c -> {
      BigDecimal contractAmount = sum(contractRows.stream()
          .filter(x -> x.getCustomerId().equals(c.getId()))
          .map(ServiceContract::getAmount).toList());
      BigDecimal received = sum(receivableRows.stream()
          .filter(x -> x.getCustomerId().equals(c.getId()))
          .map(Receivable::getSettledAmount).toList());
      List<WorkOrder> customerOrders = orderRows.stream()
          .filter(x -> c.getId().equals(x.getCustomerId())).toList();
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

  private List<EquipmentPerformance> equipmentPerformance(List<Customer> customerRows,
      List<WorkOrder> orderRows) {
    Map<UUID, Customer> customerMap = customerRows.stream()
        .collect(Collectors.toMap(Customer::getId, Function.identity()));
    return equipment.findAllByOrderByNextMaintenanceDateAsc().stream().map(asset -> {
      List<WorkOrder> history = orderRows.stream()
          .filter(o -> asset.getId().equals(o.getEquipmentId())).toList();
      BigDecimal cost = sum(history.stream().map(WorkOrder::getCostAmount).toList());
      return new EquipmentPerformance(asset.getId(), asset.getCode(), asset.getName(),
          customerMap.get(asset.getCustomerId()) == null ? null
              : customerMap.get(asset.getCustomerId()).getName(),
          history.size(),
          history.stream().filter(o -> o.getWorkType() == WorkOrderType.REPAIR).count(), cost,
          history.isEmpty() ? BigDecimal.ZERO
              : cost.divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP));
    }).sorted(Comparator.comparing(EquipmentPerformance::faultCount).reversed()).limit(20).toList();
  }

  private List<WorkforcePerformance> workforcePerformance(List<WorkOrder> orderRows) {
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
    BigDecimal in = BigDecimal.ZERO, out = BigDecimal.ZERO;
    for (AccountingEntry e : entries.findAllByOrderByAccountCodeAsc())
      if (e.getAccountCode().equals("1002")) {
        in = in.add(amount(e.getDebit()));
        out = out.add(amount(e.getCredit()));
      }
    return new FinancialCash(in, out);
  }

  private record FinancialCash(BigDecimal in, BigDecimal out) {
  }

  private BigDecimal sum(List<BigDecimal> values) {
    return values.stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal amount(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
