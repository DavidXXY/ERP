package com.company.ops.api.modules.system.service;

import com.company.ops.api.modules.system.dto.LoginRequest;
import com.company.ops.api.modules.system.dto.LoginResponse;
import com.company.ops.api.modules.system.security.JwtService;
import com.company.ops.api.modules.system.security.LoginAttemptService;
import com.company.ops.api.modules.system.security.UserPrincipal;
import com.company.ops.api.modules.system.domain.SystemUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final LoginAttemptService loginAttemptService;
  private final MfaService mfaService;

  public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
      LoginAttemptService loginAttemptService, MfaService mfaService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.loginAttemptService = loginAttemptService;
    this.mfaService = mfaService;
  }

  @Transactional
  public LoginResponse login(LoginRequest request, String clientAddress) {
    String normalizedUsername = request.username().trim().toLowerCase();
    String accountKey = "user|" + normalizedUsername;
    String attemptKey = "user-ip|" + normalizedUsername + "|" + clientAddress;
    String addressKey = "ip|" + clientAddress;
    loginAttemptService.assertAllowed(accountKey);
    loginAttemptService.assertAllowed(attemptKey);
    loginAttemptService.assertAllowed(addressKey);
    Authentication authentication;
    try {
      authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.username(), request.password())
      );
    } catch (AuthenticationException exception) {
      loginAttemptService.failed(accountKey);
      loginAttemptService.failed(attemptKey);
      loginAttemptService.failed(addressKey);
      throw exception;
    }
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    if (principal.mfaEnabled()) {
      if (request.mfaCode() == null || request.mfaCode().isBlank()) {
        return LoginResponse.mfaChallenge();
      }
      if (!mfaService.verifyLoginCode(principal.id(), request.mfaCode())) {
        loginAttemptService.failed(accountKey);
        loginAttemptService.failed(attemptKey);
        loginAttemptService.failed(addressKey);
        throw new BadCredentialsException("MFA verification failed");
      }
    }
    loginAttemptService.succeeded(accountKey);
    loginAttemptService.succeeded(attemptKey);
    loginAttemptService.succeeded(addressKey);
    return new LoginResponse(jwtService.createToken(principal), toCurrentUser(principal));
  }

  public LoginResponse.CurrentUserResponse currentUser(UserPrincipal principal) {
    if (principal == null) {
      return null;
    }
    return toCurrentUser(principal);
  }

  public LoginResponse issueSession(SystemUser user) {
    UserPrincipal principal = new UserPrincipal(user);
    return new LoginResponse(jwtService.createToken(principal), toCurrentUser(principal));
  }

  private LoginResponse.CurrentUserResponse toCurrentUser(UserPrincipal principal) {
    return new LoginResponse.CurrentUserResponse(
        principal.id().toString(),
        principal.getUsername(),
        principal.displayName(),
        principal.roleCodes(),
        principal.roleCodes(),
        principal.permissions()
    );
  }
}
