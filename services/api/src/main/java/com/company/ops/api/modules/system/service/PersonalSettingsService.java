package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.modules.qualification.domain.EmployeeContract;
import com.company.ops.api.modules.qualification.domain.PersonnelCertificate;
import com.company.ops.api.modules.qualification.domain.QualificationEmployee;
import com.company.ops.api.modules.qualification.dto.QualificationDtos.Attachment;
import com.company.ops.api.modules.qualification.repository.EmployeeContractRepository;
import com.company.ops.api.modules.qualification.repository.PersonnelCertificateRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.AccountProfile;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.ChangeMyPasswordRequest;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.EmployeeProfile;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.MyCertificateRequest;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.MyCertificateResponse;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.MyContractResponse;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.PersonalOverviewResponse;
import com.company.ops.api.modules.system.dto.PersonalSettingsDtos.UpdateMyProfileRequest;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalSettingsService {
  private final SystemUserRepository userRepository;
  private final QualificationEmployeeRepository employeeRepository;
  private final PersonnelCertificateRepository certificateRepository;
  private final EmployeeContractRepository contractRepository;
  private final PasswordEncoder passwordEncoder;
  private final ObjectMapper objectMapper;

  public PersonalSettingsService(
      SystemUserRepository userRepository,
      QualificationEmployeeRepository employeeRepository,
      PersonnelCertificateRepository certificateRepository,
      EmployeeContractRepository contractRepository,
      PasswordEncoder passwordEncoder,
      ObjectMapper objectMapper
  ) {
    this.userRepository = userRepository;
    this.employeeRepository = employeeRepository;
    this.certificateRepository = certificateRepository;
    this.contractRepository = contractRepository;
    this.passwordEncoder = passwordEncoder;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public PersonalOverviewResponse overview(UUID userId) {
    SystemUser user = requireUser(userId);
    QualificationEmployee employee = employeeRepository.findBySystemUser_Id(userId).orElse(null);
    List<MyCertificateResponse> certificates = employee == null ? List.of()
        : certificateRepository.findByEmployeeIdOrderByNameAsc(employee.getId()).stream().map(this::toCertificate).toList();
    List<MyContractResponse> contracts = employee == null ? List.of()
        : contractRepository.findByEmployeeIdOrderByStartDateDesc(employee.getId()).stream().map(this::toContract).toList();
    return new PersonalOverviewResponse(toAccount(user), toEmployee(employee), certificates, contracts);
  }

  @Transactional
  public AccountProfile updateProfile(UUID userId, UpdateMyProfileRequest request) {
    SystemUser user = requireUser(userId);
    user.setDisplayName(request.displayName().trim());
    user.setPhone(normalize(request.phone()));
    user.setEmail(normalize(request.email()));
    employeeRepository.findBySystemUser_Id(userId).ifPresent(employee -> employee.setPhone(normalize(request.phone())));
    return toAccount(userRepository.save(user));
  }

  @Transactional
  public void changePassword(UUID userId, ChangeMyPasswordRequest request) {
    SystemUser user = requireUser(userId);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new BusinessException("当前密码不正确");
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw new BusinessException("新密码不能与当前密码相同");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
  }

  @Transactional
  public MyCertificateResponse createCertificate(UUID userId, MyCertificateRequest request) {
    QualificationEmployee employee = requireEmployee(userId);
    PersonnelCertificate certificate = new PersonnelCertificate();
    certificate.setExternalId("SELF-CERT-" + UUID.randomUUID());
    certificate.setEmployee(employee);
    applySelfCertificate(certificate, request);
    return toCertificate(certificateRepository.save(certificate));
  }

  @Transactional
  public MyCertificateResponse updateCertificate(UUID userId, UUID certificateId, MyCertificateRequest request) {
    QualificationEmployee employee = requireEmployee(userId);
    PersonnelCertificate certificate = certificateRepository.findById(certificateId)
        .orElseThrow(() -> new BusinessException("证书不存在"));
    validateOwnership(employee, certificate);
    if (certificate.isLocked()) throw new BusinessException("该证书已锁定，请联系管理员处理");
    applySelfCertificate(certificate, request);
    return toCertificate(certificateRepository.save(certificate));
  }

  @Transactional
  public void deleteCertificate(UUID userId, UUID certificateId) {
    QualificationEmployee employee = requireEmployee(userId);
    PersonnelCertificate certificate = certificateRepository.findById(certificateId)
        .orElseThrow(() -> new BusinessException("证书不存在"));
    validateOwnership(employee, certificate);
    if (certificate.isLocked()) throw new BusinessException("该证书已锁定，不能删除");
    certificateRepository.delete(certificate);
  }

  @Transactional(readOnly = true)
  public void assertEmployeeLinked(UUID userId) {
    requireEmployee(userId);
  }

  private void applySelfCertificate(PersonnelCertificate certificate, MyCertificateRequest request) {
    certificate.setName(request.name().trim());
    certificate.setType(normalize(request.type()));
    certificate.setCertificateNo(normalize(request.certificateNo()));
    certificate.setSpecialty(normalize(request.specialty()));
    certificate.setIssueDate(request.issueDate());
    certificate.setValidTo(request.validTo());
    certificate.setReviewDate(request.reviewDate());
    certificate.setCompanyRegistered(false);
    certificate.setAvailableForTender(false);
    certificate.setManualStatus("UNVERIFIED");
    certificate.setLocked(false);
    certificate.setAttachmentsJson(writeAttachments(request.attachments()));
    certificate.setRemark(normalize(request.remark()));
  }

  private void validateOwnership(QualificationEmployee employee, PersonnelCertificate certificate) {
    if (!employee.getId().equals(certificate.getEmployee().getId())) {
      throw new BusinessException("无权操作其他员工的证书");
    }
  }

  private SystemUser requireUser(UUID userId) {
    return userRepository.findById(userId).orElseThrow(() -> new BusinessException("账号不存在"));
  }

  private QualificationEmployee requireEmployee(UUID userId) {
    return employeeRepository.findBySystemUser_Id(userId)
        .orElseThrow(() -> new BusinessException("当前账号尚未关联员工档案"));
  }

  private AccountProfile toAccount(SystemUser user) {
    return new AccountProfile(user.getId(), user.getUsername(), user.getDisplayName(), user.getPhone(), user.getEmail(),
        user.getOrganization() == null ? null : user.getOrganization().getName(), user.isEnabled());
  }

  private EmployeeProfile toEmployee(QualificationEmployee employee) {
    if (employee == null) return null;
    SystemOrganization organization = employee.getOrganization();
    return new EmployeeProfile(employee.getId(), employee.getName(), employee.getWorkNo(),
        organization == null ? null : organization.getName(), organization == null ? null : organizationPath(organization),
        employee.getPosition(), employee.getEntryDate(), employee.getEmploymentStatus());
  }

  private MyCertificateResponse toCertificate(PersonnelCertificate certificate) {
    LocalDate dueDate = earliest(certificate.getValidTo(), certificate.getReviewDate());
    return new MyCertificateResponse(certificate.getId(), certificate.getName(), certificate.getType(),
        certificate.getCertificateNo(), certificate.getSpecialty(), certificate.getIssueDate(), certificate.getValidTo(),
        certificate.getReviewDate(), certificate.isCompanyRegistered(), certificate.isAvailableForTender(), certificate.isLocked(),
        certificateStatus(certificate), days(dueDate), readAttachments(certificate.getAttachmentsJson()), certificate.getRemark());
  }

  private MyContractResponse toContract(EmployeeContract contract) {
    String status = "TERMINATED".equalsIgnoreCase(contract.getStatus()) || "DRAFT".equalsIgnoreCase(contract.getStatus())
        ? contract.getStatus().toUpperCase()
        : contract.getEndDate() != null && contract.getEndDate().isBefore(LocalDate.now()) ? "EXPIRED" : "ACTIVE";
    return new MyContractResponse(contract.getId(), contract.getContractNo(), contract.getContractType(), contract.getSignDate(),
        contract.getStartDate(), contract.getEndDate(), contract.getProbationEndDate(), status,
        readAttachments(contract.getAttachmentsJson()), contract.getRemark());
  }

  private String certificateStatus(PersonnelCertificate certificate) {
    if (certificate.isLocked()) return "LOCKED";
    if ("VOIDED".equalsIgnoreCase(certificate.getManualStatus())) return "VOIDED";
    if ("UNVERIFIED".equalsIgnoreCase(certificate.getManualStatus())) return "UNVERIFIED";
    LocalDate dueDate = earliest(certificate.getValidTo(), certificate.getReviewDate());
    if (dueDate == null) return "VALID";
    long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    return days < 0 ? "EXPIRED" : days <= 180 ? "EXPIRING" : "VALID";
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

  private LocalDate earliest(LocalDate first, LocalDate second) {
    if (first == null) return second;
    if (second == null) return first;
    return first.isBefore(second) ? first : second;
  }

  private Long days(LocalDate value) {
    return value == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), value);
  }

  private String writeAttachments(List<Attachment> attachments) {
    try {
      return objectMapper.writeValueAsString(attachments == null ? List.of() : attachments);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("证书附件保存失败");
    }
  }

  private List<Attachment> readAttachments(String json) {
    if (json == null || json.isBlank()) return List.of();
    try {
      return objectMapper.readValue(json, new TypeReference<List<Attachment>>() {});
    } catch (JsonProcessingException exception) {
      return List.of();
    }
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
