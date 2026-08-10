package com.company.ops.api.modules.procurement.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SupplierPortalNotificationResponse(
    UUID id,
    String type,
    String title,
    String content,
    String relatedType,
    UUID relatedId,
    boolean read,
    OffsetDateTime readAt,
    OffsetDateTime createdAt
) {}
