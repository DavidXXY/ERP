package com.company.ops.api.modules.crm.controller;

import com.company.ops.api.common.api.ApiResponse;
import com.company.ops.api.modules.crm.service.CrmAttachmentService;
import com.company.ops.api.modules.crm.service.CrmAttachmentService.AttachmentDto;
import com.company.ops.api.modules.system.security.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/crm/attachments")
public class CrmAttachmentController {

  private final CrmAttachmentService service;

  public CrmAttachmentController(CrmAttachmentService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyAuthority('crm:contract:view', 'crm:quote:view', 'crm:opportunity:view', 'crm:customer:view')")
  public ApiResponse<List<AttachmentDto>> list(
      @RequestParam String entityType, @RequestParam UUID entityId) {
    return ApiResponse.ok(service.listByEntity(entityType, entityId));
  }

  @GetMapping("/by-type")
  @PreAuthorize("hasAnyAuthority('crm:contract:view', 'crm:quote:view', 'crm:opportunity:view', 'crm:customer:view')")
  public ApiResponse<List<AttachmentDto>> listByType(
      @RequestParam String entityType, @RequestParam UUID entityId,
      @RequestParam(required = false) String attachmentType) {
    if (attachmentType != null && !attachmentType.isEmpty()) {
      return ApiResponse.ok(service.listByEntityAndType(entityType, entityId, attachmentType));
    }
    return ApiResponse.ok(service.listByEntity(entityType, entityId));
  }

  @PostMapping("/upload")
  @PreAuthorize("hasAnyAuthority('crm:contract:update', 'crm:quote:update', 'crm:quote:convert', 'crm:opportunity:update', 'crm:customer:update')")
  public ApiResponse<AttachmentDto> upload(
      @RequestParam String entityType, @RequestParam UUID entityId,
      @RequestParam(required = false) String attachmentType,
      @RequestParam MultipartFile file) {
    UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    return ApiResponse.ok(service.upload(entityType, entityId, attachmentType, file, principal.displayName()));
  }

  @GetMapping("/{id}/download")
  @PreAuthorize("hasAnyAuthority('crm:contract:view', 'crm:quote:view', 'crm:opportunity:view', 'crm:customer:view')")
  public void download(@PathVariable UUID id, HttpServletResponse response) {
    try {
      String fileName = service.getFileName(id);
      Resource resource = service.load(id);
      response.setContentType(service.getMimeType(id));
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
          "inline; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
      try (var is = resource.getInputStream()) {
        is.transferTo(response.getOutputStream());
      }
    } catch (Exception e) {
      response.setStatus(500);
    }
  }

  @GetMapping("/{id}/temporary-url")
  @PreAuthorize("hasAnyAuthority('crm:contract:view', 'crm:quote:view', 'crm:opportunity:view', 'crm:customer:view')")
  public ApiResponse<String> temporaryUrl(@PathVariable UUID id) {
    String url = service.temporaryUrl(id);
    return ApiResponse.ok(url == null ? "" : url);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('crm:contract:delete')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ApiResponse.ok(null);
  }
}
