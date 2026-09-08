package com.company.ops.api.modules.reporting.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.collaboration.domain.ProjectStaffAssignment;
import com.company.ops.api.modules.collaboration.repository.ProjectStaffAssignmentRepository;
import com.company.ops.api.modules.office.domain.SystemNotification;
import com.company.ops.api.modules.office.repository.SystemNotificationRepository;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.reporting.domain.EmployeeReport;
import com.company.ops.api.modules.project.domain.Project;
import com.company.ops.api.modules.project.domain.ProjectExecutionStatus;
import com.company.ops.api.modules.project.repository.ProjectRepository;
import com.company.ops.api.modules.reporting.dto.ReportingDtos.ReportCreateRequest;
import com.company.ops.api.modules.reporting.dto.ReportingDtos.ReportProjectOption;
import com.company.ops.api.modules.reporting.dto.ReportingDtos.ReportResponse;
import com.company.ops.api.modules.reporting.dto.ReportingDtos.ReportUserOption;
import com.company.ops.api.modules.reporting.repository.EmployeeReportRepository;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
  private static final List<String> REPORT_TYPES = List.of("DAILY", "WEEKLY", "MONTHLY");

  private final EmployeeReportRepository reportRepository;
  private final QualificationEmployeeRepository employeeRepository;
  private final SystemUserRepository userRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final SystemNotificationRepository notificationRepository;
  private final ProjectStaffAssignmentRepository staffAssignmentRepository;
  private final ProjectRepository projectRepository;
  private final ReportHoursService reportHoursService;
  private final ObjectMapper objectMapper;

  public ReportService(EmployeeReportRepository reportRepository,
                       QualificationEmployeeRepository employeeRepository,
                       SystemUserRepository userRepository,
                       SystemOrganizationRepository organizationRepository,
                       SystemNotificationRepository notificationRepository,
                       ProjectStaffAssignmentRepository staffAssignmentRepository,
                       ProjectRepository projectRepository,
                       ReportHoursService reportHoursService,
                       ObjectMapper objectMapper) {
    this.reportRepository = reportRepository;
    this.employeeRepository = employeeRepository;
    this.userRepository = userRepository;
    this.organizationRepository = organizationRepository;
    this.notificationRepository = notificationRepository;
    this.staffAssignmentRepository = staffAssignmentRepository;
    this.projectRepository = projectRepository;
    this.reportHoursService = reportHoursService;
    this.objectMapper = objectMapper;
  }

  // ------------------------------------------------------------------
  // Reference data
  // ------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<ReportUserOption> candidateCcUsers(UUID userId) {
    var currentOpt = employeeRepository.findBySystemUser_Id(userId);
    if (currentOpt.isEmpty()) return List.of();
    List<SystemUser> users = userRepository.findByEnabledTrueOrderByDisplayNameAsc();
    List<QualificationEmployee> linked = employeeRepository.findAll().stream()
        .filter(e -> e.getSystemUser() != null && e.getSystemUser().getId() != null)
        .toList();
    Set<UUID> linkedUserIds = linked.stream()
        .map(e -> e.getSystemUser().getId()).collect(Collectors.toSet());
    List<ReportUserOption> result = new ArrayList<>();
    for (SystemUser user : users) {
      if (user.getId().equals(userId)) continue;
      boolean hasEmployee = linkedUserIds.contains(user.getId());
      result.add(new ReportUserOption(user.getId(), user.getDisplayName() + (hasEmployee ? "" : "（未关联档案）")));
    }
    return result;
  }

  // ------------------------------------------------------------------
  // Employee self-service
  // ------------------------------------------------------------------

  /** 当前员工加入的有效项目（关闭/取消的项目不再出现在可汇报列表中）。 */
  @Transactional(readOnly = true)
  public List<ReportProjectOption> myProjects(UUID userId) {
    // 员工在项目成员表中有记录即为“加入的项目”，成员表存的是系统账号 ID
    List<ProjectStaffAssignment> assignments = staffAssignmentRepository.findByUserId(userId);
    if (assignments.isEmpty()) return List.of();
    Set<UUID> projectIds = assignments.stream().map(ProjectStaffAssignment::getProjectId)
        .collect(Collectors.toSet());
    return projectRepository.findAllById(projectIds).stream()
        .filter(p -> p.getExecutionStatus() != ProjectExecutionStatus.CLOSED
            && p.getExecutionStatus() != ProjectExecutionStatus.CANCELLED)
        .sorted(java.util.Comparator.comparing(Project::getCode))
        .map(p -> new ReportProjectOption(p.getId(), p.getCode(), p.getName()))
        .toList();
  }

  private boolean isEmployeeJoinedProject(UUID employeeId, UUID projectId) {
    // 员工档案关联的账号在项目成员表中，即为“加入该项目”
    var employeeOpt = employeeRepository.findById(employeeId);
    if (employeeOpt.isEmpty() || employeeOpt.get().getSystemUser() == null) return false;
    UUID userId = employeeOpt.get().getSystemUser().getId();
    return staffAssignmentRepository.findByProjectId(projectId).stream()
        .anyMatch(a -> userId.equals(a.getUserId()));
  }

  @Transactional
  public ReportResponse createReport(UUID userId, ReportCreateRequest request) {
    QualificationEmployee employee = employeeRepository.findBySystemUser_Id(userId)
        .orElseThrow(() -> new BusinessException("未找到员工档案，请联系管理员关联账号后再填写汇报"));
    if (!REPORT_TYPES.contains(request.reportType())) {
      throw new BusinessException("不支持的汇报类型");
    }
    String category = request.reportCategory() == null || request.reportCategory().isBlank()
        ? "DAILY" : request.reportCategory();
    if (!List.of("DAILY", "ENGINEERING").contains(category)) {
      throw new BusinessException("不支持的汇报类别");
    }
    if (request.reportDate().isAfter(LocalDate.now())) {
      throw new BusinessException("汇报日期不能晚于今天");
    }
    UUID projectId = null;
    UUID additionalProjectId = null;
    BigDecimal hours = null;
    Project mainProject = null;
    if ("ENGINEERING".equals(category)) {
      if (request.projectId() == null) {
        throw new BusinessException("工程汇报必须选择项目");
      }
      projectId = request.projectId();
      additionalProjectId = request.additionalProjectId();
      hours = request.hours();
      ReportHoursService.validateHours(hours, projectId, additionalProjectId);
      mainProject = projectRepository.findById(projectId)
          .orElseThrow(() -> new BusinessException("工程汇报项目不存在"));
      reportHoursService.joinedAssignments(userId, projectId, additionalProjectId);
      reportHoursService.assertDailyCapacity(userId, request.reportDate(), hours);
    }
    List<UUID> ccIds = request.ccUserIds() == null ? List.of() : request.ccUserIds().stream()
        .distinct().toList();
    String ccIdsJson = writeJson(ccIds);

    EmployeeReport entity = new EmployeeReport();
    entity.setReportType(request.reportType());
    entity.setReportCategory(category);
    entity.setProjectId(projectId);
    entity.setAdditionalProjectId(additionalProjectId);
    entity.setHours(hours);
    entity.setReportStatus("PENDING_CONFIRM");
    entity.setReportDate(request.reportDate());
    entity.setContent(request.content().trim());
    entity.setProgressSummary(trimToNull(request.progressSummary()));
    entity.setPlanNext(trimToNull(request.planNext()));
    entity.setIssue(trimToNull(request.issue()));
    entity.setEmployeeId(employee.getId());
    entity.setEmployeeName(employee.getName());
    entity.setDepartmentName(employee.getDepartment());
    entity.setCcUserIds(ccIdsJson);
    entity.setCcNames(resolveCcNames(ccIds));
    EmployeeReport saved = reportRepository.save(entity);

    // 工程汇报：生成项目经理确认审批单
    if ("ENGINEERING".equals(category)) {
      UUID approvalId = reportHoursService.createConfirmation(saved, mainProject, userId);
      saved.setApprovalId(approvalId);
      saved.setReportStatus("PENDING_CONFIRM");
      saved = reportRepository.save(saved);
    }
    notifyCcUsers(saved, ccIds, userId);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<ReportResponse> listMyReports(UUID userId) {
    return employeeRepository.findBySystemUser_Id(userId)
        .map(emp -> reportRepository.findByEmployeeIdOrderByReportDateDescCreatedAtDesc(emp.getId()).stream()
            .map(this::toResponse).toList())
        .orElse(List.of());
  }

  // ------------------------------------------------------------------
  // Manager inbox
  // ------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<ReportResponse> listReceivedReports(UUID userId, String type, LocalDate fromDate, LocalDate toDate) {
    Set<UUID> subordinateIds = subordinateEmployeeIds(userId);
    List<EmployeeReport> subordinates = subordinateIds.isEmpty() ? List.of()
        : reportRepository.findSubordinates(subordinateIds, type, fromDate, toDate);
    List<EmployeeReport> ccToMe = reportRepository.findCcToMe(userId, type, fromDate, toDate);
    Set<UUID> seen = new HashSet<>();
    List<EmployeeReport> merged = new ArrayList<>();
    for (EmployeeReport r : subordinates) {
      if (seen.add(r.getId())) merged.add(r);
    }
    for (EmployeeReport r : ccToMe) {
      if (seen.add(r.getId())) merged.add(r);
    }
    merged.sort((a, b) -> {
      int c = b.getReportDate().compareTo(a.getReportDate());
      return c != 0 ? c : b.getCreatedAt().compareTo(a.getCreatedAt());
    });
    return merged.stream().map(this::toResponse).toList();
  }

  /** 根据组织树向下递归收集当前用户的所有下属员工（直属/下级部门）。 */
  private Set<UUID> subordinateEmployeeIds(UUID userId) {
    Set<UUID> result = new LinkedHashSet<>();
    var orgOpt = userRepository.findById(userId).map(SystemUser::getOrganization);
    if (orgOpt.isEmpty()) return result;
    Set<UUID> orgIds = new LinkedHashSet<>();
    collectOrgIds(orgOpt.get(), orgIds);
    if (orgIds.isEmpty()) return result;
    for (UUID orgId : orgIds) {
      collectOrgEmployees(orgId, result, userId);
    }
    return result;
  }

  private void collectOrgEmployees(UUID orgId, Set<UUID> acc, UUID currentUserId) {
    List<QualificationEmployee> employees = employeeRepository.findByOrganization_IdOrderByNameAsc(orgId);
    for (QualificationEmployee e : employees) {
      if (e.getSystemUser() != null && e.getSystemUser().getId() != null
          && !e.getSystemUser().getId().equals(currentUserId)) {
        acc.add(e.getId());
      }
    }
  }

  private void collectOrgIds(SystemOrganization org, Set<UUID> acc) {
    if (org == null || !acc.add(org.getId())) return;
    for (SystemOrganization child : org.getChildren()) {
      collectOrgIds(child, acc);
    }
  }

  // ------------------------------------------------------------------
  // Notifications
  // ------------------------------------------------------------------

  private void notifyCcUsers(EmployeeReport entity, List<UUID> ccIds, UUID applicantUserId) {
    for (UUID ccId : ccIds) {
      if (ccId.equals(applicantUserId)) continue;
      notificationRepository.save(notification(entity, ccId));
    }
  }

  private SystemNotification notification(EmployeeReport report, UUID targetUserId) {
    SystemNotification item = new SystemNotification();
    item.setType("REPORT");
    item.setTitle("新的" + typeLabel(report.getReportType()) + "抄送：" + report.getEmployeeName());
    item.setContent(report.getReportType() + "（" + report.getReportDate() + "）："
        + firstLine(report.getContent()));
    item.setRelatedType("REPORT");
    item.setRelatedId(report.getId());
    item.setTargetUserId(targetUserId);
    item.setRead(false);
    return item;
  }

  // ------------------------------------------------------------------
  // Mapping helpers
  // ------------------------------------------------------------------

  private ReportResponse toResponse(EmployeeReport r) {
    List<UUID> ccIds = readCcIds(r.getCcUserIds());
    String projectName = null;
    if (r.getProjectId() != null) {
      projectName = projectRepository.findById(r.getProjectId())
          .map(p -> p.getCode() + " " + p.getName()).orElse(null);
    }
    String additionalProjectName = null;
    if (r.getAdditionalProjectId() != null) {
      additionalProjectName = projectRepository.findById(r.getAdditionalProjectId())
          .map(p -> p.getCode() + " " + p.getName()).orElse(null);
    }
    return new ReportResponse(r.getId(), r.getReportType(), r.getReportCategory(),
        r.getProjectId(), projectName, r.getAdditionalProjectId(), additionalProjectName,
        r.getHours(), r.getReportStatus(), r.getApprovalId(),
        r.getReportDate(), r.getContent(),
        r.getProgressSummary(), r.getPlanNext(), r.getIssue(),
        r.getEmployeeId(), r.getEmployeeName(), r.getDepartmentName(),
        ccIds, r.getCcNames() == null ? List.of() : List.of(r.getCcNames().split(",")),
        r.getCreatedAt(), r.getUpdatedAt());
  }

  private String resolveCcNames(List<UUID> ccIds) {
    if (ccIds.isEmpty()) return null;
    Set<UUID> ids = new HashSet<>(ccIds);
    return userRepository.findAllById(ids).stream()
        .map(SystemUser::getDisplayName)
        .collect(Collectors.joining(","));
  }

  private List<UUID> readCcIds(String json) {
    try {
      return objectMapper.readValue(json == null ? "[]" : json, new TypeReference<List<UUID>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }

  private String writeJson(List<UUID> ccIds) {
    try {
      return objectMapper.writeValueAsString(ccIds);
    } catch (Exception e) {
      return "[]";
    }
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String firstLine(String content) {
    if (content == null) return "";
    int idx = content.indexOf('\n');
    String line = idx >= 0 ? content.substring(0, idx) : content;
    return line.length() > 60 ? line.substring(0, 60) + "…" : line;
  }

  private String typeLabel(String type) {
    return switch (type == null ? "" : type) {
      case "DAILY" -> "日报";
      case "WEEKLY" -> "周报";
      case "MONTHLY" -> "月报";
      default -> "汇报";
    };
  }
}
