package com.company.ops.api.modules.system.security;

import com.company.ops.api.common.tenant.TenantContext;
import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.domain.SystemOrganization;
import com.company.ops.api.modules.system.repository.SystemOrganizationRepository;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataScopeService {
  private final SystemUserRepository userRepository;
  private final SystemOrganizationRepository organizationRepository;

  public DataScopeService(
      SystemUserRepository userRepository,
      SystemOrganizationRepository organizationRepository
  ) {
    this.userRepository = userRepository;
    this.organizationRepository = organizationRepository;
  }

  @Transactional(readOnly = true)
  public boolean canViewOwner(String ownerName) {
    UserPrincipal principal = principal();
    return principal != null && (principal.dataScopes().contains("ALL") || visibleDisplayNames(principal).contains(ownerName));
  }

  @Transactional(readOnly = true)
  public boolean canViewAssignee(UUID assigneeId, boolean includeUnassigned) {
    UserPrincipal principal = principal();
    if (principal == null) return false;
    if (principal.dataScopes().contains("ALL")) return true;
    if (assigneeId == null) return includeUnassigned;
    return visibleUserIds(principal).contains(assigneeId);
  }

  public boolean hasAuthority(String authority) {
    UserPrincipal principal = principal();
    return principal != null && principal.permissions().contains(authority);
  }

  @Transactional(readOnly = true)
  public boolean canViewOrganization(UUID organizationId) {
    UserPrincipal principal = principal();
    if (principal == null) return false;
    if (principal.dataScopes().contains("ALL")) return true;
    if (organizationId == null) return false;
    SystemUser user = userRepository.findById(principal.id()).orElse(null);
    if (user != null && user.getOrganization() != null) {
      UUID current = user.getOrganization().getId();
      if ((principal.dataScopes().contains("DEPT") || principal.dataScopes().contains("DEPARTMENT")) && current.equals(organizationId)) return true;
      if (principal.dataScopes().contains("DEPT_AND_SUB") && organizationAndDescendantIds(current).contains(organizationId)) return true;
    }
    return principal.dataScopes().contains("CUSTOM") && principal.dataScopeOrganizationIds().contains(organizationId);
  }

  protected Set<String> visibleDisplayNames(UserPrincipal principal) {
    Set<UUID> userIds = visibleUserIds(principal);
    if (userIds.isEmpty()) return Set.of(principal.displayName());
    Set<String> names = userRepository.findAllById(userIds).stream()
        .map(SystemUser::getDisplayName)
        .collect(Collectors.toSet());
    names.add(principal.displayName());
    return names;
  }

  protected Set<UUID> visibleUserIds(UserPrincipal principal) {
    if (principal.dataScopes().contains("ALL")) {
      return userRepository.findAll().stream().map(SystemUser::getId).collect(Collectors.toSet());
    }
    Set<UUID> visible = new HashSet<>();
    visible.add(principal.id());
    SystemUser user = userRepository.findById(principal.id()).orElse(null);
    Set<UUID> organizationIds = new HashSet<>();
    if (user != null && user.getOrganization() != null) {
      UUID currentOrganizationId = user.getOrganization().getId();
      if (principal.dataScopes().contains("DEPT") || principal.dataScopes().contains("DEPARTMENT")) {
        organizationIds.add(currentOrganizationId);
      }
      if (principal.dataScopes().contains("DEPT_AND_SUB")) {
        organizationIds.addAll(organizationAndDescendantIds(currentOrganizationId));
      }
    }
    if (principal.dataScopes().contains("CUSTOM")) {
      organizationIds.addAll(principal.dataScopeOrganizationIds());
    }
    if (!organizationIds.isEmpty()) {
      visible.addAll(userRepository.findByOrganization_IdIn(organizationIds).stream()
          .map(SystemUser::getId)
          .toList());
    }
    return visible;
  }

  private Set<UUID> organizationAndDescendantIds(UUID rootId) {
    List<SystemOrganization> organizations = organizationRepository
        .findByTenantIdOrderBySortOrderAsc(TenantContext.currentTenant());
    Set<UUID> visible = new HashSet<>();
    visible.add(rootId);
    boolean changed;
    do {
      changed = false;
      for (SystemOrganization organization : organizations) {
        if (organization.getParent() != null
            && visible.contains(organization.getParent().getId())
            && visible.add(organization.getId())) {
          changed = true;
        }
      }
    } while (changed);
    return visible;
  }

  private UserPrincipal principal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal value ? value : null;
  }
}
