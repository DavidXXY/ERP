package com.company.ops.api.modules.crm.dto;

/** Aggregated statistics for the customer pool header (computed server-side). */
public record CustomerPoolStats(
    long total,
    long strategic,
    long risk,
    long reconciliationIssue
) {}
