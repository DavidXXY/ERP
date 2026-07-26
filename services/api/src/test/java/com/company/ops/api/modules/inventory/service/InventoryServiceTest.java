package com.company.ops.api.modules.inventory.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.inventory.domain.InventoryIssueOrder;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.dto.CreateMaterialIssueRequest;
import com.company.ops.api.modules.inventory.dto.MaterialIssueLineRequest;
import com.company.ops.api.modules.inventory.repository.InventoryIssueLineRepository;
import com.company.ops.api.modules.inventory.repository.InventoryIssueOrderRepository;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.InventoryReturnLineRepository;
import com.company.ops.api.modules.inventory.repository.InventoryReturnOrderRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.project.service.ProjectCostLedgerService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
  @Mock private InventoryPartRepository parts;
  @Mock private StockMovementRepository movements;
  @Mock private InventoryIssueOrderRepository issues;
  @Mock private InventoryIssueLineRepository issueLines;
  @Mock private InventoryReturnOrderRepository returns;
  @Mock private InventoryReturnLineRepository returnLines;
  @Mock private ProjectRepository projects;
  @Mock private ProjectCostLedgerService costs;
  @Mock private CodeGenerator codes;
  private InventoryService service;

  @BeforeEach
  void setUp() {
    service = new InventoryService(parts, movements, issues, issueLines, returns, returnLines,
        projects, costs, codes);
  }

  @Test
  void generatedIssueCodeIsUsedByMovementAndProjectCost() {
    UUID projectId = UUID.randomUUID();
    UUID partId = UUID.randomUUID();
    LocalDate issueDate = LocalDate.of(2026, 7, 26);
    Project project = new Project();
    project.setId(projectId);
    project.setCode("XM-1");
    project.setApprovalStatus(ProjectApprovalStatus.APPROVED);
    project.setStage(ProjectStage.CONSTRUCTION);
    InventoryPart part = new InventoryPart();
    part.setId(partId);
    part.setName("测试物料");
    part.setStockQty(BigDecimal.TEN);
    part.setUnitCost(new BigDecimal("12.50"));

    when(codes.generate("ISSUE")).thenReturn("LD-20260726-0001");
    when(issues.existsByCode("LD-20260726-0001")).thenReturn(false);
    when(projects.findById(projectId)).thenReturn(Optional.of(project));
    when(parts.findByIdForUpdate(partId)).thenReturn(Optional.of(part));
    when(issues.save(any())).thenAnswer(invocation -> {
      InventoryIssueOrder order = invocation.getArgument(0);
      if (order.getId() == null) order.setId(UUID.randomUUID());
      return order;
    });
    when(movements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.createIssue(new CreateMaterialIssueRequest(null, projectId, issueDate, "领料人",
        "现场安装", List.of(new MaterialIssueLineRequest(partId, BigDecimal.ONE))));

    ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
    verify(movements).save(movement.capture());
    org.assertj.core.api.Assertions.assertThat(movement.getValue().getSourceNo())
        .isEqualTo("LD-20260726-0001");
    verify(costs).record(projectId, ProjectCostCategory.MATERIAL, ProjectCostSource.INVENTORY,
        "LD-20260726-0001", "项目领料：现场安装", new BigDecimal("12.50"), issueDate);
  }
}
