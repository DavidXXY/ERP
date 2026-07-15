package com.company.ops.api.modules.crm.dto;

import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.ApprovalDecision;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.FollowUpType;
import com.company.ops.api.modules.crm.domain.OpportunityStage;
import com.company.ops.api.modules.crm.domain.QuoteCustomerDecision;
import com.company.ops.api.modules.crm.domain.QuoteCostStatus;
import com.company.ops.api.modules.crm.domain.QuoteStatus;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class CrmOperationsDtos {

  private CrmOperationsDtos() {
  }

  public record CreateOpportunityRequest(
      UUID customerId,
      // auto-gen
    String code,
      @Size(max = 80) String source,
      @NotBlank @Size(max = 500) String needSummary,
      OpportunityStage stage,
      @DecimalMin("0") BigDecimal expectedAmount,
      @Min(0) @Max(100) Integer probability,
      @Size(max = 240) String nextAction,
      LocalDate nextActionAt,
      @NotBlank @Size(max = 80) String ownerName
  ) {
  }

  public record AdvanceOpportunityRequest(
      @NotNull OpportunityStage stage,
      @NotBlank @Size(max = 240) String nextAction,
      @NotNull LocalDate nextActionAt,
      @NotNull @Min(0) @Max(100) Integer probability
  ) {
  }

  public record OpportunityResponse(
      UUID id,
      UUID customerId,
      String customerName,
      String code,
      String source,
      String needSummary,
      OpportunityStage stage,
      BigDecimal expectedAmount,
      int probability,
      String nextAction,
      LocalDate nextActionAt,
      String ownerName,
      OffsetDateTime updatedAt
  ) {
  }

  public record CreateQuoteRequest(
      UUID customerId,
      UUID opportunityId,
      // auto-gen
    String code,
      @NotBlank @Size(max = 800) String serviceScope,
      @Size(max = 120) String inspectCycle,
      @Size(max = 300) String paymentNodes,
      @NotNull @DecimalMin("0") BigDecimal amount,
      @DecimalMin("0") BigDecimal taxRate,
      @DecimalMin("0") BigDecimal laborBudget,
      @DecimalMin("0") BigDecimal materialBudget,
      @DecimalMin("0") BigDecimal subcontractBudget,
      @DecimalMin("0") BigDecimal travelBudget,
      @DecimalMin("0") BigDecimal otherBudget,
      @NotBlank @Size(max = 80) String editorName
  ) {
  }

  public record UpdateQuoteRequest(
      @NotBlank @Size(max = 800) String serviceScope,
      @Size(max = 120) String inspectCycle,
      @Size(max = 300) String paymentNodes,
      @NotNull @DecimalMin("0") BigDecimal amount,
      @DecimalMin("0") BigDecimal taxRate,
      @DecimalMin("0") BigDecimal laborBudget,
      @DecimalMin("0") BigDecimal materialBudget,
      @DecimalMin("0") BigDecimal subcontractBudget,
      @DecimalMin("0") BigDecimal travelBudget,
      @DecimalMin("0") BigDecimal otherBudget,
      @NotBlank @Size(max = 500) String revisionNote,
      @NotBlank @Size(max = 80) String editorName
  ) {
  }

  public record QuoteResponse(
      UUID id,
      UUID customerId,
      String customerName,
      UUID opportunityId,
      String opportunityCode,
      String code,
      String serviceScope,
      String inspectCycle,
      String paymentNodes,
      BigDecimal amount,
      BigDecimal taxRate,
      BigDecimal netAmount,
      BigDecimal laborBudget,
      BigDecimal materialBudget,
      BigDecimal subcontractBudget,
      BigDecimal travelBudget,
      BigDecimal otherBudget,
      BigDecimal budgetAmount,
      BigDecimal grossMargin,
      BigDecimal grossMarginRate,
      int versionNo,
      QuoteStatus status,
      String lastApprovalComment,
      String lastApproverName,
      OffsetDateTime lastApprovalAt,
      QuoteCustomerDecision customerDecision,
      String customerComment,
      String customerDecisionBy,
      OffsetDateTime customerDecidedAt,
      UUID convertedContractId,
      QuoteCostRequestResponse costRequest,
      OffsetDateTime updatedAt
  ) {
  }

  public record RequestQuoteCostRequest(
      @NotBlank @Size(max = 80) String requestedBy
  ) {
  }

  public record SubmitQuoteCostRequest(
      @NotBlank @Size(max = 80) String projectManager,
      @DecimalMin("0") BigDecimal laborCost,
      @DecimalMin("0") BigDecimal laborTaxRate,
      @DecimalMin("0") BigDecimal materialCost,
      @DecimalMin("0") BigDecimal materialTaxRate,
      @DecimalMin("0") BigDecimal subcontractCost,
      @DecimalMin("0") BigDecimal subcontractTaxRate,
      @DecimalMin("0") BigDecimal travelCost,
      @DecimalMin("0") BigDecimal equipmentCost,
      @DecimalMin("0") BigDecimal equipmentTaxRate,
      @DecimalMin("0") BigDecimal riskReserve,
      @DecimalMin("0") BigDecimal otherCost,
      @DecimalMin("0") BigDecimal suggestedPrice,
      @Size(max = 800) String costRemark
  ) {
  }

  public record ApproveQuoteCostRequest(
      @NotNull ApprovalDecision decision,
      @NotBlank @Size(max = 80) String approverName,
      @NotBlank @Size(max = 500) String comment
  ) {
  }

  public record QuoteCostRequestResponse(
      UUID id,
      UUID quoteId,
      UUID opportunityId,
      UUID customerId,
      QuoteCostStatus status,
      String requestedBy,
      OffsetDateTime requestedAt,
      String projectManager,
      BigDecimal laborCost,
      BigDecimal laborTaxRate,
      BigDecimal materialCost,
      BigDecimal materialTaxRate,
      BigDecimal subcontractCost,
      BigDecimal subcontractTaxRate,
      BigDecimal travelCost,
      BigDecimal equipmentCost,
      BigDecimal equipmentTaxRate,
      BigDecimal riskReserve,
      BigDecimal otherCost,
      BigDecimal totalCost,
      BigDecimal suggestedPrice,
      String costRemark,
      OffsetDateTime submittedAt,
      String approvedBy,
      OffsetDateTime approvedAt,
      String approvalComment
  ) {
  }

  public record ProcessQuoteApprovalRequest(
      @NotNull ApprovalDecision decision,
      @NotBlank @Size(max = 500) String comment,
      @NotBlank @Size(max = 80) String approverName
  ) {
  }

  public record ProcessQuoteCustomerResultRequest(
      @NotNull QuoteCustomerDecision decision,
      @NotBlank @Size(max = 500) String comment,
      @NotBlank @Size(max = 80) String operatorName
  ) {
  }

  public record ConvertQuoteRequest(
      @Size(max = 64) String contractCode,
      @Size(max = 160) String projectName,
      @Size(max = 80) String contractType,
      LocalDate startDate,
      LocalDate endDate,
      @Size(max = 120) String serviceCycle,
      @Size(max = 64) String receivableCode,
      @DecimalMin("0") BigDecimal firstReceivableAmount,
      LocalDate firstReceivableDueDate
  ) {
  }

  public record QuoteConversionResult(
      QuoteResponse quote,
      ContractResponse contract,
      ReceivableResponse receivable
  ) {
  }

  public record QuoteRevisionResponse(
      UUID id,
      int versionNo,
      String code,
      String serviceScope,
      String inspectCycle,
      String paymentNodes,
      BigDecimal amount,
      BigDecimal taxRate,
      BigDecimal netAmount,
      BigDecimal laborBudget,
      BigDecimal materialBudget,
      BigDecimal subcontractBudget,
      BigDecimal travelBudget,
      BigDecimal otherBudget,
      BigDecimal budgetAmount,
      BigDecimal grossMargin,
      BigDecimal grossMarginRate,
      QuoteStatus status,
      String revisionNote,
      String editorName,
      OffsetDateTime revisedAt
  ) {
  }

  public record CreateFollowUpRequest(
      @NotNull UUID customerId,
      UUID opportunityId,
      @NotNull FollowUpType type,
      @NotBlank @Size(max = 160) String subject,
      @NotBlank @Size(max = 1200) String content,
      @NotNull OffsetDateTime followedAt,
      @Size(max = 240) String nextAction,
      @NotBlank @Size(max = 80) String ownerName
  ) {
  }

  public record FollowUpResponse(
      UUID id,
      UUID customerId,
      String customerName,
      UUID opportunityId,
      String opportunityCode,
      FollowUpType type,
      String subject,
      String content,
      OffsetDateTime followedAt,
      String nextAction,
      String ownerName
  ) {
  }

  public record ContractResponse(
      UUID id,
      UUID quoteId,
      UUID customerId,
      String customerName,
      String code,
      String projectName,
      String contractType,
      BigDecimal amount,
      BigDecimal taxRate,
      BigDecimal netAmount,
      LocalDate startDate,
      LocalDate endDate,
      String serviceCycle,
      ContractStatus status
  ) {
  }

  public record ReceivableResponse(
      UUID id,
      UUID customerId,
      String customerName,
      UUID contractId,
      String contractCode,
      String code,
      String sourceNo,
      BigDecimal amount,
      LocalDate dueDate,
      String invoiceNo,
      LocalDate invoiceDate,
      BigDecimal settledAmount,
      BigDecimal outstandingAmount,
      ReceivableStatus status
  ) {
  }

  public record RegisterInvoiceRequest(
      @NotBlank @Size(max = 80) String invoiceNo,
      @NotNull LocalDate invoiceDate
  ) {
  }

  public record RecordReceiptRequest(
      @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
      @NotNull LocalDate receivedDate,
      @NotBlank @Size(max = 80) String referenceNo,
      @NotBlank @Size(max = 80) String recorderName
  ) {
  }

  public record RenewalResponse(
      UUID contractId,
      UUID customerId,
      String customerName,
      String contractCode,
      String projectName,
      BigDecimal amount,
      LocalDate endDate,
      long daysRemaining,
      String renewalRisk,
      BigDecimal outstandingAmount,
      ContractStatus status
  ) {
  }

  public record CustomerProfileResponse(
      UUID customerId,
      String customerCode,
      String customerName,
      String industry,
      CustomerLevel level,
      String ownerName,
      RiskStatus riskStatus,
      String paymentHabit,
      int opportunityCount,
      BigDecimal opportunityAmount,
      int contractCount,
      BigDecimal contractAmount,
      BigDecimal outstandingAmount,
      BigDecimal overdueAmount,
      LocalDate nearestContractEndDate
  ) {
  }

  public record UpdateReceivableRequest(
      @Size(max = 64) String sourceNo,
      @DecimalMin("0") BigDecimal amount,
      LocalDate dueDate
  ) {
  }

    public record UpdateContractRequest(
      @Size(max = 120) String projectName,
      @Size(max = 40) String contractType,
      BigDecimal amount,
      BigDecimal taxRate,
      @Size(max = 80) String serviceCycle,
      String startDate,
      String endDate
  ) {}

  public record CreateContractChangeRequest(
      String changeData,
      @NotBlank @Size(max = 500) String reason,
      @NotBlank @Size(max = 80) String requestedBy
  ) {
  }

  public record ContractChangeResponse(
      UUID id, UUID contractId, String changeData, String reason,
      String status, String requestedBy, OffsetDateTime requestedAt,
      String approvedBy, OffsetDateTime approvedAt, String approvalComment,
      OffsetDateTime createdAt
  ) {
  }

  public record ApprovalActionRequest(
      @NotBlank @Size(max = 80) String operatorName,
      @Size(max = 500) String comment
  ) {
  }

}
