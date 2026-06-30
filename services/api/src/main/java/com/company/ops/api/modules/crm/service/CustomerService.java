package com.company.ops.api.modules.crm.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.domain.CustomerContact;
import com.company.ops.api.modules.crm.domain.CustomerSite;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.domain.RiskStatus;
import com.company.ops.api.modules.crm.dto.CreateCustomerRequest;
import com.company.ops.api.modules.crm.dto.CustomerDetailResponse;
import com.company.ops.api.modules.crm.dto.CustomerSummaryResponse;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.OpportunityRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.crm.repository.ServiceContractRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final OpportunityRepository opportunityRepository;
  private final ServiceContractRepository contractRepository;
  private final ReceivableRepository receivableRepository;
  private final DataScopeService dataScopeService;

  public CustomerService(
      CustomerRepository customerRepository,
      OpportunityRepository opportunityRepository,
      ServiceContractRepository contractRepository,
      ReceivableRepository receivableRepository,
      DataScopeService dataScopeService
  ) {
    this.customerRepository = customerRepository;
    this.opportunityRepository = opportunityRepository;
    this.contractRepository = contractRepository;
    this.receivableRepository = receivableRepository;
    this.dataScopeService = dataScopeService;
  }

  @Transactional(readOnly = true)
  public List<CustomerSummaryResponse> listCustomers() {
    return customerRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(customer -> dataScopeService.canViewOwner(customer.getOwnerName()))
        .map(this::toSummary)
        .toList();
  }

  @Transactional(readOnly = true)
  public CustomerDetailResponse getCustomer(UUID id) {
    Customer customer = customerRepository.findById(id)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (!dataScopeService.canViewOwner(customer.getOwnerName())) throw new BusinessException("无权查看该客户");
    var contracts = contractRepository.findByCustomerIdOrderByStartDateDesc(id);
    var opportunities = opportunityRepository.findByCustomerIdOrderByUpdatedAtDesc(id);
    var receivables = receivableRepository.findByCustomerIdOrderByDueDateAsc(id);

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
        new CustomerDetailResponse.CustomerMetrics(
            contracts.size(),
            contractAmount,
            outstanding,
            settled
        )
    );
  }

  @Transactional
  public CustomerDetailResponse createCustomer(CreateCustomerRequest request) {
    if (customerRepository.existsByCode(request.code())) {
      throw new BusinessException("客户编码已存在");
    }

    Customer customer = new Customer();
    customer.setCode(request.code());
    customer.setName(request.name());
    customer.setIndustry(request.industry());
    customer.setLevel(request.level());
    customer.setOwnerName(request.ownerName());
    customer.setPaymentHabit(request.paymentHabit());
    customer.setRiskStatus(request.riskStatus() == null ? RiskStatus.NORMAL : request.riskStatus());
    customer.setRiskNote(request.riskNote());

    if (request.invoice() != null) {
      customer.setInvoiceTitle(request.invoice().title());
      customer.setTaxNo(request.invoice().taxNo());
      customer.setBankName(request.invoice().bankName());
      customer.setBankAccount(request.invoice().bankAccount());
      customer.setRegisteredAddress(request.invoice().registeredAddress());
      customer.setRegisteredPhone(request.invoice().registeredPhone());
    }

    if (request.contacts() != null) {
      request.contacts().forEach(contactRequest -> {
        CustomerContact contact = new CustomerContact();
        contact.setName(contactRequest.name());
        contact.setTitle(contactRequest.title());
        contact.setPhone(contactRequest.phone());
        contact.setEmail(contactRequest.email());
        contact.setPrimaryContact(contactRequest.primaryContact());
        customer.addContact(contact);
      });
    }

    if (request.sites() != null) {
      request.sites().forEach(siteRequest -> {
        CustomerSite site = new CustomerSite();
        site.setName(siteRequest.name());
        site.setAddress(siteRequest.address());
        customer.addSite(site);
      });
    }

    Customer saved = customerRepository.save(customer);
    return getCustomer(saved.getId());
  }

  private CustomerSummaryResponse toSummary(Customer customer) {
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
        primaryContact
    );
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
