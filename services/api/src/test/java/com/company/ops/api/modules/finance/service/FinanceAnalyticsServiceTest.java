package com.company.ops.api.modules.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.AdjustTaxInvoiceRequest;
import com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.FinanceAnalyticsResponse;
import com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.FinanceScopeInfo;
import com.company.ops.api.modules.finance.service.FinanceOrganizationScopeService.Scope;
import com.company.ops.api.modules.finance.repository.PaymentApplicationRepository;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.governance.repository.BankStatementLineRepository;
import com.company.ops.api.modules.governance.repository.BusinessControlRecordRepository;
import com.company.ops.api.modules.ledger.repository.AccountingVoucherRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceAnalyticsServiceTest {
  @Mock private ReceivableRepository receivables;
  @Mock private ReceivableReceiptRepository receipts;
  @Mock private ServiceContractRepository contracts;
  @Mock private CustomerRepository customers;
  @Mock private ProcurementPayableRepository payables;
  @Mock private PaymentRecordRepository payments;
  @Mock private PaymentApplicationRepository applications;
  @Mock private SupplierInvoiceRepository supplierInvoices;
  @Mock private SupplierRepository suppliers;
  @Mock private AccountingVoucherRepository vouchers;
  @Mock private BankStatementLineRepository bankLines;
  @Mock private BusinessControlRecordRepository controls;
  @Mock private LedgerService ledgerService;
  @Mock private TaxFilingGuard taxFilingGuard;
  @Mock private FinanceOrganizationScopeService organizationScopeService;
  private FinanceAnalyticsService service;

  @BeforeEach
  void setUp() {
    service = new FinanceAnalyticsService(receivables, receipts, contracts, customers, payables,
        payments, applications, supplierInvoices, suppliers, vouchers, bankLines, controls,
        ledgerService, taxFilingGuard, organizationScopeService);
    when(receivables.findAll()).thenReturn(List.of());
    when(payables.findAll()).thenReturn(List.of());
    when(receipts.findAll()).thenReturn(List.of());
    when(payments.findAll()).thenReturn(List.of());
    when(vouchers.findAll()).thenReturn(List.of());
    when(bankLines.findAll()).thenReturn(List.of());
    when(supplierInvoices.findAll()).thenReturn(List.of());
    when(controls.findAll()).thenReturn(List.of());
    when(applications.findAll()).thenReturn(List.of());
    when(organizationScopeService.resolve(any(), anyBoolean())).thenReturn(new Scope(
        java.util.Set.of(), true,
        new FinanceScopeInfo(null, "全公司", "全部组织及未分摊数据", true, 0, true, false)));
    when(organizationScopeService.ownerNames(any())).thenReturn(java.util.Set.of());
  }

  @AfterEach
  void clearSecurity() { SecurityContextHolder.clearContext(); }

  @Test
  void calculatesAgingBoundariesForecastAndVatFromGrossAmounts() {
    LocalDate asOf = LocalDate.of(2026, 8, 4);
    UUID contractId = UUID.randomUUID();
    Receivable current = receivable("AR-1", asOf, "113", contractId);
    current.setInvoiceNo("OUT-1"); current.setInvoiceDate(asOf);
    Receivable day30 = receivable("AR-2", asOf.minusDays(30), "50", null);
    when(receivables.findAll()).thenReturn(List.of(current, day30));

    ProcurementPayable payable = new ProcurementPayable();
    payable.setAmount(new BigDecimal("80")); payable.setPaidAmount(BigDecimal.ZERO);
    payable.setDueDate(asOf.plusDays(7)); payable.setStatus(PayableStatus.PENDING);
    when(payables.findAll()).thenReturn(List.of(payable));

    ServiceContract contract = new ServiceContract();
    contract.setId(contractId); contract.setTaxRate(new BigDecimal("13"));
    when(contracts.findAllById(any())).thenReturn(List.of(contract));

    SupplierInvoice input = new SupplierInvoice();
    input.setInvoiceDate(asOf); input.setAmount(new BigDecimal("106"));
    input.setTaxRate(new BigDecimal("6")); input.setApprovalStatus("APPROVED");
    input.setVerificationStatus("VERIFIED");
    when(supplierInvoices.findAll()).thenReturn(List.of(input));

    FinanceAnalyticsResponse result = service.analytics(asOf, 2026, null, true);

    assertThat(result.aging()).filteredOn(item -> item.key().equals("CURRENT"))
        .singleElement().satisfies(item -> assertThat(item.receivable()).isEqualByComparingTo("113"));
    assertThat(result.aging()).filteredOn(item -> item.key().equals("D1_30"))
        .singleElement().satisfies(item -> assertThat(item.receivable()).isEqualByComparingTo("50"));
    assertThat(result.forecast()).filteredOn(item -> item.horizonDays() == 7)
        .singleElement().satisfies(item -> assertThat(item.net()).isEqualByComparingTo("83"));
    assertThat(result.tax().outputTax()).isEqualByComparingTo("13");
    assertThat(result.tax().inputTax()).isEqualByComparingTo("6");
    assertThat(result.tax().netTaxPayable()).isEqualByComparingTo("7");
  }

  @Test
  void limitsAnalyticsToTheSelectedOrganization() {
    LocalDate asOf = LocalDate.of(2026, 8, 4);
    UUID selectedOrganizationId = UUID.randomUUID();
    UUID otherOrganizationId = UUID.randomUUID();
    Receivable selected = receivable("AR-DEPT-A", asOf.minusDays(5), "120", null);
    selected.setId(UUID.randomUUID());
    selected.setOrganizationId(selectedOrganizationId);
    Receivable other = receivable("AR-DEPT-B", asOf.minusDays(5), "900", null);
    other.setId(UUID.randomUUID());
    other.setOrganizationId(otherOrganizationId);
    when(receivables.findAll()).thenReturn(List.of(selected, other));

    ProcurementPayable selectedPayable = payable(asOf.minusDays(5), "30", selectedOrganizationId);
    ProcurementPayable otherPayable = payable(asOf.minusDays(5), "700", otherOrganizationId);
    when(payables.findAll()).thenReturn(List.of(selectedPayable, otherPayable));
    Scope selectedScope = new Scope(java.util.Set.of(selectedOrganizationId), false,
        new FinanceScopeInfo(selectedOrganizationId, "销售一部", "总部 / 销售一部",
            false, 1, false, true));
    when(organizationScopeService.resolve(eq(selectedOrganizationId), eq(false)))
        .thenReturn(selectedScope);

    FinanceAnalyticsResponse result = service.analytics(asOf, 2026, selectedOrganizationId, false);

    assertThat(result.aging()).filteredOn(item -> item.key().equals("D1_30"))
        .singleElement().satisfies(item -> {
          assertThat(item.receivable()).isEqualByComparingTo("120");
          assertThat(item.payable()).isEqualByComparingTo("30");
          assertThat(item.receivableCount()).isEqualTo(1);
          assertThat(item.payableCount()).isEqualTo(1);
        });
    assertThat(result.scope().organizationId()).isEqualTo(selectedOrganizationId);
  }

  @Test
  void redFlushesOutputInvoiceAndReversesItsBusinessVoucher() {
    UUID id = UUID.randomUUID();
    Receivable invoice = receivable("AR-9", LocalDate.of(2026, 8, 1), "113", null);
    invoice.setId(id); invoice.setInvoiceNo("OUT-9"); invoice.setInvoiceDate(LocalDate.of(2026, 8, 1));
    when(receivables.findById(id)).thenReturn(java.util.Optional.of(invoice));
    when(receivables.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    authenticate();

    var result = service.adjustTaxInvoice("output", id,
        new AdjustTaxInvoiceRequest("RED_FLUSHED", LocalDate.of(2026, 8, 4), "客户合同金额调整"));

    verify(ledgerService).reverseBusinessVoucher("INVOICE", "AR-9", LocalDate.of(2026, 8, 4), "客户合同金额调整");
    assertThat(result.status()).isEqualTo("RED_FLUSHED");
    assertThat(invoice.getTaxAdjustedBy()).isEqualTo("财务测试员");
  }

  private Receivable receivable(String code, LocalDate dueDate, String amount, UUID contractId) {
    Receivable item = new Receivable();
    item.setCode(code); item.setCustomerId(UUID.randomUUID()); item.setContractId(contractId);
    item.setAmount(new BigDecimal(amount)); item.setSettledAmount(BigDecimal.ZERO);
    item.setDueDate(dueDate); item.setStatus(ReceivableStatus.PAYMENT_PENDING);
    return item;
  }

  private ProcurementPayable payable(LocalDate dueDate, String amount, UUID organizationId) {
    ProcurementPayable item = new ProcurementPayable();
    item.setId(UUID.randomUUID());
    item.setAmount(new BigDecimal(amount));
    item.setPaidAmount(BigDecimal.ZERO);
    item.setDueDate(dueDate);
    item.setStatus(PayableStatus.PENDING);
    item.setOrganizationId(organizationId);
    return item;
  }

  private void authenticate() {
    SystemUser user = new SystemUser(); user.setId(UUID.randomUUID()); user.setTenantId("default");
    user.setUsername("finance-test"); user.setDisplayName("财务测试员"); user.setPasswordHash("unused"); user.setEnabled(true);
    UserPrincipal principal = new UserPrincipal(user);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
