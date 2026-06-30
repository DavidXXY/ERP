package com.company.ops.api.modules.system.security;

import com.company.ops.api.modules.system.domain.SystemUser;
import com.company.ops.api.modules.system.repository.SystemUserRepository;
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
  public DataScopeService(SystemUserRepository userRepository) { this.userRepository = userRepository; }

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

  protected Set<String> visibleDisplayNames(UserPrincipal principal) {
    if (!principal.dataScopes().contains("DEPARTMENT")) return Set.of(principal.displayName());
    SystemUser user = userRepository.findById(principal.id()).orElse(null);
    if (user == null || user.getOrganization() == null) return Set.of(principal.displayName());
    return userRepository.findByOrganization_Id(user.getOrganization().getId()).stream().map(SystemUser::getDisplayName).collect(Collectors.toSet());
  }

  protected Set<UUID> visibleUserIds(UserPrincipal principal) {
    if (!principal.dataScopes().contains("DEPARTMENT")) return Set.of(principal.id());
    SystemUser user = userRepository.findById(principal.id()).orElse(null);
    if (user == null || user.getOrganization() == null) return Set.of(principal.id());
    return userRepository.findByOrganization_Id(user.getOrganization().getId()).stream().map(SystemUser::getId).collect(Collectors.toSet());
  }

  private UserPrincipal principal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof UserPrincipal value ? value : null;
  }
}
