package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.CloseoutReviewStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectCloseoutReviewResponse(
    UUID id,
    UUID projectId,
    CloseoutReviewStatus status,
    String requestComment,
    String reviewComment,
    String requestedBy,
    OffsetDateTime requestedAt,
    String reviewedBy,
    OffsetDateTime reviewedAt
) {}
