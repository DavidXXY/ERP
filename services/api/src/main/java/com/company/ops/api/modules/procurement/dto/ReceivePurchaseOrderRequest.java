package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivePurchaseOrderRequest(
    @NotNull @DecimalMin("0.01") BigDecimal quantity,
    @NotNull LocalDate receivedDate,
    @NotBlank @Size(max = 80) String deliveryNo,
    @NotBlank @Size(max = 80) String receiverName,
    @NotNull LocalDate payableDueDate
) {
}
