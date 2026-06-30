package com.company.ops.api.modules.inventory.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.inventory.domain.InventoryIssueLine;
import com.company.ops.api.modules.inventory.domain.InventoryIssueOrder;
import com.company.ops.api.modules.inventory.domain.InventoryIssueStatus;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.InventoryReturnLine;
import com.company.ops.api.modules.inventory.domain.InventoryReturnOrder;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.dto.CreateInventoryPartRequest;
import com.company.ops.api.modules.inventory.dto.CreateMaterialIssueRequest;
import com.company.ops.api.modules.inventory.dto.CreateMaterialReturnRequest;
import com.company.ops.api.modules.inventory.dto.CreateStockMovementRequest;
import com.company.ops.api.modules.inventory.dto.InventoryPartResponse;
import com.company.ops.api.modules.inventory.dto.InventoryProjectOptionResponse;
import com.company.ops.api.modules.inventory.dto.MaterialIssueLineRequest;
import com.company.ops.api.modules.inventory.dto.MaterialIssueLineResponse;
import com.company.ops.api.modules.inventory.dto.MaterialIssueResponse;
import com.company.ops.api.modules.inventory.dto.MaterialReturnLineResponse;
import com.company.ops.api.modules.inventory.dto.MaterialReturnResponse;
import com.company.ops.api.modules.inventory.dto.StockMovementResponse;
import com.company.ops.api.modules.inventory.repository.InventoryIssueLineRepository;
import com.company.ops.api.modules.inventory.repository.InventoryIssueOrderRepository;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.InventoryReturnLineRepository;
import com.company.ops.api.modules.inventory.repository.InventoryReturnOrderRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectCostCategory;
import com.company.ops.api.modules.project.domain.ProjectCostEntry;
import com.company.ops.api.modules.project.domain.ProjectCostSource;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.repository.ProjectCostEntryRepository;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

  private final InventoryPartRepository partRepository;
  private final StockMovementRepository movementRepository;
  private final InventoryIssueOrderRepository issueRepository;
  private final InventoryIssueLineRepository issueLineRepository;
  private final InventoryReturnOrderRepository returnRepository;
  private final InventoryReturnLineRepository returnLineRepository;
  private final ProjectRepository projectRepository;
  private final ProjectCostEntryRepository projectCostRepository;

  public InventoryService(
      InventoryPartRepository partRepository,
      StockMovementRepository movementRepository,
      InventoryIssueOrderRepository issueRepository,
      InventoryIssueLineRepository issueLineRepository,
      InventoryReturnOrderRepository returnRepository,
      InventoryReturnLineRepository returnLineRepository,
      ProjectRepository projectRepository,
      ProjectCostEntryRepository projectCostRepository
  ) {
    this.partRepository = partRepository;
    this.movementRepository = movementRepository;
    this.issueRepository = issueRepository;
    this.issueLineRepository = issueLineRepository;
    this.returnRepository = returnRepository;
    this.returnLineRepository = returnLineRepository;
    this.projectRepository = projectRepository;
    this.projectCostRepository = projectCostRepository;
  }

  @Transactional(readOnly = true)
  public List<InventoryPartResponse> listParts() {
    return partRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toPartResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<InventoryProjectOptionResponse> listEligibleProjects() {
    return projectRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(project -> project.getApprovalStatus() == ProjectApprovalStatus.APPROVED)
        .filter(project -> project.getStage() != ProjectStage.CLOSED)
        .map(project -> new InventoryProjectOptionResponse(
            project.getId(),
            project.getCode(),
            project.getName(),
            project.getManagerName(),
            project.getStage()
        ))
        .toList();
  }

  @Transactional
  public InventoryPartResponse createPart(CreateInventoryPartRequest request) {
    if (partRepository.existsByCode(request.code())) {
      throw new BusinessException("物料编码已存在");
    }
    InventoryPart part = new InventoryPart();
    part.setCode(request.code());
    part.setName(request.name());
    part.setModel(request.model());
    part.setStockQty(amount(request.stockQty()));
    part.setSafetyQty(amount(request.safetyQty()));
    part.setLocation(request.location());
    part.setUnitCost(amount(request.unitCost()));
    return toPartResponse(partRepository.save(part));
  }

  @Transactional(readOnly = true)
  public List<StockMovementResponse> listMovements(UUID partId) {
    if (!partRepository.existsById(partId)) {
      throw new BusinessException("物料不存在");
    }
    return movementRepository.findByPartIdOrderByCreatedAtDesc(partId).stream()
        .map(this::toMovementResponse)
        .toList();
  }

  @Transactional
  public InventoryPartResponse createMovement(UUID partId, CreateStockMovementRequest request) {
    if (request.movementType() == StockMovementType.OUTBOUND
        || request.movementType() == StockMovementType.RETURN) {
      throw new BusinessException("项目领料和退料必须通过领退料单办理");
    }
    InventoryPart part = partRepository.findByIdForUpdate(partId)
        .orElseThrow(() -> new BusinessException("物料不存在"));
    BigDecimal nextQty = part.getStockQty().add(toDelta(request.movementType(), request.quantity()));
    if (nextQty.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException("库存不足，无法完成出库");
    }
    saveMovement(partId, request.movementType(), request.quantity(), request.sourceNo(), request.remark());
    part.setStockQty(nextQty);
    return toPartResponse(partRepository.save(part));
  }

  @Transactional(readOnly = true)
  public List<MaterialIssueResponse> listIssues() {
    List<InventoryIssueOrder> orders = issueRepository.findAllByOrderByIssueDateDescCreatedAtDesc();
    Map<UUID, Project> projects = projectMap(orders.stream().map(InventoryIssueOrder::getProjectId).distinct().toList());
    Map<UUID, List<InventoryIssueLine>> lines = issueLinesByOrder(orders.stream().map(InventoryIssueOrder::getId).toList());
    return orders.stream()
        .map(order -> toIssueResponse(order, projects.get(order.getProjectId()), lines.getOrDefault(order.getId(), List.of())))
        .toList();
  }

  @Transactional
  public MaterialIssueResponse createIssue(CreateMaterialIssueRequest request) {
    if (issueRepository.existsByCode(request.code())) {
      throw new BusinessException("领料单号已存在");
    }
    validateUniqueParts(request.lines());
    Project project = requireExecutableProject(request.projectId());
    Map<UUID, InventoryPart> parts = lockParts(request.lines().stream().map(MaterialIssueLineRequest::partId).toList());

    InventoryIssueOrder order = new InventoryIssueOrder();
    order.setCode(request.code());
    order.setProjectId(project.getId());
    order.setIssueDate(request.issueDate());
    order.setReceiverName(request.receiverName());
    order.setPurpose(request.purpose());
    order.setStatus(InventoryIssueStatus.POSTED);
    InventoryIssueOrder savedOrder = issueRepository.save(order);

    BigDecimal total = BigDecimal.ZERO;
    List<InventoryIssueLine> lines = request.lines().stream().map(item -> {
      InventoryPart part = parts.get(item.partId());
      if (part.getStockQty().compareTo(item.quantity()) < 0) {
        throw new BusinessException(part.getName() + "库存不足，当前库存" + part.getStockQty());
      }
      BigDecimal lineAmount = item.quantity().multiply(amount(part.getUnitCost()));
      InventoryIssueLine line = new InventoryIssueLine();
      line.setIssueId(savedOrder.getId());
      line.setPartId(part.getId());
      line.setPartName(part.getName());
      line.setQuantity(item.quantity());
      line.setReturnedQty(BigDecimal.ZERO);
      line.setUnitCost(amount(part.getUnitCost()));
      line.setAmount(lineAmount);
      part.setStockQty(part.getStockQty().subtract(item.quantity()));
      saveMovement(part.getId(), StockMovementType.OUTBOUND, item.quantity(), request.code(),
          "项目领料 " + project.getCode() + " · " + request.purpose());
      return line;
    }).toList();
    issueLineRepository.saveAll(lines);
    partRepository.saveAll(parts.values());
    total = lines.stream().map(InventoryIssueLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    savedOrder.setTotalAmount(total);
    issueRepository.save(savedOrder);
    createProjectCost(project, request.code(), request.issueDate(), total, false,
        "项目领料：" + request.purpose());
    return toIssueResponse(savedOrder, project, lines);
  }

  @Transactional(readOnly = true)
  public List<MaterialReturnResponse> listReturns() {
    List<InventoryReturnOrder> orders = returnRepository.findAllByOrderByReturnDateDescCreatedAtDesc();
    Map<UUID, InventoryIssueOrder> issues = issueRepository.findAllById(
        orders.stream().map(InventoryReturnOrder::getIssueId).distinct().toList()
    ).stream().collect(Collectors.toMap(InventoryIssueOrder::getId, Function.identity()));
    Map<UUID, Project> projects = projectMap(orders.stream().map(InventoryReturnOrder::getProjectId).distinct().toList());
    Map<UUID, List<InventoryReturnLine>> lines = returnLinesByOrder(orders.stream().map(InventoryReturnOrder::getId).toList());
    return orders.stream()
        .map(order -> toReturnResponse(
            order,
            issues.get(order.getIssueId()),
            projects.get(order.getProjectId()),
            lines.getOrDefault(order.getId(), List.of())
        ))
        .toList();
  }

  @Transactional
  public MaterialReturnResponse createReturn(UUID issueId, CreateMaterialReturnRequest request) {
    if (returnRepository.existsByCode(request.code())) {
      throw new BusinessException("退料单号已存在");
    }
    InventoryIssueOrder issue = issueRepository.findById(issueId)
        .orElseThrow(() -> new BusinessException("领料单不存在"));
    if (issue.getStatus() == InventoryIssueStatus.FULLY_RETURNED) {
      throw new BusinessException("该领料单已全部退回");
    }
    Project project = requireExecutableProject(issue.getProjectId());
    Set<UUID> requestedLineIds = new HashSet<>();
    request.lines().forEach(item -> {
      if (!requestedLineIds.add(item.issueLineId())) {
        throw new BusinessException("同一领料明细不能重复退料");
      }
    });
    Map<UUID, InventoryIssueLine> issueLines = issueLineRepository.findAllById(requestedLineIds).stream()
        .collect(Collectors.toMap(InventoryIssueLine::getId, Function.identity()));
    if (issueLines.size() != requestedLineIds.size()
        || issueLines.values().stream().anyMatch(line -> !line.getIssueId().equals(issueId))) {
      throw new BusinessException("退料明细不属于该领料单");
    }
    Map<UUID, InventoryPart> parts = lockParts(issueLines.values().stream().map(InventoryIssueLine::getPartId).toList());

    InventoryReturnOrder returnOrder = new InventoryReturnOrder();
    returnOrder.setCode(request.code());
    returnOrder.setIssueId(issue.getId());
    returnOrder.setProjectId(project.getId());
    returnOrder.setReturnDate(request.returnDate());
    returnOrder.setHandlerName(request.handlerName());
    InventoryReturnOrder savedReturn = returnRepository.save(returnOrder);

    List<InventoryReturnLine> returnLines = request.lines().stream().map(item -> {
      InventoryIssueLine issueLine = issueLines.get(item.issueLineId());
      BigDecimal returnable = issueLine.getQuantity().subtract(issueLine.getReturnedQty());
      if (item.quantity().compareTo(returnable) > 0) {
        throw new BusinessException(issueLine.getPartName() + "退料数量超过可退数量" + returnable);
      }
      InventoryPart part = parts.get(issueLine.getPartId());
      BigDecimal lineAmount = item.quantity().multiply(issueLine.getUnitCost());
      InventoryReturnLine line = new InventoryReturnLine();
      line.setReturnId(savedReturn.getId());
      line.setIssueLineId(issueLine.getId());
      line.setPartId(issueLine.getPartId());
      line.setPartName(issueLine.getPartName());
      line.setQuantity(item.quantity());
      line.setUnitCost(issueLine.getUnitCost());
      line.setAmount(lineAmount);
      issueLine.setReturnedQty(issueLine.getReturnedQty().add(item.quantity()));
      part.setStockQty(part.getStockQty().add(item.quantity()));
      saveMovement(part.getId(), StockMovementType.RETURN, item.quantity(), request.code(),
          "项目退料 " + project.getCode() + " · 原领料单 " + issue.getCode());
      return line;
    }).toList();
    returnLineRepository.saveAll(returnLines);
    issueLineRepository.saveAll(issueLines.values());
    partRepository.saveAll(parts.values());
    BigDecimal total = returnLines.stream().map(InventoryReturnLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    savedReturn.setTotalAmount(total);
    returnRepository.save(savedReturn);

    List<InventoryIssueLine> allIssueLines = issueLineRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId());
    boolean fullyReturned = allIssueLines.stream()
        .allMatch(line -> line.getReturnedQty().compareTo(line.getQuantity()) == 0);
    issue.setStatus(fullyReturned ? InventoryIssueStatus.FULLY_RETURNED : InventoryIssueStatus.PARTIAL_RETURNED);
    issueRepository.save(issue);
    createProjectCost(project, request.code(), request.returnDate(), total, true,
        "项目退料：原领料单 " + issue.getCode());
    return toReturnResponse(savedReturn, issue, project, returnLines);
  }

  private Project requireExecutableProject(UUID id) {
    Project project = projectRepository.findById(id)
        .orElseThrow(() -> new BusinessException("项目不存在"));
    if (project.getApprovalStatus() != ProjectApprovalStatus.APPROVED) {
      throw new BusinessException("项目立项审批通过后才能领退料");
    }
    if (project.getStage() == ProjectStage.CLOSED) {
      throw new BusinessException("项目已关闭，不能领退料");
    }
    return project;
  }

  private void createProjectCost(
      Project project,
      String sourceNo,
      java.time.LocalDate incurredDate,
      BigDecimal costAmount,
      boolean reversal,
      String description
  ) {
    BigDecimal signedAmount = reversal ? costAmount.negate() : costAmount;
    ProjectCostEntry entry = new ProjectCostEntry();
    entry.setProjectId(project.getId());
    entry.setCategory(ProjectCostCategory.MATERIAL);
    entry.setSourceType(ProjectCostSource.INVENTORY);
    entry.setSourceNo(sourceNo);
    entry.setDescription(description);
    entry.setAmount(signedAmount);
    entry.setIncurredDate(incurredDate);
    projectCostRepository.save(entry);
    project.setActualCost(amount(project.getActualCost()).add(signedAmount));
    projectRepository.save(project);
  }

  private Map<UUID, InventoryPart> lockParts(List<UUID> partIds) {
    Map<UUID, InventoryPart> parts = partIds.stream().distinct()
        .sorted(Comparator.comparing(UUID::toString))
        .map(id -> partRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new BusinessException("物料不存在")))
        .collect(Collectors.toMap(InventoryPart::getId, Function.identity()));
    return parts;
  }

  private void validateUniqueParts(List<MaterialIssueLineRequest> lines) {
    Set<UUID> partIds = new HashSet<>();
    for (MaterialIssueLineRequest line : lines) {
      if (!partIds.add(line.partId())) {
        throw new BusinessException("同一物料不能重复出现在领料单中");
      }
    }
  }

  private Map<UUID, Project> projectMap(List<UUID> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    return projectRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Project::getId, Function.identity()));
  }

  private Map<UUID, List<InventoryIssueLine>> issueLinesByOrder(List<UUID> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    return issueLineRepository.findByIssueIdIn(ids).stream()
        .collect(Collectors.groupingBy(InventoryIssueLine::getIssueId));
  }

  private Map<UUID, List<InventoryReturnLine>> returnLinesByOrder(List<UUID> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    return returnLineRepository.findByReturnIdIn(ids).stream()
        .collect(Collectors.groupingBy(InventoryReturnLine::getReturnId));
  }

  private MaterialIssueResponse toIssueResponse(
      InventoryIssueOrder order,
      Project project,
      List<InventoryIssueLine> lines
  ) {
    return new MaterialIssueResponse(
        order.getId(),
        order.getCode(),
        order.getProjectId(),
        project == null ? null : project.getCode(),
        project == null ? null : project.getName(),
        order.getIssueDate(),
        order.getReceiverName(),
        order.getPurpose(),
        amount(order.getTotalAmount()),
        order.getStatus(),
        lines.stream().map(this::toIssueLineResponse).toList()
    );
  }

  private MaterialIssueLineResponse toIssueLineResponse(InventoryIssueLine line) {
    return new MaterialIssueLineResponse(
        line.getId(),
        line.getPartId(),
        line.getPartName(),
        line.getQuantity(),
        line.getReturnedQty(),
        line.getQuantity().subtract(line.getReturnedQty()),
        line.getUnitCost(),
        line.getAmount()
    );
  }

  private MaterialReturnResponse toReturnResponse(
      InventoryReturnOrder order,
      InventoryIssueOrder issue,
      Project project,
      List<InventoryReturnLine> lines
  ) {
    return new MaterialReturnResponse(
        order.getId(),
        order.getCode(),
        order.getIssueId(),
        issue == null ? null : issue.getCode(),
        order.getProjectId(),
        project == null ? null : project.getCode(),
        project == null ? null : project.getName(),
        order.getReturnDate(),
        order.getHandlerName(),
        amount(order.getTotalAmount()),
        lines.stream().map(line -> new MaterialReturnLineResponse(
            line.getId(),
            line.getIssueLineId(),
            line.getPartId(),
            line.getPartName(),
            line.getQuantity(),
            line.getUnitCost(),
            line.getAmount()
        )).toList()
    );
  }

  private void saveMovement(
      UUID partId,
      StockMovementType type,
      BigDecimal quantity,
      String sourceNo,
      String remark
  ) {
    StockMovement movement = new StockMovement();
    movement.setPartId(partId);
    movement.setMovementType(type);
    movement.setQuantity(quantity);
    movement.setSourceNo(sourceNo);
    movement.setRemark(remark);
    movementRepository.save(movement);
  }

  private BigDecimal toDelta(StockMovementType movementType, BigDecimal quantity) {
    return switch (movementType) {
      case INBOUND, RETURN, ADJUSTMENT -> quantity;
      case OUTBOUND, SCRAP -> quantity.negate();
    };
  }

  private InventoryPartResponse toPartResponse(InventoryPart part) {
    return new InventoryPartResponse(
        part.getId(), part.getCode(), part.getName(), part.getModel(), part.getStockQty(),
        part.getSafetyQty(), part.getLocation(), part.getUnitCost(), part.isLowStock()
    );
  }

  private StockMovementResponse toMovementResponse(StockMovement movement) {
    return new StockMovementResponse(
        movement.getId(), movement.getPartId(), movement.getMovementType(), movement.getQuantity(),
        movement.getSourceNo(), movement.getRemark(), movement.getCreatedAt()
    );
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
