package com.company.ops.api.modules.system.dto;

import java.util.List;

public record LoginResponse(
    String token,
    CurrentUserResponse user
) {

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
