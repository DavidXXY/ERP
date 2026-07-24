package com.company.ops.api.modules.procurement.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.inventory.domain.InventoryPart;
import com.company.ops.api.modules.inventory.domain.StockMovement;
import com.company.ops.api.modules.inventory.domain.StockMovementType;
import com.company.ops.api.modules.inventory.repository.InventoryPartRepository;
import com.company.ops.api.modules.inventory.repository.StockMovementRepository;
import com.company.ops.api.modules.procurement.domain.ApprovalStatus;
import com.company.ops.api.modules.procurement.domain.GoodsReceipt;
import com.company.ops.api.modules.procurement.domain.PayableStatus;
import com.company.ops.api.modules.procurement.domain.ProcurementCostAllocation;
import com.company.ops.api.modules.procurement.domain.ProcurementCostType;
import com.company.ops.api.modules.procurement.domain.ProcurementPayable;
import com.company.ops.api.modules.procurement.domain.PurchaseOrder;
import com.company.ops.api.modules.procurement.domain.PurchaseOrderStatus;
import com.company.ops.api.modules.procurement.domain.PurchaseRequest;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestApprovalRecord;
import com.company.ops.api.modules.procurement.domain.PurchaseRequestStatus;
import com.company.ops.api.modules.procurement.domain.Supplier;
import com.company.ops.api.modules.procurement.domain.SupplierInvoice;
import com.company.ops.api.modules.procurement.domain.SupplierQuotation;
import com.company.ops.api.modules.procurement.domain.SupplierQuotationLine;
import com.company.ops.api.modules.procurement.domain.ProcurementInquiry;
import com.company.ops.api.modules.procurement.domain.ProcurementContract;
import com.company.ops.api.modules.procurement.domain.SupplierRiskStatus;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.CreatePurchaseRequestRequest;
import com.company.ops.api.modules.procurement.dto.CreateSupplierRequest;
import com.company.ops.api.modules.procurement.dto.GoodsReceiptResponse;
import com.company.ops.api.modules.procurement.dto.ImportPurchaseRequestBatchResponse;
import com.company.ops.api.modules.procurement.dto.ProcessPurchaseRequestApprovalRequest;
import com.company.ops.api.modules.procurement.dto.ProcurementCostAllocationResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementCostTargetOptionResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementCostTargetOptionsResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementMatchingResponse;
import com.company.ops.api.modules.procurement.dto.ProcurementPayableResponse;
import com.company.ops.api.modules.procurement.dto.PurchaseOrderResponse;
import com.company.ops.api.modules.procurement.dto.PurchaseRequestResponse;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderRequest;
import com.company.ops.api.modules.procurement.dto.ReceivePurchaseOrderResult;
import com.company.ops.api.modules.procurement.dto.ReviewSupplierAdmissionRequest;
import com.company.ops.api.modules.procurement.dto.SupplierResponse;
import com.company.ops.api.modules.procurement.repository.GoodsReceiptRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementPayableRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementCostAllocationRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseOrderRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestApprovalRecordRepository;
import com.company.ops.api.modules.procurement.repository.PurchaseRequestRepository;
import com.company.ops.api.modules.procurement.repository.SupplierRepository;
import com.company.ops.api.modules.procurement.repository.SupplierInvoiceRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationRepository;
import com.company.ops.api.modules.procurement.repository.SupplierQuotationLineRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementInquiryRequestRepository;
import com.company.ops.api.modules.procurement.repository.ProcurementContractRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectApprovalStatus;
import com.company.ops.api.modules.project.domain.ProjectStage;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import com.company.ops.api.modules.system.security.UserPrincipal;
import static com.company.ops.api.common.util.MoneyUtils.amount;

@Service
public class ProcurementService {

  private final CodeGenerator codeGenerator;
  private final SupplierRepository supplierRepository;
  private final PurchaseRequestRepository requestRepository;
  private final PurchaseRequestApprovalRecordRepository requestApprovalRepository;
  private final PurchaseOrderRepository orderRepository;
  private final GoodsReceiptRepository receiptRepository;
  private final ProcurementPayableRepository payableRepository;
  private final ProcurementCostAllocationRepository costAllocationRepository;
  private final InventoryPartRepository partRepository;
  private final StockMovementRepository movementRepository;
  private final ProjectRepository projectRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final SupplierInvoiceRepository invoiceRepository;
  private final ProcurementInquiryRepository inquiryRepository;
  private final ProcurementInquiryRequestRepository inquiryRequestRepository;
  private final SupplierQuotationRepository quoteRepository;
  private final SupplierQuotationLineRepository quoteLineRepository;
  private final ProcurementContractRepository contractRepository;
  private final ProcurementArrivalService arrivals;

  public ProcurementService(
      CodeGenerator codeGenerator,
      SupplierRepository supplierRepository,
      PurchaseRequestRepository requestRepository,
      PurchaseRequestApprovalRecordRepository requestApprovalRepository,
      PurchaseOrderRepository orderRepository,
      GoodsReceiptRepository receiptRepository,
      ProcurementPayableRepository payableRepository,
      ProcurementCostAllocationRepository costAllocationRepository,
      InventoryPartRepository partRepository,
      StockMovementRepository movementRepository,
      ProjectRepository projectRepository,
      SystemOrganizationRepository organizationRepository,
      SupplierInvoiceRepository invoiceRepository,
      ProcurementInquiryRepository inquiryRepository,
      ProcurementInquiryRequestRepository inquiryRequestRepository,
      SupplierQuotationRepository quoteRepository,
      SupplierQuotationLineRepository quoteLineRepository,
      ProcurementContractRepository contractRepository,
      ProcurementArrivalService arrivals
  ) {
    this.codeGenerator = codeGenerator;
    this.supplierRepository = supplierRepository;
    this.requestRepository = requestRepository;
    this.requestApprovalRepository = requestApprovalRepository;
    this.orderRepository = orderRepository;
    this.receiptRepository = receiptRepository;
    this.payableRepository = payableRepository;
    this.costAllocationRepository = costAllocationRepository;
    this.partRepository = partRepository;
    this.movementRepository = movementRepository;
    this.projectRepository = projectRepository;
    this.organizationRepository = organizationRepository;
    this.invoiceRepository = invoiceRepository;
    this.inquiryRepository = inquiryRepository;
    this.inquiryRequestRepository = inquiryRequestRepository;
    this.quoteRepository = quoteRepository;
    this.quoteLineRepository = quoteLineRepository;
    this.contractRepository = contractRepository;
    this.arrivals = arrivals;
  }

  @Transactional(readOnly = true)
  public List<SupplierResponse> listSuppliers() {
    Map<UUID, SupplierFinancialSummary> summaries = supplierFinancialSummaries();
    return supplierRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(supplier -> toSupplierResponse(supplier, summaries.get(supplier.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<SupplierResponse> listSuppliers(Pageable pageable) {
    Map<UUID, SupplierFinancialSummary> summaries = supplierFinancialSummaries();
    return supplierRepository.findAll(pageable)
        .map(supplier -> toSupplierResponse(supplier, summaries.get(supplier.getId())));
  }

  @Transactional
  public SupplierResponse createSupplier(CreateSupplierRequest request) {
    String supplierCode = request.code() != null && !request.code().isBlank()
        ? request.code().trim()
        : codeGenerator.generate("SUPPLIER");
    if (supplierRepository.existsByCode(supplierCode)) {
      throw new BusinessException("供应商编码已存在");
    }
    Supplier supplier = new Supplier();
    supplier.setCode(supplierCode);
    applySupplierRequest(supplier, request);
    supplier.setAdmissionStatus("PENDING");
    supplier.setAdmissionSubmittedAt(OffsetDateTime.now());
    supplier.setAdmissionReviewedAt(null);
    supplier.setAdmissionReviewerName(null);
    supplier.setAdmissionReviewComment(null);
    return toSupplierResponse(supplierRepository.save(supplier), null);
  }

  @Transactional
  public SupplierResponse updateSupplier(UUID id, CreateSupplierRequest request) {
    Supplier supplier = supplierRepository.findById(id)
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    boolean resubmitAdmission = "REJECTED".equals(supplier.getAdmissionStatus());
    applySupplierRequest(supplier, request);
    if (resubmitAdmission) {
      supplier.setAdmissionStatus("PENDING");
      supplier.setAdmissionSubmittedAt(OffsetDateTime.now());
      supplier.setAdmissionReviewedAt(null);
      supplier.setAdmissionReviewerName(null);
      supplier.setAdmissionReviewComment(null);
    }
    Supplier saved = supplierRepository.save(supplier);
    return toSupplierResponse(saved, supplierFinancialSummaries().get(saved.getId()));
  }

  @Transactional
  public SupplierResponse reviewSupplierAdmission(
      UUID id, ReviewSupplierAdmissionRequest request) {
    Supplier supplier = supplierRepository.findById(id)
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    if (!"PENDING".equals(supplier.getAdmissionStatus())) {
      throw new BusinessException("仅待审批供应商可以执行准入审批");
    }
    String decision = request.decision().trim().toUpperCase();
    if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
      throw new BusinessException("准入审批结果仅支持通过或驳回");
    }
    if ("REJECTED".equals(decision) && !StringUtils.hasText(request.comment())) {
      throw new BusinessException("驳回供应商准入时必须填写原因");
    }
    if ("APPROVED".equals(decision)) {
      List<String> missing = missingAdmissionFields(supplier);
      if (!missing.isEmpty()) {
        throw new BusinessException("供应商资料不完整，请先补充：" + String.join("、", missing));
      }
      if (supplier.getLicenseValidTo().isBefore(java.time.LocalDate.now())) {
        throw new BusinessException("营业执照已过期，不能通过准入审批");
      }
      if (supplier.getQualificationValidTo() != null
          && supplier.getQualificationValidTo().isBefore(java.time.LocalDate.now())) {
        throw new BusinessException("供应商资质已过期，不能通过准入审批");
      }
    }
    supplier.setAdmissionStatus(decision);
    supplier.setAdmissionReviewedAt(OffsetDateTime.now());
    supplier.setAdmissionReviewerName(currentName());
    supplier.setAdmissionReviewComment(request.comment());
    Supplier saved = supplierRepository.save(supplier);
    return toSupplierResponse(saved, supplierFinancialSummaries().get(saved.getId()));
  }

  private void applySupplierRequest(Supplier supplier, CreateSupplierRequest request) {
    supplier.setName(request.name());
    supplier.setCategory(request.category());
    supplier.setContactName(request.contactName());
    supplier.setPhone(request.phone());
    supplier.setSettlementTerms(request.settlementTerms());
    supplier.setLegalRepresentative(request.legalRepresentative());
    supplier.setUnifiedSocialCreditCode(request.unifiedSocialCreditCode());
    supplier.setRegisteredCapital(request.registeredCapital());
    supplier.setRegisteredAddress(request.registeredAddress());
    supplier.setBusinessScope(request.businessScope());
    supplier.setLicenseValidTo(request.licenseValidTo());
    supplier.setQualificationValidTo(request.qualificationValidTo());
    supplier.setTaxpayerType(request.taxpayerType());
    supplier.setBankName(request.bankName());
    supplier.setBankAccount(request.bankAccount());
    supplier.setRemark(request.remark());
    supplier.setRiskStatus(request.riskStatus() == null ? SupplierRiskStatus.NORMAL : request.riskStatus());
  }

  private List<String> missingAdmissionFields(Supplier supplier) {
    java.util.ArrayList<String> missing = new java.util.ArrayList<>();
    if (!StringUtils.hasText(supplier.getCategory())) missing.add("供应商类别");
    if (!StringUtils.hasText(supplier.getContactName())) missing.add("联系人");
    if (!StringUtils.hasText(supplier.getPhone())) missing.add("联系电话");
    if (!StringUtils.hasText(supplier.getUnifiedSocialCreditCode())) missing.add("统一社会信用代码");
    if (!StringUtils.hasText(supplier.getLegalRepresentative())) missing.add("法定代表人");
    if (!StringUtils.hasText(supplier.getRegisteredAddress())) missing.add("注册地址");
    if (supplier.getLicenseValidTo() == null) missing.add("营业执照有效期");
    if (!StringUtils.hasText(supplier.getTaxpayerType())) missing.add("纳税人类型");
    if (!StringUtils.hasText(supplier.getBankName())) missing.add("开户银行");
    if (!StringUtils.hasText(supplier.getBankAccount())) missing.add("银行账号");
    if (!StringUtils.hasText(supplier.getSettlementTerms())) missing.add("结算条款");
    return missing;
  }

  @Transactional(readOnly = true)
  public ProcurementCostTargetOptionsResponse listCostTargets() {
    List<ProcurementCostTargetOptionResponse> projects = projectRepository.findAllByOrderByCreatedAtDesc().stream()
        .filter(project -> project.getApprovalStatus() == ProjectApprovalStatus.APPROVED)
        .filter(project -> project.getStage() != ProjectStage.CLOSED)
        .map(project -> new ProcurementCostTargetOptionResponse(
            project.getId(), project.getCode(), project.getName()
        ))
        .toList();
    List<ProcurementCostTargetOptionResponse> departments = organizationRepository
        .findByTenantIdOrderBySortOrderAsc(TenantContext.currentTenant()).stream()
        .filter(SystemOrganization::isEnabled)
        .filter(organization -> "DEPARTMENT".equals(organization.getType()))
        .map(organization -> new ProcurementCostTargetOptionResponse(
            organization.getId(), organization.getCode(), organization.getName()
        ))
        .toList();
    return new ProcurementCostTargetOptionsResponse(projects, departments);
  }

  @Transactional(readOnly = true)
  public List<ProcurementCostAllocationResponse> listCostAllocations() {
    List<ProcurementCostAllocation> allocations = costAllocationRepository
        .findAllByOrderByIncurredDateDescCreatedAtDesc();
    Map<UUID, PurchaseOrder> orders = orderRepository.findAllById(
        allocations.stream().map(ProcurementCostAllocation::getOrderId).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));
    Map<UUID, GoodsReceipt> receipts = receiptRepository.findAllById(
        allocations.stream().map(ProcurementCostAllocation::getReceiptId).distinct().toList()
    ).stream().collect(Collectors.toMap(GoodsReceipt::getId, Function.identity()));
    return allocations.stream()
        .map(item -> toCostAllocationResponse(
            item,
            orders.get(item.getOrderId()),
            receipts.get(item.getReceiptId())
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PurchaseRequestResponse> listPurchaseRequests() {
    return requestRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toPurchaseRequestResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<PurchaseRequestResponse> listPurchaseRequests(
      PurchaseRequestStatus status, ApprovalStatus approvalStatus,
      ProcurementCostType costType, String search, Pageable pageable) {
    return requestRepository.findByFilters(status, approvalStatus, costType, search, pageable)
        .map(this::toPurchaseRequestResponse);
  }

  @Transactional
  public PurchaseRequestResponse createPurchaseRequest(CreatePurchaseRequestRequest request) {
    String prCode = request.code() != null && !request.code().isBlank()
        ? request.code().trim()
        : codeGenerator.generate("PURCHASE_REQUEST");
    if (requestRepository.existsByCode(prCode)) {
      throw new BusinessException("采购申请编码已存在");
    }

    UUID partId = request.partId();
    String partName = request.partName();
    if (request.partId() != null) {
      InventoryPart part = partRepository.findById(request.partId())
          .orElseThrow(() -> new BusinessException("物料不存在"));
      partName = part.getName();
    } else if (StringUtils.hasText(partName)) {
      List<InventoryPart> exactMatches = partRepository.findByNameIgnoreCase(partName.trim());
      if (exactMatches.size() == 1) {
        InventoryPart part = exactMatches.get(0);
        partId = part.getId();
        partName = part.getName();
      }
    }
    if (!StringUtils.hasText(partName)) {
      throw new BusinessException("请选择物料或填写采购物料名称");
    }
    CostTarget costTarget = resolveCostTarget(
        request.costType(), request.projectId(), request.departmentId()
    );
    BigDecimal requestAmount = request.quantity().multiply(amount(request.unitPrice()));
    validateProjectBudget(request.costType(), request.projectId(), requestAmount, null);

    PurchaseRequest purchaseRequest = new PurchaseRequest();
    UUID batchId = UUID.randomUUID();
    purchaseRequest.setBatchId(batchId);
    purchaseRequest.setBatchCode(prCode);
    purchaseRequest.setBatchName(partName);
    purchaseRequest.setLineNo(1);
    purchaseRequest.setCode(prCode);
    purchaseRequest.setRequesterName(currentName());
    purchaseRequest.setPartId(partId);
    purchaseRequest.setPartName(partName);
    purchaseRequest.setQuantity(request.quantity());
    purchaseRequest.setUnitPrice(amount(request.unitPrice()));
    purchaseRequest.setTaxRate(defaultTaxRate(request.taxRate()));
    purchaseRequest.setTotalAmount(requestAmount);
    purchaseRequest.setExpectedDate(request.expectedDate());
    purchaseRequest.setReason(request.reason());
    purchaseRequest.setCostType(request.costType());
    purchaseRequest.setProjectId(request.projectId());
    purchaseRequest.setDepartmentId(request.departmentId());
    purchaseRequest.setCostTargetCode(costTarget.code());
    purchaseRequest.setCostTargetName(costTarget.name());
    purchaseRequest.setStatus(PurchaseRequestStatus.SUBMITTED);
    purchaseRequest.setApprovalStatus(ApprovalStatus.PENDING);
    return toPurchaseRequestResponse(requestRepository.save(purchaseRequest));
  }

  @Transactional
  public ImportPurchaseRequestBatchResponse importPurchaseRequestBatch(
      MultipartFile file,
      String batchName,
      ProcurementCostType costType,
      UUID projectId,
      UUID departmentId,
      String sharedReason
  ) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("请选择要导入的 Excel 或 CSV 文件");
    }
    if (!StringUtils.hasText(batchName)) {
      throw new BusinessException("请填写申请批次名称");
    }
    if (batchName.trim().length() > 180) {
      throw new BusinessException("申请批次名称不能超过180个字符");
    }
    String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    String lowerName = fileName.toLowerCase();
    if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")
        && !lowerName.endsWith(".csv")) {
      throw new BusinessException("仅支持 .xlsx、.xls 或 .csv 文件");
    }
    if (file.getSize() > 10L * 1024 * 1024) {
      throw new BusinessException("导入文件不能超过10MB");
    }

    CostTarget costTarget = resolveCostTarget(costType, projectId, departmentId);
    List<ImportedPurchaseLine> lines = lowerName.endsWith(".csv")
        ? readCsvPurchaseLines(file)
        : readExcelPurchaseLines(file);
    if (lines.isEmpty()) {
      throw new BusinessException("导入文件没有可用的采购明细");
    }
    if (lines.size() > 500) {
      throw new BusinessException("一次最多导入500条采购明细");
    }

    BigDecimal batchAmount = lines.stream()
        .map(line -> line.quantity().multiply(line.unitPrice()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    validateProjectBudget(costType, projectId, batchAmount, null);

    UUID batchId = UUID.randomUUID();
    String batchCode = codeGenerator.generate("PURCHASE_REQUEST_BATCH");
    List<PurchaseRequest> entities = new ArrayList<>();
    for (int index = 0; index < lines.size(); index++) {
      ImportedPurchaseLine line = lines.get(index);
      PurchaseRequest item = new PurchaseRequest();
      item.setBatchId(batchId);
      item.setBatchCode(batchCode);
      item.setBatchName(batchName.trim());
      item.setLineNo(index + 1);
      item.setCode(codeGenerator.generate("PURCHASE_REQUEST"));
      item.setRequesterName(currentName());
      item.setPartId(line.part() == null ? null : line.part().getId());
      item.setPartName(line.partName());
      item.setQuantity(line.quantity());
      item.setUnitPrice(line.unitPrice());
      item.setTaxRate(line.taxRate());
      item.setTotalAmount(line.quantity().multiply(line.unitPrice()));
      item.setExpectedDate(line.expectedDate());
      item.setReason(combineImportReason(sharedReason, line.reason(), line.technicalRequirement()));
      item.setCostType(costType);
      item.setProjectId(projectId);
      item.setDepartmentId(departmentId);
      item.setCostTargetCode(costTarget.code());
      item.setCostTargetName(costTarget.name());
      item.setStatus(PurchaseRequestStatus.SUBMITTED);
      item.setApprovalStatus(ApprovalStatus.PENDING);
      item.setSourceType("IMPORT");
      item.setSourceReference(fileName.length() > 120 ? fileName.substring(fileName.length() - 120) : fileName);
      entities.add(item);
    }
    List<PurchaseRequest> saved = requestRepository.saveAll(entities);
    List<PurchaseRequestResponse> items = saved.stream()
        .map(this::toPurchaseRequestResponse)
        .toList();
    return new ImportPurchaseRequestBatchResponse(
        batchId, batchCode, batchName.trim(), items.size(), amount(batchAmount), items);
  }

  @Transactional
  public PurchaseRequestResponse processRequestApproval(
      UUID id,
      ProcessPurchaseRequestApprovalRequest request
  ) {
    PurchaseRequest purchaseRequest = requestRepository.findById(id)
        .orElseThrow(() -> new BusinessException("采购申请不存在"));
    if (purchaseRequest.getApprovalStatus() != ApprovalStatus.PENDING) {
      throw new BusinessException("该采购申请已处理");
    }
    if (request.decision() == ApprovalStatus.PENDING) {
      throw new BusinessException("请选择通过或驳回");
    }

    purchaseRequest.setApprovalStatus(request.decision());
    purchaseRequest.setStatus(request.decision() == ApprovalStatus.APPROVED
        ? PurchaseRequestStatus.APPROVED
        : PurchaseRequestStatus.SUBMITTED);
    requestRepository.save(purchaseRequest);

    PurchaseRequestApprovalRecord record = new PurchaseRequestApprovalRecord();
    record.setRequestId(purchaseRequest.getId());
    record.setDecision(request.decision());
    record.setComment(request.comment());
    record.setApproverName(currentName());
    record.setDecidedAt(OffsetDateTime.now());
    requestApprovalRepository.save(record);
    return toPurchaseRequestResponse(purchaseRequest);
  }

  @Transactional
  public List<PurchaseRequestResponse> processRequestBatchApproval(
      UUID batchId,
      ProcessPurchaseRequestApprovalRequest request
  ) {
    if (request.decision() == ApprovalStatus.PENDING) {
      throw new BusinessException("请选择通过或驳回");
    }
    List<PurchaseRequest> batch = requestRepository.findByBatchIdOrderByLineNoAsc(batchId);
    if (batch.isEmpty()) {
      throw new BusinessException("采购申请批次不存在");
    }
    List<PurchaseRequest> pending = batch.stream()
        .filter(item -> item.getApprovalStatus() == ApprovalStatus.PENDING)
        .toList();
    if (pending.isEmpty()) {
      throw new BusinessException("该批次没有待处理的采购明细");
    }
    String approverName = currentName();
    OffsetDateTime decidedAt = OffsetDateTime.now();
    for (PurchaseRequest item : pending) {
      item.setApprovalStatus(request.decision());
      item.setStatus(request.decision() == ApprovalStatus.APPROVED
          ? PurchaseRequestStatus.APPROVED
          : PurchaseRequestStatus.SUBMITTED);
      PurchaseRequestApprovalRecord record = new PurchaseRequestApprovalRecord();
      record.setRequestId(item.getId());
      record.setDecision(request.decision());
      record.setComment(request.comment());
      record.setApproverName(approverName);
      record.setDecidedAt(decidedAt);
      requestApprovalRepository.save(record);
    }
    requestRepository.saveAll(pending);
    return requestRepository.findByBatchIdOrderByLineNoAsc(batchId).stream()
        .map(this::toPurchaseRequestResponse)
        .toList();
  }

  @Transactional
  public PurchaseRequestResponse updatePurchaseRequest(
      UUID id, CreatePurchaseRequestRequest request) {
    PurchaseRequest purchaseRequest = requestRepository.findById(id)
        .orElseThrow(() -> new BusinessException("采购申请不存在"));
    if (purchaseRequest.getApprovalStatus() == ApprovalStatus.APPROVED
        || purchaseRequest.getStatus() == PurchaseRequestStatus.RECEIVED
        || purchaseRequest.getStatus() == PurchaseRequestStatus.CANCELLED) {
      throw new BusinessException("该申请当前状态不可编辑");
    }
    purchaseRequest.setRequesterName(currentName());
    UUID partId = request.partId();
    if (request.partId() != null) {
      InventoryPart part = partRepository.findById(request.partId())
          .orElseThrow(() -> new BusinessException("物料不存在"));
      purchaseRequest.setPartId(part.getId());
      purchaseRequest.setPartName(part.getName());
    } else {
      String partName = request.partName();
      if (StringUtils.hasText(partName)) {
        List<InventoryPart> exactMatches = partRepository.findByNameIgnoreCase(partName.trim());
        if (exactMatches.size() == 1) {
          InventoryPart part = exactMatches.get(0);
          partId = part.getId();
          partName = part.getName();
        }
      }
      purchaseRequest.setPartId(partId);
      purchaseRequest.setPartName(partName);
    }
    purchaseRequest.setQuantity(request.quantity());
    purchaseRequest.setUnitPrice(amount(request.unitPrice()));
    purchaseRequest.setTaxRate(defaultTaxRate(request.taxRate()));
    purchaseRequest.setTotalAmount(request.quantity().multiply(amount(request.unitPrice())));
    purchaseRequest.setExpectedDate(request.expectedDate());
    purchaseRequest.setReason(request.reason());
    purchaseRequest.setCostType(request.costType());
    purchaseRequest.setProjectId(request.projectId());
    purchaseRequest.setDepartmentId(request.departmentId());
    CostTarget costTarget = resolveCostTarget(
        request.costType(), request.projectId(), request.departmentId());
    validateProjectBudget(
        request.costType(), request.projectId(), purchaseRequest.getTotalAmount(), purchaseRequest.getId());
    purchaseRequest.setCostTargetCode(costTarget.code());
    purchaseRequest.setCostTargetName(costTarget.name());
    // If was rejected, reset to PENDING for re-approval
    if (purchaseRequest.getApprovalStatus() == ApprovalStatus.REJECTED) {
      purchaseRequest.setApprovalStatus(ApprovalStatus.PENDING);
    }
    return toPurchaseRequestResponse(requestRepository.save(purchaseRequest));
  }

  @Transactional(readOnly = true)
  public List<PurchaseOrderResponse> listPurchaseOrders() {
    List<PurchaseOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc();
    return mapOrders(orders);
  }

  @Transactional(readOnly = true)
  public Page<PurchaseOrderResponse> listPurchaseOrders(
      PurchaseOrderStatus status, ProcurementCostType costType,
      String search, Pageable pageable) {
    Page<PurchaseOrder> page = orderRepository.findByFilters(status, costType, search, pageable);
    // Batch-load suppliers
    Map<UUID, Supplier> supplierMap = supplierRepository.findAllById(
        page.getContent().stream().map(PurchaseOrder::getSupplierId).distinct().toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    Map<UUID, PurchaseRequest> requestMap = requestRepository.findAllById(
        page.getContent().stream().map(PurchaseOrder::getRequestId).filter(java.util.Objects::nonNull).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, Function.identity()));
    return page.map(order -> toPurchaseOrderResponse(order, supplierMap.get(order.getSupplierId()), requestMap.get(order.getRequestId())));
  }

  @Transactional
  public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {
    String orderCode = StringUtils.hasText(request.code()) ? request.code().trim() : null;
    if (orderCode != null && orderRepository.existsByCode(orderCode)) {
      throw new BusinessException("采购订单编码已存在");
    }
    Supplier supplier = supplierRepository.findById(request.supplierId())
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    if (supplier.getRiskStatus() == SupplierRiskStatus.BLOCKED) {
      throw new BusinessException("该供应商已停用，不能下单");
    }
    if (!"APPROVED".equals(supplier.getAdmissionStatus())) {
      throw new BusinessException("供应商尚未完成准入审批，不能下单");
    }

    PurchaseRequest purchaseRequest = requestRepository.findById(request.requestId())
        .orElseThrow(() -> new BusinessException("采购申请不存在"));
    if (purchaseRequest.getApprovalStatus() != ApprovalStatus.APPROVED
        || purchaseRequest.getStatus() != PurchaseRequestStatus.APPROVED) {
      throw new BusinessException("采购申请审批通过后才能下单");
    }
    if (purchaseRequest.getPartId() == null && StringUtils.hasText(purchaseRequest.getPartName())) {
      List<InventoryPart> exactMatches = partRepository.findByNameIgnoreCase(
          purchaseRequest.getPartName().trim());
      if (exactMatches.size() == 1) {
        InventoryPart part = exactMatches.get(0);
        purchaseRequest.setPartId(part.getId());
        purchaseRequest.setPartName(part.getName());
        requestRepository.save(purchaseRequest);
      }
    }
    if (purchaseRequest.getPartId() == null) {
      throw new BusinessException("采购申请未关联唯一物料，请先建立物料档案或消除同名物料");
    }
    partRepository.findById(purchaseRequest.getPartId())
        .orElseThrow(() -> new BusinessException("关联物料不存在"));
    BigDecimal orderedQty = request.orderedQty() == null
        ? purchaseRequest.getQuantity() : request.orderedQty();
    BigDecimal alreadyOrdered = orderRepository.findByRequestId(request.requestId()).stream()
        .filter(existing -> existing.getStatus() != PurchaseOrderStatus.CANCELLED)
        .map(PurchaseOrder::getOrderedQty).map(this::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (alreadyOrdered.add(orderedQty).compareTo(purchaseRequest.getQuantity()) > 0) {
      throw new BusinessException("拆分订单数量超过采购申请剩余数量");
    }

    UUID inquiryId = request.inquiryId();
    if (inquiryId == null) {
      List<ProcurementInquiry> awardedInquiries = inquiryRepository.findAll().stream()
          .filter(inquiry -> "AWARDED".equals(inquiry.getStatus()))
          .filter(inquiry -> inquiry.getRequestId().equals(purchaseRequest.getId())
              || inquiryRequestRepository.existsByInquiryIdAndRequestId(
                  inquiry.getId(), purchaseRequest.getId()))
          .toList();
      if (awardedInquiries.size() == 1) {
        inquiryId = awardedInquiries.get(0).getId();
      } else if (awardedInquiries.size() > 1) {
        throw new BusinessException("该采购申请存在多个已定标询价，请明确选择询价单");
      }
    }

    SupplierQuotation selectedQuote = null;
    SupplierQuotationLine selectedQuoteLine = null;
    if (inquiryId != null) {
      ProcurementInquiry inquiry = inquiryRepository.findById(inquiryId)
          .orElseThrow(() -> new BusinessException("询价单不存在"));
      boolean inquiryContainsRequest = inquiry.getRequestId().equals(purchaseRequest.getId())
          || inquiryRequestRepository.existsByInquiryIdAndRequestId(
              inquiry.getId(), purchaseRequest.getId());
      if (!inquiryContainsRequest
          || !"AWARDED".equals(inquiry.getStatus()) || inquiry.getSelectedQuoteId() == null) {
        throw new BusinessException("询价单尚未完成定标或不属于该采购申请");
      }
      selectedQuote = quoteRepository.findById(inquiry.getSelectedQuoteId())
          .orElseThrow(() -> new BusinessException("定标报价不存在"));
      selectedQuoteLine = quoteLineRepository
          .findByQuoteIdAndRequestId(selectedQuote.getId(), purchaseRequest.getId())
          .orElse(null);
      if (!selectedQuote.getSupplierId().equals(request.supplierId())) {
        throw new BusinessException("订单供应商必须与定标供应商一致");
      }
      BigDecimal awardedUnitPrice = selectedQuoteLine == null
          ? selectedQuote.getUnitPrice() : selectedQuoteLine.getUnitPrice();
      if (awardedUnitPrice.compareTo(request.unitPrice()) != 0) {
        throw new BusinessException("订单价格必须与定标报价一致");
      }
      if (selectedQuoteLine != null && request.taxRate() != null
          && selectedQuoteLine.getTaxRate().compareTo(request.taxRate()) != 0) {
        throw new BusinessException("订单税率必须与该物料的定标报价一致");
      }
    } else if (!StringUtils.hasText(request.sourceReason())) {
      throw new BusinessException("未关联询价单时必须填写直接采购原因");
    }
    if (request.contractId() != null) {
      ProcurementContract contract = contractRepository.findById(request.contractId())
          .orElseThrow(() -> new BusinessException("采购合同不存在"));
      if (!contract.getSupplierId().equals(request.supplierId())
          || !"APPROVED".equals(contract.getApprovalStatus())
          || !"ACTIVE".equals(contract.getStatus())) {
        throw new BusinessException("采购合同未审批生效或供应商不一致");
      }
    }

    if (orderCode == null) {
      orderCode = codeGenerator.generate("PURCHASE_ORDER");
    }
    PurchaseOrder order = new PurchaseOrder();
    order.setCode(orderCode);
    order.setSupplierId(request.supplierId());
    order.setRequestId(request.requestId());
    order.setPartId(purchaseRequest.getPartId());
    order.setPartName(purchaseRequest.getPartName());
    order.setOrderedQty(orderedQty);
    order.setReceivedQty(BigDecimal.ZERO);
    order.setUnitPrice(request.unitPrice());
    BigDecimal awardedTaxRate = selectedQuoteLine == null
        ? purchaseRequest.getTaxRate() : selectedQuoteLine.getTaxRate();
    order.setTaxRate(defaultTaxRate(request.taxRate() == null ? awardedTaxRate : request.taxRate()));
    BigDecimal freight = request.freightAmount() == null
        ? allocatedQuoteFreight(selectedQuote, selectedQuoteLine, orderedQty)
        : request.freightAmount();
    order.setOrderAmount(orderedQty.multiply(request.unitPrice()).add(freight));
    order.setExpectedDeliveryDate(request.expectedDeliveryDate() == null
        ? selectedQuoteLine != null && selectedQuoteLine.getDeliveryDate() != null
            ? selectedQuoteLine.getDeliveryDate() : purchaseRequest.getExpectedDate()
        : request.expectedDeliveryDate());
    order.setCostType(purchaseRequest.getCostType());
    order.setProjectId(purchaseRequest.getProjectId());
    order.setDepartmentId(purchaseRequest.getDepartmentId());
    order.setCostTargetCode(purchaseRequest.getCostTargetCode());
    order.setCostTargetName(purchaseRequest.getCostTargetName());
    order.setStatus(PurchaseOrderStatus.DRAFT);
    order.setApprovalStatus(ApprovalStatus.PENDING);
    order.setInquiryId(inquiryId);
    order.setContractId(request.contractId());
    order.setCurrency(StringUtils.hasText(request.currency()) ? request.currency()
        : selectedQuote == null ? "CNY" : selectedQuote.getCurrency());
    order.setFreightAmount(freight);
    order.setSourceReason(request.sourceReason());

    PurchaseOrder saved = orderRepository.save(order);
    return toPurchaseOrderResponse(saved, supplier, purchaseRequest);
  }

  @Transactional
  public PurchaseOrderResponse cancelPurchaseOrder(UUID id) {
    PurchaseOrder order = orderRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.ORDERED) {
      throw new BusinessException("只有已下单的订单才能取消");
    }
    order.setStatus(PurchaseOrderStatus.CANCELLED);
    PurchaseOrder saved = orderRepository.save(order);
    // Revert the associated purchase request back to APPROVED
    if (order.getRequestId() != null) {
      PurchaseRequest pr = requestRepository.findById(order.getRequestId()).orElse(null);
      if (pr != null && pr.getStatus() == PurchaseRequestStatus.ORDERED) {
        pr.setStatus(PurchaseRequestStatus.APPROVED);
        requestRepository.save(pr);
      }
    }
    Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
    PurchaseRequest purchaseRequest = order.getRequestId() != null
        ? requestRepository.findById(order.getRequestId()).orElse(null) : null;
    return toPurchaseOrderResponse(saved, supplier, purchaseRequest);
  }

  @Transactional
  public PurchaseOrderResponse closePurchaseOrder(UUID id) {
    PurchaseOrder order = orderRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.PARTIAL_RECEIVED
        && order.getStatus() != PurchaseOrderStatus.RECEIVED) {
      throw new BusinessException("只有部分收货或已收货订单可以关闭");
    }
    boolean pendingInspection = receiptRepository.findByOrderId(id).stream()
        .anyMatch(receipt -> "PENDING".equals(receipt.getInspectionStatus()));
    if (pendingInspection) {
      throw new BusinessException("仍有待质检到货记录，不能关闭订单");
    }
    order.setStatus(PurchaseOrderStatus.CLOSED);
    order.setClosedAt(OffsetDateTime.now());
    Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
    PurchaseRequest purchaseRequest = requestRepository.findById(order.getRequestId()).orElse(null);
    if (purchaseRequest != null) {
      purchaseRequest.setStatus(PurchaseRequestStatus.RECEIVED);
      requestRepository.save(purchaseRequest);
    }
    return toPurchaseOrderResponse(orderRepository.save(order), supplier, purchaseRequest);
  }

  @Transactional
  public PurchaseOrderResponse submitPurchaseOrder(UUID id) {
    PurchaseOrder order = orderRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.DRAFT) throw new BusinessException("只有草稿订单可以提交审批");
    if (order.getSubmittedAt() != null && order.getApprovalStatus() == ApprovalStatus.PENDING) {
      throw new BusinessException("采购订单已提交审批，请勿重复提交");
    }
    order.setApprovalStatus(ApprovalStatus.PENDING);
    order.setSubmittedAt(OffsetDateTime.now());
    Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
    PurchaseRequest purchaseRequest = requestRepository.findById(order.getRequestId()).orElse(null);
    return toPurchaseOrderResponse(orderRepository.save(order), supplier, purchaseRequest);
  }

  @Transactional
  public PurchaseOrderResponse approvePurchaseOrder(UUID id, ApprovalStatus decision, String approverName, String comment) {
    PurchaseOrder order = orderRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    if (order.getStatus() != PurchaseOrderStatus.DRAFT || order.getApprovalStatus() != ApprovalStatus.PENDING)
      throw new BusinessException("该订单当前不可审批");
    if (order.getSubmittedAt() == null) throw new BusinessException("采购订单尚未提交审批");
    if (decision == ApprovalStatus.PENDING) throw new BusinessException("请选择通过或驳回");
    order.setApprovalStatus(decision); order.setApproverName(currentName()); order.setApprovalComment(comment); order.setApprovedAt(OffsetDateTime.now());
    if (decision == ApprovalStatus.APPROVED) {
      order.setStatus(PurchaseOrderStatus.ORDERED);
      requestRepository.findById(order.getRequestId()).ifPresent(pr -> {
        BigDecimal approvedQty = orderRepository.findByRequestId(pr.getId()).stream()
            .filter(existing -> existing.getApprovalStatus() == ApprovalStatus.APPROVED
                && existing.getStatus() != PurchaseOrderStatus.CANCELLED)
            .map(PurchaseOrder::getOrderedQty).map(this::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        pr.setStatus(approvedQty.compareTo(pr.getQuantity()) >= 0
            ? PurchaseRequestStatus.ORDERED : PurchaseRequestStatus.APPROVED);
        requestRepository.save(pr);
      });
    }
    Supplier supplier = supplierRepository.findById(order.getSupplierId()).orElse(null);
    PurchaseRequest purchaseRequest = requestRepository.findById(order.getRequestId()).orElse(null);
    return toPurchaseOrderResponse(orderRepository.save(order), supplier, purchaseRequest);
  }

  @Transactional
  public ReceivePurchaseOrderResult receiveOrder(UUID id, ReceivePurchaseOrderRequest request) {
    GoodsReceipt savedReceipt = arrivals.register(id, request);
    PurchaseOrder order = orderRepository.findById(id)
        .orElseThrow(() -> new BusinessException("采购订单不存在"));
    InventoryPart part = partRepository.findById(order.getPartId())
        .orElseThrow(() -> new BusinessException("关联物料不存在"));
    PurchaseRequest purchaseRequest = requestRepository.findById(order.getRequestId()).orElse(null);
    Supplier supplier = supplierRepository.findById(order.getSupplierId())
        .orElseThrow(() -> new BusinessException("供应商不存在"));
    return new ReceivePurchaseOrderResult(
        toPurchaseOrderResponse(order, supplier, purchaseRequest),
        toGoodsReceiptResponse(savedReceipt, order, part),
        null,
        null,
        amount(part.getStockQty())
    );
  }

  @Transactional(readOnly = true)
  public List<GoodsReceiptResponse> listGoodsReceipts() {
    List<GoodsReceipt> receipts = receiptRepository.findAllByOrderByReceivedDateDesc();
    Map<UUID, PurchaseOrder> orders = orderRepository.findAllById(
        receipts.stream().map(GoodsReceipt::getOrderId).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));
    Map<UUID, InventoryPart> parts = partRepository.findAllById(
        receipts.stream().map(GoodsReceipt::getPartId).distinct().toList()
    ).stream().collect(Collectors.toMap(InventoryPart::getId, Function.identity()));
    return receipts.stream()
        .map(item -> toGoodsReceiptResponse(item, orders.get(item.getOrderId()), parts.get(item.getPartId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProcurementPayableResponse> listPayables() {
    List<ProcurementPayable> payables = payableRepository.findAllByOrderByDueDateAsc();
    Map<UUID, Supplier> suppliers = supplierRepository.findAllById(
        payables.stream().map(ProcurementPayable::getSupplierId).distinct().toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    Map<UUID, PurchaseOrder> orders = orderRepository.findAllById(
        payables.stream().map(ProcurementPayable::getOrderId).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));
    return payables.stream()
        .map(item -> toPayableResponse(item, suppliers.get(item.getSupplierId()), orders.get(item.getOrderId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProcurementMatchingResponse> matching() {
    List<PurchaseOrder> orders = orderRepository.findAllByOrderByCreatedAtDesc();
    Map<UUID, Supplier> suppliers = supplierRepository.findAllById(
        orders.stream().map(PurchaseOrder::getSupplierId).distinct().toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    Map<UUID, List<GoodsReceipt>> receiptsByOrder = receiptRepository.findAll().stream()
        .collect(Collectors.groupingBy(GoodsReceipt::getOrderId));
    Map<UUID, List<ProcurementPayable>> payablesByOrder = payableRepository.findAll().stream()
        .collect(Collectors.groupingBy(ProcurementPayable::getOrderId));
    Map<UUID, List<SupplierInvoice>> invoicesByOrder = invoiceRepository.findAll().stream()
        .collect(Collectors.groupingBy(SupplierInvoice::getOrderId));
    return orders.stream()
        .map(order -> toMatchingResponse(
            order,
            suppliers.get(order.getSupplierId()),
            receiptsByOrder.getOrDefault(order.getId(), List.of()),
            payablesByOrder.getOrDefault(order.getId(), List.of()),
            invoicesByOrder.getOrDefault(order.getId(), List.of())
        ))
        .toList();
  }

  private List<PurchaseOrderResponse> mapOrders(List<PurchaseOrder> orders) {
    Map<UUID, Supplier> suppliers = supplierRepository.findAllById(
        orders.stream().map(PurchaseOrder::getSupplierId).distinct().toList()
    ).stream().collect(Collectors.toMap(Supplier::getId, Function.identity()));
    Map<UUID, PurchaseRequest> requests = requestRepository.findAllById(
        orders.stream().map(PurchaseOrder::getRequestId).filter(id -> id != null).distinct().toList()
    ).stream().collect(Collectors.toMap(PurchaseRequest::getId, Function.identity()));
    return orders.stream()
        .map(order -> toPurchaseOrderResponse(
            order,
            suppliers.get(order.getSupplierId()),
            order.getRequestId() == null ? null : requests.get(order.getRequestId())
        ))
        .toList();
  }

  private SupplierResponse toSupplierResponse(Supplier supplier, SupplierFinancialSummary summary) {
    SupplierFinancialSummary financials = summary == null ? SupplierFinancialSummary.ZERO : summary;
    return new SupplierResponse(
        supplier.getId(),
        supplier.getCode(),
        supplier.getName(),
        supplier.getCategory(),
        supplier.getContactName(),
        supplier.getPhone(),
        supplier.getSettlementTerms(),
        supplier.getLegalRepresentative(),
        supplier.getUnifiedSocialCreditCode(),
        supplier.getRegisteredCapital(),
        supplier.getRegisteredAddress(),
        supplier.getBusinessScope(),
        supplier.getLicenseValidTo(),
        supplier.getQualificationValidTo(),
        supplier.getTaxpayerType(),
        supplier.getBankName(),
        supplier.getBankAccount(),
        supplier.getAdmissionStatus(),
        supplier.getAdmissionSubmittedAt(),
        supplier.getAdmissionReviewedAt(),
        supplier.getAdmissionReviewerName(),
        supplier.getAdmissionReviewComment(),
        supplier.getRemark(),
        supplier.getRiskStatus(),
        financials.contractedAmount(),
        financials.payableAmount(),
        financials.paidAmount(),
        financials.outstandingAmount()
    );
  }

  private Map<UUID, SupplierFinancialSummary> supplierFinancialSummaries() {
    Map<UUID, BigDecimal> contractedAmounts = orderRepository.findAll().stream()
        .filter(order -> order.getStatus() != PurchaseOrderStatus.CANCELLED)
        .collect(Collectors.groupingBy(
            PurchaseOrder::getSupplierId,
            Collectors.reducing(BigDecimal.ZERO, order -> amount(order.getOrderAmount()), BigDecimal::add)
        ));
    Map<UUID, List<ProcurementPayable>> payablesBySupplier = payableRepository.findAll().stream()
        .filter(payable -> payable.getStatus() != PayableStatus.CANCELLED)
        .collect(Collectors.groupingBy(ProcurementPayable::getSupplierId));
    return supplierRepository.findAll().stream().collect(Collectors.toMap(
        Supplier::getId,
        supplier -> {
          List<ProcurementPayable> payables = payablesBySupplier.getOrDefault(supplier.getId(), List.of());
          BigDecimal payableAmount = payables.stream()
              .map(ProcurementPayable::getAmount).map(this::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal paidAmount = payables.stream()
              .map(ProcurementPayable::getPaidAmount).map(this::amount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          return new SupplierFinancialSummary(
              contractedAmounts.getOrDefault(supplier.getId(), BigDecimal.ZERO),
              payableAmount,
              paidAmount,
              payableAmount.subtract(paidAmount)
          );
        }
    ));
  }

  private record SupplierFinancialSummary(
      BigDecimal contractedAmount,
      BigDecimal payableAmount,
      BigDecimal paidAmount,
      BigDecimal outstandingAmount
  ) {
    private static final SupplierFinancialSummary ZERO = new SupplierFinancialSummary(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
    );
  }

  private ProcurementMatchingResponse toMatchingResponse(
      PurchaseOrder order,
      Supplier supplier,
      List<GoodsReceipt> receipts,
      List<ProcurementPayable> payables,
      List<SupplierInvoice> invoices
  ) {
    BigDecimal receiptAmount = receipts.stream().map(GoodsReceipt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal payableAmount = payables.stream().map(ProcurementPayable::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal paidAmount = payables.stream().map(ProcurementPayable::getPaidAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal invoiceAmount = invoices.stream()
        .filter(item -> !"REJECTED".equals(item.getApprovalStatus()))
        .map(SupplierInvoice::getAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal matchedInvoiceAmount = invoices.stream()
        .filter(item -> "APPROVED".equals(item.getApprovalStatus()))
        .map(SupplierInvoice::getMatchedAmount).map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal orderedQty = amount(order.getOrderedQty());
    BigDecimal receivedQty = amount(order.getReceivedQty());
    BigDecimal orderAmount = amount(order.getOrderAmount());
    String status;
    String risk;
    if (order.getStatus() == PurchaseOrderStatus.CANCELLED) {
      status = "CANCELLED"; risk = "订单已取消";
    } else if (receivedQty.compareTo(orderedQty) < 0) {
      status = "RECEIVING"; risk = "尚未收齐";
    } else if (payableAmount.compareTo(receiptAmount) < 0) {
      status = "PAYABLE_MISSING"; risk = "应付金额少于入库金额";
    } else if (payableAmount.compareTo(receiptAmount) > 0) {
      status = "AMOUNT_MISMATCH"; risk = "应付金额超过入库金额";
    } else if (invoiceAmount.compareTo(payableAmount) < 0) {
      status = "INVOICE_PENDING"; risk = "供应商发票尚未收齐";
    } else if (invoiceAmount.compareTo(payableAmount) > 0) {
      status = "INVOICE_MISMATCH"; risk = "供应商发票金额超过应付金额";
    } else if (matchedInvoiceAmount.compareTo(payableAmount) < 0) {
      status = "INVOICE_REVIEW"; risk = "发票尚未完成匹配审核";
    } else if (receiptAmount.compareTo(orderAmount) != 0) {
      status = "AMOUNT_MISMATCH"; risk = "入库金额与订单金额不一致";
    } else {
      status = "MATCHED"; risk = "三单一致";
    }
    return new ProcurementMatchingResponse(
        order.getId(), order.getCode(), supplier == null ? null : supplier.getName(), order.getPartName(),
        orderedQty, receivedQty, orderAmount, receiptAmount, payableAmount,
        invoiceAmount, matchedInvoiceAmount, paidAmount, status, risk
    );
  }

  private PurchaseRequestResponse toPurchaseRequestResponse(PurchaseRequest request) {
    PurchaseRequestApprovalRecord approval = requestApprovalRepository
        .findFirstByRequestIdOrderByDecidedAtDesc(request.getId())
        .orElse(null);
    return new PurchaseRequestResponse(
        request.getId(),
        request.getBatchId(),
        request.getBatchCode(),
        request.getBatchName(),
        request.getLineNo(),
        request.getCode(),
        request.getRequesterName(),
        request.getPartId(),
        request.getPartName(),
        request.getQuantity(),
        amount(request.getUnitPrice()),
        defaultTaxRate(request.getTaxRate()),
        amount(request.getTotalAmount()),
        request.getExpectedDate(),
        request.getReason(),
        request.getCostType(),
        costTargetId(request.getCostType(), request.getProjectId(), request.getDepartmentId()),
        request.getCostTargetCode(),
        request.getCostTargetName(),
        request.getStatus(),
        request.getApprovalStatus(),
        approval == null ? null : approval.getComment(),
        approval == null ? null : approval.getApproverName(),
        approval == null ? null : approval.getDecidedAt()
    );
  }

  private PurchaseOrderResponse toPurchaseOrderResponse(
      PurchaseOrder order,
      Supplier supplier,
      PurchaseRequest request
  ) {
    return new PurchaseOrderResponse(
        order.getId(),
        order.getCode(),
        order.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        order.getRequestId(),
        request == null ? null : request.getCode(),
        order.getPartId(),
        order.getPartName(),
        amount(order.getOrderedQty()),
        amount(order.getReceivedQty()),
        amount(order.getUnitPrice()),
        defaultTaxRate(order.getTaxRate()),
        amount(order.getOrderAmount()),
        order.getExpectedDeliveryDate(),
        order.getCostType(),
        costTargetId(order.getCostType(), order.getProjectId(), order.getDepartmentId()),
        order.getCostTargetCode(),
        order.getCostTargetName(),
        order.getStatus(), order.getApprovalStatus(), order.getApprovalComment(), order.getApproverName(),
        order.getApprovedAt(), order.getInquiryId(), order.getContractId(), order.getCurrency(),
        amount(order.getFreightAmount()), order.getSourceReason(), order.getSubmittedAt(),
        order.getClosedAt(), order.getOrderVersion()
    );
  }

  private GoodsReceiptResponse toGoodsReceiptResponse(
      GoodsReceipt receipt,
      PurchaseOrder order,
      InventoryPart part
  ) {
    return new GoodsReceiptResponse(
        receipt.getId(),
        receipt.getCode(),
        receipt.getOrderId(),
        order == null ? null : order.getCode(),
        receipt.getPartId(),
        part == null ? null : part.getName(),
        receipt.getQuantity(),
        receipt.getUnitPrice(),
        defaultTaxRate(receipt.getTaxRate()),
        receipt.getAmount(),
        receipt.getReceivedDate(),
        receipt.getPayableDueDate(),
        receipt.getDeliveryNo(),
        receipt.getReceiverName(),
        order == null ? null : order.getCostType(),
        order == null ? null : costTargetId(
            order.getCostType(), order.getProjectId(), order.getDepartmentId()
        ),
        order == null ? null : order.getCostTargetCode(),
        order == null ? null : order.getCostTargetName(),
        receipt.getInspectionStatus(), receipt.getQualifiedQty(), receipt.getRejectedQty(),
        receipt.getInspectorName(), receipt.getInspectionComment(), receipt.getInspectedAt(),
        receipt.getClientRequestId(), receipt.getAsnNo()
    );
  }

  private ProcurementPayableResponse toPayableResponse(
      ProcurementPayable payable,
      Supplier supplier,
      PurchaseOrder order
  ) {
    BigDecimal outstanding = amount(payable.getAmount()).subtract(amount(payable.getPaidAmount()));
    return new ProcurementPayableResponse(
        payable.getId(),
        payable.getCode(),
        payable.getSupplierId(),
        supplier == null ? null : supplier.getName(),
        payable.getOrderId(),
        order == null ? null : order.getCode(),
        payable.getReceiptId(),
        amount(payable.getAmount()),
        defaultTaxRate(payable.getTaxRate()),
        amount(payable.getPaidAmount()),
        outstanding,
        payable.getDueDate(),
        order == null ? null : order.getCostType(),
        order == null ? null : costTargetId(
            order.getCostType(), order.getProjectId(), order.getDepartmentId()
        ),
        order == null ? null : order.getCostTargetCode(),
        order == null ? null : order.getCostTargetName(),
        payable.getStatus()
    );
  }

  private ProcurementCostAllocationResponse toCostAllocationResponse(
      ProcurementCostAllocation allocation,
      PurchaseOrder order,
      GoodsReceipt receipt
  ) {
    return new ProcurementCostAllocationResponse(
        allocation.getId(),
        allocation.getOrderId(),
        order == null ? null : order.getCode(),
        allocation.getReceiptId(),
        receipt == null ? null : receipt.getCode(),
        allocation.getCostType(),
        costTargetId(
            allocation.getCostType(), allocation.getProjectId(), allocation.getDepartmentId()
        ),
        allocation.getTargetCode(),
        allocation.getTargetName(),
        allocation.getPartName(),
        amount(allocation.getAmount()),
        allocation.getIncurredDate()
    );
  }

  private CostTarget resolveCostTarget(
      ProcurementCostType costType,
      UUID projectId,
      UUID departmentId
  ) {
    if (costType == null) {
      throw new BusinessException("请选择采购成本归属类型");
    }
    if (costType == ProcurementCostType.PROJECT) {
      if (projectId == null) {
        throw new BusinessException("项目采购必须关联项目");
      }
      if (departmentId != null) {
        throw new BusinessException("项目采购不能同时关联部门");
      }
      Project project = projectRepository.findById(projectId)
          .orElseThrow(() -> new BusinessException("关联项目不存在"));
      if (project.getApprovalStatus() != ProjectApprovalStatus.APPROVED
          || project.getStage() == ProjectStage.CLOSED) {
        throw new BusinessException("只能选择已审批且未关闭的项目");
      }
      return new CostTarget(project.getCode(), project.getName());
    }
    if (departmentId == null) {
      throw new BusinessException("部门采购必须关联成本部门");
    }
    if (projectId != null) {
      throw new BusinessException("部门采购不能同时关联项目");
    }
    SystemOrganization department = organizationRepository.findById(departmentId)
        .orElseThrow(() -> new BusinessException("成本部门不存在"));
    if (!department.isEnabled() || !"DEPARTMENT".equals(department.getType())) {
      throw new BusinessException("请选择有效的部门作为成本归属");
    }
    return new CostTarget(department.getCode(), department.getName());
  }

  private void validateProjectBudget(
      ProcurementCostType costType, UUID projectId, BigDecimal requestedAmount, UUID excludedRequestId
  ) {
    if (costType != ProcurementCostType.PROJECT || projectId == null) return;
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException("关联项目不存在"));
    BigDecimal occupied = requestRepository.findAll().stream()
        .filter(item -> item.getProjectId() != null && item.getProjectId().equals(projectId))
        .filter(item -> excludedRequestId == null || !item.getId().equals(excludedRequestId))
        .filter(item -> item.getStatus() != PurchaseRequestStatus.CANCELLED)
        .filter(item -> item.getApprovalStatus() != ApprovalStatus.REJECTED)
        .map(item -> amount(item.getTotalAmount()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal available = amount(project.getBudgetAmount()).subtract(occupied);
    if (requestedAmount.compareTo(available) > 0) {
      throw new BusinessException(
          "项目预算不足：剩余可申请 " + available + "，本次申请 " + requestedAmount + "，请调整金额或发起超预算审批");
    }
  }

  private void validateOrderCostTarget(PurchaseOrder order) {
    if (order.getCostType() == ProcurementCostType.PROJECT) {
      if (order.getProjectId() == null) {
        throw new BusinessException("采购订单缺少关联项目，不能收货");
      }
      Project project = projectRepository.findById(order.getProjectId())
          .orElseThrow(() -> new BusinessException("采购订单关联项目不存在"));
      if (project.getApprovalStatus() != ProjectApprovalStatus.APPROVED
          || project.getStage() == ProjectStage.CLOSED) {
        throw new BusinessException("关联项目已关闭或未审批，不能归集采购成本");
      }
      return;
    }
    if (order.getCostType() != ProcurementCostType.DEPARTMENT || order.getDepartmentId() == null) {
      throw new BusinessException("采购订单缺少成本部门，不能收货");
    }
  }

  private UUID costTargetId(
      ProcurementCostType costType,
      UUID projectId,
      UUID departmentId
  ) {
    return costType == ProcurementCostType.PROJECT ? projectId : departmentId;
  }

  private BigDecimal allocatedQuoteFreight(
      SupplierQuotation quote,
      SupplierQuotationLine line,
      BigDecimal orderedQty
  ) {
    if (quote == null) return BigDecimal.ZERO;
    BigDecimal freight = amount(quote.getFreightAmount());
    if (line == null || freight.compareTo(BigDecimal.ZERO) == 0) return freight;
    BigDecimal totalMaterialAmount = quoteLineRepository
        .findByQuoteIdOrderByCreatedAtAsc(quote.getId()).stream()
        .map(item -> amount(item.getQuantity()).multiply(amount(item.getUnitPrice())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (totalMaterialAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
    BigDecimal orderMaterialAmount = amount(orderedQty).multiply(amount(line.getUnitPrice()));
    return freight.multiply(orderMaterialAmount)
        .divide(totalMaterialAmount, 2, RoundingMode.HALF_UP);
  }

  private BigDecimal amount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal defaultTaxRate(BigDecimal value) {
    return value == null ? BigDecimal.valueOf(13) : value;
  }

  private List<ImportedPurchaseLine> readExcelPurchaseLines(MultipartFile file) {
    List<String> errors = new ArrayList<>();
    List<ImportedPurchaseLine> lines = new ArrayList<>();
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();
      int headerRowIndex = findPurchaseImportHeader(sheet, formatter);
      if (headerRowIndex < 0) {
        throw new BusinessException("未找到导入表头，请使用系统提供的采购申请模板");
      }
      Map<String, Integer> columns = headerColumns(sheet.getRow(headerRowIndex), formatter);
      validateImportColumns(columns);
      for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) continue;
        Map<String, String> values = new java.util.HashMap<>();
        columns.forEach((name, index) ->
            values.put(name, formatter.formatCellValue(row.getCell(index)).trim()));
        if (values.values().stream().allMatch(value -> !StringUtils.hasText(value))) continue;
        try {
          lines.add(parseImportedLine(rowIndex + 1, values));
        } catch (BusinessException exception) {
          errors.add("第" + (rowIndex + 1) + "行：" + exception.getMessage());
        }
      }
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException("采购明细文件读取失败：" + exception.getMessage());
    }
    throwIfImportErrors(errors);
    return lines;
  }

  private List<ImportedPurchaseLine> readCsvPurchaseLines(MultipartFile file) {
    List<String> errors = new ArrayList<>();
    List<ImportedPurchaseLine> lines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      if (headerLine == null) return lines;
      List<String> headers = parseCsvRow(headerLine.replace("\uFEFF", ""));
      Map<String, Integer> columns = new java.util.HashMap<>();
      for (int index = 0; index < headers.size(); index++) {
        columns.put(normalizeImportHeader(headers.get(index)), index);
      }
      validateImportColumns(columns);
      String line;
      int rowNumber = 1;
      while ((line = reader.readLine()) != null) {
        rowNumber++;
        if (!StringUtils.hasText(line)) continue;
        List<String> cells = parseCsvRow(line);
        Map<String, String> values = new java.util.HashMap<>();
        columns.forEach((name, index) ->
            values.put(name, index < cells.size() ? cells.get(index).trim() : ""));
        if (values.values().stream().allMatch(value -> !StringUtils.hasText(value))) continue;
        try {
          lines.add(parseImportedLine(rowNumber, values));
        } catch (BusinessException exception) {
          errors.add("第" + rowNumber + "行：" + exception.getMessage());
        }
      }
    } catch (IOException exception) {
      throw new BusinessException("CSV 文件读取失败：" + exception.getMessage());
    }
    throwIfImportErrors(errors);
    return lines;
  }

  private int findPurchaseImportHeader(Sheet sheet, DataFormatter formatter) {
    int last = Math.min(sheet.getLastRowNum(), 15);
    for (int rowIndex = 0; rowIndex <= last; rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) continue;
      for (Cell cell : row) {
        if ("物料编码".equals(normalizeImportHeader(formatter.formatCellValue(cell)))) {
          return rowIndex;
        }
      }
    }
    return -1;
  }

  private Map<String, Integer> headerColumns(Row row, DataFormatter formatter) {
    Map<String, Integer> columns = new java.util.HashMap<>();
    for (Cell cell : row) {
      String name = normalizeImportHeader(formatter.formatCellValue(cell));
      if (StringUtils.hasText(name)) columns.put(name, cell.getColumnIndex());
    }
    return columns;
  }

  private void validateImportColumns(Map<String, Integer> columns) {
    if (!columns.containsKey("物料编码") || !columns.containsKey("物料名称")
        || !columns.containsKey("数量") || !columns.containsKey("预计单价")
        || !columns.containsKey("税率") || !columns.containsKey("期望到货日期")
        || !columns.containsKey("采购原因") || !columns.containsKey("规格技术要求")) {
      throw new BusinessException("导入列不完整，请下载并使用最新采购申请模板");
    }
  }

  private String normalizeImportHeader(String value) {
    if (value == null) return "";
    return value.trim()
        .replace("*", "")
        .replace("（", "(")
        .replace("）", ")")
        .replaceAll("\\s+", "")
        .replace("(%)", "")
        .replace("%", "")
        .replace("/", "");
  }

  private ImportedPurchaseLine parseImportedLine(int sourceRow, Map<String, String> values) {
    String partCode = values.getOrDefault("物料编码", "");
    String enteredName = values.getOrDefault("物料名称", "");
    InventoryPart part = null;
    if (StringUtils.hasText(partCode)) {
      part = partRepository.findByCodeIgnoreCase(partCode.trim())
          .orElseThrow(() -> new BusinessException("物料编码不存在：" + partCode));
    }
    String partName = part == null ? enteredName.trim() : part.getName();
    if (!StringUtils.hasText(partName)) {
      throw new BusinessException("物料编码和物料名称不能同时为空");
    }
    BigDecimal quantity = parseImportDecimal(values.get("数量"), "数量", true);
    String priceText = values.get("预计单价");
    BigDecimal unitPrice = StringUtils.hasText(priceText)
        ? parseImportDecimal(priceText, "预计单价", false)
        : part == null ? BigDecimal.ZERO : amount(part.getUnitCost());
    BigDecimal taxRate = StringUtils.hasText(values.get("税率"))
        ? parseImportDecimal(values.get("税率"), "税率", false)
        : BigDecimal.valueOf(13);
    if (taxRate.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new BusinessException("税率不能超过100%");
    }
    LocalDate expectedDate = parseImportDate(values.get("期望到货日期"));
    String technical = values.getOrDefault("规格技术要求", "").trim();
    if (!StringUtils.hasText(technical) && part != null && StringUtils.hasText(part.getModel())) {
      technical = part.getModel();
    }
    return new ImportedPurchaseLine(
        sourceRow, part, partName, quantity, amount(unitPrice), taxRate,
        expectedDate, values.getOrDefault("采购原因", "").trim(), technical);
  }

  private BigDecimal parseImportDecimal(String raw, String field, boolean positive) {
    if (!StringUtils.hasText(raw)) {
      throw new BusinessException(field + "不能为空");
    }
    try {
      String normalized = raw.trim()
          .replace(",", "")
          .replace("￥", "")
          .replace("¥", "")
          .replace("%", "");
      BigDecimal value = new BigDecimal(normalized);
      if ((positive && value.compareTo(BigDecimal.ZERO) <= 0)
          || (!positive && value.compareTo(BigDecimal.ZERO) < 0)) {
        throw new BusinessException(field + (positive ? "必须大于0" : "不能小于0"));
      }
      return value;
    } catch (NumberFormatException exception) {
      throw new BusinessException(field + "格式不正确：" + raw);
    }
  }

  private LocalDate parseImportDate(String raw) {
    if (!StringUtils.hasText(raw)) {
      throw new BusinessException("期望到货日期不能为空");
    }
    String normalized = raw.trim().replace(".", "-").replace("/", "-");
    for (java.time.format.DateTimeFormatter formatter : List.of(
        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
        java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d"))) {
      try {
        return LocalDate.parse(normalized, formatter);
      } catch (java.time.format.DateTimeParseException ignored) {
        // Try the next supported date format.
      }
    }
    throw new BusinessException("期望到货日期格式应为 yyyy-MM-dd");
  }

  private List<String> parseCsvRow(String line) {
    List<String> cells = new ArrayList<>();
    StringBuilder value = new StringBuilder();
    boolean quoted = false;
    for (int index = 0; index < line.length(); index++) {
      char current = line.charAt(index);
      if (current == '"') {
        if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
          value.append('"');
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (current == ',' && !quoted) {
        cells.add(value.toString());
        value.setLength(0);
      } else {
        value.append(current);
      }
    }
    cells.add(value.toString());
    return cells;
  }

  private void throwIfImportErrors(List<String> errors) {
    if (errors.isEmpty()) return;
    String detail = errors.stream().limit(20).collect(Collectors.joining("；"));
    if (errors.size() > 20) detail += "；另有" + (errors.size() - 20) + "处错误";
    throw new BusinessException("导入校验失败，未写入任何数据：" + detail);
  }

  private String combineImportReason(String shared, String lineReason, String technical) {
    List<String> parts = new ArrayList<>();
    if (StringUtils.hasText(shared)) parts.add(shared.trim());
    if (StringUtils.hasText(lineReason)) parts.add(lineReason.trim());
    if (StringUtils.hasText(technical)) parts.add("规格/技术要求：" + technical.trim());
    String combined = String.join("；", parts);
    if (combined.length() > 300) {
      throw new BusinessException("申请说明和规格要求合计不能超过300个字符");
    }
    return combined;
  }

  private String currentName() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
        ? principal.displayName() : "系统";
  }

  private record CostTarget(String code, String name) {}

  private record ImportedPurchaseLine(
      int sourceRow,
      InventoryPart part,
      String partName,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal taxRate,
      LocalDate expectedDate,
      String reason,
      String technicalRequirement
  ) {}
}
