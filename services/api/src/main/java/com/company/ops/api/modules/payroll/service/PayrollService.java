package com.company.ops.api.modules.payroll.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.payroll.domain.PayrollConfig;
import com.company.ops.api.modules.payroll.domain.PayrollRun;
import com.company.ops.api.modules.payroll.domain.PayrollRunLine;
import com.company.ops.api.modules.payroll.domain.PayrollSalaryItem;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.ConfigRequest;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.ConfigResponse;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.PayrollRunResponse;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.RunLineResponse;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.SalaryItemRequest;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.SalaryItemResponse;
import com.company.ops.api.modules.payroll.dto.PayrollDtos.SocialTaxLedgerResponse;
import com.company.ops.api.modules.payroll.repository.PayrollConfigRepository;
import com.company.ops.api.modules.payroll.repository.PayrollRunLineRepository;
import com.company.ops.api.modules.payroll.repository.PayrollRunRepository;
import com.company.ops.api.modules.payroll.repository.PayrollSalaryItemRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollService {

  private static final Set<String> ITEM_TYPES = Set.of("BASE", "ALLOWANCE", "BONUS", "DEDUCTION", "SOCIAL", "TAX");

  private final PayrollSalaryItemRepository salaryItemRepository;
  private final PayrollRunRepository runRepository;
  private final PayrollRunLineRepository runLineRepository;
  private final PayrollConfigRepository configRepository;
  private final CodeGenerator codeGenerator;
  private final LedgerService ledgerService;

  public PayrollService(PayrollSalaryItemRepository salaryItemRepository,
      PayrollRunRepository runRepository, PayrollRunLineRepository runLineRepository,
      PayrollConfigRepository configRepository, CodeGenerator codeGenerator,
      LedgerService ledgerService) {
    this.salaryItemRepository = salaryItemRepository;
    this.runRepository = runRepository;
    this.runLineRepository = runLineRepository;
    this.configRepository = configRepository;
    this.codeGenerator = codeGenerator;
    this.ledgerService = ledgerService;
  }

  // ==================== 薪资项 CRUD ====================

  @Transactional(readOnly = true)
  public Page<SalaryItemResponse> listItems(UUID employeeId, Pageable pageable) {
    Page<PayrollSalaryItem> page = employeeId == null
        ? salaryItemRepository.findAllByOrderByCreatedAtDesc(pageable)
        : salaryItemRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId, pageable);
    return page.map(this::toItemResponse);
  }

  @Transactional
  public SalaryItemResponse createItem(SalaryItemRequest request) {
    validateItemType(request.itemType());
    PayrollSalaryItem item = new PayrollSalaryItem();
    applyItem(item, request);
    item.setCreatedBy(currentUsername());
    return toItemResponse(salaryItemRepository.save(item));
  }

  @Transactional
  public SalaryItemResponse updateItem(UUID id, SalaryItemRequest request) {
    validateItemType(request.itemType());
    PayrollSalaryItem item = salaryItemRepository.findById(id)
        .orElseThrow(() -> new BusinessException("薪资项不存在"));
    applyItem(item, request);
    item.setUpdatedBy(currentUsername());
    return toItemResponse(salaryItemRepository.save(item));
  }

  @Transactional
  public void deleteItem(UUID id) {
    PayrollSalaryItem item = salaryItemRepository.findById(id)
        .orElseThrow(() -> new BusinessException("薪资项不存在"));
    salaryItemRepository.delete(item);
  }

  // ==================== 工资核算 ====================

  @Transactional
  public PayrollRunResponse runPayroll(String period) {
    YearMonth yearMonth = parsePeriod(period);
    String normalized = yearMonth.toString();
    if (runRepository.existsByPeriod(normalized)) {
      throw new BusinessException("该期间已生成工资核算：" + normalized);
    }
    List<PayrollSalaryItem> items = salaryItemRepository.findByActiveTrueOrderByEmployeeIdAsc();
    if (items.isEmpty()) {
      throw new BusinessException("没有可核算的薪资项");
    }
    Map<UUID, List<PayrollSalaryItem>> byEmployee = items.stream()
        .collect(Collectors.groupingBy(PayrollSalaryItem::getEmployeeId,
            LinkedHashMap::new, Collectors.toList()));

    BigDecimal socialRate = configValue("social_rate", new BigDecimal("0.105"));
    BigDecimal threshold = configValue("tax_threshold", new BigDecimal("5000"));

    PayrollRun run = new PayrollRun();
    run.setCode(codeGenerator.generate("PAYROLL_RUN"));
    run.setPeriod(normalized);
    run.setRunDate(yearMonth.atEndOfMonth());
    run.setStatus("CONFIRMED");
    run.setCreatedBy(currentUsername());

    List<PayrollRunLine> lines = new ArrayList<>();
    BigDecimal totalGross = BigDecimal.ZERO;
    BigDecimal totalSocial = BigDecimal.ZERO;
    BigDecimal totalTax = BigDecimal.ZERO;
    BigDecimal totalNet = BigDecimal.ZERO;
    BigDecimal totalDeductions = BigDecimal.ZERO;

    for (Map.Entry<UUID, List<PayrollSalaryItem>> entry : byEmployee.entrySet()) {
      UUID employeeId = entry.getKey();
      List<PayrollSalaryItem> empItems = entry.getValue();
      BigDecimal base = sumByType(empItems, "BASE");
      BigDecimal allowances = sumByType(empItems, "ALLOWANCE");
      BigDecimal bonus = sumByType(empItems, "BONUS");
      BigDecimal deductions = sumByType(empItems, "DEDUCTION");
      BigDecimal gross = base.add(allowances).add(bonus);
      BigDecimal social = gross.multiply(socialRate).setScale(2, RoundingMode.HALF_UP);
      BigDecimal tax = calcTax(gross, social, threshold);
      BigDecimal net = gross.subtract(social).subtract(tax).subtract(deductions);
      if (net.signum() < 0) {
        throw new BusinessException("员工 " + employeeId + " 的实发工资为负数，请检查扣款项");
      }

      PayrollRunLine line = new PayrollRunLine();
      line.setEmployeeId(employeeId);
      line.setBaseSalary(base);
      line.setAllowances(allowances);
      line.setBonus(bonus);
      line.setDeductions(deductions);
      line.setSocialInsurance(social);
      line.setTax(tax);
      line.setNetPay(net);
      lines.add(line);

      totalGross = totalGross.add(gross);
      totalSocial = totalSocial.add(social);
      totalTax = totalTax.add(tax);
      totalNet = totalNet.add(net);
      totalDeductions = totalDeductions.add(deductions);
    }

    run.setTotalGross(totalGross);
    run.setTotalSocial(totalSocial);
    run.setTotalTax(totalTax);
    run.setTotalNet(totalNet);
    PayrollRun saved = runRepository.save(run);

    for (PayrollRunLine line : lines) {
      line.setRunId(saved.getId());
    }
    runLineRepository.saveAll(lines);

    postAccrualVoucher(saved, totalDeductions);
    return toRunResponse(saved, lines);
  }

  @Transactional(readOnly = true)
  public Page<PayrollRunResponse> listRuns(Pageable pageable) {
    return runRepository.findAllByOrderByPeriodDesc(pageable)
        .map(run -> toRunResponse(run, List.of()));
  }

  @Transactional(readOnly = true)
  public PayrollRunResponse getRun(UUID id) {
    PayrollRun run = runRepository.findById(id)
        .orElseThrow(() -> new BusinessException("工资核算不存在"));
    List<PayrollRunLine> lines = runLineRepository.findByRunIdOrderByCreatedAtAsc(run.getId());
    return toRunResponse(run, lines);
  }

  // ==================== 发放 ====================

  @Transactional
  public PayrollRunResponse pay(String period) {
    String normalized = parsePeriod(period).toString();
    PayrollRun run = runRepository.findByPeriod(normalized)
        .orElseThrow(() -> new BusinessException("工资核算不存在：" + normalized));
    if (!"CONFIRMED".equals(run.getStatus())) {
      throw new BusinessException("只有已确认的工资核算可以发放");
    }
    List<PayrollRunLine> lines = runLineRepository.findByRunIdOrderByCreatedAtAsc(run.getId());
    BigDecimal deductions = lines.stream().map(PayrollRunLine::getDeductions)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal payable = run.getTotalNet().add(deductions);
    if (payable.signum() <= 0) {
      throw new BusinessException("发放金额必须大于零");
    }

    run.setStatus("PAID");
    run.setUpdatedBy(currentUsername());
    PayrollRun saved = runRepository.save(run);

    ledgerService.post("PAYROLL_PAYMENT", saved.getCode(), saved.getRunDate(),
        "工资发放：" + normalized, List.of(
            new PostingLine("2211", "应付职工薪酬", payable, BigDecimal.ZERO, "发放工资"),
            new PostingLine("1002", "银行存款", BigDecimal.ZERO, payable, "银行代发工资")
        ));
    return toRunResponse(saved, lines);
  }

  // ==================== 参数 ====================

  @Transactional(readOnly = true)
  public List<ConfigResponse> listConfigs() {
    return configRepository.findAllByOrderByConfigKeyAsc().stream()
        .map(this::toConfigResponse).toList();
  }

  @Transactional
  public ConfigResponse saveConfig(ConfigRequest request) {
    String key = request.configKey().trim();
    PayrollConfig config = configRepository.findByConfigKey(key).orElseGet(PayrollConfig::new);
    config.setConfigKey(key);
    config.setConfigValue(amount(request.configValue()));
    config.setRemark(trim(request.remark()));
    config.setUpdatedBy(currentUsername());
    return toConfigResponse(configRepository.save(config));
  }

  // ==================== 社保个税台账 ====================

  @Transactional(readOnly = true)
  public List<SocialTaxLedgerResponse> socialTaxLedger() {
    return runRepository.findAllByOrderByPeriodDesc().stream()
        .map(run -> new SocialTaxLedgerResponse(run.getPeriod(), run.getCode(),
            run.getTotalSocial(), run.getTotalTax(), run.getTotalNet(), run.getStatus()))
        .toList();
  }

  // ==================== 内部工具 ====================

  private void postAccrualVoucher(PayrollRun run, BigDecimal totalDeductions) {
    BigDecimal payable = run.getTotalNet().add(totalDeductions);
    List<PostingLine> lines = new ArrayList<>();
    lines.add(new PostingLine("6602", "管理费用", run.getTotalGross(), BigDecimal.ZERO, "计提工资薪酬"));
    lines.add(new PostingLine("2211", "应付职工薪酬", BigDecimal.ZERO, payable, "计提应付工资"));
    if (run.getTotalSocial().signum() > 0) {
      lines.add(new PostingLine("2212", "其他应付款-社保公积金", BigDecimal.ZERO,
          run.getTotalSocial(), "代扣社保公积金"));
    }
    if (run.getTotalTax().signum() > 0) {
      lines.add(new PostingLine("22210102", "应交个人所得税", BigDecimal.ZERO,
          run.getTotalTax(), "代扣个人所得税"));
    }
    ledgerService.post("PAYROLL", run.getCode(), run.getRunDate(),
        "工资核算：" + run.getPeriod(), lines);
  }

  private BigDecimal calcTax(BigDecimal gross, BigDecimal social, BigDecimal threshold) {
    BigDecimal taxable = gross.subtract(social).subtract(threshold);
    if (taxable.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal rate;
    BigDecimal quick;
    if (taxable.compareTo(new BigDecimal("3000")) <= 0) {
      rate = new BigDecimal("0.03");
      quick = BigDecimal.ZERO;
    } else if (taxable.compareTo(new BigDecimal("12000")) <= 0) {
      rate = new BigDecimal("0.10");
      quick = new BigDecimal("210");
    } else if (taxable.compareTo(new BigDecimal("25000")) <= 0) {
      rate = new BigDecimal("0.20");
      quick = new BigDecimal("1410");
    } else {
      rate = new BigDecimal("0.25");
      quick = new BigDecimal("2660");
    }
    BigDecimal tax = taxable.multiply(rate).subtract(quick);
    if (tax.signum() < 0) {
      return BigDecimal.ZERO;
    }
    return tax.setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal configValue(String key, BigDecimal defaultValue) {
    return configRepository.findByConfigKey(key)
        .map(PayrollConfig::getConfigValue)
        .orElse(defaultValue);
  }

  private BigDecimal sumByType(List<PayrollSalaryItem> items, String type) {
    return items.stream().filter(item -> type.equals(item.getItemType()))
        .map(PayrollSalaryItem::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private YearMonth parsePeriod(String period) {
    try {
      return YearMonth.parse(period == null ? "" : period.trim());
    } catch (RuntimeException e) {
      throw new BusinessException("期间格式必须为 yyyy-MM");
    }
  }

  private void validateItemType(String itemType) {
    if (itemType == null || !ITEM_TYPES.contains(itemType.trim().toUpperCase())) {
      throw new BusinessException("薪资项类型不支持");
    }
  }

  private void applyItem(PayrollSalaryItem item, SalaryItemRequest request) {
    item.setEmployeeId(request.employeeId());
    item.setName(request.name().trim());
    item.setAmount(amount(request.amount()));
    item.setItemType(request.itemType().trim().toUpperCase());
    item.setActive(request.active());
    item.setRemark(trim(request.remark()));
  }

  private SalaryItemResponse toItemResponse(PayrollSalaryItem item) {
    return new SalaryItemResponse(item.getId(), item.getEmployeeId(), item.getName(),
        item.getAmount(), item.getItemType(), item.isActive(), item.getRemark(), item.getCreatedAt());
  }

  private RunLineResponse toLineResponse(PayrollRunLine line) {
    return new RunLineResponse(line.getId(), line.getRunId(), line.getEmployeeId(),
        line.getEmployeeName(), line.getBaseSalary(), line.getAllowances(), line.getBonus(),
        line.getDeductions(), line.getSocialInsurance(), line.getTax(), line.getNetPay());
  }

  private PayrollRunResponse toRunResponse(PayrollRun run, List<PayrollRunLine> lines) {
    return new PayrollRunResponse(run.getId(), run.getCode(), run.getPeriod(), run.getRunDate(),
        run.getTotalGross(), run.getTotalSocial(), run.getTotalTax(), run.getTotalNet(),
        run.getStatus(), lines.stream().map(this::toLineResponse).toList(), run.getCreatedAt());
  }

  private ConfigResponse toConfigResponse(PayrollConfig config) {
    return new ConfigResponse(config.getId(), config.getConfigKey(), config.getConfigValue(),
        config.getRemark());
  }

  private String currentUsername() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
      return principal.getUsername();
    }
    return "system";
  }

  private String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
