package com.company.ops.api.modules.finance.service;

import com.company.ops.api.common.exception.BusinessException;
import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.FinanceOrganizationNode;
import com.company.ops.api.modules.finance.dto.FinanceAnalyticsDtos.FinanceScopeInfo;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import com.company.ops.api.modules.system.security.DataScopeService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceOrganizationScopeService {
  private final DataScopeService dataScopeService;
  private final SystemOrganizationRepository organizationRepository;
  private final SystemUserRepository userRepository;

  public FinanceOrganizationScopeService(
      DataScopeService dataScopeService,
      SystemOrganizationRepository organizationRepository,
      SystemUserRepository userRepository) {
    this.dataScopeService = dataScopeService;
    this.organizationRepository = organizationRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<FinanceOrganizationNode> visibleOrganizations() {
    List<SystemOrganization> all = organizations();
    Set<UUID> permitted = dataScopeService.visibleOrganizationIds();
    Map<UUID, List<SystemOrganization>> children = new HashMap<>();
    for (SystemOrganization organization : all) {
      if (!permitted.contains(organization.getId()) || organization.getParent() == null) continue;
      children.computeIfAbsent(organization.getParent().getId(), ignored -> new ArrayList<>())
          .add(organization);
    }
    return all.stream()
        .filter(item -> permitted.contains(item.getId()))
        .filter(item -> item.getParent() == null || !permitted.contains(item.getParent().getId()))
        .map(item -> toNode(item, children))
        .toList();
  }

  @Transactional(readOnly = true)
  public Scope resolve(UUID requestedOrganizationId, boolean includeDescendants) {
    List<SystemOrganization> all = organizations();
    Map<UUID, SystemOrganization> byId = all.stream()
        .collect(Collectors.toMap(SystemOrganization::getId, item -> item));
    Set<UUID> permitted = dataScopeService.visibleOrganizationIds();
    boolean allData = dataScopeService.hasAllDataScope();

    if (requestedOrganizationId == null) {
      String name = allData ? "全公司" : permitted.isEmpty() ? "无可用组织" : "授权组织";
      String path = allData ? "全部组织及未分摊数据" : "按角色数据范围汇总";
      return new Scope(Set.copyOf(permitted), allData,
          new FinanceScopeInfo(null, name, path, true, permitted.size(), allData, !allData));
    }

    SystemOrganization selected = byId.get(requestedOrganizationId);
    if (selected == null) throw new BusinessException("所选组织不存在");
    if (!dataScopeService.canViewOrganization(requestedOrganizationId)) {
      throw new AccessDeniedException("无权查看所选组织的财务数据");
    }
    Set<UUID> selectedIds = includeDescendants
        ? descendants(requestedOrganizationId, all)
        : Set.of(requestedOrganizationId);
    selectedIds = selectedIds.stream().filter(permitted::contains).collect(Collectors.toUnmodifiableSet());
    boolean coversAll = allData && selectedIds.size() == all.size();
    return new Scope(selectedIds, coversAll,
        new FinanceScopeInfo(selected.getId(), selected.getName(), path(selected),
            includeDescendants, selectedIds.size(), coversAll, !coversAll));
  }

  @Transactional(readOnly = true)
  public Set<String> ownerNames(Scope scope) {
    if (scope.unrestricted()) {
      return userRepository.findByEnabledTrueOrderByDisplayNameAsc().stream()
          .map(item -> item.getDisplayName()).collect(Collectors.toUnmodifiableSet());
    }
    if (scope.organizationIds().isEmpty()) return Set.of();
    return userRepository.findByOrganization_IdIn(scope.organizationIds()).stream()
        .map(item -> item.getDisplayName()).collect(Collectors.toUnmodifiableSet());
  }

  private List<SystemOrganization> organizations() {
    return organizationRepository.findByTenantIdOrderBySortOrderAsc(TenantContext.currentTenant());
  }

  private Set<UUID> descendants(UUID rootId, List<SystemOrganization> all) {
    Set<UUID> result = new HashSet<>();
    result.add(rootId);
    boolean changed;
    do {
      changed = false;
      for (SystemOrganization item : all) {
        if (item.getParent() != null && result.contains(item.getParent().getId())
            && result.add(item.getId())) changed = true;
      }
    } while (changed);
    return Set.copyOf(result);
  }

  private FinanceOrganizationNode toNode(
      SystemOrganization organization,
      Map<UUID, List<SystemOrganization>> children) {
    List<FinanceOrganizationNode> childNodes = children
        .getOrDefault(organization.getId(), List.of()).stream()
        .map(item -> toNode(item, children))
        .toList();
    return new FinanceOrganizationNode(organization.getId(), organization.getName(),
        organization.getType(), path(organization), childNodes);
  }

  private String path(SystemOrganization organization) {
    List<String> names = new ArrayList<>();
    Set<UUID> visited = new HashSet<>();
    SystemOrganization current = organization;
    while (current != null && visited.add(current.getId())) {
      names.add(current.getName());
      current = current.getParent();
    }
    Collections.reverse(names);
    return String.join(" / ", names);
  }

  public record Scope(Set<UUID> organizationIds, boolean unrestricted, FinanceScopeInfo info) {
    public boolean includes(UUID organizationId) {
      return unrestricted || organizationId != null && organizationIds.contains(organizationId);
    }
  }
}
