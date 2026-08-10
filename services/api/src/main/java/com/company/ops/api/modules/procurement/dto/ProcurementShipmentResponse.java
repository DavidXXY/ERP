package com.company.ops.api.modules.procurement.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcurementShipmentResponse(
    UUID id,
    UUID orderId,
    String orderCode,
    UUID supplierId,
    String supplierName,
    String deliveryNo,
    String carrier,
    LocalDate expectedArrival,
    String remark,
    String status,
    String createdByName,
    OffsetDateTime createdAt
) {}
