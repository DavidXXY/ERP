package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WechatBindRequest(
    @NotBlank @Size(max = 200) String code,
    @NotBlank @Size(max = 80) String username,
    @NotBlank @Size(max = 100) String password
) {}
