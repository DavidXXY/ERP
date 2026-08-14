package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.storage.FileStorageService;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.MaterialCategory;
import com.company.ops.api.modules.procurement.domain.ProcurementContract;
import com.company.ops.api.modules.procurement.domain.ProcurementOrderDocument;
import com.company.ops.api.modules.procurement.domain.ProcurementCostAllocation;
import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.PurchaseRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierCategory;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseRequestRequest;
import com.company.ops.api.modules.procurement.dto.CreateSupplierRequest;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.ReviewSupplierAdmissionRequest;
import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.MaterialCategoryRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementContractRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementOrderDocumentRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementCostAllocationRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementReturnOrderRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRequestRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationLineRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestApprovalRecordRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementShipmentRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementApprovalRuleRepository;
import com.company.ops.api.modules.procurement.repository.FrameworkAgreementRepository;
import com.company.ops.api.modules.procurement.repository.FrameworkAgreementItemRepository;import com.company.ops.api.modules.procurement.repository.SupplierCategoryRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProcurementServiceTest {

  @Mock private SupplierRepository supplierRepository;
  @Mock private CodeGenerator codeGenerator;
  @Mock private SupplierCategoryRepository supplierCategoryRepository;
  @Mock private PurchaseRequestRepository requestRepository;
  @Mock private PurchaseRequestApprovalRecordRepository requestApprovalRepository;
  @Mock private PurchaseOrderRepository orderRepository;
  @Mock private GoodsReceiptRepository receiptRepository;
  @Mock private ProcurementPayableRepository payableRepository;
  @Mock private ProcurementCostAllocationRepository costAllocationRepository;
  @Mock private ProcurementReturnOrderRepository returnRepository;
  @Mock private ProcurementContractRepository contractRepository;
  @Mock private ProcurementInquiryRequestRepository inquiryRequestRepository;
  @Mock private ProcurementInquiryRepository inquiryRepository;
  @Mock private SupplierQuotationRepository quoteRepository;
  @Mock private SupplierQuotationLineRepository quoteLineRepository;
  @Mock private InventoryPartRepository partRepository;
  @Mock private StockMovementRepository movementRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectCostEntryRepository projectCostRepository;
  @Mock private SystemOrganizationRepository organizationRepository;
  @Mock private MaterialCategoryRepository materialCategoryRepository;
  @Mock private ProcurementOrderDocumentRepository orderDocumentRepository;
  @Mock private ProcurementShipmentRepository shipmentRepository;
  @Mock private FileStorageService storage;
  @Mock private ProcurementArrivalService arrivals;
  @Mock private SupplierPortalNotifier portalNotifier;
  @Mock private ProcurementApprovalRuleRepository approvalRuleRepository;
  @Mock private FrameworkAgreementRepository frameworkAgreementRepository;
  @Mock private FrameworkAgreementItemRepository frameworkAgreementItemRepository;
  @InjectMocks private ProcurementService procurementService;

  @Test
  void projectPurchaseRequiresProject() {
    InventoryPart part = part("Pump");
    when(requestRepository.existsByCode("CGSQ-001")).thenReturn(false);
    when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));

    CreatePurchaseRequestRequest request = new CreatePurchaseRequestRequest(
        "CGSQ-001", "Buyer", part.getId(), null, BigDecimal.ONE,
        BigDecimal.ZERO, new BigDecimal("13"),
        LocalDate.now().plusDays(7), "Project equipment", ProcurementCostType.PROJECT, null, null
    );

    assertThatThrownBy(() -> procurementService.createPurchaseRequest(request))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void newSupplierAlwaysRequiresAdmissionApproval() {
    when(supplierRepository.existsByCode("GYS-NEW")).thenReturn(false);
    SupplierCategory category = new SupplierCategory();
    category.setName("Equipment");
    category.setEnabled(true);
    when(supplierCategoryRepository.findByNameIgnoreCase("Equipment"))
        .thenReturn(Optional.of(category));
    when(supplierRepository.save(any(Supplier.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = procurementService.createSupplier(
        completeSupplierRequest("GYS-NEW", "APPROVED"));

    assertThat(response.admissionStatus()).isEqualTo("PENDING");
    assertThat(response.admissionSubmittedAt()).isNotNull();
    assertThat(response.admissionReviewerName()).isNull();
  }

  @Test
  void admissionReviewActivatesCompleteSupplier() {
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setCode("GYS-READY");
    supplier.setName("Ready Supplier");
    supplier.setCategory("Equipment");
    supplier.setContactName("Contact");
    supplier.setPhone("13800000000");
    supplier.setSettlementTerms("Net 30");
    supplier.setLegalRepresentative("Legal Representative");
    supplier.setUnifiedSocialCreditCode("913100000000000000");
    supplier.setRegisteredAddress("Shanghai");
    supplier.setLicenseValidTo(LocalDate.now().plusYears(1));
    supplier.setTaxpayerType("General");
    supplier.setBankName("Bank");
    supplier.setBankAccount("6222000000000000");
    supplier.setAdmissionStatus("PENDING");
    supplier.setRiskStatus(SupplierRiskStatus.NORMAL);
    when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
    when(supplierRepository.save(any(Supplier.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var response = procurementService.reviewSupplierAdmission(
        supplier.getId(),
        new ReviewSupplierAdmissionRequest("APPROVED", "资料核验通过"));

    assertThat(response.admissionStatus()).isEqualTo("APPROVED");
    assertThat(response.admissionReviewedAt()).isNotNull();
    assertThat(response.admissionReviewComment()).isEqualTo("资料核验通过");
  }

  @Test
  void departmentPurchaseKeepsDepartmentSnapshot() {
    InventoryPart part = part("Laptop");
    SystemOrganization department = new SystemOrganization();
    department.setId(UUID.randomUUID());
    department.setCode("FINANCE_DEPARTMENT");
    department.setName("Finance Department");
    department.setType("DEPARTMENT");
    department.setEnabled(true);
    when(requestRepository.existsByCode("CGSQ-002")).thenReturn(false);
    when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
    when(organizationRepository.findById(department.getId())).thenReturn(Optional.of(department));
    when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = procurementService.createPurchaseRequest(new CreatePurchaseRequestRequest(
        "CGSQ-002", "Buyer", part.getId(), null, BigDecimal.valueOf(2),
        new BigDecimal("3000"), new BigDecimal("13"),
        LocalDate.now().plusDays(7), "Department office", ProcurementCostType.DEPARTMENT,
        null, department.getId()
    ));

    assertThat(response.costType()).isEqualTo(ProcurementCostType.DEPARTMENT);
    assertThat(response.costTargetId()).isEqualTo(department.getId());
    assertThat(response.costTargetCode()).isEqualTo("FINANCE_DEPARTMENT");
    assertThat(response.costTargetName()).isEqualTo("Finance Department");
  }

  @Test
  void projectArrivalWaitsForQualityInspectionBeforePostingCost() {
    UUID projectId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    InventoryPart part = part("Control module");
    part.setStockQty(BigDecimal.ZERO);
    part.setUnitCost(BigDecimal.ZERO);
    Project project = new Project();
    project.setId(projectId);
    project.setCode("XM-001");
    project.setName("Delivery project");
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    project.setStage(ProjectStage.CONSTRUCTION);
    project.setActualCost(BigDecimal.valueOf(500));
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setCode("GYS-001");
    supplier.setName("Equipment supplier");
    supplier.setRiskStatus(SupplierRiskStatus.NORMAL);
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setCode("CGDD-001");
    order.setRequestId(requestId);
    order.setSupplierId(supplier.getId());
    order.setPartId(part.getId());
    order.setPartName(part.getName());
    order.setOrderedQty(BigDecimal.valueOf(2));
    order.setReceivedQty(BigDecimal.ZERO);
    order.setUnitPrice(BigDecimal.valueOf(120));
    order.setOrderAmount(BigDecimal.valueOf(240));
    order.setTaxRate(BigDecimal.valueOf(13));
    order.setStatus(PurchaseOrderStatus.ORDERED);
    order.setCostType(ProcurementCostType.PROJECT);
    order.setProjectId(projectId);
    order.setCostTargetCode(project.getCode());
    order.setCostTargetName(project.getName());

    GoodsReceipt receipt = new GoodsReceipt();
    receipt.setId(UUID.randomUUID());
    receipt.setCode("DH-CGDD-001-01");
    receipt.setOrderId(order.getId());
    receipt.setPartId(part.getId());
    receipt.setQuantity(BigDecimal.ONE);
    receipt.setUnitPrice(order.getUnitPrice());
    receipt.setTaxRate(order.getTaxRate());
    receipt.setAmount(BigDecimal.valueOf(120));
    receipt.setReceivedDate(LocalDate.now());
    receipt.setDeliveryNo("SH-001");
    receipt.setReceiverName("Keeper");
    receipt.setPayableDueDate(LocalDate.now().plusDays(30));
    receipt.setInspectionStatus("PENDING");
    when(arrivals.register(org.mockito.ArgumentMatchers.eq(order.getId()), any()))
        .thenReturn(receipt);
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
    when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
    when(requestRepository.findById(requestId)).thenReturn(Optional.empty());
    when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));

    var result = procurementService.receiveOrder(order.getId(), new ReceivePurchaseOrderRequest(
        BigDecimal.ONE, LocalDate.now(), "SH-001", "Keeper", LocalDate.now().plusDays(30)
    ));

    assertThat(result.costAllocation()).isNull();
    assertThat(result.payable()).isNull();
    assertThat(result.receipt().inspectionStatus()).isEqualTo("PENDING");
    assertThat(project.getActualCost()).isEqualByComparingTo("500");
    verify(projectCostRepository, never()).save(any(ProjectCostEntry.class));
    verify(payableRepository, never()).save(any(ProcurementPayable.class));
  }

  @Test
  void orderWithRegisteredArrivalCannotBeCancelled() {
    PurchaseOrder order = new PurchaseOrder();
    order.setId(UUID.randomUUID());
    order.setStatus(PurchaseOrderStatus.ORDERED);
    GoodsReceipt receipt = new GoodsReceipt();
    receipt.setOrderId(order.getId());
    receipt.setInspectionStatus("PENDING");
    when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    when(receiptRepository.findByOrderId(order.getId())).thenReturn(List.of(receipt));

    assertThatThrownBy(() -> procurementService.cancelPurchaseOrder(order.getId()))
        .isInstanceOf(BusinessException.class).hasMessageContaining("已有到货记录");
    verify(orderRepository, never()).save(any(PurchaseOrder.class));
  }

  @Test
  void purchaseOrderAutoCreatesMaterialPartWhenRequestHasNoPart() {
    UUID requestId = UUID.randomUUID();
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setCode("GYS-001");
    supplier.setName("Equipment supplier");
    supplier.setRiskStatus(SupplierRiskStatus.NORMAL);
    supplier.setAdmissionStatus("APPROVED");
    PurchaseRequest purchaseRequest = new PurchaseRequest();
    purchaseRequest.setId(requestId);
    purchaseRequest.setPartName("111111");
    purchaseRequest.setUnitPrice(BigDecimal.valueOf(5));
    purchaseRequest.setTaxRate(new BigDecimal("13"));
    purchaseRequest.setQuantity(BigDecimal.ONE);
    purchaseRequest.setStatus(PurchaseRequestStatus.APPROVED);
    purchaseRequest.setApprovalStatus(ApprovalStatus.APPROVED);
    purchaseRequest.setCostType(ProcurementCostType.PROJECT);
    purchaseRequest.setExpectedDate(LocalDate.now().plusDays(7));

    when(orderRepository.existsByCode("CGDD-AUTO-001")).thenReturn(false);
    when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
    when(requestRepository.findById(requestId)).thenReturn(Optional.of(purchaseRequest));
    when(partRepository.findByNameIgnoreCase("111111")).thenReturn(List.of());
    when(materialCategoryRepository.findByNameIgnoreCase("未分类")).thenReturn(Optional.empty());
    when(materialCategoryRepository.save(any(MaterialCategory.class))).thenAnswer(invocation -> {
      MaterialCategory category = invocation.getArgument(0);
      category.setId(UUID.randomUUID());
      return category;
    });
    when(codeGenerator.generate("PART")).thenReturn("WL-AUTO-001");
    when(partRepository.save(any(InventoryPart.class))).thenAnswer(invocation -> {
      InventoryPart part = invocation.getArgument(0);
      part.setId(UUID.randomUUID());
      when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
      return part;
    });
    when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
      PurchaseOrder order = invocation.getArgument(0);
      order.setId(UUID.randomUUID());
      return order;
    });

    var response = procurementService.createPurchaseOrder(new CreatePurchaseOrderRequest(
        "CGDD-AUTO-001", supplier.getId(), requestId, BigDecimal.valueOf(5),
        new BigDecimal("13"), LocalDate.now().plusDays(7), BigDecimal.ONE,
        null, null, "CNY", BigDecimal.ZERO, "直接采购原因"));

    assertThat(purchaseRequest.getPartId()).isNotNull();
    assertThat(purchaseRequest.getPartName()).isEqualTo("111111");
    assertThat(response.partId()).isEqualTo(purchaseRequest.getPartId());
    verify(partRepository).save(any(InventoryPart.class));
    verify(materialCategoryRepository).save(any(MaterialCategory.class));
  }

  @Test
  void orderWithGenerateContractCreatesLinkedContractAndActivatesOnApproval() {
    UUID requestId = UUID.randomUUID();
    UUID partId = UUID.randomUUID();
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setCode("GYS-CT");
    supplier.setName("合格供应商");
    supplier.setRiskStatus(SupplierRiskStatus.NORMAL);
    supplier.setAdmissionStatus("APPROVED");
    PurchaseRequest purchaseRequest = new PurchaseRequest();
    purchaseRequest.setId(requestId);
    purchaseRequest.setPartId(partId);
    purchaseRequest.setPartName("Pump");
    purchaseRequest.setUnitPrice(BigDecimal.valueOf(10));
    purchaseRequest.setTaxRate(new BigDecimal("13"));
    purchaseRequest.setQuantity(BigDecimal.ONE);
    purchaseRequest.setStatus(PurchaseRequestStatus.APPROVED);
    purchaseRequest.setApprovalStatus(ApprovalStatus.APPROVED);
    purchaseRequest.setCostType(ProcurementCostType.DEPARTMENT);
    purchaseRequest.setExpectedDate(LocalDate.now().plusDays(7));

    when(orderRepository.existsByCode("CGDD-CT-001")).thenReturn(false);
    when(supplierRepository.findById(supplier.getId())).thenReturn(Optional.of(supplier));
    when(requestRepository.findById(requestId)).thenReturn(Optional.of(purchaseRequest));
    when(partRepository.findById(partId)).thenReturn(Optional.of(part("Pump")));
    when(codeGenerator.generate("CONTRACT")).thenReturn("HT-CT-001");
    when(contractRepository.findFirstByContractNoOrderByVersionNoDesc("HT-CT-001"))
        .thenReturn(Optional.empty());
    UUID contractUuid = UUID.randomUUID();
    ProcurementContract[] createdContracts = new ProcurementContract[1];
    when(contractRepository.save(any(ProcurementContract.class))).thenAnswer(invocation -> {
      ProcurementContract contract = invocation.getArgument(0);
      contract.setId(contractUuid);
      createdContracts[0] = contract;
      return contract;
    });
    when(contractRepository.findById(contractUuid)).thenAnswer(invocation ->
        createdContracts[0] == null ? Optional.empty() : Optional.of(createdContracts[0]));
    when(orderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
      PurchaseOrder order = invocation.getArgument(0);
      if (order.getId() == null) {
        order.setId(UUID.randomUUID());
      }
      return order;
    });

    var response = procurementService.createPurchaseOrder(new CreatePurchaseOrderRequest(
        "CGDD-CT-001", supplier.getId(), requestId, BigDecimal.valueOf(10),
        new BigDecimal("13"), LocalDate.now().plusDays(7), BigDecimal.ONE,
        null, null, "CNY", BigDecimal.ZERO, "直接采购",
        true, null, "采购合同-测试", "月结30天", LocalDate.now(), LocalDate.now().plusYears(1)));

    assertThat(response.contractId()).isNotNull();
    assertThat(response.contractNo()).isEqualTo("HT-CT-001");
    assertThat(response.contractName()).isEqualTo("采购合同-测试");
    assertThat(response.contractPaymentTerms()).isEqualTo("月结30天");
    assertThat(response.contractSourceType()).isEqualTo("FROM_ORDER");

    ArgumentCaptor<ProcurementContract> captor = ArgumentCaptor.forClass(ProcurementContract.class);
    verify(contractRepository).save(captor.capture());
    ProcurementContract savedContract = captor.getValue();
    assertThat(savedContract.getOrderId()).isEqualTo(response.id());
    assertThat(savedContract.getStatus()).isEqualTo("DRAFT");
    assertThat(savedContract.getApprovalStatus()).isEqualTo("PENDING");
    assertThat(savedContract.getAmount()).isEqualByComparingTo("10");
  }

  @Test
  void approvingOrderActivatesAutoGeneratedContract() {
    UUID orderId = UUID.randomUUID();
    UUID contractId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    UUID supplierId = UUID.randomUUID();
    PurchaseOrder order = new PurchaseOrder();
    order.setId(orderId);
    order.setCode("CGDD-APPROVE-001");
    order.setSupplierId(supplierId);
    order.setRequestId(requestId);
    order.setContractId(contractId);
    order.setStatus(PurchaseOrderStatus.DRAFT);
    order.setApprovalStatus(ApprovalStatus.PENDING);
    order.setSubmittedAt(java.time.OffsetDateTime.now());
    order.setOrderedQty(BigDecimal.ONE);
    order.setOrderAmount(BigDecimal.TEN);
    order.setUnitPrice(BigDecimal.TEN);
    order.setTaxRate(BigDecimal.valueOf(13));
    order.setCostType(ProcurementCostType.DEPARTMENT);
    order.setFreightAmount(BigDecimal.ZERO);
    order.setPartName("Pump");
    order.setPartId(UUID.randomUUID());
    order.setCostTargetCode("DPT");
    order.setCostTargetName("Dept");
    order.setReceivedQty(BigDecimal.ZERO);
    order.setCurrency("CNY");

    ProcurementContract contract = new ProcurementContract();
    contract.setId(contractId);
    contract.setContractNo("HT-APPROVE-001");
    contract.setSourceType("FROM_ORDER");
    contract.setStatus("DRAFT");
    contract.setApprovalStatus("PENDING");
    contract.setOrderId(orderId);
    contract.setSupplierId(supplierId);

    when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
    when(requestRepository.findById(requestId)).thenReturn(Optional.empty());
    when(supplierRepository.findById(supplierId)).thenReturn(Optional.empty());
    when(orderRepository.save(any(PurchaseOrder.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(contractRepository.save(any(ProcurementContract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = procurementService.approvePurchaseOrder(
        orderId, ApprovalStatus.APPROVED, "审批人", "同意");

    assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    assertThat(response.contractStatus()).isEqualTo("ACTIVE");
    assertThat(contract.getStatus()).isEqualTo("ACTIVE");
    assertThat(contract.getApprovalStatus()).isEqualTo("APPROVED");
    assertThat(contract.getApprovedAt()).isNotNull();
  }

  @Test
  void uploadOrderDocumentStoresAndLinksFileToOrder() throws Exception {
    UUID orderId = UUID.randomUUID();
    PurchaseOrder order = new PurchaseOrder();
    order.setId(orderId);
    order.setCode("CGDD-DOC-001");
    order.setSupplierId(UUID.randomUUID());
    order.setPartName("Pump");
    order.setPartId(UUID.randomUUID());
    order.setOrderedQty(BigDecimal.ONE);
    order.setUnitPrice(BigDecimal.TEN);
    order.setOrderAmount(BigDecimal.TEN);
    order.setStatus(PurchaseOrderStatus.ORDERED);
    order.setCostType(ProcurementCostType.DEPARTMENT);
    order.setCostTargetCode("DPT");
    order.setCostTargetName("Dept");
    order.setReceivedQty(BigDecimal.ZERO);
    order.setCurrency("CNY");
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

    MockMultipartFile file = new MockMultipartFile(
        "file", "采购合同.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
    FileStorageService.StoredFile stored = new FileStorageService.StoredFile(
        "采购合同.pdf", "abc123", "procurement-orders/abc123",
        ".pdf", "application/pdf", file.getSize(), null);
    when(storage.store(any(), eq("procurement-orders"), any())).thenReturn(stored);
    when(orderDocumentRepository.save(any(ProcurementOrderDocument.class))).thenAnswer(invocation -> {
      ProcurementOrderDocument document = invocation.getArgument(0);
      document.setId(UUID.randomUUID());
      return document;
    });

    var response = procurementService.uploadOrderDocument(orderId, file, "CONTRACT");

    assertThat(response.fileName()).isEqualTo("采购合同.pdf");
    assertThat(response.orderCode()).isEqualTo("CGDD-DOC-001");
    assertThat(response.contentType()).isEqualTo("application/pdf");
    assertThat(response.uploadedBy()).isEqualTo("系统");
    ArgumentCaptor<ProcurementOrderDocument> captor = ArgumentCaptor.forClass(ProcurementOrderDocument.class);
    verify(orderDocumentRepository).save(captor.capture());
    assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
    assertThat(captor.getValue().getObjectKey()).isEqualTo("abc123");
  }

  private InventoryPart part(String name) {
    InventoryPart part = new InventoryPart();
    part.setId(UUID.randomUUID());
    part.setCode("WL-001");
    part.setName(name);
    return part;
  }

  private CreateSupplierRequest completeSupplierRequest(
      String code, String requestedAdmissionStatus) {
    return new CreateSupplierRequest(
        code,
        "New Supplier",
        "Equipment",
        "Contact",
        "13800000000",
        "采购专员A",
        "Net 30",
        "Legal Representative",
        "913100000000000000",
        "1000",
        "Shanghai",
        "Equipment supply",
        LocalDate.now().plusYears(1),
        null,
        "General",
        "Bank",
        "6222000000000000",
        requestedAdmissionStatus,
        null,
        SupplierRiskStatus.NORMAL
    );
  }
}
