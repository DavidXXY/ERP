package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderChangeRepository;
import com.company.ops.api.modules.procurement.repository.SupplierChangeRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceSubmissionRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalDocumentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPerformanceReviewRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端“门户协同”待办聚合：集中统计供应商门户与采购端之间的待办事项，
 * 便于采购员在一个入口掌握需要处理或跟进的门户协同工作。
 */
@Service
public class PortalCollaborationService {
  private final SupplierPortalAccountRepository accounts;
  private final SupplierRepository suppliers;
  private final SupplierPortalDocumentRepository documents;
  private final SupplierQuotationRepository quotes;
  private final PurchaseOrderChangeRepository orderChanges;
  private final SupplierInvoiceSubmissionRepository invoiceSubmissions;
  private final GoodsReceiptRepository receipts;
  private final SupplierChangeRequestRepository supplierChanges;
  private final SupplierPerformanceReviewRepository performanceReviews;

  public PortalCollaborationService(
      SupplierPortalAccountRepository accounts,
      SupplierRepository suppliers,
      SupplierPortalDocumentRepository documents,
      SupplierQuotationRepository quotes,
      PurchaseOrderChangeRepository orderChanges,
      SupplierInvoiceSubmissionRepository invoiceSubmissions,
      GoodsReceiptRepository receipts,
      SupplierChangeRequestRepository supplierChanges,
      SupplierPerformanceReviewRepository performanceReviews
  ) {
    this.accounts = accounts;
    this.suppliers = suppliers;
    this.documents = documents;
    this.quotes = quotes;
    this.orderChanges = orderChanges;
    this.invoiceSubmissions = invoiceSubmissions;
    this.receipts = receipts;
    this.supplierChanges = supplierChanges;
    this.performanceReviews = performanceReviews;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> summary() {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("pendingAccounts", accounts.countByStatus("PENDING_REVIEW"));
    view.put("pendingAdmissions", suppliers.countByAdmissionStatus("PENDING"));
    view.put("pendingDocuments", documents.countByReviewStatus("PENDING"));
    view.put("pendingQuoteConfirmations", quotes
        .countBySubmissionSourceAndSubmissionStatusAndConfirmedByAccountIdIsNull(
            "INTERNAL_ENTRY", "SUBMITTED"));
    view.put("pendingChangeResponses", orderChanges.countByStatusAndSupplierResponseIsNull("PENDING"));
    view.put("pendingChangeDecisions", orderChanges.countByStatusAndSupplierResponseIsNotNull("PENDING"));
    view.put("pendingInvoiceSubmissions", invoiceSubmissions.countByStatus("PENDING"));
    view.put("pendingAppeals", receipts.countByAppealStatus("PENDING"));
    view.put("pendingSupplierChanges", supplierChanges.countByStatus("PENDING"));
    view.put("pendingPerformanceAppeals", performanceReviews.countByAppealStatus("PENDING"));
    view.put("updatedAt", OffsetDateTime.now());
    return view;
  }
}
