package com.company.ops.api.modules.finance.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;
import static com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.finance.domain.IntercompanyTransaction;
import com.company.ops.api.modules.finance.dto.IntercompanyDtos.*;
import com.company.ops.api.modules.finance.repository.IntercompanyTransactionRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.system.domain.LegalEntity;
import com.company.ops.api.modules.system.repository.LegalEntityRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntercompanyService {

  private final IntercompanyTransactionRepository transactionRepository;
  private final LegalEntityRepository legalEntityRepository;
  private final LedgerService ledgerService;
  private final CodeGenerator codeGenerator;

  public IntercompanyService(IntercompanyTransactionRepository transactionRepository,
      LegalEntityRepository legalEntityRepository, LedgerService ledgerService, CodeGenerator codeGenerator) {
    this.transactionRepository = transactionRepository;
    this.legalEntityRepository = legalEntityRepository;
    this.ledgerService = ledgerService;
    this.codeGenerator = codeGenerator;
  }

  @Transactional(readOnly = true)
  public Page<IntercompanyResponse> list(Pageable pageable) {
    return transactionRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
  }

  @Transactional
  public IntercompanyResponse create(CreateIntercompanyRequest request) {
    String code = request.code() != null && !request.code().isBlank()
        ? request.code().trim() : codeGenerator.generate("INTERCOMPANY");
    if (transactionRepository.existsByCode(code)) throw new BusinessException("内部交易单号已存在");
    if (request.fromEntityId().equals(request.toEntityId())) throw new BusinessException("内部交易双方不能相同");
    legalEntityRepository.findById(request.fromEntityId()).orElseThrow(() -> new BusinessException("发起法人不存在"));
    legalEntityRepository.findById(request.toEntityId()).orElseThrow(() -> new BusinessException("接收法人不存在"));

    IntercompanyTransaction tx = new IntercompanyTransaction();
    tx.setCode(code);
    tx.setFromEntityId(request.fromEntityId());
    tx.setToEntityId(request.toEntityId());
    tx.setAmount(request.amount());
    tx.setDirection(request.direction());
    tx.setTransactionDate(request.transactionDate());
    tx.setReason(request.reason());
    IntercompanyTransaction saved = transactionRepository.save(tx);

    ledgerService.post("INTERCOMPANY", code, request.transactionDate(), "内部交易 " + code,
        java.util.List.of(
            new PostingLine("1221", "内部往来", request.amount(), BigDecimal.ZERO, "内部交易 " + code),
            new PostingLine("2204", "内部往来-应付", BigDecimal.ZERO, request.amount(), "内部交易 " + code)));
    return toResponse(saved);
  }

  private IntercompanyResponse toResponse(IntercompanyTransaction t) {
    LegalEntity from = legalEntityRepository.findById(t.getFromEntityId()).orElse(null);
    LegalEntity to = legalEntityRepository.findById(t.getToEntityId()).orElse(null);
    return new IntercompanyResponse(t.getId(), t.getCode(), t.getFromEntityId(),
        from == null ? null : from.getName(), t.getToEntityId(), to == null ? null : to.getName(),
        amount(t.getAmount()), t.getDirection(), t.getTransactionDate(), t.getReason(), t.getStatus());
  }
}