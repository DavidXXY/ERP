package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class LegalEntityDtos {
  private LegalEntityDtos() {}

  public record CreateLegalEntityRequest(
      @NotBlank @Size(max = 64) String code,
      @NotBlank @Size(max = 160) String name,
      @Size(max = 24) String entityType,
      @Size(max = 8) String currency,
      boolean active,
      @Size(max = 500) String remark
  ) {}

  public record LegalEntityResponse(
      UUID id, String code, String name, String entityType, String currency, boolean active, String remark
  ) {}
}