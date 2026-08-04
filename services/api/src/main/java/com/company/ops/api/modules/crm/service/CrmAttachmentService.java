package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.common.storage.FileStorageService.FilePolicy;
import com.company.ops.api.modules.crm.domain.CrmAttachment;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.CrmAttachmentRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.QuotePlanRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CrmAttachmentService {

  private static final long MAX_SIZE = 30L * 1024 * 1024;
  private static final Set<String> ALLOWED = Set.of(
    ".jpg", ".jpeg", ".png", ".webp", ".pdf", ".doc", ".docx", ".xls", ".xlsx"
  );
  private static final FilePolicy POLICY = new FilePolicy(
      MAX_SIZE,
      ALLOWED,
      "附件不能超过30MB",
      "仅支持图片、PDF、Word、Excel文件",
      true
  );

  private final FileStorageService storageService;
  private final CrmAttachmentRepository repository;
  private final DeleteGovernanceService deleteGovernanceService;
  private final CustomerRepository customerRepository;
  private final OpportunityRepository opportunityRepository;
  private final QuotePlanRepository quotePlanRepository;
  private final ServiceContractRepository contractRepository;
  private final ReceivableRepository receivableRepository;
  private final DataScopeService dataScopeService;

  public CrmAttachmentService(
    FileStorageService storageService,
    CrmAttachmentRepository repository,
    DeleteGovernanceService deleteGovernanceService,
    CustomerRepository customerRepository,
    OpportunityRepository opportunityRepository,
    QuotePlanRepository quotePlanRepository,
    ServiceContractRepository contractRepository,
    ReceivableRepository receivableRepository,
    DataScopeService dataScopeService
  ) {
    this.storageService = storageService;
    this.repository = repository;
    this.deleteGovernanceService = deleteGovernanceService;
    this.customerRepository = customerRepository;
    this.opportunityRepository = opportunityRepository;
    this.quotePlanRepository = quotePlanRepository;
    this.contractRepository = contractRepository;
    this.receivableRepository = receivableRepository;
    this.dataScopeService = dataScopeService;
  }

  public record AttachmentDto(
    UUID id, String entityType, UUID entityId, String attachmentType,
    String fileName, long fileSize, String mimeType, String uploadedAt, String uploadedBy
  ) {}

  public List<AttachmentDto> listByEntity(String entityType, UUID entityId) {
    String normalizedType = authorize(entityType, entityId, false);
    return repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(normalizedType, entityId)
      .stream()
      .filter(item -> !deleteGovernanceService.isHidden("CRM_ATTACHMENT", item.getId()))
      .map(this::toDto).toList();
  }

  public List<AttachmentDto> listByEntityAndType(String entityType, UUID entityId, String attachmentType) {
    String normalizedType = authorize(entityType, entityId, false);
    return repository.findByEntityTypeAndEntityIdAndAttachmentType(normalizedType, entityId, attachmentType)
      .stream()
      .filter(item -> !deleteGovernanceService.isHidden("CRM_ATTACHMENT", item.getId()))
      .map(this::toDto).toList();
  }

  @Transactional
  public AttachmentDto upload(String entityType, UUID entityId, String attachmentType, MultipartFile file, String uploadedBy) {
    String normalizedType = authorize(entityType, entityId, true);
    var stored = storageService.store(file, "crm", POLICY);
    CrmAttachment attachment = new CrmAttachment();
    attachment.setEntityType(normalizedType);
    attachment.setEntityId(entityId);
    attachment.setAttachmentType(attachmentType);
    attachment.setFileName(stored.originalName());
    attachment.setFilePath(stored.relativePath());
    attachment.setFileSize(stored.sizeBytes());
    attachment.setMimeType(stored.contentType());
    attachment.setUploadedBy(uploadedBy);
    attachment.setUploadedAt(OffsetDateTime.now());
    return toDto(repository.save(attachment));
  }

  @Transactional
  public void delete(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    authorize(att.getEntityType(), att.getEntityId(), true);
    if (!deleteGovernanceService.allowPhysicalDelete("CRM_ATTACHMENT", id, att.getFileName())) return;
    storageService.delete(att.getFilePath());
    repository.delete(att);
  }

  public org.springframework.core.io.Resource load(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    authorize(att.getEntityType(), att.getEntityId(), false);
    if (deleteGovernanceService.isHidden("CRM_ATTACHMENT", id)) throw new BusinessException("附件不存在");
    return storageService.load(att.getFilePath());
  }

  public String temporaryUrl(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    authorize(att.getEntityType(), att.getEntityId(), false);
    if (deleteGovernanceService.isHidden("CRM_ATTACHMENT", id)) throw new BusinessException("附件不存在");
    return storageService.temporaryUrl(att.getFilePath());
  }

  public String getFileName(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    authorize(att.getEntityType(), att.getEntityId(), false);
    if (deleteGovernanceService.isHidden("CRM_ATTACHMENT", id)) throw new BusinessException("附件不存在");
    return att.getFileName();
  }

  public String getMimeType(UUID id) {
    CrmAttachment att = repository.findById(id).orElseThrow(() -> new BusinessException("附件不存在"));
    authorize(att.getEntityType(), att.getEntityId(), false);
    return att.getMimeType() == null || att.getMimeType().isBlank() ? "application/octet-stream" : att.getMimeType();
  }

  private String authorize(String entityType, UUID entityId, boolean mutation) {
    if (entityType == null || entityId == null) throw new BusinessException("附件所属对象不能为空");
    String normalized = entityType.trim().toUpperCase(java.util.Locale.ROOT);
    String requiredPermission;
    UUID customerId;
    switch (normalized) {
      case "CUSTOMER" -> {
        requiredPermission = mutation ? "crm:customer:update" : "crm:customer:view";
        customerId = entityId;
      }
      case "OPPORTUNITY" -> {
        requiredPermission = mutation ? "crm:opportunity:update" : "crm:opportunity:view";
        customerId = opportunityRepository.findById(entityId)
            .orElseThrow(() -> new BusinessException("商机不存在")).getCustomerId();
      }
      case "QUOTE" -> {
        requiredPermission = mutation ? "crm:quote:update" : "crm:quote:view";
        customerId = quotePlanRepository.findById(entityId)
            .orElseThrow(() -> new BusinessException("报价不存在")).getCustomerId();
      }
      case "CONTRACT" -> {
        requiredPermission = mutation ? "crm:contract:update" : "crm:contract:view";
        customerId = contractRepository.findById(entityId)
            .orElseThrow(() -> new BusinessException("合同不存在")).getCustomerId();
      }
      case "RECEIVABLE" -> {
        requiredPermission = mutation ? "crm:receivable:update" : "finance:receivable:view";
        customerId = receivableRepository.findById(entityId)
            .orElseThrow(() -> new BusinessException("应收记录不存在")).getCustomerId();
      }
      default -> throw new BusinessException("不支持的附件所属对象");
    }
    if (!dataScopeService.hasAuthority(requiredPermission)) throw new AccessDeniedException("无权访问该类附件");
    var customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new BusinessException("附件关联客户不存在"));
    if (!dataScopeService.canViewOwner(customer.getOwnerUserId())) throw new AccessDeniedException("无权访问该客户附件");
    return normalized;
  }

  private AttachmentDto toDto(CrmAttachment a) {
    return new AttachmentDto(
      a.getId(), a.getEntityType(), a.getEntityId(), a.getAttachmentType(),
      a.getFileName(), a.getFileSize() != null ? a.getFileSize() : 0L, a.getMimeType(),
      a.getUploadedAt() != null ? a.getUploadedAt().toString() : a.getCreatedAt() != null ? a.getCreatedAt().toString() : "",
      a.getUploadedBy()
    );
  }
}
