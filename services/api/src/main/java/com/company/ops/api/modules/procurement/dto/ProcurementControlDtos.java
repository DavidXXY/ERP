package com.company.ops.api.modules.procurement.dto;

import com.company.ops.api.modules.finance.dto.PaymentSplit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProcurementControlDtos {
  private ProcurementControlDtos() {}

  public record CreateInquiry(
      @NotNull UUID requestId,
      @NotBlank String title,
      LocalDate deadline,
      @NotBlank String createdByName,
      String sourcingMethod,
      Integer minQuoteCount,
      @Size(max = 500) String exceptionReason
  ) {}

  public record CreateSupplierQuote(
      @NotNull UUID supplierId,
      @Positive BigDecimal unitPrice,
      @PositiveOrZero BigDecimal taxRate,
      LocalDate deliveryDate,
      String paymentTerms,
      String remark,
      String currency,
      @PositiveOrZero BigDecimal freightAmount,
      @PositiveOrZero BigDecimal otherCostAmount,
      @PositiveOrZero BigDecimal technicalScore,
      @PositiveOrZero BigDecimal commercialScore,
      LocalDate validUntil,
      List<@Valid CreateSupplierQuoteLine> lines
  ) {}

  public record CreateSupplierQuoteLine(
      @NotNull UUID requestId,
      @NotNull @Positive BigDecimal unitPrice,
      @NotNull @PositiveOrZero BigDecimal taxRate,
      LocalDate deliveryDate,
      String remark
  ) {}

  public record SelectSupplierQuote(@NotBlank String operatorName, @NotBlank String reason) {}

  public record InviteSuppliers(
      @NotEmpty List<@NotNull UUID> supplierIds,
      Map<UUID, String> contactEmails
  ) {}

  public record UpdateInquiryDeadline(@NotNull LocalDate deadline) {}

  public record UpdateInquiryMinQuotes(@NotNull @Min(1) @Max(20) Integer minQuoteCount) {}

  public record ScoreSupplierQuote(
      @NotNull @PositiveOrZero @DecimalMax("100") BigDecimal technicalScore,
      @NotNull @PositiveOrZero @DecimalMax("100") BigDecimal commercialScore
  ) {}

  public record ApproveOrder(@NotNull String decision, @NotBlank String approverName, @NotBlank String comment) {}

  public record InspectReceipt(
      @NotNull @PositiveOrZero BigDecimal qualifiedQty,
      @NotNull @PositiveOrZero BigDecimal rejectedQty,
      @NotBlank String inspectorName,
      String comment,
      LocalDate payableDueDate // 不填时按供应商账期自动计算
  ) {}

  public record CreateInvoice(
      @NotNull UUID orderId,
      @NotBlank String invoiceNo,
      @NotNull @Positive BigDecimal amount,
      @NotNull @PositiveOrZero BigDecimal taxRate,
      @NotNull LocalDate invoiceDate,
      String remark,
      UUID payableId,
      List<UUID> payableIds, // 合并开票：同一订单的多个应付
      UUID receiptId,
      @Size(max = 80) String clientRequestId,
      UUID attachmentDocumentId
  ) {}

  public record ReviewInvoice(
      @NotBlank String decision,
      @NotBlank String reviewerName,
      String comment
  ) {}

  public record VerifyInvoice(
      @NotBlank String decision,
      @Size(max = 500) String comment
  ) {}

  public record ReviewInvoiceSubmissionRequest(
      @NotBlank String action,
      @Size(max = 500) String comment
  ) {}

  public record ResolveAppealRequest(
      @NotBlank String action,
      @Size(max = 500) String comment
  ) {}

  public record ResolveReturn(
      @PositiveOrZero BigDecimal replacementQty,
      @PositiveOrZero BigDecimal creditAmount,
      @PositiveOrZero BigDecimal claimAmount,
      String correctiveAction,
      String supplierResponse,
      @NotBlank String handlerName
  ) {}

  public record RecordPaymentRequest(
      @NotEmpty List<@Valid PaymentSplit> payments,
      @Size(max = 500) String paymentNote
  ) {}
}
