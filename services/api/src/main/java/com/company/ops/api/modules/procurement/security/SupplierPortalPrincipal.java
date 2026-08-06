package com.company.ops.api.modules.procurement.security;

import java.util.UUID;

public record SupplierPortalPrincipal(
    UUID accountId,
    UUID supplierId,
    String tenantId,
    String email,
    String contactName,
    String status,
    long authVersion
) {}
