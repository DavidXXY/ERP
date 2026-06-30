package com.company.ops.api.modules.system.security;

import com.company.ops.api.modules.system.domain.SystemPermission;
import com.company.ops.api.modules.system.domain.SystemRole;
import com.company.ops.api.modules.system.domain.SystemUser;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
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
  private final List<String> permissions;
  private final List<String> dataScopes;
  private final List<GrantedAuthority> authorities;

  public UserPrincipal(SystemUser user) {
    this.id = user.getId();
    this.username = user.getUsername();
    this.displayName = user.getDisplayName();
    this.password = user.getPasswordHash();
    this.enabled = user.isEnabled();
    this.roleCodes = user.getRoles().stream().map(SystemRole::getCode).sorted().toList();
    this.permissions = user.getRoles().stream()
        .flatMap(role -> role.getPermissions().stream())
        .map(SystemPermission::getCode)
        .distinct()
        .sorted()
        .toList();
    this.dataScopes = user.getRoles().stream().map(SystemRole::getDataScope).distinct().sorted().toList();
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

  public List<String> permissions() {
    return permissions;
  }

  public List<String> dataScopes() {
    return dataScopes;
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
