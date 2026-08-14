package com.company.ops.api.modules.procurement.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.procurement.domain.CentralPlan;
import com.company.ops.api.modules.procurement.domain.CentralPlanItem;
import com.company.ops.api.modules.procurement.domain.FrameworkAgreement;
import com.company.ops.api.modules.procurement.domain.FrameworkAgreementItem;
import com.company.ops.api.modules.procurement.domain.ProcurementApprovalRule;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiryRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.PurchaseRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseRequestRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.ApprovalRuleResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.CentralPlanItemRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.CentralPlanItemResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.CentralPlanResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.CentralPlanSuggestionItem;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.CentralPlanSuggestionsResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.FrameworkAgreementResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.FrameworkItemRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.FrameworkItemResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.SaveApprovalRuleRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.SaveCentralPlanRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementPlanningDtos.SaveFrameworkAgreementRequest;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.repository.CentralPlanItemRepository;
import com.company.ops.api.modules.procurement.repository.CentralPlanRepository;
import com.company.ops.api.modules.procurement.repository.FrameworkAgreementItemRepository;
import com.company.ops.api.modules.procurement.repository.FrameworkAgreementRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementApprovalRuleRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRequestRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 采购规划：分级审批规则、框架协议、集采计划。 */
@Service
public class ProcurementPlanningService {

  private final ProcurementApprovalRuleRepository approvalRules;
  private final FrameworkAgreementRepository agreements;
  private final FrameworkAgreementItemRepository agreementItems;
  private final CentralPlanRepository plans;
  private final CentralPlanItemRepository planItems;
  private final SupplierRepository suppliers;
  private final InventoryPartRepository parts;
  private final PurchaseRequestRepository requests;
  private final PurchaseOrderRepository orders;
  private final ProcurementInquiryRequestRepository inquiryRequests;
  private final CodeGenerator codeGenerator;
  private final ProcurementService procurementService;

  public ProcurementPlanningService(
      ProcurementApprovalRuleRepository approvalRules,
      FrameworkAgreementRepository agreements,
      FrameworkAgreementItemRepository agreementItems,
      CentralPlanRepository plans,
      CentralPlanItemRepository planItems,
      SupplierRepository suppliers,
      InventoryPartRepository parts,
      PurchaseRequestRepository requests,
      PurchaseOrderRepository orders,
      ProcurementInquiryRequestRepository inquiryRequests,
      CodeGenerator codeGenerator,
      ProcurementService procurementService
  ) {
    this.approvalRules = approvalRules;
    this.agreements = agreements;
    this.agreementItems = agreementItems;
    this.plans = plans;
    this.planItems = planItems;
    this.suppliers = suppliers;
    this.parts = parts;
    this.requests = requests;
    this.orders = orders;
    this.inquiryRequests = inquiryRequests;
    this.codeGenerator = codeGenerator;
    this.procurementService = procurementService;
  }

  // ---------- 分级审批规则 ----------

  @Transactional(readOnly = true)
  public List<ApprovalRuleResponse> listApprovalRules() {
    return approvalRules.findAllByOrderBySortOrderAsc().stream()
        .map(rule -> new ApprovalRuleResponse(
            rule.getId(), rule.getRuleName(), rule.getMinAmount(), rule.getMaxAmount(),
            rule.getApprovalLevel(), rule.getRequiredRoleCode(), rule.isEnabled(), rule.getSortOrder()))
        .toList();
  }

  @Transactional
  public ApprovalRuleResponse saveApprovalRule(UUID id, SaveApprovalRuleRequest request) {
    ProcurementApprovalRule rule = id == null ? new ProcurementApprovalRule() : approvalRules.findById(id)
        .orElseThrow(() -> new BusinessException("审批规则不存在"));
    if (rule.getId() == null) {
      rule.setTenantId(TenantContext.currentTenant());
    }
    rule.setRuleName(request.ruleName());
    rule.setMinAmount(request.minAmount());
    rule.setMaxAmount(request.maxAmount());
    rule.setApprovalLevel(request.approvalLevel());
    rule.setRequiredRoleCode(request.requiredRoleCode());
    rule.setEnabled(request.enabled());
    rule.setSortOrder(request.sortOrder());
    ProcurementApprovalRule saved = approvalRules.save(rule);
    return new ApprovalRuleResponse(
        saved.getId(), saved.getRuleName(), saved.getMinAmount(), saved.getMaxAmount(),
        saved.getApprovalLevel(), saved.getRequiredRoleCode(), saved.isEnabled(), saved.getSortOrder());
  }

  @Transactional
  public void deleteApprovalRule(UUID id) {
    if (!approvalRules.existsById(id)) {
      throw new BusinessException("审批规则不存在");
    }
    approvalRules.deleteById(id);
  }

  /** 根据金额匹配启用的审批规则，返回审批级别；无匹配规则时返回 null（保持原单级审批）。 */
  @Transactional(readOnly = true)
  public String resolveApprovalLevel(BigDecimal amount) {
    return approvalRules.findByEnabledTrueOrderBySortOrderAsc().stream()
        .filter(rule -> matches(rule, amount))
        .map(ProcurementApprovalRule::getApprovalLevel)
        .findFirst()
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public String requiredRoleForLevel(String approvalLevel) {
    if (approvalLevel == null) {
      return null;
    }
    return approvalRules.findByEnabledTrueOrderBySortOrderAsc().stream()
        .filter(rule -> approvalLevel.equals(rule.getApprovalLevel()))
        .map(ProcurementApprovalRule::getRequiredRoleCode)
        .findFirst()
        .orElse(null);
  }

  private boolean matches(ProcurementApprovalRule rule, BigDecimal value) {
    if (rule.getMinAmount() != null && value.compareTo(rule.getMinAmount()) < 0) {
      return false;
    }
    if (rule.getMaxAmount() != null && value.compareTo(rule.getMaxAmount()) >= 0) {
      return false;
    }
    return true;
  }

  // ---------- 框架协议 ----------

  @Transactional(readOnly = true)
  public List<FrameworkAgreementResponse> listFrameworkAgreements() {
    return agreements.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toAgreementResponse)
        .toList();
  }

  @Transactional
  public FrameworkAgreementResponse saveFrameworkAgreement(UUID id, SaveFrameworkAgreementRequest request) {
    FrameworkAgreement agreement = id == null ? new FrameworkAgreement() : agreements.findById(id)
        .orElseThrow(() -> new BusinessException("框架协议不存在"));
    Supplier supplier = suppliers.findById(request.supplierId())
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    if (request.validTo().isBefore(request.validFrom())) {
      throw new BusinessException("协议结束日期不能早于开始日期");
    }
    if (id == null) {
      agreement.setTenantId(TenantContext.currentTenant());
      agreement.setCode(codeGenerator.generate("FRAMEWORK_AGREEMENT"));
      agreement.setCreatedByName(currentName());
    }
    agreement.setTitle(request.title());
    agreement.setSupplierId(supplier.getId());
    agreement.setValidFrom(request.validFrom());
    agreement.setValidTo(request.validTo());
    agreement.setRemark(request.remark());
    if (id != null && !"ACTIVE".equals(agreement.getStatus())) {
      throw new BusinessException("只有生效中的协议可以编辑");
    }
    FrameworkAgreement saved = agreements.save(agreement);
    if (id == null || request.items() != null) {
      if (request.items() != null) {
        agreementItems.deleteByAgreementId(saved.getId());
        for (FrameworkItemRequest item : request.items()) {
          saveAgreementItem(saved.getId(), item);
        }
      }
    }
    return toAgreementResponse(saved);
  }

  private void saveAgreementItem(UUID agreementId, FrameworkItemRequest item) {
    parts.findById(item.partId())
        .orElseThrow(() -> new BusinessException("协议物料「" + item.partName() + "」不存在"));
    FrameworkAgreementItem entity = new FrameworkAgreementItem();
    entity.setTenantId(TenantContext.currentTenant());
    entity.setAgreementId(agreementId);
    entity.setPartId(item.partId());
    entity.setPartName(item.partName());
    entity.setUnitPrice(amount(item.unitPrice()));
    entity.setTaxRate(item.taxRate() == null ? BigDecimal.valueOf(13) : amount(item.taxRate()));
    agreementItems.save(entity);
  }

  @Transactional
  public FrameworkAgreementResponse closeFrameworkAgreement(UUID id) {
    FrameworkAgreement agreement = agreements.findById(id)
        .orElseThrow(() -> new BusinessException("框架协议不存在"));
    if (!"ACTIVE".equals(agreement.getStatus())) {
      throw new BusinessException("该协议已关闭");
    }
    agreement.setStatus("CLOSED");
    return toAgreementResponse(agreements.save(agreement));
  }

  @Transactional(readOnly = true)
  public FrameworkAgreementResponse getFrameworkAgreement(UUID id) {
    return toAgreementResponse(agreements.findById(id)
        .orElseThrow(() -> new BusinessException("框架协议不存在")));
  }

  private FrameworkAgreementResponse toAgreementResponse(FrameworkAgreement agreement) {
    Supplier supplier = suppliers.findById(agreement.getSupplierId()).orElse(null);
    List<FrameworkItemResponse> items = agreementItems.findByAgreementIdOrderByCreatedAtAsc(agreement.getId()).stream()
        .map(item -> new FrameworkItemResponse(
            item.getId(), item.getPartId(), item.getPartName(), item.getUnitPrice(), item.getTaxRate()))
        .toList();
    return new FrameworkAgreementResponse(
        agreement.getId(), agreement.getCode(), agreement.getTitle(), agreement.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        agreement.getValidFrom(), agreement.getValidTo(), agreement.getStatus(),
        agreement.getRemark(), agreement.getCreatedByName(), items);
  }

  // ---------- 集采计划 ----------

  @Transactional(readOnly = true)
  public List<CentralPlanResponse> listCentralPlans() {
    return plans.findAllByOrderByPeriodYearDescCreatedAtDesc().stream()
        .map(this::toPlanResponse)
        .toList();
  }

  @Transactional
  public CentralPlanResponse saveCentralPlan(UUID id, SaveCentralPlanRequest request) {
    CentralPlan plan = id == null ? new CentralPlan() : plans.findById(id)
        .orElseThrow(() -> new BusinessException("集采计划不存在"));
    if (id == null) {
      plan.setTenantId(TenantContext.currentTenant());
      plan.setCode(codeGenerator.generate("CENTRAL_PLAN"));
      plan.setCreatedByName(currentName());
    }
    if (id != null && "CLOSED".equals(plan.getStatus())) {
      throw new BusinessException("已关闭的计划不能编辑");
    }
    plan.setName(request.name());
    plan.setPeriodYear(request.periodYear());
    plan.setRemark(request.remark());
    CentralPlan saved = plans.save(plan);
    if (request.items() != null) {
      planItems.deleteByPlanId(saved.getId());
      for (CentralPlanItemRequest item : request.items()) {
        parts.findById(item.partId())
            .orElseThrow(() -> new BusinessException("计划物料「" + item.partName() + "」不存在"));
        CentralPlanItem entity = new CentralPlanItem();
        entity.setTenantId(TenantContext.currentTenant());
        entity.setPlanId(saved.getId());
        entity.setPartId(item.partId());
        entity.setPartName(item.partName());
        entity.setPlannedQty(amount(item.plannedQty()));
        entity.setUnitPrice(item.unitPrice() == null ? BigDecimal.ZERO : amount(item.unitPrice()));
        entity.setExpectedDate(item.expectedDate());
        entity.setStatus("PLANNED");
        planItems.save(entity);
      }
    }
    return toPlanResponse(saved);
  }

  @Transactional
  public CentralPlanResponse updateCentralPlanStatus(UUID id, String status) {
    CentralPlan plan = plans.findById(id)
        .orElseThrow(() -> new BusinessException("集采计划不存在"));
    if (!List.of("DRAFT", "ACTIVE", "CLOSED").contains(status)) {
      throw new BusinessException("不支持的计划状态");
    }
    plan.setStatus(status);
    return toPlanResponse(plans.save(plan));
  }

  @Transactional(readOnly = true)
  public CentralPlanSuggestionsResponse generateCentralPlanSuggestions(Integer periodYear) {
    int year = periodYear == null ? LocalDate.now().getYear() : periodYear;
    List<PurchaseRequest> approved = requests.findByApprovalStatusAndStatusOrderByCreatedAtDesc(
        ApprovalStatus.APPROVED, PurchaseRequestStatus.APPROVED);
    Set<UUID> activeInquiryRequestIds = inquiryRequests
        .findByInquiryStatusIn(List.of("OPEN", "AWARDED")).stream()
        .map(ProcurementInquiryRequest::getRequestId)
        .collect(Collectors.toSet());
    Map<UUID, BigDecimal> orderedByRequest = orders
        .findByRequestIdNotNullAndStatusNot(PurchaseOrderStatus.CANCELLED).stream()
        .collect(Collectors.groupingBy(
            PurchaseOrder::getRequestId,
            Collectors.reducing(BigDecimal.ZERO, PurchaseOrder::getOrderedQty, BigDecimal::add)));
    Map<UUID, InventoryPart> partMap = parts.findAllById(
        approved.stream().map(PurchaseRequest::getPartId)
            .filter(Objects::nonNull).distinct().toList()
    ).stream().collect(Collectors.toMap(InventoryPart::getId, Function.identity()));
    Map<UUID, MutableSuggestion> byPart = new LinkedHashMap<>();
    for (PurchaseRequest request : approved) {
      UUID partId = request.getPartId();
      if (partId == null || activeInquiryRequestIds.contains(request.getId())) continue;
      BigDecimal ordered = orderedByRequest.getOrDefault(request.getId(), BigDecimal.ZERO);
      BigDecimal remaining = amount(request.getQuantity()).subtract(ordered);
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;
      InventoryPart part = partMap.get(partId);
      BigDecimal unitPrice = amount(request.getUnitPrice());
      if (unitPrice.compareTo(BigDecimal.ZERO) == 0 && part != null) {
        unitPrice = amount(part.getUnitCost());
      }
      MutableSuggestion suggestion = byPart.computeIfAbsent(partId, key -> new MutableSuggestion(
          StringUtils.hasText(request.getPartName())
              ? request.getPartName().trim()
              : part == null ? "未命名物料" : part.getName()));
      suggestion.totalQty = suggestion.totalQty.add(remaining);
      suggestion.totalAmount = suggestion.totalAmount.add(remaining.multiply(unitPrice));
      suggestion.requestCount++;
    }
    List<CentralPlanSuggestionItem> items = byPart.entrySet().stream()
        .map(entry -> new CentralPlanSuggestionItem(
            entry.getKey(), entry.getValue().partName, entry.getValue().totalQty,
            entry.getValue().totalQty.signum() == 0 ? BigDecimal.ZERO
                : entry.getValue().totalAmount.divide(entry.getValue().totalQty, 2, RoundingMode.HALF_UP),
            entry.getValue().totalAmount, entry.getValue().requestCount))
        .sorted(Comparator.comparing(CentralPlanSuggestionItem::estimatedAmount).reversed())
        .toList();
    return new CentralPlanSuggestionsResponse(year, items.size(), items);
  }

  @Transactional
  public PurchaseRequestResponse convertPlanItemToRequest(
      UUID planId, UUID itemId, UUID departmentId, UUID projectId
  ) {
    CentralPlanItem item = planItems.findById(itemId)
        .orElseThrow(() -> new BusinessException("计划明细不存在"));
    if (!item.getPlanId().equals(planId)) {
      throw new BusinessException("计划明细不属于该计划");
    }
    if (!"PLANNED".equals(item.getStatus())) {
      throw new BusinessException("该计划明细已转入采购申请");
    }
    if (item.getRequestId() != null) {
      throw new BusinessException("该计划明细已转入采购申请");
    }
    CreatePurchaseRequestRequest request = new CreatePurchaseRequestRequest(
        null, currentName(), item.getPartId(), item.getPartName(), item.getPlannedQty(),
        item.getUnitPrice(), null, item.getExpectedDate(),
        "由集采计划「" + (plans.findById(planId).map(CentralPlan::getName).orElse(""))
            + "」转入", com.company.ops.api.modules.procurement.domain.ProcurementCostType.DEPARTMENT,
        projectId, departmentId);
    PurchaseRequestResponse created = procurementService.createPurchaseRequest(request);
    item.setRequestId(created.id());
    item.setStatus("REQUESTED");
    planItems.save(item);
    return created;
  }

  private CentralPlanResponse toPlanResponse(CentralPlan plan) {
    List<CentralPlanItemResponse> items = planItems.findByPlanIdOrderByCreatedAtAsc(plan.getId()).stream()
        .map(item -> {
          String requestCode = null;
          if (item.getRequestId() != null) {
            requestCode = requests.findById(item.getRequestId()).map(PurchaseRequest::getCode).orElse(null);
          }
          return new CentralPlanItemResponse(
              item.getId(), item.getPartId(), item.getPartName(), item.getPlannedQty(),
              item.getUnitPrice(), item.getExpectedDate(), item.getRequestId(), requestCode, item.getStatus());
        })
        .toList();
    return new CentralPlanResponse(
        plan.getId(), plan.getCode(), plan.getName(), plan.getPeriodYear(), plan.getStatus(),
        plan.getRemark(), plan.getCreatedByName(), items);
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }
  private static class MutableSuggestion {
    private final String partName;
    private BigDecimal totalQty = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private int requestCount = 0;

    private MutableSuggestion(String partName) {
      this.partName = partName;
    }
  }
}