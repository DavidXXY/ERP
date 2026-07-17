package com.company.ops.api.modules.finance.dto;

import com.company.ops.api.modules.crm.domain.ContractStatus;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.dto.CrmOperationsDtos.ReceivableResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FinanceReceivableDetailResponse(
    ReceivableResponse receivable,
    CustomerInvoiceInfo customerInvoice,
    ContractInfo contract
) {
  public record CustomerInvoiceInfo(
      UUID customerId,
      String customerCode,
      String customerName,
      String ownerName,
      String invoiceTitle,
      String taxNo,
      String bankName,
      String bankAccount,
      String registeredAddress,
      String registeredPhone,
      String paymentHabit
  ) {
  }

  public record ContractInfo(
      UUID id,
      UUID quoteId,
      String code,
      String projectName,
      String contractType,
      BigDecimal amount,
      BigDecimal taxRate,
      BigDecimal netAmount,
      LocalDate startDate,
      LocalDate endDate,
      String serviceCycle,
      ContractStatus status,
      ReceivableStatus receivableStatus,
      OffsetDateTime createdAt
  ) {
  }
}
