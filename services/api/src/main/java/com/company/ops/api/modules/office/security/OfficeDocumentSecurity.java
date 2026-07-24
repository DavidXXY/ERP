package com.company.ops.api.modules.office.security;

import com.company.ops.api.modules.office.repository.DocumentFileRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OfficeDocumentSecurity {

  private static final Set<String> PROCUREMENT_BIZ_TYPES = Set.of(
      "PURCHASE_REQUEST_BATCH",
      "PURCHASE_REQUEST",
      "SUPPLIER"
  );

  private final DocumentFileRepository documentRepository;

  public OfficeDocumentSecurity(DocumentFileRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  public boolean isProcurementBizType(String bizType) {
    return bizType != null && PROCUREMENT_BIZ_TYPES.contains(bizType);
  }

  public boolean isProcurementDocument(UUID documentId) {
    return documentId != null
        && documentRepository.findById(documentId)
            .map(document -> isProcurementBizType(document.getBizType()))
            .orElse(false);
  }
}
