package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
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
      @NotNull @Positive BigDecimal unitPrice,
      @NotNull @PositiveOrZero BigDecimal taxRate,
      LocalDate deliveryDate,
      String paymentTerms,
      String remark,
      String currency,
      @PositiveOrZero BigDecimal freightAmount,
      @PositiveOrZero BigDecimal otherCostAmount,
      @PositiveOrZero BigDecimal technicalScore,
      @PositiveOrZero BigDecimal commercialScore,
      LocalDate validUntil
  ) {}

  public record SelectSupplierQuote(@NotBlank String operatorName, @NotBlank String reason) {}

  public record ApproveOrder(@NotNull String decision, @NotBlank String approverName, @NotBlank String comment) {}

  public record InspectReceipt(
      @NotNull @PositiveOrZero BigDecimal qualifiedQty,
      @NotNull @PositiveOrZero BigDecimal rejectedQty,
      @NotBlank String inspectorName,
      String comment,
      @NotNull LocalDate payableDueDate
  ) {}

  public record CreateInvoice(
      @NotNull UUID orderId,
      @NotBlank String invoiceNo,
      @NotNull @Positive BigDecimal amount,
      @NotNull @PositiveOrZero BigDecimal taxRate,
      @NotNull LocalDate invoiceDate,
      String remark,
      UUID payableId,
      UUID receiptId,
      @Size(max = 80) String clientRequestId,
      UUID attachmentDocumentId
  ) {}

  public record ReviewInvoice(
      @NotBlank String decision,
      @NotBlank String reviewerName,
      String comment
  ) {}

  public record ResolveReturn(
      @PositiveOrZero BigDecimal replacementQty,
      @PositiveOrZero BigDecimal creditAmount,
      @PositiveOrZero BigDecimal claimAmount,
      String correctiveAction,
      String supplierResponse,
      @NotBlank String handlerName
  ) {}
}
