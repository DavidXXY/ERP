package com.company.ops.api.common.service;

import com.company.ops.api.modules.system.domain.CodeSequence;
import com.company.ops.api.modules.system.repository.CodeSequenceRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodeGenerator {

  private static final Map<String, String> PREFIXES = Map.ofEntries(
    Map.entry("CUSTOMER", "KH"),
    Map.entry("OPPORTUNITY", "SJ"),
    Map.entry("QUOTE", "BJ"),
    Map.entry("CONTRACT", "HT"),
    Map.entry("PROJECT", "XM"),
    Map.entry("PART", "WL"),
    Map.entry("ISSUE", "LD"),
    Map.entry("RETURN_ORDER", "TL"),
    Map.entry("SUPPLIER", "GYS"),
    Map.entry("PURCHASE_REQUEST", "CG"),
    Map.entry("PURCHASE_REQUEST_BATCH", "CGPC"),
    Map.entry("PURCHASE_ORDER", "CGDD"),
    Map.entry("RECEIPT", "RKD"),
    Map.entry("RECEIVABLE", "YS"),
    Map.entry("PAYABLE", "FY"),
    Map.entry("PAYMENT_APPLICATION", "FKSQ"),
    Map.entry("PAYMENT_RECORD", "FKLS"),
    Map.entry("EXPENSE", "BX"),
    Map.entry("OUTSOURCE", "WB"),
    Map.entry("EQUIPMENT", "SB"),
    Map.entry("MAINTENANCE_PLAN", "FWJH"),
    Map.entry("WORK_ORDER", "GZ"),
    Map.entry("LEDGER_VOUCHER", "PZ"),
    Map.entry("DOCUMENT", "DA"),
    Map.entry("ORGANIZATION", "ZZ"),
    Map.entry("QUAL_EMPLOYEE", "YG"),
    Map.entry("QUAL_COMPANY", "GSZZ"),
    Map.entry("EMPLOYEE_CONTRACT", "YGHT"),
    Map.entry("CODE_SEQUENCE", "SEQ")
  );

  private static final int DEFAULT_WIDTH = 4;

  private final CodeSequenceRepository codeSequenceRepository;

  public CodeGenerator(CodeSequenceRepository codeSequenceRepository) {
    this.codeSequenceRepository = codeSequenceRepository;
  }

  @Transactional
  public String generate(String entityType) {
    String prefix = PREFIXES.get(entityType);
    if (prefix == null) {
      throw new IllegalArgumentException("Unknown entity type: " + entityType);
    }
    CodeSequence seq = codeSequenceRepository.findByEntityTypeForUpdate(entityType)
        .orElseGet(() -> codeSequenceRepository.save(new CodeSequence(entityType, prefix)));
    long number = seq.getNextNumber();
    seq.setNextNumber(number + 1);
    codeSequenceRepository.save(seq);
    String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    return prefix + "-" + datePart + "-" + String.format("%0" + DEFAULT_WIDTH + "d", number);
  }

  @Transactional
  public String generate(String entityType, String organizationCode) {
    String base = generate(entityType);
    if (organizationCode == null || organizationCode.isBlank()) return base;
    String[] parts = base.split("-", 3);
    return parts.length == 3 ? parts[0] + "-" + organizationCode.trim().toUpperCase() + "-" + parts[1] + "-" + parts[2] : base;
  }

  public Map<String, String> rules() {
    return PREFIXES;
  }
}
