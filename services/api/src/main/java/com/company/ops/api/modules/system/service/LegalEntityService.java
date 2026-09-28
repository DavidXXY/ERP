package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.system.domain.LegalEntity;
import com.company.ops.api.modules.system.dto.LegalEntityDtos.*;
import com.company.ops.api.modules.system.repository.LegalEntityRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalEntityService {

  private final LegalEntityRepository repository;

  public LegalEntityService(LegalEntityRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Page<LegalEntityResponse> list(Pageable pageable) {
    return repository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
  }

  @Transactional
  public LegalEntityResponse create(CreateLegalEntityRequest request) {
    if (repository.existsByCode(request.code().trim())) throw new BusinessException("法人编码已存在");
    LegalEntity entity = new LegalEntity();
    entity.setCode(request.code().trim());
    entity.setName(request.name().trim());
    entity.setEntityType(request.entityType() == null || request.entityType().isBlank() ? "LEGAL_ENTITY" : request.entityType());
    entity.setCurrency(request.currency() == null || request.currency().isBlank() ? "CNY" : request.currency());
    entity.setActive(request.active());
    entity.setRemark(request.remark());
    return toResponse(repository.save(entity));
  }

  @Transactional
  public LegalEntityResponse update(UUID id, CreateLegalEntityRequest request) {
    LegalEntity entity = repository.findById(id).orElseThrow(() -> new BusinessException("法人不存在"));
    entity.setName(request.name().trim());
    entity.setEntityType(request.entityType() == null || request.entityType().isBlank() ? "LEGAL_ENTITY" : request.entityType());
    entity.setCurrency(request.currency() == null || request.currency().isBlank() ? "CNY" : request.currency());
    entity.setActive(request.active());
    entity.setRemark(request.remark());
    return toResponse(repository.save(entity));
  }

  private LegalEntityResponse toResponse(LegalEntity e) {
    return new LegalEntityResponse(e.getId(), e.getCode(), e.getName(), e.getEntityType(), e.getCurrency(), e.isActive(), e.getRemark());
  }
}