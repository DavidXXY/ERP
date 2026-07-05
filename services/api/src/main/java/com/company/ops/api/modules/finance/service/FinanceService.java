package com.company.ops.api.modules.finance.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.finance.domain.PaymentApplication;
import com.company.ops.api.modules.finance.domain.PaymentApplicationStatus;
import com.company.ops.api.modules.finance.domain.PaymentRecord;
import com.company.ops.api.modules.finance.dto.CreatePaymentApplicationRequest;
import com.company.ops.api.modules.finance.dto.ExecutePaymentRequest;
import com.company.ops.api.modules.finance.dto.FinanceOverviewResponse;
import com.company.ops.api.modules.finance.dto.FinancePayableResponse;
import com.company.ops.api.modules.finance.dto.PaymentApplicationResponse;
import com.company.ops.api.modules.finance.dto.PaymentRecordResponse;
import com.company.ops.api.modules.finance.dto.ProcessPaymentApplicationRequest;
import com.company.ops.api.modules.finance.repository.PaymentApplicationRepository;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.company.ops.api.common.util.MoneyUtils.amount;

@Service
public class FinanceService {

  private static final EnumSet<PaymentApplicationStatus> RESERVED_STATUSES = EnumSet.of(
      PaymentApplicationStatus.PENDING_APPROVAL,
      PaymentApplicationStatus.APPROVED
  );

  private CodeGenerator codeGenerator;
  private final ReceivableRepository receivableRepository;
  private final ProcurementPayableRepository payableRepository;
  private final SupplierRepository supplierRepository;
  private final PurchaseOrderRepository orderRepository;
  private final PaymentApplicationRepository applicationRepository;
  private final PaymentRecordRepository paymentRepository;
  private final LedgerService ledgerService;

  public FinanceService(ReceivableRepository receivableRepository,
      ProcurementPayableRepository payableRepository,
      SupplierRepository supplierRepository,
      PurchaseOrderRepository orderRepository,
      PaymentApplicationRepository applicationRepository,
      PaymentRecordRepository paymentRepository,
      LedgerService ledgerService,
                              CodeGenerator codeGenerator) {
    this.codeGenerator = codeGenerator;
    this.receivableRepository = receivableRepository;
    this.payableRepository = payableRepository;
    this.supplierRepository = supplierRepository;
    this.orderRepository = orderRepository;
    this.applicationRepository = applicationRepository;
    this.paymentRepository = paymentRepository;
    this.ledgerService = ledgerService;
  }

  @Transactional(readOnly = true)
  public FinanceOverviewResponse overview() {
    List<Receivable> receivables = receivableRepository.findAllByOrderByDueDateAsc();
    List<ProcurementPayable> payables = payableRepository.findAllByOrderByDueDateAsc();
    BigDecimal receivableAmount = receivables.stream().map(Receivable::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal receivedAmount = receivables.stream().map(Receivable::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal receivableOutstanding = receivableAmount.subtract(receivedAmount);
    BigDecimal receivableOverdue = receivables.stream()
        .filter(item -> item.getStatus() != ReceivableStatus.SETTLED)
        .filter(item -> item.getDueDate().isBefore(LocalDate.now()))
        .map(item -> amount(item.getAmount()).subtract(amount(item.getSettledAmount())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal payableAmount = payables.stream().map(ProcurementPayable::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal paidAmount = payables.stream().map(ProcurementPayable::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal payableOutstanding = payableAmount.subtract(paidAmount);
    BigDecimal payableOverdue = payables.stream()
        .filter(item -> item.getStatus() != PayableStatus.PAID && item.getStatus() != PayableStatus.CANCELLED)
        .filter(item -> item.getDueDate().isBefore(LocalDate.now()))
        .map(item -> amount(item.getAmount()).subtract(amount(item.getPaidAmount())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    long pendingApplications = applicationRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> item.getStatus() == PaymentApplicationStatus.PENDING_APPROVAL)
        .count();
    return new FinanceOverviewResponse(
        receivableAmount,
        receivedAmount,
        receivableOutstanding,
        receivableOverdue,
        payableAmount,
        paidAmount,
        payableOutstanding,
        payableOverdue,
        receivedAmount.subtract(paidAmount),
        pendingApplications
    );
  }

  @Transactional(readOnly = true)
  public List<FinancePayableResponse> listPayables() {
    List<ProcurementPayable> payables = payableRepository.findAllByOrderByDueDateAsc();
    Map<UUID, Supplier> suppliers = supplierMap(payables.stream().map(ProcurementPayable::getSupplierId).toList());
    Map<UUID, PurchaseOrder> orders = orderMap(payables.stream().map(ProcurementPayable::getOrderId).toList());
    Map<UUID, BigDecimal> reserved = applicationRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(item -> RESERVED_STATUSES.contains(item.getStatus()))
        .collect(Collectors.groupingBy(
            PaymentApplication::getPayableId,
            Collectors.reducing(BigDecimal.ZERO, PaymentApplication::getRequestedAmount, BigDecimal::add)
        ));
    return payables.stream().map(item -> toPayableResponse(
        item,
        suppliers.get(item.getSupplierId()),
        orders.get(item.getOrderId()),
        reserved.getOrDefault(item.getId(), BigDecimal.ZERO)
    )).toList();
  }

  @Transactional(readOnly = true)
  public List<PaymentApplicationResponse> listApplications() {
    List<PaymentApplication> applications = applicationRepository.findAllByOrderByCreatedAtDesc();
    Map<UUID, ProcurementPayable> payables = payableMap(applications.stream().map(PaymentApplication::getPayableId).toList());
    Map<UUID, Supplier> suppliers = supplierMap(applications.stream().map(PaymentApplication::getSupplierId).toList());
    Map<UUID, PaymentRecord> payments = paymentMap(applications.stream()
        .map(PaymentApplication::getPaymentId)
        .filter(id -> id != null)
        .toList());
    return applications.stream().map(item -> toApplicationResponse(
        item,
        payables.get(item.getPayableId()),
        suppliers.get(item.getSupplierId()),
        item.getPaymentId() == null ? null : payments.get(item.getPaymentId())
    )).toList();
  }

  @Transactional
  public PaymentApplicationResponse createApplication(CreatePaymentApplicationRequest request) {
    String appCode = request.code() != null ? request.code() : codeGenerator.generate("PAYMENT_APPLICATION");
    if (applicationRepository.existsByCode(appCode)) {
      throw new BusinessException("付款申请单号已存在");
    }
    ProcurementPayable payable = payableRepository.findByIdForUpdate(request.payableId())
        .orElseThrow(() -> new BusinessException("应付单不存在"));
    if (payable.getStatus() == PayableStatus.PAID || payable.getStatus() == PayableStatus.CANCELLED) {
      throw new BusinessException("当前应付单不能申请付款");
    }
    BigDecimal outstanding = amount(payable.getAmount()).subtract(amount(payable.getPaidAmount()));
    BigDecimal reserved = reservedAmount(payable.getId());
    BigDecimal available = outstanding.subtract(reserved);
    if (request.requestedAmount().compareTo(available) > 0) {
      throw new BusinessException("申请金额超过可申请金额" + available);
    }

    PaymentApplication application = new PaymentApplication();
    application.setCode(appCode);
    application.setPayableId(payable.getId());
    application.setSupplierId(payable.getSupplierId());
    application.setRequestedAmount(request.requestedAmount());
    application.setRequestedDate(request.requestedDate());
    application.setApplicantName(request.applicantName());
    application.setPurpose(request.purpose());
    application.setStatus(PaymentApplicationStatus.PENDING_APPROVAL);
    PaymentApplication saved = applicationRepository.save(application);
    Supplier supplier = supplierRepository.findById(saved.getSupplierId()).orElse(null);
    return toApplicationResponse(saved, payable, supplier, null);
  }

  @Transactional
  public PaymentApplicationResponse processApplication(
      UUID id,
      ProcessPaymentApplicationRequest request
  ) {
    PaymentApplication application = applicationRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("付款申请不存在"));
    if (application.getStatus() != PaymentApplicationStatus.PENDING_APPROVAL) {
      throw new BusinessException("该付款申请已处理");
    }
    if (request.decision() != PaymentApplicationStatus.APPROVED
        && request.decision() != PaymentApplicationStatus.REJECTED) {
      throw new BusinessException("审批结论只能选择通过或驳回");
    }
    application.setStatus(request.decision());
    application.setApprovalComment(request.comment());
    application.setApproverName(request.approverName());
    application.setApprovedAt(OffsetDateTime.now());
    PaymentApplication saved = applicationRepository.save(application);
    ProcurementPayable payable = payableRepository.findById(saved.getPayableId()).orElse(null);
    Supplier supplier = supplierRepository.findById(saved.getSupplierId()).orElse(null);
    return toApplicationResponse(saved, payable, supplier, null);
  }

  @Transactional
  public PaymentRecordResponse executePayment(UUID id, ExecutePaymentRequest request) {
    String paymentCode = request.paymentCode() != null ? request.paymentCode() : codeGenerator.generate("PAYMENT_RECORD");
    if (paymentRepository.existsByCode(paymentCode)) {
      throw new BusinessException("付款流水号已存在");
    }
    PaymentApplication application = applicationRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("付款申请不存在"));
    if (application.getStatus() != PaymentApplicationStatus.APPROVED) {
      throw new BusinessException("付款申请审批通过后才能付款");
    }
    ProcurementPayable payable = payableRepository.findByIdForUpdate(application.getPayableId())
        .orElseThrow(() -> new BusinessException("应付单不存在"));
    BigDecimal outstanding = amount(payable.getAmount()).subtract(amount(payable.getPaidAmount()));
    if (application.getRequestedAmount().compareTo(outstanding) > 0) {
      throw new BusinessException("付款金额超过应付余额");
    }

    PaymentRecord payment = new PaymentRecord();
    payment.setCode(paymentCode);
    payment.setApplicationId(application.getId());
    payment.setPayableId(payable.getId());
    payment.setSupplierId(application.getSupplierId());
    payment.setAmount(application.getRequestedAmount());
    payment.setPaidDate(request.paidDate());
    payment.setPaymentMethod(request.paymentMethod());
    payment.setBankReference(request.bankReference());
    payment.setPayerName(request.payerName());
    PaymentRecord savedPayment = paymentRepository.save(payment);

    BigDecimal paidAmount = amount(payable.getPaidAmount()).add(application.getRequestedAmount());
    payable.setPaidAmount(paidAmount);
    payable.setStatus(paidAmount.compareTo(payable.getAmount()) == 0
        ? PayableStatus.PAID
        : PayableStatus.PARTIAL_PAID);
    payableRepository.save(payable);
    application.setStatus(PaymentApplicationStatus.PAID);
    application.setPaymentId(savedPayment.getId());
    applicationRepository.save(application);
    ledgerService.post("PAYMENT", savedPayment.getCode(), savedPayment.getPaidDate(),
        "支付供应商货款 " + savedPayment.getCode(), List.of(
            new PostingLine("2202", "应付账款", savedPayment.getAmount(), BigDecimal.ZERO, payable.getCode()),
            new PostingLine("1002", "银行存款", BigDecimal.ZERO, savedPayment.getAmount(), request.bankReference())
        ));
    Supplier supplier = supplierRepository.findById(application.getSupplierId()).orElse(null);
    return toPaymentResponse(savedPayment, application, payable, supplier);
  }

  @Transactional(readOnly = true)
  public List<PaymentRecordResponse> listPayments() {
    List<PaymentRecord> payments = paymentRepository.findAllByOrderByPaidDateDescCreatedAtDesc();
    Map<UUID, PaymentApplication> applications = applicationRepository.findAllById(
        payments.stream().map(PaymentRecord::getApplicationId).distinct().toList()
    ).stream().collect(Collectors.toMap(PaymentApplication::getId, Function.identity()));
    Map<UUID, ProcurementPayable> payables = payableMap(payments.stream().map(PaymentRecord::getPayableId).toList());
    Map<UUID, Supplier> suppliers = supplierMap(payments.stream().map(PaymentRecord::getSupplierId).toList());
    return payments.stream().map(item -> toPaymentResponse(
        item,
        applications.get(item.getApplicationId()),
        payables.get(item.getPayableId()),
        suppliers.get(item.getSupplierId())
    )).toList();
  }

  private BigDecimal reservedAmount(UUID payableId) {
    return applicationRepository.findByPayableIdAndStatusIn(payableId, RESERVED_STATUSES).stream()
        .map(PaymentApplication::getRequestedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private FinancePayableResponse toPayableResponse(
      ProcurementPayable payable,
      Supplier supplier,
      PurchaseOrder order,
      BigDecimal reserved
  ) {
    BigDecimal outstanding = amount(payable.getAmount()).subtract(amount(payable.getPaidAmount()));
    return new FinancePayableResponse(
        payable.getId(),
        payable.getCode(),
        payable.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        payable.getOrderId(),
        order == null ? null : order.getCode(),
        amount(payable.getAmount()),
        amount(payable.getPaidAmount()),
        outstanding,
        reserved,
        outstanding.subtract(reserved),
        payable.getDueDate(),
        payable.getStatus(),
        payable.getStatus() != PayableStatus.PAID
            && payable.getStatus() != PayableStatus.CANCELLED
            && payable.getDueDate().isBefore(LocalDate.now())
    );
  }

  private PaymentApplicationResponse toApplicationResponse(
      PaymentApplication application,
      ProcurementPayable payable,
      Supplier supplier,
      PaymentRecord payment
  ) {
    return new PaymentApplicationResponse(
        application.getId(),
        application.getCode(),
        application.getPayableId(),
        payable == null ? null : payable.getCode(),
        application.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        application.getRequestedAmount(),
        application.getRequestedDate(),
        application.getApplicantName(),
        application.getPurpose(),
        application.getStatus(),
        application.getApprovalComment(),
        application.getApproverName(),
        application.getApprovedAt(),
        application.getPaymentId(),
        payment == null ? null : payment.getCode()
    );
  }

  private PaymentRecordResponse toPaymentResponse(
      PaymentRecord payment,
      PaymentApplication application,
      ProcurementPayable payable,
      Supplier supplier
  ) {
    return new PaymentRecordResponse(
        payment.getId(),
        payment.getCode(),
        payment.getApplicationId(),
        application == null ? null : application.getCode(),
        payment.getPayableId(),
        payable == null ? null : payable.getCode(),
        payment.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        payment.getAmount(),
        payment.getPaidDate(),
        payment.getPaymentMethod(),
        payment.getBankReference(),
        payment.getPayerName()
    );
  }

  private Map<UUID, Supplier> supplierMap(List<UUID> ids) {
    if (ids.isEmpty()) return Map.of();
    return supplierRepository.findAllById(ids.stream().distinct().toList()).stream()
        .collect(Collectors.toMap(Supplier::getId, Function.identity()));
  }

  private Map<UUID, PurchaseOrder> orderMap(List<UUID> ids) {
    if (ids.isEmpty()) return Map.of();
    return orderRepository.findAllById(ids.stream().distinct().toList()).stream()
        .collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));
  }

  private Map<UUID, ProcurementPayable> payableMap(List<UUID> ids) {
    if (ids.isEmpty()) return Map.of();
    return payableRepository.findAllById(ids.stream().distinct().toList()).stream()
        .collect(Collectors.toMap(ProcurementPayable::getId, Function.identity()));
  }

  private Map<UUID, PaymentRecord> paymentMap(List<UUID> ids) {
    if (ids.isEmpty()) return Map.of();
    return paymentRepository.findAllById(ids.stream().distinct().toList()).stream()
        .collect(Collectors.toMap(PaymentRecord::getId, Function.identity()));
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
