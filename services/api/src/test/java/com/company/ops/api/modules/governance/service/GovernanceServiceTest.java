package com.company.ops.api.modules.governance.service;

import static com.company.ops.api.modules.governance.dto.GovernanceDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.crm.repository.ReceivableReceiptRepository;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.governance.domain.BusinessControlRecord;
import com.company.ops.api.modules.governance.domain.ControlStatus;
import com.company.ops.api.modules.governance.domain.ControlType;
import com.company.ops.api.modules.governance.repository.AccountingPeriodRepository;
import com.company.ops.api.modules.governance.repository.BankStatementLineRepository;
import com.company.ops.api.modules.governance.repository.BusinessControlRecordRepository;
import com.company.ops.api.modules.governance.repository.GovernanceActionLogRepository;
import com.company.ops.api.modules.ledger.repository.AccountingVoucherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GovernanceServiceTest {
  @Mock private BusinessControlRecordRepository controls;
  @Mock private AccountingPeriodRepository periods;
  @Mock private BankStatementLineRepository bankLines;
  @Mock private AccountingVoucherRepository vouchers;
  @Mock private PaymentRecordRepository payments;
  @Mock private ReceivableReceiptRepository receipts;
  @Mock private GovernanceActionLogRepository actionLogs;

  @Test
  void detectsOverdueBudgetAndReviewExceptions() {
    BusinessControlRecord item = new BusinessControlRecord();
    item.setId(UUID.randomUUID()); item.setControlCode("CTL-TEST");
    item.setControlType(ControlType.PROJECT_FORECAST); item.setBusinessDomain("PROJECT");
    item.setName("项目完工预测"); item.setOwner("项目经理"); item.setStatus(ControlStatus.ACTIVE);
    item.setRiskLevel("HIGH"); item.setPlannedEnd(LocalDate.now().minusDays(1));
    item.setNextReviewOn(LocalDate.now()); item.setBudgetAmount(new BigDecimal("100"));
    item.setCommittedAmount(new BigDecimal("120")); item.setActualAmount(new BigDecimal("90"));
    item.setForecastAmount(new BigDecimal("150")); item.setProgressPercent(new BigDecimal("60"));
    when(controls.findByStatusInOrderByCreatedAtDesc(anyCollection())).thenReturn(List.of(item));

    List<ControlExceptionResponse> result = service().exceptions();

    assertThat(result).extracting(ControlExceptionResponse::exceptionType)
        .containsExactlyInAnyOrder("OVERDUE", "REVIEW_DUE", "COMMITMENT_OVERRUN", "FORECAST_OVERRUN");
    assertThat(result).filteredOn(row -> row.exceptionType().equals("FORECAST_OVERRUN"))
        .singleElement().extracting(ControlExceptionResponse::exposureAmount)
        .isEqualTo(new BigDecimal("50.00"));
  }

  @Test
  void requiresForecastBaselineBeforeActivation() {
    UUID id = UUID.randomUUID();
    BusinessControlRecord item = new BusinessControlRecord();
    item.setId(id); item.setControlType(ControlType.PROJECT_FORECAST); item.setBusinessDomain("PROJECT");
    item.setName("项目预测"); item.setOwner("负责人"); item.setStatus(ControlStatus.DRAFT);
    item.setBudgetAmount(BigDecimal.ZERO); item.setCommittedAmount(BigDecimal.ZERO);
    item.setActualAmount(BigDecimal.ZERO); item.setForecastAmount(BigDecimal.ZERO); item.setProgressPercent(BigDecimal.ZERO);
    when(controls.findById(id)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> service().transition(id, new TransitionControlRequest(ControlStatus.ACTIVE, null)))
        .isInstanceOf(BusinessException.class).hasMessageContaining("预算或基准金额");
  }

  @Test
  void rejectsInvalidBankDirectionsBeforePersisting() {
    ImportBankLine line = new ImportBankLine("6222****1234", LocalDate.now(), "SIDEWAYS",
        new BigDecimal("100"), "客户", "REF-1", null);

    assertThatThrownBy(() -> service().importBankStatement(new ImportBankStatementRequest(List.of(line))))
        .isInstanceOf(BusinessException.class).hasMessageContaining("IN 或 OUT");
  }

  @Test
  void createsTypedControlWithCanonicalDomainAndJsonDetails() {
    when(controls.existsByControlCode(any())).thenReturn(false);
    when(controls.save(any())).thenAnswer(invocation -> {
      BusinessControlRecord item = invocation.getArgument(0);
      item.setId(UUID.randomUUID());
      return item;
    });
    SaveControlRequest request = new SaveControlRequest(ControlType.SERVICE_SLA, null, "SC-01", "首次响应SLA",
        "服务经理", "MEDIUM", LocalDate.now(), LocalDate.now().plusDays(30), null, null,
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 7,
        Map.of("responseHours", 2));

    ControlResponse result = service().createControl(request);

    assertThat(result.businessDomain()).isEqualTo("SERVICE");
    assertThat(result.details()).containsEntry("responseHours", 2);
    assertThat(result.controlCode()).startsWith("CTL-SER-");
  }

  private GovernanceService service() {
    return new GovernanceService(controls, periods, bankLines, vouchers, payments, receipts, actionLogs, new ObjectMapper());
  }
}
