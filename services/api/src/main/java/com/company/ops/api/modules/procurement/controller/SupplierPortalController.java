package com.company.ops.api.modules.procurement.controller;

import static com.company.ops.api.modules.procurement.dto.SupplierPortalDtos.*;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.common.security.ClientIpResolver;
import com.company.ops.api.modules.procurement.dto.ProcurementShipmentResponse;
import com.company.ops.api.modules.procurement.security.SupplierPortalPrincipal;
import com.company.ops.api.modules.procurement.service.SupplierPortalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
  public ApiResponse<SessionResponse> register(
      @Valid @RequestBody RegisterRequest request,
      HttpServletRequest servletRequest
  ) {
    return ApiResponse.ok(service.register(request, clientIpResolver.resolve(servletRequest)));
  }

  @PostMapping("/auth/login")
  public ApiResponse<SessionResponse> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest servletRequest
  ) {
    return ApiResponse.ok(service.login(request, clientIpResolver.resolve(servletRequest)));
  }

  @PostMapping("/auth/forgot-password")
  public ApiResponse<String> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request,
      jakarta.servlet.http.HttpServletRequest servletRequest
  ) {
    return ApiResponse.ok(service.requestPasswordReset(
        request.email(), clientIpResolver.resolve(servletRequest)));
  }

  @PostMapping("/auth/reset-password")
  public ApiResponse<Void> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request
  ) {
    service.resetPassword(request.email(), request.code(), request.newPassword());
    return ApiResponse.ok();
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

  @GetMapping("/account/mfa/status")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<MfaStatusResponse> mfaStatus(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.mfaStatus(principal));
  }

  @GetMapping("/account/activities")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<Map<String, Object>>> accountActivities(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listAccountActivities(principal));
  }

  @PostMapping("/account/mfa/setup")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<MfaSetupResponse> mfaSetup(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @Valid @RequestBody MfaSetupRequest request
  ) {
    return ApiResponse.ok(service.beginMfaSetup(principal, request));
  }

  @PostMapping("/account/mfa/enable")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<String>> mfaEnable(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @Valid @RequestBody MfaEnableRequest request
  ) {
    return ApiResponse.ok(service.enableMfa(principal, request));
  }

  @PostMapping("/account/mfa/disable")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Void> mfaDisable(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @Valid @RequestBody MfaDisableRequest request
  ) {
    service.disableMfa(principal, request);
    return ApiResponse.ok();
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

  @GetMapping("/contract-documents/{id}/download")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<Resource> downloadContractDocument(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    Map<String, Object> metadata = service.contractDocumentMetadata(principal, id);
    String fileName = String.valueOf(metadata.get("fileName"));
    String contentType = String.valueOf(metadata.get("contentType"));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            contentType == null || "null".equals(contentType)
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
        .body(service.loadContractDocument(principal, id));
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

  @GetMapping(value = "/orders/{orderId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<byte[]> orderPdf(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID orderId
  ) {
    byte[] pdf = service.exportOrderPdf(principal, orderId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=order-" + orderId + ".pdf")
        .body(pdf);
  }

  @GetMapping(value = "/inquiries/{inquiryId}/quote/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<byte[]> quotePdf(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID inquiryId
  ) {
    byte[] pdf = service.exportQuotePdf(principal, inquiryId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quote-" + inquiryId + ".pdf")
        .body(pdf);
  }

  @GetMapping(value = "/inquiries/{inquiryId}/quote/excel",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<byte[]> quoteExcel(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID inquiryId
  ) {
    byte[] excel = service.exportQuoteExcel(principal, inquiryId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=quote-" + inquiryId + ".xlsx")
        .body(excel);
  }

  @GetMapping(value = "/orders/{orderId}/excel",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<byte[]> orderExcel(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID orderId
  ) {
    byte[] excel = service.exportOrderExcel(principal, orderId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=order-" + orderId + ".xlsx")
        .body(excel);
  }

  @GetMapping(value = "/finance/excel",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<byte[]> financeExcel(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    byte[] excel = service.exportFinanceExcel(principal);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finance.xlsx")
        .body(excel);
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

  @GetMapping("/inquiries/{id}/quote/revisions")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<QuoteRevisionResponse>> quoteRevisions(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    return ApiResponse.ok(service.listQuoteRevisions(principal, id));
  }

  @GetMapping("/change-requests")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<PortalChangeRequestResponse>> changeRequests(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listChangeRequests(principal));
  }

  @PostMapping("/change-requests")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<PortalChangeRequestResponse> createChangeRequest(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @Valid @RequestBody PortalChangeRequest request
  ) {
    return ApiResponse.ok(service.createChangeRequest(principal, request));
  }

  @GetMapping("/performance")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<PerformanceReviewResponse>> performanceReviews(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listPerformanceReviews(principal));
  }

  @PostMapping("/performance/{id}/appeal")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<PerformanceReviewResponse> appealPerformanceReview(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody PerformanceAppealRequest request
  ) {
    return ApiResponse.ok(service.appealPerformanceReview(principal, id, request.reason()));
  }

  @GetMapping("/notifications")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> notifications(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime before
  ) {
    return ApiResponse.ok(service.listNotifications(principal, before));
  }

  @GetMapping("/notifications/unread-count")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Long> unreadNotificationCount(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.unreadNotificationCount(principal));
  }

  @PostMapping("/notifications/{id}/read")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Void> markNotificationRead(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    service.markNotificationRead(principal, id);
    return ApiResponse.ok();
  }

  @PostMapping("/notifications/read-all")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Void> markAllNotificationsRead(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    service.markAllNotificationsRead(principal);
    return ApiResponse.ok();
  }

  @GetMapping("/shipments")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<ProcurementShipmentResponse>> myShipments(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listMyShipments(principal));
  }

  @GetMapping("/invoices")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<Map<String, Object>>> myInvoices(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listMyInvoices(principal));
  }


  @PostMapping(value = "/invoices/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<InvoiceSubmissionResponse> submitInvoice(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @Valid @RequestPart("metadata") SubmitInvoiceRequest request,
      @RequestPart("file") MultipartFile file
  ) {
    return ApiResponse.ok(service.submitInvoice(principal, request, file));
  }

  @GetMapping("/invoices/submissions")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<InvoiceSubmissionResponse>> myInvoiceSubmissions(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listMyInvoiceSubmissions(principal));
  }

  @GetMapping("/invoices/submissions/{id}/download")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<Resource> downloadInvoiceSubmission(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    InvoiceSubmissionResponse metadata = service.listMyInvoiceSubmissions(principal).stream()
        .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.contentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE : metadata.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(metadata.fileName(), StandardCharsets.UTF_8).build().toString())
        .body(service.loadInvoiceSubmission(principal, id));
  }

  @DeleteMapping("/invoices/submissions/{id}")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Void> deleteInvoiceSubmission(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    service.deleteInvoiceSubmission(principal, id);
    return ApiResponse.ok();
  }

  @GetMapping("/payables")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<Map<String, Object>>> myPayables(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listMyPayables(principal));
  }

  @GetMapping("/payables/{id}/receipt/download")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<Resource> downloadPaymentReceipt(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    Map<String, Object> metadata = service.listMyPayables(principal).stream()
        .filter(item -> id.equals(item.get("id"))).findFirst()
        .orElseThrow(() -> new com.company.ops.api.common.exception.BusinessException("应付单不存在"));
    String fileName = (String) metadata.get("paymentReceiptFileName");
    if (fileName == null) {
      throw new com.company.ops.api.common.exception.BusinessException("该应付单尚未上传付款回单");
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.get("paymentReceiptContentType") == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : String.valueOf(metadata.get("paymentReceiptContentType"))))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
        .body(service.loadPaymentReceipt(principal, id));
  }

  @GetMapping("/finance/summary")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> financeSummary(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.financeSummary(principal));
  }

  @GetMapping("/orders")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<Map<String, Object>>> myOrders(
      @AuthenticationPrincipal SupplierPortalPrincipal principal
  ) {
    return ApiResponse.ok(service.listMyOrders(principal));
  }

  @PostMapping("/orders/{orderId}/shipments")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<ProcurementShipmentResponse> createShipment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID orderId,
      @Valid @RequestBody CreateShipmentRequest request
  ) {
    return ApiResponse.ok(service.createShipment(principal, orderId, request));
  }

  @PostMapping(value = "/shipments/{shipmentId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<ShipmentAttachmentResponse> uploadShipmentAttachment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID shipmentId,
      @RequestPart MultipartFile file
  ) {
    return ApiResponse.ok(service.uploadShipmentAttachment(principal, shipmentId, file));
  }

  @GetMapping("/shipments/{shipmentId}/attachments")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<List<ShipmentAttachmentResponse>> shipmentAttachments(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID shipmentId
  ) {
    return ApiResponse.ok(service.listShipmentAttachments(principal, shipmentId));
  }

  @GetMapping("/shipments/{shipmentId}/attachments/{id}/download")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ResponseEntity<Resource> downloadShipmentAttachment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID shipmentId,
      @PathVariable UUID id
  ) {
    ShipmentAttachmentResponse metadata = service.listShipmentAttachments(principal, shipmentId).stream()
        .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.contentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE : metadata.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(metadata.fileName(), StandardCharsets.UTF_8).build().toString())
        .body(service.loadShipmentAttachment(principal, id));
  }

  @DeleteMapping("/shipments/{shipmentId}/attachments/{id}")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Void> deleteShipmentAttachment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID shipmentId,
      @PathVariable UUID id
  ) {
    service.deleteShipmentAttachment(principal, id);
    return ApiResponse.ok();
  }

  @PutMapping("/shipments/{shipmentId}")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<ProcurementShipmentResponse> updateShipment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID shipmentId,
      @Valid @RequestBody CreateShipmentRequest request
  ) {
    return ApiResponse.ok(service.updateShipment(principal, shipmentId, request));
  }

  @DeleteMapping("/shipments/{shipmentId}")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Void> deleteShipment(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID shipmentId
  ) {
    service.deleteShipment(principal, shipmentId);
    return ApiResponse.ok();
  }

  @PostMapping("/orders/{orderId}/changes/{changeId}/respond")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> respondOrderChange(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID orderId,
      @PathVariable UUID changeId,
      @Valid @RequestBody RespondOrderChangeRequest request
  ) {
    return ApiResponse.ok(service.respondOrderChange(principal, orderId, changeId, request));
  }

  @PostMapping("/receipts/{receiptId}/appeal")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> appealReceipt(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID receiptId,
      @Valid @RequestBody ReceiptAppealRequest request
  ) {
    return ApiResponse.ok(service.appealReceipt(principal, receiptId, request));
  }

  @PostMapping("/contracts/{id}/acknowledge")
  @PreAuthorize("hasAuthority('supplier-portal:access')")
  public ApiResponse<Map<String, Object>> acknowledgeContract(
      @AuthenticationPrincipal SupplierPortalPrincipal principal,
      @PathVariable UUID id
  ) {
    return ApiResponse.ok(service.acknowledgeContract(principal, id));
  }
}
