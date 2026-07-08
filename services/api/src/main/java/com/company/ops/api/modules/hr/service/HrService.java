package com.company.ops.api.modules.hr.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.hr.domain.EmergencyContact;
import com.company.ops.api.modules.hr.domain.EmployeeEducation;
import com.company.ops.api.modules.hr.domain.EmployeeLifecycleRecord;
import com.company.ops.api.modules.hr.domain.EmployeeWorkExperience;
import com.company.ops.api.modules.hr.domain.LeaveBalance;
import com.company.ops.api.modules.hr.domain.LeaveRequest;
import com.company.ops.api.modules.hr.dto.HrDtos.*;
import com.company.ops.api.modules.hr.repository.EmergencyContactRepository;
import com.company.ops.api.modules.hr.repository.EmployeeEducationRepository;
import com.company.ops.api.modules.hr.repository.EmployeeLifecycleRecordRepository;
import com.company.ops.api.modules.hr.repository.EmployeeWorkExperienceRepository;
import com.company.ops.api.modules.hr.repository.LeaveBalanceRepository;
import com.company.ops.api.modules.hr.repository.LeaveRequestRepository;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.domain.PersonnelCertificate;
import com.company.ops.api.modules.qualification.domain.EmployeeContract;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeAccountResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeDetailResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeContractResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PersonnelCertificateResponse;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.EmployeeContractRepository;

import com.company.ops.api.modules.crm.domain.Receivable;
import com.company.ops.api.modules.crm.domain.ReceivableStatus;
import com.company.ops.api.modules.crm.repository.ReceivableRepository;
import com.company.ops.api.modules.office.domain.ApprovalRequest;
import com.company.ops.api.modules.office.repository.ApprovalRequestRepository;
import com.company.ops.api.modules.maintenance.domain.WorkOrder;
import com.company.ops.api.modules.maintenance.repository.WorkOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final PersonnelCertificateRepository personnelCertificateRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ReceivableRepository receivableRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final ObjectMapper objectMapper;

    public HrService(QualificationEmployeeRepository employeeRepository,
                     EmployeeEducationRepository educationRepository,
                     EmployeeWorkExperienceRepository workExperienceRepository,
                     EmergencyContactRepository emergencyContactRepository,
                     EmployeeLifecycleRecordRepository lifecycleRepository,
                     LeaveRequestRepository leaveRequestRepository,
                     LeaveBalanceRepository leaveBalanceRepository,
                     ObjectMapper objectMapper,
                     PersonnelCertificateRepository personnelCertificateRepository,
                     EmployeeContractRepository employeeContractRepository,
                     ApprovalRequestRepository approvalRequestRepository,
                     WorkOrderRepository workOrderRepository,
                     ReceivableRepository receivableRepository) {
        this.employeeRepository = employeeRepository;
        this.educationRepository = educationRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.objectMapper = objectMapper;
        this.personnelCertificateRepository = personnelCertificateRepository;
        this.employeeContractRepository = employeeContractRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.workOrderRepository = workOrderRepository;
        this.receivableRepository = receivableRepository;
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
        var saved = leaveRequestRepository.save(entity);
        // Deduct leave balance on approval
        if (req.approved() && saved.getEmployee() != null) {
            try { deductLeaveBalance(saved.getEmployee(), saved.getLeaveType(), saved.getTotalDays()); } catch (Exception ignored) {}
        }
        return toLeaveResponse(saved);
    }

    private LeaveResponse toLeaveResponse(LeaveRequest r) {
        return new LeaveResponse(r.getId(), r.getEmployee().getId(), r.getEmployee().getName(),
            r.getLeaveType(), r.getStartDate(), r.getEndDate(), r.getTotalDays(),
            r.getReason(), r.getStatus(), r.getApprovedBy(), r.getApprovedAt(), r.getApprovalRemark());
    }



    // ====== Self Service helpers ======

    private EmployeeResponse toEmployeeResponse(QualificationEmployee emp) {
        java.util.UUID orgId = emp.getOrganization() != null ? emp.getOrganization().getId() : null;
        String orgName = emp.getOrganization() != null ? emp.getOrganization().getName() : null;
        String orgPath = emp.getOrganization() != null ? emp.getOrganization().getName() : null;
        EmployeeAccountResponse account = null;
        if (emp.getSystemUser() != null) {
            try {
                var u = emp.getSystemUser();
                account = new EmployeeAccountResponse(u.getId(), u.getUsername(), u.getDisplayName(),
                    u.getPhone(), u.getEmail(), u.isEnabled(),
                    u.getRoles().stream().map(r -> r.getCode()).toList());
            } catch (Exception ignored) {}
        }
        long certCount = 0, validCertCount = 0;
        try { certCount = personnelCertificateRepository.findByEmployeeIdOrderByNameAsc(emp.getId()).size(); } catch (Exception ignored) {}
        return new EmployeeResponse(emp.getId(), emp.getName(), emp.getWorkNo(), orgId, orgName, orgPath,
            emp.getDepartment(), emp.getPosition(), emp.getIdCard(), emp.getPhone(), emp.getEntryDate(),
            emp.getEmploymentStatus(), emp.getContractStart(), emp.getContractEnd(),
            emp.getSocialSecurityUnit(), emp.getSocialSecurityStart(), emp.getSocialSecurityEnd(),
            emp.getRemark(), emp.getSystemUser() != null ? emp.getSystemUser().getId() : null, account,
            certCount, validCertCount);
    }

    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeDetail(UUID employeeId) {
        var emp = findEmployee(employeeId);
        var contracts = employeeContractRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
            .stream().map(this::toContractResponse).toList();
        var certs = personnelCertificateRepository.findByEmployeeIdOrderByNameAsc(employeeId)
            .stream().map(this::toCertResponse).toList();
        var empResp = toEmployeeResponse(emp);
        return new EmployeeDetailResponse(empResp, contracts, certs);
    }
        
    // ====== Self-service certificate & contract helpers ======
    private PersonnelCertificateResponse toCertResponse(PersonnelCertificate c) {
        String status; Long daysLeft = null;
        if (c.getValidTo() != null) {
            daysLeft = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), c.getValidTo());
            if (daysLeft < 0) status = "EXPIRED";
            else if (daysLeft <= 180) status = "EXPIRING";
            else status = "VALID";
        } else status = "VALID";
        List<Attachment> atts = new java.util.ArrayList<>();
        try { var arr = objectMapper.readValue(c.getAttachmentsJson(), Attachment[].class); atts = java.util.Arrays.asList(arr); } catch (Exception ignored) {}
        return new PersonnelCertificateResponse(c.getId(), c.getEmployee().getId(), c.getEmployee().getName(),
            c.getName(), c.getType(), c.getCertificateNo(), c.getSpecialty(), c.isCompanyRegistered(),
            c.getIssueDate(), c.getValidTo(), c.getReviewDate(), c.isAvailableForTender(), c.getManualStatus(),
            c.isLocked(), atts, c.getRemark(), status, daysLeft);
    }

    private EmployeeContractResponse toContractResponse(EmployeeContract c) {
        Long daysLeft = c.getEndDate() != null ? java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), c.getEndDate()) : null;
        List<Attachment> atts = new java.util.ArrayList<>();
        try { var arr = objectMapper.readValue(c.getAttachmentsJson(), Attachment[].class); atts = java.util.Arrays.asList(arr); } catch (Exception ignored) {}
        return new EmployeeContractResponse(c.getId(), c.getEmployee().getId(), c.getContractNo(),
            c.getContractType(), c.getSignDate(), c.getStartDate(), c.getEndDate(), c.getProbationEndDate(),
            c.getStatus(), atts, c.getRemark(), daysLeft);
    }

    @Transactional(readOnly = true)
    public List<PersonnelCertificateResponse> listEmployeeCertificates(UUID employeeId) {
        return personnelCertificateRepository.findByEmployeeIdOrderByNameAsc(employeeId)
            .stream().map(this::toCertResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeContractResponse> listEmployeeContracts(UUID employeeId) {
        return employeeContractRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
            .stream().map(this::toContractResponse).toList();
    }

    
    // ====== Self-service todos ======
    @Transactional(readOnly = true)
    public java.util.List<com.company.ops.api.modules.hr.dto.HrDtos.TodoItem> listEmployeeTodos(UUID employeeId) {
        var items = new java.util.ArrayList<com.company.ops.api.modules.hr.dto.HrDtos.TodoItem>();
        var now = java.time.LocalDate.now();
        // Debug: always add a test item to verify the feature works
        try { throw new RuntimeException(); } catch (Exception e) { items.add(new com.company.ops.api.modules.hr.dto.HrDtos.TodoItem("DEBUG", "DEBUG: 待办功能正常", "如果看到这个说明待办系统在运行", "/self", "HIGH", now.toString())); }

        // 1. My pending leave requests
        var pendingLeaves = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
            .stream().filter(l -> "PENDING".equals(l.getStatus())).toList();
        for (var l : pendingLeaves) {
            items.add(new com.company.ops.api.modules.hr.dto.HrDtos.TodoItem(
                "LEAVE_PENDING", "请假待审批",
                l.getLeaveType() + " " + l.getStartDate() + " ~ " + l.getEndDate() + " 共 " + String.format("%.1f", l.getTotalDays()) + "天",
                "/self/leaves", "MEDIUM", l.getStartDate().toString()));
        }

        // 2. Certificates expiring within 30 days
        var certs = personnelCertificateRepository.findByEmployeeIdOrderByNameAsc(employeeId);
        for (var c : certs) {
            if (c.getValidTo() != null) {
                var daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, c.getValidTo());
                if (daysLeft >= 0 && daysLeft <= 30) {
                    items.add(new com.company.ops.api.modules.hr.dto.HrDtos.TodoItem(
                        "CERT_EXPIRING", "证书即将到期",
                        c.getName() + " (" + c.getCertificateNo() + ") 剩余" + daysLeft + "天",
                        "/self/profile", "HIGH", c.getValidTo().toString()));
                } else if (daysLeft < 0) {
                    items.add(new com.company.ops.api.modules.hr.dto.HrDtos.TodoItem(
                        "CERT_EXPIRED", "证书已过期",
                        c.getName() + " (" + c.getCertificateNo() + ") 已过期" + Math.abs(daysLeft) + "天",
                        "/self/profile", "HIGH", c.getValidTo().toString()));
                }
            }
        }

        // 3. Contracts ending within 30 days
        var contracts = employeeContractRepository.findByEmployeeIdOrderByStartDateDesc(employeeId);
        for (var c : contracts) {
            if (c.getEndDate() != null) {
                var daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, c.getEndDate());
                if (daysLeft >= 0 && daysLeft <= 30) {
                    items.add(new com.company.ops.api.modules.hr.dto.HrDtos.TodoItem(
                        "CONTRACT_ENDING", "合同即将到期",
                        c.getContractNo() + " " + java.time.temporal.ChronoUnit.DAYS.between(now, c.getEndDate()) + "天后到期",
                        "/self/profile", "HIGH", c.getEndDate().toString()));
                }
            }
        }

        // 4. Pending office approvals (if any)
        try {
            var pendingApprovals = approvalRequestRepository.findAll().stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .limit(10).toList();
            for (var a : pendingApprovals) {
                items.add(new com.company.ops.api.modules.hr.dto.HrDtos.TodoItem(
                    "APPROVAL_PENDING", a.getTitle() != null ? a.getTitle() : "待审批事项",
                    a.getContent() != null ? a.getContent() : "",
                    "/office/approvals", "MEDIUM", a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate().toString() : now.toString()));
            }
        } catch (Exception ignored) {}

        // 5. Work orders assigned to me that are still open
        try {
            var allWorkOrders = workOrderRepository.findAll();
            var workOrders = allWorkOrders.stream()
                .filter(wo -> wo.getAssigneeId() != null && wo.getAssigneeId().equals(employeeId))
                .filter(wo -> !"ACCEPTED".equals(wo.getStatus()) && !"CANCELLED".equals(wo.getStatus()))
                .toList();
            for (var wo : workOrders) {
                if (!"ACCEPTED".equals(wo.getStatus()) && !"CANCELLED".equals(wo.getStatus())) {
                    items.add(new com.company.ops.api.modules.hr.dto.HrDtos.TodoItem(
                        "WORK_ORDER", "待处理工单: " + wo.getCode(),
                        wo.getTitle() != null ? wo.getTitle() : "",
                        "/maintenance/work-orders", "MEDIUM", now.toString()));
                }
            }
        } catch (Exception ignored) {}

        // Sort by priority
        items.sort((a, b) -> {
            var p = java.util.Map.of("HIGH", 0, "MEDIUM", 1, "LOW", 2);
            return Integer.compare(p.getOrDefault(a.priority(), 2), p.getOrDefault(b.priority(), 2));
        });

        return items;
    }
        // ====== Leave Balance ======
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> listBalances(UUID employeeId) {
        return leaveBalanceRepository.findByEmployeeIdOrderByLeaveTypeAsc(employeeId)
            .stream().map(this::toBalanceResponse).toList();
    }

    @Transactional
    public LeaveBalanceResponse setBalance(UUID employeeId, LeaveBalanceRequest req) {
        var emp = findEmployee(employeeId);
        var existing = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(employeeId, req.leaveType(), req.year());
        LeaveBalance entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setTotalDays(req.totalDays());
            entity.setUsedDays(req.usedDays());
        } else {
            entity = new LeaveBalance();
            entity.setEmployee(emp);
            entity.setLeaveType(req.leaveType());
            entity.setYear(req.year());
            entity.setTotalDays(req.totalDays());
            entity.setUsedDays(req.usedDays());
        }
        return toBalanceResponse(leaveBalanceRepository.save(entity));
    }

    @Transactional
    public void batchInitBalances(UUID employeeId) {
        int year = LocalDate.now().getYear();
        // Auto-create default balances: annual 15 days, sick 12 days
        var existingTypes = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
            .stream().map(LeaveBalance::getLeaveType).toList();
        if (!existingTypes.contains("ANNUAL")) {
            var bal = new LeaveBalance();
            bal.setEmployee(findEmployee(employeeId));
            bal.setLeaveType("ANNUAL"); bal.setYear(year); bal.setTotalDays(15);
            leaveBalanceRepository.save(bal);
        }
        if (!existingTypes.contains("SICK")) {
            var bal = new LeaveBalance();
            bal.setEmployee(findEmployee(employeeId));
            bal.setLeaveType("SICK"); bal.setYear(year); bal.setTotalDays(12);
            leaveBalanceRepository.save(bal);
        }
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> listAllBalances() {
        return leaveBalanceRepository.findAll().stream()
            .map(this::toBalanceResponse).toList();
    }

    private void deductLeaveBalance(QualificationEmployee emp, String leaveType, double days) {
        int year = LocalDate.now().getYear();
        var balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(emp.getId(), leaveType, year);
        balance.ifPresent(b -> {
            b.setUsedDays(b.getUsedDays() + days);
            leaveBalanceRepository.save(b);
        });
    }

    private LeaveBalanceResponse toBalanceResponse(LeaveBalance b) {
        return new LeaveBalanceResponse(b.getId(), b.getEmployee().getId(), b.getEmployee().getName(),
            b.getLeaveType(), b.getYear(), b.getTotalDays(), b.getUsedDays(), b.getRemainingDays());
    }

        // ====== Analytics ======
    @Transactional(readOnly = true)
    public HrAnalyticsResponse analytics() {
        long total = employeeRepository.count();
        long active = employeeRepository.countByEmploymentStatus("ACTIVE");
        long left = employeeRepository.countByEmploymentStatus("LEFT");
        var now = LocalDate.now();
        long newThisMonth = employeeRepository.countByEntryDateBetween(
            now.withDayOfMonth(1), now);
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
        if (active > 0) statusDist.put("在职", active);
        if (left > 0) statusDist.put("离职", left);
        long disabled = total - active - left;
        if (disabled > 0) statusDist.put("停用", disabled);
        var statusList = statusDist.entrySet().stream()
            .map(e -> new CategoryCount(e.getKey(), e.getValue())).toList();

        // Organization distribution
        var orgDist = new LinkedHashMap<String, Long>();
        for (var e : employeeRepository.findAll()) {
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
