package com.company.ops.api.modules.qualification.service;

import com.company.ops.api.common.delete.DeleteGovernanceService;
import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.qualification.domain.CompanyQualification;
import com.company.ops.api.modules.qualification.domain.EmployeeContract;
import com.company.ops.api.modules.qualification.domain.PersonnelCertificate;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.domain.QualificationPerformance;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.CategoryCount;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.CompanyQualificationRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.CompanyQualificationResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.DashboardResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeDetailResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeAccountResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeContractRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeContractResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeOption;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.EmployeeResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.OrganizationOption;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PerformanceRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PerformanceResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PersonnelCertificateRequest;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.PersonnelCertificateResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.ReferenceDataResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.TenderEmployeeResponse;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.WarningResponse;
import com.company.ops.api.modules.qualification.repository.CompanyQualificationRepository;
import com.company.ops.api.modules.qualification.repository.EmployeeContractRepository;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.qualification.repository.QualificationPerformanceRepository;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QualificationService {
  private static final int WARNING_DAYS = 180;
  private CodeGenerator codeGenerator;
  private final QualificationEmployeeRepository employeeRepository;
  private final CompanyQualificationRepository companyRepository;
  private final PersonnelCertificateRepository certificateRepository;
  private final QualificationPerformanceRepository performanceRepository;
  private final EmployeeContractRepository contractRepository;
  private final SystemUserRepository systemUserRepository;
  private final SystemOrganizationRepository organizationRepository;
  private final ObjectMapper objectMapper;
  private final DeleteGovernanceService deleteGovernanceService;

  public QualificationService(QualificationEmployeeRepository employeeRepository,
                              CompanyQualificationRepository companyRepository,
                              PersonnelCertificateRepository certificateRepository,
                              QualificationPerformanceRepository performanceRepository,
                              EmployeeContractRepository contractRepository,
                              SystemUserRepository systemUserRepository,
                              SystemOrganizationRepository organizationRepository,
                              ObjectMapper objectMapper,
                              CodeGenerator codeGenerator,
                              DeleteGovernanceService deleteGovernanceService) {
    this.codeGenerator = codeGenerator;
    this.employeeRepository = employeeRepository;
    this.companyRepository = companyRepository;
    this.certificateRepository = certificateRepository;
    this.performanceRepository = performanceRepository;
    this.contractRepository = contractRepository;
    this.systemUserRepository = systemUserRepository;
    this.organizationRepository = organizationRepository;
    this.objectMapper = objectMapper;
    this.deleteGovernanceService = deleteGovernanceService;
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard() {
    List<CompanyQualification> companies = companyRepository.findAllByOrderBySubjectCompanyAscNameAsc();
    List<PersonnelCertificate> certificates = certificateRepository.findAllByOrderByEmployeeNameAscNameAsc();
    List<WarningResponse> warnings = warnings();
    long available = certificates.stream().filter(this::isTenderAvailable).count();
    long expired = companies.stream().filter(item -> "EXPIRED".equals(companyStatus(item))).count()
        + certificates.stream().filter(item -> "EXPIRED".equals(certificateStatus(item))).count();
    return new DashboardResponse(
        companies.size(), employeeRepository.count(), certificates.size(), available,
        warnings.stream().filter(item -> !"HANDLED".equals(item.status())).count(), expired,
        counts(companies, CompanyQualification::getCategory),
        counts(certificates, PersonnelCertificate::getSpecialty), warnings.stream().limit(12).toList()
    );
  }

  @Transactional(readOnly = true)
  public ReferenceDataResponse references() {
    List<QualificationEmployee> employees = employeeRepository.findAllByOrderByNameAsc();
    List<CompanyQualification> companies = companyRepository.findAllByOrderBySubjectCompanyAscNameAsc();
    List<PersonnelCertificate> certificates = certificateRepository.findAllByOrderByEmployeeNameAscNameAsc();
    List<QualificationPerformance> performances = performanceRepository.findAllByOrderBySubjectCompanyAscNameAsc();
    List<SystemOrganization> organizations = organizationRepository
        .findByTenantIdOrderBySortOrderAsc(TenantContext.currentTenant());
    Set<String> subjects = new LinkedHashSet<>();
    companies.forEach(item -> add(subjects, item.getSubjectCompany()));
    organizations.stream()
        .filter(SystemOrganization::isEnabled)
        .filter(item -> item.getParent() == null || "COMPANY".equalsIgnoreCase(item.getType()))
        .forEach(item -> add(subjects, item.getName()));
    Set<String> qualificationCategories = new LinkedHashSet<>(List.of(
        "建筑业企业资质", "安全生产许可证", "质量管理体系认证",
        "环境管理体系认证", "职业健康安全管理体系认证", "其他"));
    qualificationCategories.addAll(distinct(companies, CompanyQualification::getCategory));
    return new ReferenceDataResponse(
        List.copyOf(subjects), List.copyOf(qualificationCategories),
        distinct(certificates, PersonnelCertificate::getType), distinct(certificates, PersonnelCertificate::getSpecialty),
        distinct(performances, QualificationPerformance::getProjectType),
        employees.stream().map(item -> new EmployeeOption(item.getId(), item.getName(), item.getWorkNo())).toList(),
        organizations.stream()
            .map(item -> new OrganizationOption(item.getId(), item.getName(), organizationPath(item), item.isEnabled()))
            .toList()
    );
  }

  @Transactional(readOnly = true)
  public List<CompanyQualificationResponse> listCompanies(String keyword, String subjectCompany, String status) {
    return deleteGovernanceService.visible("QUAL_COMPANY", companyRepository.findAllByOrderBySubjectCompanyAscNameAsc(), CompanyQualification::getId).stream()
        .filter(item -> matchesCompany(item, keyword, subjectCompany, status)).map(this::toCompany).toList();
  }

  @Transactional
  public CompanyQualificationResponse createCompany(CompanyQualificationRequest request) {
    CompanyQualification item = new CompanyQualification();
    item.setExternalId("MANUAL-CQ-" + UUID.randomUUID());
    applyCompany(item, request);
    return toCompany(companyRepository.save(item));
  }

  @Transactional
  public CompanyQualificationResponse updateCompany(UUID id, CompanyQualificationRequest request) {
    CompanyQualification item = companyRepository.findById(id).orElseThrow(() -> new BusinessException("公司资质不存在"));
    applyCompany(item, request);
    return toCompany(companyRepository.save(item));
  }

  @Transactional
  public void deleteCompany(UUID id) {
    CompanyQualification item = companyRepository.findById(id).orElseThrow(() -> new BusinessException("公司资质不存在"));
    if (!deleteGovernanceService.allowPhysicalDelete("QUAL_COMPANY", id, item.getName())) return;
    companyRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<EmployeeResponse> listEmployees(String keyword, String employmentStatus, UUID organizationId) {
    Map<UUID, List<PersonnelCertificate>> certificates = certificatesByEmployee();
    return deleteGovernanceService.visible("QUAL_EMPLOYEE", employeeRepository.findAllByOrderByNameAsc(), QualificationEmployee::getId).stream()
        .filter(item -> matchesEmployee(item, keyword, employmentStatus, organizationId))
        .map(item -> toEmployee(item, certificates.getOrDefault(item.getId(), List.of()))).toList();
  }

  @Transactional(readOnly = true)
  public EmployeeDetailResponse employeeDetail(UUID id) {
    QualificationEmployee item = employeeRepository.findById(id).orElseThrow(() -> new BusinessException("人员档案不存在"));
    if (deleteGovernanceService.isHidden("QUAL_EMPLOYEE", id)) throw new BusinessException("人员档案不存在");
    List<EmployeeContractResponse> contracts = deleteGovernanceService.visible("QUAL_EMPLOYEE_CONTRACT", contractRepository.findByEmployeeIdOrderByStartDateDesc(id), EmployeeContract::getId).stream()
        .map(this::toEmployeeContract).toList();
    List<PersonnelCertificate> certificates = deleteGovernanceService.visible("QUAL_CERTIFICATE", certificateRepository.findByEmployeeIdOrderByNameAsc(id), PersonnelCertificate::getId);
    return new EmployeeDetailResponse(toEmployee(item, certificates), contracts,
        certificates.stream().map(this::toCertificate).toList());
  }

  @Transactional
  public EmployeeResponse createEmployee(EmployeeRequest request) {
    QualificationEmployee item = new QualificationEmployee();
    String empCode = codeGenerator.generate("QUAL_EMPLOYEE");
    item.setWorkNo(empCode);
    item.setExternalId("MANUAL-EMP-" + UUID.randomUUID());
    applyEmployee(item, request);
    return toEmployee(employeeRepository.save(item), List.of());
  }

  @Transactional
  public EmployeeResponse updateEmployee(UUID id, EmployeeRequest request) {
    QualificationEmployee item = employeeRepository.findById(id).orElseThrow(() -> new BusinessException("人员档案不存在"));
    applyEmployee(item, request);
    return toEmployee(employeeRepository.save(item), certificateRepository.findByEmployeeIdOrderByNameAsc(id));
  }

  @Transactional
  public void deleteEmployee(UUID id) {
    QualificationEmployee item = employeeRepository.findById(id).orElseThrow(() -> new BusinessException("人员档案不存在"));
    if (!deleteGovernanceService.allowPhysicalDelete("QUAL_EMPLOYEE", id, item.getName())) return;
    employeeRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<EmployeeContractResponse> listEmployeeContracts(UUID employeeId) {
    if (!employeeRepository.existsById(employeeId)) throw new BusinessException("人员档案不存在");
    return deleteGovernanceService.visible("QUAL_EMPLOYEE_CONTRACT", contractRepository.findByEmployeeIdOrderByStartDateDesc(employeeId), EmployeeContract::getId)
        .stream().map(this::toEmployeeContract).toList();
  }

  @Transactional
  public EmployeeContractResponse createEmployeeContract(UUID employeeId, EmployeeContractRequest request) {
    String contractNo = request.contractNo() != null ? request.contractNo() : codeGenerator.generate("EMPLOYEE_CONTRACT");
    if (contractRepository.existsByContractNo(contractNo)) throw new BusinessException("合同编号已存在");
    EmployeeContract item = new EmployeeContract();
    item.setEmployee(employeeRepository.findById(employeeId).orElseThrow(() -> new BusinessException("人员档案不存在")));
    applyEmployeeContract(item, request);
    return toEmployeeContract(contractRepository.save(item));
  }

  @Transactional
  public EmployeeContractResponse updateEmployeeContract(UUID id, EmployeeContractRequest request) {
    if (contractRepository.existsByContractNoAndIdNot(request.contractNo(), id)) throw new BusinessException("合同编号已存在");
    EmployeeContract item = contractRepository.findById(id).orElseThrow(() -> new BusinessException("员工合同不存在"));
    applyEmployeeContract(item, request);
    return toEmployeeContract(contractRepository.save(item));
  }

  @Transactional
  public void deleteEmployeeContract(UUID id) {
    EmployeeContract item = contractRepository.findById(id).orElseThrow(() -> new BusinessException("员工合同不存在"));
    if (!deleteGovernanceService.allowPhysicalDelete("QUAL_EMPLOYEE_CONTRACT", id, item.getContractNo())) return;
    contractRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<PersonnelCertificateResponse> listCertificates(String keyword, String specialty, String status,
                                                              Boolean companyRegistered) {
    return deleteGovernanceService.visible("QUAL_CERTIFICATE", certificateRepository.findAllByOrderByEmployeeNameAscNameAsc(), PersonnelCertificate::getId).stream()
        .filter(item -> matchesCertificate(item, keyword, specialty, status, companyRegistered))
        .map(this::toCertificate).toList();
  }

  @Transactional
  public PersonnelCertificateResponse createCertificate(PersonnelCertificateRequest request) {
    PersonnelCertificate item = new PersonnelCertificate();
    item.setExternalId("MANUAL-CERT-" + UUID.randomUUID());
    applyCertificate(item, request);
    return toCertificate(certificateRepository.save(item));
  }

  @Transactional
  public PersonnelCertificateResponse updateCertificate(UUID id, PersonnelCertificateRequest request) {
    PersonnelCertificate item = certificateRepository.findById(id).orElseThrow(() -> new BusinessException("人员证书不存在"));
    applyCertificate(item, request);
    return toCertificate(certificateRepository.save(item));
  }

  @Transactional
  public void deleteCertificate(UUID id) {
    PersonnelCertificate item = certificateRepository.findById(id).orElseThrow(() -> new BusinessException("人员证书不存在"));
    if (!deleteGovernanceService.allowPhysicalDelete("QUAL_CERTIFICATE", id, item.getName())) return;
    certificateRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<PerformanceResponse> listPerformances(String keyword, String subjectCompany, String projectType) {
    return deleteGovernanceService.visible("QUAL_PERFORMANCE", performanceRepository.findAllByOrderBySubjectCompanyAscNameAsc(), QualificationPerformance::getId).stream()
        .filter(item -> contains(item.getName(), keyword) || contains(item.getClientName(), keyword)
            || contains(item.getContractNo(), keyword) || blank(keyword))
        .filter(item -> blank(subjectCompany) || subjectCompany.equals(item.getSubjectCompany()))
        .filter(item -> blank(projectType) || projectType.equals(item.getProjectType()))
        .map(this::toPerformance).toList();
  }

  @Transactional
  public PerformanceResponse createPerformance(PerformanceRequest request) {
    QualificationPerformance item = new QualificationPerformance();
    item.setExternalId("MANUAL-PERF-" + UUID.randomUUID());
    applyPerformance(item, request);
    return toPerformance(performanceRepository.save(item));
  }

  @Transactional
  public PerformanceResponse updatePerformance(UUID id, PerformanceRequest request) {
    QualificationPerformance item = performanceRepository.findById(id).orElseThrow(() -> new BusinessException("项目业绩不存在"));
    applyPerformance(item, request);
    return toPerformance(performanceRepository.save(item));
  }

  @Transactional
  public void deletePerformance(UUID id) {
    QualificationPerformance item = performanceRepository.findById(id).orElseThrow(() -> new BusinessException("项目业绩不存在"));
    if (!deleteGovernanceService.allowPhysicalDelete("QUAL_PERFORMANCE", id, item.getName())) return;
    performanceRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<TenderEmployeeResponse> tenderSearch(String keyword, List<String> specialties, boolean registeredOnly,
                                                    boolean availableOnly) {
    Map<UUID, List<PersonnelCertificate>> grouped = certificatesByEmployee();
    Set<String> required = specialties == null ? Set.of() : specialties.stream().filter(value -> !blank(value)).collect(Collectors.toSet());
    return employeeRepository.findAllByOrderByNameAsc().stream()
        .filter(item -> "ACTIVE".equalsIgnoreCase(item.getEmploymentStatus()))
        .filter(item -> blank(keyword) || contains(item.getName(), keyword) || contains(item.getWorkNo(), keyword))
        .map(item -> Map.entry(item, grouped.getOrDefault(item.getId(), List.of()).stream()
            .filter(certificate -> !registeredOnly || certificate.isCompanyRegistered())
            .filter(certificate -> !availableOnly || isTenderAvailable(certificate)).toList()))
        .filter(entry -> required.isEmpty() || entry.getValue().stream().map(PersonnelCertificate::getSpecialty).collect(Collectors.toSet()).containsAll(required))
        .filter(entry -> !entry.getValue().isEmpty())
        .map(entry -> new TenderEmployeeResponse(entry.getKey().getId(), entry.getKey().getName(),
            entry.getKey().getWorkNo(), entry.getKey().getDepartment(),
            entry.getKey().getPosition(), entry.getValue().stream().map(this::toCertificate).toList()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<WarningResponse> warnings() {
    List<WarningResponse> result = new ArrayList<>();
    companyRepository.findAllByOrderBySubjectCompanyAscNameAsc().forEach(item -> {
      addWarning(result, "COMPANY", item.getId(), item.getName(), "COMPANY_EXPIRE", "公司资质到期", item.getValidTo());
      addWarning(result, "COMPANY", item.getId(), item.getName(), "COMPANY_ANNUAL", "公司资质年审", item.getAnnualReviewDate());
      addWarning(result, "COMPANY", item.getId(), item.getName(), "COMPANY_RENEWAL", "公司资质延续", item.getRenewalDate());
    });
    certificateRepository.findAllByOrderByEmployeeNameAscNameAsc().forEach(item -> {
      addWarning(result, "CERTIFICATE", item.getId(), item.getEmployee().getName() + " · " + item.getName(),
          "CERTIFICATE_EXPIRE", "人员证书到期", item.getValidTo());
      addWarning(result, "CERTIFICATE", item.getId(), item.getEmployee().getName() + " · " + item.getName(),
          "CERTIFICATE_REVIEW", "人员证书复审", item.getReviewDate());
    });
    employeeRepository.findAllByOrderByNameAsc().forEach(item ->
        addWarning(result, "EMPLOYEE", item.getId(), item.getName(), "SOCIAL_SECURITY", "社保缴纳到期", item.getSocialSecurityEnd()));
    contractRepository.findAllByOrderByEndDateAsc().stream()
        .filter(item -> !"TERMINATED".equalsIgnoreCase(item.getStatus()))
        .forEach(item -> addWarning(result, "EMPLOYEE_CONTRACT", item.getId(),
            item.getEmployee().getName() + " · " + item.getContractNo(), "CONTRACT_EXPIRE", "员工合同到期", item.getEndDate()));
    return result.stream().sorted(Comparator.comparingLong(WarningResponse::daysLeft)).toList();
  }

  private void applyCompany(CompanyQualification item, CompanyQualificationRequest request) {
    item.setSubjectCompany(request.subjectCompany()); item.setName(request.name()); item.setCategory(request.category());
    item.setLevel(request.level()); item.setCertificateNo(request.certificateNo()); item.setIssuer(request.issuer());
    item.setIssueDate(request.issueDate()); item.setValidFrom(request.validFrom()); item.setValidTo(request.validTo());
    item.setAnnualReviewDate(request.annualReviewDate()); item.setRenewalDate(request.renewalDate()); item.setScope(request.scope());
    item.setProjectTypesJson(write(request.projectTypes())); item.setHolderBranch(request.holderBranch());
    item.setStorageLocation(request.storageLocation()); item.setAvailableForTender(request.availableForTender());
    item.setManualStatus(normalize(request.manualStatus(), "NORMAL")); item.setLocked(request.locked());
    item.setAttachmentsJson(write(request.attachments())); item.setRemark(request.remark());
  }

  private void applyEmployee(QualificationEmployee item, EmployeeRequest request) {
    item.setName(request.name()); item.setWorkNo(request.workNo());
    SystemUser systemUser = resolveSystemUser(request.systemUserId(), item.getId());
    SystemOrganization organization = resolveOrganization(request.organizationId(), systemUser);
    item.setOrganization(organization);
    item.setDepartment(organization == null ? request.department() : organization.getName());
    item.setPosition(request.position()); item.setIdCard(request.idCard());
    item.setPhone(request.phone()); item.setEntryDate(request.entryDate()); item.setEmploymentStatus(normalize(request.employmentStatus(), "ACTIVE"));
    item.setContractStart(request.contractStart()); item.setContractEnd(request.contractEnd());
    item.setSocialSecurityUnit(request.socialSecurityUnit()); item.setSocialSecurityStart(request.socialSecurityStart());
    item.setSocialSecurityEnd(request.socialSecurityEnd()); item.setRemark(request.remark());
    item.setSystemUser(systemUser);
    if (systemUser != null && organization != null) systemUser.setOrganization(organization);
  }

  private void applyEmployeeContract(EmployeeContract item, EmployeeContractRequest request) {
    if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
      throw new BusinessException("合同结束日期不能早于开始日期");
    }
    item.setContractNo(request.contractNo()); item.setContractType(request.contractType());
    item.setSignDate(request.signDate()); item.setStartDate(request.startDate()); item.setEndDate(request.endDate());
    item.setProbationEndDate(request.probationEndDate()); item.setStatus(normalize(request.status(), "ACTIVE"));
    item.setAttachmentsJson(write(request.attachments())); item.setRemark(request.remark());
  }

  private SystemUser resolveSystemUser(UUID systemUserId, UUID employeeId) {
    if (systemUserId == null) return null;
    employeeRepository.findBySystemUser_Id(systemUserId).filter(item -> !item.getId().equals(employeeId)).ifPresent(item -> {
      throw new BusinessException("该登录账号已关联其他员工");
    });
    return systemUserRepository.findById(systemUserId).orElseThrow(() -> new BusinessException("登录账号不存在"));
  }

  private SystemOrganization resolveOrganization(UUID organizationId, SystemUser systemUser) {
    if (organizationId != null) {
      SystemOrganization organization = organizationRepository.findById(organizationId)
          .orElseThrow(() -> new BusinessException("所属组织不存在"));
      if (!organization.isEnabled()) throw new BusinessException("所属组织已停用");
      return organization;
    }
    return systemUser == null ? null : systemUser.getOrganization();
  }

  private void applyCertificate(PersonnelCertificate item, PersonnelCertificateRequest request) {
    item.setEmployee(employeeRepository.findById(request.employeeId()).orElseThrow(() -> new BusinessException("人员档案不存在")));
    item.setName(request.name()); item.setType(request.type()); item.setCertificateNo(request.certificateNo());
    item.setSpecialty(request.specialty()); item.setCompanyRegistered(request.companyRegistered()); item.setIssueDate(request.issueDate());
    item.setValidTo(request.validTo()); item.setReviewDate(request.reviewDate()); item.setAvailableForTender(request.availableForTender());
    item.setManualStatus(normalize(request.manualStatus(), "NORMAL")); item.setLocked(request.locked());
    item.setAttachmentsJson(write(request.attachments())); item.setRemark(request.remark());
  }

  private void applyPerformance(QualificationPerformance item, PerformanceRequest request) {
    item.setSubjectCompany(request.subjectCompany()); item.setName(request.name()); item.setClientName(request.clientName());
    item.setContractNo(request.contractNo()); item.setContractDate(request.contractDate()); item.setContractAmount(request.contractAmount());
    item.setProjectType(request.projectType()); item.setAttachmentsJson(write(request.attachments())); item.setRemark(request.remark());
  }

  private CompanyQualificationResponse toCompany(CompanyQualification item) {
    LocalDate due = earliest(item.getValidTo(), item.getAnnualReviewDate(), item.getRenewalDate());
    return new CompanyQualificationResponse(item.getId(), item.getSubjectCompany(), item.getName(), item.getCategory(),
        item.getLevel(), item.getCertificateNo(), item.getIssuer(), item.getIssueDate(), item.getValidFrom(), item.getValidTo(),
        item.getAnnualReviewDate(), item.getRenewalDate(), item.getScope(), readStrings(item.getProjectTypesJson()),
        item.getHolderBranch(), item.getStorageLocation(), item.isAvailableForTender(), item.getManualStatus(), item.isLocked(),
        readAttachments(item.getAttachmentsJson()), item.getRemark(), companyStatus(item), days(due));
  }

  private EmployeeResponse toEmployee(QualificationEmployee item, List<PersonnelCertificate> certificates) {
    EmployeeAccountResponse account = toEmployeeAccount(item.getSystemUser());
    SystemOrganization organization = item.getOrganization();
    return new EmployeeResponse(item.getId(), item.getName(), item.getWorkNo(),
        organization == null ? null : organization.getId(), organization == null ? null : organization.getName(),
        organization == null ? null : organizationPath(organization), item.getDepartment(),
        item.getPosition(), item.getIdCard(), item.getPhone(), item.getEntryDate(), item.getEmploymentStatus(),
        item.getContractStart(), item.getContractEnd(), item.getSocialSecurityUnit(), item.getSocialSecurityStart(),
        item.getSocialSecurityEnd(), item.getRemark(), item.getSystemUser() == null ? null : item.getSystemUser().getId(), account,
        certificates.size(), certificates.stream().filter(this::isTenderAvailable).count());
  }

  private EmployeeAccountResponse toEmployeeAccount(SystemUser user) {
    if (user == null) return null;
    return new EmployeeAccountResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getPhone(), user.getEmail(),
        user.isEnabled(), user.getRoles().stream().map(role -> role.getName()).sorted().toList());
  }

  private EmployeeContractResponse toEmployeeContract(EmployeeContract item) {
    return new EmployeeContractResponse(item.getId(), item.getEmployee().getId(), item.getContractNo(), item.getContractType(),
        item.getSignDate(), item.getStartDate(), item.getEndDate(), item.getProbationEndDate(), employeeContractStatus(item),
        readAttachments(item.getAttachmentsJson()), item.getRemark(), days(item.getEndDate()));
  }

  private String employeeContractStatus(EmployeeContract item) {
    if ("TERMINATED".equalsIgnoreCase(item.getStatus()) || "DRAFT".equalsIgnoreCase(item.getStatus())) return item.getStatus().toUpperCase();
    return item.getEndDate() != null && item.getEndDate().isBefore(LocalDate.now()) ? "EXPIRED" : "ACTIVE";
  }

  private PersonnelCertificateResponse toCertificate(PersonnelCertificate item) {
    LocalDate due = earliest(item.getValidTo(), item.getReviewDate());
    return new PersonnelCertificateResponse(item.getId(), item.getEmployee().getId(), item.getEmployee().getName(),
        item.getName(), item.getType(), item.getCertificateNo(), item.getSpecialty(),
        item.isCompanyRegistered(), item.getIssueDate(), item.getValidTo(), item.getReviewDate(), item.isAvailableForTender(),
        item.getManualStatus(), item.isLocked(), readAttachments(item.getAttachmentsJson()), item.getRemark(),
        certificateStatus(item), days(due));
  }

  private PerformanceResponse toPerformance(QualificationPerformance item) {
    return new PerformanceResponse(item.getId(), item.getSubjectCompany(), item.getName(), item.getClientName(), item.getContractNo(),
        item.getContractDate(), item.getContractAmount(), item.getProjectType(), readAttachments(item.getAttachmentsJson()), item.getRemark());
  }

  private boolean matchesCompany(CompanyQualification item, String keyword, String subjectCompany, String status) {
    return (blank(keyword) || contains(item.getName(), keyword) || contains(item.getCertificateNo(), keyword) || contains(item.getCategory(), keyword))
        && (blank(subjectCompany) || subjectCompany.equals(item.getSubjectCompany()))
        && (blank(status) || status.equalsIgnoreCase(companyStatus(item)));
  }

  private boolean matchesEmployee(QualificationEmployee item, String keyword, String status, UUID organizationId) {
    return (blank(keyword) || contains(item.getName(), keyword) || contains(item.getWorkNo(), keyword)
        || contains(item.getDepartment(), keyword)
        || contains(item.getOrganization() == null ? null : item.getOrganization().getName(), keyword)
        || contains(item.getIdCard(), keyword))
        && (blank(status) || status.equalsIgnoreCase(item.getEmploymentStatus()))
        && (organizationId == null || (item.getOrganization() != null && organizationId.equals(item.getOrganization().getId())));
  }

  private String organizationPath(SystemOrganization organization) {
    List<String> names = new ArrayList<>();
    Set<UUID> visited = new LinkedHashSet<>();
    SystemOrganization current = organization;
    while (current != null && visited.add(current.getId())) {
      names.add(current.getName());
      current = current.getParent();
    }
    Collections.reverse(names);
    return String.join(" / ", names);
  }

  private boolean matchesCertificate(PersonnelCertificate item, String keyword, String specialty,
                                     String status, Boolean registered) {
    return (blank(keyword) || contains(item.getName(), keyword) || contains(item.getCertificateNo(), keyword)
        || contains(item.getEmployee().getName(), keyword) || contains(item.getSpecialty(), keyword))
        && (blank(specialty) || specialty.equals(item.getSpecialty()))
        && (blank(status) || status.equalsIgnoreCase(certificateStatus(item)))
        && (registered == null || registered == item.isCompanyRegistered());
  }

  private String companyStatus(CompanyQualification item) {
    if ("VOIDED".equalsIgnoreCase(item.getManualStatus())) return "VOIDED";
    if (item.isLocked()) return "LOCKED";
    return dateStatus(earliest(item.getValidTo(), item.getAnnualReviewDate(), item.getRenewalDate()));
  }

  private String certificateStatus(PersonnelCertificate item) {
    if ("VOIDED".equalsIgnoreCase(item.getManualStatus())) return "VOIDED";
    if (item.isLocked()) return "LOCKED";
    return dateStatus(earliest(item.getValidTo(), item.getReviewDate()));
  }

  private String dateStatus(LocalDate due) {
    if (due == null) return "UNVERIFIED";
    long days = ChronoUnit.DAYS.between(LocalDate.now(), due);
    if (days < 0) return "EXPIRED";
    if (days <= 90) return "EXPIRING";
    return "VALID";
  }

  private boolean isTenderAvailable(PersonnelCertificate item) {
    String status = certificateStatus(item);
    return item.isAvailableForTender() && !item.isLocked() && "ACTIVE".equalsIgnoreCase(item.getEmployee().getEmploymentStatus())
        && ("VALID".equals(status) || "EXPIRING".equals(status));
  }

  private void addWarning(List<WarningResponse> result, String sourceType, UUID sourceId, String sourceName,
                          String warningType, String title, LocalDate dueDate) {
    if (dueDate == null) return;
    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    if (daysLeft > WARNING_DAYS) return;
    result.add(new WarningResponse(sourceType, sourceId, sourceName, warningType, title, dueDate, daysLeft,
        daysLeft < 0 ? "DANGER" : daysLeft <= 30 ? "DANGER" : daysLeft <= 90 ? "WARNING" : "INFO",
        daysLeft < 0 ? "OVERDUE" : "PENDING"));
  }

  private Map<UUID, List<PersonnelCertificate>> certificatesByEmployee() {
    return certificateRepository.findAllByOrderByEmployeeNameAscNameAsc().stream()
        .collect(Collectors.groupingBy(item -> item.getEmployee().getId(), LinkedHashMap::new, Collectors.toList()));
  }

  private <T> List<CategoryCount> counts(List<T> items, Function<T, String> classifier) {
    return items.stream().map(classifier).filter(value -> !blank(value))
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .map(entry -> new CategoryCount(entry.getKey(), entry.getValue())).toList();
  }

  private <T> List<String> distinct(List<T> items, Function<T, String> mapper) {
    return items.stream().map(mapper).filter(value -> !blank(value)).distinct().sorted().toList();
  }

  private List<Attachment> readAttachments(String json) {
    if (blank(json)) return List.of();
    try { return objectMapper.readValue(json, new TypeReference<>() {}); }
    catch (JsonProcessingException exception) { return List.of(); }
  }

  private List<String> readStrings(String json) {
    if (blank(json)) return List.of();
    try { return objectMapper.readValue(json, new TypeReference<>() {}); }
    catch (JsonProcessingException exception) { return List.of(); }
  }

  private String write(Object value) {
    if (value == null) return "[]";
    try { return objectMapper.writeValueAsString(value); }
    catch (JsonProcessingException exception) { throw new BusinessException("附件或分类数据格式不正确"); }
  }

  private LocalDate earliest(LocalDate... dates) {
    LocalDate earliest = null;
    for (LocalDate date : dates) if (date != null && (earliest == null || date.isBefore(earliest))) earliest = date;
    return earliest;
  }
  private Long days(LocalDate date) { return date == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), date); }
  private boolean blank(String value) { return value == null || value.isBlank(); }
  private boolean contains(String value, String keyword) { return !blank(value) && !blank(keyword) && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)); }
  private void add(Set<String> values, String value) { if (!blank(value)) values.add(value); }
  private String normalize(String value, String fallback) { return blank(value) ? fallback : value.toUpperCase(Locale.ROOT); }
}
