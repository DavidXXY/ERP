package com.company.ops.api.modules.office.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.office.domain.DocumentFile;
import com.company.ops.api.modules.office.dto.OfficeDtos.ApprovalResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.CompleteOutsourceRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.CreateApprovalRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.CreateExpenseRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.CreateOutsourceRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.DocumentResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ExpenseResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.NotificationResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.OfficeOverview;
import com.company.ops.api.modules.office.dto.OfficeDtos.OfficeReferenceResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.OutsourceResponse;
import com.company.ops.api.modules.office.dto.OfficeDtos.ProcessApprovalRequest;
import com.company.ops.api.modules.office.dto.OfficeDtos.SupplierOption;
import com.company.ops.api.modules.office.service.OfficeService;
import com.company.ops.api.modules.office.service.ReminderScheduler;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import java.util.ArrayList;

@RestController @RequestMapping("/api/office")
public class OfficeController {
  private final OfficeService service;
  private final ReminderScheduler reminderScheduler;
  public OfficeController(OfficeService service, ReminderScheduler reminderScheduler) { this.service = service; this.reminderScheduler = reminderScheduler; }

  @GetMapping("/overview") @PreAuthorize("hasAuthority('office:view')")
  public ApiResponse<OfficeOverview> overview() { return ApiResponse.ok(service.overview()); }
  @GetMapping("/references") @PreAuthorize("hasAnyAuthority('office:view', 'office:expense:create', 'office:outsource:create')")
  public ApiResponse<OfficeReferenceResponse> references() { return ApiResponse.ok(service.references()); }
  @GetMapping("/approvals") @PreAuthorize("hasAuthority('office:approval:view')")
  public ApiResponse<List<ApprovalResponse>> approvals() { return ApiResponse.ok(service.listApprovals()); }
  @PostMapping("/approvals") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('office:approval:create')")
  public ApiResponse<ApprovalResponse> createApproval(@Valid @RequestBody CreateApprovalRequest request) { return ApiResponse.ok(service.createApproval(request)); }
  @PostMapping("/approvals/{id}/process") @PreAuthorize("hasAuthority('office:approval:process')")
  public ApiResponse<ApprovalResponse> process(@PathVariable UUID id, @Valid @RequestBody ProcessApprovalRequest request) { return ApiResponse.ok(service.processApproval(id, request)); }
  @GetMapping("/expenses") @PreAuthorize("hasAuthority('office:expense:view')")
  public ApiResponse<List<ExpenseResponse>> expenses() { return ApiResponse.ok(service.listExpenses()); }
  @PostMapping("/expenses") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('office:expense:create')")
  public ApiResponse<ExpenseResponse> createExpense(@Valid @RequestBody CreateExpenseRequest request) { return ApiResponse.ok(service.createExpense(request)); }
  @GetMapping("/outsourcing") @PreAuthorize("hasAuthority('office:outsource:view')")
  public ApiResponse<List<OutsourceResponse>> outsourcing() { return ApiResponse.ok(service.listOutsource()); }
  @PostMapping("/outsourcing") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('office:outsource:create')")
  public ApiResponse<OutsourceResponse> createOutsource(@Valid @RequestBody CreateOutsourceRequest request) { return ApiResponse.ok(service.createOutsource(request)); }
  @PostMapping("/outsourcing/{id}/complete") @PreAuthorize("hasAuthority('office:outsource:complete')")
  public ApiResponse<OutsourceResponse> completeOutsource(@PathVariable UUID id, @Valid @RequestBody CompleteOutsourceRequest request) { return ApiResponse.ok(service.completeOutsource(id, request)); }
  @GetMapping("/suppliers") @PreAuthorize("hasAnyAuthority('office:view', 'office:outsource:create')")
  public ApiResponse<List<SupplierOption>> suppliers() { return ApiResponse.ok(service.suppliers()); }
  @GetMapping("/documents") @PreAuthorize("hasAuthority('office:document:view')")
  public ApiResponse<List<DocumentResponse>> documents() { return ApiResponse.ok(service.listDocuments()); }
  @GetMapping("/documents/search") @PreAuthorize("hasAuthority('office:document:view')")
  public ApiResponse<Page<DocumentResponse>> searchDocuments(
      @RequestParam(required = false) String bizType,
      @RequestParam(required = false) UUID bizId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (size < 1 || size > 100) { size = 20; }
    return ApiResponse.ok(service.listDocuments(bizType, bizId, page, size));
  }
  @GetMapping("/documents/by-biz") @PreAuthorize("hasAuthority('office:document:view')")
  public ApiResponse<List<DocumentResponse>> documentsByBiz(
      @RequestParam String bizType, @RequestParam(required = false) UUID bizId) {
    return ApiResponse.ok(service.listDocumentsByBiz(bizType, bizId));
  }
  @GetMapping("/documents/count") @PreAuthorize("hasAuthority('office:document:view')")
  public ApiResponse<Long> documentCount(
      @RequestParam String bizType, @RequestParam(required = false) UUID bizId) {
    return ApiResponse.ok(service.getDocumentCount(bizType, bizId));
  }
  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('office:document:upload')")
  public ApiResponse<DocumentResponse> upload(@RequestParam String bizType, @RequestParam(required = false) UUID bizId, @RequestPart MultipartFile file) { return ApiResponse.ok(service.storeDocument(bizType, bizId, file)); }
  @PostMapping(value = "/documents/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('office:document:upload')")
  public ApiResponse<List<DocumentResponse>> uploadBatch(@RequestParam String bizType, @RequestParam(required = false) UUID bizId, @RequestPart List<MultipartFile> files) { return ApiResponse.ok(service.storeDocuments(bizType, bizId, files)); }
  @GetMapping("/documents/{id}/download") @PreAuthorize("hasAuthority('office:document:view')")
  public ResponseEntity<Resource> download(@PathVariable UUID id) {
    DocumentFile item = service.requireDocument(id); Resource resource = service.loadDocument(item);
    return ResponseEntity.ok().contentType(item.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(item.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(item.getFileName(), StandardCharsets.UTF_8).build().toString())
        .body(resource);
  }
  @GetMapping("/documents/{id}/preview") @PreAuthorize("hasAuthority('office:document:view')")
  public ResponseEntity<Resource> preview(@PathVariable UUID id) {
    DocumentFile item = service.requireDocument(id); Resource resource = service.loadDocumentForPreview(item);
    String contentType = item.getContentType() != null && (item.getContentType().startsWith("image/") || item.getContentType().equals("application/pdf"))
        ? item.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(item.getFileName(), StandardCharsets.UTF_8).build().toString())
        .body(resource);
  }
  @PutMapping("/documents/{id}") @PreAuthorize("hasAuthority('office:document:upload')")
  public ApiResponse<DocumentResponse> updateDocument(@PathVariable UUID id, @RequestParam String fileName) {
    return ApiResponse.ok(service.updateDocumentName(id, fileName));
  }
  @DeleteMapping("/documents/{id}") @PreAuthorize("hasAuthority('office:document:delete')")
  public ApiResponse<Void> deleteDocument(@PathVariable UUID id) { service.deleteDocument(id); return ApiResponse.ok(); }
  @DeleteMapping("/documents/by-biz") @PreAuthorize("hasAuthority('office:document:delete')")
  public ApiResponse<Void> deleteDocumentsByBiz(@RequestParam String bizType, @RequestParam UUID bizId) {
    service.deleteDocumentsByBiz(bizType, bizId); return ApiResponse.ok();
  }
  @GetMapping("/notifications") @PreAuthorize("hasAuthority('office:notification:view')")
  public ApiResponse<List<NotificationResponse>> notifications() { return ApiResponse.ok(service.notifications()); }
  @PostMapping("/notifications/refresh") @PreAuthorize("hasAuthority('office:notification:view')")
  public ApiResponse<Integer> refreshNotifications() {
    return ApiResponse.ok(officeService.refreshNotifications());
  }

  @GetMapping("/audits") @PreAuthorize("hasAuthority('office:audit:view')")
  public ApiResponse<java.util.List> listAudits() {
    return ApiResponse.ok(officeService.listAudits());
  }

  @PostMapping("/notifications/{id}/read") @PreAuthorize("hasAuthority('office:notification:view')")
  public ApiResponse<NotificationResponse> readNotification(@PathVariable UUID id) { return ApiResponse.ok(service.readNotification(id)); }
}
