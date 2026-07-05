package com.company.ops.api.modules.system.service;

import com.company.ops.api.modules.system.dto.LoginRequest;
import com.company.ops.api.modules.system.dto.LoginResponse;
import com.company.ops.api.modules.system.security.JwtService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  public LoginResponse login(LoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password())
    );
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    return new LoginResponse(jwtService.createToken(principal), toCurrentUser(principal));
  }

  public LoginResponse.CurrentUserResponse currentUser(UserPrincipal principal) {
    if (principal == null) {
      return null;
    }
    return toCurrentUser(principal);
  }

  private LoginResponse.CurrentUserResponse toCurrentUser(UserPrincipal principal) {
    return new LoginResponse.CurrentUserResponse(
        principal.id().toString(),
        principal.getUsername(),
        principal.displayName(),
        principal.roleCodes(),
        principal.permissions()
    );
  }
}

