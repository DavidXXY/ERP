package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderChangeRepository;
import com.company.ops.api.modules.procurement.repository.SupplierChangeRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceSubmissionRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalAccountRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPortalDocumentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.repository.SupplierPerformanceReviewRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalCollaborationServiceTest {
  @Mock private SupplierPortalAccountRepository accounts;
  @Mock private SupplierRepository suppliers;
  @Mock private SupplierPortalDocumentRepository documents;
  @Mock private SupplierQuotationRepository quotes;
  @Mock private PurchaseOrderChangeRepository orderChanges;
  @Mock private SupplierInvoiceSubmissionRepository invoiceSubmissions;
  @Mock private GoodsReceiptRepository receipts;
  @Mock private SupplierChangeRequestRepository supplierChanges;
  @Mock private SupplierPerformanceReviewRepository performanceReviews;
  @InjectMocks private PortalCollaborationService service;

  @Test
  void summaryAggregatesPendingPortalTodos() {
    when(accounts.countByStatus("PENDING_REVIEW")).thenReturn(1L);
    when(suppliers.countByAdmissionStatus("PENDING")).thenReturn(2L);
    when(documents.countByReviewStatus("PENDING")).thenReturn(3L);
    when(quotes.countBySubmissionSourceAndSubmissionStatusAndConfirmedByAccountIdIsNull(
        "INTERNAL_ENTRY", "SUBMITTED")).thenReturn(4L);
    when(orderChanges.countByStatusAndSupplierResponseIsNull("PENDING")).thenReturn(5L);
    when(orderChanges.countByStatusAndSupplierResponseIsNotNull("PENDING")).thenReturn(6L);
    when(invoiceSubmissions.countByStatus("PENDING")).thenReturn(7L);
    when(receipts.countByAppealStatus("PENDING")).thenReturn(8L);
    when(supplierChanges.countByStatus("PENDING")).thenReturn(9L);
    when(performanceReviews.countByAppealStatus("PENDING")).thenReturn(10L);

    Map<String, Object> summary = service.summary();

    assertThat(summary.get("pendingAccounts")).isEqualTo(1L);
    assertThat(summary.get("pendingAdmissions")).isEqualTo(2L);
    assertThat(summary.get("pendingDocuments")).isEqualTo(3L);
    assertThat(summary.get("pendingQuoteConfirmations")).isEqualTo(4L);
    assertThat(summary.get("pendingChangeResponses")).isEqualTo(5L);
    assertThat(summary.get("pendingChangeDecisions")).isEqualTo(6L);
    assertThat(summary.get("pendingInvoiceSubmissions")).isEqualTo(7L);
    assertThat(summary.get("pendingAppeals")).isEqualTo(8L);
    assertThat(summary.get("pendingSupplierChanges")).isEqualTo(9L);
    assertThat(summary.get("pendingPerformanceAppeals")).isEqualTo(10L);
  }
}
