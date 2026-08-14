package com.company.ops.api.modules.finance.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.ServiceContract;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivableResponse;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.finance.domain.PaymentApplication;
import com.company.ops.api.modules.finance.domain.PaymentApplicationPayable;
import com.company.ops.api.modules.finance.domain.PaymentApplicationStatus;
import com.company.ops.api.modules.finance.domain.PaymentRecord;
import com.company.ops.api.modules.finance.dto.BatchExecutePaymentRequest;
import com.company.ops.api.modules.finance.dto.BatchPaymentExecutionResult;
import com.company.ops.api.modules.finance.dto.BatchPaymentExecutionResult.BatchPaymentItemResult;
import com.company.ops.api.modules.finance.dto.CancelPayableRequest;
import com.company.ops.api.modules.finance.dto.CreatePayableAdjustmentRequest;
import com.company.ops.api.modules.finance.dto.CreatePaymentApplicationRequest;
import com.company.ops.api.modules.finance.dto.ExecutePaymentRequest;
import com.company.ops.api.modules.finance.dto.FinanceReceivableDetailResponse;
import com.company.ops.api.modules.finance.dto.FinanceReceivableDetailResponse.ContractInfo;
import com.company.ops.api.modules.finance.dto.FinanceReceivableDetailResponse.CustomerInvoiceInfo;
import com.company.ops.api.modules.finance.dto.FinanceOverviewResponse;
import com.company.ops.api.modules.finance.dto.FinancePayableResponse;
import com.company.ops.api.modules.finance.dto.PayableAdjustmentResponse;
import com.company.ops.api.modules.finance.dto.PaymentExecutionResult;
import com.company.ops.api.modules.finance.dto.PaymentApplicationResponse;
import com.company.ops.api.modules.finance.dto.PaymentRecordResponse;
import com.company.ops.api.modules.finance.dto.PaymentSplit;
import com.company.ops.api.modules.finance.dto.ProcessPaymentApplicationRequest;
import com.company.ops.api.modules.finance.repository.PaymentApplicationPayableRepository;
import com.company.ops.api.modules.finance.repository.PaymentApplicationRepository;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.PayableAdjustment;
import com.company.ops.api.modules.procurement.domain.PayableAdjustmentType;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice;
import com.company.ops.api.modules.procurement.repository.PayableAdjustmentRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoicePayableRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceRepository;
import com.company.ops.api.modules.procurement.service.SupplierPortalNotifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import com.company.ops.api.modules.system.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import static com.company.ops.api.common.util.MoneyUtils.amount;
import org.springframework.cache.annotation.Cacheable;

@Service
public class FinanceService {

  private static final EnumSet<PaymentApplicationStatus> RESERVED_STATUSES = EnumSet.of(
      PaymentApplicationStatus.PENDING_APPROVAL,
      PaymentApplicationStatus.APPROVED
  );

  private CodeGenerator codeGenerator;
  private final ReceivableRepository receivableRepository;
  private final CustomerRepository customerRepository;
  private final ServiceContractRepository contractRepository;
  private final ProcurementPayableRepository payableRepository;
  private final SupplierRepository supplierRepository;
  private final PurchaseOrderRepository orderRepository;
  private final PaymentApplicationRepository applicationRepository;
  private final PaymentApplicationPayableRepository applicationPayableRepository;
  private final PaymentRecordRepository paymentRepository;
  private final PayableAdjustmentRepository adjustmentRepository;
  private final SupplierInvoiceRepository supplierInvoiceRepository;
  private final SupplierInvoicePayableRepository supplierInvoicePayableRepository;
  private final LedgerService ledgerService;
  private final SupplierPortalNotifier portalNotifier;
  private final TransactionTemplate transactions;

  public FinanceService(ReceivableRepository receivableRepository,
      CustomerRepository customerRepository,
      ServiceContractRepository contractRepository,
      ProcurementPayableRepository payableRepository,
      SupplierRepository supplierRepository,
      PurchaseOrderRepository orderRepository,
      PaymentApplicationRepository applicationRepository,
      PaymentApplicationPayableRepository applicationPayableRepository,
      PaymentRecordRepository paymentRepository,
      PayableAdjustmentRepository adjustmentRepository,
      SupplierInvoiceRepository supplierInvoiceRepository,
      SupplierInvoicePayableRepository supplierInvoicePayableRepository,
      LedgerService ledgerService,
      SupplierPortalNotifier portalNotifier,
      PlatformTransactionManager transactionManager,
                              CodeGenerator codeGenerator) {
    this.transactions = new TransactionTemplate(transactionManager);
    this.codeGenerator = codeGenerator;
    this.receivableRepository = receivableRepository;
    this.customerRepository = customerRepository;
    this.contractRepository = contractRepository;
    this.payableRepository = payableRepository;
    this.supplierRepository = supplierRepository;
    this.orderRepository = orderRepository;
    this.applicationRepository = applicationRepository;
    this.applicationPayableRepository = applicationPayableRepository;
    this.paymentRepository = paymentRepository;
    this.adjustmentRepository = adjustmentRepository;
    this.supplierInvoiceRepository = supplierInvoiceRepository;
    this.supplierInvoicePayableRepository = supplierInvoicePayableRepository;
    this.ledgerService = ledgerService;
    this.portalNotifier = portalNotifier;
  }

  @Transactional(readOnly = true)
  @Cacheable("financeOverview")
  public FinanceOverviewResponse overview() {
    var receivableTotals = receivableRepository.aggregateFinanceOverview(LocalDate.now());
    var payableTotals = payableRepository.aggregateFinanceOverview(
        LocalDate.now(), PayableStatus.PAID, PayableStatus.CANCELLED);
    BigDecimal receivableAmount = amount(receivableTotals.getTotalAmount());
    BigDecimal receivedAmount = amount(receivableTotals.getSettledAmount());
    BigDecimal receivableOutstanding = receivableAmount.subtract(receivedAmount);
    BigDecimal receivableOverdue = amount(receivableTotals.getOverdueAmount());
    BigDecimal payableAmount = amount(payableTotals.getTotalAmount());
    BigDecimal paidAmount = amount(payableTotals.getPaidAmount());
    BigDecimal payableOutstanding = payableAmount.subtract(paidAmount);
    BigDecimal payableOverdue = amount(payableTotals.getOverdueAmount());
    long pendingApplications = applicationRepository.countByStatus(PaymentApplicationStatus.PENDING_APPROVAL);
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
  public Page<FinancePayableResponse> listPayables(Pageable pageable) {
    Page<ProcurementPayable> payables = payableRepository.findAllByOrderByDueDateAsc(pageable);
    Map<UUID, Supplier> suppliers = supplierMap(payables.getContent().stream().map(ProcurementPayable::getSupplierId).toList());
    Map<UUID, PurchaseOrder> orders = orderMap(payables.getContent().stream().map(ProcurementPayable::getOrderId).toList());
    Map<UUID, BigDecimal> reservedTotal = payables.isEmpty() ? Map.of() : applicationPayableRepository
        .aggregateReservedByPayableIdIn(
            payables.getContent().stream().map(ProcurementPayable::getId).toList(), RESERVED_STATUSES).stream()
        .collect(Collectors.toMap(row -> (UUID) row[0], row -> amount((BigDecimal) row[1])));
    return payables.map(item -> toPayableResponse(
        item,
        suppliers.get(item.getSupplierId()),
        orders.get(item.getOrderId()),
        reservedTotal.getOrDefault(item.getId(), BigDecimal.ZERO)
    ));
  }

  @Transactional(readOnly = true)
  public FinanceReceivableDetailResponse getReceivableDetail(UUID id) {
    Receivable receivable = receivableRepository.findById(id)
        .orElseThrow(() -> new BusinessException("应收单不存在"));
    Customer customer = receivable.getCustomerId() == null
        ? null
        : customerRepository.findById(receivable.getCustomerId()).orElse(null);
    ServiceContract contract = receivable.getContractId() == null
        ? null
        : contractRepository.findById(receivable.getContractId()).orElse(null);
    return new FinanceReceivableDetailResponse(
        toReceivableResponse(receivable, customer, contract),
        toCustomerInvoiceInfo(receivable, customer),
        toContractInfo(receivable, contract)
    );
  }

  @Transactional(readOnly = true)
  public Page<PaymentApplicationResponse> listApplications(Pageable pageable) {
    Page<PaymentApplication> applications = applicationRepository.findAllByOrderByCreatedAtDesc(pageable);
    Map<UUID, ProcurementPayable> payables = payableMap(applications.getContent().stream().map(PaymentApplication::getPayableId).toList());
    Map<UUID, Supplier> suppliers = supplierMap(applications.getContent().stream().map(PaymentApplication::getSupplierId).toList());
    Map<UUID, PaymentRecord> payments = paymentMap(applications.getContent().stream()
        .map(PaymentApplication::getPaymentId)
        .filter(id -> id != null)
        .toList());
    return applications.map(item -> toApplicationResponse(
        item,
        payables.get(item.getPayableId()),
        suppliers.get(item.getSupplierId()),
        item.getPaymentId() == null ? null : payments.get(item.getPaymentId())
    ));
  }

  @Transactional
  public PaymentApplicationResponse createApplication(CreatePaymentApplicationRequest request) {
    String appCode = request.code() != null && !request.code().isBlank()
        ? request.code().trim()
        : codeGenerator.generate("PAYMENT_APPLICATION");
    if (applicationRepository.existsByCode(appCode)) {
      throw new BusinessException("付款申请单号已存在");
    }
    Set<UUID> selected = new LinkedHashSet<>();
    selected.add(request.payableId());
    if (request.payableIds() != null) {
      selected.addAll(request.payableIds());
    }
    List<ProcurementPayable> payables = payableRepository.findAllById(selected);
    if (payables.size() != selected.size()) {
      throw new BusinessException("部分应付单不存在");
    }
    Map<UUID, ProcurementPayable> payableById = payables.stream()
        .collect(Collectors.toMap(ProcurementPayable::getId, Function.identity()));
    Supplier supplier = null;
    BigDecimal totalAvailable = BigDecimal.ZERO;
    Map<UUID, BigDecimal> availableById = new LinkedHashMap<>();
    for (UUID payableId : selected) {
      ProcurementPayable payable = payableById.get(payableId);
      Supplier current = supplierRepository.findById(payable.getSupplierId()).orElse(null);
      if (supplier == null) {
        supplier = current;
      } else if (current != null && !supplier.getId().equals(current.getId())) {
        throw new BusinessException("合并付款只能选择同一供应商的应付单");
      }
      BigDecimal available = payableAvailable(payable);
      availableById.put(payableId, available);
      totalAvailable = totalAvailable.add(available);
    }
    if (request.requestedAmount().compareTo(totalAvailable) > 0) {
      throw new BusinessException("申请金额超过可申请金额 " + totalAvailable);
    }
    Map<UUID, BigDecimal> allocations = allocateAmounts(
        new ArrayList<>(selected), availableById, request.requestedAmount());

    PaymentApplication application = new PaymentApplication();
    application.setCode(appCode);
    application.setPayableId(request.payableId());
    application.setSupplierId(supplier == null ? null : supplier.getId());
    application.setRequestedAmount(request.requestedAmount());
    application.setRequestedDate(request.requestedDate());
    application.setApplicantName(currentName());
    application.setApplicantUserId(currentUserId());
    application.setPurpose(request.purpose());
    application.setStatus(PaymentApplicationStatus.PENDING_APPROVAL);
    PaymentApplication saved = applicationRepository.save(application);
    for (UUID payableId : selected) {
      PaymentApplicationPayable link = new PaymentApplicationPayable();
      link.setApplicationId(saved.getId());
      link.setPayableId(payableId);
      link.setAllocatedAmount(allocations.get(payableId));
      applicationPayableRepository.save(link);
    }
    return toApplicationResponse(saved, payableById.get(request.payableId()), supplier, null);
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
    if (sameActor(application.getApplicantUserId(), application.getApplicantName())) {
      throw new BusinessException("付款申请人与审批人必须分离");
    }
    application.setStatus(request.decision());
    application.setApprovalComment(request.comment());
    application.setApproverName(currentName());
    application.setApproverUserId(currentUserId());
    application.setApprovedAt(OffsetDateTime.now());
    PaymentApplication saved = applicationRepository.save(application);
    ProcurementPayable payable = payableRepository.findById(saved.getPayableId()).orElse(null);
    Supplier supplier = supplierRepository.findById(saved.getSupplierId()).orElse(null);
    return toApplicationResponse(saved, payable, supplier, null);
  }

  @Transactional
  public PaymentExecutionResult executePayment(UUID id, ExecutePaymentRequest request) {
    PaymentApplication application = applicationRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("付款申请不存在"));
    if (application.getStatus() != PaymentApplicationStatus.APPROVED) {
      throw new BusinessException("付款申请审批通过后才能付款");
    }
    if (sameActor(application.getApplicantUserId(), application.getApplicantName())
        || sameActor(application.getApproverUserId(), application.getApproverName())) {
      throw new BusinessException("付款申请、审批和执行必须由不同人员完成");
    }
    List<PaymentSplit> splits = request.payments();
    if (splits == null || splits.isEmpty()) {
      throw new BusinessException("请至少填写一笔付款");
    }
    BigDecimal total = splits.stream()
        .map(PaymentSplit::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(application.getRequestedAmount()) > 0) {
      throw new BusinessException("实际付款金额不能超过审批通过的申请金额");
    }
    Map<UUID, PaymentApplicationPayable> links = applicationPayableRepository
        .findByApplicationId(application.getId()).stream()
        .collect(Collectors.toMap(PaymentApplicationPayable::getPayableId, Function.identity()));
    Map<UUID, BigDecimal> perPayable = new LinkedHashMap<>();
    for (PaymentSplit split : splits) {
      UUID payableId = split.payableId() != null ? split.payableId() : application.getPayableId();
      if (!links.isEmpty() && !links.containsKey(payableId)) {
        throw new BusinessException("该付款申请未包含应付单 " + payableId);
      }
      if (links.isEmpty() && !payableId.equals(application.getPayableId())) {
        throw new BusinessException("该付款申请未包含应付单 " + payableId);
      }
      perPayable.merge(payableId, split.amount(), BigDecimal::add);
    }
    Map<UUID, ProcurementPayable> payableById = new LinkedHashMap<>();
    for (UUID payableId : perPayable.keySet()) {
      ProcurementPayable payable = payableRepository.findByIdForUpdate(payableId)
          .orElseThrow(() -> new BusinessException("应付单不存在"));
      payableById.put(payableId, payable);
      BigDecimal allocated = links.isEmpty()
          ? application.getRequestedAmount()
          : links.get(payableId).getAllocatedAmount();
      if (perPayable.get(payableId).compareTo(allocated) > 0) {
        throw new BusinessException("应付单 " + payable.getCode()
            + " 的付款金额超过本申请分配额度 " + allocated);
      }
      BigDecimal effective = amount(payable.getAmount()).subtract(amount(payable.getAdjustedAmount()));
      BigDecimal outstanding = effective.subtract(amount(payable.getPaidAmount()));
      if (perPayable.get(payableId).compareTo(outstanding) > 0) {
        throw new BusinessException("应付单 " + payable.getCode() + " 付款金额超过应付余额 " + outstanding);
      }
    }

    String baseCode = request.paymentCode() != null && !request.paymentCode().isBlank()
        ? request.paymentCode().trim()
        : codeGenerator.generate("PAYMENT_RECORD");
    List<PaymentRecordResponse> records = new ArrayList<>();
    int index = 1;
    for (PaymentSplit split : splits) {
      UUID payableId = split.payableId() != null ? split.payableId() : application.getPayableId();
      ProcurementPayable payable = payableById.get(payableId);
      String recordCode = splits.size() == 1 ? baseCode : baseCode + "-" + index++;
      if (paymentRepository.existsByCode(recordCode)) {
        throw new BusinessException("付款流水号已存在");
      }
      PaymentRecord payment = new PaymentRecord();
      payment.setCode(recordCode);
      payment.setApplicationId(application.getId());
      payment.setPayableId(payableId);
      payment.setSupplierId(application.getSupplierId());
      payment.setAmount(split.amount());
      payment.setPaidDate(split.paidDate());
      payment.setPaymentMethod(split.paymentMethod());
      payment.setBankReference(split.bankReference());
      payment.setPayerName(currentName());
      payment.setPayerUserId(currentUserId());
      payment.setSourceType("APPLICATION");
      payment.setNote(split.note());
      PaymentRecord savedPayment = paymentRepository.save(payment);

      BigDecimal paidAmount = amount(payable.getPaidAmount()).add(split.amount());
      payable.setPaidAmount(paidAmount);
      BigDecimal effective = amount(payable.getAmount()).subtract(amount(payable.getAdjustedAmount()));
      payable.setStatus(paidAmount.compareTo(effective) >= 0
          ? PayableStatus.PAID
          : PayableStatus.PARTIAL_PAID);
      payableRepository.save(payable);
      ledgerService.post("PAYMENT", savedPayment.getCode(), savedPayment.getPaidDate(),
          "支付供应商货款 " + savedPayment.getCode(), List.of(
              new PostingLine("2202", "应付账款", savedPayment.getAmount(), BigDecimal.ZERO, payable.getCode()),
              new PostingLine("1002", "银行存款", BigDecimal.ZERO, savedPayment.getAmount(), split.bankReference())
          ));
      Supplier supplier = supplierRepository.findById(application.getSupplierId()).orElse(null);
      records.add(toPaymentResponse(savedPayment, application, payable, supplier));
    }
    application.setStatus(PaymentApplicationStatus.PAID);
    application.setPaymentId(records.isEmpty() ? null : records.get(0).id());
    applicationRepository.save(application);
    portalNotifier.notify(application.getSupplierId(), "PAYABLE",
        "货款已付款到账",
        "付款申请 " + application.getCode() + " 已执行付款 "
            + total.stripTrailingZeros().toPlainString()
            + " 元（付款单 " + baseCode + "），可在门户开票与对账页查看。",
        "PAYABLE", application.getPayableId());
    return new PaymentExecutionResult(baseCode, total, records);
  }

  public BatchPaymentExecutionResult executePaymentsBatch(BatchExecutePaymentRequest request) {
    if (request.items() == null || request.items().isEmpty()) {
      throw new BusinessException("请至少选择一笔待付款申请");
    }
    int successCount = 0;
    List<BatchPaymentItemResult> items = new ArrayList<>();
    for (BatchExecutePaymentRequest.Item item : request.items()) {
      try {
        PaymentExecutionResult result = transactions.execute(
            status -> executePayment(item.applicationId(), item.payment()));
        successCount++;
        items.add(new BatchPaymentItemResult(
            item.applicationId(), true, result.paymentCode(), result.totalAmount(), null));
      } catch (RuntimeException e) {
        String reason = e instanceof BusinessException
            ? e.getMessage()
            : "付款执行失败：" + e.getMessage();
        items.add(new BatchPaymentItemResult(item.applicationId(), false, null, null, reason));
      }
    }
    return new BatchPaymentExecutionResult(successCount, items.size() - successCount, items);
  }

  @Transactional(readOnly = true)
  public Page<PaymentRecordResponse> listPayments(Pageable pageable) {
    Page<PaymentRecord> payments = paymentRepository.findAllByOrderByPaidDateDescCreatedAtDesc(pageable);
    Map<UUID, PaymentApplication> applications = applicationRepository.findAllById(
        payments.getContent().stream().map(PaymentRecord::getApplicationId).distinct().toList()
    ).stream().collect(Collectors.toMap(PaymentApplication::getId, Function.identity()));
    Map<UUID, ProcurementPayable> payables = payableMap(payments.getContent().stream().map(PaymentRecord::getPayableId).toList());
    Map<UUID, Supplier> suppliers = supplierMap(payments.getContent().stream().map(PaymentRecord::getSupplierId).toList());
    return payments.map(item -> toPaymentResponse(
        item,
        applications.get(item.getApplicationId()),
        payables.get(item.getPayableId()),
        suppliers.get(item.getSupplierId())
    ));
  }

  private BigDecimal reservedAmount(UUID payableId) {
    return amount(applicationPayableRepository
        .aggregateReservedByPayableIdIn(List.of(payableId), RESERVED_STATUSES).stream()
        .map(row -> (BigDecimal) row[1])
        .findFirst().orElse(BigDecimal.ZERO));
  }

  @Transactional
  public FinancePayableResponse cancelPayable(UUID payableId, CancelPayableRequest request) {
    ProcurementPayable payable = payableRepository.findByIdForUpdate(payableId)
        .orElseThrow(() -> new BusinessException("应付单不存在"));
    if (payable.getStatus() == PayableStatus.PAID || payable.getStatus() == PayableStatus.CANCELLED) {
      throw new BusinessException("已付款或已取消的应付单不能作废");
    }
    if (amount(payable.getPaidAmount()).signum() > 0) {
      throw new BusinessException("已部分付款的应付单不能作废，请先处理退款");
    }
    List<PaymentApplication> openApplications = applicationRepository
        .findByPayableIdAndStatusIn(payableId, RESERVED_STATUSES);
    if (!openApplications.isEmpty()) {
      throw new BusinessException("存在未结付款申请，不能作废应付单");
    }
    List<SupplierInvoice> invoices = supplierInvoiceRepository.findByPayableId(payableId);
    if (invoices.stream().anyMatch(item -> !"REJECTED".equals(item.getApprovalStatus()))) {
      throw new BusinessException("已登记发票的应付单不能作废，请先处理发票");
    }
    BigDecimal effective = amount(payable.getAmount()).subtract(amount(payable.getAdjustedAmount()));
    if (effective.signum() > 0) {
      PayableAdjustment adjustment = new PayableAdjustment();
      adjustment.setCode(adjustmentCode());
      adjustment.setPayableId(payable.getId());
      adjustment.setOrderId(payable.getOrderId());
      adjustment.setSupplierId(payable.getSupplierId());
      adjustment.setAdjustmentType(PayableAdjustmentType.CANCELLATION);
      adjustment.setAmount(effective);
      adjustment.setReason(request.reason());
      adjustment.setOperatorName(currentName());
      adjustment.setAppliedAt(LocalDate.now());
      adjustment.setSource("CANCELLATION");
      adjustmentRepository.save(adjustment);
      payable.setAdjustedAmount(amount(payable.getAdjustedAmount()).add(effective));
    }
    payable.setStatus(PayableStatus.CANCELLED);
    payable.setCancelReason(request.reason());
    payable.setCancelledBy(currentName());
    payable.setCancelledAt(LocalDate.now());
    ProcurementPayable saved = payableRepository.save(payable);
    Supplier supplier = supplierRepository.findById(saved.getSupplierId()).orElse(null);
    PurchaseOrder order = orderRepository.findById(saved.getOrderId()).orElse(null);
    portalNotifier.notify(saved.getSupplierId(), "PAYABLE",
        "应付单已作废",
        "应付单 " + saved.getCode() + " 已作废" + (isBlank(request.reason()) ? "。"
            : "，原因：" + request.reason()),
        "PAYABLE", saved.getId());
    return toPayableResponse(saved, supplier, order, BigDecimal.ZERO);
  }

  @Transactional
  public PayableAdjustmentResponse applyPayableAdjustment(
      UUID payableId,
      CreatePayableAdjustmentRequest request
  ) {
    ProcurementPayable payable = payableRepository.findByIdForUpdate(payableId)
        .orElseThrow(() -> new BusinessException("应付单不存在"));
    if (payable.getStatus() == PayableStatus.PAID || payable.getStatus() == PayableStatus.CANCELLED) {
      throw new BusinessException("已付款或已取消的应付单不能调整");
    }
    BigDecimal effective = amount(payable.getAmount()).subtract(amount(payable.getAdjustedAmount()));
    BigDecimal outstanding = effective.subtract(amount(payable.getPaidAmount()));
    if (request.amount().compareTo(outstanding) > 0) {
      throw new BusinessException("冲减金额不能超过待付金额 " + outstanding);
    }
    LocalDate appliedAt = request.appliedAt() == null ? LocalDate.now() : request.appliedAt();
    PayableAdjustment adjustment = new PayableAdjustment();
    adjustment.setCode(adjustmentCode());
    adjustment.setPayableId(payable.getId());
    adjustment.setOrderId(payable.getOrderId());
    adjustment.setSupplierId(payable.getSupplierId());
    adjustment.setAdjustmentType(request.adjustmentType());
    adjustment.setAmount(request.amount());
    adjustment.setReason(request.reason());
    adjustment.setOperatorName(currentName());
    adjustment.setAppliedAt(appliedAt);
    adjustment.setSource("MANUAL");
    adjustmentRepository.save(adjustment);
    payable.setAdjustedAmount(amount(payable.getAdjustedAmount()).add(request.amount()));
    BigDecimal newEffective = amount(payable.getAmount()).subtract(amount(payable.getAdjustedAmount()));
    if (newEffective.signum() == 0 && amount(payable.getPaidAmount()).signum() == 0) {
      payable.setStatus(PayableStatus.CANCELLED);
      payable.setCancelReason(request.reason());
      payable.setCancelledBy(currentName());
      payable.setCancelledAt(appliedAt);
    }
    ProcurementPayable saved = payableRepository.save(payable);
    if (request.adjustmentType() == PayableAdjustmentType.CLAIM) {
      ledgerService.post("PAYABLE_ADJUSTMENT", adjustment.getCode(), appliedAt,
          "供应商索赔冲减应付 " + adjustment.getCode(), List.of(
              new PostingLine("2202", "应付账款", request.amount(), BigDecimal.ZERO, payable.getCode()),
              new PostingLine("6111", "其他业务收入", BigDecimal.ZERO, request.amount(), adjustment.getCode())));
    } else {
      ledgerService.post("PAYABLE_ADJUSTMENT", adjustment.getCode(), appliedAt,
          "采购应付冲减 " + adjustment.getCode(), List.of(
              new PostingLine("2202", "应付账款", request.amount(), BigDecimal.ZERO, payable.getCode()),
              new PostingLine("1405", "库存商品", BigDecimal.ZERO, request.amount(), adjustment.getCode())));
    }
    portalNotifier.notify(saved.getSupplierId(), "PAYABLE",
        "应付已冲减",
        "应付单 " + saved.getCode() + " 冲减 "
            + request.amount().stripTrailingZeros().toPlainString() + " 元"
            + (isBlank(request.reason()) ? "。" : "，原因：" + request.reason()),
        "PAYABLE", saved.getId());
    return toAdjustmentResponse(adjustment);
  }

  @Transactional(readOnly = true)
  public List<PayableAdjustmentResponse> listPayableAdjustments(UUID payableId) {
    return adjustmentRepository.findByPayableIdOrderByAppliedAtAscCreatedAtAsc(payableId).stream()
        .map(this::toAdjustmentResponse)
        .toList();
  }

  private ReceivableResponse toReceivableResponse(Receivable receivable, Customer customer, ServiceContract contract) {
    return new ReceivableResponse(
        receivable.getId(),
        receivable.getCustomerId(),
        customer == null ? null : customer.getName(),
        receivable.getContractId(),
        contract == null ? receivable.getSourceNo() : contract.getCode(),
        contract == null ? "未关联合同" : contract.getProjectName(),
        customer == null || customer.getOwnerName() == null ? "" : customer.getOwnerName(),
        receivable.getCode(),
        receivable.getSourceNo(),
        amount(receivable.getAmount()),
        receivable.getDueDate(),
        receivable.getInvoiceNo(),
        receivable.getInvoiceDate(),
        receivable.isInvoiceRequested(),
        receivable.getInvoiceRequestedBy(),
        receivable.getInvoiceRequestedAt(),
        receivable.getInvoiceRequestRemark(),
        receivable.getInvoiceRequestStatus().name(),
        receivable.getInvoiceReviewedBy(),
        receivable.getInvoiceReviewedAt(),
        receivable.getInvoiceReviewComment(),
        amount(receivable.getSettledAmount()),
        outstandingAmount(receivable),
        receivable.getStatus()
    );
  }

  private CustomerInvoiceInfo toCustomerInvoiceInfo(Receivable receivable, Customer customer) {
    return new CustomerInvoiceInfo(
        receivable.getCustomerId(),
        customer == null ? null : customer.getCode(),
        customer == null ? null : customer.getName(),
        customer == null ? null : customer.getOwnerName(),
        customer == null ? null : customer.getInvoiceTitle(),
        customer == null ? null : customer.getTaxNo(),
        customer == null ? null : customer.getBankName(),
        customer == null ? null : customer.getBankAccount(),
        customer == null ? null : customer.getRegisteredAddress(),
        customer == null ? null : customer.getRegisteredPhone(),
        customer == null ? null : customer.getPaymentHabit()
    );
  }

  private ContractInfo toContractInfo(Receivable receivable, ServiceContract contract) {
    if (contract == null) {
      return null;
    }
    return new ContractInfo(
        contract.getId(),
        contract.getQuoteId(),
        contract.getCode(),
        contract.getProjectName(),
        contract.getContractType(),
        amount(contract.getAmount()),
        defaultTaxRate(contract.getTaxRate()),
        netAmount(contract.getAmount(), contract.getTaxRate()),
        contract.getStartDate(),
        contract.getEndDate(),
        contract.getServiceCycle(),
        contract.getStatus(),
        receivable.getStatus(),
        contract.getCreatedAt()
    );
  }

  private BigDecimal outstandingAmount(Receivable receivable) {
    return amount(receivable.getAmount()).subtract(amount(receivable.getSettledAmount()));
  }

  private BigDecimal defaultTaxRate(BigDecimal taxRate) {
    return taxRate == null ? BigDecimal.valueOf(13) : taxRate;
  }

  private BigDecimal netAmount(BigDecimal grossAmount, BigDecimal taxRate) {
    BigDecimal rate = defaultTaxRate(taxRate);
    return amount(grossAmount).divide(
        BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)),
        2,
        RoundingMode.HALF_UP
    );
  }

  private BigDecimal payableAvailable(ProcurementPayable payable) {
    if (payable.getStatus() == PayableStatus.PAID || payable.getStatus() == PayableStatus.CANCELLED) {
      return BigDecimal.ZERO;
    }
    BigDecimal effective = amount(payable.getAmount()).subtract(amount(payable.getAdjustedAmount()));
    BigDecimal outstanding = effective.subtract(amount(payable.getPaidAmount())).max(BigDecimal.ZERO);
    BigDecimal reserved = reservedAmount(payable.getId());
    BigDecimal matchedInvoiceAmount = matchedInvoiceAmount(payable);
    if (matchedInvoiceAmount.signum() == 0) {
      throw new BusinessException("应付单 " + payable.getCode()
          + " 尚无审核通过且验真的匹配发票，不能申请付款");
    }
    BigDecimal invoiceAvailable = matchedInvoiceAmount
        .subtract(amount(payable.getPaidAmount())).subtract(reserved).max(BigDecimal.ZERO);
    return outstanding.subtract(reserved).min(invoiceAvailable).max(BigDecimal.ZERO);
  }

  private BigDecimal matchedInvoiceAmount(ProcurementPayable payable) {
    List<SupplierInvoice> payableInvoices = supplierInvoiceRepository.findByPayableId(payable.getId());
    if (payableInvoices.isEmpty()) {
      payableInvoices = supplierInvoicePayableRepository.findByPayableId(payable.getId()).stream()
          .map(link -> supplierInvoiceRepository.findById(link.getInvoiceId()).orElse(null))
          .filter(java.util.Objects::nonNull)
          .toList();
    }
    var relevantInvoices = payableInvoices.isEmpty()
        ? supplierInvoiceRepository.findByOrderId(payable.getOrderId())
        : payableInvoices;
    return relevantInvoices.stream()
        .filter(item -> "MATCHED".equals(item.getMatchStatus()))
        .filter(item -> "APPROVED".equals(item.getApprovalStatus()))
        .filter(item -> "VERIFIED".equals(item.getVerificationStatus()))
        .map(item -> amount(item.getMatchedAmount()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private Map<UUID, BigDecimal> allocateAmounts(
      List<UUID> payableIds,
      Map<UUID, BigDecimal> availableById,
      BigDecimal requested
  ) {
    Map<UUID, BigDecimal> result = new LinkedHashMap<>();
    BigDecimal totalAvailable = availableById.values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (payableIds.size() == 1) {
      result.put(payableIds.get(0), requested);
      return result;
    }
    BigDecimal remaining = requested;
    for (int i = 0; i < payableIds.size(); i++) {
      UUID id = payableIds.get(i);
      if (i == payableIds.size() - 1) {
        result.put(id, remaining);
        break;
      }
      BigDecimal share = requested.multiply(availableById.get(id))
          .divide(totalAvailable, 2, RoundingMode.DOWN);
      result.put(id, share);
      remaining = remaining.subtract(share);
    }
    return result;
  }

  private String adjustmentCode() {
    String code = "YFTZ-" + System.currentTimeMillis();
    while (adjustmentRepository.existsByCode(code)) {
      code = "YFTZ-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
    return code;
  }

  private PayableAdjustmentResponse toAdjustmentResponse(PayableAdjustment adjustment) {
    return new PayableAdjustmentResponse(
        adjustment.getId(),
        adjustment.getCode(),
        adjustment.getPayableId(),
        adjustment.getOrderId(),
        adjustment.getSupplierId(),
        adjustment.getAdjustmentType(),
        adjustment.getAmount(),
        adjustment.getReason(),
        adjustment.getOperatorName(),
        adjustment.getAppliedAt(),
        adjustment.getStatus(),
        adjustment.getSource(),
        adjustment.getSourceId()
    );
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private FinancePayableResponse toPayableResponse(
      ProcurementPayable payable,
      Supplier supplier,
      PurchaseOrder order,
      BigDecimal reserved
  ) {
    BigDecimal effective = amount(payable.getAmount()).subtract(amount(payable.getAdjustedAmount()));
    BigDecimal outstanding = effective.subtract(amount(payable.getPaidAmount())).max(BigDecimal.ZERO);
    BigDecimal refund = amount(payable.getPaidAmount()).subtract(effective).max(BigDecimal.ZERO);
    return new FinancePayableResponse(
        payable.getId(),
        payable.getCode(),
        payable.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        payable.getOrderId(),
        order == null ? null : order.getCode(),
        payable.getHandlerName(),
        amount(payable.getAmount()),
        amount(payable.getAdjustedAmount()),
        effective,
        amount(payable.getPaidAmount()),
        outstanding,
        refund,
        reserved,
        outstanding.subtract(reserved).max(BigDecimal.ZERO),
        payable.getDueDate(),
        payable.getStatus(),
        payable.getStatus() != PayableStatus.PAID
            && payable.getStatus() != PayableStatus.CANCELLED
            && outstanding.signum() > 0
            && payable.getDueDate() != null
            && payable.getDueDate().isBefore(LocalDate.now())
    );
  }

  private PaymentApplicationResponse toApplicationResponse(
      PaymentApplication application,
      ProcurementPayable payable,
      Supplier supplier,
      PaymentRecord payment
  ) {
    List<UUID> payableIds = applicationPayableRepository
        .findByApplicationId(application.getId()).stream()
        .map(PaymentApplicationPayable::getPayableId)
        .toList();
    if (payableIds.isEmpty()) {
      payableIds = List.of(application.getPayableId());
    }
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
        payment == null ? null : payment.getCode(),
        payableIds
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
        payment.getPayerName(),
        payment.getSourceType(),
        payment.getNote()
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

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }

  private UUID currentUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.id() : null;
  }

  private boolean sameActor(UUID recordedId, String recordedName) {
    UUID currentId = currentUserId();
    if (recordedId != null && currentId != null) return recordedId.equals(currentId);
    return recordedName != null && recordedName.equals(currentName());
  }
}
