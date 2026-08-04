package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class MfaDtos {
  private MfaDtos() {}

  public record MfaStatusResponse(boolean enabled, int recoveryCodesRemaining) {}

  public record BeginMfaSetupRequest(@NotBlank String currentPassword) {}

  public record MfaSetupResponse(String secret, String otpauthUri) {}

  public record VerifyMfaRequest(@NotBlank @Size(max = 32) String code) {}

  public record ConfirmMfaResponse(List<String> recoveryCodes) {}

  public record ProtectedMfaRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(max = 32) String code
  ) {}
}
