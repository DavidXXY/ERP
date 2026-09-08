package com.company.ops.api.modules.reporting.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.collaboration.domain.ProjectStaffAssignment;
import com.company.ops.api.modules.collaboration.domain.ProjectTimesheet;
import com.company.ops.api.modules.collaboration.repository.ProjectStaffAssignmentRepository;
import com.company.ops.api.modules.collaboration.repository.ProjectTimesheetRepository;
import com.company.ops.api.modules.collaboration.service.CollaborationGovernanceService;
import com.company.ops.api.modules.office.domain.ApprovalRequest;
import com.company.ops.api.modules.office.domain.ApprovalRuntimeNode;
import com.company.ops.api.modules.office.domain.ApprovalStatus;
import com.company.ops.api.modules.office.domain.ApprovalType;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.ApprovalRequestRepository;
import com.company.ops.api.modules.office.repository.ApprovalRuntimeNodeRepository;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.reporting.domain.EmployeeReport;
import com.company.ops.api.modules.reporting.repository.EmployeeReportRepository;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportHoursService {
  public static final String BUSINESS_TYPE = "ENGINEERING_REPORT_CONFIRM";
  public static final BigDecimal HALF_DAY = BigDecimal.valueOf(4);
  public static final BigDecimal FULL_DAY = BigDecimal.valueOf(8);

  private final ApprovalRequestRepository approvalRepository;
  private final ApprovalRuntimeNodeRepository runtimeNodeRepository;
  private final EmployeeReportRepository reportRepository;
  private final ProjectRepository projectRepository;
  private final ProjectStaffAssignmentRepository assignmentRepository;
  private final ProjectTimesheetRepository timesheetRepository;
  private final CollaborationGovernanceService collaborationGovernanceService;
  private final SystemRoleRepository roleRepository;
  private final SystemUserRepository userRepository;
  private final SystemNotificationRepository notificationRepository;

  public ReportHoursService(ApprovalRequestRepository approvalRepository,
                            ApprovalRuntimeNodeRepository runtimeNodeRepository,
                            EmployeeReportRepository reportRepository,
                            ProjectRepository projectRepository,
                            ProjectStaffAssignmentRepository assignmentRepository,
                            ProjectTimesheetRepository timesheetRepository,
                            CollaborationGovernanceService collaborationGovernanceService,
                            SystemRoleRepository roleRepository,
                            SystemUserRepository userRepository,
                            SystemNotificationRepository notificationRepository) {
    this.approvalRepository = approvalRepository;
    this.runtimeNodeRepository = runtimeNodeRepository;
    this.reportRepository = reportRepository;
    this.projectRepository = projectRepository;
    this.assignmentRepository = assignmentRepository;
    this.timesheetRepository = timesheetRepository;
    this.collaborationGovernanceService = collaborationGovernanceService;
    this.roleRepository = roleRepository;
    this.userRepository = userRepository;
    this.notificationRepository = notificationRepository;
  }

  public static void validateHours(BigDecimal hours, UUID projectId, UUID additionalProjectId) {
    if (hours == null) throw new BusinessException("工程汇报必须填写工时");
    if (hours.compareTo(HALF_DAY) < 0) throw new BusinessException("工程汇报最小工时为半日（4小时）");
    if (hours.compareTo(FULL_DAY) > 0) throw new BusinessException("工程汇报单次工时不能超过全日（8小时）");
    if (additionalProjectId != null) {
      if (additionalProjectId.equals(projectId)) throw new BusinessException("第二个项目不能与主项目相同");
      if (hours.compareTo(HALF_DAY) != 0) throw new BusinessException("仅半日（4小时）工时可选填第二个项目");
    }
  }

  /** 校验当日工时容量（含已提交/已通过工时不超 8 小时），与工时填报规则一致。 */
  public void assertDailyCapacity(UUID userId, LocalDate workDate, BigDecimal hours) {
    if (userId == null || hours == null) return;
    BigDecimal occupied = timesheetRepository.findByUserIdAndWorkDate(userId, workDate).stream()
        .filter(item -> !"REJECTED".equals(item.getStatus()))
        .map(ProjectTimesheet::getHours)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (occupied.add(hours).compareTo(FULL_DAY) > 0) {
      throw new BusinessException("该员工当日工时将超过8小时（已累计 " + occupied.stripTrailingZeros().toPlainString() + " 小时），请调整汇报工时");
    }
  }

  /** 校验员工（系统账号 userId）已加入项目并返回各项目对应的派工记录。 */
  public List<ProjectStaffAssignment> joinedAssignments(UUID userId, UUID projectId, UUID additionalProjectId) {
    List<ProjectStaffAssignment> result = new ArrayList<>();
    for (UUID project : new UUID[] {projectId, additionalProjectId}) {
      if (project == null) continue;
      List<ProjectStaffAssignment> matches = assignmentRepository.findByProjectId(project).stream()
          .filter(a -> userId.equals(a.getUserId()))
          .toList();
      if (matches.isEmpty()) {
        Project entity = projectRepository.findById(project).orElse(null);
        throw new BusinessException("员工尚未加入项目" + (entity == null ? "" : "【" + entity.getName() + "】") + "，请先加入项目后再填报工程汇报");
      }
      result.add(matches.get(0));
    }
    return result;
  }

  /** 工程汇报提交时创建“项目经理确认”审批单（动态审批人：项目负责人，兜底项目经理角色）。 */
  @Transactional
  public UUID createConfirmation(EmployeeReport report, Project mainProject, UUID applicantUserId) {
    String code = "REP-" + report.getId();
    if (approvalRepository.existsByCode(code)) throw new BusinessException("审批单号已存在，请刷新后重试");
    String hoursText = report.getHours() == null ? "0" : report.getHours().stripTrailingZeros().toPlainString();
    String title = "工程汇报工时确认：" + report.getEmployeeName() + " · " + report.getReportDate() + " · " + hoursText + "小时";
    String content = "项目【" + mainProject.getName() + "】填报 " + hoursText + " 小时";
    if (report.getAdditionalProjectId() != null) {
      content += "，第二项目【" + projectName(report.getAdditionalProjectId()) + "】填报 " + hoursText + " 小时";
    }
    content += "。工作内容：" + (report.getContent() == null ? "" : report.getContent());

    ApprovalRequest approval = new ApprovalRequest();
    approval.setCode(code);
    approval.setApprovalType(ApprovalType.OTHER);
    approval.setTitle(title);
    approval.setSourceNo(report.getId().toString());
    approval.setAmount(report.getHours());
    approval.setStatus(ApprovalStatus.PENDING);
    approval.setApplicantName(report.getEmployeeName());
    approval.setApplicantUserId(applicantUserId);
    approval.setContent(content);
    approval.setDepartmentName(report.getDepartmentName());
    approval.setBusinessType(BUSINESS_TYPE);
    approval.setProjectCode(mainProject.getCode());
    approval.setApprovalMode("PARALLEL");
    approval.setCurrentStep(1);
    approval.setTotalSteps(1);
    approval.setCurrentApproverName(mainProject.getManagerName());
    ApprovalRequest saved = approvalRepository.save(approval);
    ApprovalRuntimeNode node = buildNode(saved, mainProject);
    runtimeNodeRepository.save(node);
    notifyApprover(node, saved);
    return saved.getId();
  }

  private void notifyApprover(ApprovalRuntimeNode node, ApprovalRequest approval) {
    SystemNotification notice = new SystemNotification();
    notice.setType("APPROVAL");
    notice.setTitle("工程汇报工时待确认：" + approval.getTitle());
    notice.setContent("请确认" + approval.getApplicantName() + "填报的工程汇报工时。");
    notice.setRelatedType("APPROVAL");
    notice.setRelatedId(approval.getId());
    notice.setRead(false);
    if ("USER".equals(node.getAssigneeType()) && node.getAssigneeId() != null) {
      notice.setTargetUserId(node.getAssigneeId());
      notificationRepository.save(notice);
    } else if ("ROLE".equals(node.getAssigneeType()) && node.getAssigneeId() != null) {
      roleRepository.findById(node.getAssigneeId()).ifPresent(role ->
          userRepository.findEnabledByRoleCode(role.getCode()).forEach(user -> {
            SystemNotification copy = new SystemNotification();
            copy.setType(notice.getType()); copy.setTitle(notice.getTitle()); copy.setContent(notice.getContent());
            copy.setRelatedType(notice.getRelatedType()); copy.setRelatedId(notice.getRelatedId());
            copy.setTargetUserId(user.getId()); copy.setRead(false);
            notificationRepository.save(copy);
          }));
    }
  }

  private ApprovalRuntimeNode buildNode(ApprovalRequest approval, Project mainProject) {
    ApprovalRuntimeNode node = new ApprovalRuntimeNode();
    node.setApprovalId(approval.getId());
    node.setStepNo(1);
    node.setNodeStatus("PENDING");
    node.setApprovalMode("PARALLEL");
    node.setStepPolicy("ANY_APPROVE");
    node.setSourceType("DYNAMIC");
    node.setSourceValue("PROJECT_MANAGER");
    node.setConditionText("工程汇报工时确认");
    SystemUser manager = mainProject.getManagerUserId() == null ? null
        : userRepository.findById(mainProject.getManagerUserId()).orElse(null);
    if (manager == null) {
      String managerName = mainProject.getManagerName();
      List<SystemUser> byName = managerName == null || managerName.isBlank()
          ? List.of() : userRepository.findByDisplayNameAndEnabledTrue(managerName);
      manager = byName.isEmpty() ? null : byName.get(0);
    }
    if (manager != null && manager.isEnabled()) {
      node.setAssigneeType("USER");
      node.setAssigneeId(manager.getId());
      node.setAssigneeName(manager.getDisplayName());
      return node;
    }
    SystemRole role = roleRepository.findByCodeAndTenantId("PROJECT_MANAGER", approval.getTenantId())
        .or(() -> roleRepository.findByCodeAndTenantId("PROJECT_DIRECTOR", approval.getTenantId()))
        .orElseThrow(() -> new BusinessException("无法解析项目经理审批人，请检查项目负责人或项目经理角色配置"));
    node.setAssigneeType("ROLE");
    node.setAssigneeId(role.getId());
    node.setAssigneeName(role.getName());
    return node;
  }

  /** 审批处理完成后的回写：更新汇报状态；通过时计入项目实际工时。 */
  @Transactional
  public void onApprovalProcessed(ApprovalRequest approval) {
    if (!BUSINESS_TYPE.equals(approval.getBusinessType()) || approval.getSourceNo() == null) return;
    if (approval.getStatus() != ApprovalStatus.APPROVED && approval.getStatus() != ApprovalStatus.REJECTED) return;
    UUID reportId;
    try {
      reportId = UUID.fromString(approval.getSourceNo());
    } catch (IllegalArgumentException e) {
      return;
    }
    EmployeeReport report = reportRepository.findById(reportId).orElse(null);
    if (report == null || !"ENGINEERING".equals(report.getReportCategory())) return;
    report.setApprovalId(approval.getId());
    report.setReportStatus(approval.getStatus() == ApprovalStatus.APPROVED ? "CONFIRMED" : "REJECTED");
    reportRepository.save(report);
    if (approval.getStatus() == ApprovalStatus.APPROVED) writeApprovedHours(report, approval);
  }

  private void writeApprovedHours(EmployeeReport report, ApprovalRequest approval) {
    if (approval.getApplicantUserId() == null) return;
    if (timesheetRepository.existsBySourceReportId(report.getId())) return;
    List<ProjectStaffAssignment> assignments = assignmentRepository.findByProjectIdIn(projectIds(report)).stream()
        .filter(a -> approval.getApplicantUserId().equals(a.getUserId()))
        .toList();
    if (assignments.isEmpty()) return;
    BigDecimal hoursPerProject = report.getHours() == null ? BigDecimal.ZERO
        : report.getHours().divide(BigDecimal.valueOf(assignments.size()), 2, RoundingMode.HALF_UP);
    Set<UUID> assignmentIds = new LinkedHashSet<>();
    for (ProjectStaffAssignment assignment : assignments) {
      ProjectTimesheet item = new ProjectTimesheet();
      item.setAssignmentId(assignment.getId());
      item.setProjectId(assignment.getProjectId());
      item.setUserId(assignment.getUserId());
      item.setWorkDate(report.getReportDate());
      item.setHours(hoursPerProject);
      item.setDescription("工程汇报工时（" + report.getReportDate() + "）");
      item.setStatus("APPROVED");
      item.setSubmittedBy(approval.getApplicantUserId());
      item.setReviewedBy(reviewerUserId(approval));
      item.setReviewedByName(approval.getApproverName());
      item.setReviewComment("项目经理确认工程汇报");
      item.setReviewedAt(OffsetDateTime.now());
      item.setSourceReportId(report.getId());
      item.setSourceApprovalId(approval.getId());
      timesheetRepository.save(item);
      assignmentIds.add(assignment.getId());
    }
    assignmentIds.forEach(collaborationGovernanceService::recalculateAssignment);
  }

  private List<UUID> projectIds(EmployeeReport report) {
    List<UUID> ids = new ArrayList<>();
    if (report.getProjectId() != null) ids.add(report.getProjectId());
    if (report.getAdditionalProjectId() != null) ids.add(report.getAdditionalProjectId());
    return ids;
  }

  private UUID reviewerUserId(ApprovalRequest approval) {
    if (approval.getApproverName() == null || approval.getApproverName().isBlank()) return null;
    return userRepository.findByDisplayNameAndEnabledTrue(approval.getApproverName()).stream()
        .map(SystemUser::getId).findFirst().orElse(null);
  }

  private String projectName(UUID projectId) {
    return projectRepository.findById(projectId).map(Project::getName).orElse("");
  }
}
