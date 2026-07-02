package com.company.ops.api.modules.hr.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.hr.domain.EmergencyContact;
import com.company.ops.api.modules.hr.domain.EmployeeEducation;
import com.company.ops.api.modules.hr.domain.EmployeeLifecycleRecord;
import com.company.ops.api.modules.hr.domain.EmployeeWorkExperience;
import com.company.ops.api.modules.hr.domain.LeaveRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.*;
import com.company.ops.api.modules.hr.repository.EmergencyContactRepository;
import com.company.ops.api.modules.hr.repository.EmployeeEducationRepository;
import com.company.ops.api.modules.hr.repository.EmployeeLifecycleRecordRepository;
import com.company.ops.api.modules.hr.repository.EmployeeWorkExperienceRepository;
import com.company.ops.api.modules.hr.repository.LeaveRequestRepository;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Service
public class HrService {

    private final QualificationEmployeeRepository employeeRepository;
    private final EmployeeEducationRepository educationRepository;
    private final EmployeeWorkExperienceRepository workExperienceRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final EmployeeLifecycleRecordRepository lifecycleRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public HrService(QualificationEmployeeRepository employeeRepository,
                     EmployeeEducationRepository educationRepository,
                     EmployeeWorkExperienceRepository workExperienceRepository,
                     EmergencyContactRepository emergencyContactRepository,
                     EmployeeLifecycleRecordRepository lifecycleRepository,
                     LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.educationRepository = educationRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    private QualificationEmployee findEmployee(UUID id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new BusinessException("员工不存在: " + id));
    }

    // ====== Education ======
    @Transactional(readOnly = true)
    public List<EducationResponse> listEducations(UUID employeeId) {
        return educationRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
            .stream().map(this::toEducationResponse).toList();
    }

    @Transactional
    public EducationResponse createEducation(UUID employeeId, EducationRequest req) {
        var emp = findEmployee(employeeId);
        var entity = new EmployeeEducation();
        entity.setEmployee(emp);
        applyEducation(entity, req);
        return toEducationResponse(educationRepository.save(entity));
    }

    @Transactional
    public EducationResponse updateEducation(UUID id, EducationRequest req) {
        var entity = educationRepository.findById(id)
            .orElseThrow(() -> new BusinessException("教育经历不存在"));
        applyEducation(entity, req);
        return toEducationResponse(educationRepository.save(entity));
    }

    @Transactional
    public void deleteEducation(UUID id) {
        educationRepository.deleteById(id);
    }

    private void applyEducation(EmployeeEducation e, EducationRequest r) {
        e.setSchoolName(r.schoolName()); e.setDegree(r.degree()); e.setMajor(r.major());
        e.setStartDate(r.startDate()); e.setEndDate(r.endDate());
        e.setHighest(r.highest()); e.setRemark(r.remark());
    }

    private EducationResponse toEducationResponse(EmployeeEducation e) {
        return new EducationResponse(e.getId(), e.getEmployee().getId(), e.getSchoolName(),
            e.getDegree(), e.getMajor(), e.getStartDate(), e.getEndDate(), e.isHighest(), e.getRemark());
    }

    // ====== Work Experience ======
    @Transactional(readOnly = true)
    public List<WorkExperienceResponse> listWorkExperiences(UUID employeeId) {
        return workExperienceRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
            .stream().map(this::toWorkExperienceResponse).toList();
    }

    @Transactional
    public WorkExperienceResponse createWorkExperience(UUID employeeId, WorkExperienceRequest req) {
        var emp = findEmployee(employeeId);
        var entity = new EmployeeWorkExperience();
        entity.setEmployee(emp);
        applyWorkExperience(entity, req);
        return toWorkExperienceResponse(workExperienceRepository.save(entity));
    }

    @Transactional
    public WorkExperienceResponse updateWorkExperience(UUID id, WorkExperienceRequest req) {
        var entity = workExperienceRepository.findById(id)
            .orElseThrow(() -> new BusinessException("工作经历不存在"));
        applyWorkExperience(entity, req);
        return toWorkExperienceResponse(workExperienceRepository.save(entity));
    }

    @Transactional
    public void deleteWorkExperience(UUID id) {
        workExperienceRepository.deleteById(id);
    }

    private void applyWorkExperience(EmployeeWorkExperience e, WorkExperienceRequest r) {
        e.setCompanyName(r.companyName()); e.setPosition(r.position());
        e.setStartDate(r.startDate()); e.setEndDate(r.endDate());
        e.setCurrent(r.current()); e.setDescription(r.description()); e.setRemark(r.remark());
    }

    private WorkExperienceResponse toWorkExperienceResponse(EmployeeWorkExperience e) {
        return new WorkExperienceResponse(e.getId(), e.getEmployee().getId(), e.getCompanyName(),
            e.getPosition(), e.getStartDate(), e.getEndDate(), e.isCurrent(),
            e.getDescription(), e.getRemark());
    }

    // ====== Emergency Contact ======
    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> listEmergencyContacts(UUID employeeId) {
        return emergencyContactRepository.findByEmployeeId(employeeId)
            .stream().map(this::toEmergencyContactResponse).toList();
    }

    @Transactional
    public EmergencyContactResponse createEmergencyContact(UUID employeeId, EmergencyContactRequest req) {
        var emp = findEmployee(employeeId);
        var entity = new EmergencyContact();
        entity.setEmployee(emp);
        applyEmergencyContact(entity, req);
        return toEmergencyContactResponse(emergencyContactRepository.save(entity));
    }

    @Transactional
    public EmergencyContactResponse updateEmergencyContact(UUID id, EmergencyContactRequest req) {
        var entity = emergencyContactRepository.findById(id)
            .orElseThrow(() -> new BusinessException("紧急联系人不存在"));
        applyEmergencyContact(entity, req);
        return toEmergencyContactResponse(emergencyContactRepository.save(entity));
    }

    @Transactional
    public void deleteEmergencyContact(UUID id) {
        emergencyContactRepository.deleteById(id);
    }

    private void applyEmergencyContact(EmergencyContact e, EmergencyContactRequest r) {
        e.setName(r.name()); e.setRelationship(r.relationship()); e.setPhone(r.phone());
        e.setAddress(r.address()); e.setPrimary(r.primary()); e.setRemark(r.remark());
    }

    private EmergencyContactResponse toEmergencyContactResponse(EmergencyContact e) {
        return new EmergencyContactResponse(e.getId(), e.getEmployee().getId(), e.getName(),
            e.getRelationship(), e.getPhone(), e.getAddress(), e.isPrimary(), e.getRemark());
    }

    // ====== Employee Lifecycle ======
    @Transactional(readOnly = true)
    public List<LifecycleResponse> listLifecycles(UUID employeeId) {
        return lifecycleRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
            .stream().map(this::toLifecycleResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LifecycleResponse> listAllLifecycles() {
        return lifecycleRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toLifecycleResponse).toList();
    }

    @Transactional
    public LifecycleResponse createLifecycle(UUID employeeId, LifecycleRequest req) {
        var emp = findEmployee(employeeId);
        var entity = new EmployeeLifecycleRecord();
        entity.setEmployee(emp);
        applyLifecycle(entity, req);
        entity.setStatus("PENDING");
        return toLifecycleResponse(lifecycleRepository.save(entity));
    }

    @Transactional
    public LifecycleResponse approveLifecycle(UUID id, ApproveRequest req) {
        var entity = lifecycleRepository.findById(id)
            .orElseThrow(() -> new BusinessException("流程记录不存在"));
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException("该记录已处理，不能重复审批");
        }
        if (req.approved()) {
            entity.setStatus("APPROVED");
            // Update the employee record based on lifecycle type
            var emp = entity.getEmployee();
            if ("TRANSFER".equals(entity.getLifecycleType())) {
                if (entity.getToOrganizationId() != null) {
                    try { emp.setOrganization(new com.company.ops.api.modules.system.domain.SystemOrganization());
                        emp.getOrganization().setId(UUID.fromString(entity.getToOrganizationId())); } catch (Exception ignored) {}
                }
                if (entity.getToPosition() != null) emp.setPosition(entity.getToPosition());
            } else if ("RESIGNATION".equals(entity.getLifecycleType())) {
                emp.setEmploymentStatus("LEFT");
            } else if ("ONBOARDING".equals(entity.getLifecycleType())) {
                emp.setEmploymentStatus("ACTIVE");
                if (entity.getEffectiveDate() != null) emp.setEntryDate(entity.getEffectiveDate());
                if (entity.getToPosition() != null) emp.setPosition(entity.getToPosition());
            }
            employeeRepository.save(emp);
        } else {
            entity.setStatus("REJECTED");
        }
        entity.setApprovedBy(req.operatorName());
        entity.setApprovedAt(LocalDate.now());
        entity.setRemark(req.remark());
        return toLifecycleResponse(lifecycleRepository.save(entity));
    }

    private void applyLifecycle(EmployeeLifecycleRecord r, LifecycleRequest req) {
        r.setLifecycleType(req.lifecycleType());
        r.setEffectiveDate(req.effectiveDate());
        r.setFromOrganizationId(req.fromOrganizationId());
        r.setFromName(req.fromOrganizationName());
        r.setFromPosition(req.fromPosition());
        r.setToOrganizationId(req.toOrganizationId());
        r.setToName(req.toOrganizationName());
        r.setToPosition(req.toPosition());
        r.setReason(req.reason());
        r.setRemark(req.remark());
    }

    private LifecycleResponse toLifecycleResponse(EmployeeLifecycleRecord r) {
        return new LifecycleResponse(r.getId(), r.getEmployee().getId(), r.getEmployee().getName(),
            r.getLifecycleType(), r.getEffectiveDate(),
            r.getFromOrganizationId(), r.getFromName(), r.getFromPosition(),
            r.getToOrganizationId(), r.getToName(), r.getToPosition(),
            r.getReason(), r.getStatus(), r.getApprovedBy(), r.getApprovedAt(), r.getRemark());
    }

    // ====== Leave Request ======
    @Transactional(readOnly = true)
    public List<LeaveResponse> listLeaves(UUID employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
            .stream().map(this::toLeaveResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> listAllLeaves() {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toLeaveResponse).toList();
    }

    @Transactional
    public LeaveResponse createLeave(UUID employeeId, LeaveRequestData req) {
        var emp = findEmployee(employeeId);
        var entity = new LeaveRequest();
        entity.setEmployee(emp);
        entity.setLeaveType(req.leaveType());
        entity.setStartDate(req.startDate());
        entity.setEndDate(req.endDate());
        entity.setTotalDays(req.totalDays());
        entity.setReason(req.reason());
        entity.setStatus("PENDING");
        return toLeaveResponse(leaveRequestRepository.save(entity));
    }

    @Transactional
    public LeaveResponse approveLeave(UUID id, ApproveRequest req) {
        var entity = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new BusinessException("请假申请不存在"));
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        entity.setStatus(req.approved() ? "APPROVED" : "REJECTED");
        entity.setApprovedBy(req.operatorName());
        entity.setApprovedAt(java.time.LocalDateTime.now());
        entity.setApprovalRemark(req.remark());
        return toLeaveResponse(leaveRequestRepository.save(entity));
    }

    private LeaveResponse toLeaveResponse(LeaveRequest r) {
        return new LeaveResponse(r.getId(), r.getEmployee().getId(), r.getEmployee().getName(),
            r.getLeaveType(), r.getStartDate(), r.getEndDate(), r.getTotalDays(),
            r.getReason(), r.getStatus(), r.getApprovedBy(), r.getApprovedAt(), r.getApprovalRemark());
    }

    // ====== Analytics ======
    @Transactional(readOnly = true)
    public HrAnalyticsResponse analytics() {
        var allEmployees = employeeRepository.findAll();
        long total = allEmployees.size();
        long active = allEmployees.stream().filter(e -> "ACTIVE".equals(e.getEmploymentStatus())).count();
        long left = allEmployees.stream().filter(e -> "LEFT".equals(e.getEmploymentStatus())).count();
        long newThisMonth = allEmployees.stream().filter(e -> {
            if (e.getEntryDate() == null) return false;
            var now = LocalDate.now();
            return e.getEntryDate().getYear() == now.getYear() && e.getEntryDate().getMonth() == now.getMonth();
        }).count();
        long leavePending = leaveRequestRepository.countByStatus("PENDING");

        // Education distribution (from highest education records)
        var eduDist = new java.util.LinkedHashMap<String, Long>();
        var allEdu = educationRepository.findAll();
        for (var e : allEdu) {
            if (e.isHighest() && e.getDegree() != null && !e.getDegree().isBlank()) {
                eduDist.merge(e.getDegree(), 1L, Long::sum);
            }
        }
        var eduList = eduDist.entrySet().stream()
            .map(e -> new CategoryCount(e.getKey(), e.getValue())).toList();

        // Status distribution
        var statusDist = new LinkedHashMap<String, Long>();
        for (var e : allEmployees) {
            String s = switch (e.getEmploymentStatus()) {
                case "ACTIVE" -> "在职"; case "LEFT" -> "离职"; case "DISABLED" -> "停用";
                default -> e.getEmploymentStatus();
            };
            statusDist.merge(s, 1L, Long::sum);
        }
        var statusList = statusDist.entrySet().stream()
            .map(e -> new CategoryCount(e.getKey(), e.getValue())).toList();

        // Organization distribution
        var orgDist = new LinkedHashMap<String, Long>();
        for (var e : allEmployees) {
            String orgName = "未分配";
            if (e.getOrganization() != null && e.getOrganization().getName() != null) {
                orgName = e.getOrganization().getName();
            }
            orgDist.merge(orgName, 1L, Long::sum);
        }
        var orgList = orgDist.entrySet().stream()
            .map(e -> new CategoryCount(e.getKey(), e.getValue())).toList();

        // Recent lifecycles
        var recentLifecycles = lifecycleRepository.findAllByOrderByCreatedAtDesc().stream()
            .limit(10)
            .map(l -> {
                String typeLabel = switch (l.getLifecycleType()) {
                    case "ONBOARDING" -> "入职"; case "TRANSFER" -> "调岗"; case "RESIGNATION" -> "离职";
                    default -> l.getLifecycleType();
                };
                String detail = l.getFromName() != null ? l.getFromName() + " → " + l.getToName() : l.getReason();
                return new LifecycleSummary(l.getEffectiveDate().toString(), l.getEmployee().getName(), typeLabel, detail);
            }).toList();

        return new HrAnalyticsResponse(total, active, left, newThisMonth, leavePending,
            eduList, statusList, orgList, recentLifecycles);
    }
}
