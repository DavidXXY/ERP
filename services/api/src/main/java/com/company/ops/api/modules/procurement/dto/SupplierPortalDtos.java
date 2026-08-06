package com.company.ops.api.modules.procurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SupplierPortalDtos {
  private SupplierPortalDtos() {}

  public record RegisterRequest(
      @NotBlank @Size(max = 160) String companyName,
      @NotBlank @Size(max = 80) String unifiedSocialCreditCode,
      @Size(max = 80) String category,
      @NotBlank @Size(max = 80) String contactName,
      @NotBlank @Email @Size(max = 160) String email,
      @Size(max = 40) String phone,
      @NotBlank @Size(min = 8, max = 100) String password,
      @Size(max = 80) String registrationCode,
      LocalDate licenseValidTo,
      LocalDate qualificationValidTo
  ) {}

  public record LoginRequest(
      @NotBlank @Email @Size(max = 160) String email,
      @NotBlank @Size(max = 100) String password
  ) {}

  public record UpdateProfileRequest(
      @NotBlank @Size(max = 160) String name,
      @Size(max = 80) String category,
      @Size(max = 80) String contactName,
      @Size(max = 40) String phone,
      @Size(max = 80) String legalRepresentative,
      @NotBlank @Size(max = 80) String unifiedSocialCreditCode,
      @Size(max = 80) String registeredCapital,
      @Size(max = 240) String registeredAddress,
      @Size(max = 800) String businessScope,
      LocalDate licenseValidTo,
      LocalDate qualificationValidTo,
      @Size(max = 80) String taxpayerType,
      @Size(max = 120) String bankName,
      @Size(max = 120) String bankAccount,
      @Size(max = 160) String settlementTerms
  ) {}

  public record AccountResponse(
      UUID id,
      UUID supplierId,
      String supplierCode,
      String supplierName,
      String supplierAdmissionStatus,
      String email,
      String phone,
      String contactName,
      String status,
      boolean mustChangePassword,
      String reviewComment,
      String reviewedByName,
      OffsetDateTime reviewedAt,
      OffsetDateTime lastLoginAt,
      OffsetDateTime createdAt
  ) {}

  public record SupplierProfileResponse(
      UUID id,
      String code,
      String name,
      String category,
      String contactName,
      String phone,
      String legalRepresentative,
      String unifiedSocialCreditCode,
      String registeredCapital,
      String registeredAddress,
      String businessScope,
      LocalDate licenseValidTo,
      LocalDate qualificationValidTo,
      String taxpayerType,
      String bankName,
      String maskedBankAccount,
      String settlementTerms,
      String admissionStatus,
      String admissionReviewComment,
      String riskStatus
  ) {}

  public record SessionResponse(
      String token,
      AccountResponse account,
      SupplierProfileResponse supplier
  ) {}

  public record SaveQuoteLineRequest(
      @NotNull UUID requestId,
      @NotNull @Positive BigDecimal unitPrice,
      @NotNull @PositiveOrZero BigDecimal taxRate,
      LocalDate deliveryDate,
      @Size(max = 500) String remark
  ) {}

  public record SaveQuoteRequest(
      @NotEmpty List<@Valid SaveQuoteLineRequest> lines,
      @Size(max = 180) String paymentTerms,
      @Size(max = 500) String remark,
      @Size(max = 8) String currency,
      @PositiveOrZero BigDecimal freightAmount,
      @PositiveOrZero BigDecimal otherCostAmount,
      LocalDate validUntil
  ) {}

  public record InviteSuppliersRequest(@NotEmpty List<@NotNull UUID> supplierIds) {}

  public record ScoreQuoteRequest(
      @NotNull @PositiveOrZero @DecimalMax("100") BigDecimal technicalScore,
      @NotNull @PositiveOrZero @DecimalMax("100") BigDecimal commercialScore
  ) {}

  public record ReviewAccountRequest(
      @NotBlank String decision,
      @Size(max = 500) String comment
  ) {}

  public record OpenAccountRequest(
      @NotBlank @Email @Size(max = 160) String email,
      @Size(max = 40) String phone,
      @NotBlank @Size(max = 80) String contactName
  ) {}

  public record ReviewDocumentRequest(
      @NotBlank String decision,
      @Size(max = 500) String comment
  ) {}

  public record ChangePasswordRequest(
      @NotBlank @Size(max = 100) String currentPassword,
      @NotBlank @Size(min = 8, max = 100) String newPassword
  ) {}

  public record UpdateAccountStatusRequest(
      @NotBlank String status,
      @Size(max = 500) String comment
  ) {}

  public record ResetPasswordResponse(String temporaryPassword, AccountResponse account) {}

  public record OpenAccountResponse(String temporaryPassword, AccountResponse account) {}

  public record DeclineQuoteRequest(@NotBlank @Size(max = 500) String reason) {}

  public record AskClarificationRequest(@NotBlank @Size(max = 1000) String question) {}

  public record AnswerClarificationRequest(@NotBlank @Size(max = 2000) String answer) {}

  public record ClarificationResponse(
      UUID id,
      UUID inquiryId,
      UUID supplierId,
      String supplierName,
      String question,
      OffsetDateTime askedAt,
      String answer,
      String answeredByName,
      OffsetDateTime answeredAt,
      String status
  ) {}

  public record QuoteAttachmentResponse(
      UUID id,
      UUID quoteId,
      String attachmentType,
      String fileName,
      String contentType,
      long sizeBytes,
      String sha256,
      OffsetDateTime createdAt
  ) {}

  public record DocumentResponse(
      UUID id,
      UUID supplierId,
      String documentType,
      String documentName,
      String contentType,
      long sizeBytes,
      LocalDate validTo,
      String reviewStatus,
      String reviewComment,
      String reviewedByName,
      OffsetDateTime reviewedAt,
      OffsetDateTime createdAt
  ) {}
}
