package com.company.ops.api.modules.procurement.controller;

import static com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.*;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.procurement.service.SupplierPortalService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procurement/supplier-portal")
public class SupplierPortalAdminController {
  private final SupplierPortalService service;

  public SupplierPortalAdminController(SupplierPortalService service) {
    this.service = service;
  }

  @GetMapping("/accounts")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<AccountResponse>> accounts() {
    return ApiResponse.ok(service.listAccounts());
  }

  @PostMapping("/accounts/{id}/review")
  @PreAuthorize("hasAuthority('procurement:portal-account:approve')")
  public ApiResponse<AccountResponse> reviewAccount(
      @PathVariable UUID id,
      @Valid @RequestBody ReviewAccountRequest request
  ) {
    return ApiResponse.ok(service.reviewAccount(id, request));
  }

  @PostMapping("/accounts/{id}/status")
  @PreAuthorize("hasAuthority('procurement:portal-account:approve')")
  public ApiResponse<AccountResponse> updateAccountStatus(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateAccountStatusRequest request
  ) {
    return ApiResponse.ok(service.updateAccountStatus(id, request));
  }

  @PostMapping("/accounts/{id}/reset-password")
  @PreAuthorize("hasAuthority('procurement:portal-account:approve')")
  public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable UUID id) {
    return ApiResponse.ok(service.resetPassword(id));
  }

  @GetMapping("/suppliers/{supplierId}/documents")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<DocumentResponse>> documents(@PathVariable UUID supplierId) {
    return ApiResponse.ok(service.listSupplierDocuments(supplierId));
  }

  @PostMapping("/documents/{id}/review")
  @PreAuthorize("hasAuthority('procurement:portal-document:approve')")
  public ApiResponse<DocumentResponse> reviewDocument(
      @PathVariable UUID id,
      @Valid @RequestBody ReviewDocumentRequest request
  ) {
    return ApiResponse.ok(service.reviewDocument(id, request));
  }

  @GetMapping("/documents/{id}/download")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ResponseEntity<Resource> download(@PathVariable UUID id) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("supplier-document", StandardCharsets.UTF_8).build().toString())
        .body(service.loadDocumentForInternal(id));
  }

  @GetMapping("/quotes/{quoteId}/attachments")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<QuoteAttachmentResponse>> quoteAttachments(@PathVariable UUID quoteId) {
    return ApiResponse.ok(service.listQuoteAttachmentsForInternal(quoteId));
  }

  @GetMapping("/quote-attachments/{id}/download")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ResponseEntity<Resource> downloadQuoteAttachment(@PathVariable UUID id) {
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("supplier-quote-attachment", StandardCharsets.UTF_8).build().toString())
        .body(service.loadQuoteAttachmentForInternal(id));
  }

  @GetMapping("/inquiries/{inquiryId}/clarifications")
  @PreAuthorize("hasAuthority('procurement:view')")
  public ApiResponse<List<ClarificationResponse>> clarifications(@PathVariable UUID inquiryId) {
    return ApiResponse.ok(service.listClarificationsForInternal(inquiryId));
  }

  @PostMapping("/clarifications/{id}/answer")
  @PreAuthorize("hasAuthority('procurement:purchase:create')")
  public ApiResponse<ClarificationResponse> answerClarification(
      @PathVariable UUID id,
      @Valid @RequestBody AnswerClarificationRequest request
  ) {
    return ApiResponse.ok(service.answerClarification(id, request));
  }
}
