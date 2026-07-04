package com.company.ops.api.modules.system.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.service.CodeGenerator;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.dto.CreateOrganizationRequest;
import com.company.ops.api.modules.system.dto.UpdateOrganizationRequest;
import com.company.ops.api.modules.system.dto.OrganizationResponse;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemRoleRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.qualification.repository.QualificationEmployeeRepository;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

  private final CodeGenerator codeGenerator;
  private final SystemOrganizationRepository organizationRepository;
  private final SystemUserRepository userRepository;
  private final SystemRoleRepository roleRepository;
  private final QualificationEmployeeRepository employeeRepository;

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public OrganizationService(
      CodeGenerator codeGenerator,
      SystemOrganizationRepository organizationRepository,
      SystemUserRepository userRepository,
      SystemRoleRepository roleRepository,
      QualificationEmployeeRepository employeeRepository) {
    this.codeGenerator = codeGenerator;
    this.organizationRepository = organizationRepository;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.employeeRepository = employeeRepository;
  }

  @Transactional(readOnly = true)
  public List<OrganizationResponse> listOrganizations() {
    List<SystemOrganization> all = organizationRepository.findByTenantIdOrderBySortOrderAsc("default");
    Map<UUID, List<SystemOrganization>> children = childOrganizations(all);
    Map<UUID, Long> directUsers = directUserCounts(all);
    Map<UUID, Long> directEmployees = directEmployeeCounts(all);
    return all.stream()
        .filter(item -> item.getParent() == null)
        .map(item -> toOrganizationResponse(item, children, directUsers, directEmployees, true))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<OrganizationResponse> listOrganizationsFlat() {
    List<SystemOrganization> all = organizationRepository.findByTenantIdOrderBySortOrderAsc("default");
    Map<UUID, List<SystemOrganization>> children = childOrganizations(all);
    Map<UUID, Long> directUsers = directUserCounts(all);
    Map<UUID, Long> directEmployees = directEmployeeCounts(all);
    return all.stream()
        .map(item -> toOrganizationResponse(item, children, directUsers, directEmployees, false))
        .toList();
  }

  @Transactional(readOnly = true)
  public OrganizationResponse getOrganization(UUID id) {
    List<SystemOrganization> all = organizationRepository.findByTenantIdOrderBySortOrderAsc("default");
    SystemOrganization org = all.stream().filter(item -> item.getId().equals(id)).findFirst()
        .orElseThrow(() -> new BusinessException("组织不存在"));
    return toOrganizationResponse(org, childOrganizations(all), directUserCounts(all), directEmployeeCounts(all), true);
  }

  @Transactional
  public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
    String orgCode = request.code() != null ? request.code() : codeGenerator.generate("ORGANIZATION");
    if (organizationRepository.existsByCodeAndTenantId(orgCode, "default")) {
      throw new BusinessException("组织代码已存在");
    }
    validateOrganizationType(request.type());
    SystemOrganization org = new SystemOrganization();
    org.setCode(orgCode);
    org.setName(request.name());
    org.setType(request.type() != null ? request.type() : "DEPARTMENT");
    org.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
    org.setLeaderName(request.leaderName());
    org.setPhone(request.phone());
    org.setEnabled(request.enabled() == null || request.enabled());
    org.setDescription(request.description());

    if (request.parentId() != null) {
      SystemOrganization parent = organizationRepository.findById(request.parentId())
          .orElseThrow(() -> new BusinessException("上级组织不存在"));
      org.setParent(parent);
    }

    SystemOrganization saved = organizationRepository.save(org);
    employeeRepository.findByOrganization_IdOrderByNameAsc(saved.getId()).forEach(employee -> employee.setDepartment(saved.getName()));
    return getOrganization(saved.getId());
  }

  @Transactional
  public OrganizationResponse updateOrganization(UUID id, UpdateOrganizationRequest request) {
    SystemOrganization org = organizationRepository.findById(id)
        .orElseThrow(() -> new BusinessException("组织不存在"));

    if (request.name() != null) org.setName(request.name());
    if (request.type() != null) { validateOrganizationType(request.type()); org.setType(request.type()); }
    if (request.sortOrder() != null) org.setSortOrder(request.sortOrder());
    if (request.leaderName() != null) org.setLeaderName(request.leaderName());
    if (request.phone() != null) org.setPhone(request.phone());
    if (request.enabled() != null) org.setEnabled(request.enabled());
    if (request.description() != null) org.setDescription(request.description());

    if (request.parentId() != null) {
      SystemOrganization parent = organizationRepository.findById(request.parentId())
          .orElseThrow(() -> new BusinessException("上级组织不存在"));
      validateOrganizationParent(org, parent);
      org.setParent(parent);
    } else {
      org.setParent(null);
    }

    SystemOrganization saved = organizationRepository.save(org);
    return getOrganization(saved.getId());
  }

  @Transactional
  public void deleteOrganization(UUID id) {
    SystemOrganization org = organizationRepository.findById(id)
        .orElseThrow(() -> new BusinessException("组织不存在"));
    if ("ROOT".equals(org.getCode())) { throw new BusinessException("根组织不能删除"); }
    if (organizationRepository.countByParent_Id(id) > 0) { throw new BusinessException("该组织存在下级组织，请先调整下级组织"); }
    long userCount = userRepository.countByOrganization_Id(id);
    if (userCount > 0) { throw new BusinessException("该组织仍有 " + userCount + " 个账号，请先转移成员"); }
    long employeeCount = employeeRepository.countByOrganization_Id(id);
    if (employeeCount > 0) { throw new BusinessException("该组织仍有 " + employeeCount + " 名员工，请先在人事管理中调整部门"); }
    if (roleRepository.countByDataScopeOrganizations_Id(id) > 0) { throw new BusinessException("该组织仍被角色数据范围引用，不能删除"); }
    organizationRepository.delete(org);
  }

  private OrganizationResponse toOrganizationResponse(
      SystemOrganization org,
      Map<UUID, List<SystemOrganization>> children,
      Map<UUID, Long> directUsers,
      Map<UUID, Long> directEmployees,
      boolean includeChildren) {
    String parentName = org.getParent() != null ? org.getParent().getName() : null;
    List<SystemOrganization> childEntities = children.getOrDefault(org.getId(), List.of());
    List<OrganizationResponse> childResponses = includeChildren
        ? childEntities.stream()
            .map(child -> toOrganizationResponse(child, children, directUsers, directEmployees, true))
            .toList()
        : List.of();
    return new OrganizationResponse(
        org.getId(), org.getCode(), org.getName(), org.getType(), org.getSortOrder(),
        org.getParent() != null ? org.getParent().getId() : null, parentName, organizationPath(org),
        org.getLeaderName(), org.getPhone(), org.isEnabled(), org.getDescription(),
        directUsers.getOrDefault(org.getId(), 0L),
        totalUserCount(org.getId(), children, directUsers),
        directEmployees.getOrDefault(org.getId(), 0L),
        totalEmployeeCount(org.getId(), children, directEmployees),
        childEntities.size(), childResponses,
        org.getCreatedAt() != null ? org.getCreatedAt().format(DT_FORMATTER) : null,
        org.getUpdatedAt() != null ? org.getUpdatedAt().format(DT_FORMATTER) : null);
  }

  private Map<UUID, List<SystemOrganization>> childOrganizations(List<SystemOrganization> organizations) {
    Map<UUID, List<SystemOrganization>> result = new HashMap<>();
    for (SystemOrganization organization : organizations) {
      if (organization.getParent() != null) {
        result.computeIfAbsent(organization.getParent().getId(), ignored -> new ArrayList<>()).add(organization);
      }
    }
    result.values().forEach(items -> items.sort((a, b) -> {
      int order = Integer.compare(a.getSortOrder() == null ? 0 : a.getSortOrder(), b.getSortOrder() == null ? 0 : b.getSortOrder());
      return order != 0 ? order : a.getName().compareTo(b.getName());
    }));
    return result;
  }

  private Map<UUID, Long> directUserCounts(List<SystemOrganization> organizations) {
    Map<UUID, Long> counts = new HashMap<>();
    organizations.forEach(org -> counts.put(org.getId(), userRepository.countByOrganization_Id(org.getId())));
    return counts;
  }

  private Map<UUID, Long> directEmployeeCounts(List<SystemOrganization> organizations) {
    Map<UUID, Long> counts = new HashMap<>();
    organizations.forEach(org -> counts.put(org.getId(), employeeRepository.countByOrganization_Id(org.getId())));
    return counts;
  }

  private long totalUserCount(UUID orgId, Map<UUID, List<SystemOrganization>> children, Map<UUID, Long> directUsers) {
    long total = directUsers.getOrDefault(orgId, 0L);
    for (SystemOrganization child : children.getOrDefault(orgId, List.of())) total += totalUserCount(child.getId(), children, directUsers);
    return total;
  }

  private long totalEmployeeCount(UUID orgId, Map<UUID, List<SystemOrganization>> children, Map<UUID, Long> directEmployees) {
    long total = directEmployees.getOrDefault(orgId, 0L);
    for (SystemOrganization child : children.getOrDefault(orgId, List.of())) total += totalEmployeeCount(child.getId(), children, directEmployees);
    return total;
  }

  private String organizationPath(SystemOrganization organization) {
    List<String> names = new ArrayList<>();
    Set<UUID> visited = new HashSet<>();
    SystemOrganization current = organization;
    while (current != null && visited.add(current.getId())) { names.add(current.getName()); current = current.getParent(); }
    Collections.reverse(names);
    return String.join(" / ", names);
  }

  private void validateOrganizationType(String type) {
    if (type != null && !Set.of("COMPANY", "DEPARTMENT", "TEAM").contains(type)) throw new BusinessException("组织类型不正确");
  }

  private void validateOrganizationParent(SystemOrganization org, SystemOrganization parent) {
    if (org.getId().equals(parent.getId())) throw new BusinessException("上级组织不能选择自身");
    Set<UUID> visited = new HashSet<>();
    SystemOrganization current = parent;
    while (current != null && visited.add(current.getId())) {
      if (org.getId().equals(current.getId())) throw new BusinessException("不能将组织移动到自己的下级组织");
      current = current.getParent();
    }
  }
}
