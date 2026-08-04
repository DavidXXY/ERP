package com.company.ops.api.modules.ledger.dto;
import com.company.ops.api.modules.ledger.domain.VoucherStatus; import jakarta.validation.Valid; import jakarta.validation.constraints.DecimalMin; import jakarta.validation.constraints.Max; import jakarta.validation.constraints.Min; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotEmpty; import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Size; import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import java.util.List; import java.util.UUID;
public final class LedgerDtos { private LedgerDtos(){}
  public record PostingLine(String accountCode,String accountName,BigDecimal debit,BigDecimal credit,String summary){}
  public record ManualPostingLine(@NotBlank @Size(max=32) String accountCode,@NotBlank @Size(max=120) String accountName,BigDecimal debit,BigDecimal credit,@Size(max=300) String summary){}
  public record CreateVoucherRequest(@NotBlank @Size(max=60) String bizType,@NotBlank @Size(max=80) String bizNo,@NotNull LocalDate voucherDate,@NotBlank @Size(max=500) String description,@NotEmpty @Size(max=200) List<@Valid ManualPostingLine> lines){}
  public record ReverseVoucherRequest(@NotNull LocalDate reversalDate,@NotBlank @Size(min=5,max=500) String reason){}
  public record EntryResponse(UUID id,String accountCode,String accountName,BigDecimal debit,BigDecimal credit,String summary){}
  public record VoucherResponse(UUID id,String code,String bizType,String bizNo,LocalDate voucherDate,String description,VoucherStatus status,BigDecimal totalDebit,BigDecimal totalCredit,List<EntryResponse> entries,OffsetDateTime reviewedAt,String reviewedBy,OffsetDateTime postedAt,String postedBy,OffsetDateTime reversedAt,String reversedBy,String reversalReason,UUID reversalVoucherId){}
  public record LedgerOverview(long voucherCount,BigDecimal totalDebit,BigDecimal totalCredit,BigDecimal revenue,BigDecimal expense,BigDecimal profit,BigDecimal cashBalance){}
  public record StatementLine(String accountCode,String accountName,BigDecimal debit,BigDecimal credit,BigDecimal balance){}
  public record FinancialStatements(LocalDate from,LocalDate to,List<StatementLine> assets,List<StatementLine> liabilities,List<StatementLine> equity,List<StatementLine> revenue,List<StatementLine> expenses,BigDecimal totalAssets,BigDecimal totalLiabilities,BigDecimal totalEquity,BigDecimal totalLiabilitiesAndEquity,BigDecimal totalRevenue,BigDecimal totalExpense,BigDecimal profit,BigDecimal netCashFlow){}
  public record AccountResponse(UUID id,String code,String name,String category,String normalDirection,boolean cashAccount,boolean active,boolean systemAccount){}
  public record SaveAccountRequest(@NotBlank @Size(max=32) String code,@NotBlank @Size(max=120) String name,@NotBlank @Size(max=24) String category,@NotBlank @Size(max=12) String normalDirection,boolean cashAccount,boolean active){}
  public record OpeningBalanceResponse(UUID id,int fiscalYear,String accountCode,String accountName,BigDecimal debitBalance,BigDecimal creditBalance,String note){}
  public record SaveOpeningBalanceRequest(@Min(2000) @Max(2200) int fiscalYear,@NotBlank @Size(max=32) String accountCode,@NotNull @DecimalMin("0") BigDecimal debitBalance,@NotNull @DecimalMin("0") BigDecimal creditBalance,@Size(max=500) String note){}
}
