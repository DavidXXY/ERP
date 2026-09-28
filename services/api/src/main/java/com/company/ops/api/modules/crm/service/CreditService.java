package com.company.ops.api.modules.crm.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.domain.Customer;
import com.company.ops.api.modules.crm.dto.CreditDtos.CreditInfoResponse;
import com.company.ops.api.modules.crm.dto.CreditDtos.SetCreditLimitRequest;
import com.company.ops.api.modules.crm.repository.CustomerRepository;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.sales.domain.SalesOrderStatus;
import com.company.ops.api.modules.sales.repository.SalesOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditService {

  private final CustomerRepository customerRepository;
  private final ReceivableRepository receivableRepository;
  private final SalesOrderRepository salesOrderRepository;

  public CreditService(CustomerRepository customerRepository,
      ReceivableRepository receivableRepository,
      SalesOrderRepository salesOrderRepository) {
    this.customerRepository = customerRepository;
    this.receivableRepository = receivableRepository;
    this.salesOrderRepository = salesOrderRepository;
  }

  @Transactional(readOnly = true)
  public CreditInfoResponse getCreditInfo(UUID customerId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    BigDecimal receivables = amount(receivableRepository.sumOutstandingByCustomer(customerId));
    BigDecimal openOrders = amount(salesOrderRepository.sumOpenAmountByCustomer(customerId,
        List.of(SalesOrderStatus.APPROVED, SalesOrderStatus.PARTIAL_SHIPPED)));
    return new CreditInfoResponse(customer.getId(), customer.getName(), customer.getCreditLimit(),
        customer.isCreditBlocked(), receivables, openOrders, receivables.add(openOrders));
  }

  @Transactional
  public CreditInfoResponse setCreditLimit(UUID customerId, SetCreditLimitRequest request) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    customer.setCreditLimit(request.creditLimit());
    customer.setCreditBlocked(request.creditBlocked());
    customerRepository.save(customer);
    return getCreditInfo(customerId);
  }

  @Transactional(readOnly = true)
  public void assertCreditAvailable(UUID customerId, BigDecimal additionalAmount) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (customer.isCreditBlocked()) {
      throw new BusinessException("客户已被信用冻结，不能接单");
    }
    BigDecimal limit = customer.getCreditLimit();
    if (limit == null || limit.signum() <= 0) return;
    BigDecimal exposure = amount(receivableRepository.sumOutstandingByCustomer(customerId))
        .add(amount(salesOrderRepository.sumOpenAmountByCustomer(customerId,
            List.of(SalesOrderStatus.APPROVED, SalesOrderStatus.PARTIAL_SHIPPED))));
    if (exposure.add(amount(additionalAmount)).compareTo(limit) > 0) {
      throw new BusinessException("超出客户信用额度：当前敞口 " + exposure.toPlainString()
          + "，额度 " + limit.toPlainString());
    }
  }

  @Transactional(readOnly = true)
  public void assertNotBlocked(UUID customerId) {
    Customer customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new BusinessException("客户不存在"));
    if (customer.isCreditBlocked()) {
      throw new BusinessException("客户已被信用冻结，不能发货");
    }
  }
}