package com.company.ops.api.modules.procurement.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.dto.CreateMaterialMasterRequest;
import com.company.ops.api.modules.inventory.dto.InventoryPartResponse;
import com.company.ops.api.modules.inventory.dto.UpdateMaterialMasterRequest;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.procurement.domain.MaterialCategory;
import com.company.ops.api.modules.procurement.dto.CreateMaterialCategoryRequest;
import com.company.ops.api.modules.procurement.dto.MaterialCategoryResponse;
import com.company.ops.api.modules.procurement.dto.MaterialDeletionResponse;
import com.company.ops.api.modules.procurement.repository.MaterialCategoryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcurementMaterialService {

  private static final String DELETE_ENTITY_TYPE = "MATERIAL";

  private final InventoryPartRepository partRepository;
  private final MaterialCategoryRepository categoryRepository;
  private final DeleteGovernanceService deleteGovernanceService;
  private final CodeGenerator codeGenerator;

  public ProcurementMaterialService(
      InventoryPartRepository partRepository,
      MaterialCategoryRepository categoryRepository,
      DeleteGovernanceService deleteGovernanceService,
      CodeGenerator codeGenerator
  ) {
    this.partRepository = partRepository;
    this.categoryRepository = categoryRepository;
    this.deleteGovernanceService = deleteGovernanceService;
    this.codeGenerator = codeGenerator;
  }

  @Transactional(readOnly = true)
  public Page<InventoryPartResponse> listMaterials(Pageable pageable) {
    Set<UUID> hiddenIds = deleteGovernanceService.hiddenIds(DELETE_ENTITY_TYPE);
    Page<InventoryPart> materials = hiddenIds.isEmpty()
        ? partRepository.findAllByOrderByCreatedAtDesc(pageable)
        : partRepository.findAllVisible(hiddenIds, pageable);
    return materials.map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public List<MaterialCategoryResponse> listCategories() {
    return categoryRepository.findAllByOrderByNameAsc().stream()
        .map(this::toCategoryResponse)
        .toList();
  }

  @Transactional
  public MaterialCategoryResponse createCategory(CreateMaterialCategoryRequest request) {
    String name = request.name().trim();
    if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
      throw new BusinessException("物料分类已存在");
    }
    MaterialCategory category = new MaterialCategory();
    category.setName(name);
    category.setBuiltIn(false);
    return toCategoryResponse(categoryRepository.save(category));
  }

  @Transactional
  public InventoryPartResponse createMaterial(CreateMaterialMasterRequest request) {
    String partCode = request.code() != null && !request.code().isBlank()
        ? request.code().trim()
        : codeGenerator.generate("PART");
    if (partRepository.existsByCode(partCode)) {
      throw new BusinessException("物料编码已存在");
    }
    InventoryPart part = new InventoryPart();
    part.setCode(partCode);
    applyMaterialMaster(part, request.name(), request.model(), request.category(),
        request.safetyQty(), request.unitCost());
    part.setStockQty(BigDecimal.ZERO);
    return toResponse(partRepository.save(part));
  }

  @Transactional
  public InventoryPartResponse updateMaterial(UUID materialId, UpdateMaterialMasterRequest request) {
    InventoryPart part = partRepository.findByIdForUpdate(materialId)
        .orElseThrow(() -> new BusinessException("物料不存在"));
    if (deleteGovernanceService.isHidden(DELETE_ENTITY_TYPE, materialId)) {
      throw new BusinessException("物料已进入删除审批，不能编辑");
    }
    applyMaterialMaster(part, request.name(), request.model(), request.category(),
        request.safetyQty(), request.unitCost());
    return toResponse(partRepository.save(part));
  }

  @Transactional
  public MaterialDeletionResponse deleteMaterial(UUID materialId) {
    InventoryPart part = partRepository.findByIdForUpdate(materialId)
        .orElseThrow(() -> new BusinessException("物料不存在"));
    if (amount(part.getStockQty()).compareTo(BigDecimal.ZERO) > 0) {
      throw new BusinessException("物料仍有库存，不能删除");
    }
    String title = part.getCode() + " · " + part.getName();
    if (!deleteGovernanceService.allowPhysicalDelete(DELETE_ENTITY_TYPE, materialId, title)) {
      return new MaterialDeletionResponse("PENDING_APPROVAL", "删除申请已提交，等待管理员审批");
    }
    try {
      partRepository.delete(part);
      partRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException("物料已有采购、库存或项目历史记录，不能直接删除");
    }
    return new MaterialDeletionResponse("DELETED", "物料已删除");
  }

  private void applyMaterialMaster(
      InventoryPart part,
      String name,
      String model,
      String categoryName,
      BigDecimal safetyQty,
      BigDecimal unitCost
  ) {
    MaterialCategory category = categoryRepository.findByNameIgnoreCase(categoryName.trim())
        .orElseThrow(() -> new BusinessException("物料分类不存在，请先新增分类"));
    part.setName(name.trim());
    part.setModel(trimToNull(model));
    part.setCategory(category.getName());
    part.setSafetyQty(amount(safetyQty));
    part.setUnitCost(amount(unitCost));
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }

  private InventoryPartResponse toResponse(InventoryPart part) {
    return new InventoryPartResponse(
        part.getId(), part.getCode(), part.getName(), part.getModel(), part.getCategory(),
        part.getStockQty(), part.getSafetyQty(), part.getUnitCost(), part.isLowStock()
    );
  }

  private MaterialCategoryResponse toCategoryResponse(MaterialCategory category) {
    return new MaterialCategoryResponse(category.getId(), category.getName(), category.isBuiltIn());
  }
}
