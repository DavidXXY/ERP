package com.company.ops.api.modules.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FinanceContributionDtos {
  private FinanceContributionDtos() {}

  public record ContributionSalesperson(
      UUID id,
      String displayName,
      UUID organizationId,
      String organizationName,
      String organizationPath,
      boolean enabled) {}

  public record ContributionScope(
      String subjectType,
      UUID subjectId,
      String subjectName,
      String subjectPath,
      boolean includeDescendants,
      int organizationCount,
      String attributionBasis) {}

  public record ContributionSummary(
      BigDecimal contractAmount,
      BigDecimal actualCost,
      BigDecimal grossProfit,
      BigDecimal grossMarginRate,
      BigDecimal receivedAmount,
      BigDecimal paidAmount,
      BigDecimal netCashFlow,
      BigDecimal receivableOutstanding,
      BigDecimal payableOutstanding,
      BigDecimal collectionRate,
      long projectCount) {}

  public record MonthlyContribution(
      int month,
      BigDecimal receipt,
      BigDecimal payment,
      BigDecimal netCash) {}

  public record ContributionProjectLine(
      UUID projectId,
      String projectCode,
      String projectName,
      String customerName,
      String stage,
      String salesOwnerName,
      BigDecimal contractAmount,
      BigDecimal actualCost,
      BigDecimal grossProfit,
      BigDecimal grossMarginRate,
      BigDecimal receivedAmount,
      BigDecimal paidAmount,
      BigDecimal netCashFlow,
      BigDecimal receivableOutstanding,
      BigDecimal payableOutstanding) {}

  public record ContributionDataQuality(
      long unattributedProjectCount,
      long unattributedReceivableCount,
      long unlinkedReceivableCount,
      String note) {}

  public record FinanceContributionResponse(
      LocalDate asOf,
      int fiscalYear,
      ContributionScope scope,
      ContributionSummary summary,
      List<MonthlyContribution> monthlyCashFlow,
      List<ContributionProjectLine> projects,
      ContributionDataQuality dataQuality) {}
}
