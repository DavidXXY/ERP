package com.company.ops.api.modules.qualification.controller;

import com.company.ops.api.modules.qualification.service.QualificationAttachmentService;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qualification-files")
public class QualificationAttachmentController {
  private final QualificationAttachmentService attachmentService;

  public QualificationAttachmentController(QualificationAttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  @GetMapping("/{objectKey}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Resource> download(@PathVariable String objectKey) {
    Resource resource = attachmentService.load(objectKey);
    String contentType = contentType(objectKey);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(objectKey, StandardCharsets.UTF_8).build().toString())
        .body(resource);
  }

  private String contentType(String objectKey) {
    String lower = objectKey.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
    if (lower.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
    if (lower.endsWith(".webp")) return "image/webp";
    if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
    return MediaType.APPLICATION_OCTET_STREAM_VALUE;
  }
}
