package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateConsolidatedInquiryRequest(
    @NotEmpty List<UUID> requestIds,
    @NotBlank @Size(max = 180) String title,
    LocalDate deadline,
    String sourcingMethod,
    Integer minQuoteCount,
    @Size(max = 500) String exceptionReason
) {}
