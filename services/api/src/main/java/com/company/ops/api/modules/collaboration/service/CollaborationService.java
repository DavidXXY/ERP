package com.company.ops.api.modules.collaboration.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.collaboration.domain.*;
import com.company.ops.api.modules.collaboration.dto.CollaborationDtos.*;
import com.company.ops.api.modules.collaboration.repository.*;
import com.company.ops.api.modules.crm.domain.*;
import com.company.ops.api.modules.crm.repository.*;
import com.company.ops.api.modules.finance.repository.PaymentRecordRepository;
import com.company.ops.api.modules.maintenance.repository.EmployeeCertificateRepository;
import com.company.ops.api.modules.office.domain.ApprovalStatus;
import com.company.ops.api.modules.office.repository.ApprovalRequestRepository;
import com.company.ops.api.modules.office.repository.DocumentFileRepository;
import com.company.ops.api.modules.office.service.OfficeService;
import com.company.ops.api.modules.procurement.domain.*;
import com.company.ops.api.modules.procurement.repository.*;
import com.company.ops.api.modules.project.domain.*;
import com.company.ops.api.modules.project.repository.*;
import com.company.ops.api.modules.system.domain.*;
import com.company.ops.api.modules.system.repository.*;
import com.company.ops.api.modules.system.security.DataScopeService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CollaborationService {
  private final ResponsibilityBindingRepository responsibilities;
  private final ResponsibilityCollaboratorRepository responsibilityCollaborators;
  private final ProjectHandoverRepository handovers;
  private final ProjectStaffAssignmentRepository assignments;
  private final ProjectRepository projects;
  private final ProjectCostEntryRepository projectCosts;
  private final ServiceContractRepository contracts;
  private final CustomerRepository customers;
  private final ReceivableRepository receivables;
  private final PurchaseRequestRepository purchaseRequests;
  private final PurchaseOrderRepository purchaseOrders;
  private final GoodsReceiptRepository receipts;
  private final SupplierInvoiceRepository invoices;
  private final ProcurementPayableRepository payables;
  private final PaymentRecordRepository payments;
  private final SupplierRepository suppliers;
  private final ApprovalRequestRepository approvals;
  private final SystemUserRepository users;
  private final SystemOrganizationRepository organizations;
  private final EmployeeCertificateRepository certificates;
  private final CollaborationGovernanceService governance;
  private final DataScopeService dataScope;
  private final DocumentFileRepository documents;
  private final OfficeService officeService;

  public CollaborationService(
      ResponsibilityBindingRepository responsibilities, ResponsibilityCollaboratorRepository responsibilityCollaborators,
      ProjectHandoverRepository handovers,
      ProjectStaffAssignmentRepository assignments, ProjectRepository projects,
      ProjectCostEntryRepository projectCosts, ServiceContractRepository contracts,
      CustomerRepository customers, ReceivableRepository receivables,
      PurchaseRequestRepository purchaseRequests, PurchaseOrderRepository purchaseOrders,
      GoodsReceiptRepository receipts, SupplierInvoiceRepository invoices,
      ProcurementPayableRepository payables, PaymentRecordRepository payments,
      SupplierRepository suppliers, ApprovalRequestRepository approvals,
      SystemUserRepository users, SystemOrganizationRepository organizations,
      EmployeeCertificateRepository certificates, CollaborationGovernanceService governance,
      DataScopeService dataScope, DocumentFileRepository documents, OfficeService officeService) {
    this.responsibilities=responsibilities; this.responsibilityCollaborators=responsibilityCollaborators; this.handovers=handovers; this.assignments=assignments;
    this.projects=projects; this.projectCosts=projectCosts; this.contracts=contracts; this.customers=customers;
    this.receivables=receivables; this.purchaseRequests=purchaseRequests; this.purchaseOrders=purchaseOrders;
    this.receipts=receipts; this.invoices=invoices; this.payables=payables; this.payments=payments;
    this.suppliers=suppliers; this.approvals=approvals; this.users=users; this.organizations=organizations;
    this.certificates=certificates;this.governance=governance;this.dataScope=dataScope;this.documents=documents;this.officeService=officeService;
  }

  @Transactional
  public Map<String,Object> overview() {
    return overview(null,null,null);
  }

  @Transactional
  public Map<String,Object> overview(Integer year,Integer month,UUID departmentId) {
    syncHandovers();
    List<Map<String,Object>> todoRows=todos();
    List<Map<String,Object>> budgetRows=budgets();
    List<Map<String,Object>> procurementRows=procurementReconciliation();
    List<Map<String,Object>> financeRows=financialMilestones();
    List<Map<String,Object>> staffRows=assignmentViews();
    List<Map<String,Object>> handoverRows=handoverViews();
    List<Map<String,Object>> resourceRows=governance.resourceLoads(year,month).stream().filter(x->dataScope.canViewAssignee((UUID)x.get("userId"),false)).toList();
    if(year!=null){
      todoRows=todoRows.stream().filter(x->inPeriod(x.get("createdAt"),year,month)).toList();
      budgetRows=budgetRows.stream().filter(x->inPeriod(x.get("plannedStartDate"),year,month)).toList();
      procurementRows=procurementRows.stream().filter(x->inPeriod(x.get("businessDate"),year,month)).toList();
      financeRows=financeRows.stream().filter(x->inPeriod(x.get("businessDate"),year,month)).toList();
      staffRows=staffRows.stream().filter(x->inPeriod(x.get("startDate"),year,month)).toList();
      handoverRows=handoverRows.stream().filter(x->inPeriod(x.get("createdAt"),year,month)).toList();
    }
    if(departmentId!=null){
      todoRows=filterDepartment(todoRows,departmentId);budgetRows=filterDepartment(budgetRows,departmentId);procurementRows=filterDepartment(procurementRows,departmentId);
      financeRows=filterDepartment(financeRows,departmentId);staffRows=filterDepartment(staffRows,departmentId);handoverRows=filterDepartment(handoverRows,departmentId);resourceRows=filterDepartment(resourceRows,departmentId);
    }
    Map<String,Object> cards=new LinkedHashMap<>();
    cards.put("pendingTodos",todoRows.stream().filter(x->!"DONE".equals(x.get("status"))).count());
    cards.put("pendingHandovers",handoverRows.stream().filter(x->!"ACCEPTED".equals(x.get("status"))).count());
    cards.put("budgetWarnings",budgetRows.stream().filter(x->Boolean.TRUE.equals(x.get("warning"))).count());
    cards.put("reconciliationExceptions",procurementRows.stream().filter(x->!"MATCHED".equals(x.get("matchStatus"))).count());
    cards.put("overdueReceivables",financeRows.stream().filter(x->Boolean.TRUE.equals(x.get("overdue"))).count());
    cards.put("staffAssignments",staffRows.size());
    Map<String,Object> result=new LinkedHashMap<>();
    result.put("cards",cards);result.put("todos",todoRows);result.put("handovers",handoverRows);result.put("budgets",budgetRows);
    result.put("financialMilestones",financeRows);result.put("procurementReconciliation",procurementRows);
    result.put("staffAssignments",staffRows);result.put("responsibilities",responsibilityViews());
    Set<UUID> visibleProjectIds=projects.findAllByOrderByCreatedAtDesc().stream().filter(this::visibleProject).map(Project::getId).collect(Collectors.toSet());
    List<Map<String,Object>> timesheetRows=governance.timesheetViews().stream().filter(x->visibleProjectIds.contains((UUID)x.get("projectId"))||dataScope.canViewAssignee((UUID)x.get("userId"),false)).filter(x->inPeriod(x.get("workDate"),year,month)).toList();
    if(departmentId!=null)timesheetRows=filterDepartment(timesheetRows,departmentId);
    result.put("timesheets",timesheetRows);
    result.put("resourceLoads",resourceRows.stream().filter(x->dataScope.canViewAssignee((UUID)x.get("userId"),false)).toList());
    List<Map<String,Object>> versionRows=governance.budgetVersionViews().stream().filter(x->visibleProjectIds.contains((UUID)x.get("projectId"))).filter(x->inPeriod(x.get("createdAt"),year,month)).filter(x->departmentId==null||departmentId.equals(responsibilityDepartmentId("PROJECT",(UUID)x.get("projectId")))).toList();
    result.put("budgetVersions",versionRows);result.put("periodLocks",governance.periodLockViews());
    UserPrincipal principal=currentPrincipal();boolean auditVisible=principal!=null&&(principal.roleCodes().contains("ADMIN")||principal.permissions().contains("system:audit:view"));
    result.put("actionLogs",auditVisible?governance.actionViews():List.of());result.put("managementDashboard",managementDashboard(todoRows,budgetRows,financeRows,procurementRows,staffRows,resourceRows));
    return result;
  }

  @Transactional
  public ResponsibilityBinding saveResponsibility(ResponsibilityRequest request) {
    assertSource(request.sourceType(),request.sourceId());
    if(request.ownerUserId()!=null) users.findById(request.ownerUserId()).orElseThrow(()->new BusinessException("负责人不存在"));
    if(request.departmentId()!=null) assertDepartment(request.departmentId());
    if(request.collaboratorDepartmentIds()!=null) request.collaboratorDepartmentIds().forEach(this::assertDepartment);
    ResponsibilityBinding item=responsibilities.findBySourceTypeAndSourceId(request.sourceType(),request.sourceId()).orElseGet(ResponsibilityBinding::new);
    item.setSourceType(request.sourceType().toUpperCase()); item.setSourceId(request.sourceId());
    item.setOwnerUserId(request.ownerUserId()); item.setDepartmentId(request.departmentId());
    item.setCollaboratorDepartmentIds(request.collaboratorDepartmentIds()==null?"":request.collaboratorDepartmentIds().stream().map(UUID::toString).collect(Collectors.joining(",")));
    ResponsibilityBinding saved=responsibilities.save(item);
    responsibilityCollaborators.deleteByBindingId(saved.getId());
    if(request.collaboratorDepartmentIds()!=null)request.collaboratorDepartmentIds().stream().distinct().forEach(id->{
      ResponsibilityCollaborator link=new ResponsibilityCollaborator();link.setBindingId(saved.getId());link.setDepartmentId(id);responsibilityCollaborators.save(link);
    });
    return saved;
  }

  @Transactional
  public Map<String,Object> createHandover(HandoverRequest request) {
    Project project=projects.findById(request.projectId()).orElseThrow(()->new BusinessException("项目不存在"));
    ServiceContract contract=contracts.findById(request.contractId()).orElseThrow(()->new BusinessException("合同不存在"));
    if(!Objects.equals(project.getContractId(),contract.getId())||!Objects.equals(project.getCustomerId(),contract.getCustomerId()))
      throw new BusinessException("合同与项目关联关系不一致");
    ProjectHandover item=handovers.findByProjectId(project.getId()).orElseGet(ProjectHandover::new);
    applyHandover(item,request); item.setStatus("SUBMITTED"); item.setSubmittedAt(OffsetDateTime.now());
    return handoverView(handovers.save(item),project,contract);
  }

  @Transactional
  public Map<String,Object> acceptHandover(UUID id,AcceptHandoverRequest request) {
    ProjectHandover item=handovers.findById(id).orElseThrow(()->new BusinessException("交接单不存在"));
    if("ACCEPTED".equals(item.getStatus())) throw new BusinessException("该项目已经完成交接");
    if(!"SUBMITTED".equals(item.getStatus())&&!"PENDING".equals(item.getStatus()))throw new BusinessException("当前状态不能接收");
    if(isBlank(item.getScopeSummary())||isBlank(item.getPaymentTerms())||isBlank(item.getAcceptanceCriteria())||isBlank(item.getCustomerContact())||
        isBlank(item.getTechnicalSolution())||isBlank(item.getQuotationSummary()))throw new BusinessException("交接材料不完整，请补齐范围、付款、验收、联系人、技术方案和报价摘要");
    if(documents.countByBizTypeAndBizId("PROJECT_HANDOVER",item.getId())<1)throw new BusinessException("至少上传一份合同或技术交接附件");
    UserPrincipal principal=currentPrincipal();item.setStatus("ACCEPTED"); item.setAcceptedBy(principal==null?"系统":principal.displayName()); item.setAcceptedAt(OffsetDateTime.now()); item.setComment(request.comment());
    return handoverView(handovers.save(item),projects.findById(item.getProjectId()).orElse(null),contracts.findById(item.getContractId()).orElse(null));
  }

  @Transactional
  public Map<String,Object> assignStaff(StaffAssignmentRequest request) {
    Project project=projects.findById(request.projectId()).orElseThrow(()->new BusinessException("项目不存在"));
    if(project.getApprovalStatus()!=ProjectApprovalStatus.APPROVED||project.getStage()==ProjectStage.CLOSED) throw new BusinessException("只能向已审批且未关闭的项目派工");
    SystemUser user=users.findById(request.userId()).orElseThrow(()->new BusinessException("员工账号不存在"));
    if(!user.isEnabled()) throw new BusinessException("员工账号已停用");
    if(request.endDate().isBefore(request.startDate())) throw new BusinessException("结束日期不能早于开始日期");
    BigDecimal occupied=assignments.findByUserId(user.getId()).stream().filter(x->!"CANCELLED".equals(x.getStatus())&&
        !x.getEndDate().isBefore(request.startDate())&&!x.getStartDate().isAfter(request.endDate())).map(ProjectStaffAssignment::getAllocationPercent).reduce(BigDecimal.ZERO,BigDecimal::add);
    if(occupied.add(request.allocationPercent()).compareTo(BigDecimal.valueOf(100))>0) throw new BusinessException("该员工同期项目投入比例将超过100%");
    boolean certOk=certificates.findByUserIdOrderByExpiryDateAsc(user.getId()).stream().anyMatch(x->!x.getExpiryDate().isBefore(request.endDate()));
    ProjectStaffAssignment item=new ProjectStaffAssignment(); item.setProjectId(project.getId()); item.setUserId(user.getId());
    item.setDepartmentId(user.getOrganization()==null?null:user.getOrganization().getId()); item.setRoleName(request.roleName());
    item.setPlannedHours(request.plannedHours()); item.setActualHours(BigDecimal.ZERO); item.setHourlyCost(request.hourlyCost());
    item.setAllocationPercent(request.allocationPercent());
    item.setStartDate(request.startDate()); item.setEndDate(request.endDate()); item.setCertificateStatus(certOk?"VALID":"REVIEW_REQUIRED"); item.setStatus("PLANNED");
    return assignmentView(assignments.save(item),project,user);
  }

  @Transactional
  public Map<String,Object> recordHours(UUID id,StaffHoursRequest request) {
    ProjectStaffAssignment item=assignments.findById(id).orElseThrow(()->new BusinessException("派工记录不存在"));
    BigDecimal previousCost=amount(item.getActualHours()).multiply(amount(item.getHourlyCost()));
    item.setActualHours(request.actualHours()); item.setStatus(request.actualHours().compareTo(item.getPlannedHours())>=0?"COMPLETED":"IN_PROGRESS");
    BigDecimal laborCost=amount(item.getActualHours()).multiply(amount(item.getHourlyCost()));
    String sourceNo="STAFF-"+item.getId();
    ProjectCostEntry cost=projectCosts.findBySourceNo(sourceNo).orElseGet(ProjectCostEntry::new);
    cost.setProjectId(item.getProjectId());cost.setCategory(ProjectCostCategory.LABOR);cost.setSourceType(ProjectCostSource.MANUAL);
    cost.setSourceNo(sourceNo);cost.setDescription("项目人员工时成本");cost.setAmount(laborCost);cost.setIncurredDate(LocalDate.now());projectCosts.save(cost);
    Project project=projects.findById(item.getProjectId()).orElseThrow(()->new BusinessException("项目不存在"));
    project.setActualCost(amount(project.getActualCost()).subtract(previousCost).add(laborCost));projects.save(project);
    return assignmentView(assignments.save(item),project,users.findById(item.getUserId()).orElse(null));
  }

  @Transactional(readOnly=true)
  public List<Map<String,Object>> budgets() {
    List<PurchaseOrder> orderRows=purchaseOrders.findAll();
    List<ProjectCostEntry> costRows=projectCosts.findAll();
    List<ProjectStaffAssignment> staffRows=assignments.findAll();
    return projects.findAllByOrderByCreatedAtDesc().stream().filter(this::visibleProject).map(p->{
      BigDecimal committed=orderRows.stream().filter(o->Objects.equals(o.getProjectId(),p.getId())&&o.getStatus()!=PurchaseOrderStatus.CANCELLED)
          .map(o->amount(o.getOrderAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal actual=costRows.stream().filter(c->c.getProjectId().equals(p.getId())).map(c->amount(c.getAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal labor=staffRows.stream().filter(s->s.getProjectId().equals(p.getId())).map(s->amount(s.getActualHours()).multiply(amount(s.getHourlyCost()))).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal used=actual; BigDecimal remaining=amount(p.getBudgetAmount()).subtract(committed.max(used));
      int rate=amount(p.getBudgetAmount()).signum()==0?0:committed.max(used).multiply(BigDecimal.valueOf(100)).divide(p.getBudgetAmount(),0,java.math.RoundingMode.HALF_UP).intValue();
      Map<String,Object> m=new LinkedHashMap<>(); m.put("projectId",p.getId());m.put("projectCode",p.getCode());m.put("projectName",p.getName());
      m.put("department",responsibilityDepartment("PROJECT",p.getId()));m.put("departmentId",responsibilityDepartmentId("PROJECT",p.getId()));m.put("plannedStartDate",p.getPlannedStartDate());m.put("budgetAmount",amount(p.getBudgetAmount()));m.put("committedAmount",committed);
      m.put("actualAmount",actual);m.put("laborCost",labor);m.put("remainingAmount",remaining);m.put("usageRate",rate);m.put("warning",rate>=90);return m;
    }).toList();
  }

  @Transactional(readOnly=true)
  public List<Map<String,Object>> financialMilestones() {
    Map<UUID,Customer> customerMap=customers.findAll().stream().collect(Collectors.toMap(Customer::getId,Function.identity()));
    List<Receivable> all=receivables.findAll();
    Set<UUID> visibleContractIds=projects.findAllByOrderByCreatedAtDesc().stream().filter(this::visibleProject).map(Project::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
    return contracts.findAllByOrderByEndDateAsc().stream().filter(c->visibleContractIds.contains(c.getId())||
        Optional.ofNullable(customerMap.get(c.getCustomerId())).map(x->dataScope.canViewOwner(x.getOwnerName())).orElse(false)).map(c->{
      List<Receivable> rows=all.stream().filter(r->Objects.equals(r.getContractId(),c.getId())).toList();
      BigDecimal billed=rows.stream().filter(r->r.getInvoiceNo()!=null).map(r->amount(r.getAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal received=rows.stream().map(r->amount(r.getSettledAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal outstanding=rows.stream().map(r->amount(r.getAmount()).subtract(amount(r.getSettledAmount()))).reduce(BigDecimal.ZERO,BigDecimal::add);
      LocalDate nextDue=rows.stream().filter(r->amount(r.getAmount()).compareTo(amount(r.getSettledAmount()))>0).map(Receivable::getDueDate).min(LocalDate::compareTo).orElse(null);
      Map<String,Object> m=new LinkedHashMap<>();m.put("contractId",c.getId());m.put("contractCode",c.getCode());m.put("customerName",Optional.ofNullable(customerMap.get(c.getCustomerId())).map(Customer::getName).orElse("-"));
      m.put("contractAmount",amount(c.getAmount()));m.put("billedAmount",billed);m.put("receivedAmount",received);m.put("outstandingAmount",outstanding);
      m.put("nextDueDate",nextDue);m.put("businessDate",c.getStartDate());m.put("departmentId",projectDepartmentForContract(c.getId()));m.put("overdue",nextDue!=null&&nextDue.isBefore(LocalDate.now()));m.put("status",c.getStatus().name());return m;
    }).toList();
  }

  @Transactional(readOnly=true)
  public List<Map<String,Object>> procurementReconciliation() {
    List<GoodsReceipt> receiptRows=receipts.findAll(); List<SupplierInvoice> invoiceRows=invoices.findAll();
    List<ProcurementPayable> payableRows=payables.findAll(); Map<UUID,Supplier> supplierMap=suppliers.findAll().stream().collect(Collectors.toMap(Supplier::getId,Function.identity()));
    Map<UUID,Project> projectMap=projects.findAll().stream().collect(Collectors.toMap(Project::getId,Function.identity()));
    Map<String,Long> invoiceNoCounts=invoiceRows.stream().collect(Collectors.groupingBy(SupplierInvoice::getInvoiceNo,Collectors.counting()));
    return purchaseOrders.findAllByOrderByCreatedAtDesc().stream().filter(o->o.getProjectId()!=null?
        Optional.ofNullable(projectMap.get(o.getProjectId())).map(this::visibleProject).orElse(false):dataScope.canViewOrganization(o.getDepartmentId())).map(o->{
      List<GoodsReceipt> orderReceipts=receiptRows.stream().filter(r->r.getOrderId().equals(o.getId())).toList();
      List<SupplierInvoice> orderInvoices=invoiceRows.stream().filter(i->i.getOrderId().equals(o.getId())).toList();
      BigDecimal receivedQty=orderReceipts.stream().map(r->amount(r.getQualifiedQty())).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal received=orderReceipts.stream().map(r->amount(r.getQualifiedQty()).multiply(amount(r.getUnitPrice()))).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal invoiced=orderInvoices.stream().map(i->amount(i.getAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal payable=payableRows.stream().filter(p->p.getOrderId().equals(o.getId())&&p.getStatus()!=PayableStatus.CANCELLED).map(p->amount(p.getAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal paid=payableRows.stream().filter(p->p.getOrderId().equals(o.getId())).map(p->amount(p.getPaidAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal tolerance=amount(o.getOrderAmount()).multiply(new BigDecimal("0.005")).max(BigDecimal.ONE);
      boolean amountMatch=received.subtract(invoiced).abs().compareTo(tolerance)<=0&&invoiced.subtract(payable).abs().compareTo(tolerance)<=0;
      boolean quantityComplete=receivedQty.compareTo(amount(o.getOrderedQty()))>=0;
      boolean taxMatch=orderInvoices.stream().allMatch(i->amount(i.getTaxRate()).compareTo(amount(o.getTaxRate()))==0);
      boolean duplicateInvoice=orderInvoices.stream().anyMatch(i->invoiceNoCounts.getOrDefault(i.getInvoiceNo(),0L)>1);
      String match=received.signum()==0&&invoiced.signum()==0?"WAITING_RECEIPT":amountMatch&&taxMatch&&!duplicateInvoice?(quantityComplete?"MATCHED":"PARTIAL"):"MISMATCH";
      List<String> differences=new ArrayList<>();if(!quantityComplete)differences.add("未完全收货");
      if(!amountMatch)differences.add("收货/发票/应付金额不一致");if(!taxMatch)differences.add("税率不一致");if(duplicateInvoice)differences.add("发票号码重复");
      Map<String,Object> m=new LinkedHashMap<>();m.put("orderId",o.getId());m.put("orderCode",o.getCode());m.put("supplierName",Optional.ofNullable(supplierMap.get(o.getSupplierId())).map(Supplier::getName).orElse("-"));
      m.put("orderAmount",amount(o.getOrderAmount()));m.put("receivedAmount",received);m.put("invoiceAmount",invoiced);m.put("payableAmount",payable);m.put("paidAmount",paid);
      m.put("orderedQty",amount(o.getOrderedQty()));m.put("receivedQty",receivedQty);m.put("taxRate",o.getTaxRate());m.put("toleranceAmount",tolerance);
      m.put("differenceReason",differences.isEmpty()?"全部匹配":String.join("；",differences));m.put("duplicateInvoice",duplicateInvoice);
      m.put("businessDate",o.getCreatedAt());m.put("departmentId",o.getProjectId()!=null?responsibilityDepartmentId("PROJECT",o.getProjectId()):o.getDepartmentId());
      m.put("outstandingAmount",payable.subtract(paid));m.put("matchStatus",match);m.put("paymentStatus",paid.signum()==0?"UNPAID":paid.compareTo(payable)>=0?"PAID":"PARTIAL_PAID");m.put("status",o.getStatus().name());return m;
    }).toList();
  }

  @Transactional
  public List<Map<String,Object>> todos() {
    List<Map<String,Object>> rows=new ArrayList<>(); OffsetDateTime now=OffsetDateTime.now();
    Map<UUID,Project> projectMap=projects.findAll().stream().collect(Collectors.toMap(Project::getId,Function.identity()));
    Map<UUID,PurchaseOrder> orderMap=purchaseOrders.findAll().stream().collect(Collectors.toMap(PurchaseOrder::getId,Function.identity()));
    Set<UUID> visibleContractIds=financialMilestones().stream().map(x->(UUID)x.get("contractId")).collect(Collectors.toSet());
    approvals.findAllByOrderByCreatedAtDesc().stream().filter(a->a.getStatus()==ApprovalStatus.PENDING&&dataScope.canViewOwner(a.getApplicantName())).forEach(a->rows.add(todo("APPROVAL",a.getId(),a.getTitle(),a.getApplicantName(),a.getCreatedAt(),"/office/approvals",a.getDelegatedUserId())));
    purchaseRequests.findAll().stream().filter(x->x.getApprovalStatus()==com.company.ops.api.modules.procurement.domain.ApprovalStatus.PENDING&&visibleProcurementTarget(x.getProjectId(),x.getDepartmentId(),projectMap)).forEach(x->rows.add(todo("PURCHASE_REQUEST",x.getId(),"采购申请待审批 "+x.getCode(),x.getRequesterName(),x.getCreatedAt(),"/procurement/requests",null)));
    purchaseOrders.findAll().stream().filter(x->x.getApprovalStatus()==com.company.ops.api.modules.procurement.domain.ApprovalStatus.PENDING&&visibleProcurementTarget(x.getProjectId(),x.getDepartmentId(),projectMap)).forEach(x->rows.add(todo("PURCHASE_ORDER",x.getId(),"采购订单待审批 "+x.getCode(),x.getCostTargetName(),x.getCreatedAt(),"/procurement/orders",null)));
    handovers.findAll().stream().filter(x->!"ACCEPTED".equals(x.getStatus())&&Optional.ofNullable(projectMap.get(x.getProjectId())).map(this::visibleProject).orElse(false)).forEach(x->rows.add(todo("HANDOVER",x.getId(),"合同转项目待交接",projectName(x.getProjectId()),x.getCreatedAt(),"/collaboration",x.getProjectManagerId())));
    invoices.findAll().stream().filter(x->!"MATCHED".equals(x.getMatchStatus())).filter(x->Optional.ofNullable(orderMap.get(x.getOrderId())).map(o->visibleProcurementTarget(o.getProjectId(),o.getDepartmentId(),projectMap)).orElse(false)).forEach(x->rows.add(todo("INVOICE_MATCH",x.getId(),"供应商发票匹配异常 "+x.getInvoiceNo(),x.getDifferenceAmount().toPlainString(),x.getCreatedAt(),"/procurement/controls",null)));
    receivables.findAll().stream().filter(x->visibleContractIds.contains(x.getContractId())&&amount(x.getAmount()).compareTo(amount(x.getSettledAmount()))>0&&x.getDueDate().isBefore(LocalDate.now())).forEach(x->rows.add(todo("RECEIVABLE",x.getId(),"应收款已逾期 "+x.getCode(),x.getDueDate().toString(),x.getCreatedAt(),"/finance/receivables",null)));
    rows.forEach(m->{
      UUID assignee=(UUID)m.remove("defaultAssignee");CollaborationTaskControl control=governance.syncTask((String)m.get("type"),(UUID)m.get("id"),(OffsetDateTime)m.get("createdAt"),assignee);
      Map<String,Object> state=governance.taskControlView((String)m.get("type"),(UUID)m.get("id"));m.putAll(state);
      UUID controlledAssignee=(UUID)m.get("assigneeUserId");m.put("departmentId",controlledAssignee==null?null:users.findById(controlledAssignee).map(u->u.getOrganization()==null?null:u.getOrganization().getId()).orElse(null));
      OffsetDateTime due=control.getDueAt();long overdue=due!=null&&due.isBefore(now)?Math.max(1,Duration.between(due,now).toDays()+1):0;
      m.put("overdueDays",overdue);m.put("priority",overdue>5?"URGENT":overdue>0?"HIGH":"NORMAL");m.put("escalationLevel",overdue>7?2:overdue>3?1:0);
    });
    rows.sort((a,b)->((OffsetDateTime)b.get("createdAt")).compareTo((OffsetDateTime)a.get("createdAt"))); return rows;
  }

  @Transactional(readOnly=true) public List<Map<String,Object>> references(){
    return List.of(Map.of("users",users.findByEnabledTrueOrderByDisplayNameAsc().stream().map(u->Map.of("id",u.getId(),"name",u.getDisplayName(),"departmentId",u.getOrganization()==null?"":u.getOrganization().getId())).toList(),
        "departments",organizations.findAll().stream().filter(o->o.isEnabled()&&"DEPARTMENT".equals(o.getType())).map(o->Map.of("id",o.getId(),"name",o.getName())).toList(),
        "projects",projects.findAllByOrderByCreatedAtDesc().stream().filter(this::visibleProject).map(p->Map.of("id",p.getId(),"code",p.getCode(),"name",p.getName())).toList()));
  }

  @Transactional
  public String exportCsv(Integer year,Integer month,UUID departmentId){
    Map<String,Object> data=overview(year,month,departmentId);StringBuilder out=new StringBuilder();
    out.append("跨部门协同管理指标\n指标,数值\n");
    @SuppressWarnings("unchecked") Map<String,Object> dashboard=(Map<String,Object>)data.get("managementDashboard");
    dashboard.forEach((k,v)->csvRow(out,k,v));
    out.append("\n统一待办\n事项,说明,状态,优先级,逾期天数\n");
    @SuppressWarnings("unchecked") List<Map<String,Object>> todoRows=(List<Map<String,Object>>)data.get("todos");
    todoRows.forEach(x->csvRow(out,x.get("title"),x.get("detail"),x.get("status"),x.get("priority"),x.get("overdueDays")));
    out.append("\n项目预算\n项目,预算,已占用,已发生,剩余,使用率\n");
    @SuppressWarnings("unchecked") List<Map<String,Object>> budgetRows=(List<Map<String,Object>>)data.get("budgets");
    budgetRows.forEach(x->csvRow(out,x.get("projectName"),x.get("budgetAmount"),x.get("committedAmount"),x.get("actualAmount"),x.get("remainingAmount"),x.get("usageRate")+"%"));
    out.append("\n采购对账\n订单,供应商,订单额,收货额,发票额,应付额,已付额,匹配状态,差异原因\n");
    @SuppressWarnings("unchecked") List<Map<String,Object>> procurementRows=(List<Map<String,Object>>)data.get("procurementReconciliation");
    procurementRows.forEach(x->csvRow(out,x.get("orderCode"),x.get("supplierName"),x.get("orderAmount"),x.get("receivedAmount"),x.get("invoiceAmount"),x.get("payableAmount"),x.get("paidAmount"),x.get("matchStatus"),x.get("differenceReason")));
    return out.toString();
  }

  private void syncHandovers(){
    projects.findAllByOrderByCreatedAtDesc().stream().filter(p->p.getContractId()!=null&&!handovers.existsByProjectId(p.getId())).forEach(p->{
      ProjectHandover h=new ProjectHandover();h.setProjectId(p.getId());h.setContractId(p.getContractId());h.setScopeSummary(p.getName()+" 项目交付范围");
      h.setAcceptanceCriteria("按合同约定及项目验收标准执行");h.setStatus("PENDING");
      users.findByDisplayNameAndEnabledTrue(p.getManagerName()).stream().findFirst().ifPresent(u->{h.setProjectManagerId(u.getId());if(u.getOrganization()!=null)h.setDeliveryDepartmentId(u.getOrganization().getId());});
      handovers.save(h);
    });
  }
  public Object uploadHandoverMaterial(UUID id,MultipartFile file){
    ProjectHandover item=handovers.findById(id).orElseThrow(()->new BusinessException("交接单不存在"));assertVisibleProject(item.getProjectId());
    return officeService.storeDocument("PROJECT_HANDOVER",id,file);
  }
  @Transactional(readOnly=true)
  public List<?> handoverMaterials(UUID id){
    ProjectHandover item=handovers.findById(id).orElseThrow(()->new BusinessException("交接单不存在"));assertVisibleProject(item.getProjectId());
    return officeService.listDocumentsByBiz("PROJECT_HANDOVER",id);
  }
  private void applyHandover(ProjectHandover h,HandoverRequest r){h.setProjectId(r.projectId());h.setContractId(r.contractId());h.setSalesOwnerId(r.salesOwnerId());h.setProjectManagerId(r.projectManagerId());h.setSalesDepartmentId(r.salesDepartmentId());h.setDeliveryDepartmentId(r.deliveryDepartmentId());h.setScopeSummary(r.scopeSummary());h.setPaymentTerms(r.paymentTerms());h.setAcceptanceCriteria(r.acceptanceCriteria());h.setCustomerContact(r.customerContact());h.setTechnicalSolution(r.technicalSolution());h.setQuotationSummary(r.quotationSummary());h.setRiskNotes(r.riskNotes());}
  private List<Map<String,Object>> handoverViews(){Map<UUID,Project> pm=projects.findAll().stream().collect(Collectors.toMap(Project::getId,Function.identity()));Map<UUID,ServiceContract> cm=contracts.findAll().stream().collect(Collectors.toMap(ServiceContract::getId,Function.identity()));return handovers.findAllByOrderByCreatedAtDesc().stream().filter(h->Optional.ofNullable(pm.get(h.getProjectId())).map(this::visibleProject).orElse(false)).map(h->handoverView(h,pm.get(h.getProjectId()),cm.get(h.getContractId()))).toList();}
  private Map<String,Object> handoverView(ProjectHandover h,Project p,ServiceContract c){
    Map<String,Object>m=new LinkedHashMap<>();m.put("id",h.getId());m.put("projectId",h.getProjectId());m.put("contractId",h.getContractId());m.put("departmentId",h.getDeliveryDepartmentId());
    m.put("projectCode",p==null?"-":p.getCode());m.put("projectName",p==null?"-":p.getName());m.put("contractCode",c==null?"-":c.getCode());
    m.put("scopeSummary",h.getScopeSummary());m.put("paymentTerms",h.getPaymentTerms());m.put("acceptanceCriteria",h.getAcceptanceCriteria());
    m.put("customerContact",h.getCustomerContact());m.put("technicalSolution",h.getTechnicalSolution());m.put("quotationSummary",h.getQuotationSummary());m.put("riskNotes",h.getRiskNotes());
    long materialCount=documents.countByBizTypeAndBizId("PROJECT_HANDOVER",h.getId());m.put("materialCount",materialCount);
    m.put("materialsComplete",!isBlank(h.getScopeSummary())&&!isBlank(h.getPaymentTerms())&&!isBlank(h.getAcceptanceCriteria())&&!isBlank(h.getCustomerContact())&&!isBlank(h.getTechnicalSolution())&&!isBlank(h.getQuotationSummary())&&materialCount>0);
    m.put("status",h.getStatus());m.put("createdAt",h.getCreatedAt());m.put("acceptedBy",h.getAcceptedBy());m.put("acceptedAt",h.getAcceptedAt());m.put("comment",h.getComment());return m;
  }
  private List<Map<String,Object>> assignmentViews(){Map<UUID,Project>pm=projects.findAll().stream().collect(Collectors.toMap(Project::getId,Function.identity()));Map<UUID,SystemUser>um=users.findAll().stream().collect(Collectors.toMap(SystemUser::getId,Function.identity()));return assignments.findAllByOrderByCreatedAtDesc().stream().filter(a->Optional.ofNullable(pm.get(a.getProjectId())).map(this::visibleProject).orElse(false)||dataScope.canViewAssignee(a.getUserId(),false)).map(a->assignmentView(a,pm.get(a.getProjectId()),um.get(a.getUserId()))).toList();}
  private Map<String,Object> assignmentView(ProjectStaffAssignment a,Project p,SystemUser u){
    Map<String,Object>m=new LinkedHashMap<>();m.put("id",a.getId());m.put("projectId",a.getProjectId());m.put("projectName",p==null?"-":p.getName());m.put("userId",a.getUserId());m.put("userName",u==null?"-":u.getDisplayName());
    m.put("department",u==null||u.getOrganization()==null?"-":u.getOrganization().getName());m.put("departmentId",u==null||u.getOrganization()==null?null:u.getOrganization().getId());
    m.put("roleName",a.getRoleName());m.put("plannedHours",a.getPlannedHours());m.put("actualHours",a.getActualHours());m.put("hourlyCost",a.getHourlyCost());m.put("allocationPercent",a.getAllocationPercent());
    m.put("laborCost",amount(a.getActualHours()).multiply(amount(a.getHourlyCost())));m.put("startDate",a.getStartDate());m.put("endDate",a.getEndDate());m.put("certificateStatus",a.getCertificateStatus());m.put("status",a.getStatus());return m;
  }
  private List<Map<String,Object>> responsibilityViews(){Map<UUID,SystemUser>um=users.findAll().stream().collect(Collectors.toMap(SystemUser::getId,Function.identity()));Map<UUID,SystemOrganization>om=organizations.findAll().stream().collect(Collectors.toMap(SystemOrganization::getId,Function.identity()));return responsibilities.findAll().stream().filter(this::visibleResponsibility).map(r->{List<UUID> ids=responsibilityCollaborators.findByBindingId(r.getId()).stream().map(ResponsibilityCollaborator::getDepartmentId).toList();if(ids.isEmpty()&&!isBlank(r.getCollaboratorDepartmentIds()))ids=Arrays.stream(r.getCollaboratorDepartmentIds().split(",")).map(String::trim).filter(s->!s.isBlank()).map(UUID::fromString).toList();Map<String,Object>m=new LinkedHashMap<>();m.put("id",r.getId());m.put("sourceType",r.getSourceType());m.put("sourceId",r.getSourceId());m.put("ownerUserId",r.getOwnerUserId());m.put("departmentId",r.getDepartmentId());m.put("ownerName",Optional.ofNullable(um.get(r.getOwnerUserId())).map(SystemUser::getDisplayName).orElse("-"));m.put("departmentName",Optional.ofNullable(om.get(r.getDepartmentId())).map(SystemOrganization::getName).orElse("-"));m.put("collaboratorDepartmentIds",ids);m.put("collaboratorDepartmentNames",ids.stream().map(om::get).filter(Objects::nonNull).map(SystemOrganization::getName).toList());return m;}).toList();}
  private String responsibilityDepartment(String type,UUID id){return responsibilities.findBySourceTypeAndSourceId(type,id).flatMap(x->organizations.findById(x.getDepartmentId())).map(SystemOrganization::getName).orElse("-");}
  private UUID responsibilityDepartmentId(String type,UUID id){return responsibilities.findBySourceTypeAndSourceId(type,id).map(ResponsibilityBinding::getDepartmentId).orElse(null);}
  private UUID projectDepartmentForContract(UUID contractId){return projects.findAllByOrderByCreatedAtDesc().stream().filter(p->Objects.equals(contractId,p.getContractId())).map(p->responsibilityDepartmentId("PROJECT",p.getId())).filter(Objects::nonNull).findFirst().orElse(null);}
  private Map<String,Object> todo(String type,UUID id,String title,String detail,OffsetDateTime created,String link,UUID assignee){Map<String,Object>m=new LinkedHashMap<>();m.put("type",type);m.put("id",id);m.put("title",title);m.put("detail",detail==null?"":detail);m.put("createdAt",created==null?OffsetDateTime.now():created);m.put("status","PENDING");m.put("link",link);m.put("defaultAssignee",assignee);return m;}
  private String projectName(UUID id){return projects.findById(id).map(Project::getName).orElse("-");}
  private boolean visibleProject(Project p){
    if(p==null)return false;
    if(dataScope.canViewOwner(p.getManagerName()))return true;
    return responsibilities.findBySourceTypeAndSourceId("PROJECT",p.getId()).map(r->dataScope.canViewAssignee(r.getOwnerUserId(),false)||dataScope.canViewOrganization(r.getDepartmentId())).orElse(false);
  }
  private boolean visibleResponsibility(ResponsibilityBinding r){
    return switch(r.getSourceType()){
      case "PROJECT" -> projects.findById(r.getSourceId()).map(this::visibleProject).orElse(false);
      case "CONTRACT" -> contracts.findById(r.getSourceId()).map(c->financialMilestones().stream().anyMatch(x->r.getSourceId().equals(x.get("contractId")))).orElse(false);
      case "CUSTOMER" -> customers.findById(r.getSourceId()).map(c->dataScope.canViewOwner(c.getOwnerName())).orElse(false);
      default -> false;
    };
  }
  private boolean visibleProcurementTarget(UUID projectId,UUID departmentId,Map<UUID,Project> projectMap){
    return projectId!=null?Optional.ofNullable(projectMap.get(projectId)).map(this::visibleProject).orElse(false):dataScope.canViewOrganization(departmentId);
  }
  private void assertVisibleProject(UUID projectId){Project p=projects.findById(projectId).orElseThrow(()->new BusinessException("项目不存在"));if(!visibleProject(p))throw new org.springframework.security.access.AccessDeniedException("无权查看该项目");}
  private UserPrincipal currentPrincipal(){var a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getPrincipal() instanceof UserPrincipal p?p:null;}
  private boolean isBlank(String value){return value==null||value.isBlank();}
  private List<Map<String,Object>> filterDepartment(List<Map<String,Object>> rows,UUID departmentId){return rows.stream().filter(x->departmentId.equals(x.get("departmentId"))).toList();}
  private boolean inPeriod(Object value,Integer year,Integer month){
    if(year==null||value==null)return true;LocalDate date;
    if(value instanceof LocalDate d)date=d;else if(value instanceof OffsetDateTime d)date=d.toLocalDate();else return true;
    return date.getYear()==year&&(month==null||date.getMonthValue()==month);
  }
  private Map<String,Object> managementDashboard(List<Map<String,Object>> todos,List<Map<String,Object>> budgets,List<Map<String,Object>> finance,List<Map<String,Object>> procurement,List<Map<String,Object>> staff,List<Map<String,Object>> resources){
    long todoDone=todos.stream().filter(x->"DONE".equals(x.get("status"))).count();long overdue=todos.stream().filter(x->((Number)x.getOrDefault("overdueDays",0)).longValue()>0&&!"DONE".equals(x.get("status"))).count();
    BigDecimal budgetTotal=sum(budgets,"budgetAmount"),actualTotal=sum(budgets,"actualAmount"),contractTotal=sum(finance,"contractAmount"),receivedTotal=sum(finance,"receivedAmount");
    long matched=procurement.stream().filter(x->"MATCHED".equals(x.get("matchStatus"))).count();
    long approvedCount=approvals.findAllByOrderByCreatedAtDesc().stream().filter(x->x.getProcessedAt()!=null).count();
    double averageApprovalHours=approvals.findAllByOrderByCreatedAtDesc().stream().filter(x->x.getProcessedAt()!=null).mapToLong(x->Duration.between(x.getCreatedAt(),x.getProcessedAt()).toHours()).average().orElse(0);
    List<ProjectHandover> accepted=handovers.findAll().stream().filter(x->x.getAcceptedAt()!=null).toList();
    double handoverHours=accepted.stream().mapToLong(x->Duration.between(x.getCreatedAt(),x.getAcceptedAt()).toHours()).average().orElse(0);
    Map<UUID,List<GoodsReceipt>> receiptsByOrder=receipts.findAll().stream().collect(Collectors.groupingBy(GoodsReceipt::getOrderId));
    List<PurchaseOrder> delivered=purchaseOrders.findAll().stream().filter(x->x.getExpectedDeliveryDate()!=null&&receiptsByOrder.containsKey(x.getId())).toList();
    long onTime=delivered.stream().filter(o->receiptsByOrder.get(o.getId()).stream().map(GoodsReceipt::getReceivedDate).max(LocalDate::compareTo).orElse(LocalDate.MAX).compareTo(o.getExpectedDeliveryDate())<=0).count();
    BigDecimal planned=staff.stream().map(x->amount((BigDecimal)x.get("plannedHours"))).reduce(BigDecimal.ZERO,BigDecimal::add);
    BigDecimal actual=staff.stream().map(x->amount((BigDecimal)x.get("actualHours"))).reduce(BigDecimal.ZERO,BigDecimal::add);
    Map<String,Object>m=new LinkedHashMap<>();m.put("todoCompletionRate",percent(todoDone,todos.size()));m.put("todoOverdueRate",percent(overdue,todos.size()));
    m.put("averageApprovalHours",Math.round(averageApprovalHours*10)/10.0);m.put("processedApprovalCount",approvedCount);m.put("averageHandoverHours",Math.round(handoverHours*10)/10.0);
    m.put("budgetVariance",budgetTotal.subtract(actualTotal));m.put("budgetUsageRate",percent(actualTotal,budgetTotal));m.put("collectionRate",percent(receivedTotal,contractTotal));
    m.put("procurementMatchRate",percent(matched,procurement.size()));m.put("supplierOnTimeRate",percent(onTime,delivered.size()));m.put("resourceUtilizationRate",percent(actual,planned));
    m.put("overloadedEmployees",resources.stream().filter(x->Boolean.TRUE.equals(x.get("overloaded"))).count());return m;
  }
  private BigDecimal sum(List<Map<String,Object>> rows,String key){return rows.stream().map(x->x.get(key) instanceof BigDecimal b?b:BigDecimal.ZERO).reduce(BigDecimal.ZERO,BigDecimal::add);}
  private int percent(long part,long total){return total==0?0:(int)Math.round(part*100.0/total);}
  private int percent(BigDecimal part,BigDecimal total){return total.signum()==0?0:part.multiply(BigDecimal.valueOf(100)).divide(total,0,java.math.RoundingMode.HALF_UP).intValue();}
  private void csvRow(StringBuilder out,Object... values){for(int i=0;i<values.length;i++){if(i>0)out.append(',');String v=Objects.toString(values[i],"").replace("\"","\"\"");out.append('"').append(v).append('"');}out.append('\n');}
  private void assertDepartment(UUID id){SystemOrganization o=organizations.findById(id).orElseThrow(()->new BusinessException("部门不存在"));if(!o.isEnabled()||!"DEPARTMENT".equals(o.getType()))throw new BusinessException("请选择有效部门");}
  private void assertSource(String type,UUID id){switch(type.toUpperCase()){case"PROJECT"->projects.findById(id).orElseThrow(()->new BusinessException("项目不存在"));case"CONTRACT"->contracts.findById(id).orElseThrow(()->new BusinessException("合同不存在"));case"CUSTOMER"->customers.findById(id).orElseThrow(()->new BusinessException("客户不存在"));default->throw new BusinessException("暂不支持该责任对象");}}
}
