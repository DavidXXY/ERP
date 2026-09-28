package com.company.ops.api.modules.finance.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;
import static com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.finance.domain.ContractMilestone;
import com.company.ops.api.modules.finance.domain.RevenueRecognition;
import com.company.ops.api.modules.finance.dto.RevenueRecognitionDtos.*;
import com.company.ops.api.modules.finance.repository.ContractMilestoneRepository;
import com.company.ops.api.modules.finance.repository.RevenueRecognitionRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueRecognitionService {

  private final ContractMilestoneRepository milestoneRepository;
  private final RevenueRecognitionRepository recognitionRepository;
  private final LedgerService ledgerService;
  private final CodeGenerator codeGenerator;

  public RevenueRecognitionService(ContractMilestoneRepository milestoneRepository,
      RevenueRecognitionRepository recognitionRepository,
      LedgerService ledgerService,
      CodeGenerator codeGenerator) {
    this.milestoneRepository = milestoneRepository;
    this.recognitionRepository = recognitionRepository;
    this.ledgerService = ledgerService;
    this.codeGenerator = codeGenerator;
  }

  @Transactional(readOnly = true)
  public List<MilestoneResponse> listMilestones(UUID contractId) {
    return milestoneRepository.findByContractIdOrderByCreatedAtAsc(contractId).stream()
        .map(this::toMilestoneResponse).toList();
  }

  @Transactional
  public MilestoneResponse createMilestone(CreateMilestoneRequest request) {
    ContractMilestone milestone = new ContractMilestone();
    milestone.setContractId(request.contractId());
    milestone.setName(request.name());
    milestone.setAmount(request.amount());
    milestone.setPlannedDate(request.plannedDate());
    milestone.setRemark(request.remark());
    return toMilestoneResponse(milestoneRepository.save(milestone));
  }

  @Transactional
  public MilestoneResponse recognizeMilestone(UUID milestoneId, RecognizeMilestoneRequest request) {
    ContractMilestone milestone = milestoneRepository.findById(milestoneId)
        .orElseThrow(() -> new BusinessException("履约里程碑不存在"));
    if (!"PENDING".equals(milestone.getStatus())) throw new BusinessException("该里程碑已确认收入");

    String code = codeGenerator.generate("REVENUE_RECOGNITION");
    RevenueRecognition recognition = new RevenueRecognition();
    recognition.setContractId(milestone.getContractId());
    recognition.setMilestoneId(milestone.getId());
    recognition.setCode(code);
    recognition.setAmount(milestone.getAmount());
    recognition.setRecognizeDate(request.recognizeDate());
    recognition.setRecognizedBy(currentName());
    recognition.setRemark(milestone.getName());
    recognitionRepository.save(recognition);

    ledgerService.post("REVENUE_RECOGNITION", code, request.recognizeDate(),
        "收入确认 " + milestone.getName(),
        List.of(
            new PostingLine("1122", "应收账款", milestone.getAmount(), BigDecimal.ZERO, "收入确认 " + milestone.getName()),
            new PostingLine("6001", "主营业务收入", BigDecimal.ZERO, milestone.getAmount(), "收入确认 " + milestone.getName())));

    milestone.setStatus("RECOGNIZED");
    milestone.setRecognizedDate(request.recognizeDate());
    milestone.setRecognizedBy(currentName());
    return toMilestoneResponse(milestoneRepository.save(milestone));
  }

  @Transactional(readOnly = true)
  public Page<RecognitionResponse> listRecognitions(Pageable pageable) {
    return recognitionRepository.findAllByOrderByCreatedAtDesc(pageable).map(r ->
        new RecognitionResponse(r.getId(), r.getCode(), r.getContractId(), r.getMilestoneId(),
            amount(r.getAmount()), r.getRecognizeDate(), r.getRecognizedBy(), r.getRemark()));
  }

  private MilestoneResponse toMilestoneResponse(ContractMilestone m) {
    return new MilestoneResponse(m.getId(), m.getContractId(), m.getName(), amount(m.getAmount()),
        m.getPlannedDate(), m.getStatus(), m.getRecognizedDate(), m.getRecognizedBy(), m.getRemark());
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }
}