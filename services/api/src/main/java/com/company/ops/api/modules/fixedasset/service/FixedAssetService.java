package com.company.ops.api.modules.fixedasset.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;
import static com.company.ops.api.modules.fixedasset.dto.FixedAssetDtos.*;
import static com.company.ops.api.modules.ledger.dto.LedgerDtos.PostingLine;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.fixedasset.domain.FixedAsset;
import com.company.ops.api.modules.fixedasset.domain.FixedAssetCount;
import com.company.ops.api.modules.fixedasset.domain.FixedAssetCountLine;
import com.company.ops.api.modules.fixedasset.domain.FixedAssetDepreciationLine;
import com.company.ops.api.modules.fixedasset.domain.FixedAssetDepreciationRun;
import com.company.ops.api.modules.fixedasset.domain.FixedAssetDisposal;
import com.company.ops.api.modules.fixedasset.domain.FixedAssetTransfer;
import com.company.ops.api.modules.fixedasset.repository.FixedAssetCountLineRepository;
import com.company.ops.api.modules.fixedasset.repository.FixedAssetCountRepository;
import com.company.ops.api.modules.fixedasset.repository.FixedAssetDepreciationLineRepository;
import com.company.ops.api.modules.fixedasset.repository.FixedAssetDepreciationRunRepository;
import com.company.ops.api.modules.fixedasset.repository.FixedAssetDisposalRepository;
import com.company.ops.api.modules.fixedasset.repository.FixedAssetRepository;
import com.company.ops.api.modules.fixedasset.repository.FixedAssetTransferRepository;
import com.company.ops.api.modules.ledger.domain.AccountingAccount;
import com.company.ops.api.modules.ledger.repository.AccountingAccountRepository;
import com.company.ops.api.modules.ledger.service.LedgerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FixedAssetService {

  private static final List<String> DEPRECIABLE_STATUSES = List.of("IN_USE", "IDLE");
  private static final List<String> COUNTABLE_STATUSES = List.of("IN_USE", "IDLE");

  private final FixedAssetRepository assetRepository;
  private final FixedAssetDepreciationRunRepository runRepository;
  private final FixedAssetDepreciationLineRepository lineRepository;
  private final FixedAssetTransferRepository transferRepository;
  private final FixedAssetDisposalRepository disposalRepository;
  private final FixedAssetCountRepository countRepository;
  private final FixedAssetCountLineRepository countLineRepository;
  private final CodeGenerator codeGenerator;
  private final LedgerService ledgerService;
  private final AccountingAccountRepository accountRepository;

  public FixedAssetService(FixedAssetRepository assetRepository,
      FixedAssetDepreciationRunRepository runRepository,
      FixedAssetDepreciationLineRepository lineRepository,
      FixedAssetTransferRepository transferRepository,
      FixedAssetDisposalRepository disposalRepository,
      FixedAssetCountRepository countRepository,
      FixedAssetCountLineRepository countLineRepository,
      CodeGenerator codeGenerator,
      LedgerService ledgerService,
      AccountingAccountRepository accountRepository) {
    this.assetRepository = assetRepository;
    this.runRepository = runRepository;
    this.lineRepository = lineRepository;
    this.transferRepository = transferRepository;
    this.disposalRepository = disposalRepository;
    this.countRepository = countRepository;
    this.countLineRepository = countLineRepository;
    this.codeGenerator = codeGenerator;
    this.ledgerService = ledgerService;
    this.accountRepository = accountRepository;
  }

  @Transactional
  public FixedAssetResponse createAsset(CreateAssetRequest request) {
    BigDecimal originalValue = amount(request.originalValue());
    BigDecimal residualValue = amount(request.residualValue());
    if (originalValue.signum() <= 0) throw new BusinessException("固定资产原值必须大于 0");
    if (residualValue.compareTo(originalValue) > 0) throw new BusinessException("残值不能大于原值");

    FixedAsset asset = new FixedAsset();
    asset.setCode(codeGenerator.generate("FIXED_ASSET"));
    asset.setName(request.name().trim());
    asset.setCategory(trim(request.category()));
    asset.setAcquisitionDate(request.acquisitionDate());
    asset.setOriginalValue(originalValue);
    asset.setResidualValue(residualValue);
    asset.setUsefulLifeMonths(request.usefulLifeMonths());
    asset.setDepreciationMethod("STRAIGHT_LINE");
    asset.setMonthlyDepreciation(monthlyDepreciation(originalValue, residualValue, request.usefulLifeMonths()));
    asset.setAccumulatedDepreciation(BigDecimal.ZERO);
    asset.setLastDepreciatedPeriod(null);
    asset.setStatus("IN_USE");
    asset.setLocation(trim(request.location()));
    asset.setCustodian(trim(request.custodian()));
    asset.setCustodianUserId(request.custodianUserId());
    asset.setOrganizationId(request.organizationId());
    asset.setRemark(trim(request.remark()));
    return toAssetResponse(assetRepository.save(asset));
  }

  @Transactional(readOnly = true)
  public Page<FixedAssetResponse> listAssets(Pageable pageable) {
    return assetRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toAssetResponse);
  }

  @Transactional(readOnly = true)
  public FixedAssetResponse getAsset(UUID id) {
    return toAssetResponse(requireAsset(id));
  }

  @Transactional
  public DepreciationRunResponse runDepreciation(RunDepreciationRequest request) {
    String period = parsePeriod(request.period());
    if (runRepository.findByPeriod(period).isPresent()) {
      throw new BusinessException("期间 " + period + " 已计提折旧");
    }
    LocalDate runDate = YearMonth.parse(period).atEndOfMonth();
    List<FixedAsset> assets = assetRepository.findDepreciableAssets(DEPRECIABLE_STATUSES, period);
    if (assets.isEmpty()) throw new BusinessException("期间 " + period + " 没有需要计提折旧的资产");

    BigDecimal total = BigDecimal.ZERO;
    List<FixedAssetDepreciationLine> lines = new ArrayList<>();
    for (FixedAsset asset : assets) {
      BigDecimal depreciable = asset.getOriginalValue().subtract(asset.getResidualValue());
      BigDecimal remaining = depreciable.subtract(amount(asset.getAccumulatedDepreciation()));
      if (remaining.signum() <= 0) continue;
      BigDecimal lineAmount = asset.getMonthlyDepreciation().min(remaining).setScale(2, RoundingMode.HALF_UP);
      asset.setAccumulatedDepreciation(amount(asset.getAccumulatedDepreciation()).add(lineAmount));
      asset.setLastDepreciatedPeriod(period);
      assetRepository.save(asset);
      FixedAssetDepreciationLine line = new FixedAssetDepreciationLine();
      line.setAssetId(asset.getId());
      line.setPeriod(period);
      line.setAmount(lineAmount);
      lines.add(line);
      total = total.add(lineAmount);
    }

    if (lines.isEmpty()) throw new BusinessException("期间 " + period + " 没有需要计提折旧的资产");

    FixedAssetDepreciationRun run = new FixedAssetDepreciationRun();
    run.setCode(codeGenerator.generate("FIXED_ASSET_DEPRECIATION"));
    run.setPeriod(period);
    run.setRunDate(runDate);
    run.setTotalAmount(total);
    run.setStatus("POSTED");
    FixedAssetDepreciationRun saved = runRepository.save(run);

    List<FixedAssetDepreciationLine> savedLines = new ArrayList<>();
    for (FixedAssetDepreciationLine line : lines) {
      line.setRunId(saved.getId());
      savedLines.add(lineRepository.save(line));
    }

    String description = "计提固定资产折旧 " + period;
    ledgerService.post("FIXED_ASSET_DEPRECIATION", saved.getCode(), runDate, description, List.of(
        new PostingLine("6602", accountName("6602", "管理费用"), total, BigDecimal.ZERO, description),
        new PostingLine("1602", accountName("1602", "累计折旧"), BigDecimal.ZERO, total, description)));

    return toRunResponse(saved, savedLines);
  }

  @Transactional(readOnly = true)
  public Page<DepreciationRunResponse> listDepreciationRuns(Pageable pageable) {
    return runRepository.findAllByOrderByPeriodDesc(pageable).map(run -> toRunResponse(run, List.of()));
  }

  @Transactional(readOnly = true)
  public DepreciationRunResponse getDepreciationRun(UUID id) {
    FixedAssetDepreciationRun run = runRepository.findById(id)
        .orElseThrow(() -> new BusinessException("折旧计提批次不存在"));
    return toRunResponse(run, lineRepository.findByRunIdOrderByCreatedAtAsc(id));
  }

  @Transactional
  public TransferResponse transfer(CreateTransferRequest request) {
    FixedAsset asset = assetRepository.findByIdForUpdate(request.assetId())
        .orElseThrow(() -> new BusinessException("固定资产不存在"));
    if ("DISPOSED".equals(asset.getStatus()) || "SCRAPPED".equals(asset.getStatus())) {
      throw new BusinessException("已处置或已报废的资产不能调拨");
    }
    FixedAssetTransfer transfer = new FixedAssetTransfer();
    transfer.setCode(codeGenerator.generate("FIXED_ASSET_TRANSFER"));
    transfer.setAssetId(asset.getId());
    transfer.setFromLocation(asset.getLocation());
    transfer.setToLocation(trim(request.toLocation()));
    transfer.setFromCustodian(asset.getCustodian());
    transfer.setToCustodian(trim(request.toCustodian()));
    transfer.setTransferDate(request.transferDate());
    transfer.setRemark(trim(request.remark()));

    asset.setLocation(trim(request.toLocation()));
    asset.setCustodian(trim(request.toCustodian()));
    assetRepository.save(asset);

    return toTransferResponse(transferRepository.save(transfer));
  }

  @Transactional(readOnly = true)
  public Page<TransferResponse> listTransfers(Pageable pageable) {
    return transferRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toTransferResponse);
  }

  @Transactional
  public DisposalResponse dispose(CreateDisposalRequest request) {
    FixedAsset asset = assetRepository.findByIdForUpdate(request.assetId())
        .orElseThrow(() -> new BusinessException("固定资产不存在"));
    if ("DISPOSED".equals(asset.getStatus()) || "SCRAPPED".equals(asset.getStatus())) {
      throw new BusinessException("资产已处置或已报废");
    }
    BigDecimal originalValue = asset.getOriginalValue();
    BigDecimal accumulated = amount(asset.getAccumulatedDepreciation());
    BigDecimal proceeds = amount(request.proceeds());
    BigDecimal netBookValue = originalValue.subtract(accumulated);
    BigDecimal gainLoss = proceeds.subtract(netBookValue);

    FixedAssetDisposal disposal = new FixedAssetDisposal();
    disposal.setCode(codeGenerator.generate("FIXED_ASSET_DISPOSAL"));
    disposal.setAssetId(asset.getId());
    disposal.setDisposalDate(request.disposalDate());
    disposal.setMethod(trim(request.method()));
    disposal.setProceeds(proceeds);
    disposal.setNetBookValue(netBookValue);
    disposal.setGainLoss(gainLoss);
    disposal.setRemark(trim(request.remark()));
    FixedAssetDisposal saved = disposalRepository.save(disposal);

    asset.setStatus("DISPOSED");
    assetRepository.save(asset);

    String description = "固定资产处置 " + asset.getCode() + " " + asset.getName();
    List<PostingLine> postings = new ArrayList<>();
    postings.add(new PostingLine("1601", accountName("1601", "固定资产"),
        BigDecimal.ZERO, originalValue, description));
    if (accumulated.signum() > 0) {
      postings.add(new PostingLine("1602", accountName("1602", "累计折旧"),
          accumulated, BigDecimal.ZERO, description));
    }
    if (proceeds.signum() > 0) {
      postings.add(new PostingLine("1002", accountName("1002", "银行存款"),
          proceeds, BigDecimal.ZERO, description));
    }
    if (gainLoss.signum() > 0) {
      postings.add(new PostingLine("6711", accountName("6711", "资产处置损益"),
          BigDecimal.ZERO, gainLoss, description));
    } else if (gainLoss.signum() < 0) {
      postings.add(new PostingLine("6711", accountName("6711", "资产处置损益"),
          gainLoss.negate(), BigDecimal.ZERO, description));
    }
    ledgerService.post("FIXED_ASSET_DISPOSAL", saved.getCode(), request.disposalDate(), description, postings);

    return toDisposalResponse(saved);
  }

  @Transactional(readOnly = true)
  public Page<DisposalResponse> listDisposals(Pageable pageable) {
    return disposalRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDisposalResponse);
  }

  @Transactional
  public CountResponse createCount(CreateCountRequest request) {
    FixedAssetCount count = new FixedAssetCount();
    count.setCode(codeGenerator.generate("FIXED_ASSET_COUNT"));
    count.setCountDate(request.countDate());
    count.setStatus("DRAFT");
    count.setRemark(trim(request.remark()));
    FixedAssetCount saved = countRepository.save(count);

    List<FixedAssetCountLine> lines = assetRepository
        .findByStatusInOrderByCreatedAtAsc(COUNTABLE_STATUSES).stream()
        .map(asset -> {
          FixedAssetCountLine line = new FixedAssetCountLine();
          line.setCountId(saved.getId());
          line.setAssetId(asset.getId());
          line.setBookQuantity(1);
          return line;
        }).toList();
    countLineRepository.saveAll(lines);

    return toCountResponse(saved, lines);
  }

  @Transactional(readOnly = true)
  public Page<CountResponse> listCounts(Pageable pageable) {
    return countRepository.findAllByOrderByCreatedAtDesc(pageable)
        .map(count -> toCountResponse(count, List.of()));
  }

  @Transactional(readOnly = true)
  public CountResponse getCount(UUID id) {
    FixedAssetCount count = countRepository.findById(id)
        .orElseThrow(() -> new BusinessException("盘点单不存在"));
    return toCountResponse(count, countLineRepository.findByCountIdOrderByCreatedAtAsc(id));
  }

  @Transactional
  public CountLineResponse recordCountLine(UUID countId, UUID lineId, RecordCountLineRequest request) {
    FixedAssetCount count = countRepository.findById(countId)
        .orElseThrow(() -> new BusinessException("盘点单不存在"));
    if (!"DRAFT".equals(count.getStatus())) throw new BusinessException("盘点单已过账，不能修改");
    FixedAssetCountLine line = countLineRepository.findByIdAndCountId(lineId, countId)
        .orElseThrow(() -> new BusinessException("盘点明细不存在"));
    line.setActualQuantity(request.actualQuantity());
    line.setDifference(request.actualQuantity() - line.getBookQuantity());
    line.setRemark(trim(request.remark()));
    Map<UUID, FixedAsset> assetMap = assetMap(List.of(line.getAssetId()));
    return toCountLineResponse(countLineRepository.save(line), assetMap);
  }

  @Transactional
  public CountResponse postCount(UUID id) {
    FixedAssetCount count = countRepository.findById(id)
        .orElseThrow(() -> new BusinessException("盘点单不存在"));
    if (!"DRAFT".equals(count.getStatus())) throw new BusinessException("盘点单已过账");
    count.setStatus("POSTED");
    List<FixedAssetCountLine> lines = countLineRepository.findByCountIdOrderByCreatedAtAsc(id);
    return toCountResponse(countRepository.save(count), lines);
  }

  private BigDecimal monthlyDepreciation(BigDecimal originalValue, BigDecimal residualValue, int months) {
    return originalValue.subtract(residualValue)
        .divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
  }

  private String parsePeriod(String value) {
    try {
      return YearMonth.parse(value.trim()).toString();
    } catch (DateTimeParseException ex) {
      throw new BusinessException("折旧期间格式应为 yyyy-MM");
    }
  }

  private FixedAsset requireAsset(UUID id) {
    return assetRepository.findById(id).orElseThrow(() -> new BusinessException("固定资产不存在"));
  }

  private String accountName(String code, String fallback) {
    return accountRepository.findByCode(code).map(AccountingAccount::getName).orElse(fallback);
  }

  private Map<UUID, FixedAsset> assetMap(List<UUID> ids) {
    if (ids.isEmpty()) return Map.of();
    return assetRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(FixedAsset::getId, Function.identity()));
  }

  private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

  private FixedAssetResponse toAssetResponse(FixedAsset asset) {
    return new FixedAssetResponse(asset.getId(), asset.getCode(), asset.getName(), asset.getCategory(),
        asset.getAcquisitionDate(), asset.getOriginalValue(), asset.getResidualValue(),
        asset.getUsefulLifeMonths(), asset.getDepreciationMethod(), asset.getMonthlyDepreciation(),
        asset.getAccumulatedDepreciation(), asset.getLastDepreciatedPeriod(), asset.getStatus(),
        asset.getLocation(), asset.getCustodian(), asset.getCustodianUserId(), asset.getOrganizationId(),
        asset.getRemark(), asset.getCreatedAt(), asset.getUpdatedAt(), asset.getCreatedBy(),
        asset.getUpdatedBy(), asset.getVersion());
  }

  private DepreciationRunResponse toRunResponse(FixedAssetDepreciationRun run,
      List<FixedAssetDepreciationLine> lines) {
    List<DepreciationLineResponse> lineResponses = lines.stream()
        .map(line -> new DepreciationLineResponse(line.getId(), line.getRunId(), line.getAssetId(),
            line.getPeriod(), line.getAmount()))
        .toList();
    return new DepreciationRunResponse(run.getId(), run.getCode(), run.getPeriod(), run.getRunDate(),
        run.getTotalAmount(), run.getStatus(), run.getCreatedAt(), lineResponses);
  }

  private TransferResponse toTransferResponse(FixedAssetTransfer transfer) {
    return new TransferResponse(transfer.getId(), transfer.getCode(), transfer.getAssetId(),
        transfer.getFromLocation(), transfer.getToLocation(), transfer.getFromCustodian(),
        transfer.getToCustodian(), transfer.getTransferDate(), transfer.getRemark(), transfer.getCreatedAt());
  }

  private DisposalResponse toDisposalResponse(FixedAssetDisposal disposal) {
    return new DisposalResponse(disposal.getId(), disposal.getCode(), disposal.getAssetId(),
        disposal.getDisposalDate(), disposal.getMethod(), disposal.getProceeds(), disposal.getNetBookValue(),
        disposal.getGainLoss(), disposal.getRemark(), disposal.getCreatedAt());
  }

  private CountResponse toCountResponse(FixedAssetCount count, List<FixedAssetCountLine> lines) {
    Map<UUID, FixedAsset> assetMap = assetMap(lines.stream().map(FixedAssetCountLine::getAssetId).toList());
    List<CountLineResponse> lineResponses = lines.stream()
        .map(line -> toCountLineResponse(line, assetMap))
        .toList();
    return new CountResponse(count.getId(), count.getCode(), count.getCountDate(), count.getStatus(),
        count.getRemark(), count.getCreatedAt(), lineResponses);
  }

  private CountLineResponse toCountLineResponse(FixedAssetCountLine line, Map<UUID, FixedAsset> assetMap) {
    FixedAsset asset = assetMap.get(line.getAssetId());
    return new CountLineResponse(line.getId(), line.getCountId(), line.getAssetId(),
        asset == null ? null : asset.getCode(), asset == null ? null : asset.getName(),
        line.getBookQuantity(), line.getActualQuantity(), line.getDifference(), line.getRemark());
  }
}
