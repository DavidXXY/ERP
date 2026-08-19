package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.CustomerContact;
import com.company.ops.api.modules.crm.domain.CustomerLevel;
import com.company.ops.api.modules.crm.domain.CustomerSite;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import com.company.ops.api.modules.crm.dto.CreateCustomerRequest;
import com.company.ops.api.modules.crm.dto.CustomerDetailResponse;
import com.company.ops.api.modules.crm.dto.CustomerPoolStats;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.crm.dto.CustomerSummaryResponse;
import com.company.ops.api.modules.crm.dto.UpdateCustomerRequest;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.FollowUpRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.company.ops.api.common.util.MoneyUtils.amount;

@Service
public class CustomerService {

  private CodeGenerator codeGenerator;
  private final CustomerRepository customerRepository;
  private final OpportunityRepository opportunityRepository;
  private final ServiceContractRepository contractRepository;
  private final ReceivableRepository receivableRepository;
  private final FollowUpRepository followUpRepository;
  private final DataScopeService dataScopeService;
  private final ProjectRepository projectRepository;
  private final DeleteGovernanceService deleteGovernanceService;

  @jakarta.persistence.PersistenceContext
  private jakarta.persistence.EntityManager entityManager;

  public CustomerService(
      CodeGenerator codeGenerator,
      CustomerRepository customerRepository,
      OpportunityRepository opportunityRepository,
      ServiceContractRepository contractRepository,
      ReceivableRepository receivableRepository,
      FollowUpRepository followUpRepository,
      DataScopeService dataScopeService,
      ProjectRepository projectRepository,
      DeleteGovernanceService deleteGovernanceService
  ) {
    this.codeGenerator = codeGenerator;
    this.customerRepository = customerRepository;
    this.opportunityRepository = opportunityRepository;
    this.contractRepository = contractRepository;
    this.receivableRepository = receivableRepository;
    this.followUpRepository = followUpRepository;
    this.dataScopeService = dataScopeService;
    this.projectRepository = projectRepository;
    this.deleteGovernanceService = deleteGovernanceService;
  }

  @Transactional(readOnly = true)
  public List<CustomerSummaryResponse> listCustomers() {
    Set<UUID> visibleOwnerIds = dataScopeService.visibleUserIds();
    List<Customer> customers = visibleOwnerIds.isEmpty()
        ? List.of()
        : customerRepository.findByOwnerUserIdInOrderByCreatedAtDesc(visibleOwnerIds);
    return toSummaries(deleteGovernanceService.visible("CUSTOMER", customers, Customer::getId));
  }

  @Transactional(readOnly = true)
  public Page<CustomerSummaryResponse> listCustomersPage(
      Pageable pageable, String keyword, CustomerLevel level, RiskStatus riskStatus, String ownerNames) {
    Set<UUID> visibleOwnerIds = dataScopeService.visibleUserIds();
    if (visibleOwnerIds.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }
    String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
    List<String> names = ownerNames == null || ownerNames.isBlank()
        ? List.of()
        : java.util.Arrays.stream(ownerNames.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList();
    Page<Customer> page = customerRepository.searchVisibleByOwnerUserIdIn(
        visibleOwnerIds, kw, level, riskStatus, names, pageable);
    return new PageImpl<>(
        toSummaries(deleteGovernanceService.visible("CUSTOMER", page.getContent(), Customer::getId)),
        pageable, page.getTotalElements());
  }

  @Transactional(readOnly = true)
  public CustomerPoolStats summary() {
    Set<UUID> visibleOwnerIds = dataScopeService.visibleUserIds();
    if (visibleOwnerIds.isEmpty()) return new CustomerPoolStats(0, 0, 0, 0);
    List<Customer> customers = deleteGovernanceService.visible("CUSTOMER",
        customerRepository.findByOwnerUserIdInOrderByCreatedAtDesc(visibleOwnerIds), Customer::getId);
    long total = customers.size();
    long strategic = customers.stream().filter(c -> c.getLevel() == CustomerLevel.STRATEGIC).count();
    long risk = customers.stream().filter(c -> c.getRiskStatus() != RiskStatus.NORMAL).count();
    List<UUID> ids = customers.stream().map(Customer::getId).filter(Objects::nonNull).toList();
    Map<UUID, BigDecimal> signed = toCustomerAmountMap(contractRepository.aggregateAmountByCustomerIn(ids));
    Map<UUID, BigDecimal> settled = toCustomerAmountMap(receivableRepository.aggregateSettledByCustomerIn(ids));
    Map<UUID, BigDecimal> outstanding = toCustomerAmountMap(receivableRepository.aggregateOutstandingByCustomerIn(ids));
    long reconciliation = customers.stream().filter(c -> {
      BigDecimal diff = signed.getOrDefault(c.getId(), BigDecimal.ZERO)
          .subtract(settled.getOrDefault(c.getId(), BigDecimal.ZERO))
          .subtract(outstanding.getOrDefault(c.getId(), BigDecimal.ZERO));
      return diff.abs().compareTo(new BigDecimal("0.01")) >= 0;
    }).count();
    return new CustomerPoolStats(total, strategic, risk, reconciliation);
  }

  private List<CustomerSummaryResponse> toSummaries(List<Customer> customers) {
    List<UUID> customerIds = customers.stream().map(Customer::getId)
        .filter(Objects::nonNull).toList();
    Map<UUID, BigDecimal> signedOrderAmounts = customerIds.isEmpty()
        ? Map.of() : toCustomerAmountMap(contractRepository.aggregateAmountByCustomerIn(customerIds));
    Map<UUID, BigDecimal> paidAmounts = customerIds.isEmpty()
        ? Map.of() : toCustomerAmountMap(receivableRepository.aggregateSettledByCustomerIn(customerIds));
    Map<UUID, BigDecimal> pendingAmounts = customerIds.isEmpty()
        ? Map.of() : toCustomerAmountMap(receivableRepository.aggregateOutstandingByCustomerIn(customerIds));
    return customers.stream()
        .map(customer -> toSummary(
            customer,
            signedOrderAmounts.getOrDefault(customer.getId(), BigDecimal.ZERO),
            paidAmounts.getOrDefault(customer.getId(), BigDecimal.ZERO),
            pendingAmounts.getOrDefault(customer.getId(), BigDecimal.ZERO)))
        .toList();
  }

  private Map<UUID, BigDecimal> toCustomerAmountMap(List<Object[]> rows) {
    Map<UUID, BigDecimal> map = new java.util.HashMap<>();
    for (Object[] row : rows) {
      if (row == null || row.length < 2 || row[0] == null) continue;
      map.merge((UUID) row[0], amount((BigDecimal) row[1]), BigDecimal::add);
    }
    return map;
  }

  @Transactional(readOnly = true)
  public CustomerDetailResponse getCustomer(UUID id) {
    Customer customer = customerRepository.findById(id)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (!dataScopeService.canViewOwner(customer.getOwnerUserId())) throw new BusinessException("无权查看该客户");
    if (deleteGovernanceService.isHidden("CUSTOMER", id)) throw new BusinessException("客户不存在");
    var contracts = contractRepository.findByCustomerIdOrderByStartDateDesc(id);
    var opportunities = opportunityRepository.findByCustomerIdOrderByUpdatedAtDesc(id);
    var receivables = receivableRepository.findByCustomerIdOrderByDueDateAsc(id);
    var followUps = followUpRepository.findByCustomerIdOrderByFollowedAtDesc(id);
    int projectCount = projectRepository.findByContractIdIn(
        contracts.stream().map(contract -> contract.getId()).toList()).size();
    Map<UUID, String> opportunityCodes = opportunities.stream()
        .collect(java.util.stream.Collectors.toMap(
            opportunity -> opportunity.getId(),
            opportunity -> opportunity.getCode()
        ));

    BigDecimal contractAmount = contracts.stream()
        .map(contract -> contract.getAmount() == null ? BigDecimal.ZERO : contract.getAmount())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal outstanding = receivables.stream()
        .map(receivable -> amount(receivable.getAmount()).subtract(amount(receivable.getSettledAmount())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal settled = receivables.stream()
        .map(receivable -> amount(receivable.getSettledAmount()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CustomerDetailResponse(
        customer.getId(),
        customer.getCode(),
        customer.getName(),
        customer.getIndustry(),
        customer.getLevel(),
        customer.getOwnerName(),
        customer.getPaymentHabit(),
        customer.getRiskStatus(),
        customer.getRiskNote(),
        new CustomerDetailResponse.Invoice(
            customer.getInvoiceTitle(),
            customer.getTaxNo(),
            customer.getBankName(),
            customer.getBankAccount(),
            customer.getRegisteredAddress(),
            customer.getRegisteredPhone()
        ),
        customer.getContacts().stream()
            .map(contact -> new CustomerDetailResponse.Contact(
                contact.getId(),
                contact.getName(),
                contact.getTitle(),
                contact.getPhone(),
                contact.getEmail(),
                contact.isPrimaryContact()
            ))
            .toList(),
        customer.getSites().stream()
            .map(site -> new CustomerDetailResponse.Site(site.getId(), site.getName(), site.getAddress()))
            .toList(),
        opportunities.stream()
            .map(opportunity -> new CustomerDetailResponse.Opportunity(
                opportunity.getId(),
                opportunity.getCode(),
                opportunity.getSource(),
                opportunity.getNeedSummary(),
                opportunity.getStage(),
                opportunity.getExpectedAmount(),
                opportunity.getProbability(),
                opportunity.getNextAction(),
                opportunity.getNextActionAt(),
                opportunity.getOwnerName()
            ))
            .toList(),
        contracts.stream()
            .map(contract -> new CustomerDetailResponse.Contract(
                contract.getId(),
                contract.getCode(),
                contract.getProjectName(),
                contract.getContractType(),
                contract.getAmount(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getServiceCycle(),
                contract.getStatus()
            ))
            .toList(),
        receivables.stream()
            .map(receivable -> new CustomerDetailResponse.Receivable(
                receivable.getId(),
                receivable.getCode(),
                receivable.getSourceNo(),
                receivable.getAmount(),
                receivable.getDueDate(),
                receivable.getInvoiceNo(),
                receivable.getInvoiceDate(),
                amount(receivable.getSettledAmount()),
                amount(receivable.getAmount()).subtract(amount(receivable.getSettledAmount())),
                receivable.getStatus()
            ))
            .toList(),
        followUps.stream()
            .map(followUp -> new CustomerDetailResponse.FollowUp(
                followUp.getId(),
                followUp.getOpportunityId(),
                followUp.getOpportunityId() == null ? null : opportunityCodes.get(followUp.getOpportunityId()),
                followUp.getType(),
                followUp.getSubject(),
                followUp.getContent(),
                followUp.getFollowedAt(),
                followUp.getNextAction(),
                followUp.getOwnerName()
            ))
            .toList(),
        new CustomerDetailResponse.CustomerMetrics(
            contracts.size(),
            projectCount,
            contractAmount,
            outstanding,
            settled
        )
    );
  }

  @Transactional
  public CustomerDetailResponse createCustomer(CreateCustomerRequest request) {
    String generatedCode = request.code() != null && !request.code().isBlank()
        ? request.code().trim()
        : codeGenerator.generate("CUSTOMER");
    if (customerRepository.existsByCode(generatedCode)) {
      throw new BusinessException("客户编码已存在");
    }

    UUID ownerUserId = dataScopeService.requireVisibleOwnerId(request.ownerName());

    Customer customer = new Customer();
    customer.setCode(generatedCode);
    applyCustomerDetails(customer, request.name(), request.industry(), request.level(),
        request.ownerName(), ownerUserId, request.paymentHabit(), request.riskStatus(), request.riskNote(),
        request.invoice(), request.contacts(), request.sites());

    Customer saved = customerRepository.save(customer);
    return getCustomer(saved.getId());
  }

  @Transactional
  public CustomerDetailResponse updateCustomer(UUID id, UpdateCustomerRequest request) {
    Customer customer = customerRepository.findById(id)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (!dataScopeService.canViewOwner(customer.getOwnerUserId())) {
      throw new BusinessException("无权编辑该客户");
    }
    UUID ownerUserId = dataScopeService.requireVisibleOwnerId(request.ownerName());
    applyCustomerDetails(customer, request.name(), request.industry(), request.level(),
        request.ownerName(), ownerUserId, request.paymentHabit(), request.riskStatus(), request.riskNote(),
        request.invoice(), request.contacts(), request.sites());
    customerRepository.save(customer);
    return getCustomer(id);
  }

  @Transactional
  public void transferOwner(UUID id, String ownerName) {
    Customer customer = customerRepository.findById(id)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (deleteGovernanceService.isHidden("CUSTOMER", id)) throw new BusinessException("客户不存在");
    if (!dataScopeService.canViewOwner(customer.getOwnerUserId())) {
      throw new BusinessException("无权转交该客户");
    }
    UUID ownerUserId = dataScopeService.requireVisibleOwnerId(ownerName);
    customer.setOwnerName(ownerName);
    customer.setOwnerUserId(ownerUserId);
    customerRepository.save(customer);
  }

  private void applyCustomerDetails(
      Customer customer,
      String name,
      String industry,
      com.company.ops.api.modules.crm.domain.CustomerLevel level,
      String ownerName,
      UUID ownerUserId,
      String paymentHabit,
      RiskStatus riskStatus,
      String riskNote,
      CreateCustomerRequest.InvoiceRequest invoice,
      List<CreateCustomerRequest.ContactRequest> contacts,
      List<CreateCustomerRequest.SiteRequest> sites
  ) {
    customer.setName(name);
    customer.setIndustry(industry);
    customer.setLevel(level);
    customer.setOwnerName(ownerName);
    customer.setOwnerUserId(ownerUserId);
    customer.setPaymentHabit(paymentHabit);
    customer.setRiskStatus(riskStatus == null ? RiskStatus.NORMAL : riskStatus);
    customer.setRiskNote(riskNote);

    customer.setInvoiceTitle(invoice == null ? null : invoice.title());
    customer.setTaxNo(invoice == null ? null : invoice.taxNo());
    customer.setBankName(invoice == null ? null : invoice.bankName());
    customer.setBankAccount(invoice == null ? null : invoice.bankAccount());
    customer.setRegisteredAddress(invoice == null ? null : invoice.registeredAddress());
    customer.setRegisteredPhone(invoice == null ? null : invoice.registeredPhone());

    long primaryCount = contacts == null ? 0 : contacts.stream()
        .filter(CreateCustomerRequest.ContactRequest::primaryContact)
        .count();
    if (primaryCount > 1) {
      throw new BusinessException("一个客户只能设置一位主要联系人");
    }
    customer.getContacts().clear();
    if (contacts != null) {
      for (int index = 0; index < contacts.size(); index++) {
        CreateCustomerRequest.ContactRequest request = contacts.get(index);
        CustomerContact contact = new CustomerContact();
        contact.setName(request.name());
        contact.setTitle(request.title());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setPrimaryContact(primaryCount == 0 ? index == 0 : request.primaryContact());
        customer.addContact(contact);
      }
    }

    customer.getSites().clear();
    if (sites != null) {
      sites.forEach(request -> {
        CustomerSite site = new CustomerSite();
        site.setName(request.name());
        site.setAddress(request.address());
        customer.addSite(site);
      });
    }
  }

  @Transactional
  public void deleteCustomer(UUID id) {
    Customer customer = customerRepository.findById(id)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (!dataScopeService.canViewOwner(customer.getOwnerUserId())) throw new BusinessException("无权删除该客户");
    if (!deleteGovernanceService.allowPhysicalDelete("CUSTOMER", id, customer == null ? id.toString() : customer.getCode() + " · " + customer.getName())) {
      return;
    }
    // PostgreSQL enforces every cross-module FK, so delete from leaf tables upward.
    entityManager.createNativeQuery("""
        DELETE FROM hr_field_attendance
        WHERE work_order_id IN (SELECT id FROM work_orders WHERE customer_id = ?1 OR contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1) OR project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM work_order_materials
        WHERE work_order_id IN (SELECT id FROM work_orders WHERE customer_id = ?1 OR contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1) OR project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM work_order_status_logs
        WHERE work_order_id IN (SELECT id FROM work_orders WHERE customer_id = ?1 OR contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1) OR project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM oa_expense_claims
        WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)
           OR work_order_id IN (SELECT id FROM work_orders WHERE customer_id = ?1 OR contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1) OR project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM oa_outsource_orders
        WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)
           OR work_order_id IN (SELECT id FROM work_orders WHERE customer_id = ?1 OR contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1) OR project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM work_orders
        WHERE customer_id = ?1 OR contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1)
           OR project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)
        """).setParameter(1, id).executeUpdate();

    entityManager.createNativeQuery("""
        DELETE FROM inventory_return_lines
        WHERE return_id IN (SELECT id FROM inventory_return_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM inventory_return_orders
        WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM inventory_issue_lines
        WHERE issue_id IN (SELECT id FROM inventory_issue_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM inventory_issue_orders
        WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)
        """).setParameter(1, id).executeUpdate();

    entityManager.createNativeQuery("""
        DELETE FROM fin_payment_records
        WHERE payable_id IN (
          SELECT id FROM fin_procurement_payables
          WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
             OR receipt_id IN (SELECT id FROM procurement_goods_receipts WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)))
        )
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM fin_payment_applications
        WHERE payable_id IN (
          SELECT id FROM fin_procurement_payables
          WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
             OR receipt_id IN (SELECT id FROM procurement_goods_receipts WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)))
        )
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM fin_procurement_payables
        WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
           OR receipt_id IN (SELECT id FROM procurement_goods_receipts WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_cost_allocations
        WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)
           OR order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
           OR receipt_id IN (SELECT id FROM procurement_goods_receipts WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_goods_receipts
        WHERE order_id IN (SELECT id FROM procurement_purchase_orders WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_purchase_orders
        WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_request_approval_records
        WHERE request_id IN (SELECT id FROM procurement_purchase_requests WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM procurement_purchase_requests
        WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)
        """).setParameter(1, id).executeUpdate();

    entityManager.createNativeQuery("""
        DELETE FROM maintenance_plans
        WHERE contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1)
           OR asset_id IN (SELECT id FROM maintenance_equipment_assets WHERE customer_id = ?1 OR contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1))
        """).setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("""
        DELETE FROM maintenance_equipment_assets
        WHERE customer_id = ?1 OR contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1)
        """).setParameter(1, id).executeUpdate();

    entityManager.createNativeQuery("DELETE FROM fin_receivable_receipts WHERE receivable_id IN (SELECT id FROM fin_receivables WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("UPDATE fin_receivables SET contract_id = NULL WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM fin_receivables WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_revisions WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_approval_records WHERE quote_id IN (SELECT id FROM crm_quote_plans WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_follow_ups WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_attachment WHERE entity_type = 'QUOTE' AND entity_id IN (SELECT id FROM crm_quote_plans WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_quote_plans WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_opportunities WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_contract_changes WHERE contract_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_attachment WHERE entity_type = 'CONTRACT' AND entity_id IN (SELECT id FROM crm_service_contracts WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_service_contracts WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_customer_contacts WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_customer_sites WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM project_budget_items WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM project_cost_entries WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM project_stage_records WHERE project_id IN (SELECT id FROM project_projects WHERE customer_id = ?1)").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM project_projects WHERE customer_id = ?1").setParameter(1, id).executeUpdate();
    entityManager.createNativeQuery("DELETE FROM crm_customers WHERE id = ?1").setParameter(1, id).executeUpdate();
  }

  private CustomerSummaryResponse toSummary(
      Customer customer,
      BigDecimal signedOrderAmount,
      BigDecimal paidAmount,
      BigDecimal pendingAmount
  ) {
    String primaryContact = customer.getContacts().stream()
        .filter(CustomerContact::isPrimaryContact)
        .findFirst()
        .or(() -> customer.getContacts().stream().findFirst())
        .map(contact -> contact.getName() + (contact.getPhone() == null ? "" : " / " + contact.getPhone()))
        .orElse("");

    return new CustomerSummaryResponse(
        customer.getId(),
        customer.getCode(),
        customer.getName(),
        customer.getIndustry(),
        customer.getLevel(),
        customer.getOwnerName(),
        customer.getPaymentHabit(),
        customer.getRiskStatus(),
        customer.getContacts().size(),
        customer.getSites().size(),
        primaryContact,
        signedOrderAmount,
        paidAmount,
        pendingAmount,
        signedOrderAmount.subtract(paidAmount).subtract(pendingAmount)
    );
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
