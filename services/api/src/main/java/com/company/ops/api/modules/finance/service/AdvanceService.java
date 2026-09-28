package com.company.ops.api.modules.finance.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;
import static com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.finance.domain.AdvancePayment;
import com.company.ops.api.modules.finance.domain.AdvanceReceipt;
import com.company.ops.api.modules.finance.dto.AdvanceDtos.*;
import com.company.ops.api.modules.finance.repository.AdvancePaymentRepository;
import com.company.ops.api.modules.finance.repository.AdvanceReceiptRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvanceService {

  private final AdvanceReceiptRepository receiptRepository;
  private final AdvancePaymentRepository paymentRepository;
  private final ReceivableRepository receivableRepository;
  private final ProcurementPayableRepository payableRepository;
  private final CustomerRepository customerRepository;
  private final SupplierRepository supplierRepository;
  private final LedgerService ledgerService;
  private final CodeGenerator codeGenerator;

  public AdvanceService(AdvanceReceiptRepository receiptRepository,
      AdvancePaymentRepository paymentRepository,
      ReceivableRepository receivableRepository,
      ProcurementPayableRepository payableRepository,
      CustomerRepository customerRepository,
      SupplierRepository supplierRepository,
      LedgerService ledgerService,
      CodeGenerator codeGenerator) {
    this.receiptRepository = receiptRepository;
    this.paymentRepository = paymentRepository;
    this.receivableRepository = receivableRepository;
    this.payableRepository = payableRepository;
    this.customerRepository = customerRepository;
    this.supplierRepository = supplierRepository;
    this.ledgerService = ledgerService;
    this.codeGenerator = codeGenerator;
  }

  @Transactional(readOnly = true)
  public Page<AdvanceReceiptResponse> listReceipts(Pageable pageable) {
    return receiptRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toReceiptResponse);
  }

  @Transactional
  public AdvanceReceiptResponse createReceipt(CreateAdvanceReceiptRequest request) {
    String code = request.code() != null && !request.code().isBlank()
        ? request.code().trim() : codeGenerator.generate("ADVANCE_RECEIPT");
    if (receiptRepository.existsByCode(code)) throw new BusinessException("预收款单号已存在");
    AdvanceReceipt receipt = new AdvanceReceipt();
    receipt.setCode(code);
    receipt.setCustomerId(request.customerId());
    receipt.setAmount(request.amount());
    receipt.setReceivedDate(request.receivedDate());
    receipt.setReferenceNo(request.referenceNo());
    receipt.setRemark(request.remark());
    AdvanceReceipt saved = receiptRepository.save(receipt);

    ledgerService.post("ADVANCE_RECEIPT", code, request.receivedDate(), "预收款登记 " + code,
        java.util.List.of(
            new PostingLine("1002", "银行存款", request.amount(), BigDecimal.ZERO, "预收款 " + code),
            new PostingLine("2203", "预收账款", BigDecimal.ZERO, request.amount(), "预收款 " + code)));
    return toReceiptResponse(saved);
  }

  @Transactional
  public AdvanceReceiptResponse applyReceipt(UUID receiptId, UUID receivableId, ApplyAdvanceRequest request) {
    AdvanceReceipt receipt = receiptRepository.findByIdForUpdate(receiptId)
        .orElseThrow(() -> new BusinessException("预收款不存在"));
    Receivable receivable = receivableRepository.findByIdForUpdate(receivableId)
        .orElseThrow(() -> new BusinessException("应收单不存在"));
    BigDecimal available = receipt.getAmount().subtract(receipt.getSettledAmount());
    if (request.amount().compareTo(available) > 0) throw new BusinessException("核销金额超过预收款可用余额" + available);
    BigDecimal outstanding = receivable.getAmount().subtract(receivable.getSettledAmount());
    if (request.amount().compareTo(outstanding) > 0) throw new BusinessException("核销金额超过应收未结金额" + outstanding);
    if (!receipt.getCustomerId().equals(receivable.getCustomerId())) {
      throw new BusinessException("预收款与应收单客户不一致");
    }

    receipt.setSettledAmount(receipt.getSettledAmount().add(request.amount()));
    receipt.setStatus(receipt.getSettledAmount().compareTo(receipt.getAmount()) >= 0 ? "SETTLED" : "PARTIAL");
    receiptRepository.save(receipt);

    receivable.setSettledAmount(receivable.getSettledAmount().add(request.amount()));
    if (receivable.getSettledAmount().compareTo(receivable.getAmount()) >= 0) {
      receivable.setStatus(ReceivableStatus.SETTLED);
    } else {
      receivable.setStatus(ReceivableStatus.PAYMENT_PENDING);
    }
    receivableRepository.save(receivable);

    ledgerService.post("ADVANCE_RECEIPT_APPLY", receipt.getCode() + ":" + receivable.getCode(), request.applyDate(),
        "预收款核销应收 " + receivable.getCode(),
        java.util.List.of(
            new PostingLine("2203", "预收账款", request.amount(), BigDecimal.ZERO, "核销应收 " + receivable.getCode()),
            new PostingLine("1122", "应收账款", BigDecimal.ZERO, request.amount(), "预收款核销 " + receipt.getCode())));
    return toReceiptResponse(receipt);
  }

  @Transactional(readOnly = true)
  public Page<AdvancePaymentResponse> listPayments(Pageable pageable) {
    return paymentRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toPaymentResponse);
  }

  @Transactional
  public AdvancePaymentResponse createPayment(CreateAdvancePaymentRequest request) {
    String code = request.code() != null && !request.code().isBlank()
        ? request.code().trim() : codeGenerator.generate("ADVANCE_PAYMENT");
    if (paymentRepository.existsByCode(code)) throw new BusinessException("预付款单号已存在");
    AdvancePayment payment = new AdvancePayment();
    payment.setCode(code);
    payment.setSupplierId(request.supplierId());
    payment.setAmount(request.amount());
    payment.setPaidDate(request.paidDate());
    payment.setReferenceNo(request.referenceNo());
    payment.setRemark(request.remark());
    AdvancePayment saved = paymentRepository.save(payment);

    ledgerService.post("ADVANCE_PAYMENT", code, request.paidDate(), "预付款登记 " + code,
        java.util.List.of(
            new PostingLine("1123", "预付账款", request.amount(), BigDecimal.ZERO, "预付款 " + code),
            new PostingLine("1002", "银行存款", BigDecimal.ZERO, request.amount(), "预付款 " + code)));
    return toPaymentResponse(saved);
  }

  @Transactional
  public AdvancePaymentResponse applyPayment(UUID paymentId, UUID payableId, ApplyAdvanceRequest request) {
    AdvancePayment payment = paymentRepository.findByIdForUpdate(paymentId)
        .orElseThrow(() -> new BusinessException("预付款不存在"));
    ProcurementPayable payable = payableRepository.findByIdForUpdate(payableId)
        .orElseThrow(() -> new BusinessException("应付单不存在"));
    BigDecimal available = payment.getAmount().subtract(payment.getSettledAmount());
    if (request.amount().compareTo(available) > 0) throw new BusinessException("核销金额超过预付款可用余额" + available);
    BigDecimal outstanding = amount(payable.getAmount()).subtract(amount(payable.getAdjustedAmount()))
        .subtract(amount(payable.getPaidAmount()));
    if (request.amount().compareTo(outstanding) > 0) throw new BusinessException("核销金额超过应付未结金额" + outstanding);
    if (!payment.getSupplierId().equals(payable.getSupplierId())) {
      throw new BusinessException("预付款与应付单供应商不一致");
    }

    payment.setSettledAmount(payment.getSettledAmount().add(request.amount()));
    payment.setStatus(payment.getSettledAmount().compareTo(payment.getAmount()) >= 0 ? "SETTLED" : "PARTIAL");
    paymentRepository.save(payment);

    payable.setPaidAmount(amount(payable.getPaidAmount()).add(request.amount()));
    payableRepository.save(payable);

    ledgerService.post("ADVANCE_PAYMENT_APPLY", payment.getCode() + ":" + payable.getCode(), request.applyDate(),
        "预付款核销应付 " + payable.getCode(),
        java.util.List.of(
            new PostingLine("2202", "应付账款", request.amount(), BigDecimal.ZERO, "核销应付 " + payable.getCode()),
            new PostingLine("1123", "预付账款", BigDecimal.ZERO, request.amount(), "预付款核销 " + payment.getCode())));
    return toPaymentResponse(payment);
  }

  private AdvanceReceiptResponse toReceiptResponse(AdvanceReceipt r) {
    Customer customer = customerRepository.findById(r.getCustomerId()).orElse(null);
    return new AdvanceReceiptResponse(r.getId(), r.getCode(), r.getCustomerId(),
        customer == null ? null : customer.getName(), amount(r.getAmount()), amount(r.getSettledAmount()),
        amount(r.getAmount()).subtract(amount(r.getSettledAmount())), r.getReceivedDate(),
        r.getReferenceNo(), r.getStatus(), r.getRemark());
  }

  private AdvancePaymentResponse toPaymentResponse(AdvancePayment p) {
    Supplier supplier = supplierRepository.findById(p.getSupplierId()).orElse(null);
    return new AdvancePaymentResponse(p.getId(), p.getCode(), p.getSupplierId(),
        supplier == null ? null : supplier.getName(), amount(p.getAmount()), amount(p.getSettledAmount()),
        amount(p.getAmount()).subtract(amount(p.getSettledAmount())), p.getPaidDate(),
        p.getReferenceNo(), p.getStatus(), p.getRemark());
  }
}