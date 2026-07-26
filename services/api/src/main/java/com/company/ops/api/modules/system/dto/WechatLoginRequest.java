package com.company.ops.api.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WechatLoginRequest(@NotBlank @Size(max = 200) String code) {}
