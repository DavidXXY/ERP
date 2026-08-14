package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.Opportunity;
import com.company.ops.api.modules.crm.domain.OpportunityStage;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.AdvanceOpportunityRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.CreateOpportunityRequest;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.OpportunityResponse;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.QuotePlanRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商机（Opportunity）管理。
 * 从 CrmOperationsService 拆分而来，只负责商机的查询、创建、推进、更新与删除。
 */
@Service
public class CrmOpportunityService {

  private final OpportunityRepository opportunityRepository;
  private final CustomerRepository customerRepository;
  private final QuotePlanRepository quoteRepository;
  private final DeleteGovernanceService deleteGovernanceService;
  private final DataScopeService dataScopeService;
  private final CodeGenerator codeGenerator;
  private final EntityManager entityManager;

  public CrmOpportunityService(
      OpportunityRepository opportunityRepository,
      CustomerRepository customerRepository,
      QuotePlanRepository quoteRepository,
      DeleteGovernanceService deleteGovernanceService,
      DataScopeService dataScopeService,
      CodeGenerator codeGenerator,
      EntityManager entityManager) {
    this.opportunityRepository = opportunityRepository;
    this.customerRepository = customerRepository;
    this.quoteRepository = quoteRepository;
    this.deleteGovernanceService = deleteGovernanceService;
    this.dataScopeService = dataScopeService;
    this.codeGenerator = codeGenerator;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public List<OpportunityResponse> listOpportunities() {
    List<Opportunity> opportunities = deleteGovernanceService.visible("OPPORTUNITY", opportunityRepository.findAllByOrderByUpdatedAtDesc(), Opportunity::getId)
        .stream().filter(this::canAccessOpportunity).toList();
    Map<UUID, Customer> customers = customerMap(opportunities.stream()
        .map(Opportunity::getCustomerId)
        .toList());
    return opportunities.stream().map(item -> toOpportunity(item, customers)).toList();
  }

  @Transactional(readOnly = true)
  public OpportunityResponse getOpportunity(UUID id) {
    Opportunity opportunity = opportunityRepository.findById(id)
        .orElseThrow(() -> new BusinessException("商机不存在"));
    assertOpportunityAccess(opportunity);
    if (deleteGovernanceService.isHidden("OPPORTUNITY", id)) throw new BusinessException("商机不存在");
    return toOpportunity(opportunity, customerMap(nullableId(opportunity.getCustomerId())));
  }

  @Transactional
  public OpportunityResponse createOpportunity(CreateOpportunityRequest request) {
    String oppCode = request.code() != null && !request.code().isBlank()
        ? request.code().trim()
        : codeGenerator.generate("OPPORTUNITY");
    if (opportunityRepository.existsByCode(oppCode)) {
      throw new BusinessException("商机编码已存在");
    }
    validateCustomer(request.customerId());

    Opportunity opportunity = new Opportunity();
    opportunity.setCustomerId(request.customerId());
    opportunity.setCode(oppCode);
    opportunity.setSource(request.source());
    opportunity.setNeedSummary(request.needSummary());
    opportunity.setStage(request.stage() == null ? OpportunityStage.LEAD : request.stage());
    opportunity.setExpectedAmount(defaultAmount(request.expectedAmount()));
    opportunity.setProbability(request.probability() == null ? 10 : request.probability());
    opportunity.setNextAction(request.nextAction());
    opportunity.setNextActionAt(request.nextActionAt());
    opportunity.setOwnerName(request.ownerName());
    opportunity.setOwnerUserId(dataScopeService.requireVisibleOwnerId(request.ownerName()));
    Opportunity saved = opportunityRepository.save(opportunity);
    return toOpportunity(saved, customerMap(nullableId(saved.getCustomerId())));
  }

  @Transactional
  public OpportunityResponse advanceOpportunity(UUID id, AdvanceOpportunityRequest request) {
    Opportunity opportunity = opportunityRepository.findById(id)
        .orElseThrow(() -> new BusinessException("商机不存在"));
    assertOpportunityAccess(opportunity);
    if (opportunity.getStage() == OpportunityStage.WON || opportunity.getStage() == OpportunityStage.LOST) {
      throw new BusinessException("已结束商机不能继续推进");
    }
    if ((request.stage() == OpportunityStage.NEGOTIATION || request.stage() == OpportunityStage.WON)
        && !quoteRepository.existsByOpportunityId(opportunity.getId())) {
      throw new BusinessException("请先为该商机创建报价方案才能继续推进");
    }
    opportunity.setStage(request.stage());
    opportunity.setNextAction(request.nextAction());
    opportunity.setNextActionAt(request.nextActionAt());
    opportunity.setProbability(request.probability());
    Opportunity saved = opportunityRepository.save(opportunity);
    return toOpportunity(saved, customerMap(nullableId(saved.getCustomerId())));
  }

  @Transactional
  public void deleteOpportunity(UUID id) {
    Opportunity opportunity = opportunityRepository.findById(id)
        .orElseThrow(() -> new BusinessException("商机不存在"));
    assertOpportunityAccess(opportunity);
    if (!deleteGovernanceService.allowPhysicalDelete("OPPORTUNITY", id, opportunity.getCode())) {
      return;
    }
    // Cascade delete related records
    entityManager.createNativeQuery("DELETE FROM crm_follow_ups WHERE opportunity_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_revisions WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE opportunity_id = ?1)")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_approval_records WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE opportunity_id = ?1)")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_cost_requests WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE opportunity_id = ?1)")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_plans WHERE opportunity_id = ?1")
        .setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_opportunities WHERE id = ?1")
        .setParameter(1, id).executeUpdate();
  }

  @Transactional
  public OpportunityResponse updateOpportunity(UUID id, CreateOpportunityRequest request) {
    Opportunity opp = opportunityRepository.findById(id)
        .orElseThrow(() -> new BusinessException("\u5546\u673a\u4e0d\u5b58\u5728"));
    assertOpportunityAccess(opp);
    if (request.customerId() != null) {
      assertCustomerAccess(request.customerId());
      opp.setCustomerId(request.customerId());
    }
    if (request.needSummary() != null) opp.setNeedSummary(request.needSummary());
    if (request.expectedAmount() != null) opp.setExpectedAmount(request.expectedAmount());
    if (request.nextAction() != null) opp.setNextAction(request.nextAction());
    if (request.nextActionAt() != null) opp.setNextActionAt(request.nextActionAt());
    if (request.ownerName() != null) {
      opp.setOwnerName(request.ownerName());
      opp.setOwnerUserId(dataScopeService.requireVisibleOwnerId(request.ownerName()));
    }
    return toOpportunity(opportunityRepository.save(opp), customerMap(nullableId(opp.getCustomerId())));
  }

  private OpportunityResponse toOpportunity(Opportunity item, Map<UUID, Customer> customers) {
    return new OpportunityResponse(
        item.getId(),
        item.getCustomerId(),
        customerName(customers, item.getCustomerId()),
        item.getCode(),
        item.getSource(),
        item.getNeedSummary(),
        item.getStage(),
        item.getExpectedAmount(),
        item.getProbability(),
        item.getNextAction(),
        item.getNextActionAt(),
        item.getOwnerName(),
        item.getUpdatedAt()
    );
  }


  private Map<UUID, Customer> customerMap(List<UUID> ids) {
    List<UUID> validIds = distinctIds(ids);
    if (validIds.isEmpty()) {
      return Map.of();
    }
    return customerRepository.findAllById(validIds).stream()
        .collect(Collectors.toMap(Customer::getId, Function.identity()));
  }

  private List<UUID> distinctIds(List<UUID> ids) {
    return ids.stream().filter(id -> id != null).distinct().toList();
  }

  private List<UUID> nullableId(UUID id) {
    return id == null ? List.of() : List.of(id);
  }

  private String customerName(Map<UUID, Customer> customers, UUID id) {
    return id == null || customers.get(id) == null ? "未关联客户" : customers.get(id).getName();
  }

  private void validateCustomer(UUID customerId) {
    if (customerId != null) assertCustomerAccess(customerId);
  }

  private boolean canAccessCustomer(UUID customerId) {
    if (customerId == null) return false;
    return customerRepository.findById(customerId)
        .map(item -> dataScopeService.canViewOwner(item.getOwnerUserId()))
        .orElse(false);
  }


  private void assertCustomerAccess(UUID customerId) {
    Customer customer = customerId == null ? null : customerRepository.findById(customerId).orElse(null);
    if (customer == null) throw new BusinessException("客户不存在");
    if (!dataScopeService.canViewOwner(customer.getOwnerUserId())) throw new BusinessException("无权访问该客户数据");
  }

  private boolean canAccessOpportunity(Opportunity opportunity) {
    return opportunity.getCustomerId() != null
        ? canAccessCustomer(opportunity.getCustomerId())
        : dataScopeService.canViewOwner(opportunity.getOwnerUserId());
  }

  private void assertOpportunityAccess(Opportunity opportunity) {
    if (!canAccessOpportunity(opportunity)) throw new BusinessException("无权访问该商机");
  }

  private BigDecimal defaultAmount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

}
