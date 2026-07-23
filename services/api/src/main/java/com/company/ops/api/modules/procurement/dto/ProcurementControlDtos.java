package com.company.ops.api.modules.procurement.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
public final class ProcurementControlDtos {
 private ProcurementControlDtos(){}
 public record CreateInquiry(@NotNull UUID requestId,@NotBlank String title,LocalDate deadline,@NotBlank String createdByName){}
 public record CreateSupplierQuote(@NotNull UUID supplierId,@NotNull @Positive BigDecimal unitPrice,@NotNull @PositiveOrZero BigDecimal taxRate,LocalDate deliveryDate,String paymentTerms,String remark){}
 public record SelectSupplierQuote(@NotBlank String operatorName,@NotBlank String reason){}
 public record ApproveOrder(@NotNull String decision,@NotBlank String approverName,@NotBlank String comment){}
 public record InspectReceipt(@NotNull @PositiveOrZero BigDecimal qualifiedQty,@NotNull @PositiveOrZero BigDecimal rejectedQty,@NotBlank String inspectorName,String comment,@NotNull LocalDate payableDueDate){}
 public record CreateInvoice(@NotNull UUID orderId,@NotBlank String invoiceNo,@NotNull @Positive BigDecimal amount,@NotNull @PositiveOrZero BigDecimal taxRate,@NotNull LocalDate invoiceDate,String remark){}
}
