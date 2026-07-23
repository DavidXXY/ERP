package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import com.company.ops.api.modules.procurement.domain.ProcurementCostAllocation;
import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseRequestRequest;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementCostAllocationRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestApprovalRecordRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcurementServiceTest {

  @Mock private SupplierRepository supplierRepository;
  @Mock private PurchaseRequestRepository requestRepository;
  @Mock private PurchaseRequestApprovalRecordRepository requestApprovalRepository;
  @Mock private PurchaseOrderRepository orderRepository;
  @Mock private GoodsReceiptRepository receiptRepository;
  @Mock private ProcurementPayableRepository payableRepository;
  @Mock private ProcurementCostAllocationRepository costAllocationRepository;
  @Mock private InventoryPartRepository partRepository;
  @Mock private StockMovementRepository movementRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectCostEntryRepository projectCostRepository;
  @Mock private SystemOrganizationRepository organizationRepository;
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

    when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
    when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
    when(receiptRepository.countByOrderId(order.getId())).thenReturn(0L);
    when(receiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> {
      GoodsReceipt receipt = invocation.getArgument(0);
      receipt.setId(UUID.randomUUID());
      return receipt;
    });
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

  private InventoryPart part(String name) {
    InventoryPart part = new InventoryPart();
    part.setId(UUID.randomUUID());
    part.setCode("WL-001");
    part.setName(name);
    return part;
  }
}
