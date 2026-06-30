package com.company.ops.api.modules.project.dto;

import com.company.ops.api.modules.project.domain.ProjectStage;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectStageRecordResponse(
    UUID id,
    ProjectStage fromStage,
    ProjectStage toStage,
    int progress,
    String comment,
    String operatorName,
    OffsetDateTime changedAt
) {}
