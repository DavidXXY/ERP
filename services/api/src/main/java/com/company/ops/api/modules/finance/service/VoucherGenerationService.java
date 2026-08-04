package com.company.ops.api.modules.finance.service;

import static com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.ledger.domain.AccountingAccount;
import com.company.ops.api.modules.ledger.domain.AccountingVoucher;
import com.company.ops.api.modules.ledger.repository.AccountingAccountRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class VoucherGenerationService {
  private final JdbcTemplate jdbc;
  private final LedgerService ledger;
  private final AccountingAccountRepository accounts;

  public VoucherGenerationService(JdbcTemplate jdbc, LedgerService ledger,
      AccountingAccountRepository accounts) {
    this.jdbc = jdbc;
    this.ledger = ledger;
    this.accounts = accounts;
  }

  public UUID generate(String idempotencyKey, String sourceType, String businessNo,
      LocalDate date, String description, String debitCode, String creditCode, BigDecimal amount) {
    String tenant = tenant();
    UUID requestId = ensureRequest(tenant, idempotencyKey, sourceType, businessNo);
    String status = jdbc.queryForObject(
        "select status from fin_voucher_generation_requests where tenant_id=? and id=?",
        String.class, tenant, requestId);
    if ("SUCCEEDED".equals(status)) {
      return jdbc.queryForObject(
          "select voucher_id from fin_voucher_generation_requests where tenant_id=? and id=?",
          UUID.class, tenant, requestId);
    }
    jdbc.update("update fin_voucher_generation_requests set status='PROCESSING', attempt_count=attempt_count+1, "
        + "last_attempt_at=?, last_error=null, updated_at=? where tenant_id=? and id=?",
        OffsetDateTime.now(), OffsetDateTime.now(), tenant, requestId);
    try {
      AccountingAccount debit = requireAccount(debitCode);
      AccountingAccount credit = requireAccount(creditCode);
      AccountingVoucher voucher = ledger.post(sourceType, businessNo, date, description, List.of(
          new PostingLine(debit.getCode(), debit.getName(), amount, BigDecimal.ZERO, description),
          new PostingLine(credit.getCode(), credit.getName(), BigDecimal.ZERO, amount, description)));
      jdbc.update("update fin_voucher_generation_requests set status='SUCCEEDED', voucher_id=?, completed_at=?, updated_at=? "
          + "where tenant_id=? and id=?", voucher.getId(), OffsetDateTime.now(), OffsetDateTime.now(), tenant, requestId);
      return voucher.getId();
    } catch (RuntimeException ex) {
      jdbc.update("update fin_voucher_generation_requests set status='FAILED', last_error=?, updated_at=? where tenant_id=? and id=?",
          limit(ex.getMessage()), OffsetDateTime.now(), tenant, requestId);
      throw ex;
    }
  }

  public UUID compensate(String idempotencyKey, LocalDate reversalDate, String reason) {
    String tenant = tenant();
    var rows = jdbc.query("select id,source_type,business_no,status from fin_voucher_generation_requests "
            + "where tenant_id=? and idempotency_key=?",
        (rs, n) -> new RequestRef(rs.getObject("id", UUID.class), rs.getString("source_type"),
            rs.getString("business_no"), rs.getString("status")), tenant, idempotencyKey);
    if (rows.isEmpty()) throw new BusinessException("凭证生成请求不存在");
    RequestRef request = rows.get(0);
    if ("COMPENSATED".equals(request.status())) {
      return jdbc.queryForObject("select voucher_id from fin_voucher_generation_requests where tenant_id=? and id=?",
          UUID.class, tenant, request.id());
    }
    if (!"SUCCEEDED".equals(request.status())) throw new BusinessException("只有成功的凭证请求可以补偿冲销");
    var original = ledger.reverseBusinessVoucher(request.sourceType(), request.businessNo(), reversalDate, reason);
    jdbc.update("update fin_voucher_generation_requests set status='COMPENSATED', updated_at=? where tenant_id=? and id=?",
        OffsetDateTime.now(), tenant, request.id());
    return original.reversalVoucherId();
  }

  private UUID ensureRequest(String tenant, String key, String source, String businessNo) {
    var ids = jdbc.query("select id from fin_voucher_generation_requests where tenant_id=? and idempotency_key=?",
        (rs, n) -> rs.getObject(1, UUID.class), tenant, key);
    if (!ids.isEmpty()) return ids.get(0);
    UUID id = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    try {
      jdbc.update("insert into fin_voucher_generation_requests(id,tenant_id,idempotency_key,source_type,business_no,status,"
              + "attempt_count,created_at,updated_at,created_by,version) values(?,?,?,?,?,'PENDING',0,?,?,?,0)",
          id, tenant, key, source, businessNo, now, now, username());
      return id;
    } catch (DuplicateKeyException ignored) {
      return jdbc.queryForObject("select id from fin_voucher_generation_requests where tenant_id=? and idempotency_key=?",
          UUID.class, tenant, key);
    }
  }

  private AccountingAccount requireAccount(String code) {
    AccountingAccount account = accounts.findByCode(code.trim())
        .orElseThrow(() -> new BusinessException("会计科目不存在：" + code));
    if (!account.isActive()) throw new BusinessException("会计科目已停用：" + code);
    return account;
  }

  private UserPrincipal principal() {
    Object value = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (value instanceof UserPrincipal principal) return principal;
    throw new BusinessException("当前用户身份无效");
  }
  private String tenant() { return principal().tenantId(); }
  private String username() { return principal().getUsername(); }
  private String limit(String value) {
    String text = value == null ? "凭证生成失败" : value;
    return text.length() > 1000 ? text.substring(0, 1000) : text;
  }
  private record RequestRef(UUID id, String sourceType, String businessNo, String status) {}
}
