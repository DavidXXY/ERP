package com.company.ops.api.modules.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableReceipt;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.finance.domain.PaymentRecord;
import com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.FinanceScopeInfo;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.finance.service.FinanceOrganizationScopeService.Scope;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceContributionServiceTest {
  @Mock private FinanceOrganizationScopeService organizationScopeService;
  @Mock private DataScopeService dataScopeService;
  @Mock private SystemUserRepository userRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectCostEntryRepository costRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private ReceivableRepository receivableRepository;
  @Mock private ReceivableReceiptRepository receiptRepository;
  @Mock private PurchaseOrderRepository orderRepository;
  @Mock private ProcurementPayableRepository payableRepository;
  @Mock private PaymentRecordRepository paymentRepository;
  private FinanceContributionService service;

  @BeforeEach
  void setUp() {
    service = new FinanceContributionService(organizationScopeService, dataScopeService,
        userRepository, projectRepository, costRepository, customerRepository,
        receivableRepository, receiptRepository, orderRepository, payableRepository,
        paymentRepository);
    when(projectRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
    when(receivableRepository.findAll()).thenReturn(List.of());
    when(receiptRepository.findAll()).thenReturn(List.of());
    when(paymentRepository.findAll()).thenReturn(List.of());
    when(customerRepository.findAllById(any())).thenReturn(List.of());
    when(userRepository.findAllById(any())).thenReturn(List.of());
  }

  @Test
  void calculatesProfitAndCashForOnlyTheSelectedDepartment() {
    LocalDate asOf = LocalDate.of(2026, 8, 5);
    UUID selectedOrganizationId = UUID.randomUUID();
    UUID otherOrganizationId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    UUID contractId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    Project selected = project(projectId, contractId, customerId, ownerId,
        selectedOrganizationId, "P-A", "1000");
    Project other = project(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), otherOrganizationId, "P-B", "9000");
    when(projectRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(selected, other));
    when(organizationScopeService.resolve(eq(selectedOrganizationId), eq(false)))
        .thenReturn(scope(selectedOrganizationId));

    ProjectCostEntry cost = new ProjectCostEntry();
    cost.setProjectId(projectId);
    cost.setCategory(ProjectCostCategory.MATERIAL);
    cost.setSourceType(ProjectCostSource.MANUAL);
    cost.setDescription("项目材料成本");
    cost.setAmount(new BigDecimal("400"));
    cost.setIncurredDate(asOf.minusDays(1));
    when(costRepository.findByProjectIdIn(any())).thenReturn(List.of(cost));

    UUID receivableId = UUID.randomUUID();
    Receivable receivable = new Receivable();
    receivable.setId(receivableId);
    receivable.setContractId(contractId);
    receivable.setOrganizationId(selectedOrganizationId);
    receivable.setSalesOwnerUserId(ownerId);
    receivable.setAmount(new BigDecimal("1000"));
    receivable.setSettledAmount(new BigDecimal("600"));
    receivable.setDueDate(asOf.plusDays(10));
    receivable.setStatus(ReceivableStatus.PAYMENT_PENDING);
    Receivable otherReceivable = new Receivable();
    otherReceivable.setId(UUID.randomUUID());
    otherReceivable.setOrganizationId(otherOrganizationId);
    otherReceivable.setSalesOwnerUserId(UUID.randomUUID());
    otherReceivable.setAmount(new BigDecimal("8000"));
    otherReceivable.setSettledAmount(BigDecimal.ZERO);
    when(receivableRepository.findByOrganizationIdIn(java.util.Set.of(selectedOrganizationId))).thenReturn(List.of(receivable));

    ReceivableReceipt receipt = new ReceivableReceipt();
    receipt.setId(UUID.randomUUID());
    receipt.setReceivableId(receivableId);
    receipt.setAmount(new BigDecimal("600"));
    receipt.setReceivedDate(LocalDate.of(2026, 8, 2));
    when(receiptRepository.findByReceivableIdIn(java.util.Set.of(receivableId))).thenReturn(List.of(receipt));

    UUID orderId = UUID.randomUUID();
    PurchaseOrder order = new PurchaseOrder();
    order.setId(orderId);
    order.setProjectId(projectId);
    when(orderRepository.findByProjectIdIn(any())).thenReturn(List.of(order));
    UUID payableId = UUID.randomUUID();
    ProcurementPayable payable = new ProcurementPayable();
    payable.setId(payableId);
    payable.setOrderId(orderId);
    payable.setAmount(new BigDecimal("300"));
    payable.setPaidAmount(new BigDecimal("200"));
    payable.setStatus(PayableStatus.PARTIAL_PAID);
    when(payableRepository.findByOrderIdIn(any())).thenReturn(List.of(payable));
    PaymentRecord payment = new PaymentRecord();
    payment.setId(UUID.randomUUID());
    payment.setPayableId(payableId);
    payment.setAmount(new BigDecimal("200"));
    payment.setPaidDate(LocalDate.of(2026, 8, 3));
    when(paymentRepository.findByPayableIdIn(java.util.Set.of(payableId))).thenReturn(List.of(payment));

    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setName("示例客户");
    when(customerRepository.findAllById(any())).thenReturn(List.of(customer));
    SystemUser owner = new SystemUser();
    owner.setId(ownerId);
    owner.setDisplayName("销售甲");
    when(userRepository.findAllById(any())).thenReturn(List.of(owner));

    var result = service.analytics("ORGANIZATION", selectedOrganizationId,
        false, asOf, 2026);

    assertThat(result.summary().contractAmount()).isEqualByComparingTo("1000");
    assertThat(result.summary().actualCost()).isEqualByComparingTo("400");
    assertThat(result.summary().grossProfit()).isEqualByComparingTo("600");
    assertThat(result.summary().grossMarginRate()).isEqualByComparingTo("60");
    assertThat(result.summary().receivedAmount()).isEqualByComparingTo("600");
    assertThat(result.summary().paidAmount()).isEqualByComparingTo("200");
    assertThat(result.summary().netCashFlow()).isEqualByComparingTo("400");
    assertThat(result.summary().receivableOutstanding()).isEqualByComparingTo("400");
    assertThat(result.summary().payableOutstanding()).isEqualByComparingTo("100");
    assertThat(result.projects()).singleElement().satisfies(line -> {
      assertThat(line.projectCode()).isEqualTo("P-A");
      assertThat(line.customerName()).isEqualTo("示例客户");
      assertThat(line.salesOwnerName()).isEqualTo("销售甲");
    });
    assertThat(result.monthlyCashFlow()).filteredOn(item -> item.month() == 8)
        .singleElement().satisfies(item -> assertThat(item.netCash()).isEqualByComparingTo("400"));
  }

  @Test
  void rejectsARequestedSalespersonOutsideTheDataScope() {
    UUID userId = UUID.randomUUID();
    SystemUser user = new SystemUser();
    user.setId(userId);
    user.setDisplayName("无权销售");
    when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
    when(dataScopeService.canViewOwner(userId)).thenReturn(false);

    assertThatThrownBy(() -> service.analytics(
        "USER", userId, false, LocalDate.of(2026, 8, 5), 2026))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("无权查看");
  }

  private Scope scope(UUID organizationId) {
    return new Scope(Set.of(organizationId), false,
        new FinanceScopeInfo(organizationId, "销售部", "总部 / 销售部",
            false, 1, false, true));
  }

  private Project project(UUID id, UUID contractId, UUID customerId, UUID ownerId,
      UUID organizationId, String code, String amount) {
    Project project = new Project();
    project.setId(id);
    project.setContractId(contractId);
    project.setCustomerId(customerId);
    project.setSalesOwnerUserId(ownerId);
    project.setSalesOrganizationId(organizationId);
    project.setCode(code);
    project.setName("项目 " + code);
    project.setContractAmount(new BigDecimal(amount));
    project.setStage(ProjectStage.CONSTRUCTION);
    return project;
  }
}
