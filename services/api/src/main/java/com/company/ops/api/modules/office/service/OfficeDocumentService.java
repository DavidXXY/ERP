package com.company.ops.api.modules.office.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import com.company.ops.api.modules.office.domain.DocumentFile;
import com.company.ops.api.modules.office.dto.OfficeDtos.DocumentResponse;
import com.company.ops.api.modules.office.repository.DocumentFileRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 档案（DocumentFile）管理。
 * 从 OfficeService 拆分而来，只负责档案的存储、查询、预览与删除。
 */
@Service
public class OfficeDocumentService {

  private static final FilePolicy DOCUMENT_POLICY = new FilePolicy(
      20L * 1024 * 1024,
      Set.of(".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt", ".zip", ".dwg", ".dxf"),
      "单个文件不能超过20MB",
      "仅支持图片、PDF、Word、Excel、TXT、ZIP、DWG 或 DXF 档案",
      false
  );

  private final DocumentFileRepository documentRepository;
  private final FileStorageService storageService;
  private final DeleteGovernanceService deleteGovernanceService;

  public OfficeDocumentService(
      DocumentFileRepository documentRepository,
      FileStorageService storageService,
      DeleteGovernanceService deleteGovernanceService) {
    this.documentRepository = documentRepository;
    this.storageService = storageService;
    this.deleteGovernanceService = deleteGovernanceService;
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listDocuments() {
    return documentRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDocument).toList();
  }

  @Transactional(readOnly = true)
  public Page<DocumentResponse> listDocuments(
      String bizType, UUID bizId, int page, int size) {
    Pageable p = PageRequest.of(page, size);
    Page<DocumentFile> source;
    if (bizType != null && bizId != null) {
      source = documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId, p);
    } else if (bizType != null) {
      source = documentRepository.findByBizTypeOrderByCreatedAtDesc(bizType, p);
    } else {
      source = documentRepository.findAllByOrderByCreatedAtDesc(p);
    }
    List<DocumentFile> visible = deleteGovernanceService.visible("OFFICE_DOCUMENT", source.getContent(), DocumentFile::getId);
    return new org.springframework.data.domain.PageImpl<>(
        visible.stream().map(this::toDocument).toList(),
        p,
        visible.size()
    );
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> listDocumentsByBiz(String bizType, UUID bizId) {
    return documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId).stream()
        .filter(item -> !deleteGovernanceService.isHidden("OFFICE_DOCUMENT", item.getId()))
        .map(this::toDocument).toList();
  }

  @Transactional(readOnly = true)
  public long getDocumentCount(String bizType, UUID bizId) {
    return documentRepository.countByBizTypeAndBizId(bizType, bizId);
  }

  @Transactional
  public DocumentResponse storeDocument(String bizType, UUID bizId, MultipartFile file) {
    var stored = storageService.store(file, "office", DOCUMENT_POLICY);
    DocumentFile item = new DocumentFile(); item.setBizType(bizType); item.setBizId(bizId); item.setFileName(stored.originalName());
    item.setObjectKey(stored.objectKey()); item.setContentType(stored.contentType()); item.setSizeBytes(stored.sizeBytes());
    item.setUploadedBy(currentName());
    return toDocument(documentRepository.save(item));
  }

  @Transactional
  public List<DocumentResponse> storeDocuments(String bizType, UUID bizId, List<MultipartFile> files) {
    if (files == null || files.isEmpty()) throw new BusinessException("上传文件列表不能为空");
    List<DocumentResponse> results = new ArrayList<>();
    for (MultipartFile file : files) {
      results.add(storeDocument(bizType, bizId, file));
    }
    return results;
  }

  @Transactional
  public void deleteDocument(UUID id) {
    DocumentFile item = documentRepository.findById(id).orElseThrow(() -> new BusinessException("档案不存在"));
    if (!deleteGovernanceService.allowPhysicalDelete("OFFICE_DOCUMENT", id, item.getFileName())) return;
    storageService.deleteInNamespace("office", item.getObjectKey());
    documentRepository.delete(item);
  }

  @Transactional
  public void deleteDocumentsByBiz(String bizType, UUID bizId) {
    List<DocumentFile> items = documentRepository.findByBizTypeAndBizIdOrderByCreatedAtDesc(bizType, bizId);
    for (DocumentFile item : items) {
      if (!deleteGovernanceService.allowPhysicalDelete("OFFICE_DOCUMENT", item.getId(), item.getFileName())) continue;
      storageService.deleteInNamespace("office", item.getObjectKey());
      documentRepository.delete(item);
    }
  }

  @Transactional
  public DocumentResponse updateDocumentName(UUID id, String newName) {
    if (newName == null || newName.isBlank()) throw new BusinessException("文件名不能为空");
    if (newName.length() > 240) throw new BusinessException("文件名不能超过240个字符");
    DocumentFile item = documentRepository.findById(id).orElseThrow(() -> new BusinessException("档案不存在"));
    item.setFileName(newName.trim());
    return toDocument(documentRepository.save(item));
  }

  @Transactional(readOnly = true)
  public DocumentFile requireDocument(UUID id) {
    DocumentFile item = documentRepository.findById(id).orElseThrow(() -> new BusinessException("档案不存在"));
    if (deleteGovernanceService.isHidden("OFFICE_DOCUMENT", id)) throw new BusinessException("档案不存在");
    return item;
  }

  public Resource loadDocument(DocumentFile item) {
    return storageService.load("office/" + item.getObjectKey());
  }

  public Resource loadDocumentForPreview(DocumentFile item) {
    return storageService.load("office/" + item.getObjectKey());
  }

  private DocumentResponse toDocument(DocumentFile item) { return new DocumentResponse(item.getId(), item.getBizType(), item.getBizId(), item.getFileName(), item.getContentType(), item.getSizeBytes(), item.getUploadedBy(), item.getCreatedAt()); }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }

}
