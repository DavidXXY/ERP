package com.company.ops.api.modules.system.dto;

import java.util.List;

public record LoginResponse(
    String token,
    CurrentUserResponse user,
    boolean mfaRequired
) {

  public LoginResponse(String token, CurrentUserResponse user) {
    this(token, user, false);
  }

  public static LoginResponse mfaChallenge() {
    return new LoginResponse(null, null, true);
  }

  public record CurrentUserResponse(
      String id,
      String username,
      String displayName,
      List<String> roles,
      List<String> roleCodes,
      List<String> permissions
  ) {
  }
}
