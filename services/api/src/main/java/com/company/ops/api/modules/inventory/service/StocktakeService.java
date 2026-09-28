package com.company.ops.api.modules.inventory.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.InventoryStocktake;
import com.company.ops.api.modules.inventory.domain.InventoryStocktakeLine;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.dto.StocktakeDtos.*;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.InventoryStocktakeLineRepository;
import com.company.ops.api.modules.inventory.repository.InventoryStocktakeRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StocktakeService {

  private final InventoryStocktakeRepository stocktakeRepository;
  private final InventoryStocktakeLineRepository lineRepository;
  private final InventoryPartRepository partRepository;
  private final StockMovementRepository movementRepository;
  private final CodeGenerator codeGenerator;

  public StocktakeService(InventoryStocktakeRepository stocktakeRepository,
      InventoryStocktakeLineRepository lineRepository,
      InventoryPartRepository partRepository,
      StockMovementRepository movementRepository,
      CodeGenerator codeGenerator) {
    this.stocktakeRepository = stocktakeRepository;
    this.lineRepository = lineRepository;
    this.partRepository = partRepository;
    this.movementRepository = movementRepository;
    this.codeGenerator = codeGenerator;
  }

  @Transactional(readOnly = true)
  public Page<StocktakeResponse> list(Pageable pageable) {
    Page<InventoryStocktake> stocktakes = stocktakeRepository.findAllByOrderByCreatedAtDesc(pageable);
    Map<UUID, List<InventoryStocktakeLine>> lines = lineRepository
        .findByStocktakeIdIn(stocktakes.getContent().stream().map(InventoryStocktake::getId).toList())
        .stream().collect(Collectors.groupingBy(InventoryStocktakeLine::getStocktakeId));
    return stocktakes.map(s -> toResponse(s, lines.getOrDefault(s.getId(), List.of())));
  }

  @Transactional(readOnly = true)
  public StocktakeResponse get(UUID id) {
    InventoryStocktake stocktake = requireStocktake(id);
    return toResponse(stocktake, lineRepository.findByStocktakeIdOrderByCreatedAtAsc(id));
  }

  @Transactional
  public StocktakeResponse create(CreateStocktakeRequest request) {
    String code = request.code() != null && !request.code().isBlank()
        ? request.code().trim() : codeGenerator.generate("STOCKTAKE");
    if (stocktakeRepository.existsByCode(code)) throw new BusinessException("盘点单号已存在");

    List<InventoryPart> parts = request.partIds() == null || request.partIds().isEmpty()
        ? partRepository.findAllByOrderByCreatedAtDesc()
        : partRepository.findAllById(request.partIds());
    if (parts.isEmpty()) throw new BusinessException("没有可盘点的物料");

    InventoryStocktake stocktake = new InventoryStocktake();
    stocktake.setCode(code);
    stocktake.setCountDate(request.countDate());
    stocktake.setStatus("DRAFT");
    stocktake.setRemark(request.remark());
    InventoryStocktake saved = stocktakeRepository.save(stocktake);

    List<InventoryStocktakeLine> lines = parts.stream().map(part -> {
      InventoryStocktakeLine line = new InventoryStocktakeLine();
      line.setStocktakeId(saved.getId());
      line.setPartId(part.getId());
      line.setPartName(part.getName());
      line.setBookQty(amount(part.getStockQty()));
      line.setUnitCost(amount(part.getUnitCost()));
      return line;
    }).toList();
    lineRepository.saveAll(lines);

    return toResponse(saved, lineRepository.findByStocktakeIdOrderByCreatedAtAsc(saved.getId()));
  }

  @Transactional
  public StocktakeResponse updateLine(UUID stocktakeId, UUID lineId, UpdateStocktakeLineRequest request) {
    InventoryStocktake stocktake = requireStocktake(stocktakeId);
    if (!"DRAFT".equals(stocktake.getStatus())) throw new BusinessException("只有草稿盘点单可以录入实盘数量");
    InventoryStocktakeLine line = lineRepository.findById(lineId)
        .orElseThrow(() -> new BusinessException("盘点明细不存在"));
    if (!line.getStocktakeId().equals(stocktakeId)) throw new BusinessException("盘点明细不属于该盘点单");
    line.setActualQty(amount(request.actualQty()));
    line.setDifference(line.getActualQty().subtract(line.getBookQty()));
    line.setRemark(request.remark());
    lineRepository.save(line);
    return toResponse(stocktake, lineRepository.findByStocktakeIdOrderByCreatedAtAsc(stocktakeId));
  }

  @Transactional
  public StocktakeResponse post(UUID id) {
    InventoryStocktake stocktake = stocktakeRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("盘点单不存在"));
    if (!"DRAFT".equals(stocktake.getStatus())) throw new BusinessException("盘点单已过账或状态不允许");
    List<InventoryStocktakeLine> lines = lineRepository.findByStocktakeIdOrderByCreatedAtAsc(id);
    for (InventoryStocktakeLine line : lines) {
      if (line.getDifference() == null || line.getDifference().signum() == 0) continue;
      InventoryPart part = partRepository.findByIdForUpdate(line.getPartId())
          .orElseThrow(() -> new BusinessException("物料不存在：" + line.getPartName()));
      part.setStockQty(part.getStockQty().add(line.getDifference()));
      partRepository.save(part);
      saveMovement(part.getId(), StockMovementType.ADJUSTMENT, line.getDifference().abs(),
          line.getUnitCost(), line.getDifference(), stocktake.getCode(), "盘点差异调整 " + stocktake.getCode());
    }
    stocktake.setStatus("POSTED");
    stocktakeRepository.save(stocktake);
    return toResponse(stocktake, lines);
  }

  private void saveMovement(UUID partId, StockMovementType type, BigDecimal quantity,
      BigDecimal unitCost, BigDecimal signedAmount, String sourceNo, String remark) {
    StockMovement movement = new StockMovement();
    movement.setPartId(partId);
    movement.setMovementType(type);
    movement.setQuantity(quantity);
    movement.setUnitCost(amount(unitCost));
    movement.setAmount(signedAmount);
    movement.setSourceNo(sourceNo);
    movement.setRemark(remark);
    movement.setOperatorName(currentName());
    movementRepository.save(movement);
  }

  private InventoryStocktake requireStocktake(UUID id) {
    return stocktakeRepository.findById(id).orElseThrow(() -> new BusinessException("盘点单不存在"));
  }

  private StocktakeResponse toResponse(InventoryStocktake s, List<InventoryStocktakeLine> lines) {
    return new StocktakeResponse(s.getId(), s.getCode(), s.getCountDate(), s.getStatus(), s.getRemark(),
        lines.stream().map(l -> new StocktakeLineResponse(l.getId(), l.getPartId(), l.getPartName(),
            l.getBookQty(), l.getActualQty(), l.getDifference(), l.getUnitCost(), l.getRemark())).toList());
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }
}