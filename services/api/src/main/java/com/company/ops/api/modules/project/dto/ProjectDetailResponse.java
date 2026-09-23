package com.company.ops.api.modules.project.dto;

import java.util.List;

public record ProjectDetailResponse(
    ProjectResponse project,
    List<ProjectBudgetItemResponse> budgetItems,
    List<ProjectCostEntryResponse> costEntries,
    List<ProjectStageRecordResponse> stageRecords,
    List<ProjectMilestoneResponse> milestones,
    List<ProjectRiskResponse> risks,
    List<ProjectStaffResponse> staff,
    List<ProjectTimelineEntryResponse> timeline
) {}
