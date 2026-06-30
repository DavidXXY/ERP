package com.company.ops.api.modules.ledger.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.ledger.domain.AccountingEntry;
import com.company.ops.api.modules.ledger.domain.AccountingVoucher;
import com.company.ops.api.modules.ledger.domain.VoucherStatus;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.EntryResponse;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.FinancialStatements;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.LedgerOverview;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.StatementLine;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.VoucherResponse;
import com.company.ops.api.modules.ledger.repository.AccountingEntryRepository;
import com.company.ops.api.modules.ledger.repository.AccountingVoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
  private final AccountingVoucherRepository voucherRepository; private final AccountingEntryRepository entryRepository;
  public LedgerService(AccountingVoucherRepository voucherRepository,AccountingEntryRepository entryRepository){this.voucherRepository=voucherRepository;this.entryRepository=entryRepository;}

  @Transactional
  public AccountingVoucher post(String bizType,String bizNo,LocalDate date,String description,List<PostingLine> lines){
    if(voucherRepository.existsByBizTypeAndBizNo(bizType,bizNo)) return voucherRepository.findAllByOrderByVoucherDateDescCreatedAtDesc().stream().filter(v->v.getBizType().equals(bizType)&&v.getBizNo().equals(bizNo)).findFirst().orElseThrow();
    BigDecimal debit=lines.stream().map(PostingLine::debit).map(this::amount).reduce(BigDecimal.ZERO,BigDecimal::add); BigDecimal credit=lines.stream().map(PostingLine::credit).map(this::amount).reduce(BigDecimal.ZERO,BigDecimal::add);
    if(debit.compareTo(BigDecimal.ZERO)<=0||debit.compareTo(credit)!=0) throw new BusinessException("会计凭证借贷不平衡");
    String code="PZ-"+date.toString().replace("-","")+"-"+String.format("%04d",voucherRepository.count()+1);
    while(voucherRepository.existsByCode(code)) code=code+"A";
    AccountingVoucher voucher=new AccountingVoucher();voucher.setCode(code);voucher.setBizType(bizType);voucher.setBizNo(bizNo);voucher.setVoucherDate(date);voucher.setDescription(description);voucher.setStatus(VoucherStatus.POSTED);voucher.setTotalDebit(debit);voucher.setTotalCredit(credit);AccountingVoucher saved=voucherRepository.save(voucher);
    entryRepository.saveAll(lines.stream().map(line->{AccountingEntry entry=new AccountingEntry();entry.setVoucherId(saved.getId());entry.setAccountCode(line.accountCode());entry.setAccountName(line.accountName());entry.setDebit(amount(line.debit()));entry.setCredit(amount(line.credit()));entry.setSummary(line.summary());return entry;}).toList());return saved;
  }

  @Transactional(readOnly=true) public List<VoucherResponse> vouchers(){return voucherRepository.findAllByOrderByVoucherDateDescCreatedAtDesc().stream().map(this::toResponse).toList();}
  @Transactional(readOnly=true) public LedgerOverview overview(){FinancialStatements s=statements();List<AccountingVoucher> v=voucherRepository.findAllByOrderByVoucherDateDescCreatedAtDesc();return new LedgerOverview(v.size(),v.stream().map(AccountingVoucher::getTotalDebit).reduce(BigDecimal.ZERO,BigDecimal::add),v.stream().map(AccountingVoucher::getTotalCredit).reduce(BigDecimal.ZERO,BigDecimal::add),s.totalRevenue(),s.totalExpense(),s.profit(),s.netCashFlow());}
  @Transactional(readOnly=true) public FinancialStatements statements(){
    Map<String,List<AccountingEntry>> grouped=entryRepository.findAllByOrderByAccountCodeAsc().stream().collect(Collectors.groupingBy(AccountingEntry::getAccountCode));
    List<StatementLine> lines=grouped.values().stream().map(entries->{AccountingEntry first=entries.get(0);BigDecimal debit=entries.stream().map(AccountingEntry::getDebit).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal credit=entries.stream().map(AccountingEntry::getCredit).reduce(BigDecimal.ZERO,BigDecimal::add);boolean creditNature=first.getAccountCode().startsWith("2")||(first.getAccountCode().startsWith("6")&&first.getAccountCode().compareTo("6400")<0);return new StatementLine(first.getAccountCode(),first.getAccountName(),debit,credit,creditNature?credit.subtract(debit):debit.subtract(credit));}).sorted((a,b)->a.accountCode().compareTo(b.accountCode())).toList();
    List<StatementLine> assets=lines.stream().filter(i->i.accountCode().startsWith("1")).toList();List<StatementLine> liabilities=lines.stream().filter(i->i.accountCode().startsWith("2")).toList();List<StatementLine> revenue=lines.stream().filter(i->i.accountCode().startsWith("6")&&i.accountCode().compareTo("6400")<0).toList();List<StatementLine> expenses=lines.stream().filter(i->i.accountCode().startsWith("6")&&i.accountCode().compareTo("6400")>=0).toList();
    BigDecimal totalAssets=sumBalance(assets),totalLiabilities=sumBalance(liabilities),totalRevenue=sumBalance(revenue),totalExpense=sumBalance(expenses);BigDecimal cash=lines.stream().filter(i->i.accountCode().equals("1002")).map(StatementLine::balance).findFirst().orElse(BigDecimal.ZERO);
    return new FinancialStatements(assets,liabilities,revenue,expenses,totalAssets,totalLiabilities,totalRevenue,totalExpense,totalRevenue.subtract(totalExpense),cash);
  }
  private VoucherResponse toResponse(AccountingVoucher v){return new VoucherResponse(v.getId(),v.getCode(),v.getBizType(),v.getBizNo(),v.getVoucherDate(),v.getDescription(),v.getStatus(),v.getTotalDebit(),v.getTotalCredit(),entryRepository.findByVoucherIdOrderByCreatedAtAsc(v.getId()).stream().map(e->new EntryResponse(e.getId(),e.getAccountCode(),e.getAccountName(),e.getDebit(),e.getCredit(),e.getSummary())).toList());}
  private BigDecimal sumBalance(List<StatementLine> lines){return lines.stream().map(StatementLine::balance).reduce(BigDecimal.ZERO,BigDecimal::add);} private BigDecimal amount(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
}
