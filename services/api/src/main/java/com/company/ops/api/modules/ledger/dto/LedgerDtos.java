package com.company.ops.api.modules.ledger.dto;
import com.company.ops.api.modules.ledger.domain.VoucherStatus; import java.math.BigDecimal; import java.time.LocalDate; import java.time.YearMonth; import java.util.List; import java.util.UUID;
public final class LedgerDtos { private LedgerDtos(){}
  public record PostingLine(String accountCode,String accountName,BigDecimal debit,BigDecimal credit,String summary){}
  public record EntryResponse(UUID id,String accountCode,String accountName,BigDecimal debit,BigDecimal credit,String summary){}
  public record VoucherResponse(UUID id,String code,String bizType,String bizNo,LocalDate voucherDate,String description,VoucherStatus status,BigDecimal totalDebit,BigDecimal totalCredit,List<EntryResponse> entries){}
  public record LedgerOverview(long voucherCount,BigDecimal totalDebit,BigDecimal totalCredit,BigDecimal revenue,BigDecimal expense,BigDecimal profit,BigDecimal cashBalance){}
  public record StatementLine(String accountCode,String accountName,BigDecimal debit,BigDecimal credit,BigDecimal balance){}
  public record FinancialStatements(List<StatementLine> assets,List<StatementLine> liabilities,List<StatementLine> revenue,List<StatementLine> expenses,BigDecimal totalAssets,BigDecimal totalLiabilities,BigDecimal totalRevenue,BigDecimal totalExpense,BigDecimal profit,BigDecimal netCashFlow){}
}
