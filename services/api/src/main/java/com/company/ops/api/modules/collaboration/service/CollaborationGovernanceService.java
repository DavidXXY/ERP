package com.company.ops.api.modules.collaboration.service;

import static com.company.ops.api.common.util.MoneyUtils.amount;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.collaboration.domain.*;
import com.company.ops.api.modules.collaboration.dto.CollaborationDtos.*;
import com.company.ops.api.modules.collaboration.repository.*;
import com.company.ops.api.modules.hr.repository.LeaveRequestRepository;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.project.domain.*;
import com.company.ops.api.modules.project.repository.*;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaborationGovernanceService {
  private final CollaborationTaskControlRepository controls;
  private final CollaborationActionLogRepository actions;
  private final ProjectTimesheetRepository timesheets;
  private final TimesheetPeriodLockRepository periodLocks;
  private final ProjectBudgetVersionRepository budgetVersions;
  private final ProjectStaffAssignmentRepository assignments;
  private final ProjectRepository projects;
  private final ProjectCostEntryRepository projectCosts;
  private final SystemUserRepository users;
  private final SystemNotificationRepository notifications;
  private final QualificationEmployeeRepository employees;
  private final LeaveRequestRepository leaveRequests;

  public CollaborationGovernanceService(
      CollaborationTaskControlRepository controls, CollaborationActionLogRepository actions,
      ProjectTimesheetRepository timesheets, TimesheetPeriodLockRepository periodLocks,
      ProjectBudgetVersionRepository budgetVersions, ProjectStaffAssignmentRepository assignments,
      ProjectRepository projects, ProjectCostEntryRepository projectCosts, SystemUserRepository users,
      SystemNotificationRepository notifications, QualificationEmployeeRepository employees,
      LeaveRequestRepository leaveRequests) {
    this.controls=controls;this.actions=actions;this.timesheets=timesheets;this.periodLocks=periodLocks;
    this.budgetVersions=budgetVersions;this.assignments=assignments;this.projects=projects;this.projectCosts=projectCosts;
    this.users=users;this.notifications=notifications;this.employees=employees;this.leaveRequests=leaveRequests;
  }

  @Transactional
  public CollaborationTaskControl syncTask(String type, UUID sourceId, OffsetDateTime createdAt, UUID defaultAssignee) {
    return controls.findBySourceTypeAndSourceId(type,sourceId).orElseGet(()->{
      CollaborationTaskControl item=new CollaborationTaskControl();item.setSourceType(type);item.setSourceId(sourceId);
      item.setAssigneeUserId(defaultAssignee);item.setStatus("PENDING");
      item.setDueAt((createdAt==null?OffsetDateTime.now():createdAt).plusDays(2));
      return controls.save(item);
    });
  }

  @Transactional
  public Map<String,Object> actOnTodo(String type,UUID sourceId,TodoActionRequest request) {
    UserPrincipal p=requirePrincipal(); String action=request.action().toUpperCase(Locale.ROOT);
    CollaborationTaskControl item=controls.findBySourceTypeAndSourceId(type,sourceId)
        .orElseGet(()->syncTask(type,sourceId,OffsetDateTime.now(),null));
    switch(action){
      case "TRANSFER" -> {
        if(request.targetUserId()==null)throw new BusinessException("转办必须选择接收人");
        SystemUser target=activeUser(request.targetUserId());item.setAssigneeUserId(target.getId());item.setStatus("PENDING");
        notifyUser(target.getId(),"协同待办转办","“"+type+"”待办已由"+p.displayName()+"转交给你",type,sourceId,"TRANSFER-"+type+"-"+sourceId+"-"+item.getVersion());
      }
      case "CC" -> {
        if(request.ccUserIds()==null||request.ccUserIds().isEmpty())throw new BusinessException("请选择抄送人");
        request.ccUserIds().forEach(this::activeUser);
        item.setCcUserIds(joinIds(request.ccUserIds()));
        request.ccUserIds().forEach(id->notifyUser(id,"协同待办抄送",p.displayName()+"抄送了一条"+type+"待办",type,sourceId,"CC-"+type+"-"+sourceId+"-"+id));
      }
      case "COMPLETE" -> {item.setStatus("DONE");item.setCompletedAt(OffsetDateTime.now());}
      case "REOPEN" -> {item.setStatus("PENDING");item.setCompletedAt(null);}
      case "REMIND" -> {
        item.setReminderCount(item.getReminderCount()+1);item.setLastRemindedAt(OffsetDateTime.now());
        if(item.getAssigneeUserId()!=null)notifyUser(item.getAssigneeUserId(),"协同待办催办",p.displayName()+"催办了一条"+type+"待办",type,sourceId,"REMIND-"+type+"-"+sourceId+"-"+item.getReminderCount());
      }
      case "SET_DUE" -> {
        if(request.dueDate()==null)throw new BusinessException("请选择完成期限");
        item.setDueAt(request.dueDate().atTime(18,0).atOffset(ZoneOffset.ofHours(8)));
      }
      default -> throw new BusinessException("不支持的待办操作");
    }
    item.setLastComment(request.comment());controls.save(item);
    log(type,sourceId,action,p,request.targetUserId(),request.comment());
    return controlView(item);
  }

  @Transactional
  public Map<String,Object> submitTimesheet(TimesheetSubmitRequest request) {
    UserPrincipal p=requirePrincipal();
    ProjectStaffAssignment assignment=assignments.findById(request.assignmentId()).orElseThrow(()->new BusinessException("派工记录不存在"));
    boolean manager=p.permissions().contains("hr:employee:manage")||p.permissions().contains("project:cost:create")||p.roleCodes().contains("ADMIN");
    if(!manager&&!p.id().equals(assignment.getUserId()))throw new AccessDeniedException("只能填报本人工时");
    assertPeriodOpen(request.workDate());
    if(request.workDate().isBefore(assignment.getStartDate())||request.workDate().isAfter(assignment.getEndDate()))throw new BusinessException("工时日期不在派工周期内");
    if(request.workDate().getDayOfWeek()==DayOfWeek.SATURDAY||request.workDate().getDayOfWeek()==DayOfWeek.SUNDAY)throw new BusinessException("周末工时需由管理员补录");
    assertNotOnLeave(assignment.getUserId(),request.workDate());
    BigDecimal occupied=timesheets.findByUserIdAndWorkDate(assignment.getUserId(),request.workDate()).stream()
        .filter(x->!"REJECTED".equals(x.getStatus())).map(ProjectTimesheet::getHours).reduce(BigDecimal.ZERO,BigDecimal::add);
    if(occupied.add(request.hours()).compareTo(BigDecimal.valueOf(8))>0)throw new BusinessException("该员工当日有效工时不能超过8小时");
    ProjectTimesheet item=new ProjectTimesheet();item.setAssignmentId(assignment.getId());item.setProjectId(assignment.getProjectId());
    item.setUserId(assignment.getUserId());item.setWorkDate(request.workDate());item.setHours(request.hours());
    item.setDescription(request.description());item.setStatus("SUBMITTED");item.setSubmittedBy(p.id());timesheets.save(item);
    log("TIMESHEET",item.getId(),"SUBMIT",p,null,request.description());
    return timesheetView(item);
  }

  @Transactional
  public Map<String,Object> reviewTimesheet(UUID id,ReviewRequest request) {
    UserPrincipal p=requirePrincipal(); ProjectTimesheet item=timesheets.findById(id).orElseThrow(()->new BusinessException("工时记录不存在"));
    if(!"SUBMITTED".equals(item.getStatus()))throw new BusinessException("该工时已处理");
    if(p.id().equals(item.getSubmittedBy())&&!p.roleCodes().contains("ADMIN"))throw new BusinessException("工时提交人不能审批自己的工时");
    assertPeriodOpen(item.getWorkDate());
    String decision=request.decision().toUpperCase(Locale.ROOT);if(!Set.of("APPROVED","REJECTED").contains(decision))throw new BusinessException("审批决定不正确");
    item.setStatus(decision);item.setReviewedBy(p.id());item.setReviewedByName(p.displayName());item.setReviewComment(request.comment());item.setReviewedAt(OffsetDateTime.now());
    timesheets.save(item);recalculateAssignment(item.getAssignmentId());log("TIMESHEET",item.getId(),decision,p,null,request.comment());
    notifyUser(item.getUserId(),"工时审批结果",item.getWorkDate()+" 工时已"+("APPROVED".equals(decision)?"通过":"驳回"),"TIMESHEET",item.getId(),"TS-"+item.getId()+"-"+decision);
    return timesheetView(item);
  }

  @Transactional
  public Map<String,Object> lockPeriod(PeriodLockRequest request) {
    UserPrincipal p=requirePrincipal();if(periodLocks.existsByYearMonth(request.yearMonth()))throw new BusinessException("该月份已经结账锁定");
    TimesheetPeriodLock item=new TimesheetPeriodLock();item.setYearMonth(request.yearMonth());item.setLockedBy(p.id());item.setLockedByName(p.displayName());
    item.setLockedAt(OffsetDateTime.now());item.setReason(request.reason());periodLocks.save(item);log("TIMESHEET_PERIOD",item.getId(),"LOCK",p,null,request.reason());
    return periodLockView(item);
  }

  @Transactional
  public void unlockPeriod(String yearMonth,String comment) {
    UserPrincipal p=requirePrincipal();TimesheetPeriodLock item=periodLocks.findByYearMonth(yearMonth).orElseThrow(()->new BusinessException("该月份未锁定"));
    periodLocks.delete(item);log("TIMESHEET_PERIOD",item.getId(),"UNLOCK",p,null,comment);
  }

  @Transactional
  public Map<String,Object> requestBudgetChange(BudgetChangeRequest request) {
    UserPrincipal p=requirePrincipal();Project project=projects.findById(request.projectId()).orElseThrow(()->new BusinessException("项目不存在"));
    if(budgetVersions.existsByProjectIdAndStatus(project.getId(),"PENDING"))throw new BusinessException("该项目已有待审批预算变更");
    int next=budgetVersions.findFirstByProjectIdOrderByVersionNoDesc(project.getId()).map(x->x.getVersionNo()+1).orElse(1);
    ProjectBudgetVersion item=new ProjectBudgetVersion();item.setProjectId(project.getId());item.setVersionNo(next);item.setPreviousAmount(amount(project.getBudgetAmount()));
    item.setRequestedAmount(request.requestedAmount());item.setReason(request.reason());item.setStatus("PENDING");item.setRequestedBy(p.id());item.setRequestedByName(p.displayName());
    budgetVersions.save(item);log("BUDGET_CHANGE",item.getId(),"SUBMIT",p,null,request.reason());return budgetView(item,project.getName());
  }

  @Transactional
  public Map<String,Object> reviewBudgetChange(UUID id,ReviewRequest request) {
    UserPrincipal p=requirePrincipal();ProjectBudgetVersion item=budgetVersions.findById(id).orElseThrow(()->new BusinessException("预算变更不存在"));
    if(!"PENDING".equals(item.getStatus()))throw new BusinessException("预算变更已处理");
    if(p.id().equals(item.getRequestedBy())&&!p.roleCodes().contains("ADMIN"))throw new BusinessException("申请人不能审批自己的预算变更");
    String decision=request.decision().toUpperCase(Locale.ROOT);if(!Set.of("APPROVED","REJECTED").contains(decision))throw new BusinessException("审批决定不正确");
    item.setStatus(decision);item.setReviewedBy(p.id());item.setReviewedByName(p.displayName());item.setReviewComment(request.comment());item.setReviewedAt(OffsetDateTime.now());
    Project project=projects.findById(item.getProjectId()).orElseThrow(()->new BusinessException("项目不存在"));
    if("APPROVED".equals(decision)){project.setBudgetAmount(item.getRequestedAmount());projects.save(project);item.setEffectiveAt(OffsetDateTime.now());}
    budgetVersions.save(item);log("BUDGET_CHANGE",item.getId(),decision,p,null,request.comment());
    notifyUser(item.getRequestedBy(),"预算变更审批结果",project.getName()+"预算变更已"+("APPROVED".equals(decision)?"通过":"驳回"),"BUDGET_CHANGE",item.getId(),"BUD-"+item.getId()+"-"+decision);
    return budgetView(item,project.getName());
  }

  @Transactional(readOnly=true)
  public List<Map<String,Object>> timesheetViews(){
    return timesheets.findAllByOrderByWorkDateDesc().stream().map(this::timesheetView).toList();
  }
  @Transactional(readOnly=true)
  public List<Map<String,Object>> periodLockViews(){return periodLocks.findAllByOrderByYearMonthDesc().stream().map(this::periodLockView).toList();}
  @Transactional(readOnly=true)
  public List<Map<String,Object>> budgetVersionViews(){
    Map<UUID,String> names=projects.findAll().stream().collect(Collectors.toMap(Project::getId,Project::getName));
    return budgetVersions.findAllByOrderByCreatedAtDesc().stream().map(x->budgetView(x,names.getOrDefault(x.getProjectId(),"-"))).toList();
  }
  @Transactional(readOnly=true)
  public List<Map<String,Object>> actionViews(){
    return actions.findTop100ByOrderByCreatedAtDesc().stream().map(x->Map.<String,Object>of(
        "id",x.getId(),"sourceType",x.getSourceType(),"sourceId",x.getSourceId(),"actionType",x.getActionType(),
        "operatorName",x.getOperatorName(),"comment",Objects.toString(x.getComment(),""),"createdAt",x.getCreatedAt())).toList();
  }

  @Transactional(readOnly=true)
  public Map<String,Object> taskControlView(String type,UUID id){
    return controls.findBySourceTypeAndSourceId(type,id).map(this::controlView).orElse(Map.of());
  }

  @Transactional(readOnly=true)
  public List<Map<String,Object>> resourceLoads(Integer year,Integer month){
    LocalDate start=LocalDate.of(year==null?LocalDate.now().getYear():year,month==null?LocalDate.now().getMonthValue():month,1);
    LocalDate end=start.withDayOfMonth(start.lengthOfMonth());
    Map<UUID,SystemUser> userMap=users.findAll().stream().collect(Collectors.toMap(SystemUser::getId,Function.identity()));
    Map<UUID,List<ProjectTimesheet>> sheets=timesheets.findAllByOrderByWorkDateDesc().stream().filter(x->!x.getWorkDate().isBefore(start)&&!x.getWorkDate().isAfter(end)&&"APPROVED".equals(x.getStatus())).collect(Collectors.groupingBy(ProjectTimesheet::getUserId));
    long workdays=start.datesUntil(end.plusDays(1)).filter(d->d.getDayOfWeek()!=DayOfWeek.SATURDAY&&d.getDayOfWeek()!=DayOfWeek.SUNDAY).count();
    return assignments.findAllByOrderByCreatedAtDesc().stream().collect(Collectors.groupingBy(ProjectStaffAssignment::getUserId)).entrySet().stream().map(e->{
      SystemUser u=userMap.get(e.getKey());BigDecimal planned=e.getValue().stream().filter(a->!a.getEndDate().isBefore(start)&&!a.getStartDate().isAfter(end)).map(ProjectStaffAssignment::getPlannedHours).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal actual=sheets.getOrDefault(e.getKey(),List.of()).stream().map(ProjectTimesheet::getHours).reduce(BigDecimal.ZERO,BigDecimal::add);
      BigDecimal capacity=BigDecimal.valueOf(workdays*8);int utilization=capacity.signum()==0?0:actual.multiply(BigDecimal.valueOf(100)).divide(capacity,0,RoundingMode.HALF_UP).intValue();
      BigDecimal allocation=e.getValue().stream().filter(a->!a.getEndDate().isBefore(start)&&!a.getStartDate().isAfter(end)&&!"CANCELLED".equals(a.getStatus())).map(ProjectStaffAssignment::getAllocationPercent).reduce(BigDecimal.ZERO,BigDecimal::add);
      Map<String,Object> m=new LinkedHashMap<>();m.put("userId",e.getKey());m.put("userName",u==null?"-":u.getDisplayName());m.put("department",u==null||u.getOrganization()==null?"-":u.getOrganization().getName());m.put("departmentId",u==null||u.getOrganization()==null?null:u.getOrganization().getId());
      m.put("plannedHours",planned);m.put("actualHours",actual);m.put("capacityHours",capacity);m.put("utilizationRate",utilization);m.put("allocationPercent",allocation);m.put("overloaded",allocation.compareTo(BigDecimal.valueOf(100))>0||utilization>100);return m;
    }).toList();
  }

  @Scheduled(cron="${ops.collaboration.escalation-cron:0 20 * * * *}")
  @Transactional
  public void escalateOverdueTasks(){
    OffsetDateTime now=OffsetDateTime.now();
    for(CollaborationTaskControl item:controls.findByStatusNotOrderByDueAtAsc("DONE")){
      if(item.getDueAt()==null||!item.getDueAt().isBefore(now)||item.getAssigneeUserId()==null)continue;
      long days=Math.max(1,Duration.between(item.getDueAt(),now).toDays()+1);
      String key="COLLAB-OVERDUE-"+item.getId()+"-"+days;
      notifyUser(item.getAssigneeUserId(),"跨部门待办逾期升级","待办已逾期"+days+"天，请立即处理",item.getSourceType(),item.getSourceId(),key);
      item.setReminderCount(item.getReminderCount()+1);item.setLastRemindedAt(now);controls.save(item);
    }
  }

  private void recalculateAssignment(UUID assignmentId){
    ProjectStaffAssignment assignment=assignments.findById(assignmentId).orElseThrow(()->new BusinessException("派工不存在"));
    BigDecimal previous=amount(assignment.getActualHours()).multiply(amount(assignment.getHourlyCost()));
    BigDecimal hours=timesheets.findByAssignmentIdAndStatus(assignmentId,"APPROVED").stream().map(ProjectTimesheet::getHours).reduce(BigDecimal.ZERO,BigDecimal::add);
    assignment.setActualHours(hours);assignment.setStatus(hours.compareTo(assignment.getPlannedHours())>=0?"COMPLETED":hours.signum()>0?"IN_PROGRESS":"PLANNED");assignments.save(assignment);
    BigDecimal labor=hours.multiply(amount(assignment.getHourlyCost()));String sourceNo="STAFF-"+assignment.getId();
    ProjectCostEntry cost=projectCosts.findBySourceNo(sourceNo).orElseGet(ProjectCostEntry::new);cost.setProjectId(assignment.getProjectId());cost.setCategory(ProjectCostCategory.LABOR);
    cost.setSourceType(ProjectCostSource.MANUAL);cost.setSourceNo(sourceNo);cost.setDescription("审批通过的项目人员工时成本");cost.setAmount(labor);cost.setIncurredDate(LocalDate.now());projectCosts.save(cost);
    Project project=projects.findById(assignment.getProjectId()).orElseThrow(()->new BusinessException("项目不存在"));
    project.setActualCost(amount(project.getActualCost()).subtract(previous).add(labor));projects.save(project);
  }

  private void assertNotOnLeave(UUID userId,LocalDate date){
    employees.findBySystemUser_Id(userId).ifPresent(emp->{
      boolean onLeave=leaveRequests.findByEmployeeIdOrderByCreatedAtDesc(emp.getId()).stream().anyMatch(x->"APPROVED".equals(x.getStatus())&&!date.isBefore(x.getStartDate())&&!date.isAfter(x.getEndDate()));
      if(onLeave)throw new BusinessException("该员工当天处于已批准休假状态");
    });
  }
  private void assertPeriodOpen(LocalDate date){if(periodLocks.existsByYearMonth(YearMonth.from(date).toString()))throw new BusinessException("该月份已经结账锁定");}
  private SystemUser activeUser(UUID id){SystemUser u=users.findById(id).orElseThrow(()->new BusinessException("用户不存在"));if(!u.isEnabled())throw new BusinessException("用户已停用");return u;}
  private UserPrincipal requirePrincipal(){var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!(a.getPrincipal() instanceof UserPrincipal p))throw new AccessDeniedException("请先登录");return p;}
  private String joinIds(Collection<UUID> ids){return ids==null?"":ids.stream().distinct().map(UUID::toString).collect(Collectors.joining(","));}
  private void notifyUser(UUID userId,String title,String content,String type,UUID id,String key){
    if(userId==null||notifications.existsByDedupKey(key))return;SystemNotification n=new SystemNotification();n.setType("COLLABORATION");n.setTitle(title);n.setContent(content);
    n.setTargetUserId(userId);n.setRelatedType(type);n.setRelatedId(id);n.setDedupKey(key);n.setRead(false);notifications.save(n);
  }
  private void log(String type,UUID id,String action,UserPrincipal p,UUID target,String comment){
    CollaborationActionLog x=new CollaborationActionLog();x.setSourceType(type);x.setSourceId(id);x.setActionType(action);x.setOperatorUserId(p.id());x.setOperatorName(p.displayName());x.setTargetUserId(target);x.setComment(comment);actions.save(x);
  }
  private Map<String,Object> controlView(CollaborationTaskControl x){
    Map<String,Object>m=new LinkedHashMap<>();m.put("assigneeUserId",x.getAssigneeUserId());m.put("ccUserIds",x.getCcUserIds());m.put("status",x.getStatus());
    m.put("dueAt",x.getDueAt());m.put("completedAt",x.getCompletedAt());m.put("lastComment",x.getLastComment());m.put("reminderCount",x.getReminderCount());return m;
  }
  private Map<String,Object> timesheetView(ProjectTimesheet x){
    ProjectStaffAssignment a=assignments.findById(x.getAssignmentId()).orElse(null);SystemUser u=users.findById(x.getUserId()).orElse(null);Project p=projects.findById(x.getProjectId()).orElse(null);
    Map<String,Object>m=new LinkedHashMap<>();m.put("id",x.getId());m.put("assignmentId",x.getAssignmentId());m.put("projectName",p==null?"-":p.getName());m.put("userName",u==null?"-":u.getDisplayName());
    m.put("projectId",x.getProjectId());m.put("userId",x.getUserId());m.put("departmentId",u==null||u.getOrganization()==null?null:u.getOrganization().getId());
    m.put("workDate",x.getWorkDate());m.put("hours",x.getHours());m.put("description",x.getDescription());m.put("status",x.getStatus());m.put("reviewedByName",x.getReviewedByName());m.put("reviewComment",x.getReviewComment());
    m.put("cost",a==null?BigDecimal.ZERO:amount(a.getHourlyCost()).multiply(amount(x.getHours())));return m;
  }
  private Map<String,Object> periodLockView(TimesheetPeriodLock x){return Map.of("id",x.getId(),"yearMonth",x.getYearMonth(),"lockedByName",x.getLockedByName(),"lockedAt",x.getLockedAt(),"reason",x.getReason());}
  private Map<String,Object> budgetView(ProjectBudgetVersion x,String projectName){
    Map<String,Object>m=new LinkedHashMap<>();m.put("id",x.getId());m.put("projectId",x.getProjectId());m.put("projectName",projectName);m.put("versionNo",x.getVersionNo());
    m.put("previousAmount",x.getPreviousAmount());m.put("requestedAmount",x.getRequestedAmount());m.put("differenceAmount",x.getRequestedAmount().subtract(x.getPreviousAmount()));
    m.put("status",x.getStatus());m.put("reason",x.getReason());m.put("requestedByName",x.getRequestedByName());m.put("reviewedByName",x.getReviewedByName());m.put("reviewComment",x.getReviewComment());m.put("createdAt",x.getCreatedAt());return m;
  }
}
