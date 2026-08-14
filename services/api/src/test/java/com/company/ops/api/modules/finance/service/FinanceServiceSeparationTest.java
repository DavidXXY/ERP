package com.company.ops.api.modules.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.finance.domain.PaymentApplication;
import com.company.ops.api.modules.finance.domain.PaymentApplicationStatus;
import com.company.ops.api.modules.finance.domain.PaymentMethod;
import com.company.ops.api.modules.finance.dto.BatchExecutePaymentRequest;
import com.company.ops.api.modules.finance.dto.ExecutePaymentRequest;
import com.company.ops.api.modules.finance.dto.PaymentSplit;
import com.company.ops.api.modules.finance.dto.ProcessPaymentApplicationRequest;
import com.company.ops.api.modules.finance.repository.PaymentApplicationRepository;
import com.company.ops.api.modules.finance.repository.PaymentApplicationPayableRepository;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.repository.PayableAdjustmentRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoicePayableRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.service.SupplierPortalNotifier;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class FinanceServiceSeparationTest {
  @Mock private ReceivableRepository receivables;
  @Mock private CustomerRepository customers;
  @Mock private ServiceContractRepository contracts;
  @Mock private ProcurementPayableRepository payables;
  @Mock private SupplierRepository suppliers;
  @Mock private PurchaseOrderRepository orders;
  @Mock private PaymentApplicationRepository applications;
  @Mock private PaymentApplicationPayableRepository applicationPayables;
  @Mock private PaymentRecordRepository payments;
  @Mock private PayableAdjustmentRepository adjustments;
  @Mock private SupplierInvoiceRepository invoices;
  @Mock private SupplierInvoicePayableRepository invoicePayables;
  @Mock private LedgerService ledger;
  @Mock private SupplierPortalNotifier portalNotifier;
  @Mock private CodeGenerator codes;
  @Mock private PlatformTransactionManager transactionManager;
  @InjectMocks private FinanceService service;

  @AfterEach
  void clearSecurity() { SecurityContextHolder.clearContext(); }

  @Test
  void applicantCannotApproveOwnPaymentApplication() {
    UUID userId = UUID.randomUUID();
    authenticate(userId, "申请人");
    PaymentApplication application = application(PaymentApplicationStatus.PENDING_APPROVAL);
    application.setApplicantUserId(userId); application.setApplicantName("申请人");
    when(applications.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.processApplication(application.getId(),
        new ProcessPaymentApplicationRequest(PaymentApplicationStatus.APPROVED, "同意", "申请人")))
        .isInstanceOf(BusinessException.class).hasMessageContaining("申请人与审批人必须分离");
  }

  @Test
  void approverCannotExecuteThePayment() {
    UUID approverId = UUID.randomUUID();
    authenticate(approverId, "审批人");
    PaymentApplication application = application(PaymentApplicationStatus.APPROVED);
    application.setApplicantUserId(UUID.randomUUID()); application.setApplicantName("申请人");
    application.setApproverUserId(approverId); application.setApproverName("审批人");
    when(applications.findByIdForUpdate(application.getId())).thenReturn(Optional.of(application));

    assertThatThrownBy(() -> service.executePayment(application.getId(), new ExecutePaymentRequest(
        "FK-001", List.of(new PaymentSplit(null, BigDecimal.ONE, LocalDate.now(),
            PaymentMethod.BANK_TRANSFER, "BANK-001", null)))))
        .isInstanceOf(BusinessException.class).hasMessageContaining("不同人员");
  }

  @Test
  void batchPaymentContinuesAfterPerItemFailure() {
    UUID executorId = UUID.randomUUID();
    authenticate(executorId, "出纳");
    PaymentApplication ok = application(PaymentApplicationStatus.APPROVED);
    ok.setApplicantUserId(UUID.randomUUID()); ok.setApplicantName("申请人");
    ok.setApproverUserId(UUID.randomUUID()); ok.setApproverName("审批人");
    ok.setPayableId(UUID.randomUUID());
    ok.setSupplierId(UUID.randomUUID());
    ok.setCode("FK-OK");
    UUID missingId = UUID.randomUUID();
    ProcurementPayable payable = new ProcurementPayable();
    payable.setId(ok.getPayableId());
    payable.setCode("YF-001");
    payable.setAmount(BigDecimal.TEN);
    payable.setStatus(PayableStatus.PARTIAL_PAID);
    Supplier supplier = new Supplier();
    supplier.setId(ok.getSupplierId());
    supplier.setName("测试供应商");
    when(applications.findByIdForUpdate(ok.getId())).thenReturn(Optional.of(ok));
    when(applications.findByIdForUpdate(missingId)).thenReturn(Optional.empty());
    when(applicationPayables.findByApplicationId(ok.getId())).thenReturn(List.of());
    when(payables.findByIdForUpdate(ok.getPayableId())).thenReturn(Optional.of(payable));
    when(payments.existsByCode(any())).thenReturn(false);
    when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(payables.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(applications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(suppliers.findById(ok.getSupplierId())).thenReturn(Optional.of(supplier));

    ExecutePaymentRequest payment = new ExecutePaymentRequest("FK-BATCH-001",
        List.of(new PaymentSplit(null, BigDecimal.ONE, LocalDate.now(),
            PaymentMethod.BANK_TRANSFER, "BANK-001", null)));
    var result = service.executePaymentsBatch(new BatchExecutePaymentRequest(List.of(
        new BatchExecutePaymentRequest.Item(ok.getId(), payment),
        new BatchExecutePaymentRequest.Item(missingId, payment)
    )));

    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.failedCount()).isEqualTo(1);
    assertThat(result.items()).anyMatch(item ->
        item.success() && "FK-BATCH-001".equals(item.paymentCode()));
    assertThat(result.items()).anyMatch(item ->
        !item.success() && item.errorMessage().contains("付款申请不存在"));
  }

  private PaymentApplication application(PaymentApplicationStatus status) {
    PaymentApplication item = new PaymentApplication();
    item.setId(UUID.randomUUID()); item.setStatus(status); item.setRequestedAmount(BigDecimal.TEN);
    return item;
  }

  private void authenticate(UUID id, String name) {
    UserPrincipal principal = mock(UserPrincipal.class);
    when(principal.id()).thenReturn(id);
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(principal);
    SecurityContext context = mock(SecurityContext.class);
    when(context.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(context);
  }
}
