package com.company.ops.api.modules.system.security;

import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

  private final UUID id;
  private final String username;
  private final String displayName;
  private final String password;
  private final boolean enabled;
  private final List<String> roleCodes;
  private final List<UUID> roleIds;
  private final List<String> permissions;
  private final List<String> dataScopes;
  private final Set<UUID> dataScopeOrganizationIds;
  private final List<GrantedAuthority> authorities;

  public UserPrincipal(SystemUser user) {
    this.id = user.getId();
    this.username = user.getUsername();
    this.displayName = user.getDisplayName();
    this.password = user.getPasswordHash();
    this.enabled = user.isEnabled();
    this.roleCodes = user.getRoles().stream().map(SystemRole::getCode).sorted().toList();
    this.roleIds = user.getRoles().stream().map(SystemRole::getId).sorted().toList();
    this.permissions = user.getRoles().stream()
        .flatMap(role -> role.getPermissions().stream())
        .map(SystemPermission::getCode)
        .distinct()
        .sorted()
        .toList();
    this.dataScopes = user.getRoles().stream().map(SystemRole::getDataScope).distinct().sorted().toList();
    this.dataScopeOrganizationIds = user.getRoles().stream()
        .filter(role -> "CUSTOM".equals(role.getDataScope()))
        .flatMap(role -> role.getDataScopeOrganizations().stream())
        .map(organization -> organization.getId())
        .collect(Collectors.toUnmodifiableSet());
    this.authorities = permissions.stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList();
  }

  public UUID id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }

  public List<String> roleCodes() {
    return roleCodes;
  }

  public List<UUID> roleIds() {
    return roleIds;
  }

  public List<String> permissions() {
    return permissions;
  }

  public List<String> dataScopes() {
    return dataScopes;
  }

  public Set<UUID> dataScopeOrganizationIds() {
    return dataScopeOrganizationIds;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
