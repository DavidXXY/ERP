package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.company.ops.api.common.validation.StrongPassword;
import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
    UUID orgId,
    @NotBlank @Size(max = 80) String username,
    @NotBlank @Size(max = 80) String displayName,
    @NotBlank @StrongPassword String password,
    @Size(max = 40) String phone,
    @Size(max = 120) String email,
    List<UUID> roleIds
) {}
