package com.company.ops.api.modules.project.dto;

import java.time.OffsetDateTime;

/** 统一变更审计时间线条目，聚合阶段、成本、结项、预算与负责人变更。 */
public record ProjectTimelineEntryResponse(
    String type,
    OffsetDateTime occurredAt,
    String actor,
    String title,
    String detail
) {}
