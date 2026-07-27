package com.company.ops.api.modules.procurement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.dto.CreateMaterialMasterRequest;
import com.company.ops.api.modules.inventory.dto.UpdateMaterialMasterRequest;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.procurement.domain.MaterialCategory;
import com.company.ops.api.modules.procurement.repository.MaterialCategoryRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcurementMaterialServiceTest {

  @Mock private InventoryPartRepository parts;
  @Mock private MaterialCategoryRepository categories;
  @Mock private DeleteGovernanceService deleteGovernance;
  @Mock private CodeGenerator codes;
  private ProcurementMaterialService service;

  @BeforeEach
  void setUp() {
    service = new ProcurementMaterialService(parts, categories, deleteGovernance, codes);
  }

  @Test
  void newMaterialStartsWithZeroStockAndSelectedCategory() {
    MaterialCategory category = category("材料类");
    when(categories.findByNameIgnoreCase("材料类")).thenReturn(Optional.of(category));
    when(codes.generate("PART")).thenReturn("PART-001");
    when(parts.existsByCode("PART-001")).thenReturn(false);
    when(parts.save(any())).thenAnswer(invocation -> {
      InventoryPart part = invocation.getArgument(0);
      part.setId(UUID.randomUUID());
      return part;
    });

    var result = service.createMaterial(new CreateMaterialMasterRequest(
        null, "  网线  ", " CAT6 ", "材料类", new BigDecimal("20"),
        new BigDecimal("3.50")));

    assertThat(result.code()).isEqualTo("PART-001");
    assertThat(result.name()).isEqualTo("网线");
    assertThat(result.model()).isEqualTo("CAT6");
    assertThat(result.category()).isEqualTo("材料类");
    assertThat(result.stockQty()).isEqualByComparingTo("0");
  }

  @Test
  void updatingMaterialPreservesCodeAndStock() {
    UUID materialId = UUID.randomUUID();
    InventoryPart part = material(materialId, "PART-001", new BigDecimal("12.50"));
    when(parts.findByIdForUpdate(materialId)).thenReturn(Optional.of(part));
    when(categories.findByNameIgnoreCase("工程类")).thenReturn(Optional.of(category("工程类")));
    when(parts.save(part)).thenReturn(part);

    var result = service.updateMaterial(materialId, new UpdateMaterialMasterRequest(
        "新名称", null, "工程类", new BigDecimal("5"), new BigDecimal("11.20")));

    assertThat(result.code()).isEqualTo("PART-001");
    assertThat(result.stockQty()).isEqualByComparingTo("12.50");
    assertThat(result.name()).isEqualTo("新名称");
    assertThat(result.category()).isEqualTo("工程类");
  }

  @Test
  void materialWithStockCannotBeDeleted() {
    UUID materialId = UUID.randomUUID();
    InventoryPart part = material(materialId, "PART-001", BigDecimal.ONE);
    when(parts.findByIdForUpdate(materialId)).thenReturn(Optional.of(part));

    assertThatThrownBy(() -> service.deleteMaterial(materialId))
        .hasMessage("物料仍有库存，不能删除");
    verify(deleteGovernance, never()).allowPhysicalDelete(any(), any(), any());
    verify(parts, never()).delete(any());
  }

  @Test
  void nonAdminDeletionCreatesApprovalRequest() {
    UUID materialId = UUID.randomUUID();
    InventoryPart part = material(materialId, "PART-001", BigDecimal.ZERO);
    when(parts.findByIdForUpdate(materialId)).thenReturn(Optional.of(part));
    when(deleteGovernance.allowPhysicalDelete("MATERIAL", materialId, "PART-001 · 测试物料"))
        .thenReturn(false);

    var result = service.deleteMaterial(materialId);

    assertThat(result.status()).isEqualTo("PENDING_APPROVAL");
    verify(parts, never()).delete(any());
  }

  @Test
  void adminDeletionRemovesZeroStockMaterialImmediately() {
    UUID materialId = UUID.randomUUID();
    InventoryPart part = material(materialId, "PART-001", BigDecimal.ZERO);
    when(parts.findByIdForUpdate(materialId)).thenReturn(Optional.of(part));
    when(deleteGovernance.allowPhysicalDelete("MATERIAL", materialId, "PART-001 · 测试物料"))
        .thenReturn(true);

    var result = service.deleteMaterial(materialId);

    assertThat(result.status()).isEqualTo("DELETED");
    verify(parts).delete(part);
    verify(parts).flush();
  }

  private InventoryPart material(UUID id, String code, BigDecimal stock) {
    InventoryPart part = new InventoryPart();
    part.setId(id);
    part.setCode(code);
    part.setName("测试物料");
    part.setCategory("材料类");
    part.setStockQty(stock);
    part.setSafetyQty(BigDecimal.ZERO);
    part.setUnitCost(BigDecimal.ZERO);
    return part;
  }

  private MaterialCategory category(String name) {
    MaterialCategory category = new MaterialCategory();
    category.setId(UUID.randomUUID());
    category.setName(name);
    return category;
  }
}
