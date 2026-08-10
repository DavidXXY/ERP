package com.company.ops.api.modules.procurement.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderDocumentResponse(
    UUID id,
    UUID orderId,
    String orderCode,
    String fileName,
    String contentType,
    long sizeBytes,
    String docType,
    String uploadedBy,
    OffsetDateTime uploadedAt
) {}
