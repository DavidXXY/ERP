package com.company.ops.api.modules.inventory.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.dto.CreateInventoryPartRequest;
import com.company.ops.api.modules.inventory.dto.CreateStockMovementRequest;
import com.company.ops.api.modules.inventory.dto.InventoryPartResponse;
import com.company.ops.api.modules.inventory.dto.StockMovementResponse;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

  private final InventoryPartRepository partRepository;
  private final StockMovementRepository movementRepository;

  public InventoryService(InventoryPartRepository partRepository, StockMovementRepository movementRepository) {
    this.partRepository = partRepository;
    this.movementRepository = movementRepository;
  }

  @Transactional(readOnly = true)
  public List<InventoryPartResponse> listParts() {
    return partRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toPartResponse)
        .toList();
  }

  @Transactional
  public InventoryPartResponse createPart(CreateInventoryPartRequest request) {
    if (partRepository.existsByCode(request.code())) {
      throw new BusinessException("备件编码已存在");
    }
    InventoryPart part = new InventoryPart();
    part.setCode(request.code());
    part.setName(request.name());
    part.setModel(request.model());
    part.setStockQty(defaultAmount(request.stockQty()));
    part.setSafetyQty(defaultAmount(request.safetyQty()));
    part.setLocation(request.location());
    part.setUnitCost(defaultAmount(request.unitCost()));
    return toPartResponse(partRepository.save(part));
  }

  @Transactional(readOnly = true)
  public List<StockMovementResponse> listMovements(UUID partId) {
    if (!partRepository.existsById(partId)) {
      throw new BusinessException("备件不存在");
    }
    return movementRepository.findByPartIdOrderByCreatedAtDesc(partId).stream()
        .map(this::toMovementResponse)
        .toList();
  }

  @Transactional
  public InventoryPartResponse createMovement(UUID partId, CreateStockMovementRequest request) {
    InventoryPart part = partRepository.findById(partId)
        .orElseThrow(() -> new BusinessException("备件不存在"));

    BigDecimal nextQty = part.getStockQty().add(toDelta(request.movementType(), request.quantity()));
    if (nextQty.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException("库存不足，无法完成出库");
    }

    StockMovement movement = new StockMovement();
    movement.setPartId(partId);
    movement.setMovementType(request.movementType());
    movement.setQuantity(request.quantity());
    movement.setSourceNo(request.sourceNo());
    movement.setRemark(request.remark());
    movementRepository.save(movement);

    part.setStockQty(nextQty);
    return toPartResponse(partRepository.save(part));
  }

  private BigDecimal toDelta(StockMovementType movementType, BigDecimal quantity) {
    return switch (movementType) {
      case INBOUND, RETURN, ADJUSTMENT -> quantity;
      case OUTBOUND, SCRAP -> quantity.negate();
    };
  }

  private InventoryPartResponse toPartResponse(InventoryPart part) {
    return new InventoryPartResponse(
        part.getId(),
        part.getCode(),
        part.getName(),
        part.getModel(),
        part.getStockQty(),
        part.getSafetyQty(),
        part.getLocation(),
        part.getUnitCost(),
        part.isLowStock()
    );
  }

  private StockMovementResponse toMovementResponse(StockMovement movement) {
    return new StockMovementResponse(
        movement.getId(),
        movement.getPartId(),
        movement.getMovementType(),
        movement.getQuantity(),
        movement.getSourceNo(),
        movement.getRemark(),
        movement.getCreatedAt()
    );
  }

  private BigDecimal defaultAmount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
