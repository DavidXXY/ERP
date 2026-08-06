package com.company.ops.api.modules.procurement.controller;

import static com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.*;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.security.ClientIpResolver;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.company.ops.api.modules.procurement.service.SupplierPortalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/supplier-portal")
public class SupplierPortalController {
  private final SupplierPortalService service;
  private final ClientIpResolver clientIpResolver;

  public SupplierPortalController(SupplierPortalService service, ClientIpResolver clientIpResolver) {
    this.service = service;
    this.clientIpResolver = clientIpResolver;
  }

  @PostMapping("/auth/register")
  public ApiResponse<SessionResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.ok(service.register(request));
  }

  @PostMapping("/auth/login")
  public ApiResponse<SessionResponse> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest servletRequest
  ) {
    return ApiResponse.ok(service.login(request, clientIpResolver.resolve(servletRequest)));
  }

  @GetMapping("/me")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<SessionResponse> me(@AuthenticationPrincipal SupplierPortalPrincipal principal) {
    return ApiResponse.ok(service.current(principal));
  }

  @PostMapping("/account/change-password")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<SessionResponse> changePassword(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @Valid @RequestBody ChangePasswordRequest request
  ) {
    return ApiResponse.ok(service.changePassword(principal, request));
  }

  @PutMapping("/profile")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<SupplierProfileResponse> updateProfile(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @Valid @RequestBody UpdateProfileRequest request
  ) {
    return ApiResponse.ok(service.updateProfile(principal, request));
  }

  @GetMapping("/documents")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<DocumentResponse>> documents(@AuthenticationPrincipal SupplierPortalPrincipal principal) {
    return ApiResponse.ok(service.listDocuments(principal));
  }

  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<DocumentResponse> upload(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @RequestParam String documentType,
      @RequestParam(required = false) LocalDate validTo,
      @RequestPart MultipartFile file
  ) {
    return ApiResponse.ok(service.uploadDocument(principal, documentType, validTo, file));
  }

  @GetMapping("/documents/{id}/download")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<Resource> download(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    DocumentResponse metadata = service.listDocuments(principal).stream()
        .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.contentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE : metadata.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(metadata.documentName(), StandardCharsets.UTF_8).build().toString())
        .body(service.loadDocument(principal, id));
  }

  @DeleteMapping("/documents/{id}")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    service.deleteDocument(principal, id);
    return ApiResponse.ok();
  }

  @GetMapping("/inquiries")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<Map<String, Object>>> inquiries(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listInquiries(principal));
  }

  @PutMapping("/inquiries/{id}/quote")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> saveQuote(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody SaveQuoteRequest request
  ) {
    return ApiResponse.ok(service.saveQuote(principal, id, request, false));
  }

  @PostMapping("/inquiries/{id}/quote/submit")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> submitQuote(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody SaveQuoteRequest request
  ) {
    return ApiResponse.ok(service.saveQuote(principal, id, request, true));
  }

  @PostMapping("/inquiries/{id}/quote/withdraw")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> withdrawQuote(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    return ApiResponse.ok(service.withdrawQuote(principal, id));
  }

  @PostMapping("/inquiries/{id}/quote/confirm")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> confirmQuote(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    return ApiResponse.ok(service.confirmInternalQuote(principal, id));
  }

  @PostMapping("/inquiries/{id}/decline")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> decline(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody DeclineQuoteRequest request
  ) {
    return ApiResponse.ok(service.declineInquiry(principal, id, request));
  }

  @GetMapping("/inquiries/{id}/attachments")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<QuoteAttachmentResponse>> attachments(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    return ApiResponse.ok(service.listQuoteAttachments(principal, id));
  }

  @PostMapping(value = "/inquiries/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<QuoteAttachmentResponse> uploadAttachment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id,
      @RequestParam String attachmentType,
      @RequestPart MultipartFile file
  ) {
    return ApiResponse.ok(service.uploadQuoteAttachment(principal, id, attachmentType, file));
  }

  @GetMapping("/inquiries/{inquiryId}/attachments/{id}/download")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<Resource> downloadAttachment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID inquiryId,
      @PathVariable UUID id
  ) {
    QuoteAttachmentResponse metadata = service.listQuoteAttachments(principal, inquiryId).stream()
        .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.contentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE : metadata.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(metadata.fileName(), StandardCharsets.UTF_8).build().toString())
        .body(service.loadQuoteAttachment(principal, id));
  }

  @DeleteMapping("/inquiries/{inquiryId}/attachments/{id}")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Void> deleteAttachment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID inquiryId,
      @PathVariable UUID id
  ) {
    service.deleteQuoteAttachment(principal, id);
    return ApiResponse.ok();
  }

  @GetMapping("/inquiries/{id}/clarifications")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<ClarificationResponse>> clarifications(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    return ApiResponse.ok(service.listClarifications(principal, id));
  }

  @PostMapping("/inquiries/{id}/clarifications")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<ClarificationResponse> askClarification(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody AskClarificationRequest request
  ) {
    return ApiResponse.ok(service.askClarification(principal, id, request));
  }
}
